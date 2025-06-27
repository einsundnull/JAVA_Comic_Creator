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
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ListItemLeft {

	static JPanel createThumbnailRowLeft(LinkedList<String> data, JFrame frame, int index,
			SVGDataManager svgDataManager, ListenerLeftTiles listnerLeftTiels) {
		// This method creates the ListView Items on the left and on the right
		// ScrollView.
		File file = new File(data.get(SVGDataManager.FILEPATH));
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

		JLabel thumb = new JLabel();
		thumb.setPreferredSize(new Dimension(120, 120));
		thumb.setOpaque(true);
		thumb.setBackground(Color.WHITE);
		row.add(thumb);

		// Thumbnail laden
		new Thread(() -> {
			BufferedImage img = svgDataManager.getSvgThumbnail(new File(data.get(1)));
			if (img != null)
				SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
		}).start();

		thumb.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {

			}
		});

		JButton add = new JButton("+");
		add.setPreferredSize(new Dimension(30, 25));
		add.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		add.addActionListener(e -> {
			listnerLeftTiels.onClick(data);

		});
		row.add(add, BorderLayout.EAST);
		
		JButton remove = new JButton("-");
		remove.setPreferredSize(new Dimension(30, 25));
		remove.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
		remove.addActionListener(e -> {
			listnerLeftTiels.onClickRemove(data);

		});
		row.add(remove, BorderLayout.EAST);
		row = addEditTextField(row, index, frame, file, svgDataManager, listnerLeftTiels);

		return row;
	}

//	static JPanel createImageRow(File file, JFrame frame, 
//			SVGDataManager svgDataManager, ListenerLeftTiles listnerLeftTiels) {
//		// This method creates the ListView Items on the left and on the right
//		// ScrollView.
//	
//		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
//
//		JLabel thumb = new JLabel();
//		thumb.setPreferredSize(new Dimension(120, 120));
//		thumb.setOpaque(true);
//		thumb.setBackground(Color.WHITE);
//		row.add(thumb);
//
//		// Thumbnail laden
//		new Thread(() -> {
//			BufferedImage img = svgDataManager.getThumbnail(file);
//			if (img != null)
//				SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
//		}).start();
//
//		thumb.addMouseListener(new MouseAdapter() {
//			public void mouseClicked(MouseEvent e) {
//
//			}
//		});
//
//		JButton add = new JButton("+");
//		add.setPreferredSize(new Dimension(30, 25));
//		add.setMargin(new Insets(0, 0, 0, 0)); // Minimale Padding
//		add.addActionListener(e -> {
//			listnerLeftTiels.onClick(data);
//
//		});
//		row.add(add, BorderLayout.EAST);
//	
//
//		return row;
//	}

	private static JPanel addEditTextField(JPanel row, int index, JFrame frame, File file,
			SVGDataManager svgDataManager, ListenerLeftTiles listnerLeftTiels) {
		JLabel nameLabel = new JLabel(file.getName());
		nameLabel.setForeground(Color.BLUE);
		nameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		nameLabel.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				String newName = JOptionPane.showInputDialog(frame, "Neuer Dateiname:", file.getName());
				if (newName != null && !newName.trim().isEmpty()) {
					if (!newName.toLowerCase().endsWith(".svg"))
						newName += ".svg";
					File renamed = new File(file.getParent(), newName);
					if (renamed.exists()) {
						JOptionPane.showMessageDialog(frame, "Datei existiert bereits.");
						return;
					}
					if (file.renameTo(renamed)) {
						nameLabel.setText(newName);
						svgDataManager.imageCache.remove(file.getAbsolutePath());

						// Aktualisiere Tile-Mapping nach Umbenennung
						listnerLeftTiels.actualizeTileMapping(file.getName(), index);
//						CustomImageTile tile = allTiles.remove();
//						if (tile != null) {
//					
//							tile.setFilename(newName);
//							allTiles.put(newName, tile);
//						}
//
//						loadFolderFiles(currentFolder, 0); // Refresh alle
					} else
						JOptionPane.showMessageDialog(frame, "Umbenennen fehlgeschlagen.");
				}
			}
		});
		row.add(nameLabel);
		return row;
	}

}
