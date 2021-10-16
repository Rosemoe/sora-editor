package io.github.rosemoe.sora.textmate.core.internal.utils;

import java.util.List;

public class CompareUtils {

    public static int strcmp(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
//		if (a < b) {
//			return -1;
//		}
//		if (a > b) {
//			return 1;
//		}
//		return 0;
        int result = a.compareTo(b);
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        }
        return 0;
    }

    public static int strArrCmp(List<String> a, List<String> b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        int len1 = a.size();
        int len2 = b.size();
        if (len1 == len2) {
            for (int i = 0; i < len1; i++) {
                int res = strcmp(a.get(i), b.get(i));
                if (res != 0) {
                    return res;
                }
            }
            return 0;
        }
        return len1 - len2;
    }

}
