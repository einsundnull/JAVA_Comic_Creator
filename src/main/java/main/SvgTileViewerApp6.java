package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SvgTileViewerApp6 {

	private JFrame frame;
	private JPanel tilePanel;
	private JSVGCanvas svgCanvas;
	private File currentFolder;
	private Map<String, WeakReference<BufferedImage>> imageCache = new HashMap<>();
	private boolean isLoadingFolder = false;
	private JPanel selectedPanel;
	private JPanel rightPanel;
	private Map<JCheckBox, File> checkboxFileMap = new HashMap<>();
	Map<File, JPanel> fileToTileMap = new HashMap<>();
	private File lastFolder = null;  // Feld speichern
	private final java.util.List<File> selectedFiles = new java.util.ArrayList<>();

	private JScrollPane tileScrollPane;
	private JScrollPane selectedScrollPane;

	public static void main(String[] args) {
		// Maximalen Java-Heap-Speicher erhöhen
		// Diese Zeile ist nur ein Hinweis - Sie müssen die VM-Argumente ändern
		// java -Xmx512m -jar YourApplication.jar
		SwingUtilities.invokeLater(() -> new SvgTileViewerApp6().createAndShowUI());
	}

	private void createAndShowUI() {
		File dummyFile = new File("test.svg"); // temporär
	    if (lastFolder != null && lastFolder.isDirectory()) {
	        loadFolder(lastFolder);
	    }

		frame = new JFrame("SVG Tile Viewer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1000, 700);
		frame.setLayout(new BorderLayout());

		JButton chooseFolderBtn = new JButton("Verzeichnis auswählen");
		chooseFolderBtn.addActionListener(e -> chooseFolder());

		// Linke Seite
		tilePanel = new JPanel();
		tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));

		JPanel rowPanel = createThumbnailRow(dummyFile, true, true);
		tilePanel.add(rowPanel);
		fileToTileMap.put(dummyFile, rowPanel); // Zuweisung für späteres Scrollen

		JScrollPane tileScrollPane = new JScrollPane(tilePanel);
		tileScrollPane.setPreferredSize(new Dimension(300, 0));
		tileScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		tileScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		// Lazy-Loading beim Scrollen
		tileScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (!isLoadingFolder) {
					frame.repaint();
				}
			}
		});

		svgCanvas = new JSVGCanvas();
		svgCanvas.setBackground(Color.WHITE);

		frame.add(chooseFolderBtn, BorderLayout.NORTH);
		frame.add(tileScrollPane, BorderLayout.WEST);
		frame.add(svgCanvas, BorderLayout.CENTER);

		// Rechte Seite
		selectedPanel = new JPanel();
		selectedPanel.setLayout(new BoxLayout(selectedPanel, BoxLayout.Y_AXIS));
		selectedPanel.setBackground(Color.LIGHT_GRAY);

		JPanel selectedRow = createThumbnailRow(dummyFile, false, false);
		selectedRow.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        JPanel targetPanel = fileToTileMap.get(dummyFile);
		        if (targetPanel != null) {
		            Rectangle rect = targetPanel.getBounds();
		            tilePanel.scrollRectToVisible(rect);
		            // alternativ direkt ScrollPane vertikal scrollen:
		            JScrollBar vsb = tileScrollPane.getVerticalScrollBar();
		            vsb.setValue(rect.y);
		        }
		    }
		});

		selectedPanel.add(selectedRow);

		selectedScrollPane = new JScrollPane(selectedPanel);
		selectedScrollPane.setPreferredSize(new Dimension(300, 0));
		selectedScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(selectedScrollPane, BorderLayout.CENTER);

		frame.add(rightPanel, BorderLayout.EAST);

		// Toggle-Buttons
		JButton toggleLeft = new JButton("⮜");
		JButton toggleRight = new JButton("⮞");

		toggleLeft.addActionListener(e -> {
			tileScrollPane.setVisible(!tileScrollPane.isVisible());
			toggleLeft.setText(tileScrollPane.isVisible() ? "⮜" : "⮞");
		});

		toggleRight.addActionListener(e -> {
			rightPanel.setVisible(!rightPanel.isVisible());
			toggleRight.setText(rightPanel.isVisible() ? "⮞" : "⮜");
		});

		JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		togglePanel.add(toggleLeft);
		togglePanel.add(toggleRight);
		frame.add(togglePanel, BorderLayout.SOUTH);

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		// Speicherbereinigung starten
		startMemoryCleanupTimer();
	}

	private void startMemoryCleanupTimer() {
		// Alle 60 Sekunden den Garbage Collector aufrufen
		javax.swing.Timer cleanupTimer = new javax.swing.Timer(60000, e -> {
			System.gc();
		});
		cleanupTimer.start();
	}

	private void chooseFolder() {
	    JFileChooser chooser = new JFileChooser();
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    if (lastFolder != null) {
	        chooser.setCurrentDirectory(lastFolder);
	    }
	    int res = chooser.showOpenDialog(frame);
	    if (res == JFileChooser.APPROVE_OPTION) {
	        File folder = chooser.getSelectedFile();
	        if (folder != null && folder.isDirectory()) {
	            lastFolder = folder;  // speichern
	            loadFolder(folder);
	        }
	    }
	}

	private void loadFolder(File folder) {
	    tilePanel.removeAll();
	    selectedPanel.removeAll();
	    fileToTileMap.clear();

	    File[] svgFiles = folder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".svg"));
	    if (svgFiles != null) {
	        for (File svgFile : svgFiles) {
	            JPanel tileRow = createThumbnailRow(svgFile, true, true);
	            tilePanel.add(tileRow);
	            fileToTileMap.put(svgFile, tileRow);

	            JPanel selectedRow = createThumbnailRow(svgFile, false, false);
	            selectedRow.addMouseListener(new MouseAdapter() {
	                @Override
	                public void mouseClicked(MouseEvent e) {
	                    JPanel targetPanel = fileToTileMap.get(svgFile);
	                    if (targetPanel != null) {
	                        targetPanel.scrollRectToVisible(new Rectangle(targetPanel.getBounds()));
	                    }
	                }
	            });
	            selectedPanel.add(selectedRow);
	        }
	    }
	    tilePanel.revalidate();
	    tilePanel.repaint();
	    selectedPanel.revalidate();
	    selectedPanel.repaint();
	}
	private void loadSvgTiles(File folder) {
		SwingUtilities.invokeLater(() -> {
			tilePanel.removeAll();
			tilePanel.revalidate();
			tilePanel.repaint();
		});

		File[] svgFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
		if (svgFiles == null || svgFiles.length == 0) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(frame, "Keine SVG-Dateien gefunden.");
				svgCanvas.setURI(null);
			});
			return;
		}

		// Sortiere Dateien alphabetisch
		Arrays.sort(svgFiles);

		// Batch-Verarbeitung verwenden, um UI-Blockierung zu vermeiden
		int batchSize = 5;
		for (int i = 0; i < svgFiles.length; i += batchSize) {
			final int startIdx = i;
			final int endIdx = Math.min(i + batchSize, svgFiles.length);

			SwingUtilities.invokeLater(() -> {
				for (int j = startIdx; j < endIdx; j++) {
					JPanel rowPanel = createSvgRow(svgFiles[j]);
					tilePanel.add(rowPanel);
					tilePanel.add(Box.createVerticalStrut(5));
				}
				tilePanel.revalidate();
				tilePanel.repaint();
			});

			// Kurz warten, damit die UI nicht blockiert wird
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Erstes SVG anzeigen, wenn Laden abgeschlossen
		SwingUtilities.invokeLater(() -> {
			if (svgFiles.length > 0) {
				showSvg(svgFiles[0]);
			}
		});
	}

	private JPanel createSvgRow(File svgFile) {
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

		// JLabel mit Icon statt JSVGCanvas für Vorschaubilder
		JLabel thumbnailLabel = new JLabel();
		thumbnailLabel.setPreferredSize(new Dimension(50, 50));
		thumbnailLabel.setBackground(Color.WHITE);
		thumbnailLabel.setOpaque(true);

		// In einem separaten Thread laden, um UI-Blockierung zu vermeiden
		new Thread(() -> {
			BufferedImage thumbnail = getSvgThumbnail(svgFile);
			if (thumbnail != null) {
				SwingUtilities.invokeLater(() -> {
					thumbnailLabel.setIcon(new ImageIcon(thumbnail));
				});
			}
		}).start();

		thumbnailLabel.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				showSvg(svgFile);
			}
		});

		JCheckBox checkBox = new JCheckBox();
		checkBox.addActionListener(e -> {
			if (checkBox.isSelected()) {
				selectedFiles.add(svgFile);
			} else {
				selectedFiles.remove(svgFile);
			}
			updateSelectedPanel();
		});

		checkBox.setOpaque(false);

