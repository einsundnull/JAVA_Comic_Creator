package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class ScreenshotPanel extends JPanel {
	private static final ArrayList<ColorInfoClass> colorHistory = new ArrayList<>();
	private static JPanel colorListPanel; // Jetzt als Klassenvariable
	private static JLabel imageLabel;
	private static JScrollPane imageScroll;
	private static BufferedImage image;

	public static void showScreenshotDialog(JFrame parentFrame, File screenShotFile) {
		JFrame dialog = new JFrame("Screenshot Tools");
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dialog.setSize(1000, 700);
		dialog.setLayout(new BorderLayout());

		try {
			image = ImageIO.read(screenShotFile);
			imageLabel = new JLabel(new ImageIcon(image));
			imageScroll = new JScrollPane(imageLabel);

			// Hauptpanel für Bild und untere Buttons
			JPanel mainContentPanel = new JPanel(new BorderLayout());
			mainContentPanel.add(imageScroll, BorderLayout.CENTER);

			// Neue Buttonleiste unter dem Bild
			JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
			bottomButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			// Dummy-Buttons hinzufügen
			String[] dummyButtonLabels = { "Clean Color", "Split Color", "Split All Colors", "Helligkeit", "Schärfe",
					"Größe" };
			for (String label : dummyButtonLabels) {
				JButton btn = new JButton(label);
				btn.addActionListener(e -> {
					if (btn.getLabel() == "Clean Color") {
						image = ScreenshotPanel.replaceColorsWithClosestMatch(screenShotFile);
//						image = separateColorBorders(image);
						if (image != null) {
							imageLabel.setIcon(new ImageIcon(image));
							imageLabel.revalidate();
							imageLabel.repaint();
						}
					} else if (btn.getLabel() == "Split Color") {

						image = image = separateColorBorders(image, colorHistory);
						if (image != null) {
							imageLabel.setIcon(new ImageIcon(image));
							imageLabel.revalidate();
							imageLabel.repaint();
						}
					} else if (btn.getLabel() == "Split All Colors") {

						image = separateColorBorders(image);
						if (image != null) {
							imageLabel.setIcon(new ImageIcon(image));
							imageLabel.revalidate();
							imageLabel.repaint();
						}
					}
//            	else if(btn.getLabel() == "Split Color") {
//            		
//            	}
//					JOptionPane.showMessageDialog(dialog, label + "-Funktion würde hier aktiviert werden");
				});
				bottomButtonPanel.add(btn);
			}

			mainContentPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

			// Rechte Panel für Farben
			JPanel rightPanel = new JPanel(new BorderLayout());
			rightPanel.setPreferredSize(new Dimension(200, 0));
			rightPanel.setBorder(new TitledBorder("Farbpalette"));

			colorListPanel = new JPanel();
			colorListPanel.setLayout(new BoxLayout(colorListPanel, BoxLayout.Y_AXIS));
			JScrollPane colorScroll = new JScrollPane(colorListPanel);

			// Pipetten-Funktion
			imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			imageLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						Color pickedColor = new Color(image.getRGB(e.getX(), e.getY()));
						boolean add = true;
						if (!colorHistory.isEmpty())
							for (ColorInfoClass c : colorHistory) {
								if (c.color.equals(pickedColor)) {
									add = false;
								}
							}
						if (add) {
							colorHistory.add(new ColorInfoClass(pickedColor, false));
							updateColorList();
						}
					}
				}
			});

			// Button-Panel für Farbpalette
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton pipetteBtn = new JButton("Pipette");
			JButton deleteBtn = new JButton("Löschen");
			JButton closeBtn = new JButton("Schließen");
//        JButton cleanButton = new JButton("Clean");
			JButton clearBtn = new JButton("Clear");

