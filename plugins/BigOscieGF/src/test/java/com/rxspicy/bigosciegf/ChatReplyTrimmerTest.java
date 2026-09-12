package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatReplyTrimmerTest {
    @Test
    void keepsShortRepliesUntouched() {
        assertEquals("Fine. Try not to lose it.", ChatReplyTrimmer.fit("Fine. Try not to lose it.", 80));
    }

    @Test
    void endsAtTheLastCompleteSentence() {
        assertEquals("Here, take this sword.", ChatReplyTrimmer.fit(
                "Here, take this sword. I was also going to explain several unrelated things about it.", 45));
    }

    @Test
    void fallsBackToAWholeWordWhenThereIsNoSentenceBoundary() {
        assertEquals("This answer has no useful…", ChatReplyTrimmer.fit(
                "This answer has no useful sentence boundary anywhere", 28));
    }

}
