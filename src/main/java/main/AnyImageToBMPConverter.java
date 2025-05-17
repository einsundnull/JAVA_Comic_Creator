package main;



import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class AnyImageToBMPConverter {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File folder = chooser.getSelectedFile();
                LastUsedDirectory.save(folder);

                System.out.println("Hinweis: Stelle sicher, dass 'potrace.exe' z. B. in C:\\Programme\\potrace\\potrace.exe liegt.");

                File[] images = folder.listFiles(f -> {
                    String name = f.getName().toLowerCase();
                    return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                            || name.endsWith(".bmp") || name.endsWith(".pgm");
                });

                if (images == null) return;

                for (File image : images) {
                    try {
                        File bmpFile;
                        boolean isAlreadyBmp = image.getName().toLowerCase().endsWith(".bmp");

                        if (!isAlreadyBmp) {
                            String tmpName = image.getName().replaceAll("\\.[^.]+$", "") + ".tmp.bmp";
                            bmpFile = new File(folder, tmpName);
                            ImageConverter.convertToRealBmp(image, bmpFile);
                        } else {
                            bmpFile = image;
                        }

                        String baseName = image.getName().replaceAll("\\.[^.]+$", "");
                        File svgFile = new File(folder, baseName + ".svg");

                        PotraceRunner.convertToSvg(bmpFile, svgFile);

                        if (!isAlreadyBmp) {
                            bmpFile.delete();
                        }

                    } catch (IOException e) {
                        System.err.println("Fehler bei: " + image.getName());
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}


