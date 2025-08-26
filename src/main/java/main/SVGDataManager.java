package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class SVGDataManager {

	
	
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

                        String line = String.join(",",
                            name,
                            String.valueOf(index),
                            String.valueOf(height),
                            String.valueOf(heightPercent),
                            String.valueOf(width),
                            String.valueOf(widthPercent),
                            String.valueOf(posX),
                            String.valueOf(posY)
                        );
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
}
