package com.farmplanner;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Bank to Patch",
	description = "Plans your farm run from your bank: route, seeds, compost and teleports",
	tags = {"farming", "herb", "tree", "route", "teleport", "bank", "planner"}
)
public class FarmPlannerPlugin extends Plugin
{
	private static final int INVENTORY = InventoryID.INV;
	private static final int EQUIPMENT = InventoryID.WORN;
	private static final int BANK = InventoryID.BANK;

	private static final int PLANT_DISTANCE = 4;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private FarmPlannerConfig config;

	@Inject
	private BankHighlightOverlay bankOverlay;

	@Inject
	private PatchOverlay patchOverlay;

	@Inject
	private TeleportChoiceOverlay teleportOverlay;

	private FarmPlannerPanel panel;
	private NavigationButton navButton;

	/** Laatst bekende inhoud per container. Alleen op de client thread gebruiken. */
	private final Map<Integer, Item[]> containers = new HashMap<>();
	/** Farming patches die nu in de wereld geladen zijn. Alleen op de client thread. */
	private final Set<GameObject> patchObjects = new HashSet<>();
	/** Widgets (dialoogopties, spreuken) die nu gehighlight moeten worden. */
	private List<Widget> highlightedWidgets = Collections.emptyList();

	private volatile boolean bankSeen;
	private volatile FarmPlan plan;
	/** Per route-stap: gedaan (groen) of nog niet. */
	private volatile boolean[] done = new boolean[0];
	/** Wat je bij je hebt: itemnaam (kleine letters) -> aantal. */
	private volatile Map<String, Integer> carried = Collections.emptyMap();
	/** Meeneem-items die je nog mist (worden in de bank gemarkeerd). */
	private volatile Set<Integer> missingIds = Collections.emptySet();
	private int lastSeedCount = -1;
	/** Huidige stap is geplant, maar de gardener moet nog betaald worden. */
	private volatile boolean awaitingPayment;
	/** Plan voor het tabblad Saplings, of null. */
	private volatile SaplingPlan saplingPlan;
	private volatile Set<Integer> saplingMissing = Collections.emptySet();
	/** Staat het tabblad Saplings open? Dan markeert de bank de sapling-spullen. */
	private volatile boolean saplingTabActive;
	private int lastPaymentCount = -1;
	private WorldPoint ourHintArrow;
	private int tickCounter;

	@Provides
	FarmPlannerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FarmPlannerConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new FarmPlannerPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Bank to Patch")
			.icon(createIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(bankOverlay);
		overlayManager.add(patchOverlay);
		overlayManager.add(teleportOverlay);

		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				store(INVENTORY, client.getItemContainer(INVENTORY));
				store(EQUIPMENT, client.getItemContainer(EQUIPMENT));
				store(BANK, client.getItemContainer(BANK));
				scanScene();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(bankOverlay);
		overlayManager.remove(patchOverlay);
		overlayManager.remove(teleportOverlay);
		clientThread.invokeLater(this::clearOurHintArrow);
		containers.clear();
		patchObjects.clear();
		highlightedWidgets = Collections.emptyList();
		bankSeen = false;
		plan = null;
		saplingPlan = null;
		saplingTabActive = false;
	}

