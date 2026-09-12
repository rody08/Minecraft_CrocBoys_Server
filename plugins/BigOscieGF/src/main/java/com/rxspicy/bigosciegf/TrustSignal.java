package com.rxspicy.bigosciegf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts the model's constrained emotional trust-state decision. */
record TrustSignal(int score) {
    private static final Pattern VALID = Pattern.compile("(?is)\\s*\\[\\[TRUST:\\s*(0|50|100)\\s*]]");
    private static final Pattern ANY = Pattern.compile("(?is)\\s*\\[\\[TRUST:.*?]]");

    static Extraction extract(String response) {
        String source = response == null ? "" : response;
        Matcher matcher = VALID.matcher(source);
        TrustSignal signal = matcher.find() ? new TrustSignal(Integer.parseInt(matcher.group(1))) : null;
        return new Extraction(ANY.matcher(source).replaceAll("").trim(), signal);
    }

    record Extraction(String visibleText, TrustSignal signal) {}
}
