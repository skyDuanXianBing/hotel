package server.demo.service;

import java.util.List;

public record MessageKnowledgeEmbeddingResponse(
        List<Float> vector,
        String provider,
        String model,
        Integer dimensions
) {
}
