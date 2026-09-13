package com.rxspicy.bigosciegf;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlueprintTransportTest {
    @Test void limitsTotalBodyAcrossNetworkChunks() {
        var body = new OpenAiClient.LimitedBodySubscriber(5);
        var subscription = mock(Flow.Subscription.class);
        body.onSubscribe(subscription);
        body.onNext(List.of(ByteBuffer.wrap(new byte[3])));
        body.onNext(List.of(ByteBuffer.wrap(new byte[3])));
        assertThrows(CompletionException.class, () -> body.getBody().toCompletableFuture().join());
        verify(subscription).cancel();
    }

    @Test void decodesUtf8OnlyAfterAllChunksArrive() {
        var body = new OpenAiClient.LimitedBodySubscriber(10);
        body.onSubscribe(mock(Flow.Subscription.class));
        byte[] bytes = "café".getBytes(StandardCharsets.UTF_8);
        body.onNext(List.of(ByteBuffer.wrap(bytes, 0, 4)));
        body.onNext(List.of(ByteBuffer.wrap(bytes, 4, 1)));
        body.onComplete();
        assertEquals("café", body.getBody().toCompletableFuture().join());
    }
}
