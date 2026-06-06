import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;

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
        if (w == null) throw new RuntimeException("invalid window id: " + id);
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

    public void windowName(int id, String title) {
        get(id).setTitle(title);
    }

    public void windowSize(int id, int height, int width) {
        DrawWindow w = get(id);
        w.setSize(width, height);
        w.revalidate();
        w.repaint();
    }

    public void drawPut(int id, int x, int y) {
        get(id).add(new Point(x, y, get(id).c));
    }

    public void drawCircle(int id, int cx, int cy, int r) {
        get(id).add(new Circle(cx, cy, r, get(id).c));
    }

    public void drawTri(int id, int x1, int y1, int x2, int y2, int x3, int y3) {
        get(id).add(new Triangle(x1, y1, x2, y2, x3, y3, get(id).c));
    }

    public void drawCls(int id) {
        DrawWindow w = get(id);
        w.list.clear();
        w.repaint();
    }

    public void drawFill(int id, int x, int y) {
        DrawWindow w = get(id);
        BufferedImage img = w.getImage();
        if (img == null) return;
        floodFill(img, x, y, w.c);
        w.repaintImage();
    }

    public void drawText(int id, int x, int y, String text, double scale) {
        get(id).add(new Text(x, y, text, scale, get(id).c));
    }

    public void drawImage(int id, String filename, int x, int y, int w, int h) {
        try {
            BufferedImage img = ImageIO.read(new File(filename));
            get(id).add(new ImageCmd(x, y, w, h, img));
        } catch (Exception e) {
            System.out.println("画像読み込み失敗: " + filename);
        }
    }

    public void getKey(int id, String varName, Environment env) {
        String key = get(id).getLastKey();
        double value = key.isEmpty() ? 0 : (double)key.charAt(0);
        env.set(varName, new Value(value));           // ← 修正
    }

    public void getMouseX(int id, String varName, Environment env) {
        env.set(varName, new Value(get(id).getMouseX()));   // ← 修正
    }

    public void getMouseY(int id, String varName, Environment env) {
        env.set(varName, new Value(get(id).getMouseY()));   // ← 修正
    }

    public void getMouseButton(int id, String varName, Environment env) {
        env.set(varName, new Value(get(id).getMouseButton())); // ← 修正
    }

    private void floodFill(BufferedImage img, int x, int y, Color newColor) {
        int width = img.getWidth();
        int height = img.getHeight();
        if (x < 0 || x >= width || y < 0 || y >= height) return;

        int targetColor = img.getRGB(x, y);
        int replacementColor = newColor.getRGB();
        if (targetColor == replacementColor) return;

        Queue<java.awt.Point> queue = new LinkedList<>();
        queue.add(new java.awt.Point(x, y));

        while (!queue.isEmpty()) {
            java.awt.Point p = queue.poll();
            int px = p.x, py = p.y;

            if (px < 0 || px >= width || py < 0 || py >= height || img.getRGB(px, py) != targetColor) 
                continue;

            img.setRGB(px, py, replacementColor);

            queue.add(new java.awt.Point(px + 1, py));
            queue.add(new java.awt.Point(px - 1, py));
            queue.add(new java.awt.Point(px, py + 1));
            queue.add(new java.awt.Point(px, py - 1));
        }
    }
}
