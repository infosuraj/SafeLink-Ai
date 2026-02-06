package com.example.backend.api;

public class CheckUrlResponse {
    private String url;
    private String status;
    private double confidence;

    public CheckUrlResponse(String url, String status, double confidence) {
        this.url = url;
        this.status = status;
        this.confidence = confidence;
    }

    public String getUrl() { return url; }
    public String getStatus() { return status; }
    public double getConfidence() { return confidence; }
}
