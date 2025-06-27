package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class ScreenshotPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	private static final ArrayList<DataClassColorHistory> dataClassColorHistory = new ArrayList<>();
	private static JPanel colorListPanel; // Jetzt als Klassenvariable
	private static JLabel imageLabel;
	private static JScrollPane imageScroll;
	private static BufferedImage image;
	private static ListenerTileUpdatScreenShotManager updateListener;

	private static double currentZoom = 1.0;
	private static ListenerImageListView listenerImageListView;
	private static JPanel mainContentPanel;
	private static final double ZOOM_FACTOR = 1.2;
	private static final double MIN_ZOOM = 0.1;
	private static final double MAX_ZOOM = 10.0;
	static ArrayList<File> imagesFiles = new ArrayList<File>();
	protected static File ImageFile;
	private static boolean isFloodFill;
	private static JFrame dialog;
	private static ImageListPanel imageListPanel;

	public static void showScreenshotDialog(File currentFolder, JFrame parentFrame, File ImageFile) {
		dialog = new JFrame("Screenshot Tools");
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dialog.setSize(1000, 700);
		dialog.setLayout(new BorderLayout());
//		ScreenshotPanel.ImageFile = ImageFile;
		listenerImageListView = new ListenerImageListView() {

			@Override
			public void onImageClicked(int index) {

			}

			@Override
			public void onImageClick(File file) {

				try {
					ScreenshotPanel.ImageFile = file;
					image = ImageIO.read(ScreenshotPanel.ImageFile);
					if (!imagesFiles.contains(ScreenshotPanel.ImageFile)) {
						imagesFiles.add(ScreenshotPanel.ImageFile);
					}
					imageLabel.setIcon(new ImageIcon(image));
					imageLabel.revalidate();
					imageLabel.repaint();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			@Override
			public void onImageDelete(File imageFile) {

				imageLabel.revalidate();
				imageScroll.revalidate();
				imageScroll.repaint();
				imageListPanel.refreshImageList();
			}
		};

		updateListener = new ListenerTileUpdatScreenShotManager() {

			@Override
			public void onCheckBoxClicked(boolean selected, int index) {

				dataClassColorHistory.get(index).setSelected(selected);
				System.out.println(dataClassColorHistory.get(index).getColor());
				System.out.println(dataClassColorHistory.get(index).isSelected());
			}

			@Override
			public void onCheckBoxClicked(boolean selected) {

			}
		};

//		try {
//			image = ImageIO.read(ScreenshotPanel.ImageFile);
		imageLabel = new JLabel();
		imageScroll = new JScrollPane(imageLabel);

		// Hauptpanel für Bild und untere Buttons
		mainContentPanel = new JPanel(new BorderLayout());
		mainContentPanel.add(imageScroll, BorderLayout.CENTER);

		// Neue Buttonleiste unter dem Bild
		JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		bottomButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Get the ImageListView

		JPanel leftPanel = new JPanel(new BorderLayout());
		File jarFile = null;
		try {
			jarFile = new File(ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		File programDir = jarFile.getParentFile();

		// Screenshot-Verzeichnis erstellen
		File imageDirectory = new File(programDir, "screenshots");
//		File imageDirectory = new File(currentFolder, "screenshots");
		imageListPanel = new ImageListPanel(imageDirectory, listenerImageListView);
		leftPanel.add(imageListPanel, BorderLayout.CENTER);
		leftPanel.setPreferredSize(new Dimension(300, 0));
		dialog.add(leftPanel, BorderLayout.WEST);

		// Dummy-Buttons hinzufügen
		String[] dummyButtonLabels = { "⮜", "Clean Color", "Split Color", "Split All Colors", "Thicken", "To SVG",
				"SAVE", "Refresh" };
		for (String label : dummyButtonLabels) {
			JButton btn = new JButton(label);
			btn.addActionListener(e -> {
				if (btn.getLabel() == "⮜" || btn.getLabel() == "⮞") {

					leftPanel.setVisible(!leftPanel.isVisible());
					btn.setText(leftPanel.isVisible() ? "⮜" : "⮞");
				} else if (btn.getLabel() == "Clean Color") {
					BufferedImage original = null;
					try {
						original = ImageIO.read(ScreenshotPanel.ImageFile);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					image = ScreenshotPanel.replaceColorsWithClosestMatch(original,dataClassColorHistory);
//						image = separateColorBorders(image);
					if (image != null) {
						imageLabel.setIcon(new ImageIcon(image));
						imageLabel.revalidate();
						imageLabel.repaint();
					}
				} else if (btn.getLabel() == "Split Color") {

					image = separateColorBorders(image, dataClassColorHistory);
					if (image != null) {
						imageLabel.setIcon(new ImageIcon(image));
						imageLabel.revalidate();
						imageLabel.repaint();
					}
				} else if (btn.getLabel() == "Split All Colors") {

					image = separateColorBorders(image);
					if (image != null) {
						imageLabel.setIcon(new ImageIcon(image));
						imageLabel.revalidate();
						imageLabel.repaint();
					}
				} else if (btn.getLabel() == "Thicken") {

					image = replaceSelectedBorderPixels(image, dataClassColorHistory);
					if (image != null) {
						imageLabel.setIcon(new ImageIcon(image));
						imageLabel.revalidate();
						imageLabel.repaint();
					}
				} else if (btn.getLabel() == "To SVG") {

					ConverterImageToSvgClassSuitable.convertImageToSVG(ScreenshotPanel.ImageFile);
				} else if (btn.getLabel() == "SAVE") {
//saveImage(image, screenShotFile, false);
					SaveImageFileDialog.showSaveImageDialog(bottomButtonPanel, image, label);
				} else if (btn.getLabel() == "Refresh") {
//						floodFill(image, ERROR, ALLBITS, ABORT);
					imageListPanel.refreshImageList();
				}

			});
			bottomButtonPanel.add(btn);
		}

		mainContentPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

		// Rechte Panel für Farben
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setPreferredSize(new Dimension(200, 0));
		rightPanel.setBorder(new TitledBorder("Farbpalette"));

		colorListPanel = new JPanel();
		colorListPanel.setLayout(new BoxLayout(colorListPanel, BoxLayout.Y_AXIS));
		JScrollPane colorScroll = new JScrollPane(colorListPanel);

		// Pipetten-Funktion
		imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		imageScroll.addMouseWheelListener(e -> {
			if (e.isControlDown()) {
				// Mausposition relativ zum Bildlabel bestimmen
				Point mousePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), imageLabel);

				// Zoom-Faktor basierend auf der Raddrehung berechnen
				double zoomFactor = e.getWheelRotation() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;

				// Zoom anwenden - dies wird in einem Thread erfolgen
				applyZoom(zoomFactor, mousePoint);

				e.consume();
			}
		});

		imageLabel.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {

				try {
					System.out.println(new Color(image.getRGB(e.getX(), e.getY())));
				} catch (Exception e2) {

				}

			}

			@Override
			public void mouseDragged(MouseEvent e) {

			}
		});

		imageLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (!isFloodFill) {
						Color pickedColor = new Color(image.getRGB(e.getX(), e.getY()));
						boolean add = true;
						if (!dataClassColorHistory.isEmpty())
							for (DataClassColorHistory c : dataClassColorHistory) {
								if (c.color.equals(pickedColor)) {
									add = false;
								}
							}
						if (add) {
							dataClassColorHistory.add(new DataClassColorHistory(pickedColor, false));
							updateColorList();
						}
					} else {

						floodFill(image, e.getX(), e.getY(), dataClassColorHistory.get(0).getColorAsInt());
					}

				}
			}
		});

		// Button-Panel für Farbpalette
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton pipetteBtn = new JButton("Pipette");
		JButton clearBtn = new JButton("Clear");

		pipetteBtn.addActionListener(e -> {

			isFloodFill = !isFloodFill;
//				if(!isFloodFill) {
			imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

//				} else {
//					imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
//				}
		});

		buttonPanel.add(pipetteBtn);
		buttonPanel.add(clearBtn);

		// Layout zusammensetzen
		rightPanel.add(colorScroll, BorderLayout.CENTER);
		rightPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Hauptkomponenten hinzufügen (nur einmal!)
		dialog.add(mainContentPanel, BorderLayout.CENTER);
		dialog.add(rightPanel, BorderLayout.EAST);

