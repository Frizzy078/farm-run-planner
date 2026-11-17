package com.farmplanner;

import java.util.Set;
import net.runelite.api.gameval.ObjectID;

/**
 * Object-ID's van de farming patches.
 */
final class PatchIds
{
	private PatchIds()
	{
	}

	static final Set<Integer> HERB = Set.of(
		ObjectID.FARMING_HERB_PATCH_1,
		ObjectID.FARMING_HERB_PATCH_2,
		ObjectID.FARMING_HERB_PATCH_3,
		ObjectID.FARMING_HERB_PATCH_4,
		ObjectID.FARMING_HERB_PATCH_5,
		ObjectID.MYARM_HERBPATCH,       // Trollheim
		ObjectID.FARMING_HERB_PATCH_6,  // Hosidius
		ObjectID.FARMING_HERB_PATCH_7,  // Farming Guild
		ObjectID.FARMING_HERB_PATCH_8
	);

	static final Set<Integer> TREE = Set.of(
		ObjectID.FARMING_TREE_PATCH_1,
		ObjectID.FARMING_TREE_PATCH_2,
		ObjectID.FARMING_TREE_PATCH_3,
		ObjectID.FARMING_TREE_PATCH_4,
		ObjectID.FARMING_TREE_PATCH_5,  // Lumbridge
		ObjectID.FARMING_TREE_PATCH_6,  // Farming Guild
		ObjectID.FARMING_TREE_PATCH_7
	);

	static final Set<Integer> FRUIT_TREE = Set.of(
		ObjectID.FARMING_FRUIT_TREE_PATCH_1,
		ObjectID.FARMING_FRUIT_TREE_PATCH_2,
		ObjectID.FARMING_FRUIT_TREE_PATCH_3,
		ObjectID.FARMING_FRUIT_TREE_PATCH_4,
		ObjectID.FARMING_FRUIT_TREE_PATCH_5,  // Lletya
		ObjectID.FARMING_FRUIT_TREE_PATCH_6,  // Farming Guild
		ObjectID.FARMING_FRUIT_TREE_PATCH_7
	);

	static Set<Integer> forRun(RunType type)
	{
		switch (type)
		{
			case HERB:
				return HERB;
			case TREE:
				return TREE;
			default:
				return FRUIT_TREE;
		}
	}

	static boolean isAnyPatch(int id)
	{
		return HERB.contains(id) || TREE.contains(id) || FRUIT_TREE.contains(id);
	}
}