	// ------------------------------------------------------------------ items

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == INVENTORY || id == EQUIPMENT || id == BANK)
		{
			store(id, event.getItemContainer());
		}
		if (id == INVENTORY)
		{
			checkSeedPlanted(event.getItemContainer());
		}
	}

	private void store(int id, ItemContainer container)
	{
		if (container == null)
		{
			return;
		}
		containers.put(id, container.getItems());
		if (id == BANK)
		{
			bankSeen = true;
			SwingUtilities.invokeLater(() -> panel.setBankSeen(true));
		}
		else
		{
			updateCarried();
		}
	}

	/** Telt wat je bij je hebt (inventory + equipment) en ververst het paneel. Op de client thread. */
	private void updateCarried()
	{
		Map<String, Integer> counts = new HashMap<>();
		for (int id : new int[]{INVENTORY, EQUIPMENT})
		{
			Item[] items = containers.get(id);
			if (items == null)
			{
				continue;
			}
			for (Item item : items)
			{
				if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
				{
					continue;
				}
				// Genoteerde items hebben dezelfde naam, dus tellen ook mee (bijv. noted cactus spines).
				String name = itemManager.getItemComposition(item.getId()).getName();
				counts.merge(name.toLowerCase(Locale.ROOT), item.getQuantity(), Integer::sum);
			}
		}
		carried = counts;
		FarmPlan current = plan;
		missingIds = current == null ? Collections.emptySet() : current.missingIds(counts);
		refreshPanel();
		SaplingPlan sp = saplingPlan;
		saplingMissing = sp == null ? Collections.emptySet() : sp.missingIds(counts);
		refreshSaplingPanel();
	}

	private void refreshSaplingPanel()
	{
		SaplingPlan sp = saplingPlan;
		if (sp == null)
		{
			return;
		}
		Map<String, Integer> have = carried;
		SwingUtilities.invokeLater(() -> panel.showSaplingPlan(sp, have));
	}

	/** Knop "Maak sapling-plan" in het tabblad Saplings. */
	void requestSaplingPlan(RunType type)
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN || !bankSeen)
			{
				String msg = client.getGameState() != GameState.LOGGED_IN
					? getLanguage().t("Log in first.", "Log eerst in.")
					: getLanguage().t("Open your bank once first, so the plugin can see what you have.",
						"Open eerst je bank één keer, zodat de plugin kan zien wat je hebt.");
				SwingUtilities.invokeLater(() -> panel.showSaplingMessage(msg));
				return;
			}
			saplingPlan = SaplingPlan.build(type, collectOwnedItems(), client.getRealSkillLevel(Skill.FARMING),
				getLanguage());
			updateCarried();
		});
	}

	void clearSaplingPlan()
	{
		saplingPlan = null;
		saplingMissing = Collections.emptySet();
	}

	void setSaplingTabActive(boolean active)
	{
		saplingTabActive = active;
	}

	/** Item voor de volgende sapling-actie (zaad + pot, of seedling + gieter)? */
	boolean isSaplingActionItem(int itemId)
	{
		SaplingPlan sp = saplingPlan;
		if (!saplingTabActive || sp == null)
		{
			return false;
		}
		SaplingPlan.Action next = sp.progress(carried).next;
		return sp.isNextActionItem(itemManager.getItemComposition(itemId).getName(), next);
	}

	/**
	 * Als het zaad van de huidige stap minder wordt terwijl je naast een patch staat,
	 * heb je geplant: ga door naar de volgende stap.
	 */
	private void checkSeedPlanted(ItemContainer inventory)
	{
		FarmPlan.Step step = getCurrentStep();
		if (step == null || inventory == null)
		{
			lastSeedCount = -1;
			return;
		}
		if (awaitingPayment)
		{
			// Betaald als de betaling (ook noted) minder wordt.
			int pay = paymentCount(step);
			if (lastPaymentCount >= 0 && pay < lastPaymentCount)
			{
				lastPaymentCount = -1;
				changeStep(1);
				return;
			}
			lastPaymentCount = pay;
			return;
		}
		int now = inventory.count(step.getSeedItemId());
		if (lastSeedCount >= 0 && now < lastSeedCount && isNearPatch())
		{
			lastSeedCount = -1;
			if (step.getPayment() != null)
			{
				awaitingPayment = true;
				lastPaymentCount = paymentCount(step);
				refreshPanel();
				return;
			}
			changeStep(1);
			return;
		}
		lastSeedCount = now;
	}

	/** Hoeveel van de betaling voor deze stap heb je bij je (noted telt mee, zelfde naam). */
	private int paymentCount(FarmPlan.Step step)
	{
		String item = step.getPaymentItem();
		return item == null ? 0 : carried.getOrDefault(item.toLowerCase(Locale.ROOT), 0);
	}

	/** Huidige stap is geplant en wacht op betaling aan de gardener. */
	boolean isAwaitingPayment()
	{
		return awaitingPayment;
	}

	private boolean isNearPatch()
	{
		Player p = client.getLocalPlayer();
		FarmPlan current = plan;
		if (p == null || current == null)
		{
			return false;
		}
		WorldPoint me = p.getWorldLocation();
		Set<Integer> ids = PatchIds.forRun(current.getRunType());
		for (GameObject o : patchObjects)
		{
			if (ids.contains(o.getId()) && o.getWorldLocation().distanceTo(me) <= PLANT_DISTANCE)
			{
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ plan

	/** Wordt aangeroepen door de knop in het paneel. */
	void requestPlan(RunType type)
	{
		buildPlan(type, false);
	}

	/** Taal van het paneel en het plan. */
	Language getLanguage()
	{
		Language lang = config.language();
		return lang == null ? Language.ENGLISH : lang;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!FarmPlannerConfig.GROUP.equals(event.getGroup()) || !"language".equals(event.getKey()))
		{
			return;
		}
		SwingUtilities.invokeLater(() -> panel.applyLanguage());
		SaplingPlan sp = saplingPlan;
		if (sp != null)
		{
			requestSaplingPlan(sp.getRunType());
		}
		FarmPlan current = plan;
		if (current != null)
		{
			// Zelfde run opnieuw opbouwen in de nieuwe taal, voortgang blijft staan.
			buildPlan(current.getRunType(), true);
		}
	}

	private void buildPlan(RunType type, boolean keepProgress)
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				SwingUtilities.invokeLater(() -> panel.showMessage(getLanguage().t("Log in first.", "Log eerst in.")));
				return;
			}
			if (!bankSeen)
			{
				SwingUtilities.invokeLater(() -> panel.showMessage(getLanguage().t(
					"Open your bank once first, so the plugin can see what you have.",
					"Open eerst je bank één keer, zodat de plugin kan zien wat je hebt.")));
				return;
			}

			OwnedItems owned = collectOwnedItems();
			int farming = client.getRealSkillLevel(Skill.FARMING);
			int magic = client.getBoostedSkillLevel(Skill.MAGIC);
			PlanOptions options = new PlanOptions(
				config.useSpells(),
				config.houseInHosidius(),
				config.includeTrollheim(),
				config.includeLletya(),
				Math.max(0, config.coinsPerTree()),
				config.payProtection(),
				getLanguage());

			FarmPlan newPlan = PlanBuilder.build(type, owned, farming, magic, options);
			boolean[] newDone = new boolean[newPlan.getActiveSteps().size()];
			if (keepProgress && done.length == newDone.length)
			{
				newDone = done.clone();
			}
			done = newDone;
			if (!keepProgress)
			{
				awaitingPayment = false;
			}
			plan = newPlan;
			lastSeedCount = -1;
			checkSeedPlanted(client.getItemContainer(INVENTORY));
			updateCarried(); // berekent wat je al bij je hebt en ververst het paneel
		});
	}

	private void refreshPanel()
	{
		FarmPlan current = plan;
		if (current == null)
		{
			return;
		}
		boolean[] snapshot = done.clone();
		Map<String, Integer> have = carried;
		boolean paying = awaitingPayment;
		SwingUtilities.invokeLater(() -> panel.showPlan(current, snapshot, have, paying));
	}

	/** Klik op een stap in het paneel: gedaan / niet gedaan. */
	void toggleDone(int index)
	{
		boolean[] d = done;
		if (index < 0 || index >= d.length)
		{
			return;
		}
		d[index] = !d[index];
		afterStepChange();
	}

	private void afterStepChange()
	{
		awaitingPayment = false;
		FarmPlan finished = plan;
		if (finished != null && done.length > 0 && currentIndex() < 0)
		{
			// Alle stappen gedaan: run stoppen, highlights uit, samenvatting tonen.
			clearPlan();
			SwingUtilities.invokeLater(() -> panel.showFinished(finished));
			return;
		}
		clientThread.invokeLater(() ->
		{
			lastSeedCount = -1;
			lastPaymentCount = -1;
			checkSeedPlanted(client.getItemContainer(INVENTORY));
		});
		refreshPanel();
	}

	void clearPlan()
	{
		plan = null;
		awaitingPayment = false;
		clientThread.invokeLater(() ->
		{
			clearOurHintArrow();
			highlightedWidgets = Collections.emptyList();
		});
	}

	/**
	 * +1 = huidige stap afvinken (gedaan, groen).
	 * -1 = laatst afgevinkte stap weer open zetten.
	 */
	void changeStep(int delta)
	{
		boolean[] d = done;
		if (plan == null || d.length == 0)
		{
			return;
		}
		if (delta > 0)
		{
			int i = currentIndex();
			if (i >= 0)
			{
				d[i] = true;
			}
		}
		else
		{
			for (int i = d.length - 1; i >= 0; i--)
			{
				if (d[i])
				{
					d[i] = false;
					break;
				}
			}
		}
		afterStepChange();
	}

	/** Index van de eerste stap die nog niet gedaan is, of -1 als alles klaar is. */
	private int currentIndex()
	{
		boolean[] d = done;
		for (int i = 0; i < d.length; i++)
		{
			if (!d[i])
			{
				return i;
			}
		}
		return -1;
	}

	/** Huidige stap, of null als er geen plan is of de run klaar is. */
	FarmPlan.Step getCurrentStep()
	{
		FarmPlan current = plan;
		if (current == null)
		{
			return null;
		}
		List<FarmPlan.Step> steps = current.getActiveSteps();
		int i = currentIndex();
		return i >= 0 && i < steps.size() ? steps.get(i) : null;
	}

	FarmPlan getPlan()
	{
		return plan;
	}

	/** Moet op de client thread draaien (ItemManager). */
	private OwnedItems collectOwnedItems()
	{
		OwnedItems owned = new OwnedItems();
		for (Item[] items : containers.values())
		{
			for (Item item : items)
			{
				if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
				{
					continue;
				}
				ItemComposition comp = itemManager.getItemComposition(item.getId());
				if (comp.getPlaceholderTemplateId() != -1)
				{
					continue; // bank placeholder
				}
				int realId = comp.getNote() != -1 ? comp.getLinkedNoteId() : item.getId();
				owned.add(realId, comp.getName(), item.getQuantity());
			}
		}
		return owned;
	}

	// ------------------------------------------------------------------ patches in de wereld

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (PatchIds.isAnyPatch(event.getGameObject().getId()))
		{
			patchObjects.add(event.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		patchObjects.remove(event.getGameObject());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			patchObjects.clear();
		}
	}

	private void scanScene()
	{
		patchObjects.clear();
		Scene scene = client.getTopLevelWorldView().getScene();
		Tile[][][] tiles = scene.getTiles();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null || tile.getGameObjects() == null)
					{
						continue;
					}
					for (GameObject o : tile.getGameObjects())
					{
						if (o != null && PatchIds.isAnyPatch(o.getId()))
						{
							patchObjects.add(o);
						}
					}
				}
			}
		}
	}

	/** Patches van de huidige run die nu in beeld zijn. */
	List<GameObject> getVisiblePatches()
	{
		FarmPlan current = plan;
		if (current == null)
		{
			return Collections.emptyList();
		}
		Set<Integer> ids = PatchIds.forRun(current.getRunType());
		List<GameObject> result = new ArrayList<>();
		for (GameObject o : patchObjects)
		{
			if (ids.contains(o.getId()))
			{
				result.add(o);
			}
		}
		return result;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		updateHintArrow();
	}

	private void updateHintArrow()
	{
		Player p = client.getLocalPlayer();
		if (!config.hintArrow() || getCurrentStep() == null || p == null)
		{
			clearOurHintArrow();
			return;
		}
		WorldPoint me = p.getWorldLocation();
		WorldPoint nearestPoint = null;
		int best = Integer.MAX_VALUE;
		NPC gardener = awaitingPayment ? findGardener(getCurrentStep()) : null;
		if (gardener != null)
		{
			// Geplant: wijs naar de gardener om te betalen.
			nearestPoint = gardener.getWorldLocation();
			best = nearestPoint.distanceTo(me);
		}
		else
		{
			for (GameObject o : getVisiblePatches())
			{
				int d = o.getWorldLocation().distanceTo(me);
				if (d < best)
				{
					best = d;
					nearestPoint = o.getWorldLocation();
				}
			}
		}
		if (nearestPoint == null || best <= (gardener != null ? 1 : 2))
		{
			clearOurHintArrow();
			return;
		}
		// Niet de pijl van een andere plugin (bijv. Quest Helper) overschrijven.
		boolean arrowIsOurs = ourHintArrow != null && ourHintArrow.equals(client.getHintArrowPoint());
		if (client.hasHintArrow() && !arrowIsOurs)
		{
			return;
		}
		WorldPoint target = nearestPoint;
		if (!target.equals(ourHintArrow))
		{
			client.setHintArrow(target);
			ourHintArrow = target;
		}
	}

	/** De gardener van deze patch als hij in beeld is, anders null. */
	private NPC findGardener(FarmPlan.Step step)
	{
		String name = step == null ? null : step.getPatch().getGardener();
		if (name == null)
		{
			return null;
		}
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && name.equalsIgnoreCase(npc.getName()))
			{
				return npc;
			}
		}
		return null;
	}

	private void clearOurHintArrow()
	{
		if (ourHintArrow != null && client.hasHintArrow() && ourHintArrow.equals(client.getHintArrowPoint()))
		{
			client.clearHintArrow();
		}
		ourHintArrow = null;
	}

	// ------------------------------------------------------------------ teleport-keuze highlighten

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		FarmPlan.Step step = getCurrentStep();
		if (awaitingPayment && step != null && highlightPayOption(step, event.getMenuEntry()))
		{
			return;
		}
		if (!config.highlightTeleport() || step == null || step.getTeleportOption() == null)
		{
			return;
		}
		TeleportOption t = step.getTeleportOption();
		MenuEntry entry = event.getMenuEntry();
		String option = Text.removeTags(entry.getOption());
		String target = Text.removeTags(entry.getTarget());

		Color color = config.highlightColor();
		if (t.getKind() == TeleportOption.Kind.SPELL)
		{
			if (option.equalsIgnoreCase("Cast") && target.equalsIgnoreCase(t.getSpellName()))
			{
				entry.setOption(ColorUtil.wrapWithColorTag(option, color));
			}
			return;
		}

		// Keuze in een teleport-venster (bijv. "Xeric's Glade", "Burthorpe") als menu-optie of -target.
		for (String wanted : t.getDialogOptions())
		{
			if (matchesChoice(option, wanted))
			{
				entry.setOption(ColorUtil.wrapWithColorTag(option, color));
				return;
			}
			if (matchesChoice(target, wanted))
			{
				entry.setTarget(ColorUtil.wrapWithColorTag(target, color));
				return;
			}
		}

		// Rechtermuisklik op het teleport-item zelf (elke lading, bijv. Games necklace(1) t/m (8)).
		boolean isTeleportItem = t.getKind() == TeleportOption.Kind.FREE
			? t.getMenuTarget() != null && target.equalsIgnoreCase(t.getMenuTarget())
			: matchesItem(t, target);
		if (!isTeleportItem)
		{
			return;
		}
		String lower = option.toLowerCase(Locale.ROOT);
		for (String wanted : t.getMenuOptions())
		{
			if (lower.contains(wanted.toLowerCase(Locale.ROOT)))
			{
				entry.setOption(ColorUtil.wrapWithColorTag(option, color));
				return;
			}
		}
	}

	/** Kleurt "Pay" op de gardener van deze patch. */
	private boolean highlightPayOption(FarmPlan.Step step, MenuEntry entry)
	{
		String gardener = step.getPatch().getGardener();
		if (gardener == null || !config.highlightTeleport())
		{
			return false;
		}
		String option = Text.removeTags(entry.getOption());
		String target = Text.removeTags(entry.getTarget()).toLowerCase(Locale.ROOT);
		if (option.toLowerCase(Locale.ROOT).startsWith("pay") && target.contains(gardener.toLowerCase(Locale.ROOT)))
		{
			entry.setOption(ColorUtil.wrapWithColorTag(option, config.highlightColor()));
			return true;
		}
		return false;
	}

	/** Hoort deze itemnaam bij de teleport (patronen als "Games necklace(" matchen elke lading)? */
	private static boolean matchesItem(TeleportOption t, String itemName)
	{
		String name = itemName.toLowerCase(Locale.ROOT).trim();
		for (String pattern : t.getItemPatterns())
		{
			String p = pattern.toLowerCase(Locale.ROOT);
			boolean prefix = p.endsWith("(") || p.endsWith("*");
			if (p.endsWith("*"))
			{
				p = p.substring(0, p.length() - 1);
			}
			if (prefix ? name.startsWith(p) : name.equals(p))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Komt een tekst uit een venster overeen met de gewenste keuze?
	 * Werkt ook met nummers of kleine toevoegingen: "2: Xeric's Glade", "2 Xeric's Glade", "Burthorpe."
	 */
	static boolean matchesChoice(String text, String wanted)
	{
		if (text == null)
		{
			return false;
		}
		String clean = Text.removeTags(text).trim().toLowerCase(Locale.ROOT);
		String w = wanted.toLowerCase(Locale.ROOT);
		return clean.contains(w) && clean.length() <= w.length() + 8;
	}

	/** Is dit item het teleport-item van de huidige stap (elke lading)? */
	boolean isCurrentTeleportItem(int itemId)
	{
		FarmPlan.Step step = getCurrentStep();
		if (step == null || step.getTeleportOption() == null
			|| step.getTeleportOption().getKind() != TeleportOption.Kind.ITEM)
		{
			return false;
		}
		return matchesItem(step.getTeleportOption(), itemManager.getItemComposition(itemId).getName());
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		// Dialoog/venster-opties zoeken, ongeveer 4x per seconde.
		if (++tickCounter % 10 != 0)
		{
			return;
		}
		FarmPlan.Step step = getCurrentStep();
		if (!config.highlightTeleport() || step == null || step.getTeleportOption() == null)
		{
			highlightedWidgets = Collections.emptyList();
			return;
		}
		TeleportOption t = step.getTeleportOption();
		if (t.getDialogOptions().isEmpty() && t.getSpellName() == null)
		{
			highlightedWidgets = Collections.emptyList();
			return;
		}
		List<Widget> found = new ArrayList<>();
		Widget[] roots = client.getWidgetRoots();
		if (roots != null)
		{
			for (Widget root : roots)
			{
				findWidgets(root, t, found, 0);
			}
		}
		highlightedWidgets = found;
	}

	private void findWidgets(Widget w, TeleportOption t, List<Widget> found, int depth)
	{
		if (w == null || w.isHidden() || depth > 25)
		{
			return;
		}
		// Keuzes staan soms in de tekst ("1: Tree Gnome Village") en soms in de naam van een knop.
		for (String wanted : t.getDialogOptions())
		{
			if (matchesChoice(w.getText(), wanted) || matchesChoice(w.getName(), wanted))
			{
				found.add(w);
				break;
			}
		}
		if (t.getSpellName() != null)
		{
			String name = w.getName();
			if (name != null && Text.removeTags(name).equalsIgnoreCase(t.getSpellName()))
			{
				found.add(w);
			}
		}
		findAll(w.getStaticChildren(), t, found, depth);
		findAll(w.getDynamicChildren(), t, found, depth);
		findAll(w.getNestedChildren(), t, found, depth);
	}

	private void findAll(Widget[] children, TeleportOption t, List<Widget> found, int depth)
	{
		if (children == null)
		{
			return;
		}
		for (Widget c : children)
		{
			findWidgets(c, t, found, depth + 1);
		}
	}

	List<Widget> getHighlightedWidgets()
	{
		return highlightedWidgets;
	}

	// ------------------------------------------------------------------ bank highlight

	/** Items die in de bank gemarkeerd worden: alles van het plan. */
	Set<Integer> getBankHighlightIds()
	{
		if (saplingTabActive && saplingPlan != null)
		{
			return saplingMissing;
		}
		FarmPlan current = plan;
		// Alleen wat je nog niet (genoeg) bij je hebt.
		return current == null ? Collections.emptySet() : missingIds;
	}

	/** Items die in je inventory gemarkeerd worden: teleport en zaad van de huidige stap. */
	Set<Integer> getInventoryHighlightIds()
	{
		FarmPlan.Step step = getCurrentStep();
		if (step == null)
		{
			return Collections.emptySet();
		}
		Set<Integer> ids = new HashSet<>();
		if (step.getTeleportItemId() > 0)
		{
			ids.add(step.getTeleportItemId());
		}
		if (step.getSeedItemId() > 0)
		{
			ids.add(step.getSeedItemId());
		}
		return ids;
	}

	FarmPlannerConfig getConfig()
	{
		return config;
	}

	private static BufferedImage createIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(110, 70, 35));
		g.fillRect(7, 9, 2, 6);
		g.setColor(new Color(60, 170, 70));
		g.fillOval(2, 1, 12, 10);
		g.setColor(new Color(120, 220, 110));
		g.fillOval(4, 3, 5, 4);
		g.dispose();
		return img;
	}
}
