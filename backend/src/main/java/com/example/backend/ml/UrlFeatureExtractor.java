package com.example.backend.ml;

import java.net.IDN;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public class UrlFeatureExtractor {

    private static final Pattern IPV4 =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.|$)){4}$");

    public static double[] extract(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        String lower = url.toLowerCase(Locale.ROOT);

        Parsed p = parse(url);

        double usingIp = (p.host != null && IPV4.matcher(p.host).matches()) ? 1 : 0;
        double urlLength = url.length();
        double hasAt = lower.contains("@") ? 1 : 0;

        int protoIdx = lower.indexOf("://");
        double hasDoubleSlash;
        if (protoIdx >= 0) {
            String afterProto = lower.substring(protoIdx + 3);
            hasDoubleSlash = afterProto.contains("//") ? 1 : 0;
        } else {
            hasDoubleSlash = lower.contains("//") ? 1 : 0;
        }

        double numDots = countChar(url, '.');

        String domain = p.host == null ? "" : p.host.toLowerCase(Locale.ROOT);
        double prefixSuffix = domain.contains("-") ? 1 : 0;
        double httpsInDomain = domain.contains("https") ? 1 : 0;

        double numSubdomains = 0;
        if (!domain.isBlank()) {
            String[] parts = domain.split("\\.");
            if (parts.length > 2) numSubdomains = parts.length - 2;
        }

        double hasPunycode = domain.contains("xn--") ? 1 : 0;
        double digitRatio = digitRatio(url);
        double urlEntropy = shannonEntropy(url);

        return new double[]{
                usingIp,
                urlLength,
                hasAt,
                hasDoubleSlash,
                numDots,
                prefixSuffix,
                httpsInDomain,
                numSubdomains,
                hasPunycode,
                digitRatio,
                urlEntropy
        };
    }

    private static class Parsed {
        String host;
        Parsed(String host) { this.host = host; }
    }

    private static Parsed parse(String url) {
        try {
            URI uri = url.contains("://") ? URI.create(url) : URI.create("http://" + url);
            String host = uri.getHost();
            if (host == null) return new Parsed(null);
            host = IDN.toASCII(host);
            return new Parsed(host);
        } catch (Exception e) {
            return new Parsed(null);
        }
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    private static double digitRatio(String s) {
        if (s.isEmpty()) return 0.0;
        int digits = 0;
        for (int i = 0; i < s.length(); i++) if (Character.isDigit(s.charAt(i))) digits++;
        return (double) digits / (double) s.length();
    }

    private static double shannonEntropy(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        int[] freq = new int[256];
        int len = 0;
        for (char ch : s.toCharArray()) {
            if (ch < 256) {
                freq[ch]++;
                len++;
            }
        }
        if (len == 0) return 0.0;

        double ent = 0.0;
        for (int f : freq) {
            if (f == 0) continue;
            double p = (double) f / (double) len;
            ent -= p * (Math.log(p) / Math.log(2));
        }
        return ent;
    }
}
