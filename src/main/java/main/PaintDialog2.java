package main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ein Dialog zum Anzeigen und Bearbeiten von Bilddateien ähnlich wie in MS Paint.
 */
public class PaintDialog2 extends JDialog {
    private BufferedImage image;
    private DrawPanel drawPanel;
    private Color currentColor = Color.BLACK;
    private int strokeSize = 2;
    private Tool currentTool = Tool.PENCIL;
    private List<Shape> shapes = new ArrayList<>();
    private Point startPoint;
    private Shape currentShape;
    private BufferedImage originalImage;

    // Enum zur Verwaltung der Werkzeuge
    public enum Tool {
        PENCIL, LINE, RECTANGLE, OVAL, ERASER
    }

    // Abstrakte Shape-Klasse für Formen
    abstract static class Shape {
        Color color;
        int stroke;
        abstract void draw(Graphics2D g2d);
    }

    // Freihandzeichnung
    static class PencilShape extends Shape {
        List<Point> points = new ArrayList<>();

        @Override
        void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);
                g2d.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
            }
        }
    }

    // Linien
    static class LineShape extends Shape {
        Point start, end;

        public LineShape(Point start, Point end) {
            this.start = start;
            this.end = end;
        }

        @Override
        void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(new Line2D.Double(start.x, start.y, end.x, end.y));
        }
    }

    // Rechtecke
    static class RectangleShape extends Shape {
        Point start, end;

        public RectangleShape(Point start, Point end) {
            this.start = start;
            this.end = end;
        }

        @Override
        void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(stroke));
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int width = Math.abs(start.x - end.x);
            int height = Math.abs(start.y - end.y);
            g2d.drawRect(x, y, width, height);
        }
    }

    // Ovale
    static class OvalShape extends Shape {
        Point start, end;

        public OvalShape(Point start, Point end) {
            this.start = start;
            this.end = end;
        }

        @Override
        void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(stroke));
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int width = Math.abs(start.x - end.x);
            int height = Math.abs(start.y - end.y);
            g2d.drawOval(x, y, width, height);
        }
    }

    /**
     * Konstruktor für den PaintDialog.
     * 
     * @param owner Der Parent-Frame
     * @param imageFile Die zu bearbeitende Bilddatei
     */
    public PaintDialog2(Frame owner, File imageFile) {
        super(owner, "Paint Dialog", true);
        try {
            this.image = ImageIO.read(imageFile);
            // Backup des Originalbildes erstellen
            this.originalImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            Graphics g = originalImage.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            
            initComponents();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Fehler beim Laden des Bildes: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Panel zum Zeichnen
        drawPanel = new DrawPanel();
        JScrollPane scrollPane = new JScrollPane(drawPanel);
        add(scrollPane, BorderLayout.CENTER);
        
        // Toolbar für Werkzeuge
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);
        
        // Statusleiste
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
        
        // Standardwerte setzen
        setPreferredSize(new Dimension(800, 600));
        pack();
        setLocationRelativeTo(getOwner());
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // Werkzeuge
        JButton pencilButton = new JButton("Stift");
        pencilButton.addActionListener(e -> currentTool = Tool.PENCIL);
        toolBar.add(pencilButton);
        
        JButton lineButton = new JButton("Linie");
        lineButton.addActionListener(e -> currentTool = Tool.LINE);
        toolBar.add(lineButton);
        
        JButton rectButton = new JButton("Rechteck");
        rectButton.addActionListener(e -> currentTool = Tool.RECTANGLE);
        toolBar.add(rectButton);
        
        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(e -> currentTool = Tool.OVAL);
        toolBar.add(ovalButton);
        
        JButton eraserButton = new JButton("Radierer");
        eraserButton.addActionListener(e -> currentTool = Tool.ERASER);
        toolBar.add(eraserButton);
        
        toolBar.addSeparator();
        
        // Farbe wählen
        JButton colorButton = new JButton("Farbe");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Farbe wählen", currentColor);
            if (newColor != null) {
                currentColor = newColor;
                colorButton.setBackground(currentColor);
            }
        });
        colorButton.setBackground(currentColor);
        toolBar.add(colorButton);
        
        // Strichstärke
        JLabel strokeLabel = new JLabel("Strichstärke: ");
        toolBar.add(strokeLabel);
        
        String[] strokeSizes = {"1", "2", "3", "5", "8", "10", "15"};
        JComboBox<String> strokeCombo = new JComboBox<>(strokeSizes);
        strokeCombo.setSelectedIndex(1); // 2 als Standard
        strokeCombo.addActionListener(e -> {
            strokeSize = Integer.parseInt((String) strokeCombo.getSelectedItem());
        });
        toolBar.add(strokeCombo);
        
        toolBar.addSeparator();
        
        // Speichern
        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(e -> saveImage());
        toolBar.add(saveButton);
        
        // Zurücksetzen
        JButton resetButton = new JButton("Zurücksetzen");
        resetButton.addActionListener(e -> resetImage());
        toolBar.add(resetButton);
        
        return toolBar;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("Bereit");
        statusBar.add(statusLabel);
        return statusBar;
    }

    private void saveImage() {
        try {
            // Aktuelles Bild erstellen
            BufferedImage finalImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            Graphics2D g2d = finalImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            for (Shape shape : shapes) {
                shape.draw(g2d);
            }
            g2d.dispose();
            
            // Speicherdialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Bild speichern");
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String name = file.getName();
                String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
                
                // Standarderweiterung .png wenn keine angegeben
                if (!name.contains(".")) {
                    file = new File(file.getAbsolutePath() + ".png");
                    ext = "png";
                }
                
                ImageIO.write(finalImage, ext, file);
                JOptionPane.showMessageDialog(this, "Bild erfolgreich gespeichert!");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetImage() {
        // Auf Originalbild zurücksetzen
        Graphics g = image.getGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();
        shapes.clear();
        drawPanel.repaint();
    }

    /**
     * Panel zum Zeichnen und Bearbeiten des Bildes
     */
    class DrawPanel extends JPanel {
        public DrawPanel() {
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                    
                    switch (currentTool) {
                        case PENCIL:
                        case ERASER:
                            PencilShape pencil = new PencilShape();
                            pencil.color = currentTool == Tool.ERASER ? Color.WHITE : currentColor;
                            pencil.stroke = strokeSize;
                            pencil.points.add(startPoint);
                            currentShape = pencil;
                            shapes.add(pencil);
                            break;
                        case LINE:
                            LineShape line = new LineShape(startPoint, startPoint);
                            line.color = currentColor;
                            line.stroke = strokeSize;
                            currentShape = line;
                            shapes.add(line);
                            break;
                        case RECTANGLE:
                            RectangleShape rect = new RectangleShape(startPoint, startPoint);
                            rect.color = currentColor;
                            rect.stroke = strokeSize;
                            currentShape = rect;
                            shapes.add(rect);
                            break;
                        case OVAL:
                            OvalShape oval = new OvalShape(startPoint, startPoint);
                            oval.color = currentColor;
                            oval.stroke = strokeSize;
                            currentShape = oval;
                            shapes.add(oval);
                            break;
                    }
                    repaint();
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    Point endPoint = e.getPoint();
                    
                    switch (currentTool) {
                        case LINE:
                            ((LineShape) currentShape).end = endPoint;
                            break;
                        case RECTANGLE:
                            ((RectangleShape) currentShape).end = endPoint;
                            break;
                        case OVAL:
                            ((OvalShape) currentShape).end = endPoint;
                            break;
                    }
                    
                    repaint();
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    Point currentPoint = e.getPoint();
                    
                    switch (currentTool) {
                        case PENCIL:
                        case ERASER:
                            ((PencilShape) currentShape).points.add(currentPoint);
                            break;
                        case LINE:
                            ((LineShape) currentShape).end = currentPoint;
                            break;
                        case RECTANGLE:
                            ((RectangleShape) currentShape).end = currentPoint;
                            break;
                        case OVAL:
                            ((OvalShape) currentShape).end = currentPoint;
                            break;
                    }
                    
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Hintergrundbild zeichnen
            g2d.drawImage(image, 0, 0, null);
            
            // Alle Formen zeichnen
            for (Shape shape : shapes) {
                shape.draw(g2d);
            }
        }
    }
    
    /**
     * Hauptmethode zum Testen des Dialogs.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Bilddatei öffnen");
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                PaintDialog2 dialog = new PaintDialog2(null, file);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
        });
    }
}