package com.farmplanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlanBuilderTest
{
	private static final PlanOptions NL = PlanOptions.defaults().withLanguage(Language.DUTCH);
	private static OwnedItems bank(Object... nameQty)
	{
		OwnedItems o = new OwnedItems();
		for (int i = 0; i < nameQty.length; i += 2)
		{
			o.add(1000 + i, (String) nameQty[i], (Integer) nameQty[i + 1]);
		}
		return o;
	}

	@Test
	public void herbRunUsesBestSeedsAndTeleports()
	{
		OwnedItems owned = bank(
			"Ranarr seed", 10, "Snapdragon seed", 2, "Torstol seed", 5,
			"Ultracompost", 3, "Supercompost", 10,
			"Ardougne cloak 3", 1, "Camelot teleport", 4, "Explorer's ring 2", 1,
			"Ectophial", 1, "Xeric's talisman", 1, "Skills necklace(4)", 1,
			"Rake", 1, "Seed dibber", 1, "Spade", 1);

		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 70, 60, NL);

		// 70 Farming: 6 patches (incl. Farming Guild), geen Torstol (85)
		assertEquals(6, plan.getPlantedCount());
		assertEquals("Snapdragon seed", plan.getSteps().get(0).getSeed());
		assertEquals("Snapdragon seed", plan.getSteps().get(1).getSeed());
		assertEquals("Ranarr seed", plan.getSteps().get(2).getSeed());
		assertEquals(Integer.valueOf(4), plan.getWithdraw().get("Ranarr seed"));
		assertNull(plan.getWithdraw().get("Torstol seed"));

		assertTrue(plan.getSteps().get(0).getTeleport().contains("Ardougne cloak 3"));
		assertTrue(plan.getSteps().get(1).getTeleport().contains("Camelot teleport"));
		assertEquals(Integer.valueOf(1), plan.getWithdraw().get("Camelot teleport"));

		// eerst 3 ultra, dan super
		assertEquals("Ultracompost", plan.getSteps().get(2).getCompost());
		assertEquals("Supercompost", plan.getSteps().get(3).getCompost());
		assertTrue(plan.getWarnings().isEmpty());
	}

	@Test
	public void fallsBackToSpellWhenNoItems()
	{
		OwnedItems owned = bank("Guam seed", 1, "Law rune", 10, "Air rune", 50, "Water rune", 50);
		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 20, 99, NL);
		assertEquals(1, plan.getPlantedCount());
		assertEquals("Ardougne Teleport (spreuk)", plan.getSteps().get(0).getTeleport());
		assertEquals(Integer.valueOf(2), plan.getWithdraw().get("Law rune"));
	}

	@Test
	public void noSeedsGivesWarning()
	{
		FarmPlan plan = PlanBuilder.build(RunType.TREE, bank("Spade", 1), 50, 50, NL);
		assertEquals(0, plan.getPlantedCount());
		assertTrue(plan.getWarnings().get(0).startsWith("Geen saplings"));
	}

	@Test
	public void treeRunTakesCoinsPerTree()
	{
		OwnedItems owned = bank("Yew sapling", 3, "Willow sapling", 5, "Coins", 5000, "Spade", 1, "Rake", 1);
		FarmPlan plan = PlanBuilder.build(RunType.TREE, owned, 60, 1, NL);
		// 60 Farming: Lumbridge, Varrock, Falador, Taverley, Gnome Stronghold (Guild pas op 65)
		assertEquals(5, plan.getPlantedCount());
		assertEquals(Integer.valueOf(1000), plan.getWithdraw().get("Coins"));
	}

	@Test
	public void fruitRunWarnsWhenNotEnoughCoins()
	{
		OwnedItems owned = bank("Apple sapling", 6, "Coins", 100, "Spade", 1, "Rake", 1);
		FarmPlan plan = PlanBuilder.build(RunType.FRUIT_TREE, owned, 30, 1, NL);
		// Stronghold, Village, Catherby, Brimhaven = 4 x 200 + 30 boot
		assertEquals(4, plan.getPlantedCount());
		assertEquals(Integer.valueOf(100), plan.getWithdraw().get("Coins"));
		assertTrue(plan.getWarnings().contains("Je hebt 830 coins nodig, maar hebt er maar 100."));
	}

	@Test
	public void herbRunNeedsNoCoins()
	{
		OwnedItems owned = bank("Guam seed", 2, "Coins", 500, "Ectophial", 1);
		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 10, 1, NL);
		assertNull(plan.getWithdraw().get("Coins"));
	}

	@Test
	public void stepRemembersTeleportChoice()
	{
		OwnedItems owned = bank("Ranarr seed", 10, "Skills necklace(3)", 1);
		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 70, 1, NL);
		FarmPlan.Step guild = plan.getActiveSteps().get(plan.getActiveSteps().size() - 1);
		assertEquals(Patch.GUILD_HERB, guild.getPatch());
		assertEquals("Skills necklace(3)", guild.getTeleportItem());
		assertEquals("Rub -> Farming Guild", guild.getTeleportOption().describeChoice());
	}

	@Test
	public void treeRunTakesProtectionPaymentAndCountsXp()
	{
		OwnedItems owned = bank("Yew sapling", 2, "Cactus spine", 15, "Spade", 1, "Rake", 1, "Coins", 1000);
		FarmPlan plan = PlanBuilder.build(RunType.TREE, owned, 60, 1, NL);
		assertEquals(2, plan.getPlantedCount());
		// 10 spines per Yew: eerste boom betaald, tweede niet (5 over)
		assertEquals(Integer.valueOf(10), plan.getWithdraw().get("Cactus spine"));
		assertEquals("10 cactus spines", plan.getActiveSteps().get(0).getPayment());
		assertEquals("Cactus spine", plan.getActiveSteps().get(0).getPaymentItem());
		assertEquals("Fayeth", plan.getActiveSteps().get(0).getPatch().getGardener());
		assertNull(plan.getActiveSteps().get(1).getPayment());
		assertTrue(plan.getWarnings().stream().anyMatch(w -> w.startsWith("Geen bescherming voor Yew")));
		// XP: 2 x 81 planten nu, 2 x 7069.9 check-health later
		assertEquals(162, plan.getXpNow(), 0.01);
		assertEquals(14139.8, plan.getXpLater(), 0.01);
	}

	@Test
	public void herbXpIncludesCompostAndEstimatedHarvest()
	{
		OwnedItems owned = bank("Ranarr seed", 1, "Ultracompost", 1, "Ectophial", 1);
		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 40, 1, NL);
		assertEquals(27 + 36, plan.getXpNow(), 0.01);
		assertEquals(30.5 * Seed.AVG_HERBS_PER_PATCH, plan.getXpLater(), 0.01);
	}

	@Test
	public void saplingAdviceSuggestsBestTreeAndCountsSeeds()
	{
		OwnedItems owned = bank("Maple sapling", 5, "Yew seed", 3, "Plant pot", 1, "Gardening trowel", 1,
			"Watering can(8)", 1);
		FarmPlan plan = PlanBuilder.build(RunType.TREE, owned, 62, 1, NL);
		assertTrue(plan.getSaplingAdvice().get(0).startsWith("Beste boom op jouw level: Yew"));
		assertTrue(plan.getSaplingAdvice().stream().anyMatch(a -> a.startsWith("Je kunt maken: 3 x Yew sapling")));
		assertTrue(plan.getSaplingAdvice().stream().anyMatch(a -> a.contains("je hebt er 1")
			&& a.contains("Gardening trowel (heb je)") && a.contains("Watering can (heb je)")));
		assertTrue(plan.getTips().stream().anyMatch(t -> t.startsWith("Maak eerst saplings van je Yew seeds")));
		assertTrue(plan.getSeedOverview().stream().anyMatch(s -> s.startsWith("3 x Yew seed")));
	}

	@Test
	public void noProtectionWhenDisabled()
	{
		OwnedItems owned = bank("Yew sapling", 1, "Cactus spine", 50);
		PlanOptions noPay = new PlanOptions(true, false, false, false, 200, false, Language.DUTCH);
		FarmPlan plan = PlanBuilder.build(RunType.TREE, owned, 60, 1, noPay);
		assertNull(plan.getWithdraw().get("Cactus spine"));
	}

	@Test
	public void carriedItemsAreCountedIncludingAnyJewelleryCharge()
	{
		OwnedItems owned = bank("Willow sapling", 10, "Games necklace(8)", 2, "Spade", 1, "Rake", 1, "Coins", 5000,
			"Apples(5)", 10);
		FarmPlan plan = PlanBuilder.build(RunType.TREE, owned, 30, 1, NL);
		assertEquals(Integer.valueOf(1), plan.getWithdraw().get("Games necklace(8)"));

		java.util.Map<String, Integer> carried = new java.util.HashMap<>();
		carried.put("games necklace(5)", 1);   // andere lading telt ook
		carried.put("willow sapling", 3);      // 3 van de 5
		assertEquals(1, plan.countCarried("Games necklace(8)", carried));
		assertEquals(3, plan.countCarried("Willow sapling", carried));
		assertEquals(0, plan.countCarried("Spade", carried));

		java.util.Set<Integer> missing = plan.missingIds(carried);
		assertTrue(missing.contains(owned.get("Willow sapling").getId()));
		assertTrue(missing.contains(owned.get("Spade").getId()));
		assertTrue(!missing.contains(owned.get("Games necklace(8)").getId()));
	}

	@Test
	public void prefixMatchFindsChargedJewellery()
	{
		OwnedItems owned = bank("Skills necklace", 1, "Skills necklace(2)", 1);
		assertEquals("Skills necklace(2)", owned.find("Skills necklace(").getName());
	}

	@Test
	public void englishPlanUsesEnglishTexts()
	{
		OwnedItems owned = bank("Yew sapling", 2, "Cactus spine", 15, "Coins", 100, "Guam seed", 1,
			"Law rune", 10, "Air rune", 50, "Water rune", 50);
		FarmPlan tree = PlanBuilder.build(RunType.TREE, owned, 60, 1, PlanOptions.defaults());
		assertEquals(Language.ENGLISH, tree.getLanguage());
		assertTrue(tree.getWarnings().stream().anyMatch(w -> w.startsWith("No protection for Yew")));
		assertTrue(tree.getWarnings().contains("You need 400 coins, but you only have 100."));
		assertEquals("Best tree for your level: Yew (level 60) - 7,151 XP per tree (81 planting + 7,070 check-health).",
			tree.getSaplingAdvice().get(0));

		FarmPlan herb = PlanBuilder.build(RunType.HERB, owned, 20, 99, PlanOptions.defaults());
		assertEquals("Ardougne Teleport (spell)", herb.getSteps().get(0).getTeleport());
		assertEquals("Patch north of Ardougne.", herb.getSteps().get(0).getPatch().getTip(Language.ENGLISH));
	}

	@Test
	public void everyTreePatchHasAGardener()
	{
		for (Patch p : Patch.values())
		{
			if (p.getRunType() != RunType.HERB)
			{
				assertTrue(p + " heeft geen gardener", p.getGardener() != null);
			}
			else
			{
				assertNull(p.getGardener());
			}
		}
	}
}
