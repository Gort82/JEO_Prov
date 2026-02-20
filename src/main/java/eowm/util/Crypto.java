package eowm.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class Crypto {
  private Crypto() {}

  public static byte[] sha256(byte[] in) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return md.digest(in);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String hexSha256(byte[] in) {
    byte[] d = sha256(in);
    StringBuilder sb = new StringBuilder();
    for (byte b : d) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  public static long prngLong(String... parts) {
    String joined = String.join("|", parts);
    byte[] d = sha256(joined.getBytes(StandardCharsets.UTF_8));
    long v = 0;
    for (int i = 0; i < 8; i++) v = (v << 8) | (d[i] & 0xffL);
    return v;
  }

  public static String vpkFromMeta(Map<String, Object> meta) {
    List<String> keys = new ArrayList<>(meta.keySet());
    Collections.sort(keys);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < keys.size(); i++) {
      String k = keys.get(i);
      Object v = meta.get(k);
      sb.append(k).append("=").append(String.valueOf(v));
      if (i + 1 < keys.size()) sb.append("|");
    }
    return hexSha256(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  public static int xor(int a, int b) { return a ^ b; }

  public static String joinDims(List<String> dims) { return String.join("|", dims); }
}
