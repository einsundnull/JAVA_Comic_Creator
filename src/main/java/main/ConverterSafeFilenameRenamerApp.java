package main;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ConverterSafeFilenameRenamerApp {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(ConverterSafeFilenameRenamerApp::createAndShowUI);
	}

	public static void createAndShowUI() {
		JFrame frame = new JFrame("Safe Filename Renamer für Bilddateien");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(450, 150);
		frame.setLayout(new FlowLayout());

		JButton selectDirButton = new JButton("Verzeichnis auswählen und umbenennen");
		selectDirButton.addActionListener((ActionEvent e) -> {
			JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int result = chooser.showOpenDialog(frame);
			if (result == JFileChooser.APPROVE_OPTION) {
				File folder = chooser.getSelectedFile();
				LastUsedDirectory.save(folder);

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
				if (name.endsWith(ext))
					return true;
			}
			return false;
		});

		if (files == null)
			return 0;

		int renamedCount = 0;

		for (File file : files) {
			String originalName = file.getName();
			String extension = originalName.substring(originalName.lastIndexOf("."));
			String nameWithoutExt = originalName.substring(0, originalName.lastIndexOf("."));

			String safeName = toSafeFilename(nameWithoutExt);

			if (!safeName.equals(nameWithoutExt)) {
				File newFile = new File(directory, safeName + extension);
				if (!newFile.exists()) {
					if (file.renameTo(newFile))
						renamedCount++;
				} else {
					int count = 1;
					File tmpFile;
					do {
						tmpFile = new File(directory, safeName + "_" + count + extension);
						count++;
					} while (tmpFile.exists());
					if (file.renameTo(tmpFile))
						renamedCount++;
				}
			}
		}
		return renamedCount;
	}

	private static String toSafeFilename(String name) {
		String replaced = name.replace("ä", "ae").replace("Ä", "Ae").replace("ö", "oe").replace("Ö", "Oe")
				.replace("ü", "ue").replace("Ü", "Ue").replace("ß", "ss");

		return replaced.replaceAll("[^a-zA-Z0-9]", "");
	}
}
