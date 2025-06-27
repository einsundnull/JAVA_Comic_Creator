package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class CustomPathPanel {

	ListenerPathPanel pathPanelListener;
	Panel pan;
	public static int DIM = 15;

	public CustomPathPanel(ListenerPathPanel pathPanelListener) {
		this.pathPanelListener = pathPanelListener;
		pan = new Panel();
		pan.setBackground(new Color(255,0,0,50)); // Transparent background
		pan.setSize(new Dimension(DIM,DIM));
		// Center the point on
		// click

		pan.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				pan.setLocation(pan.getX() + e.getX() - pan.getWidth() / 2,
						pan.getY() + e.getY() - pan.getHeight() / 2);
				pathPanelListener.mouseMoved();
//				scenePanel.repaint(); // Redraw lines when point is moved
//				pan.setLocation(e.getX() - pan.getWidth() / 2, e.getY() - pan.getHeight() / 2);
			}
		});

		pan.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
				pathPanelListener.mouseExited();
//					addPathPoint = true;
				pan.setBackground(new Color(255,0,0,50));
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				pathPanelListener.mouseEntered();
//				addPathPoint = false;
				pan.setBackground(new Color(0,255,0,50));
			}

			@Override
			public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() == 2) { // Double click to remove point
//					pathPoints.remove(pan);
//					scenePanel.remove(pan);
//					scenePanel.repaint();
					pathPanelListener.mouseClickedTwice(e, pan);
				} else if (e.getClickCount() == 1) {
					pan.setBackground(new Color(120,120,120,50));
				}
			}
		});
	}

	public void setListener(ListenerPathPanel pathPanelListener) {
		this.pathPanelListener = pathPanelListener;
	}

	public Panel getPanel() {
		// TODO Auto-generated method stub
		return pan;
	}
}
