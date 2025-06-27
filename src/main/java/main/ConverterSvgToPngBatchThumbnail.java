package main;



import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ConverterSvgToPngBatchThumbnail {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ConverterSvgToPngBatchThumbnail::chooseAndConvertFolder);
    }

    private static void chooseAndConvertFolder() {
        JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            LastUsedDirectory.save(selectedDir); // ← Verzeichnis speichern

            // hier deine Verarbeitung starten
    


        File folder = chooser.getSelectedFile();
        File[] svgFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
        if (svgFiles == null || svgFiles.length == 0) {
            JOptionPane.showMessageDialog(null, "Keine SVG-Dateien gefunden.");
            return;
        }
        
 


        for (File svgFile : svgFiles) {
            try {
                BufferedImage img = BufferedImageTranscoderII.transcode(svgFile.toURI().toString(), 100, 100);
                String outName = "thumbnail_" + svgFile.getName().replace(".svg", ".png");
                File outputFile = new File(folder, outName);
                ImageIO.write(img, "png", outputFile);
            } catch (Exception e) {
                System.err.println("Fehler bei: " + svgFile.getName() + " → " + e.getMessage());
            }
        }

        JOptionPane.showMessageDialog(null, "Alle Thumbnails wurden erstellt.");
        }

    }
}
