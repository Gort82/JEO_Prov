package eowm.wm;

import eowm.auth.Authenticity;
import eowm.repository.*;
import eowm.util.Crypto;

import java.util.*;

public final class VerticalSync {
  private VerticalSync() {}

  /**
   * Algorithm 3-inspired facts_filter.
   * epsilon=0: select candidates with fV==1 and (KF mod phi)==0; set fC=1,cV=0 for selected, set fC=0 for others.
   * epsilon=1: select active carriers with fC==1 and cV==1.
   */
  public static List<Integer> factsFilter(List<FactVersion> versions, String ks, int beta, List<String> d, int phi, int epsilon) {
    List<Integer> out = new ArrayList<>();
    int c = Math.max(1, phi);

    if (epsilon == 0) {
      // reset carrier eligibility deterministically each run (closer to "select new carriers")
      for (FactVersion fv : versions) {
        fv.flags.put("fC", 0);
        // cV kept as-is (busy flags are meaningful across operations), but if fC is zero we treat as not a carrier
      }
    }

    for (int i = 0; i < versions.size(); i++) {
      FactVersion fv = versions.get(i);
      int fV = ((Number)fv.flags.getOrDefault("fV", 1)).intValue();
      int fC = ((Number)fv.flags.getOrDefault("fC", 0)).intValue();
      int cV = ((Number)fv.flags.getOrDefault("cV", 0)).intValue();

      long KF = Crypto.prngLong(ks, "KF", Crypto.vpkFromMeta(fv.meta), String.valueOf(beta), String.join("|", d));

      if (epsilon == 0) {
        if (fV == 1 && Math.floorMod(KF, c) == 0) {
          fv.flags.put("fC", 1);
          fv.flags.put("cV", 0);
          out.add(i);
        }
      } else {
        if (fC == 1 && cV == 1) out.add(i);
      }
    }
    return out;
  }

  public static void embed(CubeRepository repo, String ks, Watermark W, List<String> dimOrder, VerticalParams P) {
    EmbedConfig cfg = new EmbedConfig(10_000, P.beta(), P.xi());

    // gather universes
    Map<String, List<String>> allDims = new HashMap<>();
    for (String n : dimOrder) allDims.put(n, new ArrayList<>());
    for (Cell cell : repo.allCells()) {
      for (int i = 0; i < dimOrder.size(); i++) {
        String n = dimOrder.get(i);
        String v = cell.dims.get(i);
        if (!allDims.get(n).contains(v)) allDims.get(n).add(v);
      }
    }

    Map<String, List<String>> reduced = Dimensions.dimDef(ks, allDims, P.N());
    List<List<String>> combos = Dimensions.combine(reduced, dimOrder);

    for (List<String> cellDims : combos) {
      Cell cell = repo.getCell(cellDims);
      if (cell == null) continue;
      Fork fork = cell.forks.get("F0");
      if (fork == null) continue;

      List<Integer> F = factsFilter(fork.fact.versions, ks, P.beta(), cellDims, P.phi(), 0);
      if (F.isEmpty()) continue;

      // KC selects one version from F
      StringBuilder vpkJoin = new StringBuilder();
      for (int idx : F) vpkJoin.append(Crypto.vpkFromMeta(fork.fact.versions.get(idx).meta)).append("|");
      long KC = Crypto.prngLong(ks, "KC", vpkJoin.toString());
      int vidx = F.get((int)Math.floorMod(KC, F.size()));
      FactVersion fv = fork.fact.versions.get(vidx);

      long KF = Crypto.prngLong(ks, "KF2", Crypto.vpkFromMeta(fv.meta), String.valueOf(P.beta()), String.join("|", cellDims));
      int bIndex = (int)Math.floorMod(KF, P.beta());
      int pXi = (int)Math.floorMod(Crypto.prngLong(String.valueOf(KF), "Hxi"), P.xi());

      int bBeta = cfg.msbBit(cfg.q(fv.asDouble()), bIndex);

      List<Integer> frag = WatermarkSource.fragmentForCell(W, cellDims, 32);
      int mmPos = (int)Math.floorMod(Crypto.prngLong(ks, "Hm", String.valueOf(KF)), frag.size());
      int meta = frag.get(mmPos);

      int mark = meta ^ bBeta;
      var res = cfg.setLsb(fv.asDouble(), mark, pXi);
      fv.setDouble(res.value());
      fv.flags.put("bV", res.origBit());
      fv.flags.put("cV", 1);

      // trigger recompute C/S/R for all forks in this cell
      Authenticity.recomputeCSRForCell(repo, ks, cellDims, cfg);
    }
  }

  public static List<Integer> extract(CubeRepository repo, String ks, Watermark W, List<String> dimOrder, VerticalParams P, Integer Ktheta) {
    EmbedConfig cfg = new EmbedConfig(10_000, P.beta(), P.xi());
    List<List<Integer>> M = new ArrayList<>(W.bits.size());
    for (int i = 0; i < W.bits.size(); i++) M.add(new ArrayList<>());

    // gather universes
    Map<String, List<String>> allDims = new HashMap<>();
    for (String n : dimOrder) allDims.put(n, new ArrayList<>());
    for (Cell cell : repo.allCells()) {
      for (int i = 0; i < dimOrder.size(); i++) {
        String n = dimOrder.get(i);
        String v = cell.dims.get(i);
        if (!allDims.get(n).contains(v)) allDims.get(n).add(v);
      }
    }

    Map<String, List<String>> reduced = Dimensions.dimDef(ks, allDims, P.N());
    List<List<String>> combos = Dimensions.combine(reduced, dimOrder);

    for (List<String> cellDims : combos) {
      Cell cell = repo.getCell(cellDims);
      if (cell == null) continue;
      Fork fork = cell.forks.get("F0");
      if (fork == null) continue;

      List<Integer> active = factsFilter(fork.fact.versions, ks, P.beta(), cellDims, P.phi(), 1);
      if (active.isEmpty()) continue;

      FactVersion fv = fork.fact.versions.get(active.get(0));

      long KT = Crypto.prngLong(ks, "KT", Crypto.vpkFromMeta(fv.meta));
      long K0 = (Ktheta == null) ? KT : (Crypto.prngLong(ks, "K0", String.valueOf(KT)) ^ (long)Ktheta);

      long KF = Crypto.prngLong(ks, "KF2", Crypto.vpkFromMeta(fv.meta), String.valueOf(P.beta()), String.join("|", cellDims));
      int bIndex = (int)Math.floorMod(KF, P.beta());
      int pXi = (int)Math.floorMod(Crypto.prngLong(String.valueOf(KF), "Hxi"), P.xi());

      int bBeta = cfg.msbBit(cfg.q(fv.asDouble()), bIndex);
      int mark = cfg.extractLsb(fv.asDouble(), pXi);
      int meta = mark ^ bBeta;

      int wpos = (int)Math.floorMod(Crypto.prngLong(ks, "pos", String.valueOf(K0)), W.bits.size());
      M.get(wpos).add(meta);
    }

    List<Integer> out = new ArrayList<>(W.bits.size());
    for (List<Integer> votes : M) {
      if (votes.isEmpty()) { out.add(0); continue; }
      int ones = 0;
      for (int v : votes) if (v == 1) ones++;
      int zeros = votes.size() - ones;
      out.add(ones >= zeros ? 1 : 0);
    }
    return out;
  }
}
