package com.farmplanner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class FarmPlannerPanel extends PluginPanel
{
	private static final int TEXT_WIDTH = 165;
	private static final Color DONE_BG = new Color(28, 70, 38);
	private static final Color DONE_TEXT = new Color(120, 230, 140);
	private static final Color CURRENT_BG = new Color(75, 65, 20);
	private static final Color CURRENT_BORDER = new Color(255, 200, 40);
	/** Groen: heb je bij je. Oranje: mis je nog. */
	private static final String HAVE_HEX = "#78e68c";
	private static final String MISSING_HEX = "#ffaa5a";

	private final FarmPlannerPlugin plugin;
	private final JLabel status = new JLabel();
	private final JPanel content = new JPanel();

	FarmPlannerPanel(FarmPlannerPlugin plugin)
	{
		this.plugin = plugin;
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));

		JLabel title = new JLabel("Bank to Patch");
		title.setFont(FontManager.getRunescapeBoldFont());

		JComboBox<RunType> runSelect = new JComboBox<>(RunType.values());

		JButton planButton = new JButton("Maak plan");
		planButton.addActionListener(e ->
		{
			showMessage("Bezig...");
			plugin.requestPlan((RunType) runSelect.getSelectedItem());
		});

		JButton clearButton = new JButton("Stop run");
		clearButton.addActionListener(e ->
		{
			plugin.clearPlan();
			showMessage("Run gestopt. Highlights zijn uit.");
		});

		JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 0));
		buttons.add(planButton);
		buttons.add(clearButton);

		JPanel top = new JPanel(new GridLayout(0, 1, 0, 6));
		top.add(title);
		top.add(runSelect);
		top.add(buttons);
		top.add(status);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		add(top, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		setBankSeen(false);
		showMessage("Kies een run en klik op 'Maak plan'.<br><br>"
			+ "De plugin kijkt naar je bank, inventory en equipment, "
			+ "en zegt wat je moet meenemen en waar je heen moet.");
	}

	void setBankSeen(boolean seen)
	{
		status.setText(seen ? "Bank gelezen" : "Open je bank één keer");
		status.setForeground(seen ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.PROGRESS_INPROGRESS_COLOR);
	}

	void showMessage(String html)
	{
		content.removeAll();
		content.add(block(html, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		content.revalidate();
		content.repaint();
	}

	void showPlan(FarmPlan plan, boolean[] done, Map<String, Integer> carried)
	{
		content.removeAll();
		List<FarmPlan.Step> active = plan.getActiveSteps();

		int doneCount = 0;
		int current = -1;
		for (int i = 0; i < done.length; i++)
		{
			if (done[i])
			{
				doneCount++;
			}
			else if (current < 0)
			{
				current = i;
			}
		}

		// ---- Kop met voortgang en XP
		StringBuilder head = new StringBuilder();
		head.append("<b>").append(plan.getRunType()).append("</b> (Farming ").append(plan.getFarmingLevel()).append(")<br>");
		if (!active.isEmpty())
		{
			head.append("Voortgang: ").append(doneCount).append(" / ").append(active.size()).append(" patches<br>");
			head.append("XP nu: <b>").append(xp(plan.getXpNow())).append("</b> (planten + compost)<br>");
			head.append("XP later: <b>").append(xp(plan.getXpLater())).append("</b> ")
				.append(plan.getRunType() == RunType.HERB ? "(oogsten, geschat)" : "(check-health volgende run)");
		}
		else
		{
			head.append("Niets te planten.");
		}
		content.add(block(head.toString(), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));

		// ---- Route met groene (gedaan) en gele (huidige) stappen
		if (!active.isEmpty())
		{
			content.add(header(current < 0 ? "ROUTE - KLAAR!" : "ROUTE (klik om af te vinken)"));
			for (int i = 0; i < active.size(); i++)
			{
				content.add(stepRow(i, active.get(i), i < done.length && done[i], i == current));
			}

			JButton back = new JButton("< Terug");
			back.addActionListener(e -> plugin.changeStep(-1));
			JButton next = new JButton("Klaar >");
			next.addActionListener(e -> plugin.changeStep(1));
			JPanel nav = new JPanel(new GridLayout(1, 2, 5, 0));
			nav.add(back);
			nav.add(next);
			nav.setAlignmentX(Component.LEFT_ALIGNMENT);
			nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
			content.add(nav);
		}

		// ---- Meenemen
		if (!active.isEmpty())
		{
			StringBuilder w = new StringBuilder();
			int complete = 0;
			for (Map.Entry<String, Integer> e : plan.getWithdraw().entrySet())
			{
				int need = e.getValue();
				int have = plan.countCarried(e.getKey(), carried);
				boolean ok = have >= need;
				if (ok)
				{
					complete++;
					w.append("<font color='").append(HAVE_HEX).append("'>").append(need).append(" x ")
						.append(esc(e.getKey())).append(" - OK</font><br>");
				}
				else
				{
					w.append("<font color='").append(MISSING_HEX).append("'>").append(need).append(" x ")
						.append(esc(e.getKey())).append(" (").append(have).append('/').append(need)
						.append(")</font><br>");
				}
			}
			int total = plan.getWithdraw().size();
			content.add(header(complete == total
				? "NEEM MEE - ALLES BIJ JE!"
				: "NEEM MEE UIT JE BANK (" + complete + "/" + total + " compleet)"));
			content.add(block(w.toString(), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		}

		// ---- Waarschuwingen
		if (!plan.getWarnings().isEmpty())
		{
			content.add(header("LET OP"));
			content.add(block(list(plan.getWarnings(), "! "), ColorScheme.DARKER_GRAY_COLOR, new Color(255, 170, 90)));
		}

		// ---- Seeds
		if (!plan.getSeedOverview().isEmpty())
		{
			content.add(header("JOUW SEEDS"));
			content.add(block(list(plan.getSeedOverview(), "- "), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		}

		// ---- Saplings maken
		if (!plan.getSaplingAdvice().isEmpty())
		{
			content.add(header("SAPLINGS MAKEN"));
			content.add(block(list(plan.getSaplingAdvice(), ""), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		}

		// ---- Tips
		if (!plan.getTips().isEmpty())
		{
			content.add(header("TIPS"));
			content.add(block(list(plan.getTips(), "- "), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		}

		content.revalidate();
		content.repaint();
	}

	private JLabel stepRow(int index, FarmPlan.Step s, boolean isDone, boolean isCurrent)
	{
		StringBuilder b = new StringBuilder();
		b.append("<b>").append(index + 1).append(". ").append(esc(s.getPatch().getLocation())).append("</b>");
		if (isDone)
		{
			b.append(" - GEDAAN");
		}
		else if (isCurrent)
		{
			b.append(" - NU");
		}
		b.append("<br>");
		if (!isDone)
		{
			b.append("Teleport: ").append(s.getTeleport() != null ? esc(s.getTeleport()) : "geen - lopen").append("<br>");
			String choice = s.getTeleportOption() != null ? s.getTeleportOption().describeChoice() : null;
			if (choice != null)
			{
				b.append("Kies: <b>").append(esc(choice)).append("</b><br>");
			}
			b.append("Plant: ").append(esc(s.getSeed()));
			if (s.getCompost() != null)
			{
				b.append(" + ").append(esc(s.getCompost()));
			}
			b.append("<br>");
			if (s.getPayment() != null)
			{
				b.append("Betaal gardener: ").append(esc(s.getPayment())).append("<br>");
			}
			if (isCurrent)
			{
				b.append("<i>").append(esc(s.getPatch().getTip())).append("</i>");
			}
		}

		Color bg = isDone ? DONE_BG : isCurrent ? CURRENT_BG : ColorScheme.DARKER_GRAY_COLOR;
		Color fg = isDone ? DONE_TEXT : isCurrent ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR;
		JLabel row = block(b.toString(), bg, fg);
		if (isCurrent)
		{
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 3, 0, ColorScheme.DARK_GRAY_COLOR),
				BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(CURRENT_BORDER, 1),
					BorderFactory.createEmptyBorder(5, 5, 5, 5))));
		}
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText(isDone ? "Klik om weer open te zetten" : "Klik om als gedaan te markeren");
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				plugin.toggleDone(index);
			}
		});
		return row;
	}

	private static JLabel header(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.BRAND_ORANGE);
		l.setBorder(new EmptyBorder(8, 0, 3, 0));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private static JLabel block(String html, Color bg, Color fg)
	{
		JLabel l = new JLabel("<html><div style='width:" + TEXT_WIDTH + "px'>" + html + "</div></html>");
		l.setOpaque(true);
		l.setBackground(bg);
		l.setForeground(fg);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 3, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		l.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		return l;
	}

	private static String list(List<String> items, String bullet)
	{
		StringBuilder b = new StringBuilder();
		for (String s : items)
		{
			b.append(bullet).append(esc(s)).append("<br>");
		}
		return b.toString();
	}

	private static String xp(double value)
	{
		return String.format(Locale.forLanguageTag("nl"), "%,.0f", value);
	}

	private static String esc(String s)
	{
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
