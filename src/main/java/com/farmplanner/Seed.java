package com.farmplanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Zaden en saplings die geplant kunnen worden, met het benodigde Farming level.
 * Namen zijn exact de in-game itemnamen.
 */
public enum Seed
{
	// Herbs
	GUAM(RunType.HERB, "Guam seed", 9),
	MARRENTILL(RunType.HERB, "Marrentill seed", 14),
	TARROMIN(RunType.HERB, "Tarromin seed", 19),
	HARRALANDER(RunType.HERB, "Harralander seed", 26),
	RANARR(RunType.HERB, "Ranarr seed", 32),
	TOADFLAX(RunType.HERB, "Toadflax seed", 38),
	IRIT(RunType.HERB, "Irit seed", 44),
	AVANTOE(RunType.HERB, "Avantoe seed", 50),
	KWUARM(RunType.HERB, "Kwuarm seed", 56),
	SNAPDRAGON(RunType.HERB, "Snapdragon seed", 62),
	CADANTINE(RunType.HERB, "Cadantine seed", 67),
	LANTADYME(RunType.HERB, "Lantadyme seed", 73),
	DWARF_WEED(RunType.HERB, "Dwarf weed seed", 79),
	TORSTOL(RunType.HERB, "Torstol seed", 85),

	// Bomen
	OAK(RunType.TREE, "Oak sapling", 15),
	WILLOW(RunType.TREE, "Willow sapling", 30),
	MAPLE(RunType.TREE, "Maple sapling", 45),
	YEW(RunType.TREE, "Yew sapling", 60),
	MAGIC(RunType.TREE, "Magic sapling", 75),

	// Fruitbomen
	APPLE(RunType.FRUIT_TREE, "Apple sapling", 27),
	BANANA(RunType.FRUIT_TREE, "Banana sapling", 33),
	ORANGE(RunType.FRUIT_TREE, "Orange sapling", 39),
	CURRY(RunType.FRUIT_TREE, "Curry sapling", 42),
	PINEAPPLE(RunType.FRUIT_TREE, "Pineapple sapling", 51),
	PAPAYA(RunType.FRUIT_TREE, "Papaya sapling", 57),
	PALM(RunType.FRUIT_TREE, "Palm sapling", 68),
	DRAGONFRUIT(RunType.FRUIT_TREE, "Dragonfruit sapling", 81);

	private final RunType runType;
	private final String itemName;
	private final int level;

	Seed(RunType runType, String itemName, int level)
	{
		this.runType = runType;
		this.itemName = itemName;
		this.level = level;
	}

	public RunType getRunType()
	{
		return runType;
	}

	public String getItemName()
	{
		return itemName;
	}

	public int getLevel()
	{
		return level;
	}

	/** Zaden voor deze run, hoogste level eerst (meeste XP per patch). */
	public static List<Seed> forRun(RunType type)
	{
		List<Seed> result = new ArrayList<>();
		for (Seed s : values())
		{
			if (s.runType == type)
			{
				result.add(s);
			}
		}
		result.sort(Comparator.comparingInt(Seed::getLevel).reversed());
		return result;
	}
}
