package eowm.integration;

import eowm.repository.*;

import java.util.*;

public final class SyntheticIngest {
  private SyntheticIngest() {}

  public static void ingest(CubeRepository repo, long seed) {
    Random rng = new Random(seed);
    String[] spaces = {"ROI_A","ROI_B"};
    String[] times = {"2026-01","2026-02"};
    String[] providers = {"P1","P2","P3"};
    String[] metrics = {"NDVI","LST","CLOUD"};

    for (String sp : spaces) for (String tm : times) for (String pr : providers) {
      double baseNdvi = 0.1 + rng.nextDouble() * 0.8;
      double baseLst = 260 + rng.nextDouble() * 60;
      double baseCloud = rng.nextDouble() * 0.7;

      for (String met : metrics) {
        List<String> dims = List.of(sp, tm, pr, met);

        for (int v = 0; v < 6; v++) {
          boolean excluded = (v == 5);
          double base = switch (met) {
            case "NDVI" -> baseNdvi;
            case "LST" -> baseLst;
            default -> baseCloud;
          };
          double jitter = met.equals("LST") ? (rng.nextDouble() - 0.5) : ((rng.nextDouble() - 0.5) * 0.02);
          double val = base + jitter;

          Map<String,Object> meta = new HashMap<>();
          meta.put("space", sp); meta.put("time", tm); meta.put("provider", pr); meta.put("metric", met); meta.put("v", v);

          Map<String,Object> flags = new HashMap<>();
          flags.put("fV", excluded ? 0 : 1);
          flags.put("fC", 0);     // will be set by facts_filter when eligible
          flags.put("cV", 0);
          flags.put("fS", (!excluded && (v % 2 == 0)) ? 1 : 0);
          flags.put("bV", null);

          repo.upsertFactVersion(dims, "F0", new FactVersion(val, meta, flags));
        }

        // non-numeric excluded node (goes to R in Algorithm 8)
        Map<String,Object> meta2 = new HashMap<>();
        meta2.put("space", sp); meta2.put("time", tm); meta2.put("provider", pr); meta2.put("metric", met);
        Map<String,Object> flags2 = new HashMap<>();
        flags2.put("fV", 0); flags2.put("fC", 0); flags2.put("cV", 0); flags2.put("fS", 0); flags2.put("bV", null);
        repo.upsertFactVersion(dims, "META", new FactVersion("EO " + met + " package " + sp + "/" + tm + " from " + pr, meta2, flags2));
      }
    }
  }
}
