package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class SVGTileViewerAppOutSource {
	
	
	
	private JPanel highlightCorrespondingTileInCanvas(File file, JPanel scenePanel, LinkedList<CustomImageSVGTile> addedTiles, boolean isHovered ) {
		String filename = file.getName();
		for (CustomImageSVGTile tile : addedTiles) {
			if (tile.getFilename().equals(filename)) {
				if (isHovered) {
					// Hervorhebung des Tiles im Canvas (z.B. mit einem speziellen Rahmen)
					tile.getPanel().setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
				} else {
					// Zurücksetzen auf den normalen Zustand
					if (!tile.isSelected()) {
						tile.getPanel().setBorder(null);
					} else {
						tile.getPanel().setBorder(BorderFactory.createLineBorder(Color.RED, 2));
					}
				}
				break;
			}
		}
		scenePanel.repaint();
		return scenePanel;
	}
	
	private JPanel[] createThumbnailRowRight(CustomImageSVGTile tile, JPanel selectedPanel,JPanel scenePanel, JScrollPane centerScrollPane, LinkedList<CustomImageSVGTile> addedTiles, SVGDataManager svgDataManager) {
		// Bestehender Code...
		File file = new File(tile.getData().get(1));
		JPanel row[] = new JPanel[2];
		row[0] = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)) ;
		row[0].setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
		row[0].putClientProperty("id", tile.getID());
		row[1] = selectedPanel;
		row[2] = scenePanel;
		// Fügen Sie dem gesamten Panel einen MouseListener hinzu
		row[0].addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				row[2] = highlightCorrespondingTileInCanvas(file, row[2],addedTiles, true);
				row[0].setBackground(Color.LIGHT_GRAY); // Auch das aktuelle Panel hervorheben
			}

			@Override
			public void mouseExited(MouseEvent e) {
				row[2] = highlightCorrespondingTileInCanvas(file, row[2],addedTiles, false);
				row[0].setBackground(Color.WHITE); // Zurücksetzen
			}
		});

		// Setzen Sie den Hintergrund, damit die Hervorhebung sichtbar ist
		row[0].setBackground(Color.WHITE);
		row[0].setOpaque(true);
		// This method creates the ListView Items on the left and on the right
		// ScrollView.

		JLabel thumb = new JLabel();
		thumb.setPreferredSize(new Dimension(50, 50));
		thumb.setOpaque(true);
		thumb.setBackground(Color.WHITE);
		row[0].add(thumb);

		new Thread(() -> {
			BufferedImage img = svgDataManager.getSvgThumbnail(file);
			if (img != null)
				SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
		}).start();

		thumb.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				row[1].setComponentZOrder(tile.getPanel(), 0);
			}
		});

		JButton mirrorVertical = new JButton("↔");
		mirrorVertical.setPreferredSize(new Dimension(30, 25));
		mirrorVertical.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		mirrorVertical.addActionListener(e -> {
			tile.toggleMirrorVertical();
		});
		row[0].add(mirrorVertical, BorderLayout.EAST);

		JButton mirrorHorizontal = new JButton("↨");
		mirrorHorizontal.setPreferredSize(new Dimension(30, 25));
		mirrorHorizontal.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		mirrorHorizontal.addActionListener(e -> {
			tile.toggleMirrorHorizontal();
		});
		row[0].add(mirrorHorizontal, BorderLayout.EAST);

		JButton setColor = new JButton("C");
		setColor.setPreferredSize(new Dimension(30, 25));
		setColor.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		setColor.addActionListener(e -> {
			// Öffne Color Picker
			ColorPickerWindow.open(setColor, color -> {
				// verwende die gewählte Farbe
				System.out.println("Farbe: " + color);
				tile.setSVGPathColor(ColorPickerWindow.colorToHex(color));
			});
		});
		row[0].add(setColor, BorderLayout.EAST);

		row[0] = addCheckBox( tile,row[0],row[1],row[2],centerScrollPane)[0];
		
		return row;
	}
	
	public static JPanel setTileVisible(CustomImageSVGTile tile, boolean visible, JPanel scenePanel, JScrollPane centerScrollPane) {
		System.out.println("setTileVisible für " + tile.getData().get(0) + ": " + visible);

		if (tile != null) {
			if (visible) {
				// Tile sichtbar machen und zum scenePanel hinzufügen
				if (tile.getPanel().getParent() != scenePanel) {
					System.out.println("Füge Tile zum scenePanel hinzu: " + tile.getData().get(0));
					scenePanel.add(tile.getPanel());

					// Positioniere das Tile im sichtbaren Bereich
					Rectangle visibleRect = centerScrollPane.getViewport().getViewRect();
					int x = visibleRect.x + 20;
					int y = visibleRect.y + 20;
					tile.getPanel().setLocation(x, y);
				}
				// Tile sichtbar machen
				tile.getPanel().setVisible(true);

			} else {
				// Tile aus scenePanel entfernen
				scenePanel.remove(tile.getPanel());
				tile.setSelected(false);
			}

			// UI aktualisieren - SEHR WICHTIG!
			scenePanel.revalidate();
			scenePanel.repaint();
		}
		return scenePanel;
	}
	
	private JPanel[] addCheckBox(CustomImageSVGTile tile ,JPanel row,  JPanel selectedPanel ,JPanel scenePanel,JScrollPane centerScrollPane  ) {
		JPanel[] returnValues = new JPanel[3];
		returnValues[0] = row;
		returnValues[1] = selectedPanel;
		returnValues[2] = scenePanel;
		JCheckBox cb = new JCheckBox();
		cb.setSelected(true);
		cb.setOpaque(false);
		cb.addActionListener(e -> {
			if (!cb.isSelected()) {
				// Remove from selected panel
				returnValues[1].remove(row);
				returnValues[1].revalidate();
				returnValues[1].repaint();

				// Remove from scene panel
				returnValues[2] = SVGTileViewerAppOutSource.setTileVisible(tile, false, returnValues[2], centerScrollPane);
			} else {
				// Re-add to scene panel if checkbox is checked again
				returnValues[2] = SVGTileViewerAppOutSource.setTileVisible(tile, true, returnValues[2], centerScrollPane);
			}
		});
		returnValues[0].add(cb);
		return returnValues;
	}
	
	/**
	 * Zeigt ein gespeichertes Bild in einem JDialog an.
	 * @param parentFrame Eltern-Frame (kann null sein)
	 * @param screenShotFile Die Bilddatei, die angezeigt werden soll
	 */
	public static void showScreenshotDialog(JFrame parentFrame, File screenShotFile) {
		// Erstelle ein JFrame statt JDialog für Minimize/Maximize/Close
		JFrame dialog = new JFrame("Screenshot Preview");
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dialog.setSize(800, 600);
		dialog.setLocationRelativeTo(parentFrame);

		// Hauptpanel mit BorderLayout
		JPanel mainPanel = new JPanel(new BorderLayout());

		try {
			// Bild laden
			BufferedImage image = ImageIO.read(screenShotFile);
			JLabel imageLabel = new JLabel(new ImageIcon(image));

			// ScrollPane für große Bilder
			JScrollPane scrollPane = new JScrollPane(imageLabel);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

			mainPanel.add(scrollPane, BorderLayout.CENTER);

			// Button-Panel am unteren Rand
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton closeButton = new JButton("Close");
			closeButton.addActionListener(e -> dialog.dispose());

			buttonPanel.add(closeButton);
			mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		} catch (IOException e) {
			mainPanel.add(new JLabel("Could not load image: " + e.getMessage(), JLabel.CENTER));
			dialog.setSize(400, 200);
		}

		dialog.add(mainPanel);
		dialog.setVisible(true);
	}

}
