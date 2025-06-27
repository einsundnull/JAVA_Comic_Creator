package main;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPanel;

class PanelCopyData {
	private JPanel panel;
	private List<CustomImageSVGTile> addedTiles;
	private Rectangle captureZone;
	private Point startPoint;
	private double zoom;

	public PanelCopyData(JPanel panel, List<CustomImageSVGTile> addedTiles, Rectangle captureZone, Point startPoint,
			double zoom) {
		this.panel = panel;
		this.addedTiles = addedTiles;
		this.captureZone = captureZone;
		this.startPoint = startPoint;
		this.zoom = zoom;
	}

	public PanelCopyData() {
		// TODO Auto-generated constructor stub
	}

	// Getter-Methoden
	public JPanel getPanel() {
		return panel;
	}

	public List<CustomImageSVGTile> getTiles() {
		return addedTiles;
	}

	public Rectangle getCaptureZone() {
		return captureZone;
	}

	public Point getStartPoint() {
		return startPoint;
	}

	public double getZoom() {
		return zoom;
	}

}