package eowm.cli;

import java.util.*;

public final class Args {
  private final Map<String,String> m = new HashMap<>();
  private final List<String> pos = new ArrayList<>();

  public Args(String[] argv) {
    for (int i = 0; i < argv.length; i++) {
      String a = argv[i];
      if (a.startsWith("--")) {
        String k = a.substring(2);
        String v = (i + 1 < argv.length && !argv[i+1].startsWith("--")) ? argv[++i] : "true";
        m.put(k, v);
      } else {
        pos.add(a);
      }
    }
  }

  public String cmd() { return pos.isEmpty() ? "" : pos.get(0); }
  public String get(String k, String def) { return m.getOrDefault(k, def); }
  public int getInt(String k, int def) {
    String v = m.get(k);
    return (v == null) ? def : Integer.parseInt(v);
  }
}
