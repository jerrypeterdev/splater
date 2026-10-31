package com.partydamage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.Icon;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicSpinnerUI;
import net.runelite.client.ui.PluginPanel;

class PartyDamagePanel extends PluginPanel
{
    private static final Color BACKGROUND = new Color(34, 34, 34);
    private static final Color INSET = new Color(25, 25, 25);
    private static final Color GOLD = new Color(235, 190, 75);
    private static final Color MUTED = new Color(170, 170, 170);
    private final PartyDamagePlugin plugin;
    private final JPanel rows = column(INSET);
    private final JPanel overallRows = column(INSET);
    private final JPanel encounterRows = column(INSET);
    private final JPanel takenRows = column(INSET);
    private final JPanel encounterTakenRows = column(INSET);
    private final JLabel totalValue = label("0", GOLD);
    private final JLabel dpsValue = label("0.0", GOLD);
    private final JLabel encounterValue = label("0", GOLD);
    private final JLabel encounterDpsValue = label("0.0", GOLD);
    private final JLabel takenValue = label("0", GOLD);
    private final JLabel takenDps = label("0.0", GOLD);
    private final JLabel encounterTakenValue = label("0", GOLD);
    private final JLabel encounterTakenDps = label("0.0", GOLD);
    private final JLabel groupStatus = label("", new Color(90, 210, 100));
    private final JPasswordField code = new JPasswordField();
    private final Timer timer;

