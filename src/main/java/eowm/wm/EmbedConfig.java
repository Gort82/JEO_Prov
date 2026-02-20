package eowm.wm;

public final class EmbedConfig {
  public final int scale;
  public final int beta;
  public final int xi;

  public EmbedConfig(int scale, int beta, int xi) {
    this.scale = scale;
    this.beta = beta;
    this.xi = xi;
  }

  public long q(double x) { return Math.round(x * (double)scale); }

  public int msbBit(long qv, int bIndex) {
    long uq = qv & 0xFFFFFFFFL;
    int pos = 31 - Math.floorMod(bIndex, beta);
    return (int)((uq >> pos) & 1L);
  }

  public int extractLsb(double x, int pXi) {
    long qv = q(x);
    return (int)((qv >> pXi) & 1L);
  }

  public SetResult setLsb(double x, int bit, int pXi) {
    long qv = q(x);
    long mask = 1L << pXi;
    int orig = ((qv & mask) != 0) ? 1 : 0;
    long q2 = (bit == 1) ? (qv | mask) : (qv & ~mask);
    return new SetResult(((double)q2)/scale, orig);
  }

  public record SetResult(double value, int origBit) {}
}
