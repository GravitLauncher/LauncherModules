package pro.gravit.launchermodules.startscreen;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.net.URL;

public class ImageDisplay implements Closeable {
    private final JFrame frame;

    public ImageDisplay(URL img, BufferedImage icon, Config config) {
        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setLayout(new FlowLayout());
        if (icon != null) frame.setIconImage(icon);
        JLabel lbl = new JLabel();
        lbl.setIcon(new ImageIcon(img));
        frame.add(lbl);
        frame.setBackground(new Color(config.colorR, config.colorG, config.colorB, config.colorA));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
    }

    @Override
    public void close() {
        frame.setVisible(false);
    }
}