//		} catch (IOException e) {
//			dialog.add(new JLabel("Fehler beim Laden: " + e.getMessage()));
//		}

		dialog.setLocationRelativeTo(parentFrame);

//		###

		JPanel collapsiblePanel = new JPanel(); // z.B. Optionen oder Werkzeuge
		collapsiblePanel.setPreferredSize(new Dimension(200, 0));
		collapsiblePanel.setBorder(new TitledBorder("Tools"));
		dialog.setVisible(true);
	}

	private static void resetZoom() {
		// Anzeigen, dass wir gerade arbeiten
		imageScroll.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// Thread für Reset-Operation erstellen
		Thread resetThread = new Thread(() -> {
			try {
				// UI-Aktualisierungen müssen im EDT erfolgen
				SwingUtilities.invokeLater(() -> {
					try {
						currentZoom = 1.0;
						imageLabel.setIcon(new ImageIcon(image));
						imageLabel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
						imageScroll.getViewport().setViewPosition(new Point(0, 0));

						// UI aktualisieren
						imageLabel.revalidate();
						imageScroll.revalidate();
						imageScroll.repaint();

						// Zoom-Label aktualisieren
						updateZoomLabel();

						// Cursor zurücksetzen
						imageScroll.setCursor(Cursor.getDefaultCursor());
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		// Thread starten
		resetThread.start();
	}

	// Verbesserte zoomIn und zoomOut Methoden
	public static void zoomIn(Point center) {
		Point zoomPoint = center != null ? center
				: new Point(imageScroll.getViewport().getWidth() / 2, imageScroll.getViewport().getHeight() / 2);
		applyZoom(ZOOM_FACTOR, zoomPoint);
	}

	public static void zoomOut(Point center) {
		Point zoomPoint = center != null ? center
				: new Point(imageScroll.getViewport().getWidth() / 2, imageScroll.getViewport().getHeight() / 2);
		applyZoom(1.0 / ZOOM_FACTOR, zoomPoint);
	}

	// Fügen Sie diese Hilfsmethode zur ScreenshotPanel-Klasse hinzu
	private static void performZoomOperation(double newZoom, Point zoomCenter) {
		// Thread für Bildoperationen erstellen
		Thread zoomThread = new Thread(() -> {
			try {
				// Originalbild mit neuem Zoom skalieren
				int newWidth = (int) (image.getWidth() * newZoom);
				int newHeight = (int) (image.getHeight() * newZoom);

				// Verwenden Sie SCALE_FAST für bessere Performance
				Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_FAST);

				// Hier könnten zusätzliche Berechnungen stattfinden

				// UI-Aktualisierungen müssen im EDT erfolgen
				SwingUtilities.invokeLater(() -> {
					try {
						// Viewport berechnen vor der Änderung
						JViewport viewport = imageScroll.getViewport();
						Point viewPos = viewport.getViewPosition();
						Dimension viewSize = viewport.getExtentSize();

						// Berechnung des relativen Punktes, der zentriert bleiben soll
						double relativeX = zoomCenter != null ? zoomCenter.x / (double) (imageLabel.getWidth()) : 0.5;
						double relativeY = zoomCenter != null ? zoomCenter.y / (double) (imageLabel.getHeight()) : 0.5;

						// Icon und Größe aktualisieren
						imageLabel.setIcon(new ImageIcon(scaledImage));
						imageLabel.setPreferredSize(new Dimension(newWidth, newHeight));

						// Neue Scrollposition berechnen, damit das Zoomen auf den Mauszeiger zentriert
						// ist
						int newX = (int) (newWidth * relativeX - viewSize.width * relativeX);
						int newY = (int) (newHeight * relativeY - viewSize.height * relativeY);

						// Sicherstellen, dass wir innerhalb der Grenzen bleiben
						newX = Math.max(0, Math.min(newX, newWidth - viewSize.width));
						newY = Math.max(0, Math.min(newY, newHeight - viewSize.height));

						// Scrollposition setzen
						viewport.setViewPosition(new Point(newX, newY));

						// Aktuellen Zoom-Wert aktualisieren
						currentZoom = newZoom;

						// UI aktualisieren
						imageLabel.revalidate();
						imageScroll.revalidate();
						imageScroll.repaint();

						// Optionalen Zoom-Label aktualisieren, falls vorhanden
						updateZoomLabel();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		// Thread starten
		zoomThread.start();
	}

	// Hilfsmethode zum Aktualisieren des Zoom-Labels
	private static void updateZoomLabel() {
		for (Component comp : ((JPanel) imageScroll.getParent()).getComponents()) {
			if (comp instanceof JPanel) {
				for (Component buttonPanelComp : ((JPanel) comp).getComponents()) {
					if (buttonPanelComp instanceof JPanel
							&& ((JPanel) buttonPanelComp).getLayout() instanceof FlowLayout) {
						for (Component zoomPanelComp : ((JPanel) buttonPanelComp).getComponents()) {
							if (zoomPanelComp instanceof JLabel && ((JLabel) zoomPanelComp).getText().endsWith("%")) {
								((JLabel) zoomPanelComp).setText(String.format("%.0f%%", currentZoom * 100));
								break;
							}
						}
					}
				}
			}
		}
	}

	// Ersetzen Sie die alte applyZoom-Methode durch diese neue Version
	private static void applyZoom(double zoomFactor, Point zoomCenter) {
		// Neues Zoom-Level berechnen
		double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentZoom * zoomFactor));

		// Wenn keine Änderung im Zoom, nichts tun
		if (newZoom == currentZoom)
			return;

		// Anzeigen, dass wir gerade arbeiten
		imageScroll.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// Zoom-Operation im Thread durchführen
		performZoomOperation(newZoom, zoomCenter);

		// Cursor zurücksetzen wird im Thread nach Abschluss gemacht
		SwingUtilities.invokeLater(() -> {
			imageScroll.setCursor(Cursor.getDefaultCursor());
		});
	}

	private static void updateColorList() {
		colorListPanel.removeAll();
		int i = 0;
		for (DataClassColorHistory dataColorHistory : dataClassColorHistory) {
			ColorItemPanel item = new ColorItemPanel(dataColorHistory.color, e -> {
				dataClassColorHistory.remove(dataColorHistory);
				updateColorList();
			}, i);
			item.setListener(updateListener);
			colorListPanel.add(item);
			colorListPanel.add(Box.createVerticalStrut(2));
			i++;
		}

		colorListPanel.revalidate();
		colorListPanel.repaint();
	}

	public static void saveCurrentImage(JFrame parentFrame, BufferedImage imageToSave, File originalFile) {
		// Vorgeschlagenen Dateinamen erzeugen (original Dateiname + "_edited")
		String suggestedFileName = null;
		if (originalFile != null) {
			String originalName = originalFile.getName();
			int dotIndex = originalName.lastIndexOf('.');
			if (dotIndex > 0) {
				// Fügt "_edited" vor der Dateierweiterung ein
				suggestedFileName = originalName.substring(0, dotIndex) + "_edited" + originalName.substring(dotIndex);
			} else {
				suggestedFileName = originalName + "_edited";
			}

			// Vollständigen Pfad erstellen
			suggestedFileName = new File(originalFile.getParentFile(), suggestedFileName).getAbsolutePath();
		}

		// Speichern-Dialog anzeigen
		File savedFile = SaveImageFileDialog.showSaveImageDialog(parentFrame, imageToSave, suggestedFileName);

		// Erfolgsmeldung anzeigen, wenn gespeichert wurde
		if (savedFile != null) {
			JOptionPane.showMessageDialog(parentFrame,
					"Bild wurde erfolgreich unter\n" + savedFile.getAbsolutePath() + "\ngespeichert.",
					"Speichern erfolgreich", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public static BufferedImage saveImage(BufferedImage imageToSave, File targetFile, boolean createBackup) {
		try {
			// Dateierweiterung bestimmen
			String fileName = targetFile.getName().toLowerCase();
			String format = "png"; // Standard-Bildformat

			if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
				format = "jpg";
			} else if (fileName.endsWith(".gif")) {
				format = "gif";
			} else if (fileName.endsWith(".bmp")) {
				format = "bmp";
			}

			// Backup erstellen, falls gewünscht und die Datei bereits existiert
			if (createBackup && targetFile.exists()) {
				File backupFile = new File(targetFile.getParent(), targetFile.getName() + ".bak");

				// Falls eine bestehende Backup-Datei existiert, diese löschen
				if (backupFile.exists()) {
					backupFile.delete();
				}

				// Original-Datei in Backup kopieren
				BufferedImage original = ImageIO.read(targetFile);
				ImageIO.write(original, format, backupFile);
				System.out.println("Backup erstellt: " + backupFile.getAbsolutePath());
			}

			// Bild im gewünschten Format speichern
			ImageIO.write(imageToSave, format, targetFile);
			System.out.println("Bild gespeichert unter: " + targetFile.getAbsolutePath());

			return imageToSave;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Überladene Methode, die die Originalimplementierung ersetzt und mit der neuen
	 * Methode arbeitet. Beibehaltung für Abwärtskompatibilität.
	 */
	public static BufferedImage saveImage(File imageFile) {
		try {
			BufferedImage original = ImageIO.read(imageFile);
			return saveImage(original, imageFile, true); // Mit Backup speichern
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static BufferedImage replaceColorsWithClosestMatch(File imageFile) {
		try {
			File backupFile = new File(imageFile.getParent(), imageFile.getName() + ".bak");
			BufferedImage original = ImageIO.read(imageFile);
//			ImageIO.write(original, "png", backupFile);

			int width = original.getWidth();
			int height = original.getHeight();

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					Color pixel = new Color(original.getRGB(x, y));
					Color closest = findClosestColor(pixel);
					original.setRGB(x, y, closest.getRGB());
				}
			}

//			ImageIO.write(original, "png", imageFile);
			return original;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static BufferedImage replaceColorsWithClosestMatch(BufferedImage original) {
		int width = original.getWidth();
		int height = original.getHeight();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color pixel = new Color(original.getRGB(x, y));
				Color closest = findClosestColor(pixel);
				original.setRGB(x, y, closest.getRGB());
			}
		}
		return original;
	}

	public static BufferedImage replaceColorsWithClosestMatch(BufferedImage original,
			ArrayList<DataClassColorHistory> colorHistory) {
		int width = original.getWidth();
		int height = original.getHeight();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color pixel = new Color(original.getRGB(x, y));
				Color closest = findClosestColor(pixel, colorHistory);
				original.setRGB(x, y, closest.getRGB());
			}
		}
		return original;
	}

	private static Color findClosestColor(Color target, ArrayList<DataClassColorHistory> colorHistory) {
		if (dataClassColorHistory.isEmpty())
			return target;

		Color closest = dataClassColorHistory.get(0).color;
		double minDist = colorDistance(target, closest);

		for (DataClassColorHistory c : dataClassColorHistory) {
			double dist = colorDistance(target, c.color);
			if (dist < minDist) {
				minDist = dist;
				closest = c.color;
			}
		}

		return closest;
	}

	private static Color findClosestColor(Color target) {
		if (dataClassColorHistory.isEmpty())
			return target;

		Color closest = dataClassColorHistory.get(0).color;
		double minDist = colorDistance(target, closest);

		for (DataClassColorHistory c : dataClassColorHistory) {
			double dist = colorDistance(target, c.color);
			if (dist < minDist) {
				minDist = dist;
				closest = c.color;
			}
		}

		return closest;
	}

	private static double colorDistance(Color c1, Color c2) {
		int dr = c1.getRed() - c2.getRed();
		int dg = c1.getGreen() - c2.getGreen();
		int db = c1.getBlue() - c2.getBlue();
		return Math.sqrt(dr * dr + dg * dg + db * db);
	}

	public static BufferedImage separateColorBorders(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		BufferedImage result = new BufferedImage(width, height, image.getType());

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color current = new Color(image.getRGB(x, y));
				if (!isInColorHistory(current)) {
					result.setRGB(x, y, current.getRGB());
					continue;
				}

				boolean borderFound = false;

				// Nachbarn prüfen
				for (int dy = -1; dy <= 1 && !borderFound; dy++) {
					for (int dx = -1; dx <= 1 && !borderFound; dx++) {
						if (dx == 0 && dy == 0)
							continue;
						int nx = x + dx;
						int ny = y + dy;

						if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
							Color neighbor = new Color(image.getRGB(nx, ny));
							if (!isInColorHistory(neighbor))
								continue;
							if (!isSameColor(current, neighbor)) {
								// Grenze erkannt → aktuelles Pixel weiß
								result.setRGB(x, y, Color.WHITE.getRGB());
								borderFound = true;
							}
						}
					}
				}

				if (!borderFound) {
					result.setRGB(x, y, current.getRGB());
				}
			}
		}

		return result;
	}

	public static void floodFill(BufferedImage image, int startX, int startY, int newColor) {
		int width = image.getWidth();
		int height = image.getHeight();
		int targetColor = image.getRGB(startX, startY);
		if (targetColor == newColor)
			return;

		boolean[][] visited = new boolean[width][height];
		Queue<int[]> queue = new LinkedList<>();
		queue.add(new int[] { startX, startY });
		visited[startX][startY] = true;

		while (!queue.isEmpty()) {
			int[] p = queue.poll();
			int x = p[0], y = p[1];
			image.setRGB(x, y, newColor);

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (Math.abs(dx) + Math.abs(dy) != 1)
						continue; // 4er Nachbarn
					int nx = x + dx, ny = y + dy;
					if (nx >= 0 && ny >= 0 && nx < width && ny < height && !visited[nx][ny]
							&& image.getRGB(nx, ny) == targetColor) {
						queue.add(new int[] { nx, ny });
						visited[nx][ny] = true;
					}
				}
			}
		}
	}

	public static BufferedImage separateColorBorders(BufferedImage image,
			ArrayList<DataClassColorHistory> dataClassColorHistory) {
		int width = image.getWidth();
		int height = image.getHeight();

		BufferedImage result = new BufferedImage(width, height, image.getType());

		// Get only selected colors
		ArrayList<Color> selectedColors = new ArrayList<>();
		for (DataClassColorHistory colorInfo : dataClassColorHistory) {
			if (!colorInfo.isSelected()) { // Use getter method instead of direct field access
				selectedColors.add(colorInfo.getColor());
			}
		}

		// If no colors are selected, return original image
		if (selectedColors.isEmpty()) {
			return image;
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color current = new Color(image.getRGB(x, y));

				// Only process if color is in selected colors
				if (!isInColorList(current, selectedColors)) {
					result.setRGB(x, y, current.getRGB());
					continue;
				}

				boolean borderFound = false;

				// Check 8-connected neighbors
				for (int dy = -1; dy <= 1 && !borderFound; dy++) {
					for (int dx = -1; dx <= 1 && !borderFound; dx++) {
						if (dx == 0 && dy == 0)
							continue; // Skip center pixel

						int nx = x + dx;
						int ny = y + dy;

						if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
							Color neighbor = new Color(image.getRGB(nx, ny));

							// Only consider neighbors that are also selected colors
							if (isInColorList(neighbor, selectedColors)) {
								if (!isSameColor(current, neighbor)) {
									// Border detected - make this pixel white
									result.setRGB(x, y, Color.WHITE.getRGB());
									borderFound = true;
								}
							}
						}
					}
				}

				if (!borderFound) {
					result.setRGB(x, y, current.getRGB());
				}
			}
		}

		return result;
	}

	private static boolean isInColorList(Color c, ArrayList<Color> colorList) {
		for (Color ref : colorList) {
			if (isSameColor(c, ref))
				return true;
		}
		return false;
	}

	private static boolean isInColorHistory(Color c) {
		for (DataClassColorHistory ref : dataClassColorHistory) {
			if (isSameColor(c, ref.color))
				return true;
		}
		return false;
	}
	


	private static boolean isSameColor(Color c1, Color c2) {
		return c1.getRGB() == c2.getRGB();
	}

	public static BufferedImage replaceSelectedBorderPixels(BufferedImage image,
			ArrayList<DataClassColorHistory> dataClassColorHistory) {
		int width = image.getWidth();
		int height = image.getHeight();
		BufferedImage result = new BufferedImage(width, height, image.getType());

		ArrayList<Color> selected = new ArrayList<>();
		ArrayList<Color> unselected = new ArrayList<>();
		for (DataClassColorHistory c : dataClassColorHistory) {
			if (c.isSelected())
				selected.add(c.getColor());
			else
				unselected.add(c.getColor());
		}

		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				Color current = new Color(image.getRGB(x, y));
				if (!selected.contains(current)) {
					result.setRGB(x, y, current.getRGB());
					continue;
				}

				boolean replaced = false;
				for (int dy = -1; dy <= 1 && !replaced; dy++) {
					for (int dx = -1; dx <= 1 && !replaced; dx++) {
						if (dx == 0 && dy == 0)
							continue;
						int nx = x + dx, ny = y + dy;
						Color neighbor = new Color(image.getRGB(nx, ny));
						if (unselected.contains(neighbor)) {
							result.setRGB(x, y, neighbor.getRGB());
							replaced = true;
						}
					}
				}

				if (!replaced)
					result.setRGB(x, y, current.getRGB());
			}
		}

		return result;
	}

	
}

class ColorItemPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ListenerTileUpdatScreenShotManager listener;
	private int index;

	public void setListener(ListenerTileUpdatScreenShotManager listener) {
		this.listener = listener;
	}

	public ColorItemPanel(Color color, ActionListener onDelete, int index) {
		this.index = index;
		setLayout(new BorderLayout(0, 2));
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setPreferredSize(new Dimension(180, 50));
		setMaximumSize(new Dimension(180, 50));
		// Oben: Farbfeld (zentriert)
		JPanel colorSwatch = new JPanel();
		colorSwatch.setPreferredSize(new Dimension(30, 30));
		colorSwatch.setBackground(color);
		colorSwatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		JPanel swatchPanel = new JPanel();
		swatchPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		swatchPanel.add(colorSwatch);

		// Unten: Checkbox + Delete
		JCheckBox checkBox = new JCheckBox();
		checkBox.setToolTipText(String.format("RGB(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue()));
		checkBox.addActionListener(e -> {
//			if (!checkBox.isSelected()) {
//				
//			} else {
//
//			}
			listener.onCheckBoxClicked(checkBox.isSelected(), index);
		});

		JButton deleteButton = new JButton("✖");
		deleteButton.setFont(new Font("Dialog", Font.PLAIN, 10));
		deleteButton.setMargin(new Insets(2, 6, 2, 6));
		deleteButton.addActionListener(onDelete);

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		controlPanel.add(checkBox);
		controlPanel.add(deleteButton);

		add(swatchPanel, BorderLayout.NORTH);
		add(controlPanel, BorderLayout.SOUTH);
	}
}
