package Main;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CodonGame extends JFrame {

	// ── Colours ──────────────────────────────────────────────────────────────
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

	private static final Color BG = new Color(0x1a1a2e);
	private static final Color PANEL_BG = new Color(0x16213e);
	private static final Color SLOT_IDLE = new Color(0x555555);
	private static final Color SLOT_OK = new Color(0x2ecc71);
	private static final Color SLOT_OVER = new Color(0xf39c12);

	// ── Game data ─────────────────────────────────────────────────────────────
	private static final char[][] CODONS = { { 'A', 'G', 'A' }, { 'C', 'U', 'G' }, { 'C', 'U', 'A' } };
	private static final char[][] ANTICODONS = { { 'U', 'C', 'U' }, { 'G', 'A', 'C' }, { 'G', 'A', 'U' } };

	private static final char[][] DISTRACTORS = { { 'U', 'C', 'A' }, { 'C', 'G', 'U' }, { 'A', 'A', 'G' },
			{ 'C', 'U', 'U' }, { 'U', 'G', 'A' }, { 'G', 'G', 'U' }, { 'U', 'A', 'C' }, { 'A', 'C', 'U' } };

	// ── UI state ──────────────────────────────────────────────────────────────
	private final boolean[] matched = new boolean[3];
	private int score = 0;

	private JLabel scoreLabel;
	private JLabel feedbackLabel;
	private final SlotPanel[] slots = new SlotPanel[3];
	private JPanel tileArea;
	private final List<TilePanel> tiles = new ArrayList<>();

	// ─────────────────────────────────────────────────────────────────────────
	public CodonGame() {
		super("🧬 Codon–Anticodon Matching Game");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBackground(BG);
		getContentPane().setBackground(BG);
		setLayout(new BorderLayout(0, 16));

		add(buildHeader(), BorderLayout.NORTH);
		add(buildCenter(), BorderLayout.CENTER);
		add(buildTileArea(), BorderLayout.SOUTH);

		pack();
		setMinimumSize(new Dimension(740, 560));
		setLocationRelativeTo(null);
		setVisible(true);
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

		// DNA info
		JLabel dnaInfo = new JLabel("DNA: AGACTGCTA  →  mRNA: AGA · CUG · CUA", SwingConstants.CENTER);
		dnaInfo.setFont(new Font("Monospaced", Font.PLAIN, 13));
		dnaInfo.setForeground(new Color(0x888888));
		dnaInfo.setBorder(new EmptyBorder(6, 0, 0, 0));

		// Buttons + score row
		scoreLabel = new JLabel("Score: 0");
		scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
		scoreLabel.setForeground(new Color(0xf1c40f));

		JButton rulesBtn = makeButton("Show Pairing Rules", new Color(0x8e44ad));
		rulesBtn.addActionListener(e -> showRules());

		JButton resetBtn = makeButton("🔄 Reset", new Color(0x2c3e50));
		resetBtn.addActionListener(e -> reset());

		JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
		btnRow.setBackground(BG);
		btnRow.add(scoreLabel);
		btnRow.add(rulesBtn);
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
		header.add(dnaInfo);
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

		tileArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
		tileArea.setBackground(PANEL_BG);
		tileArea.setBorder(
				new CompoundBorder(new LineBorder(new Color(0x333333), 1, true), new EmptyBorder(8, 8, 8, 8)));
		wrapper.add(tileArea, BorderLayout.CENTER);

		populateTiles();
		return wrapper;
	}

	private void populateTiles() {
		tileArea.removeAll();
		tiles.clear();

		// Collect all options: 3 correct + distractors (avoid duplicating correct)
		List<char[]> options = new ArrayList<>();
		for (char[] ac : ANTICODONS)
			options.add(ac.clone());
		char[][] dist = { { 'U', 'C', 'A' }, { 'C', 'G', 'U' }, { 'A', 'A', 'G' }, { 'C', 'U', 'U' }, { 'U', 'G', 'A' },
				{ 'G', 'G', 'U' }, { 'U', 'A', 'C' }, { 'A', 'C', 'U' } };
		for (char[] d : dist)
			options.add(d);
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
		if (feedbackTimer != null && feedbackTimer.isRunning())
			feedbackTimer.stop();
		feedbackTimer = new javax.swing.Timer(3000, e -> {
			feedbackLabel.setText(" ");
			feedbackLabel.setBackground(BG);
		});
		feedbackTimer.setRepeats(false);
		feedbackTimer.start();
	}

	// ── Drop logic ────────────────────────────────────────────────────────────
	void attemptDrop(char[] anticodon, int slotIdx, TilePanel tile) {
		if (matched[slotIdx])
			return;

		char[] correct = ANTICODONS[slotIdx];
		boolean ok = Arrays.equals(anticodon, correct);

		if (ok) {
			matched[slotIdx] = true;
			score += 10;
			scoreLabel.setText("Score: " + score);
			slots[slotIdx].setMatched(anticodon);
			tileArea.remove(tile);
			tiles.remove(tile);
			tileArea.revalidate();
			tileArea.repaint();
			showFeedback(true, "✅ Correct! " + new String(anticodon) + " pairs with " + new String(CODONS[slotIdx]));
			boolean allDone = true;
			for (boolean m : matched)
				if (!m) {
					allDone = false;
					break;
				}
			if (allDone)
				showWin();
		} else {
			slots[slotIdx].shake();
			showFeedback(false, "❌ " + new String(anticodon) + " doesn't pair with " + new String(CODONS[slotIdx])
					+ ". Remember: A↔U, G↔C only!");
		}
	}

	// ── Reset ─────────────────────────────────────────────────────────────────
	private void reset() {
		Arrays.fill(matched, false);
		score = 0;
		scoreLabel.setText("Score: 0");
		feedbackLabel.setText(" ");
		feedbackLabel.setBackground(BG);
		for (SlotPanel s : slots)
			s.reset();
		populateTiles();
	}

	// ── Win ───────────────────────────────────────────────────────────────────
	private void showWin() {
		JOptionPane.showMessageDialog(this, "<html><center><font size=5>🎉 All codons matched!</font><br><br>"
				+ "Final Score: <b>" + score + "</b><br><br>"
				+ "<font color='gray'>This is exactly how ribosomes<br>read mRNA during translation!</font></center></html>",
				"You Win!", JOptionPane.INFORMATION_MESSAGE);
	}

	// ── Rules dialog ──────────────────────────────────────────────────────────
	private void showRules() {
		String[][] pairs = { { "A", "U", "Adenine ↔ Uracil" }, { "U", "A", "Uracil ↔ Adenine" },
				{ "G", "C", "Guanine ↔ Cytosine" }, { "C", "G", "Cytosine ↔ Guanine" } };
		StringBuilder sb = new StringBuilder("<html><b>Watson-Crick Base Pairing Rules:</b><br><br>");
		for (String[] row : pairs)
			sb.append("&nbsp;&nbsp;<b>").append(row[0]).append("</b> pairs with <b>").append(row[1])
					.append("</b> &nbsp;(").append(row[2]).append(")<br>");
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
	// SlotPanel – an mRNA codon + drop zone for its anticodon
	// =========================================================================
	class SlotPanel extends JPanel {
		private final int idx;
		private JPanel dropZone;
		private JLabel statusIcon;
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

			// Drop zone
			dropZone = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
			dropZone.setBackground(PANEL_BG);
			dropZone.setPreferredSize(new Dimension(164, 68));
			dropZone.setBorder(new DashedBorder(SLOT_IDLE, 2));

			if (isMatched) {
				char[] ac = ANTICODONS[idx];
				for (char b : ac)
					dropZone.add(makeBaseLabel(b, 44));
				dropZone.setBorder(new DashedBorder(SLOT_OK, 2));
				dropZone.setBackground(new Color(0x1a3a2a));
				statusIcon = new JLabel("✓");
				statusIcon.setForeground(SLOT_OK);
				statusIcon.setFont(new Font("SansSerif", Font.BOLD, 18));
				dropZone.add(statusIcon);
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

			// Bond lines (shown only when matched)
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

			// Codon bases
			JPanel codonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
			codonRow.setBackground(BG);
			for (char b : CODONS[idx])
				codonRow.add(makeBaseLabel(b, 44));

			// Label
			JLabel lbl = new JLabel("Codon " + (idx + 1) + (idx == 0 ? "  (5')" : idx == 2 ? "  (3')" : ""),
					SwingConstants.CENTER);
			lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
			lbl.setForeground(new Color(0x888888));
			lbl.setAlignmentX(CENTER_ALIGNMENT);

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
						// find the tile
						TilePanel src = tiles.stream().filter(t -> new String(t.bases).equals(data)).findFirst()
								.orElse(null);
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

		void setMatched(char[] ac) {
			isMatched = true;
			buildUI();
		}

		void reset() {
			isMatched = false;
			buildUI();
		}

		void shake() {
			Point origin = getLocation();
			javax.swing.Timer t = new javax.swing.Timer(40, null);
			int[] step = { 0 };
			int[] offsets = { -8, 8, -6, 6, -4, 4, 0 };
			t.addActionListener(e -> {
				if (step[0] < offsets.length) {
					setLocation(origin.x + offsets[step[0]++], origin.y);
				} else {
					setLocation(origin);
					t.stop();
				}
			});
			t.start();
		}
	}

	// =========================================================================
	// TilePanel – a draggable anticodon tile
	// =========================================================================
	class TilePanel extends JPanel implements DragGestureListener, DragSourceListener {
		final char[] bases;
		private final DragSource ds = new DragSource();

		TilePanel(char[] bases) {
			this.bases = bases;
			setLayout(new FlowLayout(FlowLayout.CENTER, 4, 6));
			setBackground(PANEL_BG);
			setBorder(new CompoundBorder(new LineBorder(new Color(0x333333), 2, true), new EmptyBorder(4, 6, 4, 6)));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			for (char b : bases)
				add(makeBaseLabel(b, 40));
			ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					setBackground(new Color(0x22315e));
					setBorder(new CompoundBorder(new LineBorder(new Color(0x5555aa), 2, true),
							new EmptyBorder(4, 6, 4, 6)));
				}

				@Override
				public void mouseExited(MouseEvent e) {
					setBackground(PANEL_BG);
					setBorder(new CompoundBorder(new LineBorder(new Color(0x333333), 2, true),
							new EmptyBorder(4, 6, 4, 6)));
				}
			});
		}

		@Override
		public void dragGestureRecognized(DragGestureEvent e) {
			StringSelection sel = new StringSelection(new String(bases));
			ds.startDrag(e, DragSource.DefaultMoveDrop, sel, this);
		}

		@Override
		public void dragEnter(DragSourceDragEvent e) {
		}

		@Override
		public void dragOver(DragSourceDragEvent e) {
		}

		@Override
		public void dropActionChanged(DragSourceDragEvent e) {
		}

		@Override
		public void dragExit(DragSourceEvent e) {
		}

		@Override
		public void dragDropEnd(DragSourceDropEvent e) {
		}
	}

	// =========================================================================
	// DashedBorder
	// =========================================================================
	static class DashedBorder extends AbstractBorder {
		private final Color color;
		private final float thickness;

		DashedBorder(Color c, float t) {
			color = c;
			thickness = t;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(color);
			g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
					new float[] { 6, 4 }, 0));
			g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 10, 10);
			g2.dispose();
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(4, 4, 4, 4);
		}
	}

	// =========================================================================
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
			}
			new CodonGame();
		});
	}
}