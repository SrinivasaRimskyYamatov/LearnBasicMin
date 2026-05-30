import java.awt.*;
import java.util.*;

public class WindowManager {

    int next = 1;
    Map<Integer, DrawWindow> map = new HashMap<>();

    public int create(int h, int w) {
        int id = next++;
        map.put(id, new DrawWindow(id, h, w));
        return id;
    }

    DrawWindow get(int id) {
        DrawWindow w = map.get(id);
        if (w == null) throw new RuntimeException("invalid window id");
        return w;
    }

    public void delete(int id) {
        DrawWindow w = map.remove(id);
        if (w != null) w.dispose();
    }

    public void setColor(int id, int r, int g, int b) {
        get(id).setColor(new Color(r, g, b));
    }

    public void drawLine(int id, int x1, int y1, int x2, int y2) {
        get(id).add(new Line(x1, y1, x2, y2, get(id).c));
    }

    public void drawBox(int id, int x1, int y1, int x2, int y2) {
        get(id).add(new Box(x1, y1, x2, y2, get(id).c));
    }
}
