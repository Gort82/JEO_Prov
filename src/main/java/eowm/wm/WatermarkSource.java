package eowm.wm;

import eowm.util.Crypto;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class WatermarkSource {
  private WatermarkSource() {}

  public static List<Integer> bitsFromSeed(byte[] seed, int nbits) {
    List<Integer> out = new ArrayList<>(nbits);
    byte[] cur = seed;
    int counter = 0;
    while (out.size() < nbits) {
      if (counter > 0) {
        byte[] ext = new byte[cur.length + 4];
        System.arraycopy(cur, 0, ext, 0, cur.length);
        ext[ext.length-4] = (byte)((counter >> 24) & 0xff);
        ext[ext.length-3] = (byte)((counter >> 16) & 0xff);
        ext[ext.length-2] = (byte)((counter >> 8) & 0xff);
        ext[ext.length-1] = (byte)(counter & 0xff);
        cur = Crypto.sha256(ext);
      }
      counter++;
      for (byte b : cur) {
        for (int j = 7; j >= 0; j--) {
          out.add((b >> j) & 1);
          if (out.size() >= nbits) return out;
        }
      }
    }
    return out;
  }

  public static Watermark build(String kind, String ks, List<String> dimKeys, String metadataChunk, int wbits) {
    String msg = String.join("|", kind, ks, String.join(",", dimKeys), metadataChunk);
    byte[] seed = Crypto.sha256(msg.getBytes(StandardCharsets.UTF_8));
    return new Watermark(kind, bitsFromSeed(seed, wbits));
  }

  public static List<Integer> fragmentForCell(Watermark W, List<String> cellDims, int L) {
    String msg = W.kind + "|" + String.join("|", cellDims);
    byte[] h = Crypto.sha256(msg.getBytes(StandardCharsets.UTF_8));
    int start = ((h[0]&0xff)<<24)|((h[1]&0xff)<<16)|((h[2]&0xff)<<8)|(h[3]&0xff);
    start = Math.floorMod(start, W.bits.size());
    int len = Math.min(L, W.bits.size());
    List<Integer> frag = new ArrayList<>(len);
    for (int i = 0; i < len; i++) frag.add(W.bits.get((start + i) % W.bits.size()));
    return frag;
  }
}
