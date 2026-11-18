package com.farmplanner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Maakt een farm run op basis van wat je hebt en je levels.
 * Puur Java (geen RuneLite-afhankelijkheden), zodat het los te testen is.
 */
public final class PlanBuilder
{
	private static final String[] COMPOST_BEST_FIRST = {"Ultracompost", "Supercompost", "Compost"};
	private static final String BOTTOMLESS = "Bottomless compost bucket";
	static final int BRIMHAVEN_BOAT_COINS = 30;

	private PlanBuilder()
	{
	}

	private static final class Chosen
	{
		final TeleportOption option;
		final OwnedItems.Entry item;
		final Language lang;

		Chosen(TeleportOption option, OwnedItems.Entry item, Language lang)
		{
			this.lang = lang;
			this.option = option;
			this.item = item;
		}

		String describe()
		{
			return item != null ? option.getLabel(lang) + " [" + item.getName() + "]" : option.getLabel(lang);
		}
	}

	public static FarmPlan build(RunType type, OwnedItems owned, int farmingLevel, int magicLevel, PlanOptions options)
	{
		Language lang = options.getLanguage();
		FarmPlan plan = new FarmPlan(type, farmingLevel, lang);
		OwnedItems left = owned.copy();

		// 1. Zaden: hoogste level eerst, die je kunt planten en hebt.
		Deque<OwnedItems.Entry> seedQueue = new ArrayDeque<>();
		for (Seed seed : Seed.forRun(type))
		{
			OwnedItems.Entry e = left.get(seed.getItemName());
			if (e != null && e.getQuantity() > 0 && farmingLevel >= seed.getLevel())
			{
				seedQueue.add(e);
			}
		}
		if (seedQueue.isEmpty())
		{
			plan.warn(lang.t(
				"No " + (type == RunType.HERB ? "seeds" : "saplings") + " found that you can plant with "
					+ farmingLevel + " Farming.",
				"Geen " + (type == RunType.HERB ? "zaden" : "saplings")
					+ " gevonden die je met " + farmingLevel + " Farming kunt planten."));
		}

		boolean bottomless = left.count(BOTTOMLESS) > 0;

		// 2. Route langs de patches.
		for (Patch patch : Patch.forRun(type))
		{
			if (!patch.isEnabled(options) || farmingLevel < patch.getFarmingLevel())
			{
				continue;
			}

			OwnedItems.Entry seed = takeSeed(seedQueue, left, plan);
			if (seed == null)
			{
				plan.addStep(new FarmPlan.Step(patch, null, null, null));
				continue;
			}

			Chosen teleport = chooseTeleport(patch, left, magicLevel, options, plan);
			if (teleport == null)
			{
				plan.warn(lang.t("No teleport found for " + patch.getLocation() + " - you will have to walk there.",
					"Geen teleport gevonden voor " + patch.getLocation() + " - je moet er naartoe lopen."));
			}

			String compost = takeCompost(left, bottomless, plan);
			FarmPlan.Step step = new FarmPlan.Step(patch, teleport == null ? null : teleport.describe(), seed.getName(), compost)
				.withSeedId(seed.getId());
			if (teleport != null)
			{
				step.withTeleport(teleport.option, teleport.item);
			}

			Seed seedDef = Seed.byItemName(seed.getName());
			if (seedDef != null)
			{
				plan.addXp(seedDef.getXpPlant() + compostXp(compost), seedDef.getXpLater());
				if (options.isPayProtection())
				{
					takePayment(seedDef, patch, left, plan, step);
				}
			}
			plan.addStep(step);
		}

		addSeedOverview(type, owned, farmingLevel, plan);
		if (type != RunType.HERB)
		{
			addSaplingAdvice(type, owned, farmingLevel, plan);
		}

		if (plan.getPlantedCount() == 0)
		{
			return plan;
		}

		// 3. Gereedschap.
		addTools(type, owned, plan);

		// 4. Coins.
		addCoins(type, owned, plan, options);

		if (type != RunType.HERB)
		{
			plan.tip(lang.t("Give the payment to the gardener at the patch (Pay) so your tree can't die. "
				+ "Noted items are accepted too.",
				"Geef de betaling aan de gardener bij de patch (Pay) - dan kan je boom niet doodgaan. "
				+ "Genoteerde items (noted) worden ook geaccepteerd."));
		}
		else
		{
			plan.tip(lang.t("Don't forget to harvest your herbs before planting again.",
				"Vergeet niet je herbs te oogsten voordat je opnieuw plant."));
		}
		plan.tip(lang.t("Seeds in the Farming Guild seed vault are not counted.",
			"Zaden in de Seed vault van de Farming Guild worden niet meegeteld."));
		return plan;
	}

