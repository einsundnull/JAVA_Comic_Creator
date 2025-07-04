package main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;

public class SvgTileViewerApp {

	private static ListenerCustomTileUpdate listenerCustomTileUpdate;
	private static ListenerLeftTiles listnerLeftListItem;
	private static ListenerRightTiles listnerRightListItems;
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
	private LinkedList<LinkedList<LinkedList<String>>> svgFoldersImages = new LinkedList<>();
	private List<Rectangle> tilePositions = new ArrayList<>(); // Speichert Positionen aller Kacheln

	private List<Panel> pathPoints = new ArrayList<>();
	private boolean addPathPoint = true;
	private double minX;
	private double maxX;

	private double minY;
	private double maxY;
	private double imageCapturingWidth;
	private double imageCapturingHeight;
	private JTabbedPane leftTabbedPane;
	private JPanel leftPanel;
	private GeneralPath pathReset;
	private List<Panel> pathPointsReset;
	private ArrayList<Color> allColorsList;
	private ArrayList<DataClassColorHistory> colorHistory = new ArrayList<>();

	// Am Anfang Ihrer Anwendung oder im static Block
	static {
		System.setProperty("java.awt.useSystemAAFontSettings", "off");
		System.setProperty("swing.aatext", "false");
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new SvgTileViewerApp().createAndShowUI());
	}

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

	private void scaleSelectedTiles(double scaleFactor) {
		for (CustomImageSVGTile tile : addedTiles) {
			if (tile.isSelected())
				tile.scaleSVG(scaleFactor);
		}
		scenePanel.repaint();
	}

	private void loadFolderFiles(File folder, int tabIndex) {
		SwingUtilities.invokeLater(() -> {
			if (tabIndex >= tilePanels.size()) {
				System.err.println("Tab index out of bounds: " + tabIndex);
				return;
			}

			JPanel targetPanel = tilePanels.get(tabIndex);
			targetPanel.removeAll();

			while (allTiles.size() <= tabIndex) {
				allTiles.add(new HashMap<String, CustomImageSVGTile>());
			}
			allTiles.get(tabIndex).clear();

			if (tabIndex < svgFoldersImages.size()) {
				LinkedList<LinkedList<String>> currentTabData = svgFoldersImages.get(tabIndex);
				for (LinkedList<String> d2 : currentTabData) {
					CustomImageSVGTile tile = new CustomImageSVGTile(d2, folder);
					allTiles.get(tabIndex).put(tile.getFilename(), tile);
					JPanel leftRow = ListItemLeft.createThumbnailRowLeft(d2, frame, tabIndex, svgDataManager,
							listnerLeftListItem);
					targetPanel.add(leftRow);
				}
			}
			targetPanel.revalidate();
			targetPanel.repaint();
		});
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
			if (!(component instanceof JPanel))
				continue;

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

		initiateLeftTabbedPane();

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
				drawCaptureZoneInPaintMethodFree(g);
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
				if (e.isShiftDown())
					addPathPoint(e);

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				scenePanel.requestFocusInWindow();
				scenePanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2)); // oder andere Farbe/Stärke
				scenePanelIsSelected = true;
				setPathPointsToFront();
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
					zoomWholePanel(e);
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
					vk_ESCAPE_logic();
				}
				// Neue Tastenkombination zum Skalieren aller ausgewählten Tiles
				else if (e.getKeyCode() == KeyEvent.VK_PLUS && e.isControlDown()) {
					scaleSelectedTiles(1.1);
				} else if (e.getKeyCode() == KeyEvent.VK_MINUS && e.isControlDown()) {
					scaleSelectedTiles(0.9);
				} else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
