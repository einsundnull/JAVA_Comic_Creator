package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class SvgTileViewerApp4 {

    private JFrame frame;
    private JPanel tilePanel;
    private JSVGCanvas svgCanvas;
    private File currentFolder;
    private Map<String, WeakReference<BufferedImage>> imageCache = new HashMap<>();
    private boolean isLoadingFolder = false;

    public static void main(String[] args) {
        // Maximalen Java-Heap-Speicher erhöhen
        // Diese Zeile ist nur ein Hinweis - Sie müssen die VM-Argumente ändern
        // java -Xmx512m -jar YourApplication.jar
        SwingUtilities.invokeLater(() -> new SvgTileViewerApp4().createAndShowUI());
    }

    private void createAndShowUI() {
        frame = new JFrame("SVG Tile Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        JButton chooseFolderBtn = new JButton("Verzeichnis auswählen");
        chooseFolderBtn.addActionListener(e -> chooseFolder());

        tilePanel = new JPanel();
        tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));

        JScrollPane tileScrollPane = new JScrollPane(tilePanel);
        tileScrollPane.setPreferredSize(new Dimension(300, 0));
        tileScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tileScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Lazy-Loading beim Scrollen
        tileScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!isLoadingFolder) {
                    frame.repaint();
                }
            }
        });

        svgCanvas = new JSVGCanvas();
        svgCanvas.setBackground(Color.WHITE);

        frame.add(chooseFolderBtn, BorderLayout.NORTH);
        frame.add(tileScrollPane, BorderLayout.WEST);
        frame.add(svgCanvas, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Regelmäßige Speicherbereinigung
        startMemoryCleanupTimer();
    }

    private void startMemoryCleanupTimer() {
        // Alle 60 Sekunden den Garbage Collector aufrufen
        javax.swing.Timer cleanupTimer = new javax.swing.Timer(60000, e -> {
            System.gc();
        });
        cleanupTimer.start();
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser(LastUsedDirectory.load());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            currentFolder = chooser.getSelectedFile();
            LastUsedDirectory.save(currentFolder);
            
            // Cache leeren beim Ordnerwechsel
            imageCache.clear();
            System.gc();
            
            new Thread(() -> {
                isLoadingFolder = true;
                loadSvgTiles(currentFolder);
                isLoadingFolder = false;
            }).start();
        }
    }

    private void loadSvgTiles(File folder) {
        SwingUtilities.invokeLater(() -> {
            tilePanel.removeAll();
            tilePanel.revalidate();
            tilePanel.repaint();
        });

        File[] svgFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
        if (svgFiles == null || svgFiles.length == 0) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame, "Keine SVG-Dateien gefunden.");
                svgCanvas.setURI(null);
            });
            return;
        }
        
        // Sortiere Dateien alphabetisch
        Arrays.sort(svgFiles);

        // Batch-Verarbeitung verwenden, um UI-Blockierung zu vermeiden
        int batchSize = 5;
        for (int i = 0; i < svgFiles.length; i += batchSize) {
            final int startIdx = i;
            final int endIdx = Math.min(i + batchSize, svgFiles.length);
            
            SwingUtilities.invokeLater(() -> {
                for (int j = startIdx; j < endIdx; j++) {
                    JPanel rowPanel = createSvgRow(svgFiles[j]);
                    tilePanel.add(rowPanel);
                    tilePanel.add(Box.createVerticalStrut(5));
                }
                tilePanel.revalidate();
                tilePanel.repaint();
            });
            
            // Kurz warten, damit die UI nicht blockiert wird
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Erstes SVG anzeigen, wenn Laden abgeschlossen
        SwingUtilities.invokeLater(() -> {
            if (svgFiles.length > 0) {
                showSvg(svgFiles[0]);
            }
        });
    }
    
    private JPanel createSvgRow(File svgFile) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        // JLabel mit Icon statt JSVGCanvas für Vorschaubilder
        JLabel thumbnailLabel = new JLabel();
        thumbnailLabel.setPreferredSize(new Dimension(50, 50));
        thumbnailLabel.setBackground(Color.WHITE);
        thumbnailLabel.setOpaque(true);
        
        // In einem separaten Thread laden, um UI-Blockierung zu vermeiden
        new Thread(() -> {
            BufferedImage thumbnail = getSvgThumbnail(svgFile);
            if (thumbnail != null) {
                SwingUtilities.invokeLater(() -> {
                    thumbnailLabel.setIcon(new ImageIcon(thumbnail));
                });
            }
        }).start();

        thumbnailLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showSvg(svgFile);
            }
        });

        JCheckBox checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        
        JLabel fileNameLabel = new JLabel(svgFile.getName());
        
        rowPanel.add(thumbnailLabel);
        rowPanel.add(checkBox);
        rowPanel.add(fileNameLabel);
        
        return rowPanel;
    }
    
    private BufferedImage getSvgThumbnail(File svgFile) {
        String cacheKey = svgFile.getAbsolutePath();
        
        // Versuchen, aus dem Cache zu laden
        if (imageCache.containsKey(cacheKey)) {
            WeakReference<BufferedImage> ref = imageCache.get(cacheKey);
            BufferedImage cached = ref.get();
            if (cached != null) {
                return cached;
            }
            // Cache-Eintrag entfernen, wenn Bild nicht mehr im Speicher ist
            imageCache.remove(cacheKey);
        }
        
        try {
            // SVG in kleines PNG für die Vorschau umwandeln
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 50f);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 50f);
            
            // SVG Datei als Input
            TranscoderInput input = new TranscoderInput(svgFile.toURI().toString());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);
            
            // Transcode SVG zu PNG
            transcoder.transcode(input, output);
            
            // PNG aus Stream lesen
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            BufferedImage thumbnail = ImageIO.read(inputStream);
            
            // In Cache speichern
            imageCache.put(cacheKey, new WeakReference<>(thumbnail));
            
            return thumbnail;
        } catch (TranscoderException | java.io.IOException e) {
            System.err.println("Fehler beim Erstellen der Vorschau für " + svgFile.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void showSvg(File svgFile) {
        try {
            frame.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            
            // Altes SVG freigeben, bevor neues geladen wird
            svgCanvas.setSVGDocument(null);
            System.gc();
            
            // Kurze Pause einfügen
            Thread.sleep(50);
            
            // Neues SVG laden
            svgCanvas.setURI(svgFile.toURI().toString());
            
            frame.setCursor(java.awt.Cursor.getDefaultCursor());
        } catch (Exception ex) {
            frame.setCursor(java.awt.Cursor.getDefaultCursor());
            JOptionPane.showMessageDialog(frame, "SVG konnte nicht geladen werden: " + svgFile.getName() + "\n" + ex.getMessage());
        }
    }
}