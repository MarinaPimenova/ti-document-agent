package com.wk.ti.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ParserUtil {
    private static final String START = "[";
    private static final String END = "]";

    private ParserUtil() {
    }

    public static String listToString(List<String> input) {
        return START +
                String.join(",", fallback(input)) +
                END;
    }

    public static String listListToString(List<List<String>> input) {
        StringBuilder sb = new StringBuilder();
        sb.append(START);
        for (List<String> row : fallback(input)) {
            sb.append(listToString(row));
        }
        sb.append(END);
        return sb.toString();
    }

    private static <T> List<T> fallback(List<T> input) {
        return input == null ? List.of() : input;
    }

    public static long processLongValue(Object v) {
        if (v instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    public static int processValue(Object v) {
        long l = processLongValue(v);

        int result;
        if (l > Integer.MAX_VALUE) {
            result = Integer.MAX_VALUE;
        } else if (l < Integer.MIN_VALUE) {
            result = Integer.MIN_VALUE;
        } else {
            result = (int) l;
        }
        return result;

    }

}
