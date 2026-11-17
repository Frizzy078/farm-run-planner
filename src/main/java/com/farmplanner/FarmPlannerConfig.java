package com.farmplanner;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(FarmPlannerConfig.GROUP)
public interface FarmPlannerConfig extends Config
{
	String GROUP = "farmrunplanner";

	@ConfigItem(
		keyName = "highlightBank",
		name = "Highlight in bank",
		description = "Markeer de items die je moet meenemen in je bank en inventory",
		position = 1
	)
	default boolean highlightBank()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight kleur",
		description = "Kleur van de markering in de bank",
		position = 2
	)
	default Color highlightColor()
	{
		return new Color(0, 255, 120, 200);
	}

	@ConfigItem(
		keyName = "highlightTeleport",
		name = "Highlight teleport-keuze",
		description = "Kleurt de juiste optie in het rechtermuisklik-menu en in het teleport-venster",
		position = 10
	)
	default boolean highlightTeleport()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightPatches",
		name = "Highlight patches",
		description = "Markeer de farming patches van je run in de wereld",
		position = 11
	)
	default boolean highlightPatches()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hintArrow",
		name = "Pijl naar patch",
		description = "Toon een gele pijl (ook op de minimap) naar de dichtstbijzijnde patch",
		position = 12
	)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "coinsPerTree",
		name = "Coins per boom",
		description = "Coins die je meeneemt per (fruit)boom om de oude boom te laten weghalen",
		position = 13
	)
	default int coinsPerTree()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "useSpells",
		name = "Teleport-spreuken gebruiken",
		description = "Gebruik spreuken (met runes uit je bank) als je geen teleport-item hebt",
		position = 3
	)
	default boolean useSpells()
	{
		return true;
	}

	@ConfigItem(
		keyName = "houseInHosidius",
		name = "Huis staat in Hosidius",
		description = "Gebruik 'Teleport to house' voor de Hosidius patch",
		position = 4
	)
	default boolean houseInHosidius()
	{
		return false;
	}

	@ConfigItem(
		keyName = "includeTrollheim",
		name = "Trollheim herb patch",
		description = "Neem de Trollheim herb patch mee (My Arm's Big Adventure nodig)",
		position = 5
	)
	default boolean includeTrollheim()
	{
		return false;
	}

	@ConfigItem(
		keyName = "includeLletya",
		name = "Lletya fruit tree patch",
		description = "Neem de Lletya fruit tree patch mee (Mourning's End Part I nodig)",
		position = 6
	)
	default boolean includeLletya()
	{
		return false;
	}
}
