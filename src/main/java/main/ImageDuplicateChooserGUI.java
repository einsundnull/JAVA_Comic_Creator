package main;

import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ImageDuplicateChooserGUI {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Bildvergleich");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(300, 100);
			frame.setLayout(new FlowLayout());

			JButton fastButton = new JButton("Schnell (Hash)");
			JButton safeButton = new JButton("Sicher (Pixel)");

			fastButton.addActionListener(e -> chooseAndProcess(true));
			safeButton.addActionListener(e -> chooseAndProcess(false));

			frame.add(fastButton);
			frame.add(safeButton);
			frame.setVisible(true);
		});
	}

	private static void chooseAndProcess(boolean fast) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;

		Path dir = chooser.getSelectedFile().toPath();
		Path targetDir;
		try {
			targetDir = Files.createDirectories(dir.resolve("duplikate"));
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}

		new Thread(() -> {
			try {
				if (fast) {
					VisualDuplicateFinder.processImagesHashOnly(dir, targetDir);
				} else {
					VisualDuplicateFinder.processImagesPixelCompare(dir, targetDir);
				}
				JOptionPane.showMessageDialog(null, "Fertig.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}).start();
	}
}
