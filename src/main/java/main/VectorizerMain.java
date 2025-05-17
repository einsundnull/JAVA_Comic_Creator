package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class VectorizerMain {
    public static void main(String[] args) {
        try {
            BufferedImage image = ImageIO.read(new File("images/input.png"));
            String svg = Vectorizer.rasterToSVG(image);
            Files.writeString(Path.of("images/output.svg"), svg);
            System.out.println("SVG gespeichert.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
