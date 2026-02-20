package eowm.wm;

import java.util.List;
import java.util.Map;

public record MapParams(Map<String,Integer> N, int Theta, List<Integer> I, List<Integer> v, List<Integer> V) {}
