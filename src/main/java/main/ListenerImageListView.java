package main;

import java.io.File;

public interface ListenerImageListView {
	public void onImageClicked(int index);

	public void onImageClick(File file);

	public void onImageDelete(File imageFile);

}
