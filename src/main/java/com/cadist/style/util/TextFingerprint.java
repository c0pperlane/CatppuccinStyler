package com.cadist.style.util;

public final class TextFingerprint {

    private TextFingerprint() {}

    public static String create(String text) {
        if (text == null) return "";
        String f = text;

        // Replace UUIDs first (before numbers)
        f = f.replaceAll("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "{uuid}");

        // Replace time patterns like 1m, 5s, 10h, 15d, 1ms
        f = f.replaceAll("(?i)\\b\\d+[smhd](?:s|ms)?\\b", "{time}");

        // Replace version strings like 1.21.11 or v5.5.17
        f = f.replaceAll("(?i)\\bv?\\d+\\.\\d+(?:\\.\\d+)*\\b", "{ver}");

        // Replace numbers (including decimals, negatives)
        f = f.replaceAll("-?\\d+\\.?\\d*", "{num}");

        // Normalize whitespace
        f = f.replaceAll("\\s+", " ").trim();

        return f.toLowerCase();
    }

    public static int wordCount(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    public static boolean isSimilar(String fingerprintA, String fingerprintB) {
        if (fingerprintA == null || fingerprintB == null) return false;
        if (fingerprintA.equals(fingerprintB)) return true;

        String[] wordsA = fingerprintA.split(" ");
        String[] wordsB = fingerprintB.split(" ");

        // Word count tolerance: difference of at most 2 words
        if (Math.abs(wordsA.length - wordsB.length) > 2) return false;

        // Only allow similarity for longer messages (>5 words)
        if (wordsA.length <= 5 || wordsB.length <= 5) return false;

        int distance = levenshtein(fingerprintA, fingerprintB);
        int maxLen = Math.max(fingerprintA.length(), fingerprintB.length());
        if (maxLen == 0) return true;

        return (double) distance / maxLen < 0.20;
    }

    private static int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
