import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class DrawWindow extends JFrame {

    Color c = Color.BLACK;
    List<ShapeCmd> list = new ArrayList<>();
    private BufferedImage image;
    private Graphics2D imageG;

    public DrawWindow(int id, int h, int w) {
        super("LB " + id);
        setSize(w, h);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // BufferedImage作成
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        imageG = image.createGraphics();
        imageG.setColor(Color.WHITE);
        imageG.fillRect(0, 0, w, h);

        add(new Panel(list, image));
        setVisible(true);
    }

    public void setColor(Color c) {
        this.c = c;
        if (imageG != null) imageG.setColor(c);
    }

    public void add(ShapeCmd s) {
        list.add(s);
        s.draw(imageG);  // BufferedImageにも描画
        repaint();
    }

    // Flood Fill 用
    public BufferedImage getImage() {
        return image;
    }

    public void repaintImage() {
        repaint();
    }
}