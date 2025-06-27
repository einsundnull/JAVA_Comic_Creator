package main;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SVGDataManager {

	public static final int FILEPATH = 1;
	Map<String, WeakReference<BufferedImage>> imageCache = new HashMap<>();

	public File[] getSVGDataDirectories(File svgDirectory) {
		File[] folders = new File[0];
		ArrayList<File> files = new ArrayList<>();
		if (svgDirectory != null && svgDirectory.isDirectory()) {
			// Nur direkte Unterordner werden durchsucht
			folders = svgDirectory.listFiles(File::isDirectory);

		}
		return folders;
	}

	public LinkedList<LinkedList<String>> getSVGDataFromSVGInFolder(File svgDirectory) {
		LinkedList<LinkedList<String>> svgData = new LinkedList<>();
		File dataDir = new File(svgDirectory, "svgData");
		File dataFile = new File(dataDir, "data.txt");

		File[] svgFiles = svgDirectory.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".svg"));
		if (svgFiles != null) {
			for (int i = 0; i < svgFiles.length; i++) {
				File svg = svgFiles[i];
				// Platzhalterwerte
				String name = svg.getName();
				String path = svg.getPath();
				int index = i;
				int height = 100;
				int heightPercent = 100;
				int width = 100;
				int widthPercent = 100;
				int posX = 0;
				int posY = 0;

				String line = String.join(",", name, path, String.valueOf(index), String.valueOf(height),
						String.valueOf(heightPercent), String.valueOf(width), String.valueOf(widthPercent),
						String.valueOf(posX), String.valueOf(posY));

				String[] parts = line.split(",");
				LinkedList<String> entry = new LinkedList<>(List.of(parts));
				svgData.add(entry);
			}

		}
		return svgData;
	}

	public LinkedList<String> getSVGDataForFile(File file, LinkedList<LinkedList<String>> svgData) {
		// Suche die Daten für eine bestimmte Datei
		for (LinkedList<String> data : svgData) {
			// Annahme: Das erste Element in jedem Datensatz ist der Dateiname
			if (data.getFirst().equals(file.getName())) {
				// Eine Kopie der Daten erstellen, damit wir sie unabhängig bearbeiten können
				LinkedList<String> copy = new LinkedList<>(data);
				return copy;
			}
		}

		// Falls keine Daten gefunden wurden, erstelle neue Daten für diese Datei
		LinkedList<String> newData = new LinkedList<>();
		newData.add(file.getName()); // Dateiname als erstes Element
		return newData;
	}

	BufferedImage getSvgThumbnail(File svgFile) {
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
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 120f);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 120f);

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

	public BufferedImage getThumbnail(File file) {
		// TODO Auto-generated method stub
		return null;
	}

}
