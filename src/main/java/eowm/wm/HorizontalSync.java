package eowm.wm;

import eowm.auth.Authenticity;
import eowm.repository.*;
import eowm.util.Crypto;

import java.util.*;

public final class HorizontalSync {
  private HorizontalSync() {}

  public static void transmReceivMap(CubeRepository repo, String ks, List<String> dimOrder, MapParams mp) {
    Map<String, List<String>> allDims = new HashMap<>();
    for (String n : dimOrder) allDims.put(n, new ArrayList<>());
    for (Cell cell : repo.allCells()) {
      for (int i = 0; i < dimOrder.size(); i++) {
        String n = dimOrder.get(i);
        String v = cell.dims.get(i);
        if (!allDims.get(n).contains(v)) allDims.get(n).add(v);
      }
    }

    Map<String, List<String>> reduced = Dimensions.dimDef(ks, allDims, mp.N());
    List<List<String>> dimsLists = new ArrayList<>();
    for (String n : dimOrder) dimsLists.add(reduced.get(n));

    int nd = dimOrder.size();
    List<Integer> I = new ArrayList<>(mp.I());
    List<Integer> loOff = new ArrayList<>(mp.v());
    List<Integer> hiOff = new ArrayList<>(mp.V());

    List<String> prev = null;

    for (int t = 0; t < mp.Theta() + 1; t++) {
      List<String> chosen = new ArrayList<>(nd);
      for (int j = 0; j < nd; j++) {
        int lo = I.get(j) + loOff.get(j);
        int hi = I.get(j) + hiOff.get(j);
        int span = Math.max(1, hi - lo + 1);
        long r = Crypto.prngLong(ks, "HRND", String.valueOf(I.get(j)), dimOrder.get(j), String.valueOf(t));
        int idx = lo + (int)Math.floorMod(r, span);
        idx = Math.floorMod(idx, dimsLists.get(j).size());
        chosen.add(dimsLists.get(j).get(idx));
        I.set(j, idx);
      }

      Cell cur = repo.getOrCreateCell(chosen);

      if (t == 0) {
        cur.BI.d = null; cur.BI.fA = 1;
        prev = chosen;
        continue;
      }

      Cell prevCell = repo.getOrCreateCell(prev);

      // collusion avoidance: don't assign BI twice
      if (cur.BI.d != null) {
        prevCell.BO.d = null; prevCell.BO.fA = 0;
        break;
      }

      cur.BI.d = new ArrayList<>(prev);
      cur.BI.fA = 1;

      if (t < mp.Theta()) {
        prevCell.BO.d = new ArrayList<>(chosen);
        prevCell.BO.fA = 1;
      } else {
        prevCell.BO.d = null;
        prevCell.BO.fA = 0;
      }

      prev = chosen;
    }

    // bind BI/BO to R for each cell
    for (Cell c : repo.allCells()) Authenticity.commitCellR(repo, ks, c.dims);
  }

  public static int syncChecker(CubeRepository repo, String ks, Watermark W, int theta, int phi, int beta, int xi) {
    int moved = 0;
    for (Cell cell : new ArrayList<>(repo.allCells())) {
      if (cell.BO.fA == 1) {
        moved += horizSync(repo, ks, W, cell.dims, theta, phi, new EmbedConfig(10_000, beta, xi), null);
      }
    }
    return moved;
  }

  private static int horizSync(CubeRepository repo, String ks, Watermark W, List<String> transmitterDims, int theta, int phi, EmbedConfig cfg, Integer Ktheta) {
    if (theta <= 0) return 0;

    if (!Authenticity.validateCellR(repo, ks, transmitterDims)) return 0;

    Cell T = repo.getCell(transmitterDims);
    if (T == null || T.BO.fA != 1 || T.BO.d == null) return 0;

    List<String> receiverDims = T.BO.d;
    Cell R = repo.getCell(receiverDims);
    if (R == null || R.BI.fA != 1) return 0;
    if (R.BI.d != null && !R.BI.d.equals(transmitterDims)) return 0;

    Fork fT = T.forks.get("F0");
    Fork fR = R.forks.get("F0");
    if (fT == null || fR == null) return 0;

    // pick first active carrier in transmitter
    FactVersion carrierT = null;
    for (FactVersion fv : fT.fact.versions) {
      int fC = ((Number)fv.flags.getOrDefault("fC", 0)).intValue();
      int cV = ((Number)fv.flags.getOrDefault("cV", 0)).intValue();
      if (fC == 1 && cV == 1 && fv.isNumeric()) { carrierT = fv; break; }
    }
    if (carrierT == null) return 0;

    // reconstruct meta-mark from transmitter
    long KT = Crypto.prngLong(ks, "KT", Crypto.vpkFromMeta(carrierT.meta));
    long K0 = (Ktheta == null) ? KT : (Crypto.prngLong(ks, "K0", String.valueOf(KT)) ^ (long)Ktheta);

    long KF = Crypto.prngLong(ks, "KF2", Crypto.vpkFromMeta(carrierT.meta), String.valueOf(cfg.beta), String.join("|", transmitterDims));
    int bIndex = (int)Math.floorMod(KF, cfg.beta);
    int pXi = (int)Math.floorMod(Crypto.prngLong(String.valueOf(KF), "Hxi"), cfg.xi);

    int bBeta = cfg.msbBit(cfg.q(carrierT.asDouble()), bIndex);
    int mark = cfg.extractLsb(carrierT.asDouble(), pXi);
    int meta = mark ^ bBeta;

    // choose a free receiver carrier with fV=1,fC=1,cV=0 (facts_filter precomputes fC flags; here we enforce)
    FactVersion carrierR = null;
    for (FactVersion cand : fR.fact.versions) {
      int fV = ((Number)cand.flags.getOrDefault("fV", 1)).intValue();
      int fC2 = ((Number)cand.flags.getOrDefault("fC", 0)).intValue();
      int cV2 = ((Number)cand.flags.getOrDefault("cV", 0)).intValue();
      if (fV == 1 && fC2 == 1 && cV2 == 0 && cand.isNumeric()) { carrierR = cand; break; }
    }
    if (carrierR == null) return 0;

    long RKF = Crypto.prngLong(ks, "KF2", Crypto.vpkFromMeta(carrierR.meta), String.valueOf(cfg.beta), String.join("|", receiverDims));
    int rbIndex = (int)Math.floorMod(RKF, cfg.beta);
    int rpXi = (int)Math.floorMod(Crypto.prngLong(String.valueOf(RKF), "Hxi"), cfg.xi);

    int rbBeta = cfg.msbBit(cfg.q(carrierR.asDouble()), rbIndex);
    int rmark = meta ^ rbBeta;

    var res = cfg.setLsb(carrierR.asDouble(), rmark, rpXi);
    carrierR.setDouble(res.value());
    carrierR.flags.put("bV", res.origBit());
    carrierR.flags.put("cV", 1);

    // release transmitter
    carrierT.flags.put("cV", 0);
    T.BO.fA = 0;

    // recompute digests at both ends (trigger)
    Authenticity.recomputeCSRForCell(repo, ks, transmitterDims, cfg);
    Authenticity.recomputeCSRForCell(repo, ks, receiverDims, cfg);

    // open receiver as next transmitter if recursion continues
    if (theta - 1 > 0) R.BO.fA = 1;

    int newKtheta = (int)(K0 ^ Crypto.prngLong(ks, "Ktheta", String.valueOf(KT)));

    return 1 + horizSync(repo, ks, W, receiverDims, theta - 1, phi, cfg, newKtheta);
  }
}
