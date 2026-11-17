package com.farmplanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Zaden en saplings die geplant kunnen worden.
 * Namen zijn exact de in-game itemnamen. XP-waarden komen van de OSRS wiki.
 *
 * xpPlant = XP bij het planten.
 * xpLater = bomen: XP bij "Check-health" de volgende keer; herbs: XP per geoogste herb.
 * payment = wat de gardener wil om de boom te beschermen (null bij herbs).
 */
public enum Seed
{
	// Herbs (xpLater = XP per herb bij het oogsten)
	GUAM(RunType.HERB, "Guam seed", 9, 11, 12.5),
	MARRENTILL(RunType.HERB, "Marrentill seed", 14, 13.5, 15),
	TARROMIN(RunType.HERB, "Tarromin seed", 19, 16, 18),
	HARRALANDER(RunType.HERB, "Harralander seed", 26, 21.5, 24),
	RANARR(RunType.HERB, "Ranarr seed", 32, 27, 30.5),
	TOADFLAX(RunType.HERB, "Toadflax seed", 38, 34, 38.5),
	IRIT(RunType.HERB, "Irit seed", 44, 43, 48.5),
	AVANTOE(RunType.HERB, "Avantoe seed", 50, 54.5, 61.5),
	KWUARM(RunType.HERB, "Kwuarm seed", 56, 69, 78),
	SNAPDRAGON(RunType.HERB, "Snapdragon seed", 62, 87.5, 98.5),
	CADANTINE(RunType.HERB, "Cadantine seed", 67, 106.5, 120),
	LANTADYME(RunType.HERB, "Lantadyme seed", 73, 134.5, 151.5),
	DWARF_WEED(RunType.HERB, "Dwarf weed seed", 79, 170.5, 192),
	TORSTOL(RunType.HERB, "Torstol seed", 85, 199.5, 224.5),

	// Bomen
	OAK(RunType.TREE, "Oak sapling", 15, 14, 467.3, "Acorn", "Oak seedling", "Tomatoes(5)", 1, "1 mand tomaten"),
	WILLOW(RunType.TREE, "Willow sapling", 30, 25, 1456.5, "Willow seed", "Willow seedling", "Apples(5)", 1, "1 mand appels"),
	MAPLE(RunType.TREE, "Maple sapling", 45, 45, 3403.4, "Maple seed", "Maple seedling", "Oranges(5)", 1, "1 mand sinaasappels"),
	YEW(RunType.TREE, "Yew sapling", 60, 81, 7069.9, "Yew seed", "Yew seedling", "Cactus spine", 10, "10 cactus spines"),
	MAGIC(RunType.TREE, "Magic sapling", 75, 145.5, 13768.3, "Magic seed", "Magic seedling", "Coconut", 25, "25 coconuts"),

	// Fruitbomen
	APPLE(RunType.FRUIT_TREE, "Apple sapling", 27, 22, 1199.5, "Apple tree seed", "Apple seedling", "Sweetcorn", 9, "9 sweetcorn"),
	BANANA(RunType.FRUIT_TREE, "Banana sapling", 33, 28, 1750.5, "Banana tree seed", "Banana seedling", "Apples(5)", 4, "4 manden appels"),
	ORANGE(RunType.FRUIT_TREE, "Orange sapling", 39, 35.5, 2470.2, "Orange tree seed", "Orange seedling", "Strawberries(5)", 3, "3 manden aardbeien"),
	CURRY(RunType.FRUIT_TREE, "Curry sapling", 42, 40, 2906.9, "Curry tree seed", "Curry seedling", "Banana", 5, "5 bananen"),
	PINEAPPLE(RunType.FRUIT_TREE, "Pineapple sapling", 51, 57, 4605.7, "Pineapple seed", "Pineapple seedling", "Watermelon", 10, "10 watermeloenen"),
	PAPAYA(RunType.FRUIT_TREE, "Papaya sapling", 57, 72, 6146.4, "Papaya tree seed", "Papaya seedling", "Pineapple", 10, "10 ananassen"),
	PALM(RunType.FRUIT_TREE, "Palm sapling", 68, 110.5, 10150.1, "Palm tree seed", "Palm seedling", "Papaya fruit", 15, "15 papaya fruit"),
	DRAGONFRUIT(RunType.FRUIT_TREE, "Dragonfruit sapling", 81, 140, 17335, "Dragonfruit tree seed", "Dragonfruit seedling", "Coconut", 15, "15 coconuts");

	/** Gemiddeld aantal herbs per patch (schatting voor de XP-berekening). */
	public static final int AVG_HERBS_PER_PATCH = 7;

	private final RunType runType;
	private final String itemName;
	private final int level;
	private final double xpPlant;
	private final double xpLater;
	private final String treeSeedName;
	private final String seedlingName;
	private final String paymentItem;
	private final int paymentQty;
	private final String paymentLabel;

	Seed(RunType runType, String itemName, int level, double xpPlant, double xpLater)
	{
		this(runType, itemName, level, xpPlant, xpLater, null, null, null, 0, null);
	}

	Seed(RunType runType, String itemName, int level, double xpPlant, double xpLater,
		String treeSeedName, String seedlingName, String paymentItem, int paymentQty, String paymentLabel)
	{
		this.runType = runType;
		this.itemName = itemName;
		this.level = level;
		this.xpPlant = xpPlant;
		this.xpLater = xpLater;
		this.treeSeedName = treeSeedName;
		this.seedlingName = seedlingName;
		this.paymentItem = paymentItem;
		this.paymentQty = paymentQty;
		this.paymentLabel = paymentLabel;
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

	public double getXpPlant()
	{
		return xpPlant;
	}

	/** XP die je later krijgt: Check-health (bomen) of oogsten (herbs, geschat). */
	public double getXpLater()
	{
		return runType == RunType.HERB ? xpLater * AVG_HERBS_PER_PATCH : xpLater;
	}

	/** Totaal XP per patch. */
	public double getXpTotal()
	{
		return xpPlant + getXpLater();
	}

	/** Naam van het boomzaad (bijv. "Yew seed"), null bij herbs. */
	public String getTreeSeedName()
	{
		return treeSeedName;
	}

	public String getSeedlingName()
	{
		return seedlingName;
	}

	public String getPaymentItem()
	{
		return paymentItem;
	}

	public int getPaymentQty()
	{
		return paymentQty;
	}

	public String getPaymentLabel()
	{
		return paymentLabel;
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

	public static Seed byItemName(String name)
	{
		for (Seed s : values())
		{
			if (s.itemName.equalsIgnoreCase(name))
			{
				return s;
			}
		}
		return null;
	}
}
