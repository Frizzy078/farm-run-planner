package com.farmplanner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * In de bank: kader om alles wat je volgens het plan moet meenemen.
 * In je inventory: kader om de teleport en het zaad van de huidige stap.
 */
public class BankHighlightOverlay extends WidgetItemOverlay
{
	private final FarmPlannerPlugin plugin;

	@Inject
	BankHighlightOverlay(FarmPlannerPlugin plugin)
	{
		this.plugin = plugin;
		showOnBank();
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.getConfig().highlightBank())
		{
			return;
		}
		boolean inInventory = widgetItem.getWidget() != null
			&& WidgetUtil.componentToInterface(widgetItem.getWidget().getId()) == InterfaceID.INVENTORY;
		boolean highlight = inInventory
			? plugin.isSaplingActionItem(itemId)
				|| plugin.getInventoryHighlightIds().contains(itemId) || plugin.isCurrentTeleportItem(itemId)
			: plugin.getBankHighlightIds().contains(itemId);
		if (!highlight)
		{
			return;
		}
		Rectangle b = widgetItem.getCanvasBounds();
		Color c = plugin.getConfig().highlightColor();
		graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(60, c.getAlpha())));
		graphics.fill(b);
		graphics.setColor(c);
		graphics.setStroke(new BasicStroke(2));
		graphics.drawRect(b.x, b.y, b.width - 1, b.height - 1);
	}
}
