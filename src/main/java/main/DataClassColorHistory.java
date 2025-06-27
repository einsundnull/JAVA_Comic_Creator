package main;

import java.awt.Color;

public class DataClassColorHistory {
	
		Color color;
		boolean selected;

		public DataClassColorHistory(Color color, boolean selected) {
			this.color = color;
			this.selected = selected;

		}

		public void setSelected(boolean selected) {
			// TODO Auto-generated method stub
			this.selected = selected;
		}

		public Color getColor() {
			// TODO Auto-generated method stub
			Color c = color;
			return c;
		}

		public int getColorAsInt() {
			Color c = color;
			return c.getRGB();
		}

		public boolean isSelected() {
			// TODO Auto-generated method stub
			boolean b = selected;
			return b;
		}

	}

