package com.farmplanner;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(FarmPlannerConfig.GROUP)
public interface FarmPlannerConfig extends Config
{
	String GROUP = "banktopatch";

	@ConfigItem(
		keyName = "language",
		name = "Language / Taal",
		description = "Language of the side panel and plan / Taal van het paneel en het plan",
		position = 0
	)
	default Language language()
	{
		return Language.ENGLISH;
	}

	@ConfigItem(
		keyName = "highlightBank",
		name = "Highlight in bank",
		description = "Highlight the items you need to take in your bank and inventory",
		position = 1
	)
	default boolean highlightBank()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "Color of the highlights",
		position = 2
	)
	default Color highlightColor()
	{
		return new Color(0, 255, 120, 200);
	}

	@ConfigItem(
		keyName = "highlightTeleport",
		name = "Highlight teleport choice",
		description = "Colors the right option in the right-click menu and in the teleport window",
		position = 10
	)
	default boolean highlightTeleport()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightPatches",
		name = "Highlight patches",
		description = "Highlight the farming patches of your run in the world",
		position = 11
	)
	default boolean highlightPatches()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hintArrow",
		name = "Arrow to patch",
		description = "Show a yellow arrow (also on the minimap) to the nearest patch",
		position = 12
	)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "payProtection",
		name = "Pay for tree protection",
		description = "Take the gardener's payment (e.g. 10 cactus spines per Yew) so your trees can't die",
		position = 14
	)
	default boolean payProtection()
	{
		return true;
	}

	@ConfigItem(
		keyName = "coinsPerTree",
		name = "Coins per tree",
		description = "Coins to take per (fruit) tree to have the gardener remove the old tree",
		position = 13
	)
	default int coinsPerTree()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "useSpells",
		name = "Use teleport spells",
		description = "Use spells (with runes from your bank) when you have no teleport item",
		position = 3
	)
	default boolean useSpells()
	{
		return true;
	}

	@ConfigItem(
		keyName = "houseInHosidius",
		name = "House in Hosidius",
		description = "Use 'Teleport to house' for the Hosidius patch",
		position = 4
	)
	default boolean houseInHosidius()
	{
		return false;
	}

	@ConfigItem(
		keyName = "includeTrollheim",
		name = "Trollheim herb patch",
		description = "Include the Trollheim herb patch (requires My Arm's Big Adventure)",
		position = 5
	)
	default boolean includeTrollheim()
	{
		return false;
	}

	@ConfigItem(
		keyName = "includeLletya",
		name = "Lletya fruit tree patch",
		description = "Include the Lletya fruit tree patch (requires Mourning's End Part I)",
		position = 6
	)
	default boolean includeLletya()
	{
		return false;
	}
}
