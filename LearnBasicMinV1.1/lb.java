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
        new Interpreter().runFile(args[0]);
    }
}

class Interpreter {
    private final Map<String, Double> globals = new HashMap<>();
    private final Map<String, FunctionDef> functions = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private final WindowManager windowManager = new WindowManager();

    void runFile(String path) throws IOException {
        List<String> lines = preprocess(Files.readAllLines(Path.of(path)));

        registerFunctions(lines);     // ← 重要
        executeLines(lines, new Environment(null));

        if (functions.containsKey("main")) {
            callFunction("main", 0.0);
        }
    }

    // ---------- 前処理 ----------
    private List<String> preprocess(List<String> raw) {
        List<String> out = new ArrayList<>();
        boolean inBlock = false;

        for (String line : raw) {
            String t = line.trim();
            if (t.startsWith("#-")) { inBlock = true; continue; }
            if (t.endsWith("-#")) { inBlock = false; continue; }
            if (inBlock || t.isEmpty() || t.startsWith("//")) continue;
            out.add(line);
        }
        return out;
    }

    // ---------- 関数登録 ----------
    private void registerFunctions(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String l = strip(lines.get(i));
            if (l.startsWith("function ")) {
                int end = findEnd(lines, i + 1);
                parseFunction(lines.subList(i, end + 1));
                i = end;
            }
        }
    }

    private void parseFunction(List<String> block) {
        String h = block.get(0).trim();
        int p1 = h.indexOf('(');
        int p2 = h.indexOf(')');
        String name = h.substring(8, p1).trim();
        String arg = h.substring(p1 + 1, p2).trim();

        List<String> body = new ArrayList<>(block.subList(1, block.size() - 1));
        functions.put(name, new FunctionDef(name, arg, body));
    }

    // ---------- 実行 ----------
    private void executeLines(List<String> lines, Environment env) {
        for (int i = 0; i < lines.size(); i++) {
            String line = strip(lines.get(i));
            if (line.isEmpty()) continue;

            if (line.startsWith("function ")) {
                i = findEnd(lines, i + 1);
            }
            else if (line.startsWith("if")) {
                int end = findEnd(lines, i + 1);
                execIf(lines.subList(i, end + 1), env);
                i = end;
            }
            else if (line.startsWith("while")) {
                int end = findEnd(lines, i + 1);
                execWhile(lines.subList(i, end + 1), env);
                i = end;
            }
            else {
                execStmt(line, env);
            }
        }
    }

    private void execIf(List<String> block, Environment env) {
        List<Branch> branches = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        String cond = extractCond(block.get(0));

        for (int i = 1; i < block.size() - 1; i++) {
            String l = strip(block.get(i));

            if (l.startsWith("else if")) {
                branches.add(new Branch(cond, new ArrayList<>(cur)));
                cur.clear();
                cond = extractCond(l);
            }
            else if (l.equals("else")) {
                branches.add(new Branch(cond, new ArrayList<>(cur)));
                cur.clear();
                cond = null;
            }
            else cur.add(block.get(i));
        }

        branches.add(new Branch(cond, cur));

        for (Branch b : branches) {
            if (b.cond == null || evalCond(b.cond, env)) {
                executeLines(b.body, env.child());
                return;
            }
        }
    }

    private void execWhile(List<String> block, Environment env) {
        String cond = extractCond(block.get(0));
        List<String> body = block.subList(1, block.size() - 1);

        while (evalCond(cond, env)) {
            executeLines(body, env.child());
        }
    }

    private void execStmt(String line, Environment env) {

        if (line.startsWith("var ")) {
            String[] p = line.substring(4).split("=", 2);
            env.setLocal(p[0].trim(), eval(p[1], env));
        }

        else if (line.startsWith("print ")) {
            String x = line.substring(6).trim();
            if (x.startsWith("\"")) System.out.println(x.replace("\"", ""));
            else System.out.println(trim(eval(x, env)));
        }

        else if (line.startsWith("input ")) {
            env.set(line.substring(6).trim(), scanner.nextDouble());
        }

        else if (line.startsWith("return ")) {
            throw new ReturnValue(eval(line.substring(7), env));
        }

        else if (line.startsWith("window.new(")) {
            List<String> a = split(line, 11);
            int id = windowManager.create((int)eval(a.get(1), env), (int)eval(a.get(2), env));
            env.set(a.get(0), id);
        }

        else if (line.startsWith("window.delete(")) {
            windowManager.delete((int)eval(inner(line), env));
        }

        else if (line.startsWith("setcolor(")) {
            List<String> a = split(line, 9);
            windowManager.setColor((int)eval(a.get(0), env),
                    (int)eval(a.get(1), env),
                    (int)eval(a.get(2), env),
                    (int)eval(a.get(3), env));
        }

        else if (line.startsWith("draw.line(")) {
            List<String> a = split(line, 10);
            windowManager.drawLine((int)eval(a.get(0), env),
                    (int)eval(a.get(1), env),
                    (int)eval(a.get(2), env),
                    (int)eval(a.get(3), env),
                    (int)eval(a.get(4), env));
        }

        else if (line.startsWith("draw.box(")) {
            List<String> a = split(line, 9);
            windowManager.drawBox((int)eval(a.get(0), env),
                    (int)eval(a.get(1), env),
                    (int)eval(a.get(2), env),
                    (int)eval(a.get(3), env),
                    (int)eval(a.get(4), env));
        }

        else if (line.startsWith("load(")) {
            try {
                executeLines(preprocess(Files.readAllLines(Path.of(inner(line)))), env);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        else if (line.contains("=") && !line.contains("==")) {
            String[] p = line.split("=", 2);
            env.set(p[0].trim(), eval(p[1], env));
        }

        else if (line.endsWith(")")) {
            callFunction(parseName(line), eval(inner(line), env));
        }
    }

    // ---------- 関数 ----------
    double callFunction(String name, double arg) {
        FunctionDef f = functions.get(name);
        if (f == null) throw new RuntimeException("No function: " + name);

        Environment env = new Environment(null);
        if (!f.arg.isEmpty()) env.setLocal(f.arg, arg);

        try {
            executeLines(f.body, env);
        } catch (ReturnValue r) {
            return r.value;
        }
        return 0;
    }

    // ---------- 式 ----------
    private double eval(String s, Environment env) {
        return new Expr(s, env, this).parse();
    }

    private boolean evalCond(String c, Environment e) {
        String[] ops = {">=", "<=", "==", "!=", ">", "<"};
        for (String op : ops) {
            int i = c.indexOf(op);
            if (i >= 0) {
                double l = eval(c.substring(0, i), e);
                double r = eval(c.substring(i + op.length()), e);
                return switch (op) {
                    case ">=" -> l >= r;
                    case "<=" -> l <= r;
                    case "==" -> l == r;
                    case "!=" -> l != r;
                    case ">" -> l > r;
                    default -> l < r;
                };
            }
        }
        return eval(c, e) != 0;
    }

    // ---------- util ----------
    private String strip(String s) {
        int i = s.indexOf("//");
        if (i >= 0) s = s.substring(0, i);
        return s.trim();
    }

    private int findEnd(List<String> l, int s) {
        int d = 1;
        for (int i = s; i < l.size(); i++) {
            String t = strip(l.get(i));
            if (t.startsWith("if") || t.startsWith("while") || t.startsWith("function")) d++;
            if (t.equals("end")) d--;
            if (d == 0) return i;
        }
        throw new RuntimeException("end not found");
    }

    private String extractCond(String l) {
        int a = l.indexOf('(');
        int b = l.lastIndexOf(')');
        return l.substring(a + 1, b).replace("then", "").trim();
    }

    private List<String> split(String line, int start) {
        return Arrays.asList(inner(line).split(","));
    }

    private String inner(String l) {
        return l.substring(l.indexOf('(') + 1, l.lastIndexOf(')'));
    }

    private String parseName(String l) {
        return l.substring(0, l.indexOf('(')).trim();
    }

    private String trim(double v) {
        return v == (long)v ? ""+(long)v : ""+v;
    }

    record FunctionDef(String name, String arg, List<String> body) {}
    record Branch(String cond, List<String> body) {}
    static class ReturnValue extends RuntimeException {
        double value; ReturnValue(double v){value=v;}
    }

    class Environment {
        Environment parent;
        Map<String, Double> vars = new HashMap<>();
        Environment(Environment p){ parent=p; }
        Environment child(){ return new Environment(this); }
        void setLocal(String k,double v){ vars.put(k,v); }
        void set(String k,double v){
            if(vars.containsKey(k)) vars.put(k,v);
            else if(parent!=null) parent.set(k,v);
            else globals.put(k,v);
        }
        double get(String k){
            if(vars.containsKey(k)) return vars.get(k);
            if(parent!=null) return parent.get(k);
            return globals.getOrDefault(k,0.0);
        }
    }
}

// ---------- 式 ----------
class Expr {
    String s; int p=0; Interpreter.Environment env; Interpreter in;
    Expr(String s, Interpreter.Environment e, Interpreter i){ this.s=s; env=e; in=i; }

    double parse(){ return expr(); }

    double expr(){
        double v=term();
        while(true){
            skip();
            if(match('+')) v+=term();
            else if(match('-')) v-=term();
            else return v;
        }
    }

    double term(){
        double v=factor();
        while(true){
            skip();
            if(match('*')) v*=factor();
            else if(match('/')) v/=factor();
            else return v;
        }
    }

double factor(){
    skip();

    if(match('(')){
        double v = expr();
        match(')');
        return v;
    }

    if(match('-')) return -factor();

    if(Character.isDigit(peek())) return number();

    String name = name();

    skip();

    // ここ追加（組み込み関数）
    if(name.equals("rand") && match('(')){
        double min = expr();
        match(',');
        double max = expr();
        match(')');
        return (int)min + new java.util.Random().nextInt((int)(max - min + 1));
    }

    // 通常の関数
    if(match('(')){
        double arg = 0;
        if(peek() != ')') arg = expr();
        match(')');
        return in.callFunction(name, arg);
    }

    return env.get(name);
}

    double number(){
        int s0=p;
        while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.')) p++;
        return Double.parseDouble(s.substring(s0,p));
    }

    String name(){
        int s0=p;
        while(p<s.length()&&!"+-*/() ".contains(""+s.charAt(p))) p++;
        return s.substring(s0,p);
    }

    char peek(){ return p<s.length()?s.charAt(p):'\0'; }
    boolean match(char c){ if(peek()==c){p++;return true;}return false;}
    void skip(){ while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++; }
}

// ---------- Window ----------
class WindowManager {
    int next=1;
    Map<Integer,DrawWindow> map=new HashMap<>();

    int create(int h,int w){
        int id=next++;
        map.put(id,new DrawWindow(id,h,w));
        return id;
    }

    DrawWindow get(int id){
        DrawWindow w=map.get(id);
        if(w==null) throw new RuntimeException("invalid window id");
        return w;
    }

    void delete(int id){ DrawWindow w=map.remove(id); if(w!=null) w.dispose(); }
    void setColor(int id,int r,int g,int b){ get(id).setColor(new Color(r,g,b)); }
    void drawLine(int id,int x1,int y1,int x2,int y2){ get(id).add(new Line(x1,y1,x2,y2,get(id).c)); }
    void drawBox(int id,int x1,int y1,int x2,int y2){ get(id).add(new Box(x1,y1,x2,y2,get(id).c)); }
}

class DrawWindow extends JFrame {
    Color c=Color.BLACK;
    java.util.List<ShapeCmd> list=new ArrayList<>();
    DrawWindow(int id,int h,int w){
        super("LB "+id);
        setSize(w,h);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(new Panel(list));
        setVisible(true);
    }
    void setColor(Color c){ this.c=c; }
    void add(ShapeCmd s){ list.add(s); repaint(); }
}

class Panel extends JPanel {
    java.util.List<ShapeCmd> list;
    Panel(java.util.List<ShapeCmd> l){ list=l; }
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        for(ShapeCmd s:list) s.draw((Graphics2D)g);
    }
}

interface ShapeCmd { void draw(Graphics2D g); }

record Line(int x1,int y1,int x2,int y2,Color c) implements ShapeCmd {
    public void draw(Graphics2D g){ g.setColor(c); g.drawLine(x1,y1,x2,y2); }
}

record Box(int x1,int y1,int x2,int y2,Color c) implements ShapeCmd {
    public void draw(Graphics2D g){
        g.setColor(c);
        g.drawRect(Math.min(x1,x2),Math.min(y1,y2),
                Math.abs(x2-x1),Math.abs(y2-y1));
    }
}
