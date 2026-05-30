import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class Panel extends JPanel {

    List<ShapeCmd> list;

    public Panel(List<ShapeCmd> list) {
        this.list = list;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        List<ShapeCmd> snapshot = new ArrayList<>(list);

        for (ShapeCmd s : snapshot) {
            s.draw(g2);
        }
    }
}