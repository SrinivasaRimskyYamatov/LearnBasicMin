import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

public class Panel extends JPanel {

    List<ShapeCmd> list;
    BufferedImage image;

    public Panel(List<ShapeCmd> list, BufferedImage image) {
        this.list = list;
        this.image = image;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // BufferedImageを描画
        if (image != null) {
            g2.drawImage(image, 0, 0, null);
        }

        // ベクター描画も残す
        List<ShapeCmd> snapshot = new ArrayList<>(list);
        for (ShapeCmd s : snapshot) {
            s.draw(g2);
        }
    }
}