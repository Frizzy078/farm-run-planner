package com.farmplanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlanBuilderTest
{
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

		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 70, 60, PlanOptions.defaults());

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
		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 20, 99, PlanOptions.defaults());
		assertEquals(1, plan.getPlantedCount());
		assertEquals("Ardougne Teleport (spreuk)", plan.getSteps().get(0).getTeleport());
		assertEquals(Integer.valueOf(2), plan.getWithdraw().get("Law rune"));
	}

	@Test
	public void noSeedsGivesWarning()
	{
		FarmPlan plan = PlanBuilder.build(RunType.TREE, bank("Spade", 1), 50, 50, PlanOptions.defaults());
		assertEquals(0, plan.getPlantedCount());
		assertTrue(plan.getWarnings().get(0).startsWith("Geen saplings"));
	}

	@Test
	public void treeRunTakesCoinsPerTree()
	{
		OwnedItems owned = bank("Yew sapling", 3, "Willow sapling", 5, "Coins", 5000, "Spade", 1, "Rake", 1);
		FarmPlan plan = PlanBuilder.build(RunType.TREE, owned, 60, 1, PlanOptions.defaults());
		// 60 Farming: Lumbridge, Varrock, Falador, Taverley, Gnome Stronghold (Guild pas op 65)
		assertEquals(5, plan.getPlantedCount());
		assertEquals(Integer.valueOf(1000), plan.getWithdraw().get("Coins"));
	}

	@Test
	public void fruitRunWarnsWhenNotEnoughCoins()
	{
		OwnedItems owned = bank("Apple sapling", 6, "Coins", 100, "Spade", 1, "Rake", 1);
		FarmPlan plan = PlanBuilder.build(RunType.FRUIT_TREE, owned, 30, 1, PlanOptions.defaults());
		// Stronghold, Village, Catherby, Brimhaven = 4 x 200 + 30 boot
		assertEquals(4, plan.getPlantedCount());
		assertEquals(Integer.valueOf(100), plan.getWithdraw().get("Coins"));
		assertTrue(plan.getWarnings().contains("Je hebt 830 coins nodig, maar hebt er maar 100."));
	}

	@Test
	public void herbRunNeedsNoCoins()
	{
		OwnedItems owned = bank("Guam seed", 2, "Coins", 500, "Ectophial", 1);
		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 10, 1, PlanOptions.defaults());
		assertNull(plan.getWithdraw().get("Coins"));
	}

	@Test
	public void stepRemembersTeleportChoice()
	{
		OwnedItems owned = bank("Ranarr seed", 10, "Skills necklace(3)", 1);
		FarmPlan plan = PlanBuilder.build(RunType.HERB, owned, 70, 1, PlanOptions.defaults());
		FarmPlan.Step guild = plan.getActiveSteps().get(plan.getActiveSteps().size() - 1);
		assertEquals(Patch.GUILD_HERB, guild.getPatch());
		assertEquals("Skills necklace(3)", guild.getTeleportItem());
		assertEquals("Rub -> Farming Guild", guild.getTeleportOption().describeChoice());
	}

	@Test
	public void prefixMatchFindsChargedJewellery()
	{
		OwnedItems owned = bank("Skills necklace", 1, "Skills necklace(2)", 1);
		assertEquals("Skills necklace(2)", owned.find("Skills necklace(").getName());
	}
}
