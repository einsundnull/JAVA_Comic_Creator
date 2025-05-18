package main;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

public class CustomImageTile4 {
	private static final int IDX_FILENAME = 0;
	private static final int IDX_INDEX = 1;
	private static final int IDX_HEIGHT = 2;
	private static final int IDX_HEIGHT_PERCENT = 3;
	private static final int IDX_WIDTH = 4;
	private static final int IDX_WIDTH_PERCENT = 5;
	private static final int IDX_POS_X = 6;
	private static final int IDX_POS_Y = 7;

	private final JPanel panel;
//	private final JSVGCanvas svgCanvas;
	RotatableSVG svgCanvas;
	private final LinkedList<String> data;
	private final int RESIZE_MARGIN = 10;
	private Point dragOffset;
	private boolean enableDrag = true;

	public CustomImageTile4(LinkedList<String> data, File svgFolder) {
		this.data = data;
		panel = new JPanel(null);
		svgCanvas = new RotatableSVG(svgFolder);
		initView(svgFolder);
		enableDrag();
		enableResize();
		enableSelection();
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

		svgCanvas = new RotatableSVG(new File(svgFolder, fileName));
		svgCanvas.setBounds(0, 0, w, h);
		panel.add(svgCanvas);
		svgCanvas.setOpaque(false);
		svgCanvas.setBackground(new Color(0, 0, 0, 0)); // vollständig transparent

//		svgCanvas.setURI(new File(svgFolder, fileName).toURI().toString());
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
			public void mouseEntered(MouseEvent e) {
				panel.setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // oder andere Farbe/Stärke
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (!selected)
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
			}
		});
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
	}

	public JPanel getPanel() {
		return panel;
	}

	public String getFilename() {
		return data.get(IDX_FILENAME);
	}

	private class RotatableSVG extends JComponent {
		BufferedImage image;
		double rotation = 0;

		public RotatableSVG(File svgFile) {
			try {
				image = transcoderToBufferedImage(svgFile); // mit Batik transcodieren
			} catch (Exception e) {
				e.printStackTrace();
			}
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (image == null)
				return;

			Graphics2D g2d = (Graphics2D) g.create();
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			g2d.rotate(rotation, cx, cy);
			g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
			g2d.dispose();
		}

		private BufferedImage transcoderToBufferedImage(File file) {
			try {
				TranscoderInput input = new TranscoderInput(file.toURI().toString());
				BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
				transcoder.transcode(input, null);
				return transcoder.getBufferedImage();
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}

	}

	private static class BufferedImageTranscoder extends ImageTranscoder {
		private BufferedImage image;

		@Override
		public BufferedImage createImage(int w, int h) {
			return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		@Override
		public void writeImage(BufferedImage img, TranscoderOutput out) {
			this.image = img;
		}

		public BufferedImage getBufferedImage() {
			return image;
		}
	}

}
