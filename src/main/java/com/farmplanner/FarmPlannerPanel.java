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
import java.util.function.ToIntFunction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel saplingContent = new JPanel();
	private final JButton saplingPlanButton = new JButton();
	private final JButton saplingClearButton = new JButton();
	private boolean showingSaplingIntro = true;
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
		JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
		header.add(title);
		header.add(status);

		// ---- Tabblad Run
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
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		JPanel runTab = tab(runSelect, planButton, clearButton, content);

		// ---- Tabblad Saplings
		JComboBox<RunType> saplingSelect = new JComboBox<>(new RunType[]{RunType.TREE, RunType.FRUIT_TREE});
		saplingPlanButton.addActionListener(e ->
		{
			showSaplingMessage(lang().t("Working...", "Bezig..."));
			plugin.requestSaplingPlan((RunType) saplingSelect.getSelectedItem());
		});
		saplingClearButton.addActionListener(e ->
		{
			plugin.clearSaplingPlan();
			showSaplingIntro();
		});
		saplingContent.setLayout(new BoxLayout(saplingContent, BoxLayout.Y_AXIS));
		JPanel saplingTab = tab(saplingSelect, saplingPlanButton, saplingClearButton, saplingContent);

		tabs.addTab("Run", runTab);
		tabs.addTab("Saplings", saplingTab);
		tabs.addChangeListener(e -> plugin.setSaplingTabActive(tabs.getSelectedIndex() == 1));

		add(header, BorderLayout.NORTH);
		add(tabs, BorderLayout.CENTER);

		applyLanguage();
		showSaplingIntro();
	}

	/** Eén tabblad: keuzelijst, twee knoppen en een inhoud-deel. */
	private static JPanel tab(JComboBox<RunType> select, JButton make, JButton stop, JPanel body)
	{
		JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 0));
		buttons.add(make);
		buttons.add(stop);
		JPanel top = new JPanel(new GridLayout(0, 1, 0, 6));
		top.add(select);
		top.add(buttons);

		JPanel tab = new JPanel(new BorderLayout(0, 8));
		tab.setBorder(new EmptyBorder(8, 0, 0, 0));
		tab.add(top, BorderLayout.NORTH);
		tab.add(body, BorderLayout.CENTER);
		return tab;
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
		saplingPlanButton.setText(l.t("Make plan", "Maak plan"));
		saplingClearButton.setText(l.t("Stop", "Stop"));
		tabs.setTitleAt(0, l.t("Farm run", "Farm run"));
		tabs.setTitleAt(1, l.t("Saplings", "Saplings"));
		setBankSeen(bankSeen);
		if (showingIntro)
		{
			showIntro();
		}
		if (showingSaplingIntro)
		{
			showSaplingIntro();
		}
	}

	// ------------------------------------------------------------------ tabblad Saplings

	private void showSaplingIntro()
	{
		showSaplingMessage(lang().t(
			"Turn your tree seeds into saplings.<br><br>"
				+ "Choose trees or fruit trees and click 'Make plan'. The plugin picks your best seeds for one "
				+ "inventory, shows what to take and highlights the items for the next action.",
			"Maak saplings van je boomzaden.<br><br>"
				+ "Kies bomen of fruitbomen en klik op 'Maak plan'. De plugin kiest je beste zaden voor één "
				+ "inventory, laat zien wat je moet meenemen en markeert de items voor de volgende actie."));
		showingSaplingIntro = true;
	}

	void showSaplingMessage(String html)
	{
		showingSaplingIntro = false;
		saplingContent.removeAll();
		saplingContent.add(block(html, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
		saplingContent.revalidate();
		saplingContent.repaint();
	}

	void showSaplingPlan(SaplingPlan plan, Map<String, Integer> carried)
	{
		showingSaplingIntro = false;
		saplingContent.removeAll();
		Language l = plan.getLanguage();

		if (!plan.getBatches().isEmpty())
		{
			// ---- Wat ga je maken
			StringBuilder b = new StringBuilder();
			for (SaplingPlan.Batch batch : plan.getBatches())
			{
				b.append(batch.getAmount()).append(" x ").append(esc(batch.getSeed().getItemName())).append("<br>");
			}
			b.append(l.t("XP when planted + checked: <b>", "XP als je ze plant + check-health: <b>"))
				.append(l.number(plan.getXp())).append("</b>");
			saplingContent.add(header(l.t("YOU WILL MAKE", "JE GAAT MAKEN")));
			saplingContent.add(block(b.toString(), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));

			// ---- Voortgang en volgende actie
			SaplingPlan.Progress p = plan.progress(carried);
			String next;
			switch (p.next)
			{
				case PLANT:
					next = l.t("Use a seed on a filled Plant pot (trowel in your inventory).",
						"Gebruik een zaad op een gevulde Plant pot (trowel in je inventory).");
					break;
				case WATER:
					next = l.t("Water your seedlings with the Watering can.",
						"Geef je seedlings water met de Watering can.");
					break;
				case WAIT:
					next = l.t("Wait a few minutes: the watered seedlings turn into saplings (also in your bank).",
						"Wacht een paar minuten: de gewaterde seedlings worden saplings (ook in je bank).");
					break;
				case DONE:
					next = l.t("All done! Your saplings are ready to plant on a tree run.",
						"Klaar! Je saplings kunnen geplant worden tijdens een tree run.");
					break;
				default:
					next = l.t("Take the items below from your bank.", "Haal de items hieronder uit je bank.");
					break;
			}
			StringBuilder prog = new StringBuilder();
			prog.append("<b>").append(l.t("Next: ", "Volgende: ")).append("</b>").append(esc(next)).append("<br><br>");
			prog.append(l.t("Seeds: ", "Zaden: ")).append(p.seeds)
				.append(l.t(" - filled pots: ", " - gevulde potten: ")).append(p.pots).append("<br>");
			prog.append(l.t("Seedlings without water: ", "Seedlings zonder water: ")).append(p.dry).append("<br>");
			prog.append(l.t("Growing: ", "Groeien: ")).append(p.growing).append("<br>");
			prog.append(l.t("Saplings ready: ", "Saplings klaar: ")).append("<b>").append(p.saplings).append("</b>");
			boolean done = p.next == SaplingPlan.Action.DONE;
			saplingContent.add(header(l.t("PROGRESS", "VOORTGANG")));
			saplingContent.add(block(prog.toString(), done ? DONE_BG : CURRENT_BG, done ? DONE_TEXT : Color.WHITE));

			// ---- Meenemen
			addTakeList(saplingContent, l, plan.getWithdraw(), name -> plan.countCarried(name, carried));
		}

		if (!plan.getWarnings().isEmpty())
		{
			saplingContent.add(header(l.t("WARNING", "LET OP")));
			saplingContent.add(block(list(plan.getWarnings(), "! "), ColorScheme.DARKER_GRAY_COLOR,
				new Color(255, 170, 90)));
		}
		if (!plan.getInfo().isEmpty())
		{
			saplingContent.add(header(l.t("OTHER SEEDS", "OVERIGE ZADEN")));
			saplingContent.add(block(list(plan.getInfo(), "- "), ColorScheme.DARKER_GRAY_COLOR,
				ColorScheme.LIGHT_GRAY_COLOR));
		}

		saplingContent.add(header(l.t("HOW IT WORKS", "ZO WERKT HET")));
		saplingContent.add(block(list(java.util.Arrays.asList(
			l.t("1. Use the tree seed on a filled Plant pot (Gardening trowel in your inventory).",
				"1. Gebruik het boomzaad op een gevulde Plant pot (Gardening trowel in je inventory)."),
			l.t("2. Use the Watering can on the seedling.", "2. Gebruik de Watering can op de seedling."),
			l.t("3. After a few minutes the seedling becomes a sapling, also in your bank.",
				"3. Na een paar minuten wordt de seedling een sapling, ook in je bank.")), ""),
			ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));

		saplingContent.revalidate();
		saplingContent.repaint();
	}

	// ------------------------------------------------------------------ run klaar

	/** Alle stappen zijn gedaan: samenvatting, highlights zijn al uit. */
	void showFinished(FarmPlan plan)
	{
		Language l = plan.getLanguage();
		showMessage("<b>" + l.t("Run finished!", "Run klaar!") + "</b><br><br>"
			+ plan.getRunType() + ": " + plan.getActiveSteps().size() + " patches<br>"
			+ l.t("XP now: <b>", "XP nu: <b>") + l.number(plan.getXpNow()) + "</b><br>"
			+ "XP later: <b>" + l.number(plan.getXpLater()) + "</b><br><br>"
			+ l.t("Highlights are off. Click 'Make plan' for a new run.",
				"Highlights zijn uit. Klik op 'Maak plan' voor een nieuwe run."));
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

	void showPlan(FarmPlan plan, boolean[] done, Map<String, Integer> carried, boolean awaitingPayment)
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
				content.add(stepRow(l, i, active.get(i), i < done.length && done[i], i == current,
					i == current && awaitingPayment));
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
			addTakeList(content, l, plan.getWithdraw(), name -> plan.countCarried(name, carried));
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

	/** Meeneemlijst: groen als je het bij je hebt, oranje met have/need als je het nog mist. */
	private static void addTakeList(JPanel target, Language l, Map<String, Integer> withdraw,
		ToIntFunction<String> carriedCount)
	{
		StringBuilder w = new StringBuilder();
		int complete = 0;
		for (Map.Entry<String, Integer> e : withdraw.entrySet())
		{
			int need = e.getValue();
			int have = carriedCount.applyAsInt(e.getKey());
			if (have >= need)
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
		int total = withdraw.size();
		target.add(header(complete == total
			? l.t("TAKE WITH YOU - ALL CARRIED!", "NEEM MEE - ALLES BIJ JE!")
			: l.t("TAKE FROM YOUR BANK (" + complete + "/" + total + " complete)",
				"NEEM MEE UIT JE BANK (" + complete + "/" + total + " compleet)")));
		target.add(block(w.toString(), ColorScheme.DARKER_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR));
	}

	private JLabel stepRow(Language l, int index, FarmPlan.Step s, boolean isDone, boolean isCurrent, boolean paying)
	{
		StringBuilder b = new StringBuilder();
		b.append("<b>").append(index + 1).append(". ").append(esc(s.getPatch().getLocation())).append("</b>");
		if (isDone)
		{
			b.append(l.t(" - DONE", " - GEDAAN"));
		}
		else if (isCurrent)
		{
			b.append(paying ? l.t(" - PAY NOW", " - NU BETALEN") : l.t(" - NOW", " - NU"));
		}
		b.append("<br>");
		if (!isDone)
		{
			int n = 1;
			String plantedMark = paying ? "<font color='" + HAVE_HEX + "'>" : "";
			String plantedEnd = paying ? l.t(" - done", " - gedaan") + "</font>" : "";

			b.append(plantedMark).append(n++).append(". Teleport: ")
				.append(s.getTeleport() != null ? esc(s.getTeleport()) : l.t("none - walk", "geen - lopen"));
			String choice = s.getTeleportOption() != null ? s.getTeleportOption().describeChoice() : null;
			if (choice != null && !paying)
			{
				b.append("<br>&nbsp;&nbsp;&nbsp;").append(l.t("Choose: <b>", "Kies: <b>")).append(esc(choice)).append("</b>");
			}
			b.append(plantedEnd).append("<br>");

			b.append(plantedMark).append(n++).append(". ").append(l.t("Plant: ", "Plant: ")).append(esc(s.getSeed()));
			if (s.getCompost() != null)
			{
				b.append(" + ").append(esc(s.getCompost()));
			}
			b.append(plantedEnd).append("<br>");

			if (s.getPayment() != null)
			{
				String gardener = s.getPatch().getGardener() != null ? s.getPatch().getGardener() : "gardener";
				b.append(paying ? "<b>" : "").append(n++).append(". ")
					.append(l.t("Pay ", "Betaal ")).append(esc(gardener)).append(": ").append(esc(s.getPayment()))
					.append("<br>&nbsp;&nbsp;&nbsp;")
					.append(l.t("Right-click the gardener -> Pay (protects your tree)",
						"Rechtsklik op de gardener -> Pay (beschermt je boom)"))
					.append(paying ? "</b>" : "").append("<br>");
			}
			if (isCurrent)
			{
				b.append("<i>").append(esc(paying
					? l.t("Planted! Now pay the gardener, then the route continues.",
						"Geplant! Betaal nu de gardener, daarna gaat de route verder.")
					: s.getPatch().getTip(l))).append("</i>");
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
