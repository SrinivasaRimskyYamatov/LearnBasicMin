import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class lb {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java lb <file.lb>");
            return;
        }
        Interpreter interpreter = new Interpreter();
        interpreter.runFile(args[0]);
    }
}

class Interpreter {
    private final Map<String, Double> globals = new HashMap<>();
    private final Map<String, FunctionDef> functions = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private final WindowManager windowManager = new WindowManager();

    void runFile(String path) throws IOException {
        executeLines(preprocess(Files.readAllLines(Path.of(path))), new Environment(null));
        if (functions.containsKey("main")) {
            callFunction("main", 0.0, new Environment(null));
        }
    }

    private List<String> preprocess(List<String> raw) {
        List<String> out = new ArrayList<>();
        boolean inBlockComment = false;
        for (String line : raw) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#-")) { inBlockComment = true; continue; }
            if (trimmed.endsWith("-#")) { inBlockComment = false; continue; }
            if (inBlockComment || trimmed.isEmpty() || trimmed.startsWith("//")) continue;
            out.add(line);
        }
        return out;
    }

    private void executeLines(List<String> lines, Environment env) {
        for (int i = 0; i < lines.size(); i++) {
            String line = stripInlineComment(lines.get(i)).trim();
            if (line.isEmpty()) continue;

            if (line.matches("^function\s+.+")) {
                int end = findMatchingEnd(lines, i + 1);
                parseFunction(lines.subList(i, end + 1));
                i = end;
            } else if (line.matches("^if\\s*\\(.*")) {
                int end = findMatchingEnd(lines, i + 1);
                executeIf(lines.subList(i, end + 1), env);
                i = end;
            } else if (line.matches("^while\\s*\\(.*")) {
                int end = findMatchingEnd(lines, i + 1);
                executeWhile(lines.subList(i, end + 1), env);
                i = end;
            } else {
                executeStatement(line, env);
            }
        }
    }

    private String stripInlineComment(String line) {
        int p = line.indexOf("//");
        return p >= 0 ? line.substring(0, p) : line;
    }

    private int findMatchingEnd(List<String> lines, int start) {
        int depth = 1;
        for (int i = start; i < lines.size(); i++) {
            String s = stripInlineComment(lines.get(i)).trim();
            if (s.matches("^if\\s*\\(.*") || s.matches("^while\\s*\\(.*") || s.matches("^function\\s+.+")) depth++;
            if (s.equals("end")) depth--;
            if (depth == 0) return i;
        }
        throw new RuntimeException("'end' not found");
    }

    private void parseFunction(List<String> block) {
        String header = block.get(0).trim();
        int nameStart = "function".length();
        int parenOpen = header.indexOf('(', nameStart);
        int parenClose = header.indexOf(')', parenOpen);
        String name = header.substring(nameStart, parenOpen).trim();
        String arg = header.substring(parenOpen + 1, parenClose).trim();
        List<String> body = new ArrayList<>(block.subList(1, block.size() - 1));
        functions.put(name, new FunctionDef(name, arg, body));
    }

    private void executeIf(List<String> block, Environment env) {
        List<Branch> branches = new ArrayList<>();
        List<String> curBody = new ArrayList<>();
        String curCond = block.get(0).trim();
        curCond = extractParenCondition(curCond, "if");

        for (int i = 1; i < block.size() - 1; i++) {
            String t = stripInlineComment(block.get(i)).trim();
            if (t.matches("^else\\s+if\\s*\\(.*")) {
                branches.add(new Branch(curCond, new ArrayList<>(curBody)));
                curBody.clear();
                curCond = extractParenCondition(t, "else if");
            } else if (t.equals("else")) {
                branches.add(new Branch(curCond, new ArrayList<>(curBody)));
                curBody.clear();
                curCond = null;
            } else {
                curBody.add(block.get(i));
            }
        }
        branches.add(new Branch(curCond, curBody));

        for (Branch b : branches) {
            if (b.cond == null || evaluateCondition(b.cond, env)) {
                executeLines(b.body, env.child());
                break;
            }
        }
    }

    private void executeWhile(List<String> block, Environment env) {
        String line = block.get(0).trim();
        String cond = extractParenCondition(line, "while");
        List<String> body = block.subList(1, block.size() - 1);
        while (evaluateCondition(cond, env)) {
            executeLines(body, env.child());
        }
    }


    private String extractParenCondition(String line, String keyword) {
        int open = line.indexOf('(');
        int close = line.lastIndexOf(')');
        if (open < 0 || close < 0 || close <= open) throw new RuntimeException("Invalid condition syntax: " + line);
        return line.substring(open + 1, close).trim();
    }
    private void executeStatement(String line, Environment env) {
        if (line.startsWith("var ")) {
            String rest = line.substring(4).trim();
            String[] parts = rest.split("=", 2);
            env.setLocal(parts[0].trim(), evalExpr(parts[1].trim(), env));
        } else if (line.startsWith("print ")) {
            String x = line.substring(6).trim();
            if (x.startsWith("\"") && x.endsWith("\"")) System.out.println(x.substring(1, x.length() - 1));
            else System.out.println(trimDouble(evalExpr(x, env)));
        } else if (line.startsWith("input ")) {
            env.set(line.substring(6).trim(), scanner.nextDouble());
        } else if (line.startsWith("return ")) {
            throw new ReturnValue(evalExpr(line.substring(7).trim(), env));
        } else if (line.startsWith("window.new(")) {
            String in = line.substring(11, line.length() - 1);
            List<String> a = splitArgs(in);
            int id = windowManager.create((int)evalExpr(a.get(1), env), (int)evalExpr(a.get(2), env));
            env.set(a.get(0).trim(), id);
        } else if (line.startsWith("window.delete(")) {
            int id = (int) evalExpr(line.substring(14, line.length() - 1), env);
            windowManager.delete(id);
        } else if (line.startsWith("setcolor(")) {
            List<String> a = splitArgs(line.substring(9, line.length() - 1));
            windowManager.setColor((int)evalExpr(a.get(0), env), (int)evalExpr(a.get(1), env), (int)evalExpr(a.get(2), env), (int)evalExpr(a.get(3), env));
        } else if (line.startsWith("draw.line(")) {
            List<String> a = splitArgs(line.substring(10, line.length() - 1));
            windowManager.drawLine((int)evalExpr(a.get(0), env), (int)evalExpr(a.get(1), env), (int)evalExpr(a.get(2), env), (int)evalExpr(a.get(3), env), (int)evalExpr(a.get(4), env));
        } else if (line.startsWith("draw.box(")) {
            List<String> a = splitArgs(line.substring(9, line.length() - 1));
            windowManager.drawBox((int)evalExpr(a.get(0), env), (int)evalExpr(a.get(1), env), (int)evalExpr(a.get(2), env), (int)evalExpr(a.get(3), env), (int)evalExpr(a.get(4), env));
        } else if (line.startsWith("load(")) {
            String file = line.substring(5, line.length() - 1).trim().replace("\"", "");
            try { executeLines(preprocess(Files.readAllLines(Path.of(file))), env); } catch (IOException e) { throw new RuntimeException(e); }
        } else if (line.contains("=") && !line.contains("==")) {
            String[] p = line.split("=", 2);
            env.set(p[0].trim(), evalExpr(p[1].trim(), env));
        } else if (line.endsWith(")") && line.contains("(")) {
            int op = line.indexOf('(');
            String name = line.substring(0, op).trim();
            String inside = line.substring(op + 1, line.length() - 1).trim();
            double arg = inside.isEmpty() ? 0.0 : evalExpr(inside, env);
            callFunction(name, arg, env);
        }
    }

    private double callFunction(String name, double arg, Environment caller) {
        FunctionDef f = functions.get(name);
        if (f == null) throw new RuntimeException("No function: " + name);
        Environment local = new Environment(caller);
        if (!f.argName.isEmpty()) local.setLocal(f.argName, arg);
        try { executeLines(f.body, local); } catch (ReturnValue rv) { return rv.value; }
        return 0.0;
    }

    private boolean evaluateCondition(String cond, Environment env) {
        String[] ops = {">=", "<=", "==", "!=", ">", "<"};
        for (String op : ops) {
            int i = cond.indexOf(op);
            if (i >= 0) {
                double l = evalExpr(cond.substring(0, i).trim(), env);
                double r = evalExpr(cond.substring(i + op.length()).trim(), env);
                return switch (op) {
                    case ">=" -> l >= r; case "<=" -> l <= r; case "==" -> l == r;
                    case "!=" -> l != r; case ">" -> l > r; default -> l < r;
                };
            }
        }
        return evalExpr(cond, env) != 0;
    }

    private double evalExpr(String expr, Environment env) {
        expr = expr.trim();
        if (expr.startsWith("rand(")) {
            List<String> a = splitArgs(expr.substring(5, expr.length() - 1));
            int min = (int) evalExpr(a.get(0), env), max = (int) evalExpr(a.get(1), env);
            return min + new Random().nextInt(max - min + 1);
        }
        return new ExpressionParser(expr, env).parse();
    }

    private String trimDouble(double v) {
        return (v == Math.rint(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private List<String> splitArgs(String s) {
        List<String> out = new ArrayList<>();
        int depth = 0; StringBuilder cur = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == ',' && depth == 0) { out.add(cur.toString().trim()); cur = new StringBuilder(); continue; }
            if (c == '(') depth++; if (c == ')') depth--;
            cur.append(c);
        }
        out.add(cur.toString().trim());
        return out;
    }

    record FunctionDef(String name, String argName, List<String> body) {}
    record Branch(String cond, List<String> body) {}

    static class ReturnValue extends RuntimeException { final double value; ReturnValue(double v){value=v;} }

    class Environment {
        private final Environment parent;
        private final Map<String, Double> local = new HashMap<>();
        Environment(Environment p){ parent = p; }
        Environment child(){ return new Environment(this); }
        void setLocal(String n,double v){ local.put(n,v); }
        void set(String n,double v){ if(local.containsKey(n)) local.put(n,v); else if(parent!=null && parent.has(n)) parent.set(n,v); else globals.put(n,v); }
        boolean has(String n){ return local.containsKey(n) || (parent!=null && parent.has(n)) || globals.containsKey(n); }
        double get(String n){ if(local.containsKey(n)) return local.get(n); if(parent!=null && parent.has(n)) return parent.get(n); return globals.getOrDefault(n,0.0); }
    }
}

