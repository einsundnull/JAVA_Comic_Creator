// Klasse: PotraceUIAll.java
package main;

import javax.swing.*;
import java.io.File;

public class ConverterToSVGPotraceUIAll2 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                System.out.println("If an error occurs it might be that you have not installed Potrace!\n"
                        + "Make sure that potrace.exe can be found here -> \"C:\\\\Programme\\\\potrace\\\\potrace.exe\", ...");

                File folder = chooser.getSelectedFile();
                LastUsedDirectory.save(folder);

                ConverterImageToSvgClassSuitable.convertAllImagesInFolder(folder); // eingebaut
            }
        });
    }
}
