package Main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class CodonGame extends JFrame {

    public int ticks;
    public Timer timer;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Map<Character, Color> BASE_BG = new HashMap<>();
    private static final Map<Character, Color> BASE_FG = new HashMap<>();
    static {
        BASE_BG.put('A', new Color(0xe74c3c));
        BASE_BG.put('G', new Color(0x2980b9));
        BASE_BG.put('C', new Color(0x8e44ad));
        BASE_BG.put('U', new Color(0xf1c40f));
        BASE_FG.put('A', Color.WHITE);
        BASE_FG.put('G', Color.WHITE);
        BASE_FG.put('C', Color.WHITE);
        BASE_FG.put('U', new Color(0x333333));
    }

    private static final Color BG       = new Color(0x1a1a2e);
    private static final Color PANEL_BG = new Color(0x16213e);
    private static final Color SLOT_IDLE = new Color(0x555555);
    private static final Color SLOT_OK   = new Color(0x2ecc71);
    private static final Color SLOT_OVER = new Color(0xf39c12);

    // ── Full codon → anticodon → amino acid table ─────────────────────────────
    // Each entry: { codon, anticodon, aminoAcidFull, aminoAcid3Letter }
    private static final Object[][] ALL_PAIRS = {
        // Phe
        {new char[]{'U','U','U'}, new char[]{'A','A','A'}, "Phenylalanine",   "Phe"},
        {new char[]{'U','U','C'}, new char[]{'A','A','G'}, "Phenylalanine",   "Phe"},
        // Leu
        {new char[]{'U','U','A'}, new char[]{'A','A','U'}, "Leucine",         "Leu"},
        {new char[]{'U','U','G'}, new char[]{'A','A','C'}, "Leucine",         "Leu"},
        {new char[]{'C','U','U'}, new char[]{'G','A','A'}, "Leucine",         "Leu"},
        {new char[]{'C','U','C'}, new char[]{'G','A','G'}, "Leucine",         "Leu"},
        {new char[]{'C','U','A'}, new char[]{'G','A','U'}, "Leucine",         "Leu"},
        {new char[]{'C','U','G'}, new char[]{'G','A','C'}, "Leucine",         "Leu"},
        // Ile
        {new char[]{'A','U','U'}, new char[]{'U','A','A'}, "Isoleucine",      "Ile"},
        {new char[]{'A','U','C'}, new char[]{'U','A','G'}, "Isoleucine",      "Ile"},
        {new char[]{'A','U','A'}, new char[]{'U','A','U'}, "Isoleucine",      "Ile"},
        // Met (start)
        {new char[]{'A','U','G'}, new char[]{'U','A','C'}, "Methionine",      "Met"},
        // Val
        {new char[]{'G','U','U'}, new char[]{'C','A','A'}, "Valine",          "Val"},
        {new char[]{'G','U','C'}, new char[]{'C','A','G'}, "Valine",          "Val"},
        {new char[]{'G','U','A'}, new char[]{'C','A','U'}, "Valine",          "Val"},
        {new char[]{'G','U','G'}, new char[]{'C','A','C'}, "Valine",          "Val"},
        // Ser
        {new char[]{'U','C','U'}, new char[]{'A','G','A'}, "Serine",          "Ser"},
        {new char[]{'U','C','C'}, new char[]{'A','G','G'}, "Serine",          "Ser"},
        {new char[]{'U','C','A'}, new char[]{'A','G','U'}, "Serine",          "Ser"},
        {new char[]{'U','C','G'}, new char[]{'A','G','C'}, "Serine",          "Ser"},
        // Pro
        {new char[]{'C','C','U'}, new char[]{'G','G','A'}, "Proline",         "Pro"},
        {new char[]{'C','C','C'}, new char[]{'G','G','G'}, "Proline",         "Pro"},
        {new char[]{'C','C','A'}, new char[]{'G','G','U'}, "Proline",         "Pro"},
        {new char[]{'C','C','G'}, new char[]{'G','G','C'}, "Proline",         "Pro"},
        // Thr
        {new char[]{'A','C','U'}, new char[]{'U','G','A'}, "Threonine",       "Thr"},
        {new char[]{'A','C','C'}, new char[]{'U','G','G'}, "Threonine",       "Thr"},
        {new char[]{'A','C','A'}, new char[]{'U','G','U'}, "Threonine",       "Thr"},
        {new char[]{'A','C','G'}, new char[]{'U','G','C'}, "Threonine",       "Thr"},
        // Ala
        {new char[]{'G','C','U'}, new char[]{'C','G','A'}, "Alanine",         "Ala"},
        {new char[]{'G','C','C'}, new char[]{'C','G','G'}, "Alanine",         "Ala"},
        {new char[]{'G','C','A'}, new char[]{'C','G','U'}, "Alanine",         "Ala"},
        {new char[]{'G','C','G'}, new char[]{'C','G','C'}, "Alanine",         "Ala"},
        // Tyr
        {new char[]{'U','A','U'}, new char[]{'A','U','A'}, "Tyrosine",        "Tyr"},
        {new char[]{'U','A','C'}, new char[]{'A','U','G'}, "Tyrosine",        "Tyr"},
        // His
        {new char[]{'C','A','U'}, new char[]{'G','U','A'}, "Histidine",       "His"},
        {new char[]{'C','A','C'}, new char[]{'G','U','G'}, "Histidine",       "His"},
        // Gln
        {new char[]{'C','A','A'}, new char[]{'G','U','U'}, "Glutamine",       "Gln"},
        {new char[]{'C','A','G'}, new char[]{'G','U','C'}, "Glutamine",       "Gln"},
        // Asn
        {new char[]{'A','A','U'}, new char[]{'U','U','A'}, "Asparagine",      "Asn"},
        {new char[]{'A','A','C'}, new char[]{'U','U','G'}, "Asparagine",      "Asn"},
        // Lys
        {new char[]{'A','A','A'}, new char[]{'U','U','U'}, "Lysine",          "Lys"},
        {new char[]{'A','A','G'}, new char[]{'U','U','C'}, "Lysine",          "Lys"},
        // Asp
        {new char[]{'G','A','U'}, new char[]{'C','U','A'}, "Aspartic Acid",   "Asp"},
        {new char[]{'G','A','C'}, new char[]{'C','U','G'}, "Aspartic Acid",   "Asp"},
        // Glu
        {new char[]{'G','A','A'}, new char[]{'C','U','U'}, "Glutamic Acid",   "Glu"},
        {new char[]{'G','A','G'}, new char[]{'C','U','C'}, "Glutamic Acid",   "Glu"},
        // Cys
        {new char[]{'U','G','U'}, new char[]{'A','C','A'}, "Cysteine",        "Cys"},
        {new char[]{'U','G','C'}, new char[]{'A','C','G'}, "Cysteine",        "Cys"},
        // Trp
        {new char[]{'U','G','G'}, new char[]{'A','C','C'}, "Tryptophan",      "Trp"},
        // Arg
        {new char[]{'C','G','U'}, new char[]{'G','C','A'}, "Arginine",        "Arg"},
        {new char[]{'C','G','C'}, new char[]{'G','C','G'}, "Arginine",        "Arg"},
        {new char[]{'C','G','A'}, new char[]{'G','C','U'}, "Arginine",        "Arg"},
        {new char[]{'C','G','G'}, new char[]{'G','C','C'}, "Arginine",        "Arg"},
        {new char[]{'A','G','A'}, new char[]{'U','C','U'}, "Arginine",        "Arg"},
        {new char[]{'A','G','G'}, new char[]{'U','C','C'}, "Arginine",        "Arg"},
        // Ser (AGU/AGC)
        {new char[]{'A','G','U'}, new char[]{'U','C','A'}, "Serine",          "Ser"},
        {new char[]{'A','G','C'}, new char[]{'U','C','G'}, "Serine",          "Ser"},
        // Gly
        {new char[]{'G','G','U'}, new char[]{'C','C','A'}, "Glycine",         "Gly"},
        {new char[]{'G','G','C'}, new char[]{'C','C','G'}, "Glycine",         "Gly"},
        {new char[]{'G','G','A'}, new char[]{'C','C','U'}, "Glycine",         "Gly"},
        {new char[]{'G','G','G'}, new char[]{'C','C','C'}, "Glycine",         "Gly"},
    };

    // ── Per-round data (re-sampled each game) ─────────────────────────────────
    private char[][]  CODONS     = new char[3][];
    private char[][]  ANTICODONS = new char[3][];
    private String[]  AMINO_FULL = new String[3];
    private String[]  AMINO_3    = new String[3];
    private char[][]  DISTRACTORS;

    // ── Leaderboard (top 3 fastest completion times in seconds) ───────────────
    private static final int LB_SIZE = 3;
    private final String[] lbNames  = new String[LB_SIZE];
    private final double[] lbTimes  = new double[LB_SIZE];
    {
        Arrays.fill(lbNames, "---");
        Arrays.fill(lbTimes, Double.MAX_VALUE);
    }

    // ── UI state ──────────────────────────────────────────────────────────────
    private final boolean[] matched = new boolean[3];

    private JLabel scoreLabel;
    private JLabel timerLabel;
    private JLabel feedbackLabel;
    private JLabel dnaInfoLabel;
    private JPanel leaderboardPanel;
    private final SlotPanel[] slots = new SlotPanel[3];
    private JPanel tileArea;
    private final List<TilePanel> tiles = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    public CodonGame() {
        super("Codon–Anticodon Matching Game");
        samplePairs();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(BG);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildTileArea(),  BorderLayout.SOUTH);

        timer = new Timer(17, e -> {
            ticks++;
            timerLabel.setText("Time: " + String.format("%.1f", ticks / 60.0) + "s");
        });
        timer.start();

        pack();
        setMinimumSize(new Dimension(820, 620));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Sample 3 random codon pairs each round ────────────────────────────────
    private void samplePairs() {
        List<Object[]> pool = new ArrayList<>(Arrays.asList(ALL_PAIRS));
        Collections.shuffle(pool);
        for (int i = 0; i < 3; i++) {
            CODONS[i]     = (char[]) pool.get(i)[0];
            ANTICODONS[i] = (char[]) pool.get(i)[1];
            AMINO_FULL[i] = (String)  pool.get(i)[2];
            AMINO_3[i]    = (String)  pool.get(i)[3];
        }
        // Distractors: anticodons from the next 8 pairs
        List<char[]> dist = new ArrayList<>();
        for (int i = 3; i < Math.min(11, pool.size()); i++)
            dist.add((char[]) pool.get(i)[1]);
        DISTRACTORS = dist.toArray(new char[0][]);
    }

    private String getDnaInfoText() {
        StringBuilder sb = new StringBuilder("mRNA: ");
        for (int i = 0; i < 3; i++) {
            if (i > 0) sb.append(" · ");
            sb.append(new String(CODONS[i]));
        }
        return sb.toString();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 0, 16));

        JLabel title = new JLabel("🧬 Codon–Anticodon Matching", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(0xe0e0ff));

        JLabel sub = new JLabel("Drag the correct anticodon tile onto each mRNA codon slot", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(0x888888));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.setBackground(BG);
        top.add(title);
        top.add(sub);

        dnaInfoLabel = new JLabel(getDnaInfoText(), SwingConstants.CENTER);
        dnaInfoLabel.setFont(new Font("Monospaced", Font.PLAIN, 13));
        dnaInfoLabel.setForeground(new Color(0x888888));
        dnaInfoLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        scoreLabel.setForeground(new Color(0xf1c40f));

        timerLabel = new JLabel("Time: 0.0s");
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        timerLabel.setForeground(new Color(0x2ecc71));

        JButton rulesBtn = makeButton("Pairing Rules", new Color(0x8e44ad));
        rulesBtn.addActionListener(e -> showRules());

        JButton lbBtn = makeButton("🏆 Leaderboard", new Color(0x27ae60));
        lbBtn.addActionListener(e -> showLeaderboard());

        JButton resetBtn = makeButton("🔄 Reset", new Color(0x2c3e50));
        resetBtn.addActionListener(e -> reset());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        btnRow.setBackground(BG);
        btnRow.add(scoreLabel);
        btnRow.add(timerLabel);
        btnRow.add(rulesBtn);
        btnRow.add(lbBtn);
        btnRow.add(resetBtn);

        feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        feedbackLabel.setOpaque(true);
        feedbackLabel.setBackground(BG);
        feedbackLabel.setForeground(BG);
        feedbackLabel.setBorder(new EmptyBorder(4, 12, 4, 12));

        JPanel header = new JPanel(new GridLayout(5, 1, 0, 2));
        header.setBackground(BG);
        header.add(top);
        header.add(dnaInfoLabel);
        header.add(btnRow);
        header.add(feedbackLabel);

        p.add(header, BorderLayout.CENTER);
        return p;
    }

    // ── Codon slots ───────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 32, 16));
        p.setBackground(BG);
        for (int i = 0; i < 3; i++) {
            slots[i] = new SlotPanel(i);
            p.add(slots[i]);
        }
        return p;
    }

    // ── Tile area ─────────────────────────────────────────────────────────────
    private JPanel buildTileArea() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(0, 16, 16, 16));

        JLabel lbl = new JLabel("tRNA Anticodon Tiles — drag to match:", SwingConstants.CENTER);
        lbl.setForeground(new Color(0x888888));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        wrapper.add(lbl, BorderLayout.NORTH);

        tileArea = new JPanel(new WrapLayout(FlowLayout.CENTER, 10, 8));
        tileArea.setBackground(PANEL_BG);
        tileArea.setBorder(new CompoundBorder(
                new LineBorder(new Color(0x333333), 1, true),
                new EmptyBorder(8, 8, 8, 8)));
        wrapper.add(tileArea, BorderLayout.CENTER);

        populateTiles();
        return wrapper;
    }

    private void populateTiles() {
        tileArea.removeAll();
        tiles.clear();

        List<char[]> options = new ArrayList<>();
        for (char[] ac : ANTICODONS) options.add(ac.clone());
        for (char[] d  : DISTRACTORS) options.add(d);
        Collections.shuffle(options);

        for (char[] bases : options) {
            TilePanel tile = new TilePanel(bases);
            tiles.add(tile);
            tileArea.add(tile);
        }
        tileArea.revalidate();
        tileArea.repaint();
    }

    // ── Feedback ──────────────────────────────────────────────────────────────
    private javax.swing.Timer feedbackTimer;

    void showFeedback(boolean ok, String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setForeground(ok ? new Color(0x2ecc71) : new Color(0xe74c3c));
        feedbackLabel.setBackground(ok ? new Color(0x1a3a2a) : new Color(0x3a1a1a));
        if (feedbackTimer != null && feedbackTimer.isRunning()) feedbackTimer.stop();
        feedbackTimer = new javax.swing.Timer(3000, e -> {
            feedbackLabel.setText(" ");
            feedbackLabel.setBackground(BG);
        });
        feedbackTimer.setRepeats(false);
        feedbackTimer.start();
    }

    // ── Drop logic ────────────────────────────────────────────────────────────
    void attemptDrop(char[] anticodon, int slotIdx, TilePanel tile) {
        if (matched[slotIdx]) return;

        char[] correct = ANTICODONS[slotIdx];
        boolean ok = Arrays.equals(anticodon, correct);

        if (ok) {
            matched[slotIdx] = true;
            slots[slotIdx].setMatched(anticodon);
            tileArea.remove(tile);
            tiles.remove(tile);
            tileArea.revalidate();
            tileArea.repaint();
            showFeedback(true, "✅ " + new String(anticodon) + " pairs with " + new String(CODONS[slotIdx])
                    + "  →  " + AMINO_FULL[slotIdx] + " (" + AMINO_3[slotIdx] + ")");

            boolean allDone = true;
            for (boolean m : matched) if (!m) { allDone = false; break; }
            if (allDone) showWin();
        } else {
            slots[slotIdx].shake();
            ticks += 30;
            showFeedback(false, "❌ " + new String(anticodon) + " doesn't pair with "
                    + new String(CODONS[slotIdx]) + ". Remember: A↔U, G↔C only! +0.5 Seconds");
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────
    private void reset() {
        samplePairs();
        Arrays.fill(matched, false);
        ticks = 0;
        scoreLabel.setText("Score: 0");
        timerLabel.setText("Time: 0.0s");
        feedbackLabel.setText(" ");
        feedbackLabel.setBackground(BG);
        dnaInfoLabel.setText(getDnaInfoText());

        // Rebuild slots with new codon data
        Component centerPanel = ((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        if (centerPanel instanceof JPanel cp) {
            cp.removeAll();
            for (int i = 0; i < 3; i++) {
                slots[i] = new SlotPanel(i);
                cp.add(slots[i]);
            }
            cp.revalidate();
            cp.repaint();
        }

        populateTiles();
        timer.restart();
    }

    // ── Win + leaderboard check ───────────────────────────────────────────────
    private void showWin() {
        timer.stop();
        double elapsed = ticks / 60.0;

        // Check if qualifies for leaderboard
        boolean qualifies = false;
        for (double t : lbTimes) if (elapsed < t) { qualifies = true; break; }

        String name = null;
        if (qualifies) {
            JTextField tf = new JTextField(12);
            JPanel panel = new JPanel(new BorderLayout(6, 6));
            panel.setBackground(Color.WHITE);
            JLabel msg = new JLabel("<html><center>🎉 You completed it in <b>"
                    + String.format("%.1f", elapsed) + "s</b>!<br>You made the leaderboard! Enter your name:</center></html>");
            msg.setFont(new Font("SansSerif", Font.PLAIN, 13));
            panel.add(msg, BorderLayout.NORTH);
            panel.add(tf,  BorderLayout.CENTER);
            int res = JOptionPane.showConfirmDialog(this, panel, "You Win!", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                name = tf.getText().trim();
                if (name.isEmpty()) name = "Player";
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "<html><center><font size=5>🎉 All codons matched!</font><br><br>"
                    + "Time: <b>" + String.format("%.1f", elapsed) + "s</b></center></html>",
                    "You Win!", JOptionPane.INFORMATION_MESSAGE);
        }

        if (name != null) {
            insertLeaderboard(name, elapsed);
            showLeaderboard();
        }
    }

    private void insertLeaderboard(String name, double time) {
        // Insert into sorted leaderboard (ascending = faster is better)
        for (int i = 0; i < LB_SIZE; i++) {
            if (time < lbTimes[i]) {
                // Shift down
                for (int j = LB_SIZE - 1; j > i; j--) {
                    lbTimes[j] = lbTimes[j - 1];
                    lbNames[j] = lbNames[j - 1];
                }
                lbTimes[i] = time;
                lbNames[i] = name;
                break;
            }
        }
    }

    private void showLeaderboard() {
        StringBuilder sb = new StringBuilder(
                "<html><center><font size=4><b>🏆 Top 3 Fastest Times</b></font><br><br>");
        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < LB_SIZE; i++) {
            sb.append(medals[i]).append("  ");
            if (lbTimes[i] == Double.MAX_VALUE) {
                sb.append("<font color='gray'>---</font>");
            } else {
                sb.append("<b>").append(lbNames[i]).append("</b>")
                  .append(" &nbsp;—&nbsp; ")
                  .append(String.format("%.1f", lbTimes[i])).append("s");
            }
            sb.append("<br><br>");
        }
        sb.append("</center></html>");
        JOptionPane.showMessageDialog(this, sb.toString(), "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Rules dialog ──────────────────────────────────────────────────────────
    private void showRules() {
        String[][] pairs = {
            {"A","U","Adenine ↔ Uracil"},
            {"U","A","Uracil ↔ Adenine"},
            {"G","C","Guanine ↔ Cytosine"},
            {"C","G","Cytosine ↔ Guanine"}
        };
        StringBuilder sb = new StringBuilder("<html><b>Watson-Crick Base Pairing Rules:</b><br><br>");
        for (String[] row : pairs)
            sb.append("&nbsp;&nbsp;<b>").append(row[0]).append("</b> pairs with <b>")
              .append(row[1]).append("</b> &nbsp;(").append(row[2]).append(")<br>");
        sb.append("<br><font color='gray' size='3'>T (Thymine) in DNA → U (Uracil) in mRNA</font></html>");
        JOptionPane.showMessageDialog(this, sb.toString(), "Pairing Rules", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JLabel makeBaseLabel(char base, int size) {
        JLabel l = new JLabel(String.valueOf(base), SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, (int) (size * 0.45)));
        l.setPreferredSize(new Dimension(size, size));
        l.setOpaque(true);
        l.setBackground(BASE_BG.getOrDefault(base, Color.GRAY));
        l.setForeground(BASE_FG.getOrDefault(base, Color.WHITE));
        l.setBorder(new LineBorder(new Color(255, 255, 255, 60), 2, true));
        return l;
    }

    // =========================================================================
    // SlotPanel – mRNA codon + drop zone + amino acid label
    // =========================================================================
    class SlotPanel extends JPanel {
        private final int idx;
        private JPanel dropZone;
        private boolean isMatched = false;

        SlotPanel(int idx) {
            this.idx = idx;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(BG);
            setAlignmentX(CENTER_ALIGNMENT);
            buildUI();
        }

        private void buildUI() {
            removeAll();

            // ── Amino acid label (top) ────────────────────────────────────────
            JPanel aminoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            aminoPanel.setBackground(BG);

            if (isMatched) {
                // Show full amino acid info
                JLabel aminoLabel = new JLabel(AMINO_3[idx] + " — " + AMINO_FULL[idx]);
                aminoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                aminoLabel.setForeground(new Color(0x2ecc71));
                aminoPanel.add(aminoLabel);
            } else {
                // Show placeholder
                JLabel aminoLabel = new JLabel("Amino Acid: ?");
                aminoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                aminoLabel.setForeground(new Color(0x555555));
                aminoPanel.add(aminoLabel);
            }

            // ── Drop zone ─────────────────────────────────────────────────────
            dropZone = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
            dropZone.setBackground(PANEL_BG);
            dropZone.setPreferredSize(new Dimension(164, 68));
            dropZone.setBorder(new DashedBorder(SLOT_IDLE, 2));

            if (isMatched) {
                char[] ac = ANTICODONS[idx];
                for (char b : ac) dropZone.add(makeBaseLabel(b, 44));
                dropZone.setBorder(new DashedBorder(SLOT_OK, 2));
                dropZone.setBackground(new Color(0x1a3a2a));
                JLabel check = new JLabel("✓");
                check.setForeground(SLOT_OK);
                check.setFont(new Font("SansSerif", Font.BOLD, 18));
                dropZone.add(check);
            } else {
                JLabel hint = new JLabel("Drop anticodon here");
                hint.setForeground(new Color(0x555555));
                hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
                dropZone.add(hint);
                setupDropTarget();
            }

            JPanel dropWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dropWrapper.setBackground(BG);
            dropWrapper.add(dropZone);

            // ── Bond lines ────────────────────────────────────────────────────
            JPanel bonds = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            bonds.setBackground(BG);
            bonds.setPreferredSize(new Dimension(164, 14));
            if (isMatched) {
                for (int i = 0; i < 3; i++) {
                    JPanel line = new JPanel();
                    line.setBackground(SLOT_OK);
                    line.setPreferredSize(new Dimension(4, 14));
                    bonds.add(line);
                }
            }

            // ── Codon bases ───────────────────────────────────────────────────
            JPanel codonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            codonRow.setBackground(BG);
            for (char b : CODONS[idx]) codonRow.add(makeBaseLabel(b, 44));

            // ── Slot label ────────────────────────────────────────────────────
            JLabel lbl = new JLabel(
                    "Codon " + (idx + 1) + (idx == 0 ? "  (5')" : idx == 2 ? "  (3')" : ""),
                    SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lbl.setForeground(new Color(0x888888));
            lbl.setAlignmentX(CENTER_ALIGNMENT);

            // Layout: amino acid label on top, then drop zone, bonds, codon, label
            JPanel aminoWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            aminoWrapper.setBackground(BG);
            aminoWrapper.add(aminoPanel);

            add(aminoWrapper);
            add(dropWrapper);
            add(bonds);
            add(codonRow);
            add(Box.createVerticalStrut(4));
            add(lbl);
            revalidate();
            repaint();
        }

        private void setupDropTarget() {
            new DropTarget(dropZone, new DropTargetAdapter() {
                @Override
                public void dragEnter(DropTargetDragEvent e) {
                    dropZone.setBorder(new DashedBorder(SLOT_OVER, 2));
                    dropZone.setBackground(new Color(0x2a2000));
                    e.acceptDrag(DnDConstants.ACTION_MOVE);
                }
                @Override
                public void dragExit(DropTargetEvent e) {
                    dropZone.setBorder(new DashedBorder(SLOT_IDLE, 2));
                    dropZone.setBackground(PANEL_BG);
                }
                @Override
                public void drop(DropTargetDropEvent e) {
                    try {
                        e.acceptDrop(DnDConstants.ACTION_MOVE);
                        String data = (String) e.getTransferable().getTransferData(DataFlavor.stringFlavor);
                        char[] ac = data.toCharArray();
                        TilePanel src = tiles.stream()
                                .filter(t -> new String(t.bases).equals(data))
                                .findFirst().orElse(null);
                        attemptDrop(ac, idx, src);
                        dropZone.setBorder(new DashedBorder(SLOT_IDLE, 2));
                        dropZone.setBackground(PANEL_BG);
                        e.dropComplete(true);
                    } catch (Exception ex) {
                        e.dropComplete(false);
                    }
                }
            });
        }

        void setMatched(char[] ac) { isMatched = true; buildUI(); }
        void reset()               { isMatched = false; buildUI(); }

        void shake() {
            Point origin = getLocation();
            javax.swing.Timer t = new javax.swing.Timer(40, null);
            int[] step = {0};
            int[] offsets = {-8, 8, -6, 6, -4, 4, 0};
            t.addActionListener(e -> {
                if (step[0] < offsets.length) setLocation(origin.x + offsets[step[0]++], origin.y);
                else { setLocation(origin); t.stop(); }
            });
            t.start();
        }
    }

    // =========================================================================
    // TilePanel – draggable anticodon tile
    // =========================================================================
    class TilePanel extends JPanel implements DragGestureListener, DragSourceListener {
        final char[] bases;
        private final DragSource ds = new DragSource();

        TilePanel(char[] bases) {
            this.bases = bases;
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 6));
            setBackground(PANEL_BG);
            setBorder(new CompoundBorder(new LineBorder(new Color(0x333333), 2, true),
                    new EmptyBorder(4, 6, 4, 6)));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            for (char b : bases) add(makeBaseLabel(b, 40));
            ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(0x22315e));
                    setBorder(new CompoundBorder(new LineBorder(new Color(0x5555aa), 2, true),
                            new EmptyBorder(4, 6, 4, 6)));
                }
                @Override public void mouseExited(MouseEvent e) {
                    setBackground(PANEL_BG);
                    setBorder(new CompoundBorder(new LineBorder(new Color(0x333333), 2, true),
                            new EmptyBorder(4, 6, 4, 6)));
                }
            });
        }

        @Override public void dragGestureRecognized(DragGestureEvent e) {
            ds.startDrag(e, DragSource.DefaultMoveDrop, new StringSelection(new String(bases)), this);
        }
        @Override public void dragEnter(DragSourceDragEvent e)    {}
        @Override public void dragOver(DragSourceDragEvent e)     {}
        @Override public void dropActionChanged(DragSourceDragEvent e) {}
        @Override public void dragExit(DragSourceEvent e)         {}
        @Override public void dragDropEnd(DragSourceDropEvent e)  {}
    }

    // =========================================================================
    // WrapLayout
    // =========================================================================
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container t) { return layoutSize(t, true); }
        @Override public Dimension minimumLayoutSize(Container t)   { return layoutSize(t, false); }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;
                int width = 0, height = 0, rowWidth = 0, rowHeight = 0;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        width = Math.max(width, rowWidth);
                        height += rowHeight + vgap;
                        rowWidth = 0; rowHeight = 0;
                    }
                    rowWidth += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                width = Math.max(width, rowWidth);
                height += rowHeight + insets.top + insets.bottom + vgap * 2;
                return new Dimension(width, height);
            }
        }
    }

    // =========================================================================
    // DashedBorder
    // =========================================================================
    static class DashedBorder extends AbstractBorder {
        private final Color color;
        private final float thickness;

        DashedBorder(Color c, float t) { color = c; thickness = t; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{6, 4}, 0));
            g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 10, 10);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(4, 4, 4, 4); }
    }

    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new CodonGame();
        });
    }
}