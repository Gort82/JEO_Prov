package eowm.wm;

import java.util.*;

public final class Scoring {
  private Scoring() {}
  public static double hammingSimilarity(List<Integer> a, List<Integer> b) {
    if (a.size() != b.size()) throw new IllegalArgumentException("length mismatch");
    if (a.isEmpty()) return 1.0;
    int eq = 0;
    for (int i = 0; i < a.size(); i++) if (Objects.equals(a.get(i), b.get(i))) eq++;
    return ((double)eq) / a.size();
  }
}