//        cleanButton.addActionListener(e -> {
////            BufferedImage image = ScreenshotPanel.replaceColorsWithClosestMatch(screenShotFile);
////            image = separateColorBorders(image);
////            if (image != null) {
////                imageLabel.setIcon(new ImageIcon(image));
////                imageLabel.revalidate();
////                imageLabel.repaint();
////            }
//        });

			pipetteBtn.addActionListener(e -> {
				imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			});

			deleteBtn.addActionListener(e -> {
				for (int i = colorListPanel.getComponentCount() - 1; i >= 0; i--) {
					Component comp = colorListPanel.getComponent(i);
					if (comp instanceof JPanel) {
						JCheckBox checkBox = (JCheckBox) ((JPanel) comp).getComponent(1);
						if (checkBox.isSelected()) {
							colorHistory.remove(i);
						}
					}
				}
				updateColorList();
			});

			closeBtn.addActionListener(e -> dialog.dispose());
			clearBtn.addActionListener(e -> {
				colorHistory.clear();
				updateColorList();
			});

			buttonPanel.add(pipetteBtn);
//        buttonPanel.add(cleanButton);
			buttonPanel.add(clearBtn);
			buttonPanel.add(deleteBtn);
			buttonPanel.add(closeBtn);

			// Layout zusammensetzen
			rightPanel.add(colorScroll, BorderLayout.CENTER);
			rightPanel.add(buttonPanel, BorderLayout.SOUTH);

			// Hauptkomponenten hinzufügen (nur einmal!)
			dialog.add(mainContentPanel, BorderLayout.CENTER);
			dialog.add(rightPanel, BorderLayout.EAST);

		} catch (IOException e) {
			dialog.add(new JLabel("Fehler beim Laden: " + e.getMessage()));
		}

		dialog.setLocationRelativeTo(parentFrame);
		dialog.setVisible(true);
	}

	private static void updateColorList() {
		colorListPanel.removeAll();

		for (ColorInfoClass dataColorHistory : colorHistory) {
			ColorItemPanel item = new ColorItemPanel(dataColorHistory.color, e -> {
				colorHistory.remove(dataColorHistory);
				updateColorList();
			});
			colorListPanel.add(item);
			colorListPanel.add(Box.createVerticalStrut(2));
		}

		colorListPanel.revalidate();
		colorListPanel.repaint();
	}

	public static BufferedImage replaceColorsWithClosestMatch(File imageFile) {
		try {
			File backupFile = new File(imageFile.getParent(), imageFile.getName() + ".bak");
			BufferedImage original = ImageIO.read(imageFile);
			ImageIO.write(original, "png", backupFile);

			int width = original.getWidth();
			int height = original.getHeight();

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					Color pixel = new Color(original.getRGB(x, y));
					Color closest = findClosestColor(pixel);
					original.setRGB(x, y, closest.getRGB());
				}
			}

