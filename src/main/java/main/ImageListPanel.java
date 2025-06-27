package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

//Klasse: ImageListPanel
public class ImageListPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final File imageDir;
//	private final JPanel imageContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
	private final JPanel imageContainer = new JPanel();
	private ListenerImageListView listenerImageListView;

	public ImageListPanel(File imageDir, ListenerImageListView listenerImageListView) {
		this.imageDir = imageDir;
		setLayout(new BorderLayout());
		imageContainer.setLayout(new javax.swing.BoxLayout(imageContainer, javax.swing.BoxLayout.Y_AXIS));
		this.listenerImageListView = listenerImageListView;
		JScrollPane scrollPane = new JScrollPane(imageContainer);

		add(scrollPane, BorderLayout.CENTER);
		setupDropTarget();
		refreshImageList();
	}

	private List<File> loadImageFiles(File dir) {
		File[] files = dir.listFiles((d, name) -> {
			String n = name.toLowerCase();
			return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif")
					|| n.endsWith(".bmp");
		});
		return files != null ? List.of(files) : List.of();
	}

	public void refreshImageList() {
		imageContainer.removeAll();
		List<File> images = loadImageFiles(imageDir);
		for (File img : images) {
			CustomImageBMPItem item = new CustomImageBMPItem(img, listenerImageListView);
			item.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			item.setToolTipText(img.getName());
			item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			item.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					listenerImageListView.onImageClick(new File(img.getAbsolutePath()));
				}
			});
			imageContainer.add(item);
		}
		revalidate();
		repaint();
	}

	private void setupDropTarget() {
		new DropTarget(this, new DropTargetAdapter() {
			@Override
			public void drop(DropTargetDropEvent dtde) {
				try {
					dtde.acceptDrop(DnDConstants.ACTION_COPY);
					@SuppressWarnings("unchecked")
					List<File> dropped = (List<File>) dtde.getTransferable()
							.getTransferData(DataFlavor.javaFileListFlavor);
					for (File file : dropped) {
						if (file.isFile()) {
							String name = file.getName().toLowerCase();
							if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
									|| name.endsWith(".gif") || name.endsWith(".bmpFS")) {
								Files.copy(file.toPath(), new File(imageDir, file.getName()).toPath(),
										StandardCopyOption.REPLACE_EXISTING);
							}
						}
					}
					refreshImageList();
				} catch (Exception ignored) {
				}
			}
		});
	}
}

class CustomImageBMPItem extends JPanel {

	private final File imageFile;
	private final ListenerImageListView listener;

	public CustomImageBMPItem(File imageFile, ListenerImageListView listener) {
		this.imageFile = imageFile;
		this.listener = listener;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createLineBorder(Color.GRAY));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 120)); // für BoxLayout

		add(createImageLabel(), BorderLayout.WEST);
		add(createButtonPanel(), BorderLayout.CENTER);
	}

	private JLabel createImageLabel() {
		try {
			Image img = ImageIO.read(imageFile).getScaledInstance(100, 100, Image.SCALE_SMOOTH);
			JLabel label = new JLabel(new ImageIcon(img));
			label.setToolTipText(imageFile.getName());
			label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			label.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e) {
					listener.onImageClick(imageFile);
				}
			});
			return label;
		} catch (Exception e) {
			return new JLabel("Fehler");
		}
	}

	private JPanel createButtonPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton openBtn = new JButton("Öffnen");
		openBtn.addActionListener((ActionEvent e) -> listener.onImageClick(imageFile));

		JButton deleteBtn = new JButton("Löschen");
		deleteBtn.addActionListener((ActionEvent e) -> {
			if (imageFile.delete()) {
				listener.onImageDelete(imageFile);

//				SwingUtilities.getWindowAncestor(this).repaint(); // unsauber, besser wäre Listener
			}
		});

		panel.add(openBtn);
		panel.add(deleteBtn);
		return panel;
	}
}
