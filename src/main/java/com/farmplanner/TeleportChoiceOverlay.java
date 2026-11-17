package com.farmplanner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Tekent een kader om de juiste keuze in een teleport-venster
 * (bijv. "Farming Guild" na het rubben van een Skills necklace) en om de juiste spreuk.
 */
public class TeleportChoiceOverlay extends Overlay
{
	private final FarmPlannerPlugin plugin;

	@Inject
	TeleportChoiceOverlay(FarmPlannerPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Color color = plugin.getConfig().highlightColor();
		Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
		for (Widget w : plugin.getHighlightedWidgets())
		{
			if (w.isHidden())
			{
				continue;
			}
			Rectangle b = w.getBounds();
			if (b == null || b.width <= 0 || b.height <= 0)
			{
				continue;
			}
			graphics.setColor(fill);
			graphics.fill(b);
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(2));
			graphics.drawRect(b.x, b.y, b.width - 1, b.height - 1);
		}
		return null;
	}
}
