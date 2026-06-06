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
