package main;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImageListView {

	public static JPanel[] createImageListView(File directory, JPanel scenePanel) {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Provided path is not a directory");
		}

		// Get all image files from directory
		File[] imageFiles = directory.listFiles((dir, name) -> {
			String lower = name.toLowerCase();
			return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
					|| lower.endsWith(".bmp") || lower.endsWith(".svg");
		});
		System.out.println("imageListView - size: "+ imageFiles.length);

		if (imageFiles == null || imageFiles.length == 0) {
			return new JPanel[] { new JPanel(), scenePanel };
		}

		// Sort files alphabetically
		Arrays.sort(imageFiles);

		// Create the row panels
		JPanel row[] = new JPanel[2];
		row[0] = new JPanel();
		row[0].setLayout(new BoxLayout(row[0], BoxLayout.Y_AXIS));
		row[0].setBackground(Color.WHITE);
		row[1] = scenePanel;

		// Add scroll pane for the list
		JScrollPane scrollPane = new JScrollPane(row[0]);
		scrollPane.setPreferredSize(new Dimension(250, 600));

		// Create an item for each image
		for (File imageFile : imageFiles) {
			JPanel itemPanel = createImageListItem(imageFile, scenePanel);
			row[0].add(itemPanel);
			row[0].add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
		}

		return row;
	}

	private static JPanel createImageListItem(File imageFile, JPanel scenePanel) {
		JPanel itemPanel = new JPanel(new BorderLayout());
		itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
		itemPanel.setBackground(Color.WHITE);
		itemPanel.setOpaque(true);
		itemPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

		// Thumbnail
		JLabel thumb = new JLabel();
		thumb.setPreferredSize(new Dimension(50, 50));
		thumb.setOpaque(true);
		thumb.setBackground(Color.WHITE);

		// Load thumbnail in background
		new Thread(() -> {
//			try {
				BufferedImage img;
//				if (imageFile.getName().toLowerCase().endsWith(".svg")) {
//					// Special handling for SVG - you would need your SVGDataManager here
					img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2d = img.createGraphics();
					g2d.setColor(Color.LIGHT_GRAY);
					g2d.fillRect(0, 0, 50, 50);
					g2d.setColor(Color.BLACK);
					g2d.drawString("SVG", 10, 25);
					g2d.dispose();
//				} else {
					// Load and scale other image formats
//					BufferedImage original = ImageIO.read(imageFile);
//					if (original != null) {
//						img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
//						Graphics2D g2d = img.createGraphics();
//						g2d.drawImage(original, 0, 0, 50, 50, null);
//						g2d.dispose();
//					} else {
//						throw new IOException("Could not read image");
//					}
//				}

				SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
//			} catch (IOException e) {
//				System.err.println("Error loading thumbnail: " + e.getMessage());
//				SwingUtilities.invokeLater(() -> thumb.setText("Error"));
//			}
		}).start();

		// File name label
		JLabel nameLabel = new JLabel(imageFile.getName());
		nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

		// Panel for the image and name
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(thumb, BorderLayout.WEST);
		leftPanel.add(nameLabel, BorderLayout.CENTER);
		leftPanel.setOpaque(false);

		// Add mouse hover effect
		itemPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				itemPanel.setBackground(Color.LIGHT_GRAY);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				itemPanel.setBackground(Color.WHITE);
			}
		});

		// Add buttons panel (similar to your example)
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		buttonsPanel.setOpaque(false);

		JButton mirrorVertical = new JButton("↔");
		mirrorVertical.setPreferredSize(new Dimension(30, 25));
		mirrorVertical.setMargin(new Insets(0, 0, 0, 0));

		JButton mirrorHorizontal = new JButton("↨");
		mirrorHorizontal.setPreferredSize(new Dimension(30, 25));
		mirrorHorizontal.setMargin(new Insets(0, 0, 0, 0));

		JButton setColor = new JButton("C");
		setColor.setPreferredSize(new Dimension(30, 25));
		setColor.setMargin(new Insets(0, 0, 0, 0));

		buttonsPanel.add(mirrorVertical);
		buttonsPanel.add(mirrorHorizontal);
		buttonsPanel.add(setColor);

		// Add components to item panel
		itemPanel.add(leftPanel, BorderLayout.CENTER);
		itemPanel.add(buttonsPanel, BorderLayout.EAST);

		return itemPanel;
	}
}