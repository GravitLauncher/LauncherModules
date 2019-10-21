package pro.gravit.launchermodules.startscreen;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ImageDisplay implements Closeable {
    private final JFrame frame;

	public ImageDisplay(BufferedImage img)
    {
        frame = new JFrame();
        frame.setUndecorated(true);
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