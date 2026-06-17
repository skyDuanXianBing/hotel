package server.demo.service;

public interface MessageKnowledgeEmbeddingProvider {

    boolean isEnabled();

    String providerName();

    MessageKnowledgeEmbeddingResponse embed(String input);
}
