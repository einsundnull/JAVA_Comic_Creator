package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
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

public class SvgTileViewerApp {

    private boolean isLoadingFolder = false;
    private File currentFolder;
    private SVGDataManager dataManager = new SVGDataManager();

    private JSVGCanvas svgCanvas;
    private JFrame frame;
    private JPanel tilePanel;
    private JPanel selectedPanel;
    private JPanel rightPanel;
    private JPanel scenePanel = new JPanel(null);
    private JScrollPane tileScrollPane;
    private JScrollPane selectedScrollPane;
    private JScrollPane centerScrollPane;

    private Map<String, WeakReference<BufferedImage>> imageCache = new HashMap<>();
    private Map<JCheckBox, File> checkboxFileMap = new HashMap<>();
    private Map<File, JPanel> fileToTileMap = new HashMap<>();
    private Map<File, JCheckBox> fileCheckboxMap = new HashMap<>();
    private Map<File, JPanel> selectedFilePanels = new HashMap<>();
    
    // Hauptsammlung aller Tiles, indexiert nach Dateinamen
    private final Map<String, CustomImageTile> allTiles = new HashMap<>();
    
    // Neue separate Liste für ausgewählte Tiles
    private final List<CustomImageTile> selectedTiles = new ArrayList<>();

    private LinkedList<LinkedList<String>> svgData = new LinkedList<>();
    private LinkedList<LinkedList<String>> svgs = new LinkedList<>();
    private final List<File> selectedFiles = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SvgTileViewerApp().createAndShowUI());
    }

    private void createAndShowUI() {
        currentFolder = LastUsedDirectory.load();
        if (currentFolder == null || !currentFolder.isDirectory())
            currentFolder = new File(System.getProperty("user.home"));

        frame = new JFrame("SVG Tile Viewer");
  
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        JButton chooseFolderBtn = new JButton("Verzeichnis auswählen");
        chooseFolderBtn.addActionListener(e -> chooseFolder());
        frame.add(chooseFolderBtn, BorderLayout.NORTH);

        // Linkes Panel
        tilePanel = new JPanel();
        tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
        tilePanel.setBackground(Color.WHITE);
        tileScrollPane = new JScrollPane(tilePanel);
        tileScrollPane.setPreferredSize(new Dimension(300, 0));
        tileScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(tileScrollPane, BorderLayout.WEST);

        // Zentrales Panel für CustomImageTiles
        scenePanel = new JPanel(null); // Null-Layout für freie Positionierung der Tiles
        scenePanel.setPreferredSize(new Dimension(2000, 2000));
        scenePanel.setBackground(Color.WHITE);
        scenePanel.setFocusable(true);
        
        scenePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("ScenePanel clicked");
                scenePanel.requestFocusInWindow();
                selectAllTiles(false);
            }
            
			@Override
			public void mouseEntered(MouseEvent e) {
				scenePanel.requestFocusInWindow();
				scenePanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2)); // oder andere Farbe/Stärke
			}

			@Override
			public void mouseExited(MouseEvent e) {
				scenePanel.setBorder(null);
			}
        });
        
        // Verbesserte Mausrad-Behandlung für das Skalieren der ausgewählten Tiles
        scenePanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    double scaleFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                    // Direkte Nutzung der Liste der ausgewählten Tiles statt Durchlaufen aller Tiles
                    for (CustomImageTile tile : selectedTiles) {
                        tile.scaleSVG(scaleFactor);
                    }
                    scenePanel.repaint();
                }
            }
        });

        // Verbesserte Tastatursteuerung
        scenePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
                    selectAllTiles(true);
                    System.out.println("Select All: " + selectedTiles.size() + " Tiles");
                    scenePanel.repaint();
                }
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    selectAllTiles(false);
                    System.out.println("Select None");
                    scenePanel.repaint();
                }
                // Neue Tastenkombination zum Skalieren aller ausgewählten Tiles
                else if (e.getKeyCode() == KeyEvent.VK_PLUS && e.isControlDown()) {
                    scaleSelectedTiles(1.1);
                }
                else if (e.getKeyCode() == KeyEvent.VK_MINUS && e.isControlDown()) {
                    scaleSelectedTiles(0.9);
                }
            }
        });

        centerScrollPane = new JScrollPane(scenePanel);
        centerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        frame.add(centerScrollPane, BorderLayout.CENTER);

        // Rechtes Panel
        selectedPanel = new JPanel();
        selectedPanel.setLayout(new BoxLayout(selectedPanel, BoxLayout.Y_AXIS));
        selectedPanel.setBackground(Color.WHITE);
        selectedPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        selectedScrollPane = new JScrollPane(selectedPanel);
        selectedScrollPane.setPreferredSize(new Dimension(300, 0));
        selectedScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(selectedScrollPane, BorderLayout.CENTER);
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        frame.add(rightPanel, BorderLayout.EAST);

        // Toggle Buttons
        JButton toggleLeft = new JButton("⮜");
        toggleLeft.addActionListener(e -> {
            tileScrollPane.setVisible(!tileScrollPane.isVisible());
            toggleLeft.setText(tileScrollPane.isVisible() ? "⮜" : "⮞");
        });
        JButton toggleRight = new JButton("⮞");
        toggleRight.addActionListener(e -> {
            rightPanel.setVisible(!rightPanel.isVisible());
            toggleRight.setText(rightPanel.isVisible() ? "⮞" : "⮜");
        });
        
        // Hinzufügen von Steuerungstasten-Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton selectAllBtn = new JButton("Alle auswählen");
        selectAllBtn.addActionListener(e -> {
            selectAllTiles(true);
            scenePanel.repaint();
        });
        
        JButton selectNoneBtn = new JButton("Keine auswählen");
        selectNoneBtn.addActionListener(e -> {
            selectAllTiles(false);
            scenePanel.repaint();
        });
        
        JButton scaleUpBtn = new JButton("Vergrößern");
        scaleUpBtn.addActionListener(e -> scaleSelectedTiles(1.1));
        
        JButton scaleDownBtn = new JButton("Verkleinern");
        scaleDownBtn.addActionListener(e -> scaleSelectedTiles(0.9));
        
        controlPanel.add(selectAllBtn);
        controlPanel.add(selectNoneBtn);
        controlPanel.add(scaleUpBtn);
        controlPanel.add(scaleDownBtn);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        togglePanel.add(toggleLeft);
        togglePanel.add(toggleRight);
        bottomPanel.add(togglePanel, BorderLayout.SOUTH);
        
        frame.add(bottomPanel, BorderLayout.SOUTH);

        loadFolderFiles(currentFolder);

        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);  // Maximiert das Fenster