//        JLabel fileNameLabel = new JLabel(svgFile.getName());
		JLabel fileNameLabel = new JLabel(svgFile.getName());
		fileNameLabel.setForeground(Color.BLUE);
		fileNameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		fileNameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				String newName = JOptionPane.showInputDialog(frame, "Neuer Dateiname:", svgFile.getName());
				if (newName != null && !newName.trim().isEmpty()) {
					if (!newName.toLowerCase().endsWith(".svg"))
						newName += ".svg";
					File renamedFile = new File(svgFile.getParent(), newName);
					if (renamedFile.exists()) {
						JOptionPane.showMessageDialog(frame, "Datei existiert bereits.");
						return;
					}
					boolean success = svgFile.renameTo(renamedFile);
					if (success) {
						fileNameLabel.setText(newName);
						checkboxFileMap.put(checkBox, renamedFile);
						imageCache.remove(svgFile.getAbsolutePath());
						updateSelectedPanel();
					} else {
						JOptionPane.showMessageDialog(frame, "Umbenennen fehlgeschlagen.");
					}
				}
			}
		});

		rowPanel.add(thumbnailLabel);
		rowPanel.add(checkBox);
		rowPanel.add(fileNameLabel);

		checkboxFileMap.put(checkBox, svgFile);
		checkBox.addActionListener(e -> updateSelectedPanel());

		return rowPanel;
	}

	private JPanel createThumbnailRow(File svgFile, boolean showCheckbox, boolean showName) {
		JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

		JLabel thumbnailLabel = new JLabel();
		thumbnailLabel.setPreferredSize(new Dimension(50, 50));
		thumbnailLabel.setOpaque(true);
		thumbnailLabel.setBackground(Color.WHITE);

		new Thread(() -> {
			BufferedImage thumbnail = getSvgThumbnail(svgFile);
			if (thumbnail != null) {
				SwingUtilities.invokeLater(() -> {
					thumbnailLabel.setIcon(new ImageIcon(thumbnail));
				});
			}
		}).start();

		rowPanel.add(thumbnailLabel);

		if (showCheckbox) {
			JCheckBox checkBox = new JCheckBox();
			checkBox.setOpaque(false);
			rowPanel.add(checkBox);
			checkboxFileMap.put(checkBox, svgFile);
		}

		if (showName) {
			JTextField nameField = new JTextField(svgFile.getName());
			nameField.setBorder(null);
			nameField.setEditable(false);
			nameField.setForeground(Color.BLUE);
			nameField.setOpaque(false);
			nameField.setColumns(20);

			nameField.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					nameField.setEditable(true);
					nameField.setOpaque(true);
					nameField.requestFocus();
				}
			});

			nameField.addActionListener(e -> {
				String newName = nameField.getText().trim();
				if (!newName.isEmpty() && !newName.equals(svgFile.getName())) {
					File newFile = new File(svgFile.getParentFile(), newName);
					if (svgFile.renameTo(newFile)) {
						nameField.setText(newFile.getName());
					} else {
						JOptionPane.showMessageDialog(rowPanel, "Umbenennen fehlgeschlagen.");
					}
				}
				nameField.setEditable(false);
				nameField.setOpaque(false);
			});

			nameField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					nameField.setEditable(false);
					nameField.setOpaque(false);
				}
			});

			rowPanel.add(nameField);
		}

		return rowPanel;
	}

	private void updateSelectedPanel() {
		SwingUtilities.invokeLater(() -> {
			selectedPanel.removeAll();
			for (Map.Entry<JCheckBox, File> entry : checkboxFileMap.entrySet()) {
				if (entry.getKey().isSelected()) {
					BufferedImage thumb = getSvgThumbnail(entry.getValue());
					JLabel label = new JLabel(new ImageIcon(thumb));
					label.setToolTipText(entry.getValue().getName());
					label.setPreferredSize(new Dimension(60, 60));
					selectedPanel.add(label);
				}
			}
			selectedPanel.revalidate();
			selectedPanel.repaint();
		});
	}

	private BufferedImage getSvgThumbnail(File svgFile) {
		String cacheKey = svgFile.getAbsolutePath();

		// Versuchen, aus dem Cache zu laden
		if (imageCache.containsKey(cacheKey)) {
			WeakReference<BufferedImage> ref = imageCache.get(cacheKey);
			BufferedImage cached = ref.get();
			if (cached != null) {
				return cached;
			}
			// Cache-Eintrag entfernen, wenn Bild nicht mehr im Speicher ist
			imageCache.remove(cacheKey);
		}

		try {
			// SVG in kleines PNG für die Vorschau umwandeln
			PNGTranscoder transcoder = new PNGTranscoder();
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 50f);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 50f);

			// SVG Datei als Input
			TranscoderInput input = new TranscoderInput(svgFile.toURI().toString());
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			TranscoderOutput output = new TranscoderOutput(outputStream);

			// Transcode SVG zu PNG
			transcoder.transcode(input, output);

			// PNG aus Stream lesen
			ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
			BufferedImage thumbnail = ImageIO.read(inputStream);

			// In Cache speichern
			imageCache.put(cacheKey, new WeakReference<>(thumbnail));

			return thumbnail;
		} catch (TranscoderException | java.io.IOException e) {
			System.err.println("Fehler beim Erstellen der Vorschau für " + svgFile.getName() + ": " + e.getMessage());
			return null;
		}
	}

	private void showSvg(File svgFile) {
		try {
			frame.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

			// Altes SVG freigeben, bevor neues geladen wird
			svgCanvas.setSVGDocument(null);
			System.gc();

			// Kurze Pause einfügen
			Thread.sleep(50);

			// Neues SVG laden
			svgCanvas.setURI(svgFile.toURI().toString());

			frame.setCursor(java.awt.Cursor.getDefaultCursor());

			if (!selectedFiles.contains(svgFile)) {
				selectedFiles.add(svgFile);
				updateSelectedPanel();
			}

		} catch (Exception ex) {
			frame.setCursor(java.awt.Cursor.getDefaultCursor());
			JOptionPane.showMessageDialog(frame,
					"SVG konnte nicht geladen werden: " + svgFile.getName() + "\n" + ex.getMessage());
		}
	}
}