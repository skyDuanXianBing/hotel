package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageKnowledgeThreadExtractionOutputParserTest {

    @Test
    void parse_shouldReadMultipleStrictJsonItems() {
        MessageKnowledgeThreadExtractionOutputParser parser =
                new MessageKnowledgeThreadExtractionOutputParser(new ObjectMapper());

        MessageKnowledgeThreadParsedOutput output = parser.parse("""
                {"schemaVersion":"message_knowledge_thread_v1","items":[
                  {"clientItemId":"a","topicCode":"wifi","canonicalQuestion":"WiFi?",
                   "canonicalAnswer":"The WiFi password is shared at check-in.",
                   "confidence":0.91,"sourceMessageIds":[101,102]},
                  {"clientItemId":"b","topic_code":"breakfast","canonical_question":"Breakfast?",
                   "canonical_answer":"Breakfast starts at 07:00.",
                   "confidence":"0.82","source_message_ids":["103","104"]}
                ]}
                """);

        assertEquals("message_knowledge_thread_v1", output.schemaVersion());
        assertEquals(2, output.items().size());
        assertEquals("wifi", output.items().get(0).topicCode());
        assertEquals(102L, output.items().get(0).sourceMessageIds().get(1));
        assertEquals("breakfast", output.items().get(1).topicCode());
        assertEquals(104L, output.items().get(1).sourceMessageIds().get(1));
    }

    @Test
    void parse_shouldRejectMalformedOutput() {
        MessageKnowledgeThreadExtractionOutputParser parser =
                new MessageKnowledgeThreadExtractionOutputParser(new ObjectMapper());

        assertThrows(IllegalArgumentException.class, () -> parser.parse("not-json"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{bad-json"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{\"items\":{}}"));
    }
}
