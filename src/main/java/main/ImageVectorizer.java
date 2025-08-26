package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ImageVectorizer extends JFrame {
    private JTextField folderPathField;
    private JButton browseButton;
    private JButton processButton;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JSpinner simplificationLevelSpinner;
    private JCheckBox previewSvgCheckBox;
    
    // Parameter für Vektorisierung
    private double simplificationTolerance = 1.0;
    
    public ImageVectorizer() {
        setTitle("Schwarz-Transparent Bild Vektorisierer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        folderPathField = new JTextField(20);
        browseButton = new JButton("Durchsuchen...");
        processButton = new JButton("Bilder vektorisieren");
        logArea = new JTextArea();
        logArea.setEditable(false);
        progressBar = new JProgressBar(0, 100);
        statusLabel = new JLabel("Bereit");
        
        // Spinner für Vereinfachungslevel (0.5-5.0)
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1.0, 0.5, 5.0, 0.1);
        simplificationLevelSpinner = new JSpinner(spinnerModel);
        
        // Checkbox für SVG-Vorschau
        previewSvgCheckBox = new JCheckBox("Vorschau nach Vektorisierung öffnen");
        previewSvgCheckBox.setSelected(true);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseFolder();
            }
        });

        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processImages();
            }
        });
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Oberer Bereich für Pfad und Durchsuchen-Button
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.add(new JLabel("Bilderordner:"), BorderLayout.WEST);
        topPanel.add(folderPathField, BorderLayout.CENTER);
        topPanel.add(browseButton, BorderLayout.EAST);

        // Mittlerer Bereich mit den Einstellungen
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        settingsPanel.add(new JLabel("Vereinfachungslevel:"));
        settingsPanel.add(simplificationLevelSpinner);
        settingsPanel.add(Box.createHorizontalStrut(20));
        settingsPanel.add(previewSvgCheckBox);
        settingsPanel.add(Box.createHorizontalStrut(20));
        settingsPanel.add(processButton);

        // Kombiniere Einstellungen und oberen Bereich
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(settingsPanel, BorderLayout.CENTER);
        controlPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

        // Log-Bereich
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        // Status-Bereich
        JPanel statusPanel = new JPanel(new BorderLayout(5, 0));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        // Füge alles zum Hauptpanel hinzu
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Füge das Hauptpanel zum Frame hinzu
        add(mainPanel);
    }

    private void browseFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Bildordner auswählen");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            folderPathField.setText(selectedFolder.getAbsolutePath());
        }
    }

    private void processImages() {
        final String folderPath = folderPathField.getText();
        if (folderPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Bitte einen gültigen Ordnerpfad angeben.", 
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(this, 
                "Der angegebene Pfad ist kein gültiger Ordner.", 
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // UI-Elemente vor der Verarbeitung aktualisieren
        processButton.setEnabled(false);
        browseButton.setEnabled(false);
        folderPathField.setEnabled(false);
        simplificationLevelSpinner.setEnabled(false);
        previewSvgCheckBox.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);
        statusLabel.setText("Vektorisiere Bilder...");

        // Parameter holen
        simplificationTolerance = (Double) simplificationLevelSpinner.getValue();
        final boolean openPreview = previewSvgCheckBox.isSelected();
        
        // Verarbeitung in einem SwingWorker ausführen
        SwingWorker<List<File>, String> worker = new SwingWorker<List<File>, String>() {
            @Override
            protected List<File> doInBackground() throws Exception {
                List<File> results = new ArrayList<>();
                
                File[] files = folder.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                           lowerName.endsWith(".png") || lowerName.endsWith(".bmp") ||
                           lowerName.endsWith(".gif");
                });
                
                if (files == null || files.length == 0) {
                    publish("Keine Bilder im ausgewählten Ordner gefunden.");
                    return results;
                }
                
                publish("Gefundene Bilder: " + files.length);
                
                int processedCount = 0;
                for (File file : files) {
                    try {
                        publish("Vektorisiere: " + file.getName());
                        
                        // Bild laden
                        BufferedImage originalImage = ImageIO.read(file);
                        if (originalImage == null) {
                            publish("  Warnung: Konnte Bild nicht laden: " + file.getName());
                            continue;
                        }
                        
                        // Binarisierung sicherstellen (nur Schwarz und Transparent)
                        BufferedImage binaryImage = ensureBinaryImage(originalImage);
                        
                        // Konturen erkennen und vektorisieren
                        File svgFile = vectorizeImage(binaryImage, file, folder);
                        if (svgFile != null) {
                            results.add(svgFile);
                            publish("  Vektorisiert und gespeichert als: " + svgFile.getName());
                        }
                        
                        processedCount++;
                        
                        // Progressbar aktualisieren
                        int progress = (int)((processedCount / (double)files.length) * 100);
                        setProgress(progress);
                    } catch (Exception e) {
                        publish("  Fehler: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                return results;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    logArea.append(message + "\n");
                    // Scrolle automatisch nach unten
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }
            
            @Override
            protected void done() {
                try {
                    List<File> results = get();
                    progressBar.setValue(100);
                    statusLabel.setText("Fertig: " + results.size() + " Bilder vektorisiert");
                    
                    // Öffne das erste SVG zur Vorschau, wenn gewünscht
                    if (openPreview && !results.isEmpty()) {
                        try {
                            Desktop.getDesktop().open(results.get(0));
                        } catch (IOException e) {
                            publish("Konnte Vorschau nicht öffnen: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    statusLabel.setText("Fehler: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // UI-Elemente nach der Verarbeitung zurücksetzen
                    processButton.setEnabled(true);
                    browseButton.setEnabled(true);
                    folderPathField.setEnabled(true);
                    simplificationLevelSpinner.setEnabled(true);
                    previewSvgCheckBox.setEnabled(true);
                }
            }
        };
        
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
            }
        });
        
        worker.execute();
    }
    
    // Stellt sicher, dass das Bild nur die Farben Schwarz und Transparent enthält
private BufferedImage ensureBinaryImage(BufferedImage original) {
    int width = original.getWidth();
    int height = original.getHeight();
    
    BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    
    // Threshold-Wert ähnlich wie InkScape (0.450)
    double threshold = 0.450; // Wert zwischen 0 und 1
    int thresholdValue = (int)(threshold * 255);
    
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int rgb = original.getRGB(x, y);
            int alpha = (rgb >> 24) & 0xFF;
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            
            // Helligkeit berechnen (vereinfacht)
            int brightness = (red + green + blue) / 3;
            
            // Falls vollständig transparent, beibehalten
            if (alpha < 10) {
                binaryImage.setRGB(x, y, 0); // Vollständig transparent
            } else if (brightness < thresholdValue) {
                // Dunkler als der Schwellenwert -> Schwarz
                binaryImage.setRGB(x, y, 0xFF000000); // Opakes Schwarz
            } else {
                // Heller als der Schwellenwert -> Transparent
                binaryImage.setRGB(x, y, 0); // Transparent
            }
        }
    }
    
    return binaryImage;
}    
    // Hauptmethode zur Vektorisierung des Bildes
    private File vectorizeImage(BufferedImage image, File sourceFile, File folder) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 1. Konturen finden (Markierung von schwarzen Pixeln)
        boolean[][] visited = new boolean[width][height];
        List<List<Point>> allContours = new ArrayList<>();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBlackPixel(image, x, y) && !visited[x][y]) {
                    // Kontur verfolgen
                    List<Point> contour = traceContour(image, x, y, visited);
                    if (contour.size() > 2) { // Ignoriere sehr kleine Konturen
                        allContours.add(contour);
                    }
                }
            }
        }
        
        // 2. Konturen vereinfachen mit Douglas-Peucker-Algorithmus
        List<List<Point>> simplifiedContours = new ArrayList<>();
        for (List<Point> contour : allContours) {
            List<Point> simplified = simplifyPath(contour, simplificationTolerance);
            simplifiedContours.add(simplified);
        }
        
        // 3. SVG-Datei erstellen
        String fileName = sourceFile.getName();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        File svgFile = new File(folder, baseName + "_vector.svg");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(svgFile))) {
            // SVG-Header
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            writer.printf("<svg width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" xmlns=\"http://www.w3.org/2000/svg\">\n", 
                          width, height, width, height);
            
            // Hintergrund transparent lassen
            
            // Konturen als Pfade hinzufügen
            for (List<Point> contour : simplifiedContours) {
                if (contour.isEmpty()) continue;
                
                StringBuilder pathData = new StringBuilder();
                Point first = contour.get(0);
                pathData.append("M ").append(first.x).append(",").append(first.y).append(" ");
                
                for (int i = 1; i < contour.size(); i++) {
                    Point p = contour.get(i);
                    pathData.append("L ").append(p.x).append(",").append(p.y).append(" ");
                }
                
                // Pfad schließen
                pathData.append("Z");
                
                writer.printf("<path d=\"%s\" fill=\"black\" />\n", pathData.toString());
            }
            
            writer.println("</svg>");
        }
        
        return svgFile;
    }
    
    // Überprüft, ob ein Pixel schwarz ist (nicht transparent)
    private boolean isBlackPixel(BufferedImage image, int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return false;
        }
        
        int rgb = image.getRGB(x, y);
        int alpha = (rgb >> 24) & 0xFF;
        return alpha > 128; // Nicht-transparente Pixel als "schwarz" betrachten
    }
    
    // Verfolgt eine Kontur und liefert die Liste der Konturpunkte zurück
    private List<Point> traceContour(BufferedImage image, int startX, int startY, boolean[][] visited) {
        List<Point> contour = new ArrayList<>();
        
        // Richtungen für die 8-Nachbarschaft
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};
        
        // Startpunkt hinzufügen
        contour.add(new Point(startX, startY));
        visited[startX][startY] = true;
        
        int x = startX;
        int y = startY;
        
        // Verfolge die Kontur, bis wir entweder zurück am Start sind oder keine weiteren Punkte finden
        boolean foundNext = true;
        while (foundNext) {
            foundNext = false;
            
            // Versuche alle Nachbarn
            for (int dir = 0; dir < 8; dir++) {
                int nx = x + dx[dir];
                int ny = y + dy[dir];
                
                if (nx >= 0 && ny >= 0 && nx < image.getWidth() && ny < image.getHeight() && 
                    isBlackPixel(image, nx, ny) && !visited[nx][ny]) {
                    // Nächsten Punkt hinzufügen
                    contour.add(new Point(nx, ny));
                    visited[nx][ny] = true;
                    x = nx;
                    y = ny;
                    foundNext = true;
                    break;
                }
            }
        }
        
        return contour;
    }
    
    // Vereinfacht einen Pfad mit dem Douglas-Peucker-Algorithmus
    private List<Point> simplifyPath(List<Point> points, double tolerance) {
        if (points.size() <= 2) {
            return new ArrayList<>(points);
        }
        
        // Finde den Punkt mit dem maximalen Abstand
        double maxDistance = 0;
        int index = 0;
        
        Point first = points.get(0);
        Point last = points.get(points.size() - 1);
        
        for (int i = 1; i < points.size() - 1; i++) {
            double distance = perpendicularDistance(points.get(i), first, last);
            if (distance > maxDistance) {
                maxDistance = distance;
                index = i;
            }
        }
        
        // Wenn die maximale Distanz größer als die Toleranz ist, rekursiv vereinfachen
        if (maxDistance > tolerance) {
            // Teile die Linie und vereinfache rekursiv
            List<Point> firstLine = points.subList(0, index + 1);
            List<Point> lastLine = points.subList(index, points.size());
            
            List<Point> simplifiedFirstPart = simplifyPath(firstLine, tolerance);
            List<Point> simplifiedLastPart = simplifyPath(lastLine, tolerance);
            
            // Kombiniere die Ergebnisse (ohne doppelte Punkte)
            List<Point> result = new ArrayList<>(simplifiedFirstPart);
            result.addAll(simplifiedLastPart.subList(1, simplifiedLastPart.size()));
            return result;
        } else {
            // Unter der Toleranz - behalte nur die Endpunkte
            List<Point> result = new ArrayList<>();
            result.add(first);
            result.add(last);
            return result;
        }
    }
    
    // Berechnet den senkrechten Abstand eines Punktes von einer Linie
    private double perpendicularDistance(Point point, Point lineStart, Point lineEnd) {
        double dx = lineEnd.x - lineStart.x;
        double dy = lineEnd.y - lineStart.y;
        
        // Wenn die Linie ein Punkt ist, berechne einfachen Abstand
        if (dx == 0 && dy == 0) {
            return Math.sqrt(Math.pow(point.x - lineStart.x, 2) + Math.pow(point.y - lineStart.y, 2));
        }
        
        // Berechne den Abstand vom Punkt zur Linie
        double lineLengthSquared = dx * dx + dy * dy;
        double u = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lineLengthSquared;
        
        if (u < 0) {
            // Punkt ist näher am Startpunkt
            return Math.sqrt(Math.pow(point.x - lineStart.x, 2) + Math.pow(point.y - lineStart.y, 2));
        } else if (u > 1) {
            // Punkt ist näher am Endpunkt
            return Math.sqrt(Math.pow(point.x - lineEnd.x, 2) + Math.pow(point.y - lineEnd.y, 2));
        } else {
            // Punkt ist innerhalb des Liniensegments
            double px = lineStart.x + u * dx;
            double py = lineStart.y + u * dy;
            return Math.sqrt(Math.pow(point.x - px, 2) + Math.pow(point.y - py, 2));
        }
    }

    public static void main(String[] args) {
        try {
            // Look and Feel des Systems verwenden
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ImageVectorizer().setVisible(true);
            }
        });
    }
}
