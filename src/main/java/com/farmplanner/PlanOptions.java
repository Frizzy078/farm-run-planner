package com.farmplanner;

/** Instellingen die het plan beïnvloeden (komen uit de plugin-config). */
public final class PlanOptions
{
	private final boolean useSpells;
	private final boolean houseInHosidius;
	private final boolean includeTrollheim;
	private final boolean includeLletya;
	private final int coinsPerTree;

	public PlanOptions(boolean useSpells, boolean houseInHosidius, boolean includeTrollheim, boolean includeLletya,
		int coinsPerTree)
	{
		this.useSpells = useSpells;
		this.houseInHosidius = houseInHosidius;
		this.includeTrollheim = includeTrollheim;
		this.includeLletya = includeLletya;
		this.coinsPerTree = coinsPerTree;
	}

	public static PlanOptions defaults()
	{
		return new PlanOptions(true, false, false, false, 200);
	}

	/** Coins per (fruit)boom om de gardener de oude boom te laten weghalen. */
	public int getCoinsPerTree()
	{
		return coinsPerTree;
	}

	public boolean isUseSpells()
	{
		return useSpells;
	}

	public boolean isHouseInHosidius()
	{
		return houseInHosidius;
	}

	public boolean isIncludeTrollheim()
	{
		return includeTrollheim;
	}

	public boolean isIncludeLletya()
	{
		return includeLletya;
	}
}
