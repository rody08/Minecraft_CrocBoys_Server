package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.channels.ClosedChannelException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConversationTurnTest {
    @Test
    void buildsRealResponsesApiConversationMessages() {
        assertEquals(
                "[{\"role\":\"user\",\"content\":\"[RXSpicy] hi \\\"Nyx\\\"\"}," +
                        "{\"role\":\"assistant\",\"content\":\"Hey!\"}]",
                OpenAiClient.conversationInputJson(List.of(
                        new AiConversationTurn("user", "[RXSpicy] hi \"Nyx\""),
                        new AiConversationTurn("assistant", "Hey!"))));
    }

    @Test
    void refusesUnknownRoles() {
        assertEquals("user", new AiConversationTurn("system", "ignored role").role());
    }

    @Test
    void prefersOllamaRelayWhenTheLocalTokenIsConfigured() {
        var resolved = OpenAiClient.resolveSettings(
                "openai",
                "https://api.openai.com/v1/responses",
                "gpt-5.6-luna",
                "",
                "relay-token-1234567890abcdef",
                "hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M",
                "",
                "");

        assertEquals("ollama", resolved.provider());
        assertEquals("hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M", resolved.model());
        assertEquals("http://127.0.0.1:11435/v1/responses", resolved.endpoint());
    }

    @Test
    void honorsConfiguredRemoteRelayEndpointWhenTheTokenIsPresent() {
        var resolved = OpenAiClient.resolveSettings(
                "ollama",
                "https://balloon-exist-nancy-architecture.trycloudflare.com/v1/responses",
                "hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M",
                "",
                "relay-token-1234567890abcdef",
                "hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M",
                "",
                "");

        assertEquals("ollama", resolved.provider());
        assertEquals("https://balloon-exist-nancy-architecture.trycloudflare.com/v1/responses", resolved.endpoint());
        assertEquals("hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M", resolved.model());
        assertEquals("relay-token-1234567890abcdef", resolved.apiKey());
    }

    @Test
    void buildsHttp11ClientForTunnelCompatibility() {
        HttpClient client = OpenAiClient.buildHttpClient();
        assertEquals(HttpClient.Version.HTTP_1_1, client.version());
    }

    @Test
    void extractsOllamaOutputTextFromMessageResponses() {
        String body = "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"The answer is forty-two.\"}]}]}";

        assertEquals("The answer is forty-two.", OpenAiClient.extractOutputText(body));
    }

    @Test
    void extractsOpenAiStyleTextFromChoicesContent() {
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Actually yes, that works.\"}}]}";

        assertEquals("Actually yes, that works.", OpenAiClient.extractOutputText(body));
    }

    @Test
    void treatsClosedChannelAndSocketResetAsTransientNetworkFailures() {
        assertTrue(OpenAiClient.isTransientFailure(new ClosedChannelException()));
        assertTrue(OpenAiClient.isTransientFailure(new IOException("Connection reset by peer")));
        assertFalse(OpenAiClient.isTransientFailure(new IllegalStateException("bad payload")));
    }
}
