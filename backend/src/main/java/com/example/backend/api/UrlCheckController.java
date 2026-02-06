package com.example.backend.api;

import com.example.backend.ml.WekaModelService;
import com.example.backend.ml.GeminiBrandService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UrlCheckController {

    private final WekaModelService modelService;
    private final GeminiBrandService geminiService;

    public UrlCheckController(WekaModelService modelService, GeminiBrandService geminiService) {
        this.modelService = modelService;
        this.geminiService = geminiService;
    }

    @PostMapping("/check-url")
    public ResponseEntity<?> check(@Valid @RequestBody CheckUrlRequest req) {
        try {
            String rawUrl = req.getUrl().trim();
            if (!rawUrl.contains("://")) {
                rawUrl = "https://" + rawUrl;
            }
            String lowerUrl = rawUrl.toLowerCase();
            String currentHost = extractHost(lowerUrl);

            boolean isFakeBrand = geminiService.isSuspiciousBrand(currentHost);
            if (isFakeBrand) {
                return ResponseEntity.ok(new CheckUrlResponse(rawUrl, "Phishing", 0.99));
            }

            var result = modelService.predict(rawUrl);
            String rawStatus = result.status().toLowerCase();
            String status = (rawStatus.contains("phish") || rawStatus.equals("bad")) ? "Phishing" : "Legitimate";

            return ResponseEntity.ok(new CheckUrlResponse(rawUrl, status, result.confidence()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", "MODEL_NOT_READY", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    private String extractHost(String url) {
        String host = url.replace("http://", "").replace("https://", "");
        int slash = host.indexOf("/");
        if (slash > 0) host = host.substring(0, slash);
        return host;
    }
}