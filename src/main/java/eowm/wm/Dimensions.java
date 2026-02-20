package eowm.wm;

import eowm.util.Crypto;
import java.util.*;

public final class Dimensions {
  private Dimensions() {}

  public static Map<String, List<String>> dimDef(String ks, Map<String, List<String>> allDims, Map<String, Integer> N) {
    Map<String, List<String>> reduced = new HashMap<>();
    for (var e : allDims.entrySet()) {
      String name = e.getKey();
      List<String> vals = new ArrayList<>(e.getValue());
      int k = N.getOrDefault(name, vals.size());
      if (k <= 0 || k >= vals.size()) { reduced.put(name, vals); continue; }
      vals.sort(Comparator.comparingLong(v -> Crypto.prngLong(ks, "RND", name, v)));
      reduced.put(name, vals.subList(0, k));
    }
    return reduced;
  }

  public static List<List<String>> combine(Map<String, List<String>> reduced, List<String> order) {
    List<List<String>> combos = new ArrayList<>();
    combos.add(new ArrayList<>());
    for (String dim : order) {
      List<List<String>> next = new ArrayList<>();
      for (List<String> prefix : combos) {
        for (String v : reduced.get(dim)) {
          List<String> p2 = new ArrayList<>(prefix);
          p2.add(v);
          next.add(p2);
        }
      }
      combos = next;
    }
    return combos;
  }
}
