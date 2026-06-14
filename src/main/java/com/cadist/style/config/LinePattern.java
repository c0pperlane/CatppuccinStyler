package com.cadist.style.config;

public class LinePattern {

    private final String fingerprint;
    private String gradient;
    private String sample;
    private long lastSeen;

    public LinePattern(String fingerprint, String gradient, String sample) {
        this.fingerprint = fingerprint;
        this.gradient = gradient;
        this.sample = sample;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getGradient() {
        return gradient;
    }

    public void setGradient(String gradient) {
        this.gradient = gradient;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
