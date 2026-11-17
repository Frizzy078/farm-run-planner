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
	private final List<Step> steps = new ArrayList<>();
	private final Map<String, Integer> withdraw = new LinkedHashMap<>();
	private final Set<Integer> highlightIds = new HashSet<>();
	private final List<String> warnings = new ArrayList<>();
	private final List<String> tips = new ArrayList<>();

	FarmPlan(RunType runType, int farmingLevel)
	{
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
	}

	/** Voor herbruikbare items: hooguit 1 meenemen. */
	void withdrawOnce(OwnedItems.Entry item)
	{
		withdraw.putIfAbsent(item.getName(), 1);
		highlightIds.add(item.getId());
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
