package eowm.repository;

import java.util.*;

public final class CubeRepository {
  private final Map<String, Cell> cells = new HashMap<>();

  public Cell getOrCreateCell(List<String> dims) {
    String key = String.join("|", dims);
    return cells.computeIfAbsent(key, k -> new Cell(new ArrayList<>(dims)));
  }

  public Cell getCell(List<String> dims) {
    return cells.get(String.join("|", dims));
  }

  public Collection<Cell> allCells() { return cells.values(); }

  public void upsertFactVersion(List<String> dims, String factId, FactVersion fv) {
    Cell cell = getOrCreateCell(dims);
    Fork fork = cell.getOrCreateFork(factId);
    fork.fact.versions.add(fv);
  }
}
