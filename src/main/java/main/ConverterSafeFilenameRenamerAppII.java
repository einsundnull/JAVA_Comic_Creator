package main;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import javax.swing.*;

public class ConverterSafeFilenameRenamerAppII {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(ConverterSafeFilenameRenamerApp::createAndShowUI);
	}

	public static void createAndShowUI() {
		JFrame frame = new JFrame("Bild-Dateien sicher umbenennen");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(450, 150);
		frame.setLayout(new FlowLayout());

		JButton selectDirButton = new JButton("Verzeichnis auswählen und umbenennen");
		selectDirButton.addActionListener((ActionEvent e) -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int result = chooser.showOpenDialog(frame);
			if (result == JFileChooser.APPROVE_OPTION) {
				File folder = chooser.getSelectedFile();
				int renamedCount = renameUnsafeFilenames(folder);
				JOptionPane.showMessageDialog(frame, "Umbenennung abgeschlossen.\nDateien umbenannt: " + renamedCount);
			}
		});

		frame.add(selectDirButton);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private static int renameUnsafeFilenames(File directory) {
		if (directory == null || !directory.isDirectory())
			return 0;

		String[] imageExtensions = { ".jpg", ".jpeg", ".png", ".gif", ".bmp" };
		File[] files = directory.listFiles(file -> {
			String name = file.getName().toLowerCase();
			for (String ext : imageExtensions) {
				if (name.endsWith(ext)) return true;
			}
			return false;
		});

		if (files == null || files.length == 0)
			return 0;

		Arrays.sort(files); // konsistente Reihenfolge
		int renamedCount = 0;

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String extension = file.getName().substring(file.getName().lastIndexOf("."));
			String base = generateName(i);
			File newFile = new File(directory, base + extension);

			if (!file.getName().equals(newFile.getName())) {
				if (!newFile.exists()) {
					if (file.renameTo(newFile)) renamedCount++;
				} else {
					JOptionPane.showMessageDialog(null, "Datei existiert schon: " + newFile.getName());
				}
			}
		}
		return renamedCount;
	}

	private static String generateName(int index) {
		int numPerPrefix = 999;
		int prefixIndex = index / numPerPrefix;
		int number = index % numPerPrefix + 1;
		StringBuilder prefix = new StringBuilder();

		do {
			prefix.insert(0, (char) ('a' + (prefixIndex % 26)));
			prefixIndex = prefixIndex / 26 - 1;
		} while (prefixIndex >= 0);

		return prefix.toString() + number;
	}
}
