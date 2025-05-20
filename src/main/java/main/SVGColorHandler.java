package main;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SVGColorHandler {

	public static void setSVGPathColor(Document document, String color) {
		if (document == null || color == null || color.isEmpty()) return;

		NodeList paths = document.getElementsByTagName("path");
		for (int i = 0; i < paths.getLength(); i++) {
			Element path = (Element) paths.item(i);
			path.setAttribute("fill", color);
		}
	}

	public static String getSVGPathColor(Document document) {
		if (document == null) return null;

		NodeList paths = document.getElementsByTagName("path");
		if (paths.getLength() > 0) {
			Element path = (Element) paths.item(0);
			return path.getAttribute("fill");
		}
		return null;
	}
}
