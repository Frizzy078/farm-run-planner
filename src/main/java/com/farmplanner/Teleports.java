package com.farmplanner;

import static com.farmplanner.TeleportOption.free;
import static com.farmplanner.TeleportOption.item;
import static com.farmplanner.TeleportOption.spell;
import static com.farmplanner.TeleportOption.tablet;

/**
 * Teleport-opties per bestemming, op volgorde van voorkeur (beste eerst).
 * De planner kiest de eerste optie die je in je bank/inventory/equipment hebt.
 *
 * .menu(...)   = rechtermuisklik-optie(s) op het item die gehighlight worden (deel van de tekst is genoeg)
 * .dialog(...) = keuze in het venster dat daarna opent
 */
final class Teleports
{
	private Teleports()
	{
	}

	static final TeleportOption[] ARDOUGNE = {
		item("Ardougne cloak (Farm Teleport)", "Ardougne cloak 2", "Ardougne cloak 3", "Ardougne cloak 4")
			.menu("Farm"),
		tablet("Ardougne teleport tablet", "Ardougne teleport"),
		spell("Ardougne Teleport", 51, "Law rune", 2, "Water rune", 2),
	};

	static final TeleportOption[] CATHERBY = {
		tablet("Catherby teleport tablet", "Catherby teleport"),
		tablet("Camelot teleport tablet", "Camelot teleport"),
		spell("Camelot Teleport", 45, "Law rune", 1, "Air rune", 5),
	};

	static final TeleportOption[] FALADOR_HERB = {
		item("Explorer's ring (Cabbage-patch teleport)", "Explorer's ring 2", "Explorer's ring 3", "Explorer's ring 4")
			.menu("Teleport"),
		tablet("Falador teleport tablet", "Falador teleport"),
		spell("Falador Teleport", 37, "Law rune", 1, "Air rune", 3, "Water rune", 1),
	};

	static final TeleportOption[] FALADOR = {
		tablet("Falador teleport tablet", "Falador teleport"),
		spell("Falador Teleport", 37, "Law rune", 1, "Air rune", 3, "Water rune", 1),
	};

	static final TeleportOption[] MORYTANIA = {
		item("Ectophial", "Ectophial").menu("Empty"),
	};

	static final TeleportOption[] HOSIDIUS = {
		item("Xeric's talisman (Xeric's Glade)", "Xeric's talisman")
			.menu("Rub").dialog("Xeric's Glade"),
		tablet("Teleport to house (huis in Hosidius)", "Teleport to house")
			.onlyIf(PlanOptions::isHouseInHosidius),
	};

	static final TeleportOption[] FARMING_GUILD = {
		item("Skills necklace (Farming Guild)", "Skills necklace(")
			.menu("Farming Guild", "Rub").dialog("Farming Guild"),
		item("Farming cape", "Farming cape", "Farming cape(t)").menu("Teleport"),
	};

	static final TeleportOption[] TROLLHEIM = {
		item("Stony basalt", "Stony basalt").menu("Troll Stronghold", "Break"),
		tablet("Trollheim teleport tablet", "Trollheim teleport"),
		spell("Trollheim Teleport", 61, "Law rune", 2, "Fire rune", 2),
	};

	static final TeleportOption[] LUMBRIDGE = {
		tablet("Lumbridge teleport tablet", "Lumbridge teleport"),
		spell("Lumbridge Teleport", 31, "Law rune", 1, "Air rune", 3, "Earth rune", 1),
	};

	static final TeleportOption[] VARROCK = {
		tablet("Varrock teleport tablet", "Varrock teleport"),
		spell("Varrock Teleport", 25, "Law rune", 1, "Air rune", 3, "Fire rune", 1),
	};

	static final TeleportOption[] TAVERLEY = {
		item("Games necklace (Burthorpe)", "Games necklace(")
			.menu("Burthorpe", "Rub").dialog("Burthorpe"),
		tablet("Falador teleport tablet", "Falador teleport"),
		spell("Falador Teleport", 37, "Law rune", 1, "Air rune", 3, "Water rune", 1),
	};

	static final TeleportOption[] GNOME_STRONGHOLD = {
		item("Royal seed pod", "Royal seed pod").menu("Commune"),
		free("Spirit tree", "Spirit tree").menu("Travel").dialog("Gnome Stronghold"),
	};

	static final TeleportOption[] SPIRIT_TREE_VILLAGE = {
		free("Spirit tree", "Spirit tree").menu("Travel").dialog("Tree Gnome Village"),
	};

	static final TeleportOption[] LLETYA = {
		item("Teleport crystal (Lletya)", "Teleport crystal (", "Eternal teleport crystal")
			.menu("Lletya", "Teleport").dialog("Lletya"),
	};
}
