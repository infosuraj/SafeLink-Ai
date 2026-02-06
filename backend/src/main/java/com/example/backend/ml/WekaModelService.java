package com.example.backend.ml;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.*;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class WekaModelService {

    private Classifier model;
    private Instances header;

    @PostConstruct
    public void start() throws Exception {
        Path modelPath = Path.of("model", "phishing-url.model");
        File modelFile = modelPath.toFile();
        Path headerPath = Path.of("model", "header.json");

        if (!Files.exists(modelPath.getParent())) {
            Files.createDirectories(modelPath.getParent());
        }

        if (!modelFile.exists()) {
            downloadModel("https://www.dropbox.com/scl/fi/mf3xh0iw31t31ejbwbbxv/phishing-url.model?rlkey=dse4c0lk21y1xxgpbi0pffc7p&st=aie0xcom&dl=1", modelFile);
        }

        if (Files.exists(headerPath)) {
            this.header = loadHeaderFromJson(headerPath);
        } else {
            throw new IllegalStateException("header.json missing");
        }

        if (modelFile.exists()) {
            this.model = (Classifier) SerializationHelper.read(modelFile.getPath());
        }
    }

    private void downloadModel(String urlStr, File destination) {
        try {
            URL url = new URL(urlStr);
            try (InputStream in = url.openStream()) {
                Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public PredictionResult predict(String url) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Model missing");
        }

        double[] feats = UrlFeatureExtractor.extract(url);

        if (feats.length != header.numAttributes() - 1) {
            throw new IllegalStateException("Feature mismatch");
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

    private Instances loadHeaderFromJson(Path headerPath) throws Exception {
        String json = Files.readString(headerPath);
        List<String> featureNames = new ArrayList<>();
        List<String> classValues = new ArrayList<>();

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