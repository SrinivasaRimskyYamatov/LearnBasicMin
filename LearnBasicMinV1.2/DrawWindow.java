import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class DrawWindow extends JFrame {

    Color c = Color.BLACK;
    List<ShapeCmd> list = new ArrayList<>();

    public DrawWindow(int id, int h, int w) {
        super("LB " + id);
        setSize(w, h);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(new Panel(list));
        setVisible(true);
    }

    public void setColor(Color c) {
        this.c = c;
    }

    public void add(ShapeCmd s) {
        list.add(s);
        repaint();
    }
}
