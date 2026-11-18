package com.farmplanner;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Eén manier om bij een patch te komen: een item, een spreuk, of iets zonder item (spirit tree).
 *
 * Itemnamen die eindigen op "(" of "*" matchen op het begin van de naam,
 * zodat bijv. "Skills necklace(" alle geladen versies (1)-(6) vindt.
 *
 * menuOptions: welke rechtermuisklik-optie op het item je moet kiezen (bijv. "Rub", "Farming Guild").
 * dialogOptions: welke keuze in het venster/dialoog daarna (bijv. "Farming Guild").
 */
public final class TeleportOption
{
	public enum Kind
	{
		ITEM, SPELL, FREE
	}

	private final String label;
	private final Kind kind;
	private final List<String> itemPatterns;
	private final boolean consumable;
	private final Map<String, Integer> runes;
	private final int magicLevel;
	private final String spellName;
	private final String menuTarget;
	private Predicate<PlanOptions> condition;
	private List<String> menuOptions = Collections.emptyList();
	private List<String> dialogOptions = Collections.emptyList();

	private TeleportOption(String label, Kind kind, List<String> itemPatterns, boolean consumable,
		Map<String, Integer> runes, int magicLevel, String spellName, String menuTarget, Predicate<PlanOptions> condition)
	{
		this.label = label;
		this.kind = kind;
		this.itemPatterns = itemPatterns;
		this.consumable = consumable;
		this.runes = runes;
		this.magicLevel = magicLevel;
		this.spellName = spellName;
		this.menuTarget = menuTarget;
		this.condition = condition;
	}

	/** Herbruikbaar item (cloak, ring, sieraad met charges). */
	static TeleportOption item(String label, String... patterns)
	{
		return new TeleportOption(label, Kind.ITEM, Arrays.asList(patterns), false,
			Collections.emptyMap(), 0, null, null, o -> true);
	}

	/** Item dat opgaat bij gebruik (teleport tablet). Highlight standaard "Break". */
	static TeleportOption tablet(String label, String... patterns)
	{
		return new TeleportOption(label, Kind.ITEM, Arrays.asList(patterns), true,
			Collections.emptyMap(), 0, null, null, o -> true).menu("Break");
	}

	/**
	 * Spreuk. runeSpec als "Law rune", 1, "Air rune", 3, ...
	 */
	static TeleportOption spell(String spellName, int magicLevel, Object... runeSpec)
	{
		Map<String, Integer> runes = new LinkedHashMap<>();
		for (int i = 0; i < runeSpec.length; i += 2)
		{
			runes.put((String) runeSpec[i], (Integer) runeSpec[i + 1]);
		}
		return new TeleportOption(spellName, Kind.SPELL, Collections.emptyList(), true,
			runes, magicLevel, spellName, null, PlanOptions::isUseSpells);
	}

	/** Geen item nodig, bijv. een spirit tree. menuTarget = naam van het object waar je op klikt. */
	static TeleportOption free(String label, String menuTarget)
	{
		return new TeleportOption(label, Kind.FREE, Collections.emptyList(), false,
			Collections.emptyMap(), 0, null, menuTarget, o -> true);
	}

	TeleportOption onlyIf(Predicate<PlanOptions> extra)
	{
		condition = condition.and(extra);
		return this;
	}

	TeleportOption menu(String... options)
	{
		menuOptions = Arrays.asList(options);
		return this;
	}

	TeleportOption dialog(String... options)
	{
		dialogOptions = Arrays.asList(options);
		return this;
	}

	public String getLabel(Language lang)
	{
		return kind == Kind.SPELL ? label + lang.t(" (spell)", " (spreuk)") : label;
	}

	public Kind getKind()
	{
		return kind;
	}

	public List<String> getItemPatterns()
	{
		return itemPatterns;
	}

	public boolean isConsumable()
	{
		return consumable;
	}

	public Map<String, Integer> getRunes()
	{
		return runes;
	}

	public int getMagicLevel()
	{
		return magicLevel;
	}

	/** Naam van de spreuk in het spellbook (alleen bij SPELL). */
	public String getSpellName()
	{
		return spellName;
	}

	/** Object waar je op klikt bij FREE (bijv. "Spirit tree"). */
	public String getMenuTarget()
	{
		return menuTarget;
	}

	public List<String> getMenuOptions()
	{
		return menuOptions;
	}

	public List<String> getDialogOptions()
	{
		return dialogOptions;
	}

	/** Korte uitleg welke optie je moet kiezen, voor in het paneel. */
	public String describeChoice()
	{
		if (kind == Kind.SPELL)
		{
			return "Cast " + spellName;
		}
		if (!dialogOptions.isEmpty())
		{
			String first = menuOptions.isEmpty() ? "" : menuOptions.get(menuOptions.size() - 1) + " -> ";
			return first + dialogOptions.get(0);
		}
		if (!menuOptions.isEmpty())
		{
			return menuOptions.get(0);
		}
		return null;
	}

	public boolean isEnabled(PlanOptions options)
	{
		return condition.test(options);
	}
}
