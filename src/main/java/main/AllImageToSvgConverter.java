// Klasse: AllImageToSvgConverter.java
package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class AllImageToSvgConverter {

    public static void convertAllImagesInFolder(File folder) {
        File[] images = folder.listFiles(f -> {
            String name = f.getName().toLowerCase();
            return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".bmp") || name.endsWith(".pgm");
        });

        if (images == null) return;

        for (File image : images) {
            try {
                // BMP vorbereiten
                File bmpFile;
                boolean isTempBmp = false;

                if (!image.getName().toLowerCase().endsWith(".bmp")) {
                    BufferedImage img = ImageIO.read(image);
                    String baseName = image.getName().replaceAll("\\.[^.]+$", "");
                    bmpFile = new File(folder, baseName + "_temp.bmp");
                    ImageIO.write(img, "bmp", bmpFile); // <--- hier kleingeschrieben
                    isTempBmp = true;
                } else {
                    bmpFile = image;
                }

                // Ziel-SVG-Datei
                String baseName = image.getName().replaceAll("\\.[^.]+$", "");
                File svgFile = new File(folder, baseName + ".svg");

                PotraceRunner.convertToSvg(bmpFile, svgFile);

                if (isTempBmp) {
                    bmpFile.delete();
                }

            } catch (IOException e) {
                System.err.println("Fehler bei: " + image.getName());
                e.printStackTrace();
            }
        }
    }
}
