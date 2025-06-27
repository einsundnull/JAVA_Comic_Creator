package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

public class PotraceUIAll {

    private static File droppedImage;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Potrace SVG Converter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);

            JLabel dropLabel = new JLabel("Bild hierher ziehen", SwingConstants.CENTER);
            dropLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            dropLabel.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
            dropLabel.setTransferHandler(new TransferHandler("text"));

            new DropTarget(dropLabel, new DropTargetAdapter() {
                public void drop(DropTargetDropEvent dtde) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        List<File> droppedFiles = (List<File>)
                                dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        if (!droppedFiles.isEmpty()) {
                            droppedImage = droppedFiles.get(0);
                            dropLabel.setText("Datei: " + droppedImage.getName());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            JButton convertButton = new JButton("Konvertieren");
            convertButton.addActionListener(e -> {
                if (droppedImage != null && droppedImage.exists()) {
                    ConverterImageToSvgClassSuitable.convertImageToSVG(droppedImage);
                } else {
                    JOptionPane.showMessageDialog(frame, "Bitte zuerst eine Bilddatei droppen.");
                }
            });

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            panel.add(dropLabel, BorderLayout.CENTER);
            panel.add(convertButton, BorderLayout.SOUTH);

            frame.setContentPane(panel);
            frame.setVisible(true);
        });
    }
}
