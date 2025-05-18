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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

public class CustomImageTile5 {
	private static final int IDX_FILENAME = 0;
	private static final int IDX_INDEX = 1;
	private static final int IDX_HEIGHT = 2;
	private static final int IDX_HEIGHT_PERCENT = 3;
	private static final int IDX_WIDTH = 4;
	private static final int IDX_WIDTH_PERCENT = 5;
	private static final int IDX_POS_X = 6;
	private static final int IDX_POS_Y = 7;

	private final JPanel panel;
	private final JSVGCanvas svgCanvas;
	private final LinkedList<String> data;
	private final int RESIZE_MARGIN = 10;
	private Point dragOffset;
	private boolean enableDrag = true;
	private TileUpdateListener updateListener;

	public CustomImageTile5(LinkedList<String> data, File svgFolder) {
		this.data = data;
		panel = new JPanel(null);
		svgCanvas = new JSVGCanvas();
		initView(svgFolder);
		enableDrag();
		enableResize();
		enableSelection();
//		enableRotate();
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
//				System.out.println("CustomImageTile Drag x: " + e.getX() + " y: " + e.getY());
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				toggleSelected();
//				System.out.println("CustomImageTile Drag x: " + e.getX() + " y: " + e.getY());
			}

			
			@Override
			public void mouseEntered(MouseEvent e) {
				panel.setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // oder andere Farbe/Stärke
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if(!selected)
				panel.setBorder(null);
			}
		});
		svgCanvas.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if (enableDrag) {
//					System.out.println("CustomImageTile Drag x: " + e.getX() + " y: " + e.getY());
					int nx = panel.getX() + e.getX() - dragOffset.x;
					int ny = panel.getY() + e.getY() - dragOffset.y;
					panel.setLocation(nx, ny);
					updateData();
				}
			}

		});
		  // NEU: Zoom per Mausrad
	    svgCanvas.addMouseWheelListener(e -> {
	        if (!enableDrag || e.isControlDown()) { // Nur zoomen, wenn kein Drag aktiv oder Strg gedrückt
	            double scaleFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9; // Hoch = Vergrößern, Runter = Verkleinern
	            scaleSVG(scaleFactor);
	            e.consume(); // Verhindert Scrollen des Eltern-Panels
	        } else  if (!enableDrag || e.isAltDown()) { // Nur zoomen, wenn kein Drag aktiv oder Strg gedrückt
	            double scaleFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9; // Hoch = Vergrößern, Runter = Verkleinern
	            rotate(10*scaleFactor); // 45 Grad rotieren
	            e.consume(); // Verhindert Scrollen des Eltern-Panels
	        }
	    });
	}

	private void enableResize() {
	    svgCanvas.addMouseMotionListener(new MouseMotionAdapter() {
	        @Override
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

	        @Override
	        public void mouseDragged(MouseEvent e) {
	            if (svgCanvas.getCursor().getType() == Cursor.SE_RESIZE_CURSOR) {
	                int nw = Math.max(10, e.getX()); // Mindestgröße 10x10
	                int nh = Math.max(10, e.getY());
	                panel.setSize(nw, nh);
	                svgCanvas.setSize(nw, nh);
	                updateData();
	            }
	        }
	    });
	}

	public void enableSelection() {
		svgCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isShiftDown()) {
					toggleSelected();
				}
			}
		});
	}
	
//	public void enableRotate() {
//		svgCanvas.addMouseListener(new MouseAdapter() {
//		    @Override
//		    public void mouseClicked(MouseEvent e) {
//		        if (e.isAltDown()) { // Alt + Klick für Rotation
//		            rotate(45); // 45 Grad rotieren
//		        }
//		    }
//		});
//	}
	
	public void rotate(double degrees) {
	    rotateSVG(degrees); // Oder setRotation(degrees), je nach gewünschter Methode
	    updateData(); // Daten aktualisieren
	}

	// Methode zum Rotieren der SVG
	public void rotateSVG(double degrees) {
	    try {
	        SVGDocument doc = (SVGDocument) svgCanvas.getSVGDocument();
	        if (doc == null) return;

	        Element root = doc.getDocumentElement();
	        String transform = String.format("rotate(%f %f %f)", degrees, panel.getWidth() / 2.0, panel.getHeight() / 2.0);
	        root.setAttribute("transform", transform);

	        svgCanvas.setSVGDocument(doc); // <-- das ist entscheidend!
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	
	 void scaleSVG(double scaleFactor) {
	    // Aktuelle Größe des SVG-Canvas
	    int currentWidth = svgCanvas.getWidth();
	    int currentHeight = svgCanvas.getHeight();
	    
	    // Neue Größe berechnen
	    int newWidth = (int) (currentWidth * scaleFactor);
	    int newHeight = (int) (currentHeight * scaleFactor);
	    
	    // Größe anpassen (Panel + Canvas)
	    panel.setSize(newWidth, newHeight);
	    svgCanvas.setSize(newWidth, newHeight);
	    
	    updateData(); // Daten aktualisieren
	}

	private boolean selected = false;

	private void toggleSelected() {
		selected = !selected;
//		if(selected) {
////			svgCanvas.setBackground(new Color(0, 1, 0, 0.3f)); // vollständig transparent
//		} else {
//			svgCanvas.setBackground(new Color(0, 0, 0, 0)); // vollständig transparent
//		}
		
		panel.setBorder(selected ? BorderFactory.createLineBorder(Color.RED, 2) : null);
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		panel.setBorder(selected ? BorderFactory.createLineBorder(Color.RED, 2) : null);
	}

	private void updateData() {
	    data.set(IDX_POS_X, String.valueOf(panel.getX()));
	    data.set(IDX_POS_Y, String.valueOf(panel.getY()));
	    data.set(IDX_WIDTH, String.valueOf(panel.getWidth()));
	    data.set(IDX_HEIGHT, String.valueOf(panel.getHeight()));
	    notifyUpdate(); // <- Hier
	}




	public void setTileUpdateListener(TileUpdateListener listener) {
	    this.updateListener = listener;
	}

	private void notifyUpdate() {
	    if (updateListener != null) {
	        updateListener.onTileUpdated(this);
	    }
	}


	public JPanel getPanel() {
		return panel;
	}

	public String getFilename() {
		return data.get(IDX_FILENAME);
	}

	public void setFilename(String newName) {
		// TODO Auto-generated method stub
//		data.set(newName);
	}
}


