package com.farmplanner;

import java.util.Set;

/**
 * Object-ID's van de farming patches (uit net.runelite.api.gameval.ObjectID).
 */
final class PatchIds
{
	private PatchIds()
	{
	}

	static final Set<Integer> HERB = Set.of(
		8150, 8151, 8152, 8153, // FARMING_HERB_PATCH_1-4 (Falador, Catherby, Ardougne, Port Phasmatys)
		9372,                   // FARMING_HERB_PATCH_5
		18816,                  // MYARM_HERBPATCH (Trollheim)
		27115,                  // FARMING_HERB_PATCH_6 (Hosidius)
		33979,                  // FARMING_HERB_PATCH_7 (Farming Guild)
		50697                   // FARMING_HERB_PATCH_8 (Varlamore)
	);

	static final Set<Integer> TREE = Set.of(
		8388, 8389, 8390, 8391, // FARMING_TREE_PATCH_1-4
		19147,                  // FARMING_TREE_PATCH_5 (Lumbridge)
		33732,                  // FARMING_TREE_PATCH_6 (Farming Guild)
		56953                   // FARMING_TREE_PATCH_7
	);

	static final Set<Integer> FRUIT_TREE = Set.of(
		7962, 7963, 7964, 7965, // FARMING_FRUIT_TREE_PATCH_1-4
		26579,                  // FARMING_FRUIT_TREE_PATCH_5 (Lletya)
		34007,                  // FARMING_FRUIT_TREE_PATCH_6 (Farming Guild)
		56955                   // FARMING_FRUIT_TREE_PATCH_7
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
