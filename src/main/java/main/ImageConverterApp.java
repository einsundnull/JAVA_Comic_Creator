package main;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageConverterApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImageConverterApp::createAndShowUI);
    }

    public static void createAndShowUI() {
        JFrame frame = new JFrame("Image to BMP Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 150);
        frame.setLayout(new FlowLayout());

        JButton selectDirButton = new JButton("Verzeichnis auswählen und konvertieren");
        selectDirButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File folder = chooser.getSelectedFile();
                LastUsedDirectory.save(folder);
                convertImagesToBmp(folder);
                JOptionPane.showMessageDialog(frame, "Konvertierung abgeschlossen.");
            }
        });

        frame.add(selectDirButton);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void convertImagesToBmp(File directory) {
        File[] files = directory.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg") ||
                name.toLowerCase().endsWith(".png") ||
                name.toLowerCase().endsWith(".gif")
        );
        if (files == null) return;

        for (File file : files) {
            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    BufferedImage rgbImage = new BufferedImage(
                            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgbImage.createGraphics();
                    g.drawImage(image, 0, 0, Color.WHITE, null); // Transparenz ersetzen
                    g.dispose();

                    String nameWithoutExt = file.getName().replaceAll("\\.[^.]+$", "");
                    File bmpFile = new File(file.getParent(), nameWithoutExt + ".bmp");
                    ImageIO.write(rgbImage, "bmp", bmpFile);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