//					System.out.println("Pressed Delete at MainWindow");

					deleteSelectedTile();
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
		scaleUpBtn.addActionListener(e -> ScreenshotPanel.showScreenshotDialog(currentFolder, frame, null));

		JButton takeScreenShot = initScreenshotButton(frame);

		JButton quickSVG = new JButton("QuickSVG");
		quickSVG.addActionListener(e -> {
		});

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

	private void initiateLeftTabbedPane() {
		leftTabbedPane = new JTabbedPane();
		svgFolders = svgDataManager.getSVGDataDirectories(currentFolder);
		startWatchService();
		// SVG-Daten für alle Ordner laden
		try {
			for (File f : svgFolders) {
				svgFoldersImages.add(svgDataManager.getSVGDataFromSVGInFolder(f));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

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

		leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(leftTabbedPane, BorderLayout.CENTER);
		leftPanel.setPreferredSize(new Dimension(300, 0));

	}

	// Class: ListPanel
	// Method: startWatchService()
	public void startWatchService() {
		Thread watchThread = new Thread(() -> {
			try {
				WatchService watchService = FileSystems.getDefault().newWatchService();
				for (File folder : svgFolders) {
					folder.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
							StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
				}

				while (true) {
					WatchKey key = watchService.take();
					if (key == null)
						continue;

					int selectedIndex = leftTabbedPane.getSelectedIndex();
					refreshTabContent(selectedIndex); // vereinfacht: immer neu laden
					loadFolderFiles(currentFolder, selectedIndex);
					key.pollEvents(); // leeren
					key.reset();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		watchThread.setDaemon(true);
		watchThread.start();
	}

	// Class: ListPanel
	// Method: refreshTabContent(int index)
	public void refreshTabContent(int index) {
		if (index < 0 || index >= svgFolders.length)
			return;

		File folder = svgFolders[index];
		LinkedList<LinkedList<String>> newData = svgDataManager.getSVGDataFromSVGInFolder(folder);
		svgFoldersImages.set(index, newData); // vorhandene Daten ersetzen

		JPanel tilePanel = tilePanels.get(index);
		tilePanel.removeAll(); // alte Komponenten entfernen

		for (LinkedList<String> data : newData) {

			String name = data.get(0);
			// Instead of a JLabel I need ot add a CustomImageTile
			JLabel label = new JLabel(name); // z. B. Darstellung als Label
			tilePanel.add(label);
		}

		tilePanel.revalidate();
		tilePanel.repaint();
	}

	private void vk_ESCAPE_logic() {
		selectAllTiles(false);
		clearPath();
		scenePanel.repaint();
	}

	private void setListeners() {

		listenerCustomTileUpdate = new ListenerCustomTileUpdate() {

			@Override
			public void onTileHover(CustomImageSVGTile tile, boolean isHovered) {
				if (allowDrag) {

				}
				System.out.println("hover " + tile.getFilename());
				highlightCorrespondingItemInRightPanel(tile, isHovered);
				setPathPointsToFront();
			}

			@Override
			public void drawCaptureZoneFromCustomTileSetStartPoint(Point point) {
				startPoint = point;
			}

			@Override
			public void drawCaptureZoneFromCustomTile(Point e) {
				if (!drawPath) {
					System.out.println("SvgTileViewApp.setListeners() I");
					drawCaptureZoneOnTileHover(e);
				} else {
					System.out.println("SvgTileViewApp.setListeners() II");
					drawCaptureZonePathOnTileHover(e);
				}
			}

			@Override
			public void addPathPointOnTileHoverFromCustomTile(Point e) {
				addPathPointOnTileHover(e);
				setPathPointsToFront();
			}

			@Override
			public void onVK_DELETE_typed(String id) {
				deleteSelectedTile();
//				for (CustomImageTile t : addedTiles) {
//					if (t.getID().equals(id)) {
//						scenePanel.remove(t.getPanel());
//						addedTiles.remove(t);
//					}
//				}

			}

			@Override
			public void removeSelectedSVGFromListener(JPanel row, CustomImageSVGTile tile) {
				removeSelectedSVG(row, tile);
			}

			@Override
			public void onVK_ESCAPE_typed(CustomImageSVGTile customImageSVGTile) {
				vk_ESCAPE_logic();
			}

			@Override
			public void zoom(MouseWheelEvent e) {
				zoomWholePanel(e);
			}

		};

		listnerLeftListItem = new ListenerLeftTiles() {

			@Override
			public void onClick(LinkedList<String> data) {
				System.out.println("listnerLeftListItem.onClick()");
				addNewTileToAddedTiles(data);
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

			@Override
			public void onClickRemove(LinkedList<String> data) {
				new File(data.get(CustomImageSVGTile.IDX_FILEPATH)).delete();
				System.out.println(data.get(CustomImageSVGTile.IDX_FILEPATH));
			}
		};

		listnerRightListItems = new ListenerRightTiles() {

			@Override
			public void setComponentZOrder(JPanel panel, int i) {
				scenePanel.setComponentZOrder(panel, 0);
			}

			@Override
			public void highlightCorrespondingTileInCanvas(File file, JPanel scenePanel, boolean b) {

			}

			@Override
			public void mouseEntered(File file, JPanel scenePanel, boolean b) {

				highlightCorrespondingTileInCanvas(file, scenePanel, true);
			}

			@Override
			public void mouseExited(File file, JPanel scenePanel, boolean b) {

				highlightCorrespondingTileInCanvas(file, scenePanel, false);
			}

			@Override
			public void checkBoxDeselectAction(JPanel row, CustomImageSVGTile tile) {
				removeSelectedSVG(row, tile);

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

	private void addNewTileToAddedTiles(LinkedList<String> data) {

		CustomImageSVGTile t = new CustomImageSVGTile(data);
		t.setUpdateListener(listenerCustomTileUpdate);
		addedTiles.add(t);
		CustomRightListViewItem item = new CustomRightListViewItem(addedTiles.get(addedTiles.size() - 1), scenePanel,
				svgDataManager, listnerRightListItems);

		rightRows.add(item.getRow());
		selectedPanel.add(rightRows.get(rightRows.size() - 1));
		selectedPanel.revalidate();
		selectedPanel.repaint();
		scenePanel = SVGTileViewerAppOutSource.setTileVisible(addedTiles.get(addedTiles.size() - 1), true, scenePanel,
				centerScrollPane);
		scenePanel.setComponentZOrder(addedTiles.get(addedTiles.size() - 1).getPanel(), 0);
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

	private JButton initScreenshotButton(JFrame parentFrame) {
		JButton takeScreenshot = new JButton();
		takeScreenshot.setText("📸"); // Fallback
		takeScreenshot.setToolTipText("Screenshot-Panel öffnen");
		takeScreenshot.addActionListener(e -> {
			takeScreenShot();
		});
		return takeScreenshot;
	}

	private void takeScreenShot() {

//		zoomScenePanel(10.0f);
//		zoomPath(10.0f);

		pathReset = path;
		pathPointsReset = pathPoints;
		captureZoneReset = captureZone;
		startPointReset = startPoint;
		// Removes the visible representation of the Rectangle
		clearPathForScreenShot(true);
		getAllColorsFromSVGs();
		try {
			Thread.sleep(500);

			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

			scheduler.schedule(() -> {

				File svgFolder = new File(currentFolder.getAbsolutePath(), "SVG_Cut");
				if (!svgFolder.exists()) {
					svgFolder.mkdir();
				}
				try {
				BufferedImage img = ScreenShotHandler.createImageFromCaptureZone(scenePanel, captureZoneReset);
				Thread.sleep(500);
				BufferedImage	img2 = capturePathAreaAsScreenshot(path, scenePanel, captureZoneReset, img);
				Thread.sleep(500);
//				File file_I = ScreenShotHandler.saveScreenshot(currentFolder, img);
				BufferedImage img3 = ScreenshotPanel.replaceColorsWithClosestMatch(img2, colorHistory);
				Thread.sleep(500);
//				file_I = ScreenShotHandler.saveScreenshot(currentFolder, img);
				BufferedImage img4 = ScreenshotPanel.separateColorBorders(img3, colorHistory);
				Thread.sleep(500);
				File file_I = ScreenShotHandler.saveScreenshot(currentFolder, img);
//				colorHistory.get(colorHistory.size() - 1).setSelected(true);
//				BufferedImage img5 = ScreenshotPanel.replaceSelectedBorderPixels(img4, colorHistory);
//				Thread.sleep(500);
////				 file_I = ScreenShotHandler.saveScreenshot(currentFolder, img5);
//				Thread.sleep(500);
				file_I = ConverterImageToSvgClassSuitable.convertImageToSVG(file_I, svgFolder);
//					file_I = ScreenShotHandler.saveScreenshot(currentFolder, img);
				scheduler.shutdown();

//				LinkedList<String> data = new LinkedList<String>();
//				data.add(file_I.getName());
//				data.add(file_I.getAbsolutePath());
//				data.add(new String().valueOf(120));
//				data.add(new String().valueOf(120));
//				data.add(new String().valueOf(120));
//				data.add(new String().valueOf(120));
//				listnerLeftListItem.onClick(data);

				captureZone = captureZoneReset;
				startPoint = startPointReset;
				path = pathReset;
				pathPoints = pathPointsReset;
				createPathFromPathPointsAndZoom((float) currentZoom);
				drawCaptureZone((int) minX, (int) minY, (int) imageCapturingWidth, (int) imageCapturingHeight);

				scenePanel.repaint();
				leftTabbedPane.updateUI();
//				zoomScenePanel(0.1f);
//				zoomPath(0.1f);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}, 300, TimeUnit.MILLISECONDS);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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

	private void deleteSelectedTile() {
		for (Component component : selectedPanel.getComponents()) {
			if (!(component instanceof JPanel))
				continue;

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

	public ArrayList<Color> getAllColorsFromSVGs() {
		allColorsList = new ArrayList<Color>();

		for (CustomImageSVGTile t : addedTiles) {
			DataClassColorHistory c = new DataClassColorHistory(Color.decode(t.getInitialColorToSet()), false);
			allColorsList.add(Color.decode(t.getInitialColorToSet()));
			colorHistory.add(c);
			System.out.println(t.getInitialColorToSet());
		}
		DataClassColorHistory c = new DataClassColorHistory(Color.WHITE, false);
		colorHistory.add(c);
		allColorsList.add(Color.WHITE);
		return allColorsList;
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

	protected void drawCaptureZoneInPaintMethodFree(Graphics g) {
		if (pathPoints.size() < 2)
			return; // Mindestens 2 Punkte benötigt

		Graphics2D g2d = (Graphics2D) g;

		// Anti-Aliasing für glattere Linien
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Pfad erstellen
		createPathFromPathPointsAndZoom(1.0f);

		// Pfad zeichnen
		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(2.0f));
		g2d.draw(path);

		// Optional: Geschlossenen Pfad zeichnen (wenn gewünscht)
		if (true && pathPoints.size() > 2) {
			g2d.setColor(new Color(0, 0, 255, 100)); // Transparentes Blau
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

//			scenePanel.setComponentZOrder(pathPoints.get(i), 0);
		}
	}

	private BufferedImage capturePathAreaAsScreenshot(GeneralPath path, JPanel scenePanel, Rectangle bounds,
			BufferedImage resultImage) {
		Area area = new Area(path);
		WritableRaster raster = resultImage.getRaster();
		for (int y = 0; y < imageCapturingHeight; y++) {
			for (int x = 0; x < imageCapturingWidth; x++) {
				double worldX = minX + x;
				double worldY = minY + y;
				if (!area.contains(worldX, worldY)) {
					raster.setPixel(x, y, new int[] { 255, 255, 255 }); // RGB Weiß
				}
			}
		}
		return resultImage;
	}

	private void addPathPointOnTileHover(Point e) {
		// TODO Auto-generated method stub
		CustomPathPanel pp = new CustomPathPanel(pathPanelListener);
		Panel pan = pp.getPanel();
//		scenePanel.setComponentZOrder(pan, 0);
		int x = (int) (e.getX());
		int y = (int) (e.getY());
		pan.setLocation(x, y);

		if (addPathPoint) {
			pathPoints.add(pan);
			System.out.println("add x: " + pan.getX() + " y: " + pan.getY());
			scenePanel.add(pathPoints.get(pathPoints.size() - 1));
			scenePanel.repaint();
		}

	}

	private void addPathPoint(MouseEvent e) {
		// TODO Auto-generated method stub
		CustomPathPanel pp = new CustomPathPanel(pathPanelListener);
		Panel pan = pp.getPanel();
//		scenePanel.setComponentZOrder(pan, 0);
		pan.setLocation(e.getX() - pp.DIM / 2, e.getY() - pp.DIM / 2);

		if (addPathPoint) {
			pathPoints.add(pan);
			System.out.println("add x: " + pan.getX() + " y: " + pan.getY());
			scenePanel.add(pathPoints.get(pathPoints.size() - 1));
			scenePanel.repaint();
		}
	}

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

	private void clearPathForScreenShot(Boolean repaint) {
		captureZone = null;
		startPoint = null;
		for (Panel p : pathPoints)
			scenePanel.remove(p);
		pathPoints.clear();

	}

	private void setPathPointsToFront() {
		for (Panel p : pathPoints) {
			try {
				scenePanel.setComponentZOrder(p, 0);
			} catch (Exception e) {
//				e.printStackTrace();
			}

		}
	}

	private void zoomWholePanel(MouseWheelEvent e) {
		// TODO Auto-generated method stub
		double zoomFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
		startPoint = e.getPoint();
		zoomScenePanel(zoomFactor);
		zoomPath(zoomFactor);
	}
}