	static double compostXp(String compost)
	{
		if (compost == null)
		{
			return 0;
		}
		switch (compost)
		{
			case "Compost":
				return 18;
			case "Supercompost":
				return 26;
			default:
				return 36; // Ultracompost / Bottomless compost bucket (meestal ultra)
		}
	}

	private static void takePayment(Seed seed, Patch patch, OwnedItems left, FarmPlan plan, FarmPlan.Step step)
	{
		if (seed.getPaymentItem() == null)
		{
			return;
		}
		OwnedItems.Entry pay = left.get(seed.getPaymentItem());
		if (pay != null && pay.getQuantity() >= seed.getPaymentQty())
		{
			left.use(pay.getName(), seed.getPaymentQty());
			plan.withdraw(pay, seed.getPaymentQty());
			step.setPayment(seed.getPaymentLabel(plan.getLanguage()));
		}
		else
		{
			Language lang = plan.getLanguage();
			String tree = seed.getItemName().replace(" sapling", "");
			String label = seed.getPaymentLabel(lang) + " (" + seed.getPaymentItem() + ")";
			plan.warn(lang.t("No protection for " + tree + " in " + patch.getLocation() + ": you are missing " + label + ".",
				"Geen bescherming voor " + tree + " in " + patch.getLocation() + ": je mist " + label + "."));
		}
	}

	private static String treeName(Seed s)
	{
		return s.getItemName().replace(" sapling", "");
	}

	/** Overzicht van je zaden/saplings: hoeveel runs je ermee kunt. */
	private static void addSeedOverview(RunType type, OwnedItems owned, int farmingLevel, FarmPlan plan)
	{
		List<String> out = plan.getSeedOverview();
		Language lang = plan.getLanguage();
		int patches = plan.getSteps().size();
		int usable = 0;
		Seed bestUsed = null;
		for (Seed s : Seed.forRun(type))
		{
			int n = owned.count(s.getItemName());
			if (n <= 0)
			{
				continue;
			}
			if (farmingLevel < s.getLevel())
			{
				out.add(n + " x " + s.getItemName() + lang.t(" - can't plant yet (level ", " - nog niet te planten (level ")
					+ s.getLevel() + ")");
				continue;
			}
			usable += n;
			if (bestUsed == null)
			{
				bestUsed = s;
			}
			String runs = patches > 0
				? lang.t(" - enough for " + (n / patches) + " full run(s)", " - genoeg voor " + (n / patches) + " volle run(s)")
				: "";
			out.add(n + " x " + s.getItemName() + " (" + lang.number(s.getXpTotal()) + " XP per patch)" + runs);
		}

		if (type != RunType.HERB)
		{
			for (Seed s : Seed.forRun(type))
			{
				int seeds = owned.count(s.getTreeSeedName());
				int seedlings = owned.count(s.getSeedlingName()) + owned.count(s.getSeedlingName() + " (w)");
				if (seeds > 0)
				{
					out.add(seeds + " x " + s.getTreeSeedName()
						+ lang.t(" - not a sapling yet (see Making saplings)", " - nog geen sapling (zie Saplings maken)"));
				}
				if (seedlings > 0)
				{
					out.add(seedlings + " x " + s.getSeedlingName()
						+ lang.t(" - still growing (water it if it is dry)", " - groeit nog (geef water als hij droog is)"));
				}
				// Betere zaden die nog geen sapling zijn: tip om die eerst te maken
				if (seeds > 0 && farmingLevel >= s.getLevel() && (bestUsed == null || s.getLevel() > bestUsed.getLevel()))
				{
					plan.tip(lang.t(
						"Make saplings from your " + s.getTreeSeedName() + "s first - " + treeName(s)
							+ " gives more XP than " + (bestUsed == null ? "planting nothing" : treeName(bestUsed)) + ".",
						"Maak eerst saplings van je " + s.getTreeSeedName() + "s - " + treeName(s)
							+ " geeft meer XP dan " + (bestUsed == null ? "niets planten" : treeName(bestUsed)) + "."));
				}
			}
		}

		if (patches > 0 && usable > 0 && usable < patches)
		{
			out.add(lang.t("You have " + usable + " to plant for " + patches + " patches.",
				"Je hebt " + usable + " te planten voor " + patches + " patches."));
		}
		if (usable > 0)
		{
			out.add(lang.t("The planner plants the highest (most XP) first; the rest stays in your bank for the next run.",
				"De planner plant de hoogste (meeste XP) eerst; de rest blijft in je bank voor de volgende run."));
		}
	}

