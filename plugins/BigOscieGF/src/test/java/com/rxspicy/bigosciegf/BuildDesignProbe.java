package com.rxspicy.bigosciegf;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

/** Manual local-GPU smoke test; not run by the unit test suite. */
public final class BuildDesignProbe {
    public static void main(String[] args) throws Exception {
        var limits = new BuildBlueprint.Limits(4096, 32);
        var http = HttpClient.newHttpClient();
        Path output = Path.of("build", "design-probe");
        Files.createDirectories(output);
        for (String subject : new String[]{"a red sports car with four black wheels and glass windows",
                "a small purple dragon statue with wings and a long tail", "a two-story birch house with windows and a doorway"}) {
            long start = System.nanoTime();
            var design = BuildBlueprint.generate(subject, limits, prompt -> {
            String payload = "{\"model\":\"" + json(args[0]) + "\",\"instructions\":\""
                    + json(BuildBlueprint.instructions(limits)) + "\",\"input\":\"" + json(prompt)
                    + "\",\"max_output_tokens\":4096,\"temperature\":0.2,\"stream\":false}";
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:11434/v1/responses"))
                    .timeout(Duration.ofSeconds(120)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
            String text = OpenAiClient.extractOutputText(response.body());
            Files.writeString(output.resolve(subject.split(" ")[2] + ".txt"), text == null ? "" : text);
            return text;
            });
            System.out.printf("PASS %s: %dx%dx%d, %d blocks, %.1f seconds%n", subject,
                    design.width(), design.height(), design.depth(), design.blocks().size(), (System.nanoTime() - start) / 1e9);
        }
    }
    private static String json(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
