package com.cadist.style.config;

public class MessagePattern {
    private final String fingerprint;
    private String gradient;
    private String pluginName;
    private String originalSample;

    public MessagePattern(String fingerprint, String gradient, String pluginName, String originalSample) {
        this.fingerprint = fingerprint;
        this.gradient = gradient;
        this.pluginName = pluginName;
        this.originalSample = originalSample;
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

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getOriginalSample() {
        return originalSample;
    }

    public void setOriginalSample(String originalSample) {
        this.originalSample = originalSample;
    }
}
