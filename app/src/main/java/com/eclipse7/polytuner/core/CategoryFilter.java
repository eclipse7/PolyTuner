package com.eclipse7.polytuner.core;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CategoryFilter {
    private int p;
    private int[] xMem;

    public CategoryFilter() {
        this.p = 15;
        xMem = new int[p];
        for (int i = 0; i < p; i++) xMem[i] = 0;
    }

    private static Integer findKeyWithMaxValue(Map<Integer, Integer> map) {
        List<Map.Entry<Integer, Integer>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return ((Comparable<Integer>) ((Map.Entry<Integer, Integer>) (o1)).getValue())
                        .compareTo(((Map.Entry<Integer, Integer>) (o2)).getValue());
            }
        });
        return list.get(list.size() - 1).getKey();
    }

    public final int filtering(int x) {
        //shift
        for (int i = (p - 1); i > 0; i--)
            xMem[i] = xMem[i - 1];
        xMem[0] = x;

        Map<Integer,Integer> dict = new HashMap<>();
        for (int i = 0; i < p; i++) {
            int c = xMem[i];
            if (dict.get(c) == null) {
                dict.put(c, 1);
            }
            else {
                int value = dict.get(c) + 1;
                dict.put(c, value);
            }
        }
        return findKeyWithMaxValue(dict);
    }
}
