package main;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;

public class ConverterHelperPotraceRunner {
	public static void convertToSvg(File bmpInput, File svgOutput) throws IOException {
		if (!bmpInput.getName().toLowerCase().endsWith(".bmp")) {
			throw new IllegalArgumentException("Nur .bmp-Dateien erlaubt: " + bmpInput.getName());
		}

		ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\potrace\\potrace.exe", bmpInput.getAbsolutePath(),
				"-s", "-o", svgOutput.getAbsolutePath());

		pb.redirectErrorStream(true);
		Process process = pb.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line); // Debug-Ausgabe
			}
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Lesen von Potrace-Ausgabe", e);
		}

		try {
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new RuntimeException("Potrace fehlgeschlagen (exit " + exitCode + ")");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static BufferedImage convertImageToBMP(BufferedImage image, String outputPath, String baseName) {
		try {
			// Temporäre BMP-Datei erstellen
			String tmpName = baseName + ".tmp.bmp";
			File bmpFile = new File(outputPath, tmpName);

			// BufferedImage als BMP speichern
			ImageIO.write(image, "bmp", bmpFile);

			// SVG-Datei für die Konvertierung vorbereiten
			File svgFile = new File(outputPath, baseName + ".svg");

			// BMP zu SVG konvertieren
			BufferedImage result = ConverterHelperPotraceRunner.convertToSvg(image, svgFile);

			// Temporäre BMP-Datei löschen
			bmpFile.delete();

			return result;
		} catch (IOException e) {
			System.err.println("Fehler bei der Konvertierung von: " + baseName);
			e.printStackTrace();
		}
		return image;
	}

	public static BufferedImage convertToSvg(BufferedImage bmpInput, File svgOutput) throws IOException {
		// Temporäre BMP-Datei für Potrace erstellen
		File tempBmpFile = File.createTempFile("potrace_input_", ".bmp");

		try {
			// BufferedImage als BMP-Datei speichern
			ImageIO.write(bmpInput, "bmp", tempBmpFile);

			ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\potrace\\potrace.exe",
					tempBmpFile.getAbsolutePath(), "-s", "-o", svgOutput.getAbsolutePath());

			pb.redirectErrorStream(true);
			Process process = pb.start();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line); // Debug-Ausgabe
				}
			} catch (IOException e) {
				throw new RuntimeException("Fehler beim Lesen von Potrace-Ausgabe", e);
			}

			try {
				int exitCode = process.waitFor();
				if (exitCode != 0) {
					throw new RuntimeException("Potrace fehlgeschlagen (exit " + exitCode + ")");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} finally {
			// Temporäre Datei aufräumen
			if (tempBmpFile.exists()) {
				tempBmpFile.delete();
			}
		}
		return bmpInput;
	}

}
