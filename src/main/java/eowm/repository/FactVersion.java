package eowm.repository;

import java.util.*;

public final class FactVersion {
  public Object payload;
  public final Map<String, Object> meta;
  public final Map<String, Object> flags;

  public FactVersion(Object payload, Map<String, Object> meta, Map<String, Object> flags) {
    this.payload = payload;
    this.meta = meta;
    this.flags = flags;
  }

  public boolean isNumeric() {
    return (payload instanceof Integer) || (payload instanceof Long) || (payload instanceof Double) || (payload instanceof Float);
  }

  public double asDouble() {
    if (payload instanceof Integer i) return i.doubleValue();
    if (payload instanceof Long l) return l.doubleValue();
    if (payload instanceof Float f) return f.doubleValue();
    if (payload instanceof Double d) return d;
    throw new IllegalStateException("Not numeric payload: " + payload);
  }

  public void setDouble(double v) { this.payload = v; }
}
