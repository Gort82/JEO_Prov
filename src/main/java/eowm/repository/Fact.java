package eowm.repository;

import java.util.*;

public final class Fact {
  public final String factId;
  public final List<FactVersion> versions = new ArrayList<>();

  public Fact(String factId) { this.factId = factId; }
}
