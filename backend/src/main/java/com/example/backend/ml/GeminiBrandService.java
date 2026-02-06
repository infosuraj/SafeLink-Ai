package com.example.backend.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class GeminiBrandService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GeminiBrandService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public boolean isSuspiciousBrand(String domain) {
        try {
            String apiUrl = GEMINI_URL + apiKey;

            String prompt = "You are a cybersecurity expert. Analyze the domain: '" + domain + "'. " +
                    "KNOWLEDGE: 'ajio.com', 'google.com', and 'paypal.com' are official trusted domains. " +
                    "QUESTION: Is this domain a fake, brand-squatting, or phishing attempt (like 'support-paypal.com' or 'ajio-deals.com')? " +
                    "Respond with only 'TRUE' if it is a fake/phish, or 'FALSE' if it is an official trusted domain.";

            Map<String, Object> contents = Map.of("parts", List.of(Map.of("text", prompt)));
            Map<String, Object> body = Map.of("contents", List.of(contents));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            assert response.getBody() != null;
            String aiResponse = response.getBody().toString().toUpperCase();
            return aiResponse.contains("TRUE");

        } catch (Exception e) {
            System.err.println("Gemini API Error: " + e.getMessage());
            return false;
        }
    }
}