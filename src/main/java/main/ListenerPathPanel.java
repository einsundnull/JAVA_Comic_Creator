package main;

import java.awt.Panel;
import java.awt.event.MouseEvent;

public interface ListenerPathPanel {

	public void mouseExited();

	public void mouseEntered();

	public void mouseClickedTwice(MouseEvent e, Panel p);
	
	public void mouseMoved();

}
