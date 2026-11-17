package com.farmplanner;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Start RuneLite met deze plugin (gebruikt door ./gradlew runClient). */
public class FarmPlannerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FarmPlannerPlugin.class);
		RuneLite.main(args);
	}
}