class ExpressionParser {
    private final String s; private int pos = 0; private final Interpreter.Environment env;
    ExpressionParser(String s, Interpreter.Environment env){ this.s=s; this.env=env; }
    double parse(){ double v = expr(); skip(); if(pos!=s.length()) throw new RuntimeException("Bad expr: "+s); return v; }
    double expr(){ double v=term(); while(true){ skip(); if(match('+')) v+=term(); else if(match('-')) v-=term(); else return v; } }
    double term(){ double v=factor(); while(true){ skip(); if(match('*')) v*=factor(); else if(match('/')) v/=factor(); else return v; } }
    double factor(){ skip(); if(match('(')){ double v=expr(); expect(')'); return v; } if(match('-')) return -factor();
        if(Character.isDigit(peek())||peek()=='.') return number(); return variable(); }
    double number(){ int st=pos; while(pos<s.length()&&(Character.isDigit(s.charAt(pos))||s.charAt(pos)=='.')) pos++; return Double.parseDouble(s.substring(st,pos)); }
    double variable(){ int st=pos; while(pos<s.length()&&!Character.isWhitespace(s.charAt(pos))&&"+-*/()".indexOf(s.charAt(pos))<0) pos++; String n=s.substring(st,pos); return env.get(n); }
    char peek(){ return pos<s.length()?s.charAt(pos):'\0'; }
    boolean match(char c){ if(peek()==c){pos++; return true;} return false; }
    void expect(char c){ if(!match(c)) throw new RuntimeException("Expected "+c); }
    void skip(){ while(pos<s.length()&&Character.isWhitespace(s.charAt(pos))) pos++; }
}

