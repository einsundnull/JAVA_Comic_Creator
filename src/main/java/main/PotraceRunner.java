package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class PotraceRunner {
	public static void convertToSvg(File bmpInput, File svgOutput) throws IOException {
	    if (!bmpInput.getName().toLowerCase().endsWith(".bmp")) {
	        throw new IllegalArgumentException("Nur .bmp-Dateien erlaubt: " + bmpInput.getName());
	    }

	    ProcessBuilder pb = new ProcessBuilder(
	            "C:\\Program Files\\potrace\\potrace.exe",
	            bmpInput.getAbsolutePath(),
	            "-s",
	            "-o",
	            svgOutput.getAbsolutePath()
	    );

	    pb.redirectErrorStream(true);
	    Process process = pb.start();

	    try (BufferedReader reader = new BufferedReader(
	            new InputStreamReader(process.getInputStream()))) {
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

    
    
}
