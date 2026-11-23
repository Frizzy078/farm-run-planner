package com.farmplanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Plan om saplings te maken van boomzaden: wat je uit de bank haalt, hoeveel XP het oplevert
 * en hoe ver je bent. Puur Java, zodat het los te testen is.
 */
public final class SaplingPlan
{
	/** Plekken in je inventory. */
	static final int INVENTORY_SLOTS = 28;
	static final String FILLED_POT = "Plant pot";
	static final String EMPTY_POT = "Empty plant pot";
	static final String TROWEL = "Gardening trowel";
	static final String CAN_PREFIX = "Watering can(";
	static final String GRICOLLER = "Gricoller's can";

	/** Wat je met de zaden van één soort gaat doen. */
	public static final class Batch
	{
		private final Seed seed;
		private final int amount;

		Batch(Seed seed, int amount)
		{
			this.seed = seed;
			this.amount = amount;
		}

		public Seed getSeed()
		{
			return seed;
		}

		public int getAmount()
		{
			return amount;
		}
	}

	private final RunType runType;
	private final Language language;
	private final List<Batch> batches = new ArrayList<>();
	private final Map<String, Integer> withdraw = new LinkedHashMap<>();
	private final Map<String, Integer> withdrawIds = new HashMap<>();
	private final Map<String, String> anyChargePrefix = new HashMap<>();
	private final List<String> warnings = new ArrayList<>();
	private final List<String> info = new ArrayList<>();
	private double xp;

	private SaplingPlan(RunType runType, Language language)
	{
		this.runType = runType;
		this.language = language;
	}

	/**
	 * Maakt een plan voor één inventory vol: beste zaden eerst, één pot per zaad.
	 */
	public static SaplingPlan build(RunType type, OwnedItems owned, int farmingLevel, Language lang)
	{
		SaplingPlan plan = new SaplingPlan(type, lang);

		// Gereedschap: trowel en een (gevulde) gieter.
		OwnedItems.Entry trowel = owned.get(TROWEL);
		OwnedItems.Entry can = owned.get(GRICOLLER);
		if (can == null)
		{
			can = owned.find(CAN_PREFIX);
			if (can != null)
			{
				plan.anyChargePrefix.put(can.getName(), CAN_PREFIX);
			}
		}
		int freeSlots = INVENTORY_SLOTS - 2;

		// Zaden: hoogste level eerst, zolang er plek is (1 vak per soort + 1 per pot).
		for (Seed s : Seed.forRun(type))
		{
			OwnedItems.Entry seeds = owned.get(s.getTreeSeedName());
			if (seeds == null || seeds.getQuantity() <= 0)
			{
				continue;
			}
			if (farmingLevel < s.getLevel())
			{
				plan.info.add(lang.t(
					seeds.getQuantity() + " x " + s.getTreeSeedName() + " - needs level " + s.getLevel(),
					seeds.getQuantity() + " x " + s.getTreeSeedName() + " - level " + s.getLevel() + " nodig"));
				continue;
			}
			int room = freeSlots - 1;
			if (room <= 0)
			{
				plan.info.add(lang.t(
					seeds.getQuantity() + " x " + s.getTreeSeedName() + " - next inventory",
					seeds.getQuantity() + " x " + s.getTreeSeedName() + " - volgende inventory"));
				continue;
			}
			int amount = Math.min(seeds.getQuantity(), room);
			freeSlots -= amount + 1;
			plan.batches.add(new Batch(s, amount));
			plan.add(seeds, amount);
			plan.xp += amount * s.getXpTotal();
			if (amount < seeds.getQuantity())
			{
				plan.info.add(lang.t(
					(seeds.getQuantity() - amount) + " x " + s.getTreeSeedName() + " left for a next inventory",
					(seeds.getQuantity() - amount) + " x " + s.getTreeSeedName() + " over voor een volgende inventory"));
			}
		}

		if (plan.batches.isEmpty())
		{
			plan.warnings.add(lang.t(
				"No " + (type == RunType.TREE ? "tree" : "fruit tree") + " seeds found that you can use with "
					+ farmingLevel + " Farming.",
				"Geen " + (type == RunType.TREE ? "boomzaden" : "fruitboomzaden") + " gevonden die je met "
					+ farmingLevel + " Farming kunt gebruiken."));
			return plan;
		}

		// Potten: eerst gevulde, dan lege (die moet je nog vullen).
		int pots = plan.getTotalSeeds();
		OwnedItems.Entry filled = owned.get(FILLED_POT);
		int fromFilled = filled == null ? 0 : Math.min(filled.getQuantity(), pots);
		if (fromFilled > 0)
		{
			plan.add(filled, fromFilled);
		}
		int stillNeeded = pots - fromFilled;
		if (stillNeeded > 0)
		{
			OwnedItems.Entry empty = owned.get(EMPTY_POT);
			int fromEmpty = empty == null ? 0 : Math.min(empty.getQuantity(), stillNeeded);
			if (fromEmpty > 0)
			{
				plan.add(empty, fromEmpty);
				plan.warnings.add(lang.t(
					fromEmpty + " pots are still empty: fill them first by using them on an empty (weeded) patch "
						+ "with a Gardening trowel in your inventory.",
					fromEmpty + " potten zijn nog leeg: vul ze eerst door ze op een lege (gewiede) patch te gebruiken "
						+ "met een Gardening trowel in je inventory."));
			}
			if (fromEmpty < stillNeeded)
			{
				plan.warnings.add(lang.t(
					"You are " + (stillNeeded - fromEmpty) + " plant pots short. Buy them at a farming shop "
						+ "(e.g. Sarah's farming shop south of Falador).",
					"Je komt " + (stillNeeded - fromEmpty) + " plant pots tekort. Koop ze bij een farming-winkel "
						+ "(bijv. de farming-winkel van Sarah ten zuiden van Falador)."));
			}
		}

		if (trowel != null)
		{
			plan.add(trowel, 1);
		}
		else
		{
			plan.warnings.add(lang.t("You have no Gardening trowel - you need it to plant the seeds.",
				"Je hebt geen Gardening trowel - die heb je nodig om de zaden te planten."));
		}
		if (can != null)
		{
			plan.add(can, 1);
		}
		else
		{
			plan.warnings.add(lang.t(
				"You have no filled Watering can. Fill one at a water source, or use a Gricoller's can.",
				"Je hebt geen gevulde Watering can. Vul er een bij water, of gebruik een Gricoller's can."));
		}
		return plan;
	}

