package main;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.apache.batik.swing.JSVGCanvas;
public class CustomImageTile {
    private static final int IDX_FILENAME       = 0;
    private static final int IDX_INDEX          = 1;
    private static final int IDX_HEIGHT         = 2;
    private static final int IDX_HEIGHT_PERCENT = 3;
    private static final int IDX_WIDTH          = 4;
    private static final int IDX_WIDTH_PERCENT  = 5;
    private static final int IDX_POS_X          = 6;
    private static final int IDX_POS_Y          = 7;

    private final JPanel panel;
    private final JSVGCanvas svgCanvas;
    private final LinkedList<String> data;
    private final int RESIZE_MARGIN = 10;
    private Point dragOffset;
    private boolean enableDrag = true;

    public CustomImageTile(LinkedList<String> data, File svgFolder) {
        this.data = data;
        panel     = new JPanel(null);
        svgCanvas = new JSVGCanvas();
        initView(svgFolder);
        enableDrag();
        enableResize();
    }

    private void initView(File svgFolder) {
        String fileName = data.get(IDX_FILENAME);
        int h = Integer.parseInt(data.get(IDX_HEIGHT));
        int w = Integer.parseInt(data.get(IDX_WIDTH));
        int x = Integer.parseInt(data.get(IDX_POS_X));
        int y = Integer.parseInt(data.get(IDX_POS_Y));

        panel.setBounds(x, y, w, h);
        panel.setOpaque(false); // Setzen Sie das Panel als transparent
        panel.setBorder(null); // Entfernen Sie den Rand, falls nicht benötigt
   

        svgCanvas.setOpaque(false);
        svgCanvas.setBackground(new Color(0, 0, 0, 0)); // vollständig transparent


        svgCanvas.setURI(new File(svgFolder, fileName).toURI().toString());
        svgCanvas.setBounds(0, 0, w, h);
        panel.add(svgCanvas);
    }


    private void enableDrag() {
        dragOffset = new Point();
        svgCanvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset.setLocation(e.getPoint());
            	System.out.println("CustomImageTile Drag x: " + e.getX() + " y: " + e.getY());
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // oder andere Farbe/Stärke
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBorder(null);
            }
        });
        svgCanvas.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
            	if(enableDrag) {
            	System.out.println("CustomImageTile Drag x: " + e.getX() + " y: " + e.getY());
                int nx = panel.getX() + e.getX() - dragOffset.x;
                int ny = panel.getY() + e.getY() - dragOffset.y;
                panel.setLocation(nx, ny);
                updateData();
            	}
            }
            
            
        });
    }

    private void enableResize() {
    	svgCanvas.addMouseMotionListener(new MouseMotionAdapter() {

			public void mouseMoved(MouseEvent e) {
                if (e.getX() >= panel.getWidth() - RESIZE_MARGIN &&
                    e.getY() >= panel.getHeight() - RESIZE_MARGIN) {
                	svgCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                	enableDrag = false;
                } else {
                	svgCanvas.setCursor(Cursor.getDefaultCursor());
                	enableDrag = true;
                }
            }
            public void mouseDragged(MouseEvent e) {
                if (svgCanvas.getCursor().getType() == Cursor.SE_RESIZE_CURSOR) {
                    int nw = e.getX(), nh = e.getY();
                    panel.setSize(nw, nh);
                    svgCanvas.setSize(nw, nh);
                    updateData();
                }
            }
        });
    }

    private void updateData() {
        data.set(IDX_POS_X, String.valueOf(panel.getX()));
        data.set(IDX_POS_Y, String.valueOf(panel.getY()));
        data.set(IDX_WIDTH, String.valueOf(panel.getWidth()));
        data.set(IDX_HEIGHT, String.valueOf(panel.getHeight()));
    }

    public JPanel getPanel() {
        return panel;
    }

    public String getFilename() {
        return data.get(IDX_FILENAME);
    }
}
