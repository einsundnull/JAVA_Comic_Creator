package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;

public class SvgTileViewerApp4 {

	private static ListenerCustomTileUpdate updateListener;
	private boolean isLoadingFolder = false;
	private boolean scenePanelIsSelected;
	private File currentFolder;

	private JSVGCanvas svgCanvas;
	private JFrame frame;
	private JPanel tilePanel;
	private JPanel selectedPanel;
	private JPanel rightPanel;
	private JPanel scenePanel = new JPanel(null);
	private JScrollPane tileScrollPane;
	private JScrollPane selectedScrollPane;
	private JScrollPane centerScrollPane;

	private Rectangle captureZone;
	private Point startPoint = new Point(0, 0);

	private Rectangle captureZoneReset;
	private Point startPointReset;

	private JTextField nameField;

	private File tempFile;
	private double currentZoom = 1.0;

	SVGDataManager svgDataManager = new SVGDataManager();
	private Map<File, JPanel> fileToTileMap = new HashMap<>();
	private Map<File, List<JPanel>> selectedFilePanels = new HashMap<>();
	private Map<String, CustomImageSVGTile> allTiles = new HashMap<>();
	private List<CustomImageSVGTile> addedTiles = new ArrayList<>();
	private LinkedList<LinkedList<String>> svgData = new LinkedList<>();
	private List<Rectangle> tilePositions = new ArrayList<>(); // Speichert Positionen aller Kacheln
	// For renaming
	private String oldFileName;

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> new SvgTileViewerApp4().createAndShowUI());
	}
	
	private File getScreenShotFolder() {
		File jarFile = null;
		try {
			jarFile = new File(ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		File programDir = jarFile.getParentFile();
		File screenshotsDir = new File(programDir, "screenshots");
		System.out.println("screenshotsDir: "+ screenshotsDir.getAbsolutePath()) ;
		String fileName = "A_ScreenShot1747778089222.png";
		File screenshotFile = new File(screenshotsDir, fileName);
		return screenshotsDir;
	}

	// Helper-Methode zum Auswählen/Abwählen aller Tiles
	private void selectAllTiles(boolean select) {

		if (select) {
			for (CustomImageSVGTile tile : addedTiles) {
				tile.setSelected(true);

			}
		} else {
			for (CustomImageSVGTile tile : addedTiles) {
				tile.setSelected(false);
			}
		}
	}

	// Helper-Methode zum Skalieren aller ausgewählten Tiles
	private void scaleSelectedTiles(double scaleFactor) {
		for (CustomImageSVGTile tile : addedTiles) {
			if (tile.isSelected())
				tile.scaleSVG(scaleFactor);
		}
		scenePanel.repaint();
	}

//	private void loadFolderFiles(File folder) {
//		SwingUtilities.invokeLater(() -> {
//			tilePanel.removeAll();
//			selectedPanel.removeAll();
//			fileToTileMap.clear();
//			selectedFilePanels.clear();
//			allTiles.clear();
//			svgData = svgDataManager.getSVGData(folder);
//			// Alle SVG-Dateien prüfen
//			File[] allSVGFiles = folder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".svg"));
//			if (allSVGFiles == null || allSVGFiles.length == 0) {
//				JOptionPane.showMessageDialog(frame, "Keine SVG-Dateien gefunden.");
//				if (svgCanvas != null) {
//					svgCanvas.setURI(null);
//				}
//				return;
//			}
//			Arrays.sort(allSVGFiles);
//
//			// Tiles erzeugen und in Panel einfügen
//			for (LinkedList<String> data : svgData) {
//				CustomImageTile tile = new CustomImageTile(data, folder);
//				allTiles.put(tile.getFilename(), tile);
//
//				JPanel leftRow = createThumbnailRowLeft(data);
//				tilePanel.add(leftRow);
//				fileToTileMap.put(new File(data.get(1)), leftRow);
//			}
//
//			tilePanel.revalidate();
//			tilePanel.repaint();
//			selectedPanel.revalidate();
//			selectedPanel.repaint();
//		});
//	}

	private JPanel createThumbnailRowLeft(LinkedList<String> data) {
		// This method creates the ListView Items on the left and on the right
		// ScrollView.
		File file = new File(data.get(1));
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

		JLabel thumb = new JLabel();
		thumb.setPreferredSize(new Dimension(50, 50));
		thumb.setOpaque(true);
		thumb.setBackground(Color.WHITE);
		row.add(thumb);

		// Thumbnail laden
		new Thread(() -> {
			BufferedImage img = svgDataManager.getSvgThumbnail(new File(data.get(1)));
			if (img != null)
				SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
		}).start();

		thumb.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {

			}
		});

		JButton add = new JButton("+");
		add.setPreferredSize(new Dimension(30, 25));
		add.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		add.addActionListener(e -> {
			CustomImageSVGTile t = new CustomImageSVGTile(data);
			t.setUpdateListener(updateListener);
			addedTiles.add(t);
			JPanel returnValues[] = new JPanel[2];
			returnValues[0] = createThumbnailRowRight(addedTiles.get(addedTiles.size() - 1), scenePanel,
					svgDataManager)[0];
			returnValues[1] = createThumbnailRowRight(addedTiles.get(addedTiles.size() - 1), scenePanel,
					svgDataManager)[1];
			JPanel rightRow = returnValues[0];
			scenePanel = returnValues[1];
			selectedPanel.add(rightRow);
			selectedPanel.revalidate();
			selectedPanel.repaint();
			scenePanel = SVGTileViewerAppOutSource.setTileVisible(addedTiles.get(addedTiles.size() - 1), true,
					scenePanel, centerScrollPane);

		});
		row.add(add, BorderLayout.EAST);
		row = addEditTextField(row, file);

		return row;
	}

	private JPanel[] createThumbnailRowRight(CustomImageSVGTile tile, JPanel scenePanel, SVGDataManager svgDataManager) {
		// Bestehender Code...
		File file = new File(tile.getData().get(1));
		JPanel row[] = new JPanel[2];
		row[0] = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		row[0].setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
		row[0].putClientProperty("id", tile.getID());
		row[1] = scenePanel;
		// Fügen Sie dem gesamten Panel einen MouseListener hinzu
		row[0].addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				row[1] = highlightCorrespondingTileInCanvas(file, row[1], true);
				row[0].setBackground(Color.LIGHT_GRAY); // Auch das aktuelle Panel hervorheben
			}

			@Override
			public void mouseExited(MouseEvent e) {
				row[1] = highlightCorrespondingTileInCanvas(file, row[1], false);
				row[0].setBackground(Color.WHITE); // Zurücksetzen
			}
		});

		// Setzen Sie den Hintergrund, damit die Hervorhebung sichtbar ist
		row[0].setBackground(Color.WHITE);
		row[0].setOpaque(true);
		// This method creates the ListView Items on the left and on the right
		// ScrollView.

		JLabel thumb = new JLabel();
		thumb.setPreferredSize(new Dimension(50, 50));
		thumb.setOpaque(true);
		thumb.setBackground(Color.WHITE);
		row[0].add(thumb);

		new Thread(() -> {
			BufferedImage img = svgDataManager.getSvgThumbnail(file);
			if (img != null)
				SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
		}).start();

		thumb.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				scenePanel.setComponentZOrder(tile.getPanel(), 0);
			}
		});

		JButton mirrorVertical = new JButton("↔");
		mirrorVertical.setPreferredSize(new Dimension(30, 25));
		mirrorVertical.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		mirrorVertical.addActionListener(e -> {
			tile.toggleMirrorVertical();
		});
		row[0].add(mirrorVertical, BorderLayout.EAST);

		JButton mirrorHorizontal = new JButton("↨");
		mirrorHorizontal.setPreferredSize(new Dimension(30, 25));
		mirrorHorizontal.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		mirrorHorizontal.addActionListener(e -> {
			tile.toggleMirrorHorizontal();
		});
		row[0].add(mirrorHorizontal, BorderLayout.EAST);

		JButton setColor = new JButton("C");
		setColor.setPreferredSize(new Dimension(30, 25));
		setColor.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		setColor.addActionListener(e -> {
			// Öffne Color Picker
			ColorPickerWindow.open(setColor, color -> {
				// verwende die gewählte Farbe
				System.out.println("Farbe: " + color);
				tile.setSVGPathColor(ColorPickerWindow.colorToHex(color));
			});
		});
		row[0].add(setColor, BorderLayout.EAST);

		row[0] = addCheckBox(row[0], tile);
		JPanel returnValues[] = { row[0], scenePanel };
		return returnValues;
	}

	private JPanel highlightCorrespondingTileInCanvas(File file, JPanel scenePanel, boolean isHovered) {
		String filename = file.getName();
		for (CustomImageSVGTile tile : addedTiles) {
			if (tile.getFilename().equals(filename)) {
				if (isHovered) {
					// Hervorhebung des Tiles im Canvas (z.B. mit einem speziellen Rahmen)
					tile.getPanel().setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
				} else {
					// Zurücksetzen auf den normalen Zustand
					if (!tile.isSelected()) {
						tile.getPanel().setBorder(null);
					} else {
						tile.getPanel().setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
					}
				}
				break;
			}
		}
		scenePanel.repaint();
		return scenePanel;
	}

	private void highlightCorrespondingItemInRightPanel(CustomImageSVGTile tile, boolean isHovered) {
		int index = 0;
		for (Component component : selectedPanel.getComponents()) {
//			if (!(component instanceof JPanel))
//				continue;

			JPanel panel = (JPanel) component;
			String id = (String) panel.getClientProperty("id");
			System.err.println(id);
			System.err.println(tile.getID());
			if (id.equals(tile.getID())) {
				if (isHovered) {
					panel.setBackground(Color.LIGHT_GRAY); // Hover color
					// Scrollen, um das entsprechende Panel sichtbar zu machen
					selectedScrollPane.getViewport().scrollRectToVisible(panel.getBounds());
				} else {
					panel.setBackground(Color.WHITE); // Original color
				}
				break;
			}
			index++;
		}
	}

	// Und schließlich die removeFileFromSelectedPanel-Methode

	private void chooseFolder() {
		JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int result = chooser.showOpenDialog(frame);

		if (result == JFileChooser.APPROVE_OPTION) {
			currentFolder = chooser.getSelectedFile();
			LastUsedDirectory.save(currentFolder);

			// Cache leeren beim Ordnerwechsel
			svgDataManager.imageCache.clear();
			System.gc();

			new Thread(() -> {
				isLoadingFolder = true;
				loadFolderFiles(currentFolder);
				isLoadingFolder = false;
			}).start();
		}
	}

	private void scrollToThumbnailInLeftPanel(File file) {
		JPanel leftThumbnail = fileToTileMap.get(file);
		if (leftThumbnail != null) {
			SwingUtilities.invokeLater(() -> {
				Rectangle rect = leftThumbnail.getBounds();
				rect.y = leftThumbnail.getY();
				tileScrollPane.getViewport().scrollRectToVisible(rect);
			});
		}
	}

	private void startMemoryCleanupTimer() {
		new javax.swing.Timer(60000, e -> System.gc()).start();
	}

	private void createAndShowUI() {
	    updateListener = new ListenerCustomTileUpdate() {
	        @Override
	        public void onTileUpdated(CustomImageSVGTile tile) {
	            // TODO Auto-generated method stub
	        }

	        @Override
	        public void onTileHover(boolean isHovered) {
	            System.out.println("SHH");
	        }

	        @Override
	        public void onTileHover(CustomImageSVGTile tile, boolean isHovered) {
	            System.out.println("hover " + tile.getFilename());
	            highlightCorrespondingItemInRightPanel(tile, isHovered);
	        }

	        @Override
	        public void onTileHover(String id, boolean isHovered) {
	        }
	    };
	    
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

	    // Zentrales Panel für CustomImageTiles
	    scenePanel = new JPanel(null) {
	        @Override
	        protected void paintComponent(Graphics g) {
	            super.paintComponent(g);
	            if (captureZone != null) {
	                g.setColor(Color.RED);
	                g.drawRect(captureZone.x, captureZone.y, captureZone.width, captureZone.height);
	            }
	        }
	    };
	    
	    // Scene Panel Konfiguration
	    configureScenePanel();
	    
	    // Linkes Panel mit Tabs für beide Ansichten
	    JTabbedPane leftTabbedPane = new JTabbedPane();
	    
	    // Original SVG TilePanel erstellen
	    tilePanel = new JPanel();
	    tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
	    tilePanel.setBackground(Color.WHITE);
	    JScrollPane svgScrollPane = new JScrollPane(tilePanel);
	    svgScrollPane.getVerticalScrollBar().setUnitIncrement(16);
	    
	    // ImageListView als zweiten Tab erstellen
	    JPanel[] panels = ImageListView.createImageListView(getScreenShotFolder(), scenePanel);
	    JPanel imageListPanel = panels[0]; // Das linke Panel mit der Bilderliste
	    JScrollPane imageListScrollPane = new JScrollPane(imageListPanel);
	    imageListScrollPane.getVerticalScrollBar().setUnitIncrement(16);
	    
	    // Tabs hinzufügen
	    leftTabbedPane.addTab("SVG Tiles", svgScrollPane);
	    leftTabbedPane.addTab("Bilder", imageListScrollPane);
	    
	    // TabbedPane zum Frame hinzufügen
	    tileScrollPane = new JScrollPane(leftTabbedPane);
	    tileScrollPane.setPreferredSize(new Dimension(300, 0));
	    frame.add(tileScrollPane, BorderLayout.WEST);

	    // Zentrales Panel für CustomImageTiles
	    scenePanel.setPreferredSize(new Dimension(2000, 2000));
	    scenePanel.setBackground(Color.WHITE);
	    scenePanel.setFocusable(true);

	    centerScrollPane = new JScrollPane(scenePanel);
	    centerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
	    centerScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
	    frame.add(centerScrollPane, BorderLayout.CENTER);

	    // Rechtes Panel
	    selectedPanel = new JPanel();
	    selectedPanel.setLayout(new BoxLayout(selectedPanel, BoxLayout.Y_AXIS));
	    selectedPanel.setBackground(Color.WHITE);
	    selectedPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    selectedScrollPane = new JScrollPane(selectedPanel);
	    selectedScrollPane.setPreferredSize(new Dimension(300, 0));
	    selectedScrollPane.getVerticalScrollBar().setUnitIncrement(16);

	    rightPanel = new JPanel(new BorderLayout());
	    rightPanel.add(selectedScrollPane, BorderLayout.CENTER);
	    rightPanel.setBackground(Color.WHITE);
	    rightPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
	    frame.add(rightPanel, BorderLayout.EAST);

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
	    
	    // Zusätzlicher Button zum Wechseln zwischen den Tabs
	    JButton switchTabBtn = new JButton("Wechsle Ansicht");
	    switchTabBtn.addActionListener(e -> {
	        JTabbedPane tabbedPane = findTabbedPane();
	        if (tabbedPane != null) {
	            int nextTab = (tabbedPane.getSelectedIndex() + 1) % tabbedPane.getTabCount();
	            tabbedPane.setSelectedIndex(nextTab);
	        }
	    });

	    // Hinzufügen von Steuerungstasten-Panel
	    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	    JButton selectAllBtn = new JButton("Alle auswählen");
	    selectAllBtn.addActionListener(e -> {
	        selectAllTiles(true);
	        scenePanel.repaint();
	    });

	    JButton selectNoneBtn = new JButton("Keine auswählen");
	    selectNoneBtn.addActionListener(e -> {
	        selectAllTiles(false);
	        scenePanel.repaint();
	    });

	    JButton scaleUpBtn = new JButton("BildLaden");
	    File jarFile = null;
	    try {
	        jarFile = new File(ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	    } catch (URISyntaxException e1) {
	        e1.printStackTrace();
	    }
	    File programDir = jarFile.getParentFile();

	    // Screenshot-Verzeichnis erstellen
	    File screenshotsDir = new File(programDir, "screenshots");
	    String fileName = "A_ScreenShot1747778089222.png";
	    File screenshotFile = new File(screenshotsDir, fileName);
	    scaleUpBtn.addActionListener(e -> ScreenshotPanel.showScreenshotDialog(frame,screenshotFile));

	    JButton takeScreenShot = initScreenshotButton(frame);

	    JButton scaleDownBtn = new JButton("Verkleinern");
	    scaleDownBtn.addActionListener(e -> scaleSelectedTiles(0.9));

	    controlPanel.add(takeScreenShot);
	    controlPanel.add(selectAllBtn);
	    controlPanel.add(selectNoneBtn);
	    controlPanel.add(scaleUpBtn);
	    controlPanel.add(scaleDownBtn);

	    JPanel bottomPanel = new JPanel(new BorderLayout());
	    bottomPanel.add(controlPanel, BorderLayout.CENTER);

	    JButton resetZoomBtn = new JButton("Zoom zurücksetzen");
	    resetZoomBtn.addActionListener(e -> resetZoom());
	    controlPanel.add(resetZoomBtn);

	    JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    togglePanel.add(toggleLeft);
	    togglePanel.add(toggleRight);
	    togglePanel.add(switchTabBtn); // Neuen Button hinzufügen
	    bottomPanel.add(togglePanel, BorderLayout.SOUTH);

	    frame.add(bottomPanel, BorderLayout.SOUTH);

	    frame.setLocationRelativeTo(null);
	    frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximiert das Fenster
	    frame.setVisible(true);
	    scenePanel.requestFocusInWindow();
	    centerScrollPane.setFocusable(false);

	    startMemoryCleanupTimer();
	}

	// Diese Methode extrahiert die ScenePanel-Konfiguration, um den Code übersichtlicher zu machen
	private void configureScenePanel() {
	    scenePanel.addMouseMotionListener(new MouseMotionAdapter() {
	        @Override
	        public void mouseDragged(MouseEvent e) {
	            if (startPoint != null) {
	                int x = Math.min(startPoint.x, e.getX());
	                int y = Math.min(startPoint.y, e.getY());
	                int width = Math.abs(startPoint.x - e.getX());
	                int height = Math.abs(startPoint.y - e.getY());
	                captureZone = new Rectangle(x, y, width, height);
	                captureZoneReset = captureZone;
	                startPointReset = startPoint;
	                scenePanel.repaint();
	            }
	        }
	    });

	    scenePanel.addMouseListener(new MouseAdapter() {
	        @Override
	        public void mousePressed(MouseEvent e) {
	            startPoint = e.getPoint();
	        }

	        @Override
	        public void mouseReleased(MouseEvent e) {
	            if (startPoint != null) {
	                int x = Math.min(startPoint.x, e.getX());
	                int y = Math.min(startPoint.y, e.getY());
	                int width = Math.abs(startPoint.x - e.getX());
	                int height = Math.abs(startPoint.y - e.getY());
	                captureZone = new Rectangle(x, y, width, height);
	                saveCaptureZoneToTempFile();
	                scenePanel.repaint();
	            }
	        }

	        @Override
	        public void mouseClicked(MouseEvent e) {
	            scenePanel.requestFocusInWindow();
	            selectAllTiles(false);
	        }

	        @Override
	        public void mouseEntered(MouseEvent e) {
	            scenePanel.requestFocusInWindow();
	            scenePanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2)); // oder andere Farbe/Stärke
	            scenePanelIsSelected = true;
	        }

	        @Override
	        public void mouseExited(MouseEvent e) {
	            scenePanel.setBorder(null);
	            scenePanelIsSelected = false;
	        }
	    });

	    scenePanel.addMouseWheelListener(new MouseWheelListener() {
	        @Override
	        public void mouseWheelMoved(MouseWheelEvent e) {
	            if (e.isControlDown() && !scenePanelIsSelected) {
	                // Zoom für ausgewählte Tiles (bereits implementiert)
	                double scaleFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
	                for (CustomImageSVGTile tile : addedTiles) {
	                    if (tile.isSelected())
	                        tile.scaleSVG(scaleFactor);
	                }
	                scenePanel.repaint();
	            } else if (e.isControlDown() && scenePanelIsSelected) {
	                // Zoom in und out für das gesamte Panel
	                double zoomFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
	                startPoint = e.getPoint();
	                zoomScenePanel(zoomFactor);
	            } else if (!e.isControlDown() && scenePanelIsSelected) {
	                // Vertikales Scrollen
	                JScrollBar vScrollBar = centerScrollPane.getVerticalScrollBar();
	                int scrollAmount = e.getUnitsToScroll() * vScrollBar.getUnitIncrement();
	                vScrollBar.setValue(vScrollBar.getValue() + scrollAmount);
	            } else if (!e.isControlDown() && e.isShiftDown() && scenePanelIsSelected) {
	                // Horizontales Scrollen
	                JScrollBar hScrollBar = centerScrollPane.getHorizontalScrollBar();
	                int scrollAmount = e.getUnitsToScroll() * hScrollBar.getUnitIncrement();
	                hScrollBar.setValue(hScrollBar.getValue() + scrollAmount);
	            }
	        }
	    });

	    // Verbesserte Tastatursteuerung
	    scenePanel.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyPressed(KeyEvent e) {
	            if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
	                selectAllTiles(true);
	                scenePanel.repaint();
	            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
	                selectAllTiles(false);
	                clearRectangle(false);
	                scenePanel.repaint();
	            }
	            // Neue Tastenkombination zum Skalieren aller ausgewählten Tiles
	            else if (e.getKeyCode() == KeyEvent.VK_PLUS && e.isControlDown()) {
	                scaleSelectedTiles(1.1);
	            } else if (e.getKeyCode() == KeyEvent.VK_MINUS && e.isControlDown()) {
	                scaleSelectedTiles(0.9);
	            } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
	                // Auskommentierter Code zur Entfernung von ausgewählten SVGs
	            }
	        }
	    });
	}

	// Methode zum Aktualisieren der Bildliste, wenn ein neues Verzeichnis ausgewählt wird
	private void loadFolderFiles(File directory) {
	    // In Swing-Thread ausführen, um UI-Thread-Sicherheit zu gewährleisten
	    SwingUtilities.invokeLater(() -> {
	        // Bestehende Logik zur Verwaltung des aktuell ausgewählten Ordners
	        currentFolder = directory;
	        LastUsedDirectory.save(currentFolder);
	        
	        // Entfernen aller bestehenden Komponenten und Datenstrukturen zurücksetzen
	        tilePanel.removeAll();
	        selectedPanel.removeAll();
	        fileToTileMap.clear();
	        selectedFilePanels.clear();
	        allTiles.clear();
	        
	        // SVG-Daten vom Manager holen
	        svgData = svgDataManager.getSVGDataFromSVGInFolder(directory);
	        
	        // Alle SVG-Dateien prüfen
	        File[] allSVGFiles = directory.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".svg"));
	        if (allSVGFiles == null || allSVGFiles.length == 0) {
	            JOptionPane.showMessageDialog(frame, "Keine SVG-Dateien gefunden.");
	            if (svgCanvas != null) {
	                svgCanvas.setURI(null);
	            }
	            
	            // Trotzdem das ImageListView aktualisieren für andere Bildformate
	            updateImageListView(directory);
	            return;
	        }
	        Arrays.sort(allSVGFiles);

	        // Tiles erzeugen und in Panel einfügen
	        for (LinkedList<String> data : svgData) {
	            CustomImageSVGTile tile = new CustomImageSVGTile(data, directory);
	            allTiles.put(tile.getFilename(), tile);

	            JPanel leftRow = createThumbnailRowLeft(data);
	            tilePanel.add(leftRow);
	            fileToTileMap.put(new File(data.get(1)), leftRow);
	        }
	        
	        // ImageListView im zweiten Tab aktualisieren
	        updateImageListView(directory);
	        
	        // UI aktualisieren
	        tilePanel.revalidate();
	        tilePanel.repaint();
	        selectedPanel.revalidate();
	        selectedPanel.repaint();
	    });
	}

	// Separate Methode zur Aktualisierung der ImageListView im zweiten Tab
	private void updateImageListView(File directory) {
	    JTabbedPane tabbedPane = findTabbedPane();
	    if (tabbedPane != null && tabbedPane.getTabCount() >= 2) {
	        JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(1);
	        
	        // Neues ImageListView erstellen
	        JPanel[] panels = ImageListView.createImageListView(directory, scenePanel);
	        JPanel newImageListPanel = panels[0];
	        
	        // Viewport mit dem neuen Panel aktualisieren
	        scrollPane.setViewportView(newImageListPanel);
	        
	        // UI aktualisieren
	        scrollPane.revalidate();
	        scrollPane.repaint();
	    }
	}

	// Hilfsmethode, um das JTabbedPane zu finden
	private JTabbedPane findTabbedPane() {
	    // Das JScrollPane tileScrollPane enthält das JTabbedPane
	    if (tileScrollPane != null && tileScrollPane.getViewport() != null) {
	        Component comp = tileScrollPane.getViewport().getView();
	        if (comp instanceof JTabbedPane) {
	            return (JTabbedPane) comp;
	        }
	    }
	    return null;
	}

	// Methode zum Hervorheben des entsprechenden Elements in der Bilderliste
	private void highlightCorrespondingItemInImageList(String filename, boolean highlight) {
	    // Diese Methode findet und hebt das entsprechende Element in der ImageListView hervor
	    
	    // Zuerst das TabbedPane und das ImageListPanel finden
	    JTabbedPane tabbedPane = findTabbedPane();
	    if (tabbedPane != null && tabbedPane.getTabCount() >= 2) {
	        JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(1); // Bilderliste ist im zweiten Tab
	        JPanel imageListPanel = (JPanel) scrollPane.getViewport().getView();
	        
	        // Alle Komponenten des ImageListPanels durchlaufen
	        Component[] components = imageListPanel.getComponents();
	        for (Component comp : components) {
	            if (comp instanceof JPanel) {
	                JPanel itemPanel = (JPanel) comp;
	                
	                // Suchen nach dem nameLabel, um den Dateinamen zu überprüfen
	                Component[] itemComponents = itemPanel.getComponents();
	                for (Component itemComp : itemComponents) {
	                    if (itemComp instanceof JPanel) {
	                        JPanel leftPanel = (JPanel) itemComp;
	                        Component[] leftComponents = leftPanel.getComponents();
	                        for (Component leftComp : leftComponents) {
	                            if (leftComp instanceof JLabel) {
	                                JLabel nameLabel = (JLabel) leftComp;
	                                if (nameLabel.getText().equals(filename)) {
	                                    // Wenn gefunden, Hintergrundfarbe ändern
	                                    itemPanel.setBackground(highlight ? Color.LIGHT_GRAY : Color.WHITE);
	                                    return;
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	}

	public void resetZoom() {

		scenePanel.setPreferredSize(new Dimension(2000, 2000));

		scenePanel.revalidate();
		scenePanel.repaint();

		loadCurrentSizeOfSelectedTiles();

		// Scrollbars zurücksetzen
		centerScrollPane.getHorizontalScrollBar().setValue(0);
		centerScrollPane.getVerticalScrollBar().setValue(0);

		captureZone = captureZoneReset;
		startPoint = startPointReset;
		currentZoom = 1.0;
	}

	private void zoomScenePanel(double zoomFactor) {
		// Aktuelle Position der Scrollbalken relativ zum Viewport merken
		JViewport viewport = centerScrollPane.getViewport();
		Point viewPosition = viewport.getViewPosition();
		if (currentZoom == 1) {
			tilePositions = saveCurrentSizeAndPositionOfSelectedTiles(tilePositions);
		}
		// Neuen Zoom-Faktor berechnen

		currentZoom *= zoomFactor;

		// Minimalen und maximalen Zoom begrenzen
		currentZoom = Math.max(0.1, Math.min(5.0, currentZoom));
		if (startPoint == null) {
			if (startPointReset == null) {
				startPointReset = new Point(0, 0);

			}
			startPoint = startPointReset;
		}
		if (startPoint != null) {
			startPoint.x *= zoomFactor;
			startPoint.y *= zoomFactor;
			int x = (int) (captureZone.getX() * zoomFactor);
			int y = (int) (captureZone.getY() * zoomFactor);
			int width = (int) (captureZone.getWidth() * zoomFactor);
			int height = (int) (captureZone.getHeight() * zoomFactor);
			captureZone = new Rectangle(x, y, width, height);
		}
		// Neue Größe des scenePanels berechnen
		int newWidth = (int) (2000 * currentZoom); // Ursprüngliche Größe war 2000x2000
		int newHeight = (int) (2000 * currentZoom);
		scenePanel.setPreferredSize(new Dimension(newWidth, newHeight));

		// Position aller Tiles anpassen
		for (CustomImageSVGTile tile : addedTiles) {
			JPanel tilePanel = tile.getPanel();
			int x = (int) (tilePanel.getX() * zoomFactor);
			int y = (int) (tilePanel.getY() * zoomFactor);
			int width = (int) (tilePanel.getWidth() * zoomFactor);
			int height = (int) (tilePanel.getHeight() * zoomFactor);
			tilePanel.setBounds(x, y, width, height);
			// Aktuallisiert die Größe des SVG Canvas
			tile.scaleSVG(zoomFactor);
		}

		// Neu berechnen, wo der Viewport sein sollte, damit der Zoom-Punkt im Zentrum
		// bleibt
		int newX = (int) ((viewPosition.x + startPoint.x) * zoomFactor - startPoint.x);
		int newY = (int) ((viewPosition.y + startPoint.y) * zoomFactor - startPoint.y);

		// Layout aktualisieren
		scenePanel.revalidate();
		// Viewport-Position setzen
		viewport.setViewPosition(new Point(newX, newY));
		// Panel neu zeichnen
		scenePanel.repaint();
	}

	private List<Rectangle> saveCurrentSizeAndPositionOfSelectedTiles(List<Rectangle> positions) {
		positions.clear(); // Alte Daten löschen
		for (CustomImageSVGTile tile : addedTiles) {
			JPanel tilePanel = tile.getPanel();
			Rectangle bounds = new Rectangle(tilePanel.getX(), tilePanel.getY(), tilePanel.getWidth(),
					tilePanel.getHeight());
			positions.add(bounds); // Speichert Position & Größe
		}
		return positions;
	}

	private void loadCurrentSizeOfSelectedTiles() {
		if (tilePositions.isEmpty() || addedTiles == null || addedTiles.isEmpty()) {
			System.out.println("Keine Tiles oder gespeicherten Positionen vorhanden!");
			return;
		}

		int index = 0;
		for (CustomImageSVGTile tile : addedTiles) {
			if (index < tilePositions.size()) {
				Rectangle rect = tilePositions.get(index);
				JPanel tilePanel = tile.getPanel();

				// Position und Größe auf gespeicherten Wert setzen
				tilePanel.setBounds(rect.x, rect.y, rect.width, rect.height);

				// Auch den SVG-Canvas im Tile anpassen
				JSVGCanvas svgCanvas = tile.getSvgCanvas(); // Diese Methode müsste in CustomImageTile existieren
				if (svgCanvas != null) {
					svgCanvas.setSize(rect.width, rect.height);
				}

//	            tile.updateData();
				index++;
			}
		}

		// Layout aktualisieren
		scenePanel.revalidate();
		scenePanel.repaint();
	}

	/**
	 * Ergänzung der CustomImageTile-Klasse um Zugriff auf den SVGCanvas zu
	 * ermöglichen
	 */
	// Diese Methode müsste in der CustomImageTile-Klasse hinzugefügt werden:

	public JSVGCanvas getSvgCanvas() {
		return svgCanvas;
	}

	private void saveCaptureZoneToTempFile() {
		try {
			tempFile = File.createTempFile("captureZone", ".tmp");
			Files.write(Paths.get(tempFile.toURI()),
					(captureZone.x + "," + captureZone.y + "," + captureZone.width + "," + captureZone.height)
							.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void clearRectangle(Boolean repaint) {

		captureZone = null;
		startPoint = null;
		if (repaint)
			scenePanel.repaint();
	}

	private JButton initScreenshotButton(JFrame parentFrame) {
		JButton takeScreenshot = new JButton();
//		try {
//			// SVG laden und in Icon konvertieren
//			BufferedImage svgImg = SVGConverter.loadSvgAsImage("/camera.svg", 24, 24);
//			takeScreenshot.setIcon(new ImageIcon(svgImg));
//		} catch (Exception ex) {
		takeScreenshot.setText("📸"); // Fallback
//		}

		takeScreenshot.setToolTipText("Screenshot-Panel öffnen");
		takeScreenshot.addActionListener(e -> {
			zoomScenePanel(10.0);
			captureZoneReset = captureZone;
			startPointReset = startPoint;
			clearRectangle(true);
			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.schedule(() -> {

				File screenShotFile = ScreenShotHandler.takeScreenshot(captureZoneReset, scenePanel, frame);
				captureZone = captureZoneReset;
				startPoint = startPointReset;
				zoomScenePanel(0.10);
				scheduler.shutdown();
//				SVGTileViewerAppOutSource.showScreenshotDialog(parentFrame, screenShotFile);

				ScreenshotPanel.showScreenshotDialog(parentFrame, screenShotFile);
				scenePanel.repaint();
			}, 300, TimeUnit.MILLISECONDS);

		});
		return takeScreenshot;
	}

	private JPanel addEditTextField(JPanel row, File file) {
		JLabel nameLabel = new JLabel(file.getName());
		nameLabel.setForeground(Color.BLUE);
		nameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		nameLabel.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				String newName = JOptionPane.showInputDialog(frame, "Neuer Dateiname:", file.getName());
				if (newName != null && !newName.trim().isEmpty()) {
					if (!newName.toLowerCase().endsWith(".svg"))
						newName += ".svg";
					File renamed = new File(file.getParent(), newName);
					if (renamed.exists()) {
						JOptionPane.showMessageDialog(frame, "Datei existiert bereits.");
						return;
					}
					if (file.renameTo(renamed)) {
						nameLabel.setText(newName);
						svgDataManager.imageCache.remove(file.getAbsolutePath());

						// Aktualisiere Tile-Mapping nach Umbenennung
						CustomImageSVGTile tile = allTiles.remove(file.getName());
						if (tile != null) {
							tile.setFilename(newName);
							allTiles.put(newName, tile);
						}

						loadFolderFiles(currentFolder); // Refresh alle
					} else
						JOptionPane.showMessageDialog(frame, "Umbenennen fehlgeschlagen.");
				}
			}
		});
		row.add(nameLabel);
		return row;
	}

	private JPanel addCheckBox(JPanel row, CustomImageSVGTile tile) {
		JCheckBox cb = new JCheckBox();
		cb.setSelected(true);
		cb.setOpaque(false);
		cb.addActionListener(e -> {
			if (!cb.isSelected()) {
				removeSelectedSVG(row, tile);
			} else {
				// Re-add to scene panel if checkbox is checked again
				scenePanel = SVGTileViewerAppOutSource.setTileVisible(tile, true, scenePanel, centerScrollPane);
			}
		});
		row.add(cb);
		return row;
	}

	private void removeSelectedSVG(JPanel row, CustomImageSVGTile tile) {
		// TODO Auto-generated method stub
		selectedPanel.remove(row);
		selectedPanel.revalidate();
		selectedPanel.repaint();

		// Remove from scene panel
		scenePanel = SVGTileViewerAppOutSource.setTileVisible(tile, false, scenePanel, centerScrollPane);
	}

}