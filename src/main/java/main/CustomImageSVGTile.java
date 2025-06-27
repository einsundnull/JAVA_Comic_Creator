package main;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.LinkedList;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CustomImageSVGTile {
	public static File file;
	public static final int IDX_FILENAME = 0;
	public static final int IDX_FILEPATH = 1;
	public static final int IDX_INDEX = 2;
	public static final int IDX_HEIGHT = 3;
	public static final int IDX_HEIGHT_PERCENT = 4;
	public static final int IDX_WIDTH = 5;
	public static final int IDX_WIDTH_PERCENT = 6;
	public static final int IDX_POS_X = 7;
	public static final int IDX_POS_Y = 8;

	private final JPanel panel;
	private final JSVGCanvas svgCanvas;
	private final LinkedList<String> data;

	private final int RESIZE_MARGIN = 10;
	private Point dragOffset;
	private Point point;
	private boolean enableDrag = true;
	private boolean disableDragFromOutside = false;
	private boolean isMirroredHorizontal = false;
	private boolean isMirroredVertical = false;
	private boolean temporarySelected = false;
	private boolean selected = false;
	private ListenerCustomTileUpdate updateListener;
	private double rotation = 0;
	private String id;
	private String index;
	private int h;
	private int w;
	private int x;
	private int y;
//	private Document svgDocument;
	private Document document;
	private String initialColorToSet;
	private MouseMotionAdapter mouseMotionAdapter;

	public CustomImageSVGTile(LinkedList<String> data, File svgFolder) {
		this.data = data;
		panel = new JPanel(null);
		svgCanvas = new JSVGCanvas();
//		svgCanvas = new NoAntialiasJSVGCanvas();
		svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
		id = generateUniqueId();
		initView();
		enableDrag();
		enableResize();
		enableSelection();
		this.initialColorToSet = "#000000"; // Speichern für später
		setSVGPathColor(initialColorToSet);

	}

	public String getInitialColorToSet() {
		return initialColorToSet;
	}

	public void setInitialColorToSet(String initialColorToSet) {
		this.initialColorToSet = initialColorToSet;
	}

	public CustomImageSVGTile(LinkedList<String> data) {
		this.data = data;
		panel = new JPanel(null);
		svgCanvas = new JSVGCanvas();
//		svgCanvas = new NoAntialiasJSVGCanvas();
		svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
		id = generateUniqueId();
		initView();
		enableDrag();
		enableResize();
		enableSelection();
		this.initialColorToSet = "#000000"; // Speichern für später
		setSVGPathColor(initialColorToSet);

	}

	public CustomImageSVGTile() {
		this.panel = null;
		this.data = null;
		this.svgCanvas = null;
	}

	public Document loadSVGDocument(String svgFilePath) throws Exception {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
		return factory.createDocument(new File(svgFilePath).toURI().toString());
	}

	private void createMousMotionAdapter() {

		mouseMotionAdapter = new MouseMotionAdapter() {
			private Point lastPoint;

			public void mouseDragged(MouseEvent e) {
				if (disableDragFromOutside) {
//					Point pointInParent = SwingUtilities.convertPoint(panel, e.getPoint(), panel.getParent());
					int x = panel.getX();
					int y = panel.getY();
					int xii = e.getX();
					int yii = e.getY();
					int xiii = x + xii;
					int yiii = y + yii;
					System.out.println("x: " + x + " xii: " + xii + " xiii: " + xiii);
					System.out.println("y: " + y + " yii: " + yii + " yiii: " + yiii);
					Point p = new Point(xiii, yiii);
					updateListener.drawCaptureZoneFromCustomTile(p);
					System.out.println("CustomImageTile.createMousMotionAdapter() I");
				} else {
					System.out.println("CustomImageTile.createMousMotionAdapter() II");
					if (enableDrag && !e.isAltDown()) {
						// Normales Drag-Verhalten für panel
						int nx = panel.getX() + e.getX() - dragOffset.x;
						int ny = panel.getY() + e.getY() - dragOffset.y;
						panel.setLocation(nx, ny);
						updateData();
						e.consume();
					} else if (enableDrag && e.isAltDown()) {
						// Rotationslogik
						if (lastPoint != null) {
							// Berechne die Differenz zur letzten Position
							int deltaX = e.getX() - lastPoint.x;
							// Sie können deltaY auch verwenden, wenn Sie Rotation in beide Richtungen
							// möchten

							// Rotationswinkel basierend auf der horizontalen Bewegung
							rotation += deltaX * 0.5; // Anpassungsfaktor für die Empfindlichkeit
							System.out.println("Rotate: " + rotation + "°");
							rotateSVG(rotation);
							updateData();
							e.consume();
						}
						lastPoint = e.getPoint();
					}
				}
			}

		};

		svgCanvas.addMouseMotionListener(mouseMotionAdapter);
	}

	private void initView() {
		try {
			document = loadSVGDocument(data.get(IDX_FILEPATH));
		} catch (Exception e) {
			e.printStackTrace();
		}
		File svgFileToLoad = new File(data.get(IDX_FILEPATH));
		String fileName = data.get(IDX_FILENAME);
		index = data.get(IDX_INDEX);
		h = Integer.parseInt(data.get(IDX_HEIGHT));
		w = Integer.parseInt(data.get(IDX_WIDTH));
		x = Integer.parseInt(data.get(IDX_POS_X));
		y = Integer.parseInt(data.get(IDX_POS_Y));

		panel.setBounds(x, y, w, h);
		panel.setOpaque(false);
		panel.setBorder(null);
		panel.setLayout(null);

		svgCanvas.setOpaque(false);
		svgCanvas.setBackground(new Color(0, 0, 0, 0)); // Vollständig transparent

		// Listener hinzufügen, BEVOR setURI aufgerufen wird
		svgCanvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {
			@Override
			public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
				System.out.println("SVG Document loaded: " + svgFileToLoad.getName());
				// Apply any pending color change
				if (initialColorToSet != null && !initialColorToSet.isEmpty()) {
					setSVGPathColor(initialColorToSet);
				}
			}
		});

		svgCanvas.setURI(svgFileToLoad.toURI().toString()); // Löst das Laden und den Listener aus
		svgCanvas.setLocation(0, 0);
		svgCanvas.setSize(w, h);

		panel.add(svgCanvas);
	}

	private void enableDrag() {
		dragOffset = new Point();
		svgCanvas.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {

				if (disableDragFromOutside) {
					getMouseLocation(e);
					dragOffset.setLocation(point);
					updateListener.drawCaptureZoneFromCustomTileSetStartPoint(dragOffset);
				} else {
					dragOffset.setLocation(e.getPoint());

				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				toggleSelected();
				getMouseLocation(e);
				if (e.isShiftDown()) {
					updateListener.addPathPointOnTileHoverFromCustomTile(point);
				} else {

				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				temporarySelected = true;
				if (!e.isAltDown()) {
					if (!isSelected()) {
						panel.setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // oder andere Farbe/Stärke
					} else {
						panel.setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // oder andere Farbe/Stärke
					}

				} else {
					panel.setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // oder andere Farbe/Stärke
					svgCanvas.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2)); // oder andere Farbe/Stärke
				}
				if (updateListener != null)
					updateListener.onTileHover(CustomImageSVGTile.this, true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				temporarySelected = false;
				if (!selected) {

					panel.setBorder(null);
					svgCanvas.setBorder(null);
				} else {
					panel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2)); // oder andere Farbe/Stärke
				}

				if (updateListener != null)
					updateListener.onTileHover(CustomImageSVGTile.this, false);

			}
		});

		createMousMotionAdapter();
		// NEU: Zoom per Mausrad
		svgCanvas.addMouseWheelListener(e -> {
			if (!enableDrag || e.isControlDown()) { // Nur zoomen, wenn kein Drag aktiv oder Strg
				if (e.isShiftDown()) { // gedrückt
					double scaleFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9; // Hoch = Vergrößern, Runter =
																				// Verkleinern
					scaleSVG(scaleFactor);
					updateData();
					e.consume(); // Verhindert Scrollen des Eltern-Panels
				} else {
					updateListener.zoom(e);
				}
			} else if (!enableDrag || e.isAltDown()) {
				int direction = e.getWheelRotation(); // -1 = hoch, +1 = runter
				rotation += direction * 1.1; // z.B. 5° pro Schritt
				System.out.println("Rotate: " + rotation + "°");
				rotateSVG(rotation);
				updateData();
				e.consume();
			}

		});

	}

	private void getMouseLocation(MouseEvent e) {
		int x = panel.getX();
		int y = panel.getY();
		int xii = e.getX();
		int yii = e.getY();
		int xiii = x + xii;
		int yiii = y + yii;
		System.out.println("x: " + x + " xii: " + xii + " xiii: " + xiii);
		System.out.println("y: " + y + " yii: " + yii + " yiii: " + yiii);
		point = new Point(xiii, yiii);
	}

	private void enableResize() {
		svgCanvas.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (e.getX() >= panel.getWidth() - RESIZE_MARGIN && e.getY() >= panel.getHeight() - RESIZE_MARGIN) {
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

		svgCanvas.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					updateListener.onVK_DELETE_typed( id);
					System.out.println("Pressed Delete Key");
				
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					updateListener.onVK_ESCAPE_typed(CustomImageSVGTile.this);

				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {

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

	// Methode zum Rotieren der SVG
	public void rotateSVG(double degrees) {
		this.rotation = degrees;

		AffineTransform at = new AffineTransform();
		double centerX = panel.getWidth() / 2.0;
		double centerY = panel.getHeight() / 2.0;

		at.translate(centerX, centerY);
		at.rotate(Math.toRadians(degrees));
		at.translate(-centerX, -centerY);

		panel.setLayout(new OverlayLayout(panel));
		svgCanvas.setRenderingTransform(at, true);

		panel.revalidate();
		panel.repaint();

		// applyTransform war hier zu früh
		updateData();
	}

	public JSVGCanvas getSvgCanvas() {
		return svgCanvas;
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

	public void toggleMirrorHorizontal() {
		isMirroredVertical = !isMirroredVertical;
		applyTransform();
	}

	public void toggleMirrorVertical() {
		isMirroredHorizontal = !isMirroredHorizontal;
		applyTransform();
	}

	private void toggleSelected() {
		selected = !selected;
		panel.setBorder(selected ? BorderFactory.createLineBorder(Color.GREEN, 2) : null);
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

	}

	public JPanel getPanel() {
		return panel;
	}

	public String getFilename() {
		return data.get(IDX_FILENAME);
	}

	public String getFile() {
		return data.get(IDX_FILEPATH);
	}

	public void setFilename(String newName) {
		data.set(IDX_FILENAME, newName);
	}

	public void removeFromParent() {
		if (panel.getParent() != null) {
			panel.getParent().remove(panel);
			panel.getParent().revalidate();
			panel.getParent().repaint();
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (e.isShiftDown()) {
			// Canvas innerhalb des Panels verschieben
			int nx = svgCanvas.getX() + e.getX() - dragOffset.x;
			int ny = svgCanvas.getY() + e.getY() - dragOffset.y;
			svgCanvas.setLocation(nx, ny);
			updateData();
			return;
		}

		if (e.isAltDown() && e.isControlDown()) {
			// Nur Panelgröße ändern, Canvas bleibt wie er ist
			int nw = Math.max(10, panel.getWidth() + e.getX() - dragOffset.x);
			int nh = Math.max(10, panel.getHeight() + e.getY() - dragOffset.y);
			panel.setSize(nw, nh);
			updateData();
			return;
		}

		if (enableDrag && !disableDragFromOutside) {
			// Normales Dragging
			int nx = panel.getX() + e.getX() - dragOffset.x;
			int ny = panel.getY() + e.getY() - dragOffset.y;
			panel.setLocation(nx, ny);
			updateData();
		}
	}

	public boolean isTemporarySelected() {
		return temporarySelected;
	}

	private void applyTransform() {
		AffineTransform at = new AffineTransform();

		double centerX = panel.getWidth() / 2.0;
		double centerY = panel.getHeight() / 2.0;

		at.translate(centerX, centerY);
		if (isMirroredHorizontal)
			at.scale(-1, 1);
		if (isMirroredVertical)
			at.scale(1, -1);
		at.rotate(Math.toRadians(rotation));
		at.translate(-centerX, -centerY);

		svgCanvas.setRenderingTransform(at, true);
		panel.revalidate();
		panel.repaint();
	}

	public void setSVGPathColor(String color) {
		if (color == null || color.isEmpty()) {
			return;
		}

		try {
			// Get the SVG document directly from the canvas
			Document svgDoc = svgCanvas.getSVGDocument();

			if (svgDoc == null) {
				System.err.println("SVG document not available in canvas yet");
				// Store color for later application when document is loaded
				this.initialColorToSet = color;
				return;
			}
			this.initialColorToSet = color;
			System.out.println("Setting SVG color to: " + color);

			// Apply color to all relevant SVG elements
			setColorForElementType(svgDoc, "path", color);
			setColorForElementType(svgDoc, "circle", color);
			setColorForElementType(svgDoc, "rect", color);
			setColorForElementType(svgDoc, "polygon", color);
			setColorForElementType(svgDoc, "polyline", color);
			setColorForElementType(svgDoc, "ellipse", color);

			// Force repaint
			svgCanvas.repaint();

		} catch (Exception e) {
			System.err.println("Error changing SVG color: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void setColorForElementType(Document doc, String tagName, String color) {
		NodeList elements = doc.getElementsByTagName(tagName);
		for (int i = 0; i < elements.getLength(); i++) {
			Element element = (Element) elements.item(i);
			// Remove any style that might override our fill
			String currentStyle = element.getAttribute("style");
			if (currentStyle != null && !currentStyle.isEmpty()) {
				// Remove any fill property from inline style
				currentStyle = currentStyle.replaceAll("fill:[^;]+;?", "");
				if (!currentStyle.isEmpty()) {
					element.setAttribute("style", currentStyle);
				} else {
					element.removeAttribute("style");
				}
			}
			element.setAttribute("fill", color);
		}
	}

	public LinkedList<String> getData() {
		return data;
	}

	private String generateUniqueId() {
		return UUID.randomUUID().toString(); // Oder eine andere ID-Generierung
	}

	public void setUpdateListener(ListenerCustomTileUpdate listener) {
		this.updateListener = listener;
	}

	public String getID() {
		return id;
	}

//	private void applyFillColor(Element element, String color) {
//		element.removeAttribute("style");
//		element.setAttribute("fill", color);
//	}

	public void updateTileInSvg(String tileId, double x, double y, double width, double height) {
		Element tileElement = document.getElementById(tileId);
		if (tileElement != null) {
			tileElement.setAttribute("x", String.valueOf(x));
			tileElement.setAttribute("y", String.valueOf(y));
			tileElement.setAttribute("width", String.valueOf(width));
			tileElement.setAttribute("height", String.valueOf(height));
		} else {
			System.err.println("Element mit ID '" + tileId + "' nicht gefunden.");
		}
	}

	public static CustomImageSVGTile copyFrom(CustomImageSVGTile original) {
		CustomImageSVGTile copy = original;
		return copy;
	}

	public static CustomImageSVGTile copyTile(CustomImageSVGTile original) {
		// Dies ist nur ein Beispiel - Sie müssen diese Methode entsprechend Ihrer
		// CustomImageTile-Klasse anpassen

		// Annahme: CustomImageTile hat einen Konstruktor oder eine Methode zum Kopieren
		// Zum Beispiel: CustomImageTile(Image image, String name, etc.)

		// Alternativ könnten Sie eine copyFrom-Methode in CustomImageTile haben
		CustomImageSVGTile copy = new CustomImageSVGTile(); // Leeren Konstruktor aufrufen
		copy.copyFrom(original); // Diese Methode müssten Sie in CustomImageTile implementieren

		return copy;
	}

	public void toggleDragEnabled() {
		disableDragFromOutside = !disableDragFromOutside;

	}

}

class NoAntialiasJSVGCanvas extends JSVGCanvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		super.paintComponent(g2d);
		g2d.dispose();
	}
}
