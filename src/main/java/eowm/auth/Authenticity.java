package eowm.auth;

import eowm.repository.*;
import eowm.util.Crypto;
import eowm.wm.EmbedConfig;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Authenticity {
  private Authenticity() {}

  private static String HC(String msg) { return Crypto.hexSha256(("HC|" + msg).getBytes(StandardCharsets.UTF_8)); }
  private static String HS(String msg) { return Crypto.hexSha256(("HS|" + msg).getBytes(StandardCharsets.UTF_8)); }
  private static String HR(String msg) { return Crypto.hexSha256(("HR|" + msg).getBytes(StandardCharsets.UTF_8)); }

  public static void recomputeCSRForFork(CubeRepository repo, String ks, List<String> cellDims, String factId, EmbedConfig cfg) {
    Cell cell = repo.getCell(cellDims);
    if (cell == null) return;
    Fork fork = cell.forks.get(factId);
    if (fork == null) return;

    StringBuilder C = new StringBuilder();
    StringBuilder S = new StringBuilder();
    StringBuilder R = new StringBuilder();

    for (FactVersion fv : fork.fact.versions) {
      int fV = ((Number)fv.flags.getOrDefault("fV", 1)).intValue();
      int fC = ((Number)fv.flags.getOrDefault("fC", 0)).intValue();
      int fS = ((Number)fv.flags.getOrDefault("fS", 0)).intValue();

      if (!fv.isNumeric()) {
        if (fV == 0) R.append(String.valueOf(fv.payload)).append("|");
        continue;
      }

      long qv = cfg.q(fv.asDouble());

      if (fV == 1 && fC == 1) {
        for (int i = cfg.xi - 1; i >= 0; i--) C.append((qv >> i) & 1L);
      }
      if (fV == 1 && fS == 1) {
        for (int i = 0; i < cfg.beta; i++) {
          int pos = 31 - i;
          S.append((qv >> pos) & 1L);
        }
      }
      if (fV == 0) {
        R.append(Long.toBinaryString(qv & 0xFFFFFFFFL)).append("|");
      }
    }

    String base = ks + "|" + String.join("|", cellDims) + "|";
    fork.C = HC(base + C);
    fork.S = HS(base + S);
    fork.R = HR(base + R);
  }

  public static void recomputeCSRForCell(CubeRepository repo, String ks, List<String> cellDims, EmbedConfig cfg) {
    Cell cell = repo.getCell(cellDims);
    if (cell == null) return;
    for (String fid : cell.forks.keySet()) {
      recomputeCSRForFork(repo, ks, cellDims, fid, cfg);
    }
  }

  public static void commitCellR(CubeRepository repo, String ks, List<String> cellDims) {
    Cell cell = repo.getCell(cellDims);
    if (cell == null) return;
    String msg = ks + "|" + String.join("|", cellDims) + "|BI=" + cell.BI.fA + "," + cell.BI.d + "|BO=" + cell.BO.fA + "," + cell.BO.d;
    cell.cellR = HR(msg);
  }

  public static boolean validateCellR(CubeRepository repo, String ks, List<String> cellDims) {
    Cell cell = repo.getCell(cellDims);
    if (cell == null) return false;
    String msg = ks + "|" + String.join("|", cellDims) + "|BI=" + cell.BI.fA + "," + cell.BI.d + "|BO=" + cell.BO.fA + "," + cell.BO.d;
    String cur = HR(msg);
    if (cell.cellR == null || cell.cellR.isBlank()) {
      cell.cellR = cur;
      return true;
    }
    return Objects.equals(cell.cellR, cur);
  }
}
