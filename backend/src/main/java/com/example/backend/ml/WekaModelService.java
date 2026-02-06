package com.example.backend.ml;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.*;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class WekaModelService {

    private Classifier model;
    private Instances header;

    @PostConstruct
    public void start() throws Exception {
        // 1. Setup local directory for the .model file
        Path rootPath = Path.of("").toAbsolutePath();
        Path modelDir = rootPath.resolve("model");

        if (!Files.exists(modelDir)) {
            Files.createDirectories(modelDir);
        }

        File modelFile = modelDir.resolve("phishing-url.model").toFile();

        // 2. Download model if it's missing (using your Dropbox link)
        if (!modelFile.exists()) {
            System.out.println("Model not found locally. Downloading...");
            downloadModel("https://www.dropbox.com/scl/fi/mf3xh0iw31t31ejbwbbxv/phishing-url.model?rlkey=dse4c0lk21y1xxgpbi0pffc7p&st=aie0xcom&dl=1", modelFile);
        }

        // 3. LOAD HEADER: Read from Classpath (src/main/resources)
        ClassPathResource resource = new ClassPathResource("header.json");
        if (!resource.exists()) {
            throw new IllegalStateException("CRITICAL ERROR: header.json missing from src/main/resources!");
        }

        // Use the input stream to parse the header
        try (InputStream inputStream = resource.getInputStream()) {
            this.header = loadHeaderFromStream(inputStream);
            System.out.println("Header loaded successfully from classpath.");
        }

        // 4. LOAD MODEL: Read the serialized Weka model
        if (modelFile.exists()) {
            this.model = (Classifier) SerializationHelper.read(modelFile.getPath());
            System.out.println("Weka model loaded successfully.");
        } else {
            throw new IllegalStateException("Failed to load model file after download attempt.");
        }
    }

    private void downloadModel(String urlStr, File destination) {
        try {
            URL url = new URL(urlStr);
            try (InputStream in = url.openStream()) {
                Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Download failed: " + e.getMessage());
        }
    }

    public PredictionResult predict(String url) throws Exception {
        if (model == null || header == null) {
            throw new IllegalStateException("Model or Header is not initialized.");
        }

        double[] feats = UrlFeatureExtractor.extract(url);

        // Ensure features match expected header count (minus class attribute)
        if (feats.length != header.numAttributes() - 1) {
            throw new IllegalStateException("Feature mismatch! Expected: " + (header.numAttributes() - 1) + " but got: " + feats.length);
        }

        DenseInstance inst = new DenseInstance(header.numAttributes());
        inst.setDataset(header);

        for (int i = 0; i < feats.length; i++) {
            inst.setValue(i, feats[i]);
        }
        inst.setClassMissing();

        double[] dist = model.distributionForInstance(inst);
        int bestIdx = argmax(dist);

        return new PredictionResult(
                header.classAttribute().value(bestIdx),
                dist[bestIdx],
                dist
        );
    }

    // Updated to handle InputStream from Classpath
    private Instances loadHeaderFromStream(InputStream is) throws Exception {
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        List<String> featureNames = new ArrayList<>();
        List<String> classValues = new ArrayList<>();

        // Manual JSON parsing logic (as per your original code)
        String fn = json.substring(json.indexOf("\"features\""));
        String fnArr = fn.substring(fn.indexOf("[") + 1, fn.indexOf("]"));
        for (String s : fnArr.split(",")) {
            String v = s.trim().replaceAll("^\"|\"$", "");
            if (!v.isBlank()) featureNames.add(v);
        }

        String cv = json.substring(json.indexOf("\"classValues\""));
        String cvArr = cv.substring(cv.indexOf("[") + 1, cv.indexOf("]"));
        for (String s : cvArr.split(",")) {
            String v = s.trim().replaceAll("^\"|\"$", "");
            if (!v.isBlank()) classValues.add(v);
        }

        ArrayList<Attribute> attrs = new ArrayList<>();
        for (String f : featureNames) attrs.add(new Attribute(f));
        attrs.add(new Attribute("class", classValues));

        Instances h = new Instances("phishing_url_header", attrs, 0);
        h.setClassIndex(h.numAttributes() - 1);
        return h;
    }

    private static int argmax(double[] a) {
        int best = 0;
        for (int i = 1; i < a.length; i++) if (a[i] > a[best]) best = i;
        return best;
    }

    public record PredictionResult(String status, double confidence, double[] probs) {}
}