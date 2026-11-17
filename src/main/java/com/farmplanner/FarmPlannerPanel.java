package com.farmplanner;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class FarmPlannerPanel extends PluginPanel
{
	private final JLabel status = new JLabel();
	private final JTextArea current = new JTextArea();
	private final JTextArea output = new JTextArea();
	private final JPanel stepButtons = new JPanel(new GridLayout(1, 2, 5, 0));

	FarmPlannerPanel(FarmPlannerPlugin plugin)
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));

		JLabel title = new JLabel("Farm Run Planner");
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

		JButton prev = new JButton("< Vorige");
		prev.addActionListener(e -> plugin.changeStep(-1));
		JButton next = new JButton("Volgende >");
		next.addActionListener(e -> plugin.changeStep(1));
		stepButtons.add(prev);
		stepButtons.add(next);
		stepButtons.setVisible(false);

		JPanel top = new JPanel(new GridLayout(0, 1, 0, 6));
		top.add(title);
		top.add(runSelect);
		top.add(buttons);
		top.add(status);

		styleText(current);
		current.setFont(FontManager.getRunescapeFont());
		current.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		current.setVisible(false);
		styleText(output);

		JPanel middle = new JPanel(new BorderLayout(0, 6));
		middle.add(current, BorderLayout.NORTH);
		middle.add(stepButtons, BorderLayout.CENTER);

		JPanel north = new JPanel(new BorderLayout(0, 10));
		north.add(top, BorderLayout.NORTH);
		north.add(middle, BorderLayout.SOUTH);

		add(north, BorderLayout.NORTH);
		add(output, BorderLayout.CENTER);

		setBankSeen(false);
		output.setText("Kies een run en klik op 'Maak plan'.\n\n"
			+ "De plugin kijkt naar je bank, inventory en equipment, "
			+ "en zegt wat je moet meenemen en waar je heen moet.");
	}

	private static void styleText(JTextArea area)
	{
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		area.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		area.setFont(FontManager.getRunescapeSmallFont());
	}

	void setBankSeen(boolean seen)
	{
		status.setText(seen ? "Bank gelezen" : "Open je bank één keer");
		status.setForeground(seen ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.PROGRESS_INPROGRESS_COLOR);
	}

	void showMessage(String message)
	{
		current.setVisible(false);
		stepButtons.setVisible(false);
		output.setText(message);
		revalidate();
	}

	void showPlan(FarmPlan plan, int currentIndex)
	{
		List<FarmPlan.Step> active = plan.getActiveSteps();

		// ---- Huidige stap
		if (!active.isEmpty())
		{
			if (currentIndex < active.size())
			{
				FarmPlan.Step s = active.get(currentIndex);
				StringBuilder c = new StringBuilder();
				c.append("NU (").append(currentIndex + 1).append('/').append(active.size()).append("): ")
					.append(s.getPatch().getLocation()).append('\n');
				c.append(describeTeleport(s));
				c.append("Plant: ").append(s.getSeed()).append('\n');
				c.append("Compost: ").append(s.getCompost() != null ? s.getCompost() : "geen");
				current.setText(c.toString());
			}
			else
			{
				current.setText("Run klaar! Alle " + active.size() + " patches zijn geplant.");
			}
			current.setVisible(true);
			stepButtons.setVisible(true);
		}
		else
		{
			current.setVisible(false);
			stepButtons.setVisible(false);
		}

		// ---- Volledig plan
		StringBuilder sb = new StringBuilder();
		sb.append(plan.getRunType()).append("  (Farming ").append(plan.getFarmingLevel()).append(")\n");
		sb.append(plan.getPlantedCount()).append(" patches\n\n");

		if (!active.isEmpty())
		{
			sb.append("NEEM MEE UIT JE BANK\n");
			for (Map.Entry<String, Integer> e : plan.getWithdraw().entrySet())
			{
				sb.append("  ").append(e.getValue()).append(" x ").append(e.getKey()).append('\n');
			}
			sb.append("\nROUTE\n");
			for (int i = 0; i < active.size(); i++)
			{
				FarmPlan.Step s = active.get(i);
				String marker = i < currentIndex ? "[x] " : (i == currentIndex ? ">> " : "");
				sb.append(marker).append(i + 1).append(". ").append(s.getPatch().getLocation()).append('\n');
				sb.append("   ").append(describeTeleport(s).replace("\n", "\n   ").trim()).append('\n');
				sb.append("   Plant: ").append(s.getSeed()).append('\n');
				sb.append("   Compost: ").append(s.getCompost() != null ? s.getCompost() : "geen").append('\n');
				sb.append("   ").append(s.getPatch().getTip()).append("\n\n");
			}

			int skipped = plan.getSteps().size() - active.size();
			if (skipped > 0)
			{
				sb.append("Niet genoeg zaden voor ").append(skipped).append(" patch(es):\n");
				for (FarmPlan.Step s : plan.getSteps())
				{
					if (s.getSeed() == null)
					{
						sb.append("  - ").append(s.getPatch().getLocation()).append('\n');
					}
				}
				sb.append('\n');
			}
		}

		if (!plan.getWarnings().isEmpty())
		{
			sb.append("LET OP\n");
			for (String w : plan.getWarnings())
			{
				sb.append("  ! ").append(w).append('\n');
			}
			sb.append('\n');
		}
		if (!plan.getTips().isEmpty())
		{
			sb.append("TIPS\n");
			for (String t : plan.getTips())
			{
				sb.append("  - ").append(t).append('\n');
			}
		}

		output.setText(sb.toString());
		output.setCaretPosition(0);
		revalidate();
	}

	private static String describeTeleport(FarmPlan.Step s)
	{
		if (s.getTeleport() == null)
		{
			return "Teleport: geen - lopen\n";
		}
		StringBuilder b = new StringBuilder("Teleport: ").append(s.getTeleport()).append('\n');
		String choice = s.getTeleportOption() != null ? s.getTeleportOption().describeChoice() : null;
		if (choice != null)
		{
			b.append("Kies: ").append(choice).append('\n');
		}
		return b.toString();
	}
}
