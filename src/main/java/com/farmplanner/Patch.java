package com.farmplanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Farming patches. De volgorde binnen elke run is de route-volgorde.
 */
public enum Patch
{
	// ---- Herb run ----
	ARDOUGNE_HERB(RunType.HERB, "Ardougne", 1, "Patch ten noorden van Ardougne.", Teleports.ARDOUGNE),
	CATHERBY_HERB(RunType.HERB, "Catherby", 1, "Patch ten westen van de bank, aan het strand.", Teleports.CATHERBY),
	FALADOR_HERB(RunType.HERB, "Falador", 1, "Patch ten zuiden van Falador (bij de cabbage patch).", Teleports.FALADOR_HERB),
	MORYTANIA_HERB(RunType.HERB, "Port Phasmatys", 1, "Loop vanaf de Ectofuntus naar het westen.", Teleports.MORYTANIA),
	HOSIDIUS_HERB(RunType.HERB, "Hosidius", 1, "Patch ten zuiden van Hosidius.", Teleports.HOSIDIUS),
	GUILD_HERB(RunType.HERB, "Farming Guild", 65, "Herb patch in de Farming Guild (65 Farming).", Teleports.FARMING_GUILD),
	TROLLHEIM_HERB(RunType.HERB, "Trollheim", 1, "Disease-vrije patch (My Arm's Big Adventure nodig).", Teleports.TROLLHEIM,
		PlanOptions::isIncludeTrollheim),

	// ---- Tree run ----
	LUMBRIDGE_TREE(RunType.TREE, "Lumbridge", 1, "Ten westen van het kasteel.", Teleports.LUMBRIDGE),
	VARROCK_TREE(RunType.TREE, "Varrock", 1, "Bij het kasteel, ten noordoosten van de fontein.", Teleports.VARROCK),
	FALADOR_TREE(RunType.TREE, "Falador", 1, "In het Falador park.", Teleports.FALADOR),
	TAVERLEY_TREE(RunType.TREE, "Taverley", 1, "Ten zuiden van Taverley.", Teleports.TAVERLEY),
	GNOME_TREE(RunType.TREE, "Tree Gnome Stronghold", 1, "Ten zuiden van de Grand Tree.", Teleports.GNOME_STRONGHOLD),
	GUILD_TREE(RunType.TREE, "Farming Guild", 65, "Tree patch in de Farming Guild (65 Farming).", Teleports.FARMING_GUILD),

	// ---- Fruit tree run ----
	GNOME_FRUIT(RunType.FRUIT_TREE, "Tree Gnome Stronghold", 1, "Ten oosten van de Grand Tree.", Teleports.GNOME_STRONGHOLD),
	VILLAGE_FRUIT(RunType.FRUIT_TREE, "Tree Gnome Village", 1, "Bij de spirit tree in Tree Gnome Village.", Teleports.SPIRIT_TREE_VILLAGE),
	CATHERBY_FRUIT(RunType.FRUIT_TREE, "Catherby", 1, "Aan de oostkant van Catherby, bij het strand.", Teleports.CATHERBY),
	BRIMHAVEN_FRUIT(RunType.FRUIT_TREE, "Brimhaven", 1, "Vanaf Ardougne de boot naar Brimhaven (30 coins).", Teleports.ARDOUGNE),
	LLETYA_FRUIT(RunType.FRUIT_TREE, "Lletya", 1, "Mourning's End Part I nodig.", Teleports.LLETYA,
		PlanOptions::isIncludeLletya),
	GUILD_FRUIT(RunType.FRUIT_TREE, "Farming Guild", 85, "Fruit tree patch in de Farming Guild (85 Farming).", Teleports.FARMING_GUILD);

	private final RunType runType;
	private final String location;
	private final int farmingLevel;
	private final String tip;
	private final List<TeleportOption> teleports;
	private final Predicate<PlanOptions> enabled;

	Patch(RunType runType, String location, int farmingLevel, String tip, TeleportOption[] teleports)
	{
		this(runType, location, farmingLevel, tip, teleports, o -> true);
	}

	Patch(RunType runType, String location, int farmingLevel, String tip, TeleportOption[] teleports,
		Predicate<PlanOptions> enabled)
	{
		this.runType = runType;
		this.location = location;
		this.farmingLevel = farmingLevel;
		this.tip = tip;
		this.teleports = Arrays.asList(teleports);
		this.enabled = enabled;
	}

	public RunType getRunType()
	{
		return runType;
	}

	public String getLocation()
	{
		return location;
	}

	public int getFarmingLevel()
	{
		return farmingLevel;
	}

	public String getTip()
	{
		return tip;
	}

	public List<TeleportOption> getTeleports()
	{
		return teleports;
	}

	public boolean isEnabled(PlanOptions options)
	{
		return enabled.test(options);
	}

	/** Patches voor deze run, in route-volgorde. */
	public static List<Patch> forRun(RunType type)
	{
		List<Patch> result = new ArrayList<>();
		for (Patch p : values())
		{
			if (p.runType == type)
			{
				result.add(p);
			}
		}
		return result;
	}
}