class WindowManager {
    private int nextId = 1;
    private final Map<Integer, DrawWindow> windows = new HashMap<>();

    int create(int h, int w) {
        int id = nextId++;
        DrawWindow dw = new DrawWindow(id, h, w);
        windows.put(id, dw);
        return id;
    }
    void delete(int id){ DrawWindow w=windows.remove(id); if(w!=null) w.dispose(); }
    void setColor(int id, int r, int g, int b){ windows.get(id).setColor(new Color(clamp(r), clamp(g), clamp(b))); }
    void drawLine(int id, int x1, int y1, int x2, int y2){ windows.get(id).addShape(new LineShape(x1,y1,x2,y2, windows.get(id).getCurrentColor())); }
    void drawBox(int id, int x1, int y1, int x2, int y2){ windows.get(id).addShape(new BoxShape(x1,y1,x2,y2, windows.get(id).getCurrentColor())); }
    int clamp(int x){ return Math.max(0, Math.min(255, x)); }
}

class DrawWindow extends JFrame {
    private Color currentColor = Color.BLACK;
    private final List<ShapeCmd> shapes = new ArrayList<>();
    DrawWindow(int id, int h, int w) {
        super("LB Window " + id);
        setSize(w, h);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
        setContentPane(new DrawPanel(shapes));
        setVisible(true);
    }
    void setColor(Color c){ currentColor = c; }
    Color getCurrentColor(){ return currentColor; }
    void addShape(ShapeCmd cmd){ shapes.add(cmd); repaint(); }
}

class DrawPanel extends JPanel {
    private final List<ShapeCmd> shapes;
    DrawPanel(List<ShapeCmd> shapes){ this.shapes = shapes; }
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        for (ShapeCmd s : shapes) s.draw((Graphics2D) g);
    }
}

interface ShapeCmd { void draw(Graphics2D g); }
record LineShape(int x1,int y1,int x2,int y2,Color c) implements ShapeCmd { public void draw(Graphics2D g){ g.setColor(c); g.drawLine(x1,y1,x2,y2);} }
record BoxShape(int x1,int y1,int x2,int y2,Color c) implements ShapeCmd { public void draw(Graphics2D g){ g.setColor(c); g.drawRect(Math.min(x1,x2),Math.min(y1,y2),Math.abs(x2-x1),Math.abs(y2-y1));} }
