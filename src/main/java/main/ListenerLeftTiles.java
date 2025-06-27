package main;

import java.util.LinkedList;

public interface ListenerLeftTiles {

	void onClick(LinkedList<String> data);

	void actualizeTileMapping(String name, int index);

	void onClickRemove(LinkedList<String> data);

}
