import java.util.Map;

public class Environment {

    Environment parent;
    Map<String, Double> globals;
    java.util.Map<String, Double> vars = new java.util.HashMap<>();

    public Environment(Environment parent, Map<String, Double> globals) {
        this.parent = parent;
        this.globals = globals;
    }

    public Environment child() {
        return new Environment(this, globals);
    }

    public void setLocal(String k, double v) { vars.put(k, v); }

    public void set(String k, double v) {
        if (vars.containsKey(k)) vars.put(k, v);
        else if (parent != null) parent.set(k, v);
        else globals.put(k, v);
    }

    public double get(String k) {
        if (vars.containsKey(k)) return vars.get(k);
        if (parent != null) return parent.get(k);
        return globals.getOrDefault(k, 0.0);
    }
}