	private void add(OwnedItems.Entry item, int amount)
	{
		withdraw.merge(item.getName(), amount, Integer::sum);
		withdrawIds.put(item.getName(), item.getId());
	}

	public RunType getRunType()
	{
		return runType;
	}

	public Language getLanguage()
	{
		return language;
	}

	public List<Batch> getBatches()
	{
		return batches;
	}

	public int getTotalSeeds()
	{
		int n = 0;
		for (Batch b : batches)
		{
			n += b.amount;
		}
		return n;
	}

	/** XP als je al deze saplings plant en later check-health doet. */
	public double getXp()
	{
		return xp;
	}

	public Map<String, Integer> getWithdraw()
	{
		return withdraw;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}

	public List<String> getInfo()
	{
		return info;
	}

	/** Hoeveel heb je van dit meeneem-item bij je (gieter telt met elke vulling). */
	public int countCarried(String itemName, Map<String, Integer> carried)
	{
		String prefix = anyChargePrefix.get(itemName);
		if (prefix == null)
		{
			return carried.getOrDefault(itemName.toLowerCase(Locale.ROOT), 0);
		}
		String p = prefix.toLowerCase(Locale.ROOT);
		int total = 0;
		for (Map.Entry<String, Integer> e : carried.entrySet())
		{
			if (e.getKey().startsWith(p))
			{
				total += e.getValue();
			}
		}
		return total;
	}

	/** Item-ID's die je nog niet (genoeg) bij je hebt: die worden in de bank gemarkeerd. */
	public Set<Integer> missingIds(Map<String, Integer> carried)
	{
		Set<Integer> result = new HashSet<>();
		for (Map.Entry<String, Integer> e : withdraw.entrySet())
		{
			if (countCarried(e.getKey(), carried) < e.getValue())
			{
				Integer id = withdrawIds.get(e.getKey());
				if (id != null)
				{
					result.add(id);
				}
			}
		}
		return result;
	}

	/** Wat is nu de volgende actie? */
	public enum Action
	{
		/** Nog spullen uit de bank halen. */
		WITHDRAW,
		/** Zaad op gevulde pot gebruiken. */
		PLANT,
		/** Seedlings water geven. */
		WATER,
		/** Wachten tot seedlings saplings worden. */
		WAIT,
		/** Alles is sapling. */
		DONE
	}

	/** Voortgang op basis van wat je bij je hebt. */
	public static final class Progress
	{
		public int seeds;
		public int pots;
		public int dry;
		public int growing;
		public int saplings;
		public Action next;
	}

	public Progress progress(Map<String, Integer> carried)
	{
		Progress p = new Progress();
		p.pots = carried.getOrDefault(FILLED_POT.toLowerCase(Locale.ROOT), 0);
		for (Batch b : batches)
		{
			Seed s = b.seed;
			p.seeds += carried.getOrDefault(s.getTreeSeedName().toLowerCase(Locale.ROOT), 0);
			p.dry += carried.getOrDefault(s.getSeedlingName().toLowerCase(Locale.ROOT), 0);
			p.growing += carried.getOrDefault((s.getSeedlingName() + " (w)").toLowerCase(Locale.ROOT), 0);
			p.saplings += carried.getOrDefault(s.getItemName().toLowerCase(Locale.ROOT), 0);
		}
		if (p.dry > 0)
		{
			p.next = Action.WATER;
		}
		else if (p.seeds > 0 && p.pots > 0)
		{
			p.next = Action.PLANT;
		}
		else if (p.growing > 0)
		{
			p.next = Action.WAIT;
		}
		else if (p.saplings > 0 && p.seeds == 0)
		{
			p.next = Action.DONE;
		}
		else
		{
			p.next = Action.WITHDRAW;
		}
		return p;
	}

	/**
	 * Moet dit item in je inventory oplichten? (de items voor de volgende actie)
	 *
	 * @param itemName naam van het item
	 */
	public boolean isNextActionItem(String itemName, Action next)
	{
		String n = itemName.toLowerCase(Locale.ROOT);
		boolean can = n.startsWith(CAN_PREFIX.toLowerCase(Locale.ROOT)) || n.equals(GRICOLLER.toLowerCase(Locale.ROOT));
		for (Batch b : batches)
		{
			Seed s = b.seed;
			if (next == Action.PLANT && (n.equals(s.getTreeSeedName().toLowerCase(Locale.ROOT))
				|| n.equals(FILLED_POT.toLowerCase(Locale.ROOT))))
			{
				return true;
			}
			if (next == Action.WATER && (can || n.equals(s.getSeedlingName().toLowerCase(Locale.ROOT))))
			{
				return true;
			}
		}
		return false;
	}
}
