package eowm;

import eowm.auth.Authenticity;
import eowm.integration.SyntheticIngest;
import eowm.repository.*;
import eowm.wm.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public final class PipelineTest {

  @Test
  void deterministicRegressionPipelineRunsAndMaintainsInvariants() {
    String ks = "test-secret";
    int wbits = 128;

    CubeRepository repo = new CubeRepository();
    SyntheticIngest.ingest(repo, 123L);

    List<String> dimOrder = List.of("space","time","provider","metric");
    Watermark Wp = WatermarkSource.build("+p", ks, List.of("AOI","TIME","SRC","METRIC"), "provenance=test", wbits);

    VerticalParams P = new VerticalParams(Map.of("space",2,"time",2,"provider",2,"metric",3), 2, 8, 8);

    VerticalSync.embed(repo, ks, Wp, dimOrder, P);

    // at least one active carrier exists
    boolean anyActive = false;
    for (Cell cell : repo.allCells()) {
      Fork fork = cell.forks.get("F0");
      if (fork == null) continue;
      for (FactVersion fv : fork.fact.versions) {
        int cV = ((Number)fv.flags.getOrDefault("cV", 0)).intValue();
        if (cV == 1) { anyActive = true; break; }
      }
      if (anyActive) break;
    }
    assertTrue(anyActive, "Expected at least one active carrier after embedding.");

    MapParams mp = new MapParams(P.N(), 2, List.of(0,0,0,0), List.of(0,0,0,0), List.of(1,1,1,2));
    HorizontalSync.transmReceivMap(repo, ks, dimOrder, mp);

    Cell some = repo.allCells().iterator().next();
    assertTrue(Authenticity.validateCellR(repo, ks, some.dims));

    int moved = HorizontalSync.syncChecker(repo, ks, Wp, 2, P.phi(), P.beta(), P.xi());
    assertTrue(moved >= 0);

    List<Integer> Wp2 = VerticalSync.extract(repo, ks, Wp, dimOrder, P, null);
    assertEquals(Wp.bits.size(), Wp2.size());
    for (int b : Wp2) assertTrue(b == 0 || b == 1);
  }

  @Test
  void biBoTamperingIsDetectableViaRBinding() {
    String ks = "test-secret";
    CubeRepository repo = new CubeRepository();
    SyntheticIngest.ingest(repo, 123L);

    Cell cell = repo.allCells().iterator().next();
    Authenticity.commitCellR(repo, ks, cell.dims);
    assertTrue(Authenticity.validateCellR(repo, ks, cell.dims));

    cell.BO.fA = 1;
    cell.BO.d = List.of("TAMPERED");

    assertFalse(Authenticity.validateCellR(repo, ks, cell.dims));
  }
}
