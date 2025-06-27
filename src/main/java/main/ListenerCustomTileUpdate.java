package main;

import java.awt.Point;
import java.awt.event.MouseWheelEvent;

import javax.swing.JPanel;

public interface ListenerCustomTileUpdate {

	void onTileHover(CustomImageSVGTile tile, boolean isHovered); // Neue Methode für Hover-Events

	void drawCaptureZoneFromCustomTile(Point pointInParent);

	void drawCaptureZoneFromCustomTileSetStartPoint(Point point);

	void onVK_DELETE_typed(String id);

	void removeSelectedSVGFromListener(JPanel row, CustomImageSVGTile tile);

	void addPathPointOnTileHoverFromCustomTile(Point e);

	void onVK_ESCAPE_typed(CustomImageSVGTile customImageSVGTile);

	void zoom(MouseWheelEvent e);

}
