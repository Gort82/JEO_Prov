package eowm.repository;

import java.util.*;

public final class Cell {
  public final List<String> dims;
  public final IOBlock BI = new IOBlock();
  public final IOBlock BO = new IOBlock();

  // Fig. 15: forks per fact-id
  public final Map<String, Fork> forks = new HashMap<>();

  // BI/BO authenticity binding via R in a reserved slot (see paper rationale)
  public String cellR = "";

  public Cell(List<String> dims) { this.dims = dims; }

  public Fork getOrCreateFork(String factId) {
    return forks.computeIfAbsent(factId, id -> new Fork(new Fact(id)));
  }
}
