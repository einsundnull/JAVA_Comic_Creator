package main;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Consumer;

import javax.swing.JColorChooser;

public class ColorPickerWindow {

	public static void open(Component parent, Consumer<Color> callback) {
		Color selectedColor = JColorChooser.showDialog(parent, "Farbe wählen", Color.WHITE);
		if (selectedColor != null && callback != null) {
			callback.accept(selectedColor);
		}
	}
	
	public static String colorToHex(Color color) {
	    if (color == null) {
	        return "#000000"; // Fallback für null
	    }
	    
	    // Rot, Grün und Blau Komponenten (0-255) in HEX umwandeln
	    String red = Integer.toHexString(color.getRed());
	    String green = Integer.toHexString(color.getGreen());
	    String blue = Integer.toHexString(color.getBlue());
	    
	    // Führende Nullen hinzufügen falls nötig
	    red = red.length() == 1 ? "0" + red : red;
	    green = green.length() == 1 ? "0" + green : green;
	    blue = blue.length() == 1 ? "0" + blue : blue;
	    
	    return "#" + red + green + blue;
	}
}
