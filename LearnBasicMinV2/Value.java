import java.util.*;

public class Value {
    public final Object data;
    public final Type type;

    public enum Type { NUMBER, STRING, ARRAY }

    public Value(Object data) {
        this.data = data;
        if (data instanceof List) this.type = Type.ARRAY;
        else if (data instanceof String) this.type = Type.STRING;
        else this.type = Type.NUMBER;
    }

    public double asNumber() {
        if (data instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    public String asString() {
        if (data instanceof String s) return s;
        return data != null ? data.toString() : "";
    }

    @SuppressWarnings("unchecked")
    public List<Object> asArray() {
        if (data instanceof List) return (List<Object>) data;
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        if (type == Type.STRING) return (String) data;
        if (type == Type.ARRAY) return data.toString();
        return String.valueOf(asNumber());
    }
}
