package main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
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

public class SvgTileViewerApp8 {

	private static ListenerCustomTileUpdate listenerCustomTileUpdate;
	private static ListenerLeftTiles listnerLeftTiels;
	private static ListenerPathPanel pathPanelListener;

	private boolean isLoadingFolder = false;
	private boolean scenePanelIsSelected;
	private boolean allowDrag;
	private boolean drawPath;
	private File currentFolder;

	private JSVGCanvas svgCanvas;
	private JFrame frame;

	private JPanel selectedPanel;
	private JPanel rightPanel;
	private JPanel scenePanel = new JPanel(null);
	private JScrollPane tileScrollPane;
	private JScrollPane selectedScrollPane;
	private JScrollPane centerScrollPane;

	private Rectangle captureZone;
	private Point startPoint = new Point(0, 0);
	private double currentZoom = 1.0;
	private Rectangle captureZoneReset;
	private Point startPointReset;
	private GeneralPath path;
	private GeneralPath selectionPath;

	private boolean isDrawing;
	private Double currentPath;

	// For renaming
	private String oldFileName;
	private JTextField nameField;

	private File tempFile;
	private File[] svgFolders;

//	private ArrayList<JPanel> tilePanel = new ArrayList<>();
	List<JPanel> tilePanels = new ArrayList<JPanel>();
	private ArrayList<JPanel> rightRows = new ArrayList<JPanel>();
	SVGDataManager svgDataManager = new SVGDataManager();
	private List<CustomImageSVGTile> addedTiles = new ArrayList<>();

	// Need to be in list because for different Tabs
	private LinkedList<Map<String, CustomImageSVGTile>> allTiles = new LinkedList<>();
	private LinkedList<LinkedList<LinkedList<String>>> svgFileData = new LinkedList<>();
	private List<Rectangle> tilePositions = new ArrayList<>(); // Speichert Positionen aller Kacheln

	private static Color pathPointColor = Color.BLUE;
	private static int pathLineColorALpha = 100;
	private static Color pathFillColor = new Color(0,0,255,pathLineColorALpha);

	private List<Panel> pathPoints = new ArrayList<>();
	private boolean addPathPoint = true;
	private double minX;
	private double maxX;

	private double minY;
	private double maxY;
	private double imageCapturingWidth;
	private double imageCapturingHeight;

	// Am Anfang Ihrer Anwendung oder im static Block
	static {
		System.setProperty("java.awt.useSystemAAFontSettings", "off");
		System.setProperty("swing.aatext", "false");
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> new SvgTileViewerApp8().createAndShowUI());
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

	private void loadFolderFiles(File folder, int tabIndex) {
		SwingUtilities.invokeLater(() -> {
			// Nur das spezifische Panel für diesen Tab bearbeiten
			if (tabIndex >= tilePanels.size()) {
				System.err.println("Tab index out of bounds: " + tabIndex);
				return;
			}

			JPanel targetPanel = tilePanels.get(tabIndex);
			targetPanel.removeAll();

			// Sicherstellen, dass allTiles groß genug ist
			while (allTiles.size() <= tabIndex) {
				allTiles.add(new HashMap<String, CustomImageSVGTile>());
			}
			allTiles.get(tabIndex).clear();

			// Daten für diesen Tab verwenden
			if (tabIndex < svgFileData.size()) {
				LinkedList<LinkedList<String>> currentTabData = svgFileData.get(tabIndex);

				for (LinkedList<String> d2 : currentTabData) {
					CustomImageSVGTile tile = new CustomImageSVGTile(d2, folder);
					allTiles.get(tabIndex).put(tile.getFilename(), tile);
					JPanel leftRow = ListItemLeft.createThumbnailRowLeft(d2, frame, tabIndex, svgDataManager,
							listnerLeftTiels);
					targetPanel.add(leftRow);
				}
			}

			targetPanel.revalidate();
			targetPanel.repaint();
		});
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
			svgFolders = svgDataManager.getSVGDataDirectories(currentFolder);
			LastUsedDirectory.save(currentFolder);

			// Cache leeren beim Ordnerwechsel
			svgDataManager.imageCache.clear();
			System.gc();

			new Thread(() -> {
				isLoadingFolder = true;
				System.out.println("chooseFolder: " + svgFolders[0]);

				loadFolderFiles(svgFolders[0], 0);
				isLoadingFolder = false;
			}).start();
		}
	}

