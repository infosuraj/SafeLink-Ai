package com.example.backend.api;

import jakarta.validation.constraints.NotBlank;

public class CheckUrlRequest {
    @NotBlank(message = "url must not be blank")
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
