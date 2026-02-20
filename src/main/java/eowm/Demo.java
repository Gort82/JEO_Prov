package eowm;

import eowm.integration.SyntheticIngest;
import eowm.repository.*;
import eowm.wm.*;
import eowm.auth.Authenticity;

import java.util.*;

public final class Demo {
  private Demo() {}

  public static CubeRepository buildRepo() {
    CubeRepository repo = new CubeRepository();
    SyntheticIngest.ingest(repo, 42L);
    return repo;
  }

  public static void run(String ks, int wbits, int Theta, int theta) {
    CubeRepository repo = buildRepo();
    List<String> dimOrder = List.of("space","time","provider","metric");

    Watermark Wp = WatermarkSource.build("+p", ks, List.of("AOI","TIME","SRC","METRIC"), "provenance=demo", wbits);
    Watermark Wa = WatermarkSource.build("+a", ks, List.of("AUTHOR"), "author=MLPG", wbits);
    Watermark Wo = WatermarkSource.build("+o", ks, List.of("OWNER"), "owner=Org", wbits);

    VerticalParams P = new VerticalParams(Map.of("space",2,"time",2,"provider",2,"metric",3), 2, 8, 8);

    System.out.println("== Vertical embedding (+p)");
    VerticalSync.embed(repo, ks, Wp, dimOrder, P);

    System.out.println("== Build transmitter/receiver strings (Algorithm 6-like)");
    MapParams mp = new MapParams(P.N(), Theta, List.of(0,0,0,0), List.of(0,0,0,0), List.of(1,1,1,2));
    HorizontalSync.transmReceivMap(repo, ks, dimOrder, mp);

    System.out.println("== Horizontal synchronization (Algorithms 4-5-like)");
    int moved = HorizontalSync.syncChecker(repo, ks, Wp, theta, P.phi(), P.beta(), P.xi());
    System.out.println("Moved marks: " + moved);

    System.out.println("== Extraction (+p) with majority voting");
    List<Integer> Wp2 = VerticalSync.extract(repo, ks, Wp, dimOrder, P, null);
    System.out.printf("Similarity +p: %.3f%n", Scoring.hammingSimilarity(Wp.bits, Wp2));

    System.out.println("== (+a/+o) are available: call VerticalSync.embed(repo, ks, Wa/Wo, ...) to embed other layers.");
  }
}
