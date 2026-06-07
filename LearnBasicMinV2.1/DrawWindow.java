import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class DrawWindow extends JFrame {

    Color c = Color.BLACK;
    List<ShapeCmd> list = new ArrayList<>();
    private BufferedImage image;
    private Graphics2D imageG;

    // 入力状態
    private String lastKey = "";
    private int mouseX = 0;
    private int mouseY = 0;
    private int mouseButton = 0;

    public DrawWindow(int id, int h, int w) {
        super("LB " + id);
        setSize(w, h);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // BufferedImage
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        imageG = image.createGraphics();
        imageG.setColor(Color.WHITE);
        imageG.fillRect(0, 0, w, h);

        Panel panel = new Panel(list, image);
        add(panel);

        // キー入力
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                lastKey = String.valueOf(e.getKeyChar());
                if (lastKey.equals("\0")) lastKey = String.valueOf(e.getKeyCode());
            }
        });

        // マウス入力
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseButton = e.getButton();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseButton = 0;
            }
        });

        setVisible(true);
        setFocusable(true);
    }

    public void setColor(Color c) {
        this.c = c;
        if (imageG != null) imageG.setColor(c);
    }

    public void add(ShapeCmd s) {
        list.add(s);
        if (imageG != null) s.draw(imageG);
        repaint();
    }

    public BufferedImage getImage() { return image; }
    public void repaintImage() { repaint(); }

    // 入力取得用
    public String getLastKey() { 
        String k = lastKey;
        lastKey = ""; 
        return k; 
    }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public int getMouseButton() { return mouseButton; }
}