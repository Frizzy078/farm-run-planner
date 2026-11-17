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

		Chosen(TeleportOption option, OwnedItems.Entry item)
		{
			this.option = option;
			this.item = item;
		}

		String describe()
		{
			return item != null ? option.getLabel() + " [" + item.getName() + "]" : option.getLabel();
		}
	}

	public static FarmPlan build(RunType type, OwnedItems owned, int farmingLevel, int magicLevel, PlanOptions options)
	{
		FarmPlan plan = new FarmPlan(type, farmingLevel);
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
			plan.warn("Geen " + (type == RunType.HERB ? "zaden" : "saplings")
				+ " gevonden die je met " + farmingLevel + " Farming kunt planten.");
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
				plan.warn("Geen teleport gevonden voor " + patch.getLocation() + " - je moet er naartoe lopen.");
			}

			String compost = takeCompost(left, bottomless, plan);
			FarmPlan.Step step = new FarmPlan.Step(patch, teleport == null ? null : teleport.describe(), seed.getName(), compost)
				.withSeedId(seed.getId());
			if (teleport != null)
			{
				step.withTeleport(teleport.option, teleport.item);
			}
			plan.addStep(step);
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
			plan.tip("Betaal de gardener bij elke patch om je boom te beschermen tegen ziekte.");
		}
		else
		{
			plan.tip("Vergeet niet je herbs te oogsten voordat je opnieuw plant.");
		}
		plan.tip("Zaden in de Seed vault van de Farming Guild worden niet meegeteld.");
		return plan;
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
			plan.warn("Je hebt " + needed + " coins nodig, maar hebt er maar " + have + ".");
		}
		if (type != RunType.HERB)
		{
			plan.tip(options.getCoinsPerTree() + " coins per boom zodat de gardener je oude boom weghaalt.");
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
					return new Chosen(opt, null);

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
							}
							return new Chosen(opt, e);
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
						return new Chosen(opt, null);
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
		plan.warn("Niet genoeg compost voor alle patches.");
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
				plan.warn(tool + " niet gevonden - misschien ligt hij bij de Tool Leprechaun.");
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