	/** Uitleg en advies om saplings te maken van boomzaden. */
	private static void addSaplingAdvice(RunType type, OwnedItems owned, int farmingLevel, FarmPlan plan)
	{
		List<String> out = plan.getSaplingAdvice();
		Language lang = plan.getLanguage();
		String treeWord = type == RunType.TREE ? lang.t("tree", "boom") : lang.t("fruit tree", "fruitboom");

		Seed best = null;
		for (Seed s : Seed.forRun(type))
		{
			if (farmingLevel >= s.getLevel())
			{
				best = s;
				break;
			}
		}
		if (best == null)
		{
			out.add(lang.t("You don't have enough Farming for a " + treeWord + " yet.",
				"Je hebt nog niet genoeg Farming voor een " + treeWord + "."));
			return;
		}
		String bestInfo = treeName(best) + " (level " + best.getLevel() + ") - " + lang.number(best.getXpTotal());
		out.add(lang.t(
			"Best " + treeWord + " for your level: " + bestInfo + " XP per tree ("
				+ lang.number(best.getXpPlant()) + " planting + " + lang.number(best.getXpLater()) + " check-health).",
			"Beste " + treeWord + " op jouw level: " + bestInfo + " XP per boom ("
				+ lang.number(best.getXpPlant()) + " planten + " + lang.number(best.getXpLater()) + " check-health)."));

		int totalSeeds = 0;
		for (Seed s : Seed.forRun(type))
		{
			int seeds = owned.count(s.getTreeSeedName());
			if (seeds <= 0 || farmingLevel < s.getLevel())
			{
				continue;
			}
			totalSeeds += seeds;
			String xpText = " (" + lang.number(seeds * s.getXpTotal()) + " XP).";
			out.add(lang.t(
				"You can make: " + seeds + " x " + s.getItemName() + " from your " + s.getTreeSeedName() + "s" + xpText,
				"Je kunt maken: " + seeds + " x " + s.getItemName() + " van je " + s.getTreeSeedName() + "s" + xpText));
		}
		if (owned.count(best.getTreeSeedName()) == 0 && owned.count(best.getItemName()) == 0)
		{
			out.add(lang.t("You have no " + best.getTreeSeedName() + "s. You can get them from bird nests, "
				+ "Master Farmers and the Grand Exchange, among others.",
				"Je hebt geen " + best.getTreeSeedName() + "s. Die krijg je van o.a. bird nests, "
				+ "Master Farmers en de Grand Exchange."));
		}

		if (totalSeeds > 0)
		{
			int pots = owned.count("Plant pot");
			boolean trowel = owned.count("Gardening trowel") > 0;
			boolean can = owned.find("Watering can(") != null || owned.count("Gricoller's can") > 0;
			String have = lang.t("(have)", "(heb je)");
			String missing = lang.t("(missing)", "(mist)");
			out.add(lang.t("Needed: " + totalSeeds + " filled Plant pots (you have " + pots + ")",
				"Nodig: " + totalSeeds + " gevulde Plant pots (je hebt er " + pots + ")")
				+ ", Gardening trowel " + (trowel ? have : missing)
				+ ", Watering can " + (can ? have : missing) + ".");
		}

		out.add(lang.t("How to make a sapling:", "Hoe maak je een sapling:"));
		out.add(lang.t("1. Fill an Empty plant pot with soil: use it on an empty patch with a Gardening trowel "
			+ "in your inventory (or buy filled Plant pots from a farming shop).",
			"1. Vul een Empty plant pot met aarde: gebruik hem op een lege patch met een Gardening trowel "
			+ "in je inventory (of koop gevulde Plant pots bij een farming-winkel)."));
		out.add(lang.t("2. Use the tree seed on the filled Plant pot (trowel in your inventory) -> seedling.",
			"2. Gebruik het boomzaad op de gevulde Plant pot (trowel in je inventory) -> seedling."));
		out.add(lang.t("3. Water the seedling with a Watering can.", "3. Geef de seedling water met een Watering can."));
		out.add(lang.t("4. After a few minutes (in your inventory or bank) it turns into a sapling.",
			"4. Na een paar minuten (in je inventory of bank) wordt het een sapling."));
	}

	/** Berekent hoeveel coins je nodig hebt en zet ze op de lijst. */
	static int coinsNeeded(RunType type, FarmPlan plan, PlanOptions options)
	{
		int coins = 0;
		for (FarmPlan.Step s : plan.getActiveSteps())
		{
			if (type != RunType.HERB)
			{
				coins += options.getCoinsPerTree();
			}
			if (s.getPatch() == Patch.BRIMHAVEN_FRUIT)
			{
				coins += BRIMHAVEN_BOAT_COINS;
			}
		}
		return coins;
	}

