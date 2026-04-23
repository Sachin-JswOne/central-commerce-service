package com.jswone.commerce.core.util.shortlinks;

public class Base62Util {

    private static final String BASE62 = "v6QY9S5Z2p4R7W8n1D3M0kLxbGfHjKmNqRsTuVwXyZaBcDeFiJlOp4z8A1E7gIqtUh";
    private static final int BASE = 62;
    private static final long OFFSET = 100000000L; // Offset to avoid short numeric keys

    public static String encode(long value) {
        // Scramble the ID to make consecutive IDs result in non-sequential codes
        long scrambled = hash(value + OFFSET);

        StringBuilder sb = new StringBuilder();
        while (scrambled > 0) {
            sb.append(BASE62.charAt((int) (scrambled % BASE)));
            scrambled /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Reversible mixing function to spread consecutive IDs
     */
    private static long hash(long x) {
        x = ((x >>> 16) ^ x) * 0x45d9f3bL;
        x = ((x >>> 16) ^ x) * 0x45d9f3bL;
        x = (x >>> 16) ^ x;
        return x & 0xFFFFFFFFFFFL; // Keep within ~43 bits to avoid extremely long strings
    }

    public static long decode(String str) {
        long value = 0;
        for (int i = 0; i < str.length(); i++) {
            value = value * BASE + BASE62.indexOf(str.charAt(i));
        }
        return value - OFFSET;
    }
}
