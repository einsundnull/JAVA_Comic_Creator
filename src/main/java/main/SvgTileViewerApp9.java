package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.Arrays;

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

public class SvgTileViewerApp9 {

    private JFrame frame;
    private JPanel tilePanel;
    private JSVGCanvas svgCanvas;
    private File currentFolder;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SvgTileViewerApp9().createAndShowUI());
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
        
        // Verbesserte Scroll-Performance
        tileScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tileScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

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
        
        // Sortiere Dateien alphabetisch für bessere Benutzerfreundlichkeit
        Arrays.sort(svgFiles);

        for (File svgFile : svgFiles) {
            JPanel rowPanel = createSvgRow(svgFile);
            tilePanel.add(rowPanel);
            tilePanel.add(Box.createVerticalStrut(5));
        }

        // Komplettes Neuzeichnen erzwingen
        tilePanel.revalidate();
        frame.repaint();

        // Erstes SVG direkt anzeigen
        showSvg(svgFiles[0]);
    }
    
    private JPanel createSvgRow(File svgFile) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Links ausgerichtet mit Abstand
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); // Höhe begrenzen
        
        // SVG-Miniaturansicht
        JSVGCanvas miniCanvas = new JSVGCanvas();
        miniCanvas.setPreferredSize(new Dimension(50, 50));
        miniCanvas.setMinimumSize(new Dimension(50, 50));
        miniCanvas.setMaximumSize(new Dimension(50, 50));
        miniCanvas.setBackground(Color.WHITE);
        
        // Performance-Optimierungen
        miniCanvas.setEnableZoomInteractor(false);
        miniCanvas.setEnablePanInteractor(false);
        miniCanvas.setEnableRotateInteractor(false);
        miniCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        
        // URI setzen
        try {
            miniCanvas.setURI(svgFile.toURI().toString());
        } catch (Exception e) {
            miniCanvas.setBackground(Color.LIGHT_GRAY);
        }

        miniCanvas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showSvg(svgFile);
            }
        });

        // Checkbox direkt nach dem Bild platzieren
        JCheckBox checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        
        // Dateiname hinzufügen
        javax.swing.JLabel fileNameLabel = new javax.swing.JLabel(svgFile.getName());
        
        // Komponenten der Reihe nach hinzufügen: Miniaturansicht, Checkbox, Dateiname
        rowPanel.add(miniCanvas);
        rowPanel.add(checkBox);
        rowPanel.add(fileNameLabel);
        
        return rowPanel;
    }

    private void showSvg(File svgFile) {
        try {
            // Setze Cursor auf Warten
            frame.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            
            svgCanvas.setURI(svgFile.toURI().toString());
            
            // Cursor zurücksetzen
            frame.setCursor(java.awt.Cursor.getDefaultCursor());
        } catch (Exception ex) {
            frame.setCursor(java.awt.Cursor.getDefaultCursor());
            JOptionPane.showMessageDialog(frame, "SVG konnte nicht geladen werden: " + svgFile.getName());
        }
    }
}