package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ScreenShotHandler extends JPanel {
	private Rectangle captureZone;
	private Point startPoint;

	public static File takeScreenshot(File currentFolder, Rectangle captureZone, JPanel scenePanel, JFrame frame) {
		File screenshotFile = null;
		try {

			BufferedImage screenshot = null;
			if (captureZone == null) {
				screenshot = createImageFromPanel(scenePanel);
			} else {
				screenshot = createImageFromCaptureZone(scenePanel, captureZone);
			}

			// Dateispeicherort bestimmen
			File jarFile = new File(
					ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			File programDir = jarFile.getParentFile();

			// Screenshot-Verzeichnis erstellen
			File screenshotsDir = new File(programDir, "screenshots");
//			File screenshotsDir = new File(currentFolder, "screenshots");
			if (!screenshotsDir.exists()) {
				screenshotsDir.mkdir();
			}

			String fileName = "A_ScreenShot" + System.currentTimeMillis() + ".png";

			screenshotFile = new File(screenshotsDir, fileName);
			ImageIO.write(screenshot, "png", screenshotFile);

			JOptionPane.showMessageDialog(frame, "Screenshot saved as " + screenshotFile.getName());
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Error taking screenshot: " + e.getMessage());
		}
		return screenshotFile;
	}

//	public static BufferedImage takeScreenshotII(Rectangle captureZone, JPanel scenePanel, JFrame frame) {
////		File screenshotFile = null;
//		BufferedImage screenshot = null;
//		try {
////			if (captureZone == null) {
////				screenshot = createImageFromPanel(scenePanel);
////			} else {
//			screenshot = createImageFromPanelRegion(scenePanel, captureZone);
////			}
//
//			// Dateispeicherort bestimmen
////			
////
////			JOptionPane.showMessageDialog(frame, "Screenshot saved as " + screenshotFile.getName());
//		} catch (Exception e) {
//			e.printStackTrace();
//			JOptionPane.showMessageDialog(frame, "Error taking screenshot: " + e.getMessage());
//		}
//		return screenshot;
//	}

	public static File saveScreenshot(File currentFolder, BufferedImage screenShot) {
		File screenshotFile = null;
		try {

			// Dateispeicherort bestimmen
			File jarFile = new File(
					ScreenShotHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			File programDir = jarFile.getParentFile();

			// Screenshot-Verzeichnis erstellen
//			File screenshotsDir = new File(programDir, "screenshots");
			File screenshotsDir = new File(currentFolder, "screenshots");
			if (!screenshotsDir.exists()) {
				screenshotsDir.mkdir();
			}

			String fileName = "A_ScreenShot" + System.currentTimeMillis() + ".png";

			screenshotFile = new File(screenshotsDir, fileName);
			ImageIO.write(screenShot, "png", screenshotFile);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return screenshotFile;
	}

//	public class PanelToImage {
	public static BufferedImage createImageFromPanel(JPanel panel) {
		int width = panel.getWidth();
		int height = panel.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		panel.paint(g2d); // Zeichne das Panel in das Bild
		g2d.dispose();
		return image;
	}

	public static BufferedImage createImageFromCaptureZone(JPanel panel, Rectangle captureZone) {
		// Sicherstellen, dass das Panel gerendert ist (wichtig für korrekte Größe)
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();

		// Begrenze die Aufnahmezone auf die tatsächlichen Panel-Dimensionen
		Rectangle panelBounds = new Rectangle(0, 0, panel.getWidth(), panel.getHeight());
		Rectangle clippedZone = captureZone.intersection(panelBounds);

		// Falls die Zone außerhalb des Panels liegt: leeres Bild zurückgeben
		if (clippedZone.isEmpty()) {
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB); // oder null
		}

		// Bild nur für den sichtbaren Teil erstellen
		BufferedImage image = new BufferedImage(clippedZone.width, clippedZone.height, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = image.createGraphics();
		g2d.translate(-clippedZone.x, -clippedZone.y); // Verschiebung für den Ausschnitt
		panel.paint(g2d); // Rendert nur den relevanten Teil
		g2d.dispose();

		return image;
	}

	public static int getRGBAt(JPanel panel, int x, int y) {
		// Erstelle ein BufferedImage mit der Größe des JPanels
		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);

		// Zeichne den Inhalt des JPanels in das BufferedImage
		panel.paint(image.getGraphics());

		// Hole den RGB-Wert an der Position (x, y)
		return image.getRGB(x, y);
	}

	// Optional: Methode, die den RGB-Wert in separate R-, G-, B-Komponenten
	// aufteilt
	public static Color getColorAt(JPanel panel, int x, int y) {
		int rgb = getRGBAt(panel, x, y);
		return new Color(rgb);
	}

}