//			ImageIO.write(original, "png", imageFile);
			return original;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Color findClosestColor(Color target) {
		if (colorHistory.isEmpty())
			return target;

		Color closest = colorHistory.get(0).color;
		double minDist = colorDistance(target, closest);

		for (ColorInfoClass c : colorHistory) {
			double dist = colorDistance(target, c.color);
			if (dist < minDist) {
				minDist = dist;
				closest = c.color;
			}
		}

		return closest;
	}

	private static double colorDistance(Color c1, Color c2) {
		int dr = c1.getRed() - c2.getRed();
		int dg = c1.getGreen() - c2.getGreen();
		int db = c1.getBlue() - c2.getBlue();
		return Math.sqrt(dr * dr + dg * dg + db * db);
	}

	public static BufferedImage separateColorBorders(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		BufferedImage result = new BufferedImage(width, height, image.getType());

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color current = new Color(image.getRGB(x, y));
				if (!isInColorHistory(current)) {
					result.setRGB(x, y, current.getRGB());
					continue;
				}

				boolean borderFound = false;

				// Nachbarn prüfen
				for (int dy = -1; dy <= 1 && !borderFound; dy++) {
					for (int dx = -1; dx <= 1 && !borderFound; dx++) {
						if (dx == 0 && dy == 0)
							continue;
						int nx = x + dx;
						int ny = y + dy;

						if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
							Color neighbor = new Color(image.getRGB(nx, ny));
							if (!isInColorHistory(neighbor))
								continue;
							if (!isSameColor(current, neighbor)) {
								// Grenze erkannt → aktuelles Pixel weiß
								result.setRGB(x, y, Color.WHITE.getRGB());
								borderFound = true;
							}
						}
					}
				}

				if (!borderFound) {
					result.setRGB(x, y, current.getRGB());
				}
			}
		}

		return result;
	}

	public static BufferedImage separateColorBorders(BufferedImage image, ArrayList<ColorInfoClass> colorHistory) {
	    int width = image.getWidth();
	    int height = image.getHeight();

	    BufferedImage result = new BufferedImage(width, height, image.getType());

	    // Get only selected colors
	    ArrayList<Color> selectedColors = new ArrayList<>();
	    for (ColorInfoClass colorInfo : colorHistory) {
	        if (!colorInfo.isSelected()) {  // Use getter method instead of direct field access
	            selectedColors.add(colorInfo.getColor());
	        }
	    }

	    // If no colors are selected, return original image
	    if (selectedColors.isEmpty()) {
	        return image;
	    }

	    for (int y = 0; y < height; y++) {
	        for (int x = 0; x < width; x++) {
	            Color current = new Color(image.getRGB(x, y));
	            
	            // Only process if color is in selected colors
	            if (!isInColorList(current, selectedColors)) {
	                result.setRGB(x, y, current.getRGB());
	                continue;
	            }

	            boolean borderFound = false;

	            // Check 8-connected neighbors
	            for (int dy = -1; dy <= 1 && !borderFound; dy++) {
	                for (int dx = -1; dx <= 1 && !borderFound; dx++) {
	                    if (dx == 0 && dy == 0) continue; // Skip center pixel
	                    
	                    int nx = x + dx;
	                    int ny = y + dy;

	                    if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
	                        Color neighbor = new Color(image.getRGB(nx, ny));
	                        
	                        // Only consider neighbors that are also selected colors
	                        if (isInColorList(neighbor, selectedColors)) {
	                            if (!isSameColor(current, neighbor)) {
	                                // Border detected - make this pixel white
	                                result.setRGB(x, y, Color.WHITE.getRGB());
	                                borderFound = true;
	                            }
	                        }
	                    }
	                }
	            }

	            if (!borderFound) {
	                result.setRGB(x, y, current.getRGB());
	            }
	        }
	    }

	    return result;
	}

	private static boolean isInColorList(Color c, ArrayList<Color> colorList) {
		for (Color ref : colorList) {
			if (isSameColor(c, ref))
				return true;
		}
		return false;
	}

	private static boolean isInColorHistory(Color c) {
		for (ColorInfoClass ref : colorHistory) {
			if (isSameColor(c, ref.color))
				return true;
		}
		return false;
	}

	private static boolean isSameColor(Color c1, Color c2) {
		return c1.getRGB() == c2.getRGB();
	}

}

class ColorItemPanel extends JPanel {

	public ColorItemPanel(Color color, ActionListener onDelete) {
		setLayout(new BorderLayout(0, 2));
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setPreferredSize(new Dimension(180, 50));
		setMaximumSize(new Dimension(180, 50));
		// Oben: Farbfeld (zentriert)
		JPanel colorSwatch = new JPanel();
		colorSwatch.setPreferredSize(new Dimension(30, 30));
		colorSwatch.setBackground(color);
		colorSwatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		JPanel swatchPanel = new JPanel();
		swatchPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		swatchPanel.add(colorSwatch);

		// Unten: Checkbox + Delete
		JCheckBox checkBox = new JCheckBox();
		checkBox.setToolTipText(String.format("RGB(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue()));

		JButton deleteButton = new JButton("✖");
		deleteButton.setFont(new Font("Dialog", Font.PLAIN, 10));
		deleteButton.setMargin(new Insets(2, 6, 2, 6));
		deleteButton.addActionListener(onDelete);

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		controlPanel.add(checkBox);
		controlPanel.add(deleteButton);

		add(swatchPanel, BorderLayout.NORTH);
		add(controlPanel, BorderLayout.SOUTH);
	}
}

class ColorInfoClass {
	Color color;
	boolean selected;

	public ColorInfoClass(Color color, boolean selected) {
		this.color = color;
		this.selected = selected;

	}

	public Color getColor() {
		// TODO Auto-generated method stub
		Color c = color;
		return c;
	}

	public boolean isSelected() {
		// TODO Auto-generated method stub
		boolean b = selected;
		return b;
	}

}