	private static void addCoins(RunType type, OwnedItems owned, FarmPlan plan, PlanOptions options)
	{
		int needed = coinsNeeded(type, plan, options);
		if (needed <= 0)
		{
			return;
		}
		OwnedItems.Entry coins = owned.get("Coins");
		int have = coins == null ? 0 : coins.getQuantity();
		if (have >= needed)
		{
			plan.withdraw(coins, needed);
		}
		else
		{
			if (coins != null)
			{
				plan.withdraw(coins, have);
			}
			plan.warn(plan.getLanguage().t("You need " + needed + " coins, but you only have " + have + ".",
				"Je hebt " + needed + " coins nodig, maar hebt er maar " + have + "."));
		}
		if (type != RunType.HERB)
		{
			plan.tip(options.getCoinsPerTree() + plan.getLanguage().t(" coins per tree so the gardener removes your old tree.",
				" coins per boom zodat de gardener je oude boom weghaalt."));
		}
	}

	private static OwnedItems.Entry takeSeed(Deque<OwnedItems.Entry> queue, OwnedItems left, FarmPlan plan)
	{
		while (!queue.isEmpty())
		{
			OwnedItems.Entry e = queue.peek();
			if (e.getQuantity() > 0)
			{
				left.use(e.getName(), 1);
				plan.withdraw(e, 1);
				return e;
			}
			queue.poll();
		}
		return null;
	}

	private static Chosen chooseTeleport(Patch patch, OwnedItems left, int magicLevel, PlanOptions options, FarmPlan plan)
	{
		for (TeleportOption opt : patch.getTeleports())
		{
			if (!opt.isEnabled(options))
			{
				continue;
			}
			switch (opt.getKind())
			{
				case FREE:
					return new Chosen(opt, null, options.getLanguage());

				case ITEM:
					for (String pattern : opt.getItemPatterns())
					{
						OwnedItems.Entry e = left.find(pattern);
						if (e != null && e.getQuantity() > 0)
						{
							if (opt.isConsumable())
							{
								left.use(e.getName(), 1);
								plan.withdraw(e, 1);
							}
							else
							{
								plan.withdrawOnce(e);
								plan.allowAnyCharge(e.getName(), pattern);
							}
							return new Chosen(opt, e, options.getLanguage());
						}
					}
					break;

				case SPELL:
					if (magicLevel >= opt.getMagicLevel() && hasRunes(left, opt.getRunes()))
					{
						for (Map.Entry<String, Integer> r : opt.getRunes().entrySet())
						{
							OwnedItems.Entry rune = left.get(r.getKey());
							left.use(r.getKey(), r.getValue());
							plan.withdraw(rune, r.getValue());
						}
						return new Chosen(opt, null, options.getLanguage());
					}
					break;
			}
		}
		return null;
	}

	private static boolean hasRunes(OwnedItems left, Map<String, Integer> runes)
	{
		for (Map.Entry<String, Integer> r : runes.entrySet())
		{
			if (left.count(r.getKey()) < r.getValue())
			{
				return false;
			}
		}
		return true;
	}

	private static String takeCompost(OwnedItems left, boolean bottomless, FarmPlan plan)
	{
		if (bottomless)
		{
			plan.withdrawOnce(left.get(BOTTOMLESS));
			return BOTTOMLESS;
		}
		for (String name : COMPOST_BEST_FIRST)
		{
			OwnedItems.Entry e = left.get(name);
			if (e != null && e.getQuantity() > 0)
			{
				left.use(name, 1);
				plan.withdraw(e, 1);
				return name;
			}
		}
		plan.warn(plan.getLanguage().t("Not enough compost for all patches.", "Niet genoeg compost voor alle patches."));
		return null;
	}

	private static void addTools(RunType type, OwnedItems owned, FarmPlan plan)
	{
		List<String> needed = type == RunType.HERB
			? List.of("Rake", "Seed dibber", "Spade")
			: List.of("Rake", "Spade");
		for (String tool : needed)
		{
			OwnedItems.Entry e = owned.get(tool);
			if (e != null)
			{
				plan.withdrawOnce(e);
			}
			else
			{
				plan.warn(tool + plan.getLanguage().t(" not found - maybe it is stored with the Tool Leprechaun.",
					" niet gevonden - misschien ligt hij bij de Tool Leprechaun."));
			}
		}
		if (type != RunType.TREE)
		{
			OwnedItems.Entry secateurs = owned.get("Magic secateurs");
			if (secateurs != null)
			{
				plan.withdrawOnce(secateurs);
			}
		}
	}
}
