package com.farmplanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Het resultaat: route-stappen, wat je moet meenemen, en waarschuwingen. */
public final class FarmPlan
{
	public static final class Step
	{
		private final Patch patch;
		private final String teleport;
		private final String seed;
		private final String compost;
		private TeleportOption teleportOption;
		private String teleportItem;
		private int teleportItemId = -1;
		private int seedItemId = -1;
		private String payment;

		Step(Patch patch, String teleport, String seed, String compost)
		{
			this.patch = patch;
			this.teleport = teleport;
			this.seed = seed;
			this.compost = compost;
		}

		Step withTeleport(TeleportOption option, OwnedItems.Entry item)
		{
			this.teleportOption = option;
			if (item != null)
			{
				this.teleportItem = item.getName();
				this.teleportItemId = item.getId();
			}
			return this;
		}

		Step withSeedId(int id)
		{
			this.seedItemId = id;
			return this;
		}

		public Patch getPatch()
		{
			return patch;
		}

		/** De gekozen teleport-optie, of null. */
		public TeleportOption getTeleportOption()
		{
			return teleportOption;
		}

		/** Exacte naam van het teleport-item (bijv. "Skills necklace(4)"), of null bij spreuk/spirit tree. */
		public String getTeleportItem()
		{
			return teleportItem;
		}

		public int getTeleportItemId()
		{
			return teleportItemId;
		}

		public int getSeedItemId()
		{
			return seedItemId;
		}

		/** Betaling voor de gardener (bijv. "10 cactus spines"), of null. */
		public String getPayment()
		{
			return payment;
		}

		void setPayment(String payment)
		{
			this.payment = payment;
		}

		/** null = geen teleport gevonden. */
		public String getTeleport()
		{
			return teleport;
		}

		/** null = geen zaad meer over voor deze patch. */
		public String getSeed()
		{
			return seed;
		}

		/** null = geen compost meer over. */
		public String getCompost()
		{
			return compost;
		}
	}

	private final RunType runType;
	private final int farmingLevel;
	private final Language language;
	private final List<Step> steps = new ArrayList<>();
	private final Map<String, Integer> withdraw = new LinkedHashMap<>();
	private final Set<Integer> highlightIds = new HashSet<>();
	private final Map<String, Integer> withdrawIds = new java.util.HashMap<>();
	private final Map<String, String> anyChargePrefix = new java.util.HashMap<>();
	private final List<String> warnings = new ArrayList<>();
	private final List<String> tips = new ArrayList<>();
	private final List<String> saplingAdvice = new ArrayList<>();
	private final List<String> seedOverview = new ArrayList<>();
	private double xpNow;
	private double xpLater;

	FarmPlan(RunType runType, int farmingLevel, Language language)
	{
		this.language = language;
		this.runType = runType;
		this.farmingLevel = farmingLevel;
	}

	void addStep(Step step)
	{
		steps.add(step);
	}

	void withdraw(OwnedItems.Entry item, int amount)
	{
		withdraw.merge(item.getName(), amount, Integer::sum);
		highlightIds.add(item.getId());
		withdrawIds.put(item.getName(), item.getId());
	}

	/** Voor herbruikbare items: hooguit 1 meenemen. */
	void withdrawOnce(OwnedItems.Entry item)
	{
		withdraw.putIfAbsent(item.getName(), 1);
		highlightIds.add(item.getId());
		withdrawIds.put(item.getName(), item.getId());
	}

	/**
	 * Dit item mag met elke lading meetellen, bijv. "Games necklace(" voor Games necklace(1) t/m (8).
	 */
	void allowAnyCharge(String itemName, String prefixPattern)
	{
		if (prefixPattern.endsWith("(") || prefixPattern.endsWith("*"))
		{
			anyChargePrefix.put(itemName, prefixPattern.endsWith("*")
				? prefixPattern.substring(0, prefixPattern.length() - 1)
				: prefixPattern);
		}
	}

	/**
	 * Hoeveel heb je van dit meeneem-item bij je?
	 *
	 * @param carried itemnaam (kleine letters) -> aantal, uit je inventory en equipment
	 */
	public int countCarried(String itemName, Map<String, Integer> carried)
	{
		String prefix = anyChargePrefix.get(itemName);
		if (prefix == null)
		{
			return carried.getOrDefault(itemName.toLowerCase(java.util.Locale.ROOT), 0);
		}
		String p = prefix.toLowerCase(java.util.Locale.ROOT);
		int total = 0;
		for (Map.Entry<String, Integer> e : carried.entrySet())
		{
			if (e.getKey().startsWith(p))
			{
				total += e.getValue();
			}
		}
		return total;
	}

	/** Item-ID's die je nog niet (genoeg) bij je hebt: die worden in de bank gemarkeerd. */
	public Set<Integer> missingIds(Map<String, Integer> carried)
	{
		Set<Integer> result = new HashSet<>();
		for (Map.Entry<String, Integer> e : withdraw.entrySet())
		{
			if (countCarried(e.getKey(), carried) < e.getValue())
			{
				Integer id = withdrawIds.get(e.getKey());
				if (id != null)
				{
					result.add(id);
				}
			}
		}
		return result;
	}

	void warn(String warning)
	{
		if (!warnings.contains(warning))
		{
			warnings.add(warning);
		}
	}

	void tip(String tip)
	{
		tips.add(tip);
	}

	void addXp(double now, double later)
	{
		xpNow += now;
		xpLater += later;
	}

	/** XP die je deze run krijgt (planten + compost). */
	public double getXpNow()
	{
		return xpNow;
	}

	/** XP die je later krijgt (Check-health bij bomen, oogsten bij herbs - geschat). */
	public double getXpLater()
	{
		return xpLater;
	}

	public List<String> getSaplingAdvice()
	{
		return saplingAdvice;
	}

	public List<String> getSeedOverview()
	{
		return seedOverview;
	}

	/** Taal waarin de teksten van dit plan staan. */
	public Language getLanguage()
	{
		return language;
	}

	public RunType getRunType()
	{
		return runType;
	}

	public int getFarmingLevel()
	{
		return farmingLevel;
	}

	public List<Step> getSteps()
	{
		return steps;
	}

	public Map<String, Integer> getWithdraw()
	{
		return withdraw;
	}

	public Set<Integer> getHighlightIds()
	{
		return highlightIds;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}

	public List<String> getTips()
	{
		return tips;
	}

	/** Alleen de stappen waar echt iets geplant wordt, in route-volgorde. */
	public List<Step> getActiveSteps()
	{
		List<Step> result = new ArrayList<>();
		for (Step s : steps)
		{
			if (s.seed != null)
			{
				result.add(s);
			}
		}
		return result;
	}

	/** Aantal patches waar echt iets geplant wordt. */
	public int getPlantedCount()
	{
		int n = 0;
		for (Step s : steps)
		{
			if (s.seed != null)
			{
				n++;
			}
		}
		return n;
	}
}
