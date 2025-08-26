package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
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
import javax.swing.BorderFactory;
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
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SvgTileViewerApp7 {

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

	private final java.util.List<File> selectedFiles = new java.util.ArrayList<>();

	private JScrollPane tileScrollPane;
	private JScrollPane selectedScrollPane;

	public static void main(String[] args) {
		// Maximalen Java-Heap-Speicher erhöhen
		// Diese Zeile ist nur ein Hinweis - Sie müssen die VM-Argumente ändern
		// java -Xmx512m -jar YourApplication.jar
		SwingUtilities.invokeLater(() -> new SvgTileViewerApp7().createAndShowUI());
	}

	// Annahme: map<File, JPanel> fileToTileMap (linkes Panel Thumbnails)
//  map<File, JPanel> fileToSelectedTileMap (rechtes Panel Thumbnails)
//  svgCanvas zum Anzeigen der SVG-Datei

	private void createAndShowUI() {
	    currentFolder = LastUsedDirectory.load();
	    if (currentFolder == null || !currentFolder.isDirectory())
	        currentFolder = new File(System.getProperty("user.home"));

	    frame = new JFrame("SVG Tile Viewer");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setSize(1000, 700);
	    frame.setLayout(new BorderLayout());

	    JButton chooseFolderBtn = new JButton("Verzeichnis auswählen");
	    chooseFolderBtn.addActionListener(e -> chooseFolder());
	    frame.add(chooseFolderBtn, BorderLayout.NORTH);

	    // Linkes Panel
	    tilePanel = new JPanel();
	    tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
	    tilePanel.setBackground(Color.WHITE);
	    tileScrollPane = new JScrollPane(tilePanel);
	    tileScrollPane.setPreferredSize(new Dimension(300, 0));
	    tileScrollPane.getVerticalScrollBar().setUnitIncrement(16);
	    frame.add(tileScrollPane, BorderLayout.WEST);

	    // SVG Canvas Mitte
	    svgCanvas = new JSVGCanvas();
	    svgCanvas.setBackground(Color.WHITE);
	    frame.add(svgCanvas, BorderLayout.CENTER);

	    // Rechtes Panel
	    selectedPanel = new JPanel();
	    selectedPanel.setLayout(new BoxLayout(selectedPanel, BoxLayout.Y_AXIS));
	    selectedPanel.setBackground(Color.WHITE);
	    selectedPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    selectedScrollPane = new JScrollPane(selectedPanel);
	    selectedScrollPane.setPreferredSize(new Dimension(300, 0));
	    selectedScrollPane.getVerticalScrollBar().setUnitIncrement(16);

	    rightPanel = new JPanel(new BorderLayout());
	    rightPanel.add(selectedScrollPane, BorderLayout.CENTER);
	    rightPanel.setBackground(Color.WHITE);
	    rightPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
	    frame.add(rightPanel, BorderLayout.EAST);

	    // Dragging rechts
	    MouseAdapter dragListener = new MouseAdapter() {
	        Point origin;
	        public void mousePressed(MouseEvent e) {
	            origin = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), frame.getContentPane());
	        }
	        public void mouseDragged(MouseEvent e) {
	            Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), frame.getContentPane());
	            int dx = pt.x - origin.x, dy = pt.y - origin.y;
	            Point loc = rightPanel.getLocation();
	            rightPanel.setLocation(loc.x + dx, loc.y + dy);
	            origin = pt;
	            frame.repaint();
	        }
	    };
	    rightPanel.addMouseListener(dragListener);
	    rightPanel.addMouseMotionListener(dragListener);

	    // Toggle Buttons
	    JButton toggleLeft = new JButton("⮜");
	    toggleLeft.addActionListener(e -> {
	        tileScrollPane.setVisible(!tileScrollPane.isVisible());
	        toggleLeft.setText(tileScrollPane.isVisible() ? "⮜" : "⮞");
	    });
	    JButton toggleRight = new JButton("⮞");
	    toggleRight.addActionListener(e -> {
	        rightPanel.setVisible(!rightPanel.isVisible());
	        toggleRight.setText(rightPanel.isVisible() ? "⮞" : "⮜");
	    });
	    JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    togglePanel.add(toggleLeft);
	    togglePanel.add(toggleRight);
	    frame.add(togglePanel, BorderLayout.SOUTH);

	    loadFolderFiles(currentFolder);

	    frame.setLocationRelativeTo(null);
	    frame.setVisible(true);

	    startMemoryCleanupTimer();
	}

	private void loadFolderFiles(File folder) {
	    SwingUtilities.invokeLater(() -> {
	        tilePanel.removeAll();
	        selectedPanel.removeAll();
	        fileToTileMap.clear();
	        selectedFilePanels.clear();
	        fileCheckboxMap.clear();

	        File[] files = folder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".svg"));
	        if (files == null || files.length == 0) {
	            JOptionPane.showMessageDialog(frame, "Keine SVG-Dateien gefunden.");
	            svgCanvas.setURI(null);
	            return;
	        }

	        Arrays.sort(files);

	        for (File file : files) {
	            // Links mit Checkbox + Name
	            JPanel leftRow = createThumbnailRow(file, true, true);
	            tilePanel.add(leftRow);
	            fileToTileMap.put(file, leftRow);

	            JCheckBox cb = findCheckboxInPanel(leftRow);
	            if (cb != null) {
	                fileCheckboxMap.put(file, cb);
	                cb.addActionListener(e -> {
	                    if (cb.isSelected()) addFileToSelectedPanel(file);
	                    else removeFileFromSelectedPanel(file);
	                });
	            }
	        }
	        tilePanel.revalidate();
	        tilePanel.repaint();
	        selectedPanel.revalidate();
	        selectedPanel.repaint();

	        // Erstes SVG anzeigen
	        if (files.length > 0) showSvg(files[0]);
	    });
	}

	private JPanel createThumbnailRow(File file, boolean withCheckbox, boolean withName) {
	    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

	    JLabel thumb = new JLabel();
	    thumb.setPreferredSize(new Dimension(50, 50));
	    thumb.setOpaque(true);
	    thumb.setBackground(Color.WHITE);
	    row.add(thumb);

	    // Thumbnail laden
	    new Thread(() -> {
	        BufferedImage img = getSvgThumbnail(file);
	        if (img != null) SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
	    }).start();

	    thumb.addMouseListener(new MouseAdapter() {
	        public void mouseClicked(MouseEvent e) { showSvg(file); }
	    });

	    if (withCheckbox) {
	        JCheckBox cb = new JCheckBox();
	        cb.setOpaque(false);
	        row.add(cb);
	    }
	    if (withName) {
	        JLabel nameLabel = new JLabel(file.getName());
	        nameLabel.setForeground(Color.BLUE);
	        nameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
	        nameLabel.addMouseListener(new MouseAdapter() {
	            public void mouseClicked(MouseEvent e) {
	                String newName = JOptionPane.showInputDialog(frame, "Neuer Dateiname:", file.getName());
	                if (newName != null && !newName.trim().isEmpty()) {
	                    if (!newName.toLowerCase().endsWith(".svg")) newName += ".svg";
	                    File renamed = new File(file.getParent(), newName);
	                    if (renamed.exists()) {
	                        JOptionPane.showMessageDialog(frame, "Datei existiert bereits.");
	                        return;
	                    }
	                    if (file.renameTo(renamed)) {
	                        nameLabel.setText(newName);
	                        imageCache.remove(file.getAbsolutePath());
	                        loadFolderFiles(currentFolder); // Refresh alle
	                    } else JOptionPane.showMessageDialog(frame, "Umbenennen fehlgeschlagen.");
	                }
	            }
	        });
	        row.add(nameLabel);
	    }

	    return row;
	}

	private JCheckBox findCheckboxInPanel(JPanel p) {
	    for (Component c : p.getComponents())
	        if (c instanceof JCheckBox) return (JCheckBox) c;
	    return null;
	}

	private void addFileToSelectedPanel(File file) {
	    if (selectedFilePanels.containsKey(file)) return;

	    JPanel rightRow = createThumbnailRow(file, false, false);
	    rightRow.addMouseListener(new MouseAdapter() {
	        public void mouseClicked(MouseEvent e) {
	            JPanel leftThumb = fileToTileMap.get(file);
	            if (leftThumb != null) {
	                tileScrollPane.getViewport().scrollRectToVisible(leftThumb.getBounds());
	            }
	            showSvg(file);
	        }
	    });
	    selectedFilePanels.put(file, rightRow);
	    selectedPanel.add(rightRow);
	    selectedPanel.revalidate();
	    selectedPanel.repaint();
	}

	private void removeFileFromSelectedPanel(File file) {
	    JPanel panel = selectedFilePanels.remove(file);
	    if (panel != null) {
	        selectedPanel.remove(panel);
	        selectedPanel.revalidate();
	        selectedPanel.repaint();
	    }
	}

	private void showSvg(File file) {
	    svgCanvas.setURI(file.toURI().toString());
	}

	private void startMemoryCleanupTimer() {
	    new javax.swing.Timer(60000, e -> System.gc()).start();
	}


	private void createLeftScroll() {
		tilePanel = new JPanel();
		tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
		tileScrollPane = new JScrollPane(tilePanel);
		tileScrollPane.setPreferredSize(new Dimension(300, 0));
		tileScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		frame.add(tileScrollPane, BorderLayout.WEST);
	}

	private void createRightScroll() {
		selectedPanel = new JPanel();
		selectedPanel.setLayout(new BoxLayout(selectedPanel, BoxLayout.Y_AXIS));
		selectedPanel.setBackground(Color.LIGHT_GRAY);
		selectedScrollPane = new JScrollPane(selectedPanel);
		selectedScrollPane.setPreferredSize(new Dimension(300, 0));
		selectedScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(selectedScrollPane, BorderLayout.CENTER);
		frame.add(rightPanel, BorderLayout.EAST);
	}

	private Map<File, JCheckBox> fileCheckboxMap = new HashMap<>();

	



	private Map<File, JPanel> selectedFilePanels = new HashMap<>();







	private void chooseFolder() {
		JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int result = chooser.showOpenDialog(frame);

		if (result == JFileChooser.APPROVE_OPTION) {
			currentFolder = chooser.getSelectedFile();
			LastUsedDirectory.save(currentFolder);

			// Cache leeren beim Ordnerwechsel
			imageCache.clear();
			System.gc();

			new Thread(() -> {
				isLoadingFolder = true;
				loadSvgTiles(currentFolder);
				isLoadingFolder = false;
			}).start();
		}
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

	private void addThumbnailClickListener(JPanel thumbnailPanel, File file) {
		thumbnailPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// SVG anzeigen
				svgCanvas.setURI(file.toURI().toString());

				// Scroll im linken Panel zum Bild
				JPanel leftThumbnail = fileToTileMap.get(file);
				if (leftThumbnail != null) {
					SwingUtilities.invokeLater(() -> {
						Rectangle r = leftThumbnail.getBounds();
						tileScrollPane.getViewport().scrollRectToVisible(leftThumbnail.getBounds());

					});
				}
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

	
}