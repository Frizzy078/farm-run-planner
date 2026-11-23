package com.farmplanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SaplingPlanTest
{
	private static OwnedItems bank(Object... nameQty)
	{
		OwnedItems o = new OwnedItems();
		for (int i = 0; i < nameQty.length; i += 2)
		{
			o.add(2000 + i, (String) nameQty[i], (Integer) nameQty[i + 1]);
		}
		return o;
	}

	@Test
	public void bestSeedsFirstAndFitsOneInventory()
	{
		OwnedItems owned = bank("Yew seed", 5, "Maple seed", 40, "Magic seed", 3, "Plant pot", 100,
			"Gardening trowel", 1, "Watering can(8)", 1);
		SaplingPlan plan = SaplingPlan.build(RunType.TREE, owned, 60, Language.DUTCH);

		// Magic (75) kan niet, Yew eerst, dan Maple tot de inventory vol is:
		// 28 - trowel - gieter = 26; Yew: 1 vak zaad + 5 potten = 6; Maple: 1 vak zaad + 19 potten.
		assertEquals(2, plan.getBatches().size());
		assertEquals(Seed.YEW, plan.getBatches().get(0).getSeed());
		assertEquals(5, plan.getBatches().get(0).getAmount());
		assertEquals(19, plan.getBatches().get(1).getAmount());
		assertEquals(Integer.valueOf(24), plan.getWithdraw().get("Plant pot"));
		assertEquals(Integer.valueOf(1), plan.getWithdraw().get("Watering can(8)"));
		assertTrue(plan.getInfo().stream().anyMatch(i -> i.startsWith("3 x Magic seed")));
		assertTrue(plan.getInfo().stream().anyMatch(i -> i.startsWith("21 x Maple seed")));
		assertTrue(plan.getWarnings().isEmpty());
	}

	@Test
	public void warnsForEmptyPotsAndMissingTools()
	{
		OwnedItems owned = bank("Apple tree seed", 4, "Plant pot", 1, "Empty plant pot", 2);
		SaplingPlan plan = SaplingPlan.build(RunType.FRUIT_TREE, owned, 30, Language.ENGLISH);
		assertEquals(Integer.valueOf(1), plan.getWithdraw().get("Plant pot"));
		assertEquals(Integer.valueOf(2), plan.getWithdraw().get("Empty plant pot"));
		assertTrue(plan.getWarnings().stream().anyMatch(w -> w.startsWith("2 pots are still empty")));
		assertTrue(plan.getWarnings().stream().anyMatch(w -> w.startsWith("You are 1 plant pots short")));
		assertTrue(plan.getWarnings().stream().anyMatch(w -> w.contains("Gardening trowel")));
		assertTrue(plan.getWarnings().stream().anyMatch(w -> w.contains("Watering can")));
	}

	@Test
	public void progressFollowsInventory()
	{
		OwnedItems owned = bank("Willow seed", 3, "Plant pot", 3, "Gardening trowel", 1, "Watering can(8)", 1);
		SaplingPlan plan = SaplingPlan.build(RunType.TREE, owned, 30, Language.DUTCH);

		Map<String, Integer> inv = new HashMap<>();
		assertEquals(SaplingPlan.Action.WITHDRAW, plan.progress(inv).next);

		inv.put("willow seed", 3);
		inv.put("plant pot", 3);
		inv.put("watering can(5)", 1); // andere vulling telt ook
		assertEquals(SaplingPlan.Action.PLANT, plan.progress(inv).next);
		assertTrue(plan.missingIds(inv).contains(owned.get("Gardening trowel").getId()));
		assertFalse(plan.missingIds(inv).contains(owned.get("Watering can(8)").getId()));
		assertTrue(plan.isNextActionItem("Willow seed", SaplingPlan.Action.PLANT));
		assertFalse(plan.isNextActionItem("Watering can(5)", SaplingPlan.Action.PLANT));

		inv.put("willow seed", 2);
		inv.put("plant pot", 2);
		inv.put("willow seedling", 1);
		assertEquals(SaplingPlan.Action.WATER, plan.progress(inv).next);
		assertTrue(plan.isNextActionItem("Watering can(5)", SaplingPlan.Action.WATER));

		inv.clear();
		inv.put("willow seedling (w)", 3);
		assertEquals(SaplingPlan.Action.WAIT, plan.progress(inv).next);

		inv.clear();
		inv.put("willow sapling", 3);
		assertEquals(SaplingPlan.Action.DONE, plan.progress(inv).next);
		assertEquals(3, plan.progress(inv).saplings);
	}
}
