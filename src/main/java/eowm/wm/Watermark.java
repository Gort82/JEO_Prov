package eowm.wm;

import java.util.*;

public final class Watermark {
  public final List<Integer> bits;
  public final String kind; // "+p", "+a", "+o"
  public Watermark(String kind, List<Integer> bits) { this.kind = kind; this.bits = bits; }
}
