import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Interpreter {

    Map<String, Double> globals = new HashMap<>(); // 後方互換用
    Map<String, FunctionDef> functions = new HashMap<>();
    Scanner scanner = new Scanner(System.in);
    WindowManager windowManager = new WindowManager();

    public void runFile(String path) throws IOException {
        List<String> lines = preprocess(Files.readAllLines(Path.of(path)));
        registerFunctions(lines);
        executeLines(lines, new Environment(null, globals));
        if (functions.containsKey("main")) {
            callFunction("main", List.of());
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

    // ---------- 関数 ----------
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
        String argStr = h.substring(p1 + 1, p2).trim();
        List<String> args = new ArrayList<>();
        if (!argStr.isEmpty()) {
            for (String a : argStr.split(",")) args.add(a.trim());
        }
        List<String> body = new ArrayList<>(block.subList(1, block.size() - 1));
        functions.put(name, new FunctionDef(name, args, body));
    }

    public double callFunction(String name, List<Double> args) {
        FunctionDef f = functions.get(name);
        if (f == null) throw new RuntimeException("No function: " + name);

        Environment env = new Environment(null, globals);
        for (int i = 0; i < f.args().size(); i++) {
            double v = (i < args.size()) ? args.get(i) : 0;
            env.setLocal(f.args().get(i), v);  // 後方互換
        }
        try {
            executeLines(f.body(), env);
        } catch (ReturnValue r) {
            return r.value;
        }
        return 0;
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

            // ==================== 型推論付き宣言 ====================
            else if (line.startsWith("let.local ") || line.startsWith("var ")) {
                String rest = line.startsWith("let.local ") ? line.substring(10) : line.substring(4);
                declareWithInference(rest, env, false);
            }
            else if (line.startsWith("let.global ")) {
                declareWithInference(line.substring(11), env, false);
            }
            else if (line.startsWith("let.static ")) {
                declareWithInference(line.substring(11), env, true);
            }

            else if (line.startsWith("print ")) {
                String x = line.substring(6).trim();
                if (x.startsWith("\"")) {
                    System.out.print(x.replace("\"", ""));
                } else {
                    System.out.print(getPrintValue(x, env));
                }
            }
            else if (line.startsWith("println ")) {
                String x = line.substring(8).trim();
                if (x.startsWith("\"")) {
                    System.out.println(x.replace("\"", ""));
                } else {
                    System.out.println(getPrintValue(x, env));
                }
            }
            else if (line.startsWith("input ")) {
                env.set(line.substring(6).trim(), new Value(scanner.nextDouble()));
            }
            else if (line.startsWith("return ")) {
                throw new ReturnValue(eval(line.substring(7), env));
            }

            // ========== ウィンドウ関連 ==========
            else if (line.startsWith("window.new(")) {
                List<String> a = split(inner(line));
                int id = windowManager.create((int)eval(a.get(1), env), (int)eval(a.get(2), env));
                env.set(a.get(0), new Value((double)id));
            }
            else if (line.startsWith("window.delete(")) {
                windowManager.delete((int)eval(inner(line), env));
            }
            else if (line.startsWith("window.name(")) {
                List<String> a = split(inner(line));
                windowManager.windowName((int)eval(a.get(0), env), a.get(1).replace("\"", "").trim());
            }
            else if (line.startsWith("window.size(")) {
                List<String> a = split(inner(line));
                windowManager.windowSize(
                    (int)eval(a.get(0), env),
                    (int)eval(a.get(1), env),
                    (int)eval(a.get(2), env)
                );
            }
            else if (line.startsWith("setcolor(") || line.startsWith("draw.color(")) {
                List<String> a = split(inner(line));
                windowManager.setColor(
                        (int)eval(a.get(0), env),
                        (int)eval(a.get(1), env),
                        (int)eval(a.get(2), env),
                        (int)eval(a.get(3), env)
                );
            }

            // ========== 描画関連 ==========
            else if (line.startsWith("draw.line(")) {
                List<String> a = split(inner(line));
                windowManager.drawLine(
                        (int)eval(a.get(0), env),
                        (int)eval(a.get(1), env),
                        (int)eval(a.get(2), env),
                        (int)eval(a.get(3), env),
                        (int)eval(a.get(4), env)
                );
            }
            else if (line.startsWith("draw.box(")) {
                List<String> a = split(inner(line));
                windowManager.drawBox(
                        (int)eval(a.get(0), env),
                        (int)eval(a.get(1), env),
                        (int)eval(a.get(2), env),
                        (int)eval(a.get(3), env),
                        (int)eval(a.get(4), env)
                );
            }
            else if (line.startsWith("draw.put(")) {
                List<String> a = split(inner(line));
                windowManager.drawPut(
                        (int)eval(a.get(0), env),
                        (int)eval(a.get(1), env),
                        (int)eval(a.get(2), env)
                );
            }
            else if (line.startsWith("draw.circle(")) {
                List<String> a = split(inner(line));
                windowManager.drawCircle(
                        (int)eval(a.get(0), env),
                        (int)eval(a.get(1), env),
                        (int)eval(a.get(2), env),
                        (int)eval(a.get(3), env)
                );
            }
            else if (line.startsWith("draw.tri(")) {
                List<String> a = split(inner(line));
                windowManager.drawTri(
                        (int)eval(a.get(0), env),
                        (int)eval(a.get(1), env),
                        (int)eval(a.get(2), env),
                        (int)eval(a.get(3), env),
                        (int)eval(a.get(4), env),
                        (int)eval(a.get(5), env),
                        (int)eval(a.get(6), env)
                );
            }
            else if (line.startsWith("draw.cls(")) {
                windowManager.drawCls((int)eval(inner(line), env));
            }
            else if (line.startsWith("draw.fill(")) {
                List<String> a = split(inner(line));
                windowManager.drawFill(
                        (int)eval(a.get(0), env),
                        (int)eval(a.get(1), env),
                        (int)eval(a.get(2), env)
                );
            }
            else if (line.startsWith("draw.text(")) {
                List<String> a = split(inner(line));
                int id = (int)eval(a.get(0), env);
                int x = (int)eval(a.get(1), env);
                int y = (int)eval(a.get(2), env);
                double scale = (a.size() > 4) ? eval(a.get(4), env) : 1.0;
                String textPart = a.get(3).trim();
                String text = textPart.startsWith("\"") ? textPart.replace("\"", "") : getPrintValue(textPart, env);
                windowManager.drawText(id, x, y, text, scale);
            }
            else if (line.startsWith("draw.image(")) {
                List<String> a = split(inner(line));
                int winId = (int)eval(a.get(0), env);
                String filename = a.get(1).replace("\"", "").trim();
                int x = (int)eval(a.get(2), env);
                int y = (int)eval(a.get(3), env);
                int w = (a.size() > 5) ? (int)eval(a.get(4), env) : 0;
                int h = (a.size() > 5) ? (int)eval(a.get(5), env) : 0;
                windowManager.drawImage(winId, filename, x, y, w, h);
            }

            // ========== 入力関連 ==========
            else if (line.startsWith("window.getkey(")) {
                List<String> a = split(inner(line));
                windowManager.getKey((int)eval(a.get(0), env), a.get(1).trim(), env);
            }
            else if (line.startsWith("window.mousex(")) {
                List<String> a = split(inner(line));
                windowManager.getMouseX((int)eval(a.get(0), env), a.get(1).trim(), env);
            }
            else if (line.startsWith("window.mousey(")) {
                List<String> a = split(inner(line));
                windowManager.getMouseY((int)eval(a.get(0), env), a.get(1).trim(), env);
            }
            else if (line.startsWith("window.mousebutton(")) {
                List<String> a = split(inner(line));
                windowManager.getMouseButton((int)eval(a.get(0), env), a.get(1).trim(), env);
            }

            // ========== array 操作 ==========
            else if (line.startsWith("array.push(")) {
                List<String> a = split(inner(line));
                String arrName = a.get(0).trim();
                Value val = parseValue(a.get(1), env);
                List<Object> arr = env.getArray(arrName);
                if (arr != null) arr.add(val.data);
            }
            else if (line.startsWith("array.pop(")) {
                List<String> a = split(inner(line));
                String arrName = a.get(0).trim();
                String target = a.get(1).trim();
                List<Object> arr = env.getArray(arrName);
                if (arr != null && !arr.isEmpty()) {
                    Object val = arr.remove(arr.size() - 1);
                    env.set(target, new Value(val));
                }
            }

            // ========== wait / load ==========
            else if (line.startsWith("wait ")) {
                try {
                    int ms = (int)eval(line.substring(5).trim(), env);
                    if (ms > 0) Thread.sleep(ms);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            else if (line.startsWith("load(")) {
                try {
                    List<String> l = preprocess(Files.readAllLines(Path.of(inner(line))));
                    registerFunctions(l);
                    executeLines(l, env);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // ========== 代入（型推論対応） ==========
            else if (line.contains("=") && !line.contains("==")) {
                String[] p = line.split("=", 2);
                String name = p[0].trim();
                Value value = parseValue(p[1].trim(), env);
                env.set(name, value);
            }
            else if (line.endsWith(")")) {
                callFunction(parseName(line), parseArgs(inner(line), env));
            }
        }
    }

    private void declareWithInference(String rest, Environment env, boolean isStatic) {
        String[] p = rest.split("=", 2);
        String name = p[0].trim();
        Value value = (p.length > 1) ? parseValue(p[1].trim(), env) : new Value(0.0);
        env.declare(name, value, isStatic);
    }

    private Value parseValue(String exprStr, Environment env) {
        if (exprStr.startsWith("\"")) {
            return new Value(exprStr.replace("\"", ""));
        }
        if (exprStr.startsWith("[")) {
            return new Value(parseArrayLiteral(exprStr));
        }
        try {
            return new Value(Double.parseDouble(exprStr));
        } catch (Exception e) {
            return new Value(eval(exprStr, env));
        }
    }

    private List<Object> parseArrayLiteral(String s) {
        List<Object> list = new ArrayList<>();
        String content = s.substring(1, s.length() - 1);
        for (String token : content.split(",")) {
            token = token.trim();
            if (token.isEmpty()) continue;
            try {
                list.add(Double.parseDouble(token));
            } catch (Exception e) {
                list.add(token.replace("\"", ""));
            }
        }
        return list;
    }

    private String getPrintValue(String expr, Environment env) {
        Value v = null;
        try {
            v = env.getValue(expr);
        } catch (Exception ignored) {}
        if (v == null) {
            double num = eval(expr, env);
            return new Value(num).toString();
        }
        return v.toString();
    }

    // ---------- if / while ----------
    void execWhile(List<String> block, Environment env) {
        String cond = extractCond(block.get(0));
        List<String> body = block.subList(1, block.size() - 1);
        while (evalCond(cond, env)) {
            executeLines(body, env.child());
        }
    }

    void execIf(List<String> block, Environment env) {
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

    boolean evalCond(String c, Environment e) {
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

    private String inner(String l) {
        return l.substring(l.indexOf('(') + 1, l.lastIndexOf(')'));
    }

    private String parseName(String l) {
        return l.substring(0, l.indexOf('(')).trim();
    }

    private List<Double> parseArgs(String s, Environment env) {
        List<Double> list = new ArrayList<>();
        if (s.isEmpty()) return list;
        String[] parts = s.split(",");
        for (String p : parts) list.add(eval(p, env));
        return list;
    }

    private List<String> split(String s) {
        return Arrays.asList(s.split(","));
    }

    private double eval(String s, Environment env) {
        return new Expr(s, env, this).parse();
    }

    record Branch(String cond, List<String> body) {}

    static class ReturnValue extends RuntimeException {
        double value;
        ReturnValue(double v){ value=v; }
    }
}