//        frame.setUndecorated(true);         
        frame.setVisible(true);
        scenePanel.requestFocusInWindow();
        centerScrollPane.setFocusable(false);

        startMemoryCleanupTimer();
    }

    // Helper-Methode zum Auswählen/Abwählen aller Tiles
    private void selectAllTiles(boolean select) {
        selectedTiles.clear();
        
        if (select) {
            for (CustomImageTile tile : allTiles.values()) {
                tile.setSelected(true);
                selectedTiles.add(tile);
            }
        } else {
            for (CustomImageTile tile : allTiles.values()) {
                tile.setSelected(false);
            }
        }
    }
    
    // Helper-Methode zum Skalieren aller ausgewählten Tiles
    private void scaleSelectedTiles(double scaleFactor) {
        for (CustomImageTile tile : selectedTiles) {
            tile.scaleSVG(scaleFactor);
        }
        scenePanel.repaint();
    }

    private void loadFolderFiles(File folder) {
        SwingUtilities.invokeLater(() -> {
            tilePanel.removeAll();
            selectedPanel.removeAll();
            fileToTileMap.clear();
            selectedFilePanels.clear();
            fileCheckboxMap.clear();
            
            // Bestehende Sammlungen zurücksetzen
            allTiles.clear();
            selectedTiles.clear();

            // SVG-Daten laden oder erstellen
            SVGDataManager dataManager = new SVGDataManager();
            svgData = dataManager.getSVGData(folder);
            svgs.clear();
            svgs.addAll(svgData);

            // Alle SVG-Dateien prüfen
            File[] files = folder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".svg"));
            if (files == null || files.length == 0) {
                JOptionPane.showMessageDialog(frame, "Keine SVG-Dateien gefunden.");
                if (svgCanvas != null) {
                    svgCanvas.setURI(null);
                }
                return;
            }
            Arrays.sort(files);

            // Tiles erzeugen und in Panel einfügen
            for (LinkedList<String> data : svgData) {
                CustomImageTile tile = new CustomImageTile(data, folder);
//                tile.setTileUpdateListener(updatedTile -> {
//                    // z. B. Liste aktualisieren oder Flag setzen
//                	if(selectedTiles.contains(tile)) {
//                		tile.setSelected(!tile.isSelected());
//                	}
////                    System.out.println("Tile updated: " + updatedTile.getFilename());
//                });
                allTiles.put(tile.getFilename(), tile);
                
                // Füge Listener für Auswahlstatus-Änderungen hinzu
//                tile.addSelectionListener(isSelected -> {
//                    if (isSelected) {
//                        if (!selectedTiles.contains(tile)) {
//                            selectedTiles.add(tile);
//                        }
//                    } else {
//                        selectedTiles.remove(tile);
//                    }
//                });
            }

            // Linke Dateiliste mit Checkboxen
            for (File file : files) {
                JPanel leftRow = createThumbnailRow(file, true, true);
                tilePanel.add(leftRow);
                fileToTileMap.put(file, leftRow);

                JCheckBox cb = findCheckboxInPanel(leftRow);
                if (cb != null) {
                    fileCheckboxMap.put(file, cb);
                    cb.addActionListener(e -> {
                        if (cb.isSelected()) {
                            addFileToSelectedPanel(file);
                            setTileVisible(file, true);
                            addSVGThatYouClickedOn(file, "Clicked CheckBox");
                        } else {
                            removeFileFromSelectedPanel(file);
                            setTileVisible(file, false);
                        }
                    });
                }
            }

            tilePanel.revalidate();
            tilePanel.repaint();
            selectedPanel.revalidate();
            selectedPanel.repaint();
        });
    }

    private JPanel createThumbnailRow(File file, boolean withCheckbox, boolean withName) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel thumb = new JLabel();
        thumb.setPreferredSize(new Dimension(50, 50));
        thumb.setOpaque(true);
        thumb.setBackground(Color.WHITE);
        row.add(thumb);

        // Thumbnail laden
        new Thread(() -> {
            BufferedImage img = getSvgThumbnail(file);
            if (img != null)
                SwingUtilities.invokeLater(() -> thumb.setIcon(new ImageIcon(img)));
        }).start();

        thumb.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                addSVGThatYouClickedOn(file, "Clicked thumb");
            }
        });

        if (withCheckbox) {
            JCheckBox cb = new JCheckBox();
            cb.setOpaque(false);
            row.add(cb);
        }
        if (withName) {
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
                            imageCache.remove(file.getAbsolutePath());
                            
                            // Aktualisiere Tile-Mapping nach Umbenennung
                            CustomImageTile tile = allTiles.remove(file.getName());
                            if (tile != null) {
                                tile.setFilename(newName);
                                allTiles.put(newName, tile);
                            }
                            
                            loadFolderFiles(currentFolder); // Refresh alle
                        } else
                            JOptionPane.showMessageDialog(frame, "Umbenennen fehlgeschlagen.");
                    }
                }
            });
            row.add(nameLabel);
        }

        return row;
    }

    private void setTileVisible(File file, boolean visible) {
        CustomImageTile tile = allTiles.get(file.getName());
        if (tile != null) {
            tile.getPanel().setVisible(visible);
            
            // Verwalte die ausgewählten Tiles
            if (visible) {
                if (!selectedTiles.contains(tile)) {
                    selectedTiles.add(tile);
                }
            } else {
                selectedTiles.remove(tile);
                tile.setSelected(false);
            }
        }
    }

    private void addSVGThatYouClickedOn(File file, String id) {
        CustomImageTile tile = allTiles.get(file.getName());
        if (tile != null) {
            // Bringen Sie das Panel des Tiles in den Vordergrund
            scenePanel.add(tile.getPanel());
            scenePanel.setComponentZOrder(tile.getPanel(), 0);
            
            // Ändere die Auswahl des Tiles
            boolean isSelected = !tile.isSelected();
            tile.setSelected(isSelected);
            
            // Aktualisiere die Liste der ausgewählten Tiles
            if (isSelected) {
                if (!selectedTiles.contains(tile)) {
                    selectedTiles.add(tile);
                }
            } else {
                selectedTiles.remove(tile);
            }
            
            scenePanel.revalidate();
            scenePanel.repaint();
        } else {
            System.out.println(id + ": Tile not found for " + file.getName());
        }
    }

    private JCheckBox findCheckboxInPanel(JPanel p) {
        for (Component c : p.getComponents())
            if (c instanceof JCheckBox)
                return (JCheckBox) c;
        return null;
    }

    private void addFileToSelectedPanel(File file) {
        if (selectedFilePanels.containsKey(file))
            return;

        JPanel rightRow = createThumbnailRow(file, false, false);

        rightRow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                scrollToThumbnailInLeftPanel(file);
            }
        });

        selectedFilePanels.put(file, rightRow);
        selectedPanel.add(rightRow);
        selectedPanel.revalidate();
        selectedPanel.repaint();
    }

    private void scrollToThumbnailInLeftPanel(File file) {
        JPanel leftThumbnail = fileToTileMap.get(file);
        if (leftThumbnail != null) {
            SwingUtilities.invokeLater(() -> {
                Rectangle rect = leftThumbnail.getBounds();
                rect.y = leftThumbnail.getY();
                tileScrollPane.getViewport().scrollRectToVisible(rect);
            });
        }
    }

    private void removeFileFromSelectedPanel(File file) {
        JPanel panel = selectedFilePanels.remove(file);
        if (panel != null) {
            selectedPanel.remove(panel);
            selectedPanel.revalidate();
            selectedPanel.repaint();
        }
    }

    private void showSvg(File file) {
        if (svgCanvas != null) {
            svgCanvas.setURI(file.toURI().toString());
        }
    }

    private void startMemoryCleanupTimer() {
        new javax.swing.Timer(60000, e -> System.gc()).start();
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
                loadFolderFiles(currentFolder);
                isLoadingFolder = false;
            }).start();
        }
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

    // Hilfsmethode für Debugging
    private void printSelectedTiles() {
        System.out.println("Ausgewählte Tiles: " + selectedTiles.size());
        for (CustomImageTile tile : selectedTiles) {
            System.out.println(" - " + tile.getFilename());
        }
    }
}