//	private void scrollToThumbnailInLeftPanel(File file) {
//		JPanel leftThumbnail = fileToTileMap.get(file);
//		if (leftThumbnail != null) {
//			SwingUtilities.invokeLater(() -> {
//				Rectangle rect = leftThumbnail.getBounds();
//				rect.y = leftThumbnail.getY();
//				tileScrollPane.getViewport().scrollRectToVisible(rect);
//			});
//		}
//	}

	private void startMemoryCleanupTimer() {
		new javax.swing.Timer(60000, e -> System.gc()).start();
	}

	private void createAndShowUI() {
		setListeners();

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

		// Linkes Panel mit Tabs
		svgFolders = svgDataManager.getSVGDataDirectories(currentFolder);

		// SVG-Daten für alle Ordner laden
		try {
			for (File f : svgFolders) {
				svgFileData.add(svgDataManager.getSVGDataFromSVGInFolder(f));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		JTabbedPane leftTabbedPane = new JTabbedPane();

		// Alle Tabs erstellen
		for (int i = 0; i < svgFolders.length; i++) {
			JPanel tilePanel = new JPanel();
			tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
			tilePanel.setBackground(Color.WHITE);
			tilePanels.add(tilePanel);

			JScrollPane scrollPane = new JScrollPane(tilePanels.get(i));
			scrollPane.getVerticalScrollBar().setUnitIncrement(16);
			leftTabbedPane.addTab(svgFolders[i].getName(), scrollPane);

			// Für das erste Tab die Referenz beibehalten
			if (i == 0) {
				tileScrollPane = scrollPane;
			}
		}

		// ChangeListener für lazy loading hinzufügen
		leftTabbedPane.addChangeListener(e -> {
			int selectedIndex = leftTabbedPane.getSelectedIndex();
			if (selectedIndex >= 0 && selectedIndex < svgFolders.length) {
				loadFolderFiles(svgFolders[selectedIndex], selectedIndex);
			}
		});

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(leftTabbedPane, BorderLayout.CENTER);
		leftPanel.setPreferredSize(new Dimension(300, 0));
		frame.add(leftPanel, BorderLayout.WEST);

		// Erstes Tab initial laden
		if (svgFolders.length > 0) {
			loadFolderFiles(svgFolders[0], 0);
		}
		// Zentrales Panel für CustomImageTiles
		scenePanel = new JPanel(null) {

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawCaptureZoneInPaintMethod(g);
				drawCaptureZoneInPaintMethodFree(g, pathPointColor, pathFillColor, pathLineColorALpha);
				drawCaptureZone((int) minX, (int) minY, (int) imageCapturingWidth, (int) imageCapturingHeight);

			}
		};

		scenePanel.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(MouseEvent e) {
				if (!drawPath) {
					drawCaptureZone(e);
				} else {
//					drawCaptureZonePath(e);
//					if (selectionPath != null) {
//						selectionPath.lineTo(e.getX(), e.getY());
//						scenePanel.repaint();
//					}

					if (isDrawing && currentPath != null) {
						currentPath.lineTo(e.getX(), e.getY());
						scenePanel.repaint(); // Neuzeichnung des Panels
					}
				}

			}

		});

		scenePanel.setPreferredSize(new Dimension(2000, 2000));
		scenePanel.setBackground(Color.WHITE);
		scenePanel.setFocusable(true);

		scenePanel.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				startPoint = e.getPoint();
				if (drawPath) {
//					selectionPath = new GeneralPath();
//					selectionPath.moveTo(startPoint.x, startPoint.y);

					isDrawing = true;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!drawPath) {
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
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				scenePanel.requestFocusInWindow();
				selectAllTiles(false);

				CustomPathPanel pp = new CustomPathPanel(pathPanelListener);
				Panel pan = pp.getPanel();
				pan.setLocation(e.getX(), e.getY());

				if (addPathPoint) {
					pathPoints.add(pan);
					System.out.println("add x: " + pan.getX() + " y: " + pan.getY());
					scenePanel.add(pathPoints.get(pathPoints.size() - 1));
					scenePanel.repaint();
				}

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
					zoomPath(zoomFactor);
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
					for (Panel p : pathPoints) {
						scenePanel.remove(p);
					}
					pathPoints.clear();
					scenePanel.repaint();
				}
				// Neue Tastenkombination zum Skalieren aller ausgewählten Tiles
				else if (e.getKeyCode() == KeyEvent.VK_PLUS && e.isControlDown()) {
					scaleSelectedTiles(1.1);
				} else if (e.getKeyCode() == KeyEvent.VK_MINUS && e.isControlDown()) {
					scaleSelectedTiles(0.9);
				} else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
