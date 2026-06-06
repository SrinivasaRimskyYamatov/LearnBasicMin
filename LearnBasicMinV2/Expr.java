import java.util.*;

public class Expr {

    String s;
    int p = 0;
    Environment env;
    Interpreter in;

    public Expr(String s, Environment env, Interpreter in) {
        this.s = s;
        this.env = env;
        this.in = in;
    }

    public double parse() { return expr(); }

    double expr() {
        double v = term();
        while (true) {
            skip();
            if (match('+')) v += term();
            else if (match('-')) v -= term();
            else return v;
        }
    }

    double term() {
        double v = factor();
        while (true) {
            skip();
            if (match('*')) v *= factor();
            else if (match('/')) v /= factor();
            else return v;
        }
    }

    double factor() {
        skip();

        if (match('(')) {
            double v = expr();
            match(')');
            return v;
        }

        if (match('-')) return -factor();

        if (Character.isDigit(peek())) return number();

        String name = name();
        skip();

        // 配列アクセス arr[0]、arr[1][2] 対応
        if (peek() == '[') {
            List<Integer> indices = parseIndices();
            List<Object> arr = env.getArray(name);
            if (arr != null) {
                return getNestedValue(arr, indices);
            }
            return 0.0;
        }

        if (name.equals("rand") && match('(')) {
            double min = expr();
            match(',');
            double max = expr();
            match(')');
            return (int)min + new java.util.Random().nextInt((int)(max - min + 1));
        }

        if (match('(')) {
            java.util.List<Double> args = new java.util.ArrayList<>();
            if (peek() != ')') {
                args.add(expr());
                while (match(',')) args.add(expr());
            }
            match(')');
            return in.callFunction(name, args);
        }

        return env.get(name);
    }

    private List<Integer> parseIndices() {
        List<Integer> indices = new ArrayList<>();
        while (match('[')) {
            double idx = expr();
            indices.add((int)idx);
            match(']');
        }
        return indices;
    }
    @SuppressWarnings("unchecked")
    private double getNestedValue(List<Object> arr, List<Integer> indices) {
        Object current = arr;
        for (int i = 0; i < indices.size(); i++) {
            int idx = indices.get(i);
            if (current instanceof List) {
                List<Object> list = (List<Object>) current;
                if (idx >= 0 && idx < list.size()) {
                    current = list.get(idx);
                } else {
                    return 0.0;
                }
            } else {
                return 0.0;
            }
        }
        return current instanceof Double ? (Double) current : 0.0;
    }

    double number() {
        int s0 = p;
        while (p < s.length() && (Character.isDigit(s.charAt(p)) || s.charAt(p)=='.')) p++;
        return Double.parseDouble(s.substring(s0, p));
    }

    String name() {
        int s0 = p;
        while (p < s.length() && !"+-*/(),[] ".contains(""+s.charAt(p))) p++;
        return s.substring(s0, p);
    }

    char peek() { return p < s.length() ? s.charAt(p) : '\0'; }
    boolean match(char c) { if (peek()==c){ p++; return true; } return false; }
    void skip() { while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++; }
}