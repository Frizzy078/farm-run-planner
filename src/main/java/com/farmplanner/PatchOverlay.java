package com.farmplanner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Markeert de farming patches van je run in de wereld, met wat je moet planten. */
public class PatchOverlay extends Overlay
{
	private final FarmPlannerPlugin plugin;

	@Inject
	PatchOverlay(FarmPlannerPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		FarmPlan.Step step = plugin.getCurrentStep();
		if (!plugin.getConfig().highlightPatches() || step == null)
		{
			return null;
		}
		Color color = plugin.getConfig().highlightColor();
		Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 40);
		String label = "Plant: " + step.getSeed()
			+ (step.getCompost() != null ? " + " + step.getCompost() : "");

		for (GameObject patch : plugin.getVisiblePatches())
		{
			Shape shape = patch.getClickbox();
			if (shape == null)
			{
				shape = patch.getConvexHull();
			}
			if (shape != null)
			{
				OverlayUtil.renderPolygon(graphics, shape, color, fill, new BasicStroke(2));
			}
			Point text = patch.getCanvasTextLocation(graphics, label, 120);
			if (text != null)
			{
				OverlayUtil.renderTextLocation(graphics, text, label, color);
			}
		}
		return null;
	}
}
