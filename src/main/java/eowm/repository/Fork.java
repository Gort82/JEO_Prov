package eowm.repository;

import java.util.*;

/**
 * Paper Fig. 15: fork node stores the fact plus authenticity hashes C/S/R.
 */
public final class Fork {
  public final Fact fact;
  public String C = "";
  public String S = "";
  public String R = "";

  public Fork(Fact fact) { this.fact = fact; }

  public Map<String,String> digests() {
    return Map.of("C", C, "S", S, "R", R);
  }
}
