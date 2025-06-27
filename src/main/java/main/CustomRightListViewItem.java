package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class CustomRightListViewItem {

	public CustomRightListViewItem(CustomImageSVGTile tile, JPanel scenePanel, SVGDataManager svgDataManager,
			ListenerRightTiles listnerRightListItems) {
		setRow(createThumbnailRowRight(tile, scenePanel, svgDataManager, listnerRightListItems));
	}

	private JPanel row;

	public JPanel createThumbnailRowRight(CustomImageSVGTile tile, JPanel scenePanel, SVGDataManager svgDataManager,
			ListenerRightTiles listnerRightListItems) {

		File file = new File(tile.getData().get(1));
		row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
		row.putClientProperty("id", tile.getID());
		row.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				listnerRightListItems.mouseEntered(file, scenePanel, true);
				row.setBackground(Color.LIGHT_GRAY); // Auch das aktuelle Panel hervorheben
			}

			@Override
			public void mouseExited(MouseEvent e) {
				listnerRightListItems.mouseExited(file, scenePanel, false);
				row.setBackground(Color.WHITE); // Auch das aktuelle Panel hervorheben
			}
		});

		// Setzen Sie den Hintergrund, damit die Hervorhebung sichtbar ist
		row.setBackground(Color.WHITE);
		row.setOpaque(true);

		JLabel thumb = new JLabel();
		thumb.setPreferredSize(new Dimension(50, 50));
		thumb.setOpaque(true);
		thumb.setBackground(Color.WHITE);
		row.add(thumb);

		new Thread(() -> {
			BufferedImage img = svgDataManager.getSvgThumbnail(file);
			if (img != null)
				SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
		}).start();

		thumb.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				listnerRightListItems.setComponentZOrder(tile.getPanel(), 0);
//				scenePanel.setComponentZOrder(tile.getPanel(), 0);
			}
		});

		JButton mirrorVertical = new JButton("↔");
		mirrorVertical.setPreferredSize(new Dimension(30, 25));
		mirrorVertical.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		mirrorVertical.addActionListener(e -> {
			tile.toggleMirrorVertical();
		});
		row.add(mirrorVertical, BorderLayout.EAST);

		JButton mirrorHorizontal = new JButton("↨");
		mirrorHorizontal.setPreferredSize(new Dimension(30, 25));
		mirrorHorizontal.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		mirrorHorizontal.addActionListener(e -> {
			tile.toggleMirrorHorizontal();
		});
		row.add(mirrorHorizontal, BorderLayout.EAST);

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
		row.add(setColor, BorderLayout.EAST);

//			row = addCheckBox(row, tile);
		JCheckBox cb = new JCheckBox();
		cb.setSelected(true);
		cb.setOpaque(false);
		cb.addActionListener(e -> {
			if (!cb.isSelected()) {
//					removeSelectedSVG(row, tile);
				listnerRightListItems.checkBoxDeselectAction(row, tile);
			} else {
				// Re-add to scene panel if checkbox is checked again
//					scenePanel = SVGTileViewerAppOutSource.setTileVisible(tile, true, scenePanel, centerScrollPane);
			}
		});

		row.add(cb);

		return row;
	}

	/**
	 * @return the row
	 */
	public JPanel getRow() {
		return row;
	}

	/**
	 * @param row the row to set
	 */
	public void setRow(JPanel row) {
		this.row = row;
	}

//		private JPanel addCheckBox(JPanel row, CustomImageTile tile) {
//			JCheckBox cb = new JCheckBox();
//			cb.setSelected(true);
//			cb.setOpaque(false);
//			cb.addActionListener(e -> {
//				if (!cb.isSelected()) {
////					removeSelectedSVG(row, tile);
//				} else {
//					// Re-add to scene panel if checkbox is checked again
////					scenePanel = SVGTileViewerAppOutSource.setTileVisible(tile, true, scenePanel, centerScrollPane);
//				}
//			});
//			row.add(cb);
//			return row;
//		}
}
