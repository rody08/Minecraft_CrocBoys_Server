package com.rxspicy.bigosciegf;

record AiConversationTurn(String role, String content) {
    AiConversationTurn {
        role = "assistant".equals(role) ? "assistant" : "user";
        content = content == null ? "" : content.trim();
    }
}
