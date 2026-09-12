package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
