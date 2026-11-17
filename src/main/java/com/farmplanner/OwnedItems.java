package com.farmplanner;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Alle items die je hebt (bank + inventory + equipment), opgezocht op naam.
 */
public final class OwnedItems
{
	public static final class Entry
	{
		private final int id;
		private final String name;
		private int quantity;

		Entry(int id, String name, int quantity)
		{
			this.id = id;
			this.name = name;
			this.quantity = quantity;
		}

		public int getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public int getQuantity()
		{
			return quantity;
		}
	}

	private final Map<String, Entry> byName = new LinkedHashMap<>();

	public void add(int id, String name, int quantity)
	{
		if (name == null || quantity <= 0)
		{
			return;
		}
		String key = name.toLowerCase(Locale.ROOT);
		Entry existing = byName.get(key);
		if (existing != null)
		{
			existing.quantity += quantity;
		}
		else
		{
			byName.put(key, new Entry(id, name, quantity));
		}
	}

	public boolean isEmpty()
	{
		return byName.isEmpty();
	}

	public int size()
	{
		return byName.size();
	}

	/** Aantal van een item met exact deze naam. */
	public int count(String exactName)
	{
		Entry e = byName.get(exactName.toLowerCase(Locale.ROOT));
		return e == null ? 0 : e.quantity;
	}

	public Entry get(String exactName)
	{
		return byName.get(exactName.toLowerCase(Locale.ROOT));
	}

	/**
	 * Zoekt een item. Patronen die eindigen op "(" of "*" matchen op het begin van de naam.
	 * Bij meerdere matches wint degene met het hoogste aantal.
	 */
	public Entry find(String pattern)
	{
		String p = pattern.toLowerCase(Locale.ROOT);
		boolean prefix = p.endsWith("(") || p.endsWith("*");
		if (!prefix)
		{
			return byName.get(p);
		}
		if (p.endsWith("*"))
		{
			p = p.substring(0, p.length() - 1);
		}
		Entry best = null;
		for (Map.Entry<String, Entry> e : byName.entrySet())
		{
			if (e.getKey().startsWith(p) && (best == null || e.getValue().quantity > best.quantity))
			{
				best = e.getValue();
			}
		}
		return best;
	}

	/** Diepe kopie zodat de planner aantallen kan aftrekken. */
	public OwnedItems copy()
	{
		OwnedItems c = new OwnedItems();
		for (Entry e : byName.values())
		{
			c.add(e.id, e.name, e.quantity);
		}
		return c;
	}

	/** Trekt een aantal af (voor zaden, tablets, runes die opgaan). */
	void use(String exactName, int amount)
	{
		Entry e = get(exactName);
		if (e != null)
		{
			e.quantity = Math.max(0, e.quantity - amount);
		}
	}
}
