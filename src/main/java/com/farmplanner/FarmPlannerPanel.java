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
	private final JButton planButton = new JButton();
	private final JButton clearButton = new JButton();
	private boolean bankSeen;
	/** Laatst getoonde plan, zodat de tekst bij een taalwissel ververst kan worden. */
	private boolean showingIntro = true;

	FarmPlannerPanel(FarmPlannerPlugin plugin)
	{
		this.plugin = plugin;
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));

		JLabel title = new JLabel("Bank to Patch");
		title.setFont(FontManager.getRunescapeBoldFont());

		JComboBox<RunType> runSelect = new JComboBox<>(RunType.values());

		planButton.addActionListener(e ->
		{
			showMessage(lang().t("Working...", "Bezig..."));
			plugin.requestPlan((RunType) runSelect.getSelectedItem());
		});

		clearButton.addActionListener(e ->
		{
			plugin.clearPlan();
			showMessage(lang().t("Run stopped. Highlights are off.", "Run gestopt. Highlights zijn uit."));
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

		applyLanguage();
	}

	private Language lang()
	{
		return plugin.getLanguage();
	}

	/** Zet de vaste teksten (knoppen, status, intro) in de gekozen taal. */
	void applyLanguage()
	{
		Language l = lang();
		planButton.setText(l.t("Make plan", "Maak plan"));
		clearButton.setText(l.t("Stop run", "Stop run"));
		setBankSeen(bankSeen);
		if (showingIntro)
		{
			showIntro();
		}
	}

	private void showIntro()
	{
		showMessage(lang().t(
			"Choose a run and click 'Make plan'.<br><br>"
				+ "The plugin looks at your bank, inventory and equipment, "
				+ "and tells you what to take and where to go.",
			"Kies een run en klik op 'Maak plan'.<br><br>"
				+ "De plugin kijkt naar je bank, inventory en equipment, "
				+ "en zegt wat je moet meenemen en waar je heen moet."));
		showingIntro = true;
	}

	void setBankSeen(boolean seen)
	{
		bankSeen = seen;
		Language l = lang();
		status.setText(seen ? l.t("Bank read", "Bank gelezen") : l.t("Open your bank once", "Open je bank één keer"));
		status.setForeground(seen ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.PROGRESS_INPROGRESS_COLOR);
	}

	void showMessage(String html)
	{
		showingIntro = false;
		content.removeAll();
		content.add(block(html, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		content.revalidate();
		content.repaint();
	}

	void showPlan(FarmPlan plan, boolean[] done, Map<String, Integer> carried)
	{
		showingIntro = false;
		content.removeAll();
		Language l = plan.getLanguage();
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
			head.append(l.t("Progress: ", "Voortgang: ")).append(doneCount).append(" / ").append(active.size())
				.append(" patches<br>");
			head.append(l.t("XP now: <b>", "XP nu: <b>")).append(l.number(plan.getXpNow())).append("</b> ")
				.append(l.t("(planting + compost)", "(planten + compost)")).append("<br>");
			head.append("XP later: <b>").append(l.number(plan.getXpLater())).append("</b> ")
				.append(plan.getRunType() == RunType.HERB
					? l.t("(harvest, estimated)", "(oogsten, geschat)")
					: l.t("(check-health next run)", "(check-health volgende run)"));
		}
		else
		{
			head.append(l.t("Nothing to plant.", "Niets te planten."));
		}
		content.add(block(head.toString(), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));

		// ---- Route met groene (gedaan) en gele (huidige) stappen
		if (!active.isEmpty())
		{
			content.add(header(current < 0
				? l.t("ROUTE - FINISHED!", "ROUTE - KLAAR!")
				: l.t("ROUTE (click to tick off)", "ROUTE (klik om af te vinken)")));
			for (int i = 0; i < active.size(); i++)
			{
				content.add(stepRow(l, i, active.get(i), i < done.length && done[i], i == current));
			}

			JButton back = new JButton(l.t("< Back", "< Terug"));
			back.addActionListener(e -> plugin.changeStep(-1));
			JButton next = new JButton(l.t("Done >", "Klaar >"));
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
				? l.t("TAKE WITH YOU - ALL CARRIED!", "NEEM MEE - ALLES BIJ JE!")
				: l.t("TAKE FROM YOUR BANK (" + complete + "/" + total + " complete)",
					"NEEM MEE UIT JE BANK (" + complete + "/" + total + " compleet)")));
			content.add(block(w.toString(), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		}

		// ---- Waarschuwingen
		if (!plan.getWarnings().isEmpty())
		{
			content.add(header(l.t("WARNING", "LET OP")));
			content.add(block(list(plan.getWarnings(), "! "), ColorScheme.DARKER_GRAY_COLOR, new Color(255, 170, 90)));
		}

		// ---- Seeds
		if (!plan.getSeedOverview().isEmpty())
		{
			content.add(header(l.t("YOUR SEEDS", "JOUW SEEDS")));
			content.add(block(list(plan.getSeedOverview(), "- "), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		}

		// ---- Saplings maken
		if (!plan.getSaplingAdvice().isEmpty())
		{
			content.add(header(l.t("MAKING SAPLINGS", "SAPLINGS MAKEN")));
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

	private JLabel stepRow(Language l, int index, FarmPlan.Step s, boolean isDone, boolean isCurrent)
	{
		StringBuilder b = new StringBuilder();
		b.append("<b>").append(index + 1).append(". ").append(esc(s.getPatch().getLocation())).append("</b>");
		if (isDone)
		{
			b.append(l.t(" - DONE", " - GEDAAN"));
		}
		else if (isCurrent)
		{
			b.append(l.t(" - NOW", " - NU"));
		}
		b.append("<br>");
		if (!isDone)
		{
			b.append("Teleport: ").append(s.getTeleport() != null ? esc(s.getTeleport()) : l.t("none - walk", "geen - lopen"))
				.append("<br>");
			String choice = s.getTeleportOption() != null ? s.getTeleportOption().describeChoice() : null;
			if (choice != null)
			{
				b.append(l.t("Choose: <b>", "Kies: <b>")).append(esc(choice)).append("</b><br>");
			}
			b.append("Plant: ").append(esc(s.getSeed()));
			if (s.getCompost() != null)
			{
				b.append(" + ").append(esc(s.getCompost()));
			}
			b.append("<br>");
			if (s.getPayment() != null)
			{
				b.append(l.t("Pay gardener: ", "Betaal gardener: ")).append(esc(s.getPayment())).append("<br>");
			}
			if (isCurrent)
			{
				b.append("<i>").append(esc(s.getPatch().getTip(l))).append("</i>");
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
		row.setToolTipText(isDone
			? l.t("Click to mark as not done", "Klik om weer open te zetten")
			: l.t("Click to mark as done", "Klik om als gedaan te markeren"));
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

	private static String esc(String s)
	{
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
