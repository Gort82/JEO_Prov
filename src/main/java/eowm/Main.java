package eowm;

import eowm.cli.Args;
import eowm.repository.CubeRepository;

public final class Main {
  public static void main(String[] argv) {
    Args args = new Args(argv);
    String cmd = args.cmd();

    if (cmd.isBlank() || cmd.equals("--help") || cmd.equals("-h")) {
      help();
      return;
    }

    String ks = args.get("ks", "demo-secret");
    int wbits = args.getInt("wbits", 128);
    int Theta = args.getInt("Theta", 2);
    int theta = args.getInt("theta", 2);

    switch (cmd) {
      case "demo" -> Demo.run(ks, wbits, Theta, theta);
      case "experiment" -> eowm.cli.Experiment.run(ks, wbits, Theta, theta, args);
      default -> {
        System.err.println("Unknown command: " + cmd);
        help();
        System.exit(2);
      }
    }
  }

  private static void help() {
    System.out.println("Usage:");
    System.out.println("  java -jar target/eowm-java-0.2.0.jar demo [--ks <secret>] [--wbits <n>] [--Theta <n>] [--theta <n>]");
    System.out.println("  java -jar target/eowm-java-0.2.0.jar experiment [--trials <n>] [--ks <secret>] [--wbits <n>] [--Theta <n>] [--theta <n>]");
  }
}
