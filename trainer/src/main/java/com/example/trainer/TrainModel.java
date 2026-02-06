package com.example.trainer;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import weka.core.SerializationHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TrainModel {
    private static final String[] FEATURE_NAMES = new String[]{
            "Using_IP_Address",
            "URL_Length",
            "Has_At_Symbol",
            "Has_Double_Slash",
            "Num_Dots",
            "Prefix_Suffix_Separation",
            "HTTPS_in_Domain",
            "Num_Subdomains",
            "Has_Punycode",
            "Digit_Ratio",
            "URL_Entropy"
    };

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  mvn clean compile exec:java -Dexec.mainClass=\"com.example.trainer.TrainModel\" -Dexec.args=\"<inputFile> <outputDir>\"");
            System.out.println("Example:");
            System.out.println("  mvn clean compile exec:java -Dexec.mainClass=\"com.example.trainer.TrainModel\" -Dexec.args=\"..\\data\\urls.csv ..\\backend\\model\"");
            return;
        }

        String inputPath = args[0];
        String outputDir = args[1];

        Instances data = loadAndBuildInstances(inputPath);

        data.randomize(new Random(42));

        RandomForest rf = new RandomForest();
        rf.setNumIterations(200);
        rf.setMaxDepth(0);
        rf.buildClassifier(data);

        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(rf, data, 10, new Random(42));

        System.out.println("\n=== 10-Fold CV Results ===");
        System.out.println(eval.toSummaryString());
        System.out.println(eval.toClassDetailsString());
        System.out.println(eval.toMatrixString());

        Files.createDirectories(Path.of(outputDir));
        Path modelPath = Path.of(outputDir, "phishing-url.model");
        SerializationHelper.write(modelPath.toString(), rf);

        Path headerPath = Path.of(outputDir, "header.json");
        Files.writeString(headerPath, buildHeaderJson());

        System.out.println("Saved model to: " + modelPath.toAbsolutePath());
        System.out.println("Saved header to: " + headerPath.toAbsolutePath());
    }

    private static Instances loadAndBuildInstances(String path) throws Exception {
        ArrayList<Attribute> attrs = new ArrayList<>();
        for (String f : FEATURE_NAMES) attrs.add(new Attribute(f));

        List<String> classVals = Arrays.asList("legitimate", "phishing");
        attrs.add(new Attribute("class", classVals));

        Instances data = new Instances("phishing_url_dataset", attrs, 1000);
        data.setClassIndex(data.numAttributes() - 1);

        int total = 0;
        int kept = 0;
        int skipped = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))) {
            String line;

            while ((line = br.readLine()) != null) {
                total++;

                if (line == null) { skipped++; continue; }

                line = line.replace("\u0000", "").trim();
                if (line.isEmpty()) { skipped++; continue; }

                if (total == 1) {
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.contains("url") && lower.contains("label")) {
                        skipped++;
                        continue;
                    }
                }

                String[] parts;
                if (line.contains("\t")) {
                    parts = line.split("\t");
                } else if (line.contains(",")) {
                    parts = line.split(",", 2);
                } else {
                    parts = line.split("\\s+", 2);
                }

                if (parts.length < 2) { skipped++; continue; }

                String url = parts[0].trim();
                String labelRaw = parts[parts.length - 1].trim();

                url = stripQuotes(url);
                String label = stripQuotes(labelRaw).toLowerCase(Locale.ROOT).trim();

                if (url.length() < 3) { skipped++; continue; }
                if (containsBinaryGarbage(url)) { skipped++; continue; }

                label = normalizeLabel(label);
                if (!classVals.contains(label)) { skipped++; continue; }

                double[] feats = UrlFeatureExtractor.extract(url);

                DenseInstance inst = new DenseInstance(data.numAttributes());
                inst.setDataset(data);

                for (int j = 0; j < feats.length; j++) {
                    inst.setValue(j, feats[j]);
                }
                inst.setValue(data.classAttribute(), label);

                data.add(inst);
                kept++;
            }
        }

        System.out.println("Loaded lines: total=" + total + ", kept=" + kept + ", skipped=" + skipped);

        if (kept < 50) {
            throw new IllegalStateException("Too few valid rows after cleaning. Kept=" + kept +
                    ". Check delimiter/labels in dataset.");
        }

        return data;
    }

    private static String normalizeLabel(String label) {
        if (label.equals("bad") || label.equals("phishing") || label.equals("malicious") || label.equals("1")) {
            return "phishing";
        }
        if (label.equals("good") || label.equals("legitimate") || label.equals("benign") || label.equals("0")) {
            return "legitimate";
        }
        if (label.equals("phish")) return "phishing";
        if (label.equals("legit")) return "legitimate";
        return label;
    }

    private static boolean containsBinaryGarbage(String s) {
        int nonPrintable = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') continue;
            if (c < 32 || c == 127) nonPrintable++;
        }
        return nonPrintable > 2;
    }

    private static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static String buildHeaderJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"features\": [\n");
        for (int i = 0; i < FEATURE_NAMES.length; i++) {
            sb.append("    \"").append(FEATURE_NAMES[i]).append("\"");
            sb.append(i == FEATURE_NAMES.length - 1 ? "\n" : ",\n");
        }
        sb.append("  ],\n");
        sb.append("  \"classValues\": [\"legitimate\", \"phishing\"],\n");
        sb.append("  \"algorithm\": \"RandomForest\",\n");
        sb.append("  \"note\": \"URL-only lexical + structural features; loader auto-detects delimiter and skips corrupted rows\"\n");
        sb.append("}\n");
        return sb.toString();
    }
}
