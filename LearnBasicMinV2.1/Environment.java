import java.util.*;

public class Environment {

    Environment parent;
    Map<String, Double> globals;
    Map<String, Value> vars = new HashMap<>();
    static Map<String, Value> staticVars = new HashMap<>();

    public Environment(Environment parent, Map<String, Double> globals) {
        this.parent = parent;
        this.globals = globals;
    }

    public Environment child() {
        return new Environment(this, globals);
    }

    public void setLocal(String k, double v) {
        vars.put(k, new Value(v));
    }

    public void declare(String name, Value value, boolean isStatic) {
        if (isStatic) {
            staticVars.put(name, value);
        } else {
            vars.put(name, value);
        }
    }

    public void set(String name, Value value) {
        if (vars.containsKey(name)) {
            vars.put(name, value);
            return;
        }
        if (parent != null) {
            parent.set(name, value);
            return;
        }
        staticVars.put(name, value);
    }

    // 新規追加：ネスト配列要素のセット
    @SuppressWarnings("unchecked")
    public void setNestedArray(String name, List<Integer> indices, Object value) {
        Value v = getValue(name);
        if (v == null || v.type != Value.Type.ARRAY) {
            List<Object> newArr = new ArrayList<>();
            set(name, new Value(newArr));
            v = getValue(name);
        }

        List<Object> current = v.asArray();
        for (int i = 0; i < indices.size() - 1; i++) {
            int idx = indices.get(i);
            while (current.size() <= idx) current.add(new ArrayList<>());
            Object next = current.get(idx);
            if (!(next instanceof List)) {
                next = new ArrayList<>();
                current.set(idx, next);
            }
            current = (List<Object>) next;
        }

        int lastIdx = indices.get(indices.size() - 1);
        while (current.size() <= lastIdx) current.add(0.0);
        current.set(lastIdx, value);
    }

    public Value getValue(String name) {
        if (vars.containsKey(name)) return vars.get(name);
        if (parent != null) return parent.getValue(name);
        return staticVars.get(name);
    }

    public double get(String name) {
        Value v = getValue(name);
        return v != null ? v.asNumber() : 0.0;
    }

    public List<Object> getArray(String name) {
        Value v = getValue(name);
        return (v != null && v.type == Value.Type.ARRAY) ? v.asArray() : null;
    }
}