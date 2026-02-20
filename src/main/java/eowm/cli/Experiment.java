package eowm.cli;

import eowm.integration.SyntheticIngest;
import eowm.repository.CubeRepository;
import eowm.wm.*;

import java.util.*;

public final class Experiment {
  private Experiment() {}

  /**
   * Repeatable experiment runner (NOT part of CI).
   * Runs multiple trials with different ingest seeds and reports similarity statistics.
   *
   * Usage:
   *   java -jar ... experiment --trials 10 --wbits 64 --theta 2 --Theta 2
   */
  public static void run(String ks, int wbits, int Theta, int theta, Args args) {
    int trials = args.getInt("trials", 10);

    List<String> dimOrder = List.of("space","time","provider","metric");
    VerticalParams P = new VerticalParams(Map.of("space",2,"time",2,"provider",2,"metric",3), 2, 8, 8);

    List<Double> sims = new ArrayList<>();
    for (int t = 0; t < trials; t++) {
      long seed = 1000L + t;
      CubeRepository repo = new CubeRepository();
      SyntheticIngest.ingest(repo, seed);

      Watermark Wp = WatermarkSource.build("+p", ks, List.of("AOI","TIME","SRC","METRIC"), "provenance=experiment", wbits);

      VerticalSync.embed(repo, ks, Wp, dimOrder, P);

      MapParams mp = new MapParams(P.N(), Theta, List.of(0,0,0,0), List.of(0,0,0,0), List.of(1,1,1,2));
      HorizontalSync.transmReceivMap(repo, ks, dimOrder, mp);

      HorizontalSync.syncChecker(repo, ks, Wp, theta, P.phi(), P.beta(), P.xi());

      List<Integer> Wp2 = VerticalSync.extract(repo, ks, Wp, dimOrder, P, null);
      double sim = Scoring.hammingSimilarity(Wp.bits, Wp2);
      sims.add(sim);
      System.out.printf("trial=%d seed=%d similarity=%.3f%n", t+1, seed, sim);
    }

    double mean = sims.stream().mapToDouble(x -> x).average().orElse(0.0);
    double min = sims.stream().mapToDouble(x -> x).min().orElse(0.0);
    double max = sims.stream().mapToDouble(x -> x).max().orElse(0.0);

    System.out.println("----");
    System.out.printf("trials=%d wbits=%d theta=%d Theta=%d%n", trials, wbits, theta, Theta);
    System.out.printf("similarity: mean=%.3f min=%.3f max=%.3f%n", mean, min, max);
    System.out.println("Note: similarity depends on vote coverage and parameters; this runner is for exploration/reproducibility, not CI gating.");
  }
}
