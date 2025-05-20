package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class SVGDataManager2 {

	// Here I check the selected Folder for SVG files. I get all the data like "FilePath" Size etc. 
	public LinkedList<LinkedList<String>> getSVGData(File svgDirectory) {
		LinkedList<LinkedList<String>> svgData = new LinkedList<>();
		File dataDir = new File(svgDirectory, "svgData");
		File dataFile = new File(dataDir, "data.txt");

		if (!dataDir.exists()) {
			dataDir.mkdir();
			try (PrintWriter writer = new PrintWriter(dataFile)) {
				File[] svgFiles = svgDirectory.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".svg"));
				if (svgFiles != null) {
					for (int i = 0; i < svgFiles.length; i++) {
						File svg = svgFiles[i];
						// Platzhalterwerte
						String name = svg.getName();
						int index = i;
						int height = 100;
						int heightPercent = 100;
						int width = 100;
						int widthPercent = 100;
						int posX = 0;
						int posY = 0;

						String line = String.join(",", name, String.valueOf(index), String.valueOf(height),
								String.valueOf(heightPercent), String.valueOf(width), String.valueOf(widthPercent),
								String.valueOf(posX), String.valueOf(posY));
						writer.println(line);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Einlesen der Daten
		if (dataFile.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(",");
					LinkedList<String> entry = new LinkedList<>(List.of(parts));
					svgData.add(entry);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return svgData;
	}

	// Modifikation des SVGDataManager, um SVG-Daten für eine einzelne Datei
	// abzurufen
	// Fügen Sie diese Methode zu Ihrer SVGDataManager-Klasse hinzu

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
		// Fügen Sie weitere Standardwerte hinzu, die für ein CustomImageTile benötigt
		// werden
		// (abhängig von Ihrer CustomImageTile-Implementierung)

		return newData;
	}
}