//					System.out.println("Pressed Delete at MainWindow");
					for (Component component : selectedPanel.getComponents()) {
//						if (!(component instanceof JPanel))
//							continue;

						JPanel panel = (JPanel) component;
						String id = (String) panel.getClientProperty("id");
						System.err.println(id);
						int index = 0;
						for (CustomImageSVGTile t : addedTiles) {
							if (t.getID().equals(id) && t.isSelected()) {
								try {
									removeSelectedSVG(rightRows.get(index), t);
									rightRows.remove(index);
								} catch (Exception e2) {

									System.err.println("ListenerTileUpdate KeyEvent.Delete");
								}

							}
							index++;
						}

					}
//					int i = 0;
//					for (CustomImageTile t : addedTiles) {
//						if (t.isSelected()) {
//
//							for (Component component : t.getPanel().getComponents()) {
//								if (!(component instanceof JPanel))
//									continue;
//								JPanel panel = (JPanel) component;
//								String id = (String) panel.getClientProperty("id");
//								System.err.println(id);
//								selectedPanel.remove(panel);
//								selectedPanel.revalidate();
//								selectedPanel.repaint();
//
//							}
//							SVGTileViewerAppOutSource.setTileVisible(t, false, scenePanel, centerScrollPane);
//						}
//						i++;
//					}
				}
				if (e.getKeyCode() == KeyEvent.VK_D && e.isControlDown()) {

					selectAllTilesMouseInvisible();

				}
				if (e.getKeyCode() == KeyEvent.VK_P && e.isControlDown()) {
					drawPath = !drawPath;
					selectAllTilesMouseInvisible();

				}
				if (e.getKeyCode() == KeyEvent.VK_T) {
					for (Panel p : pathPoints) {
						System.out.println("x: " + p.getX() + " y: " + p.getY());
					}

				}

			}
		});

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
			leftPanel.setVisible(!leftPanel.isVisible());
			toggleLeft.setText(leftPanel.isVisible() ? "⮜" : "⮞");
		});
		JButton toggleRight = new JButton("⮞");
		toggleRight.addActionListener(e -> {
			rightPanel.setVisible(!rightPanel.isVisible());
			toggleRight.setText(rightPanel.isVisible() ? "⮞" : "⮜");
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

		JButton scaleUpBtn = new JButton("Image Edit");
//		File jarFile = null;
//		try {
//			jarFile = new File(ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//		} catch (URISyntaxException e1) {
//			e1.printStackTrace();
//		}
//		File programDir = jarFile.getParentFile();
//
//		// Screenshot-Verzeichnis erstellen
//		File screenshotsDir = new File(programDir, "screenshots");
//		String fileName = "A_ScreenShot1747778089222.png";
//		File screenshotFile = new File(screenshotsDir, fileName);
		scaleUpBtn.addActionListener(e -> ScreenshotPanel.showScreenshotDialog(frame, null));

		JButton takeScreenShot = initScreenshotButton(frame);

		JButton quickSVG = new JButton("QuickSVG");
		quickSVG.addActionListener(e -> createQuickSVG());

		controlPanel.add(takeScreenShot);
		controlPanel.add(selectAllBtn);
		controlPanel.add(selectNoneBtn);
		controlPanel.add(scaleUpBtn);
		controlPanel.add(quickSVG);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(controlPanel, BorderLayout.CENTER);

		JButton resetZoomBtn = new JButton("Zoom zurücksetzen");
		resetZoomBtn.addActionListener(e -> resetZoom());
		controlPanel.add(resetZoomBtn);

		JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		togglePanel.add(toggleLeft);
		togglePanel.add(toggleRight);
		bottomPanel.add(togglePanel, BorderLayout.SOUTH);

		frame.add(bottomPanel, BorderLayout.SOUTH);

//		loadFolderFiles(currentFolder);

		frame.setLocationRelativeTo(null);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximiert das Fenster
		frame.setVisible(true);
		scenePanel.requestFocusInWindow();
		centerScrollPane.setFocusable(false);

		startMemoryCleanupTimer();
	}

	private void setListeners() {

		listenerCustomTileUpdate = new ListenerCustomTileUpdate() {

			@Override
			public void onTileHover(CustomImageSVGTile tile, boolean isHovered) {
				if (allowDrag) {

				}
				System.out.println("hover " + tile.getFilename());
				highlightCorrespondingItemInRightPanel(tile, isHovered);
			}

			@Override
			public void drawCaptureZoneFromCustomTileSetStartPoint(Point point) {

				startPoint = point;
			}

			@Override
			public void drawCaptureZoneFromCustomTile(Point e) {

				if (!drawPath) {
					drawCaptureZoneOnTileHover(e);
				} else {
					drawCaptureZonePathOnTileHover(e);
				}

			}

			@Override
			public void onVK_DELETE_typed(CustomImageSVGTile tile) {

//				addedTiles.remove(tile);
			}

			@Override
			public void removeSelectedSVGFromListener(JPanel row, CustomImageSVGTile tile) {

				removeSelectedSVG(row, tile);
			}

		};

		listnerLeftTiels = new ListenerLeftTiles() {

			@Override
			public void onClick(LinkedList<String> data) {

				CustomImageSVGTile t = new CustomImageSVGTile(data);
				t.setUpdateListener(listenerCustomTileUpdate);
				addedTiles.add(t);
				JPanel returnValues[] = new JPanel[2];
				returnValues[0] = createThumbnailRowRight(addedTiles.get(addedTiles.size() - 1), scenePanel,
						svgDataManager)[0];
				returnValues[1] = createThumbnailRowRight(addedTiles.get(addedTiles.size() - 1), scenePanel,
						svgDataManager)[1];
				rightRows.add(returnValues[0]);
				scenePanel = returnValues[1];
				selectedPanel.add(rightRows.get(rightRows.size() - 1));
				selectedPanel.revalidate();
				selectedPanel.repaint();
				scenePanel = SVGTileViewerAppOutSource.setTileVisible(addedTiles.get(addedTiles.size() - 1), true,
						scenePanel, centerScrollPane);
				scenePanel.setComponentZOrder(addedTiles.get(addedTiles.size() - 1).getPanel(), 0);
			}

			@Override
			public void actualizeTileMapping(String name, int index) {

//				CustomImageTile tile = allTiles.get(index).remove();
//				if (tile != null) {
//
//					tile.setFilename(name);
//					allTiles.get(index).put(name, tile);
//				}

				loadFolderFiles(currentFolder, 0); // Refresh alle
			}
		};

		pathPanelListener = new ListenerPathPanel() {

			@Override
			public void mouseExited() {

				addPathPoint = true;
			}

			@Override
			public void mouseEntered() {

				addPathPoint = false;
			}

			@Override
			public void mouseClickedTwice(MouseEvent e, Panel p) {

				pathPoints.remove(p);
				scenePanel.remove(p);
				scenePanel.repaint();
			}

			@Override
			public void mouseMoved() {
				scenePanel.repaint();
			}
		};
	}

	private Object createQuickSVG() {

		return null;
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
			if (captureZone != null) {
				int x = (int) (captureZone.getX() * zoomFactor);
				int y = (int) (captureZone.getY() * zoomFactor);
				int width = (int) (captureZone.getWidth() * zoomFactor);
				int height = (int) (captureZone.getHeight() * zoomFactor);
				captureZone = new Rectangle(x, y, width, height);
			}
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

	public void zoomPath(double zoomFactor) {
		// Skalierung um 10x und Verschiebung um (20, 30)
		for (Panel p : pathPoints) {
			p.setLocation((int) (p.getX() * zoomFactor), (int) (p.getY() * zoomFactor));
		}
		// Jetzt hat der Pfad 10-fache Größe und ist verschoben
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
		path = null;
		minX = 0;
		maxX = 0;

		minY = 0;
		maxY = 0;
		imageCapturingWidth = 0;
		imageCapturingHeight = 0;
		for (Panel p : pathPoints)
			scenePanel.remove(p);
		pathPoints.clear();
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
//			zoomScenePanel(10.0);
			takeScreenShot();

//			try {
//				if (captureZone == null) {
//					screenshot = createImageFromPanel(scenePanel);
//				} else {
//			BufferedImage screenshot = ScreenShotHandler.createImageFromPanelRegion(scenePanel, captureZone);
//			createPathFromPathPointsAndZoom(path,10.0f);
//			capturePathAreaAsScreenshot(path, scenePanel, captureZone, screenshot);
//			createPathFromPathPointsAndZoom(path,0.1f);
//			zoomScenePanel(0.1);
		});
		return takeScreenshot;
	}

	private void takeScreenShot() {

		zoomScenePanel(3.0f);
		zoomPath(3.0f);
		pathPointColor = Color.WHITE;
		pathLineColorALpha = 0;
		pathFillColor = new Color(0,0,255,pathLineColorALpha);
		
		captureZoneReset = captureZone;
		startPointReset = startPoint;
		// Removes the visible representation of the Rectangle
		clearRectangle(true);
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.schedule(() -> {

			BufferedImage screenShot = ScreenShotHandler.takeScreenshotII(captureZoneReset, scenePanel, frame);
			ScreenShotHandler.saveScreenshot(screenShot);
			capturePathAreaAsScreenshot(path, scenePanel, captureZoneReset, screenShot);
			captureZone = captureZoneReset;
			startPoint = startPointReset;

			pathPointColor = Color.BLUE;
			pathLineColorALpha = 100;
			pathFillColor = new Color(0,0,255,pathLineColorALpha);
			zoomScenePanel(0.7f);
			zoomPath(0.7f);

			scheduler.shutdown();
//			SVGTileViewerAppOutSource.showScreenshotDialog(parentFrame, screenShotFile);
//			screenShotFile
//			screenShotFile = ScreenshotPanel.replaceColorsWithClosestMatch(screenShotFile);

//			ScreenshotPanel.replaceColorsWithClosestMatch(ScreenshotPanel.ImageFile);
//			ConverterImageToSvg.convertImageToSVG(ScreenshotPanel.ImageFile);

//			ScreenshotPanel.showScreenshotDialog(frame, screenShotFile);
			scenePanel.repaint();
		}, 300, TimeUnit.MILLISECONDS);
	}

	private JPanel addEditTextField(JPanel row, File file) {
		JLabel nameLabel = new JLabel(file.getName());
		nameLabel.setForeground(Color.BLUE);
		nameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		nameLabel.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
//				String newName = JOptionPane.showInputDialog(frame, "Neuer Dateiname:", file.getName());
//				if (newName != null && !newName.trim().isEmpty()) {
//					if (!newName.toLowerCase().endsWith(".svg"))
//						newName += ".svg";
//					File renamed = new File(file.getParent(), newName);
//					if (renamed.exists()) {
//						JOptionPane.showMessageDialog(frame, "Datei existiert bereits.");
//						return;
//					}
//					if (file.renameTo(renamed)) {
//						nameLabel.setText(newName);
//						svgDataManager.imageCache.remove(file.getAbsolutePath());
//
//						// Aktualisiere Tile-Mapping nach Umbenennung
//						CustomImageTile tile = allTiles.remove(file.getName());
//						if (tile != null) {
//							tile.setFilename(newName);
//							allTiles.put(newName, tile);
//						}
//
//						loadFolderFiles(currentFolder, 0); // Refresh alle
//					} else
//						JOptionPane.showMessageDialog(frame, "Umbenennen fehlgeschlagen.");
//				}
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
//				removeSelectedSVG(row, tile);
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

		selectedPanel.remove(row);
		selectedPanel.revalidate();
		selectedPanel.repaint();

		// Remove from scene panel
		scenePanel = SVGTileViewerAppOutSource.setTileVisible(tile, false, scenePanel, centerScrollPane);
	}

	private void selectAllTilesMouseInvisible() {

		for (CustomImageSVGTile tile : addedTiles) {
			tile.toggleDragEnabled();
		}
	}

	protected void drawCaptureZoneInPaintMethod(Graphics g) {

		if (!drawPath) {
			if (captureZone != null) {
				g.setColor(Color.RED);
				g.drawRect(captureZone.x, captureZone.y, captureZone.width, captureZone.height);

			}
		} else {
			Graphics2D g2d = (Graphics2D) g;
			// Anti-Aliasing für glattere Linien
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			// Aktuellen Pfad zeichnen
			if (currentPath != null) {
				g2d.setColor(Color.BLACK);
				g2d.setStroke(new BasicStroke(2.0f));
				g2d.draw(currentPath);
			}
		}
	}

	protected void drawCaptureZoneInPaintMethodFree(Graphics g, Color pathFillColor, Color pathPointColor,
			int pathLineColorALpha) {
		if (pathPoints.size() < 2)
			return; // Mindestens 2 Punkte benötigt

		Graphics2D g2d = (Graphics2D) g;

		// Anti-Aliasing für glattere Linien
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Pfad erstellen
		createPathFromPathPointsAndZoom(1.0f);

		// Pfad zeichnen
		g2d.setColor(pathFillColor);
		g2d.setStroke(new BasicStroke(2.0f));
		g2d.draw(path);

		// Optional: Geschlossenen Pfad zeichnen (wenn gewünscht)
//	    if (closePath && pathPoints.size() > 2) {
		if (true && pathPoints.size() > 2) {
//			path.closePath();
			g2d.setColor(pathFillColor); // Transparentes Blau
			g2d.fill(path);
		}
	}

	private void createPathFromPathPointsAndZoom(float zoom) {
		path = new GeneralPath();
		// Ersten Punkt setzen (Mitte des ersten Panels)
		Point firstPoint = pathPoints.get(0).getLocation();
		int firstCenterX = firstPoint.x + pathPoints.get(0).getWidth() / 2;
		int firstCenterY = firstPoint.y + pathPoints.get(0).getHeight() / 2;
		path.moveTo(firstCenterX, firstCenterY);

		if (!pathPoints.isEmpty()) {
			minX = pathPoints.stream().mapToDouble(p -> p.getLocation().x + p.getWidth() / 2.0).min().getAsDouble();
			maxX = pathPoints.stream().mapToDouble(p -> p.getLocation().x + p.getWidth() / 2.0).max().getAsDouble();
			// Analog für Y-Werte
			minY = pathPoints.stream().mapToDouble(p -> p.getLocation().y + p.getHeight() / 2.0).min().getAsDouble();
			maxY = pathPoints.stream().mapToDouble(p -> p.getLocation().y + p.getHeight() / 2.0).max().getAsDouble();

			imageCapturingWidth = maxX - minX;
			imageCapturingHeight = maxY - minY;
		}

		for (int i = 1; i < pathPoints.size(); i++) {
			Point p = pathPoints.get(i).getLocation();
			int centerX = p.x + pathPoints.get(i).getWidth() / 2;
			int centerY = p.y + pathPoints.get(i).getHeight() / 2;
			path.lineTo(centerX, centerY);
		}
	}

	private BufferedImage capturePathAreaAsScreenshot(GeneralPath path, JPanel scenePanel, Rectangle bounds,
			BufferedImage resultImage) {
//		Panel p = new Panel();
//		scenePanel.add(p);
//		p.setSize(new Dimension(5, 5));
		Area area = new Area(path);

		WritableRaster raster = resultImage.getRaster();

		int yRGB = 0;
		for (int y = 0; y < imageCapturingHeight; y++) {
			for (int x = 0; x < imageCapturingWidth; x++) {
				double worldX = minX + x;
				double worldY = minY + y;
				if (!area.contains(worldX, worldY)) {
					raster.setPixel(x, y, new int[] { 255, 255, 255 }); // RGB Weiß
				}
			}
		}

		try {
			File jarFile = new File(
					ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			File programDir = jarFile.getParentFile();

			// Screenshot-Verzeichnis erstellen
			File screenshotsDir = new File(programDir, "screenshots");
			if (!screenshotsDir.exists()) {
				screenshotsDir.mkdir();
			}

			String fileName = "A_ScreenShot_Path" + System.currentTimeMillis() + ".png";

			File screenshotFile = new File(screenshotsDir, fileName);
			ImageIO.write(resultImage, "png", screenshotFile);
			JOptionPane.showMessageDialog(frame, "Screenshot saved as " + screenshotFile.getName());
		} catch (Exception e) {

		}

		return resultImage;
	}

//	private BufferedImage capturePathAreaAsScreenshot(GeneralPath path, JPanel scenePanel) {
//		// Bounding Rectangle des Pfades ermitteln
////		Rectangle bounds = path.getBounds();
//		Rectangle bounds = new Rectangle((int) minX, (int) minY, (int) imageCapturingWidth, (int) imageCapturingHeight);
//		System.out.println(bounds.width + " " + bounds.height);
//		// Panel als Bild erfassen
////		BufferedImage panelImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
////		BufferedImage panelImage =	new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
////		Graphics2D g2d = panelImage.createGraphics();
////		paintComponent(g2d); // oder paint(g2d) je nach Implementierung
////		g2d.dispose();
//
//		// Ergebnis-Bild nur in der Größe des Pfad-Bereichs
//		BufferedImage resultImage = new BufferedImage((int) imageCapturingWidth, (int) imageCapturingHeight,
//				BufferedImage.TYPE_INT_ARGB);
//
//		// Pixel innerhalb des Pfades kopieren
////		Panel xx = new Panel();
////		xx.setSize(new Dimension(10, 10));
////		xx.setBackground(Color.red);
////		scenePanel.add(xx);
//
//		int yRGB = 0;
//		for (double y = minY; y < imageCapturingHeight + minY; y++) {
//			int xRGB = 0;
//			for (double x = minX; x < imageCapturingWidth + minX; x++) {
////				int panelX = (int) (bounds.x + x);
////				int panelY = (int) (bounds.y + y);
////				xx.setLocation((int) x, (int) y);
//
//				if (path.contains(x, y)) {
//					// Pixel innerhalb des Pfades kopieren
//					try {
//						int rgb = ScreenShotHandler.getRGBAt(scenePanel, (int) x, (int) y);
//						resultImage.setRGB((int) xRGB, (int) yRGB, rgb);
////						System.err.println("rgb ... " + rgb);
////						xx.setBackground(Color.green);
////						System.err.println("xRGB: " + xRGB + " yRGB: " + yRGB + "  x: " + x + "  y: " + y + "   iii");
//					} catch (Exception e) {
//						
////						resultImage.setRGB((int)xRGB, (int)yRGB,  0x00000000);
////						System.err.println("capturePathAreaAsScreenshot: IndexOutOfBounds Exception in if(path.contains ...");
////						System.err.println("xRGB: " + xRGB + " yRGB: " + yRGB + "  x: " + x + "  y: " + y);
////						System.err.println(x + " " + y
////								+ " capturePathAreaAsScreenshot: IndexOutOfBounds Exception in if(path.contains ...");
////						xx.setBackground(Color.yellow);
////						xx.setBackground(new Color(rgb));
//					}
//
//				} else {
//					// Pixel außerhalb transparent machen (Alpha = 0)
//					try {
//						resultImage.setRGB((int) xRGB, (int) yRGB, 0x00000000);
////						xx.setBackground(Color.blue);
////						System.err.println("xRGB: " + xRGB + " yRGB: " + yRGB + "  x: " + x + "  y: " + y + "   iii");
//					} catch (Exception e) {
////						resultImage.setRGB((int)xRGB, (int)yRGB, 0x00000000);
////						xx.setBackground(Color.red);
////						System.err.println("xRGB: " + xRGB + " yRGB: " + yRGB + "  x: " + x + "  y: " + y);
//					}
//
//				}
//
//				xRGB++;
//			}
//			yRGB++;
//		}
////		for (int y = 0; y < bounds.height; y++) {
////			for (int x = 0; x < bounds.width; x++) {
////				int panelX = bounds.x + x;
////				int panelY = bounds.y + y;
////				xx.setLocation(x, y);
////				if (path.contains(panelX, panelY)) {
////					// Pixel innerhalb des Pfades kopieren
////					try {
////						int rgb = panelImage.getRGB(panelX, panelY);
////						resultImage.setRGB(x, y, rgb);
////						System.err.println("rgb ... " + rgb);
////					} catch (Exception e) {
////						
////						resultImage.setRGB(x, y, 1);
//////						System.err.println("capturePathAreaAsScreenshot: IndexOutOfBounds Exception in if(path.contains ...");
////						System.err.println(x + " " + y
////								+ " capturePathAreaAsScreenshot: IndexOutOfBounds Exception in if(path.contains ...");
////					}
////
////				} else {
////					// Pixel außerhalb transparent machen (Alpha = 0)
////					resultImage.setRGB(x, y, 0x00000000);
////				}
////			}
////		}
//		try {
//			File jarFile = new File(
//					ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//			File programDir = jarFile.getParentFile();
//
//			// Screenshot-Verzeichnis erstellen
//			File screenshotsDir = new File(programDir, "screenshots");
//			if (!screenshotsDir.exists()) {
//				screenshotsDir.mkdir();
//			}
//
//			String fileName = "A_ScreenShot_Path" + System.currentTimeMillis() + ".png";
//
//			File screenshotFile = new File(screenshotsDir, fileName);
//			ImageIO.write(resultImage, "png", screenshotFile);
//			JOptionPane.showMessageDialog(frame, "Screenshot saved as " + screenshotFile.getName());
//		} catch (Exception e) {
//			
//		}
//
//		return resultImage;
//	}

//	private void captureSelectedPixelsFromPanel() {
//	    BufferedImage panelImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
//	    Graphics2D g2d = panelImage.createGraphics();
//	    paint(g2d); // Panelinhalt zeichnen
//	    g2d.dispose();
//
//	    for (int y = 0; y < panelImage.getHeight(); y++) {
//	        for (int x = 0; x < panelImage.getWidth(); x++) {
//	            if (selectionPath != null && selectionPath.contains(x, y)) {
//	                int rgb = panelImage.getRGB(x, y);
//	                // tu etwas mit dem Pixel
//	            }
//	        }
//	    }
//	}

//	protected void drawCaptureZoneInPaintMethodFree(Graphics g) {
//
//		if (pathPoints.size() > 1) {
//			g.setColor(Color.BLUE);
//			if (path == null) {
//				path = new GeneralPath();
//				path.moveTo(startPoint.x, startPoint.y);
//			}
//
//			for (int i = 1; i < pathPoints.size(); i++) {
//				Point p1 = pathPoints.get(i - 1).getLocation();
//				Point p2 = pathPoints.get(i).getLocation();
//				path.moveTo(p1.x, p1.y);
//				path.lineTo(p2.x, p2.y);
//				// Draw line between center points
//				g.drawLine(p1.x + pathPoints.get(i - 1).getWidth() / 2, p1.y + pathPoints.get(i - 1).getHeight() / 2,
//						p2.x + pathPoints.get(i).getWidth() / 2, p2.y + pathPoints.get(i - 1).getHeight() / 2);
//
//				Graphics2D g2d = (Graphics2D) g;
//				// Anti-Aliasing für glattere Linien
//				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
//				// Aktuellen Pfad zeichnen
//				if (currentPath != null) {
//					g2d.setColor(Color.BLACK);
//					g2d.setStroke(new BasicStroke(2.0f));
//					g2d.draw(currentPath);
//				}
//			}
//		}
//	}

	private void drawCaptureZone(int x, int y, int width, int height) {

		if (startPoint != null) {

			captureZone = new Rectangle(x, y, width, height);
			captureZoneReset = captureZone;
			startPointReset = startPoint;
			scenePanel.repaint();
		}
	}

	private void drawCaptureZone(MouseEvent e) {

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

	private void drawCaptureZoneOnTileHover(Point p) {
		if (startPoint != null) {
			int x = Math.min(startPoint.x, p.x);
			int y = Math.min(startPoint.y, p.y);
			int width = Math.abs(startPoint.x - p.x);
			int height = Math.abs(startPoint.y - p.y);
			captureZone = new Rectangle(x, y, width, height);
			captureZoneReset = captureZone;
			startPointReset = startPoint;
			scenePanel.repaint();
		}
	}

	private void drawCaptureZonePathOnTileHover(Point e) {
		if (startPoint != null) {
			if (path == null) {
				path = new GeneralPath();

				path.moveTo(startPoint.x, startPoint.y);
			}
			path.lineTo(e.getX(), e.getY());
			scenePanel.repaint();
		}
	}

	public void clearPath() {
		captureZone = null;
		startPoint = null;
		path = null;
		minX = 0;
		maxX = 0;

		minY = 0;
		maxY = 0;
		imageCapturingWidth = 0;
		imageCapturingHeight = 0;
		for (Panel p : pathPoints)
			scenePanel.remove(p);
		pathPoints.clear();

		currentPath = null;
		scenePanel.repaint();
	}

}