package main;

import java.io.File;

import javax.swing.JPanel;

public interface ListenerRightTiles {

	void setComponentZOrder(JPanel panel, int i);

	void highlightCorrespondingTileInCanvas(File file, JPanel scenePanel, boolean b);

	void mouseEntered(File file, JPanel scenePanel, boolean b);

	void mouseExited(File file, JPanel scenePanel, boolean b);

	void checkBoxDeselectAction(JPanel row, CustomImageSVGTile tile);

}
