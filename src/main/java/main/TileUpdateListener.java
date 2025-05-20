package main;

import java.awt.event.MouseAdapter;

public interface TileUpdateListener {
	void onTileUpdated(CustomImageTile tile);

	void onTileHover(CustomImageTile tile, boolean isHovered); // Neue Methode für Hover-Events

	void onTileHover(String id, boolean isHovered);
	void onTileHover( boolean isHovered);

	
	
}
