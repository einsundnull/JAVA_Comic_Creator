package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;

public class SvgTileViewerApp2 {

    private JFrame frame;
    private JPanel tilePanel;
    private JSVGCanvas svgCanvas;
    private File currentFolder;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SvgTileViewerApp2().createAndShowUI());
    }

    private void createAndShowUI() {
        frame = new JFrame("SVG Tile Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        JButton chooseFolderBtn = new JButton("Verzeichnis auswählen");
        chooseFolderBtn.addActionListener(e -> chooseFolder());

        tilePanel = new JPanel();
        tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS)); // Vertikale Liste

        JScrollPane tileScrollPane = new JScrollPane(tilePanel);
        tileScrollPane.setPreferredSize(new Dimension(300, 0));

        svgCanvas = new JSVGCanvas();
        svgCanvas.setBackground(Color.WHITE);

        frame.add(chooseFolderBtn, BorderLayout.NORTH);
        frame.add(tileScrollPane, BorderLayout.WEST);
        frame.add(svgCanvas, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

   
    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            currentFolder = chooser.getSelectedFile();
            LastUsedDirectory.save(currentFolder);
            loadSvgTiles(currentFolder);
//            loadSvgTilesWithPng(currentFolder);
        }
    }

    private void loadSvgTiles(File folder) {
        tilePanel.removeAll();

        File[] svgFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
        if (svgFiles == null || svgFiles.length == 0) {
            JOptionPane.showMessageDialog(frame, "Keine SVG-Dateien gefunden.");
            svgCanvas.setURI(null);
            tilePanel.revalidate();
            tilePanel.repaint();
            return;
        }

        for (File svgFile : svgFiles) {
            JPanel rowPanel = new JPanel(new BorderLayout(5, 5));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); // Höhe

            JSVGCanvas miniCanvas = new JSVGCanvas();
            miniCanvas.setURI(svgFile.toURI().toString());

            miniCanvas.setPreferredSize(new Dimension(50, 50));
            miniCanvas.setMinimumSize(new Dimension(50, 50));
            miniCanvas.setMaximumSize(new Dimension(50, 50));
            miniCanvas.setBackground(Color.WHITE);

            // Zoom deaktivieren (stabileres Verhalten)
            miniCanvas.setEnableZoomInteractor(false);
            // Cache deaktivieren, falls SVG nicht lädt
            miniCanvas.setDocumentState(org.apache.batik.swing.JSVGCanvas.ALWAYS_DYNAMIC);

            miniCanvas.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showSvg(svgFile);
                }
            });

            JCheckBox checkBox = new JCheckBox();

            rowPanel.add(miniCanvas, BorderLayout.WEST);
            rowPanel.add(checkBox, BorderLayout.EAST);

            tilePanel.add(rowPanel);
            tilePanel.add(Box.createVerticalStrut(5));
        }

        tilePanel.revalidate();
        tilePanel.repaint();

        // Erstes SVG direkt anzeigen
        showSvg(svgFiles[0]);
    }
    



    private void showSvg(File svgFile) {
        try {
            svgCanvas.setURI(svgFile.toURI().toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "SVG konnte nicht geladen werden: " + svgFile.getName());
        }
    }
}