    @Inject PartyDamagePanel(PartyDamagePlugin plugin, PartyDamageConfig config)
    {
        this.plugin = plugin;
        setLayout(new BorderLayout()); setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        getScrollPane().setBorder(BorderFactory.createEmptyBorder());
        getScrollPane().getViewport().setBackground(BACKGROUND);
        JPanel content = column(BACKGROUND); add(content, BorderLayout.NORTH);
        JLabel title = label("Splater", GOLD);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        title.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        content.add(title);
        JLabel subtitle = label("The Ultimate Damage Tracker", MUTED);
        subtitle.setHorizontalAlignment(JLabel.CENTER);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        content.add(subtitle); content.add(Box.createVerticalStrut(8));
        content.add(section("GROUP CODE"));
        code.setToolTipText("Group code");
        code.setBackground(INSET); code.setForeground(Color.WHITE); code.setCaretColor(Color.WHITE);
        code.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JPanel codeRow = new JPanel(new BorderLayout(5, 0));
        codeRow.setBackground(BACKGROUND); codeRow.setAlignmentX(LEFT_ALIGNMENT);
        codeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        groupStatus.setPreferredSize(new Dimension(14, 22));
        groupStatus.setHorizontalAlignment(JLabel.CENTER);
        codeRow.add(groupStatus, BorderLayout.WEST); codeRow.add(code, BorderLayout.CENTER);
        content.add(codeRow);
        JPanel groupButtons = new JPanel(new GridLayout(1, 2, 5, 0)); groupButtons.setBackground(BACKGROUND);
        JButton join = button("Join group"), leave = button("Leave group");
        join.addActionListener(e -> {
            String entered = new String(code.getPassword()).trim();
            if (entered.isEmpty()) { groupStatus.setText(""); groupStatus.setToolTipText("Enter a group code"); return; }
            plugin.joinGroup(entered);
            groupStatus.setText("\u2713"); groupStatus.setToolTipText("Group join requested");
        });
        code.addActionListener(e -> join.doClick());
        leave.addActionListener(e -> { plugin.leaveGroup(); groupStatus.setText(""); groupStatus.setToolTipText("Solo mode"); });
        groupButtons.add(join); groupButtons.add(leave); groupButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); groupButtons.setAlignmentX(LEFT_ALIGNMENT);
        content.add(Box.createVerticalStrut(4)); content.add(groupButtons);
        content.add(Box.createVerticalStrut(12)); content.add(section("PLAYERS"));
        content.add(rows);
        addDivider(content);
        content.add(section("OVERALL DAMAGE"));
        JPanel stats = new JPanel(new GridLayout(1, 2, 4, 0)); stats.setBackground(BACKGROUND);
        stats.add(stat("TOTAL DAMAGE", totalValue)); stats.add(stat("DAMAGE / SEC", dpsValue));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 57)); stats.setAlignmentX(LEFT_ALIGNMENT); content.add(stats);
        content.add(Box.createVerticalStrut(6)); content.add(overallRows); content.add(Box.createVerticalStrut(8));
        JButton reset = button("Reset total damage"); reset.setAlignmentX(LEFT_ALIGNMENT);
        reset.addActionListener(e -> { plugin.reset(); refresh(); }); content.add(reset);
        content.add(Box.createVerticalStrut(12)); content.add(section("ENCOUNTER DAMAGE"));
        JPanel encounterStat = new JPanel(new GridLayout(1, 2, 4, 0));
        encounterStat.setBackground(BACKGROUND);
        encounterStat.add(stat("TOTAL DAMAGE", encounterValue));
        encounterStat.add(stat("DAMAGE / SEC", encounterDpsValue));
        encounterStat.setAlignmentX(LEFT_ALIGNMENT);
        encounterStat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 57));
        content.add(encounterStat); content.add(Box.createVerticalStrut(6)); content.add(encounterRows);
        content.add(Box.createVerticalStrut(6));
        JButton resetEncounter = button("Reset encounter"); resetEncounter.setAlignmentX(LEFT_ALIGNMENT);
        resetEncounter.addActionListener(e -> { plugin.resetEncounter(); refresh(); });
        content.add(resetEncounter);
        addDivider(content);
        addTakenSection(content, "Overall damage taken", takenValue, takenDps, takenRows, plugin.overallTaken());
        content.add(Box.createVerticalStrut(12));
        addTakenSection(content, "Encounter damage taken", encounterTakenValue, encounterTakenDps, encounterTakenRows, plugin.encounterTaken());
        addDivider(content);
        content.add(section("SETTINGS"));
        JCheckBox overlay = new JCheckBox("Show damage dealt overlay", config.showOverlay());
        overlay.setBackground(BACKGROUND); overlay.setForeground(Color.WHITE); overlay.setAlignmentX(LEFT_ALIGNMENT);
        overlay.setIcon(checkIcon(false)); overlay.setSelectedIcon(checkIcon(true));
        overlay.addActionListener(e -> plugin.setShowOverlay(overlay.isSelected())); content.add(overlay);
        JCheckBox takenOverlay = new JCheckBox("Show damage taken overlay", config.showTakenOverlay());
        takenOverlay.setBackground(BACKGROUND); takenOverlay.setForeground(Color.WHITE); takenOverlay.setAlignmentX(LEFT_ALIGNMENT);
        takenOverlay.setIcon(checkIcon(false)); takenOverlay.setSelectedIcon(checkIcon(true));
        takenOverlay.addActionListener(e -> plugin.setShowTakenOverlay(takenOverlay.isSelected())); content.add(takenOverlay);
        content.add(Box.createVerticalStrut(6));
        JPanel opacityRow = new JPanel(new BorderLayout(8, 0));
        opacityRow.setBackground(BACKGROUND); opacityRow.setAlignmentX(LEFT_ALIGNMENT);
        opacityRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        opacityRow.add(label("Background opacity (%)", MUTED), BorderLayout.CENTER);
        JSpinner opacity = new JSpinner(new SpinnerNumberModel(Math.max(0, Math.min(100, config.overlayOpacity())), 0, 100, 1));
        opacity.setUI(new BasicSpinnerUI() {
            @Override protected java.awt.Component createNextButton() { return arrowButton(true); }
            @Override protected java.awt.Component createPreviousButton() { return arrowButton(false); }
            private JButton arrowButton(boolean up)
            {
                JButton button = new JButton(new Icon() {
                    public int getIconWidth() { return 9; }
                    public int getIconHeight() { return 7; }
                    public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                        g.setColor(new Color(185, 185, 185));
                        if (up) { g.drawLine(x + 1, y + 5, x + 4, y + 2); g.drawLine(x + 4, y + 2, x + 7, y + 5); }
                        else { g.drawLine(x + 1, y + 2, x + 4, y + 5); g.drawLine(x + 4, y + 5, x + 7, y + 2); }
                    }
                });
                button.setBackground(INSET); button.setBorder(BorderFactory.createEmptyBorder());
                button.setFocusPainted(false); button.setPreferredSize(new Dimension(18, 11));
                if (up) installNextButtonListeners(button); else installPreviousButtonListeners(button);
                return button;
            }
        });
        opacity.setBackground(INSET);
        opacity.setBorder(BorderFactory.createLineBorder(new Color(18, 18, 18)));
        JFormattedTextField opacityField = ((JSpinner.DefaultEditor) opacity.getEditor()).getTextField();
        opacityField.setBackground(INSET); opacityField.setForeground(Color.WHITE); opacityField.setCaretColor(Color.WHITE);
        opacityField.setHorizontalAlignment(JFormattedTextField.RIGHT);
        opacityField.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 3));
        opacity.setPreferredSize(new Dimension(75, 24));
        opacity.addChangeListener(e -> plugin.setOverlayOpacity((Integer) opacity.getValue()));
        opacityRow.add(opacity, BorderLayout.EAST);
        content.add(opacityRow);
        content.add(Box.createVerticalStrut(6));
        content.add(sizeResetButton("Reset damage dealt size", false));
        content.add(Box.createVerticalStrut(4));
        content.add(sizeResetButton("Reset damage taken size", true));
        timer = new Timer(1000, e -> refresh());
    }
    private static Icon checkIcon(boolean checked)
    {
        return new Icon() {
            public int getIconWidth() { return 17; }
            public int getIconHeight() { return 17; }
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                g.setColor(INSET); g.fillRect(x + 1, y + 1, 14, 14);
                g.setColor(checked ? GOLD : new Color(160, 160, 160)); g.drawRect(x + 1, y + 1, 14, 14);
                if (checked) {
                    g.setColor(Color.WHITE);
                    g.drawLine(x + 4, y + 8, x + 7, y + 11); g.drawLine(x + 7, y + 11, x + 12, y + 5);
                    g.drawLine(x + 4, y + 9, x + 7, y + 12); g.drawLine(x + 7, y + 12, x + 12, y + 6);
                }
            }
        };
    }
    private JButton sizeResetButton(String text, boolean taken)
    {
        JButton reset = button(text);
        reset.setAlignmentX(LEFT_ALIGNMENT);
        reset.addActionListener(e -> {
            String name = taken ? "damage taken" : "damage dealt";
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset the " + name + " overlay size?",
                "Reset overlay size", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) plugin.resetOverlaySize(taken);
        });
        return reset;
    }
    private static JPanel column(Color colour)
    {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(colour); panel.setAlignmentX(LEFT_ALIGNMENT); return panel;
    }
    private static JLabel label(String text, Color colour)
    {
        JLabel label = new JLabel(text); label.setForeground(colour); label.setAlignmentX(LEFT_ALIGNMENT); return label;
    }
    private static JLabel section(String text)
    {
        JLabel label = label(text, GOLD); label.setFont(label.getFont().deriveFont(Font.BOLD)); return label;
    }
    private static JButton button(String text)
    {
        JButton button = new JButton(text); button.setBackground(INSET); button.setForeground(Color.WHITE);
        button.setFocusPainted(false); return button;
    }
    private static JPanel stat(String name, JLabel value)
    {
        JPanel panel = column(INSET); panel.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        panel.add(label(name, MUTED)); value.setFont(value.getFont().deriveFont(Font.BOLD, 15f)); panel.add(value); return panel;
    }
    private void addTakenSection(JPanel content, String title, JLabel total, JLabel rate, JPanel playerRows, DamageTakenSeries series)
    {
        content.add(section(title.toUpperCase(Locale.ROOT)));
        JPanel stats = new JPanel(new GridLayout(1, 2, 4, 0));
        stats.setBackground(BACKGROUND); stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.add(stat("TOTAL DAMAGE", total)); stats.add(stat("DAMAGE / SEC", rate));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 57)); content.add(stats);
        content.add(Box.createVerticalStrut(6)); content.add(playerRows);
        content.add(Box.createVerticalStrut(6));
        JButton reset = button(title.startsWith("Overall") ? "Reset overall taken" : "Reset encounter taken");
        reset.setAlignmentX(LEFT_ALIGNMENT);
        reset.addActionListener(e -> { series.reset(); refresh(); });
        content.add(reset);
    }
    private static void addDivider(JPanel content)
    {
        content.add(Box.createVerticalStrut(14));
        JPanel divider = new JPanel(); divider.setBackground(new Color(110, 90, 50));
        divider.setAlignmentX(LEFT_ALIGNMENT); divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(1, 1)); content.add(divider);
        content.add(Box.createVerticalStrut(12));
    }
    void start() { code.setText(plugin.savedGroupCode()); groupStatus.setText(""); groupStatus.setToolTipText("Solo mode"); refresh(); timer.start(); }
    void stop() { timer.stop(); }
    private void refresh()
    {
        Map<String, Long> snapshot = plugin.snapshot();
        List<Map.Entry<String, Long>> entries = new ArrayList<>(snapshot.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
        long total = entries.stream().mapToLong(Map.Entry::getValue).sum();
        totalValue.setText(String.format(Locale.ROOT, "%,d", total));
        dpsValue.setText(String.format(Locale.ROOT, "%.1f", total / plugin.elapsedSeconds()));
        Map<String, Long> playerNames = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        playerNames.putAll(snapshot);
        plugin.encounterSnapshot().forEach(playerNames::putIfAbsent);
        plugin.overallTaken().snapshot().forEach(playerNames::putIfAbsent);
        plugin.encounterTaken().snapshot().forEach(playerNames::putIfAbsent);
        renderRows(rows, new ArrayList<>(playerNames.entrySet()), 0, true);
        renderRows(overallRows, entries, total, false);
        Map<String, Long> encounter = plugin.encounterSnapshot();
        List<Map.Entry<String, Long>> encounterEntries = new ArrayList<>(encounter.entrySet());
        encounterEntries.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
        long encounterTotal = encounterEntries.stream().mapToLong(Map.Entry::getValue).sum();
        encounterValue.setText(String.format(Locale.ROOT, "%,d", encounterTotal));
        encounterDpsValue.setText(String.format(Locale.ROOT, "%.1f", encounterTotal / plugin.encounterElapsedSeconds()));
        renderRows(encounterRows, encounterEntries, encounterTotal, false);
        refreshTaken(plugin.overallTaken(), takenValue, takenDps, takenRows);
        refreshTaken(plugin.encounterTaken(), encounterTakenValue, encounterTakenDps, encounterTakenRows);
    }
    private void refreshTaken(DamageTakenSeries series, JLabel totalValue, JLabel rateValue, JPanel playerRows)
    {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(series.snapshot().entrySet());
        entries.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
        long total = entries.stream().mapToLong(Map.Entry::getValue).sum();
        totalValue.setText(String.format(Locale.ROOT, "%,d", total));
        rateValue.setText(String.format(Locale.ROOT, "%.1f", total / series.seconds()));
        renderRows(playerRows, entries, total, false);
    }
    private void renderRows(JPanel destination, List<Map.Entry<String, Long>> entries, long total, boolean coloursOnly)
    {
        destination.removeAll();
        if (entries.isEmpty()) destination.add(label("Players appear after their first hit.", MUTED));
        for (Map.Entry<String, Long> entry : entries)
        {
            String name = entry.getKey();
            JPanel row = new JPanel(new BorderLayout(8, 0)); row.setBackground(INSET);
            row.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
            JButton picker = new JButton(new javax.swing.Icon() {
                public int getIconWidth() { return 16; }
                public int getIconHeight() { return 16; }
                public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                    g.setColor(plugin.colourFor(name)); g.fillOval(x + 3, y + 3, 10, 10);
                    g.setColor(Color.LIGHT_GRAY); g.drawOval(x + 3, y + 3, 10, 10);
                }
            });
            picker.setToolTipText("Change colour for " + name); picker.setBackground(INSET);
            picker.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); picker.setFocusPainted(false);
            picker.addActionListener(e -> chooseColour(name));
            JLabel player = label(name, plugin.colourFor(name));
            JLabel value = label(String.format(Locale.ROOT, "%,d  |  %.1f%%", entry.getValue(), total == 0 ? 0 : 100.0 * entry.getValue() / total), MUTED);
            value.setFont(value.getFont().deriveFont(Font.BOLD));
            if (coloursOnly) row.add(picker, BorderLayout.WEST);
            row.add(player, BorderLayout.CENTER);
            if (!coloursOnly) row.add(value, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26)); destination.add(row);
        }
        destination.revalidate(); destination.repaint();
    }
    private void chooseColour(String name)
    {
        Color selected = JColorChooser.showDialog(this, "Colour for " + name, plugin.colourFor(name));
        if (selected != null) { plugin.setColour(name, selected); refresh(); }
    }
}
