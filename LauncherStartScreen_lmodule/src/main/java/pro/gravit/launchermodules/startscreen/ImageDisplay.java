package pro.gravit.launchermodules.startscreen;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;

public class ImageDisplay implements Closeable {
    private final JFrame frame;

    public ImageDisplay(BufferedImage img) {
        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setBackground(new Color(1.0f,1.0f,1.0f,0.0f));
        frame.setLayout(new FlowLayout());
        JLabel lbl = new JLabel();
        lbl.setIcon(new ImageIcon(img));
        frame.add(lbl);
        frame.setSize(img.getWidth(), img.getHeight());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    @Override
    public void close() {
        frame.setVisible(false);
    }
}
