package main;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class SaveImageFileDialog {

	
	/**
	 * Zeigt einen Speichern-Dialog an und speichert das übergebene Bild unter dem gewählten Namen.
	 * 
	 * @param parentComponent Die übergeordnete Komponente für den Dialog
	 * @param imageToSave Das zu speichernde Bild
	 * @param suggestedFileName Vorschlag für den Dateinamen (Optional)
	 * @return Der Pfad der gespeicherten Datei oder null, wenn abgebrochen wurde
	 */
	public static File showSaveImageDialog(Component parentComponent, BufferedImage imageToSave, String suggestedFileName) {
	    // JFileChooser für den Speichern-Dialog erstellen
	    JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setDialogTitle("Bild speichern");
	    
	    // Filter für PNG-Dateien setzen
	    FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG-Bilder (*.png)", "png");
	    fileChooser.addChoosableFileFilter(pngFilter);
	    
	    // Weitere Filter für andere Bildformate hinzufügen, falls gewünscht
	    FileNameExtensionFilter jpgFilter = new FileNameExtensionFilter("JPEG-Bilder (*.jpg, *.jpeg)", "jpg", "jpeg");
	    fileChooser.addChoosableFileFilter(jpgFilter);
	    
	    // PNG als Standard-Filter setzen
	    fileChooser.setFileFilter(pngFilter);
	    
	    // Falls ein Dateiname vorgeschlagen wurde, diesen setzen
	    if (suggestedFileName != null && !suggestedFileName.isEmpty()) {
	        fileChooser.setSelectedFile(new File(suggestedFileName));
	    }
	    
	    // Speichern-Dialog anzeigen
	    int result = fileChooser.showSaveDialog(parentComponent);
	    
	    // Wenn Benutzer "Speichern" geklickt hat
	    if (result == JFileChooser.APPROVE_OPTION) {
	        File selectedFile = fileChooser.getSelectedFile();
	        String filePath = selectedFile.getAbsolutePath();
	        
	        // Dateiformat aus dem gewählten Filter bestimmen
	        String format = "png"; // Standardformat
	        FileFilter selectedFilter = fileChooser.getFileFilter();
	        if (selectedFilter instanceof FileNameExtensionFilter) {
	            String description = selectedFilter.getDescription().toLowerCase();
	            if (description.contains("jpg") || description.contains("jpeg")) {
	                format = "jpg";
	            }
	        }
	        
	        // Dateierweiterung prüfen und ggf. hinzufügen
	        if (!filePath.toLowerCase().endsWith("." + format)) {
	            selectedFile = new File(filePath + "." + format);
	        }
	        
	        // Prüfen, ob die Datei bereits existiert
	        if (selectedFile.exists()) {
	            int overwrite = JOptionPane.showConfirmDialog(
	                parentComponent,
	                "Die Datei \"" + selectedFile.getName() + "\" existiert bereits. Überschreiben?",
	                "Datei existiert bereits",
	                JOptionPane.YES_NO_OPTION,
	                JOptionPane.WARNING_MESSAGE
	            );
	            
	            if (overwrite != JOptionPane.YES_OPTION) {
	                // Benutzer hat "Nein" gewählt, also Dialog erneut anzeigen
	                return showSaveImageDialog(parentComponent, imageToSave, selectedFile.getAbsolutePath());
	            }
	        }
	        
	        try {
	            // Bild speichern
	            ImageIO.write(imageToSave, format, selectedFile);
	            return selectedFile;
	        } catch (IOException e) {
	            JOptionPane.showMessageDialog(
	                parentComponent,
	                "Fehler beim Speichern der Datei:\n" + e.getMessage(),
	                "Fehler",
	                JOptionPane.ERROR_MESSAGE
	            );
	            e.printStackTrace();
	        }
	    }
	    
	    // Bei Abbruch oder Fehler null zurückgeben
	    return null;
	}
}
