package com.example.ipd_sp_back_end.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AssistantPromptResourcesTests {
    private ByteArrayResource resource(String text) {
        return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8), "test prompt");
    }

    @Test void removesOnlyTerminalLineBreaksAndPreservesBodyWhitespace() {
        String body = " leading space\n正文\r\nlast line ";
        for (String ending : new String[]{"", "\n", "\r\n", "\n\r\n"}) {
            assertEquals(body, AssistantPromptResources.read(resource(body + ending)));
        }
    }

    @Test void missingResourceReportsItsPathAndOriginalCause() {
        var error = assertThrows(IllegalStateException.class,
                () -> AssistantPromptResources.load("prompts/missing-test-prompt.txt"));
        assertTrue(error.getMessage().contains("prompts/missing-test-prompt.txt"));
        assertInstanceOf(IOException.class, error.getCause());
    }

    @Test void rejectsBlankTextAndBomBeforeAnyPromptCanBeUsed() {
        for (String text : new String[]{"", " \t\r\n", "\uFEFFinstructions"}) {
            var error = assertThrows(IllegalStateException.class,
                    () -> AssistantPromptResources.read(resource(text)));
            assertTrue(error.getMessage().contains("test prompt"));
            assertInstanceOf(IOException.class, error.getCause());
        }
    }

    @Test void bundledLanguageTemplatesHaveExactlyOnePlaceholder() {
        for (String file : new String[]{"answer-v1.txt", "memory-extraction-v1.txt", "summary-v1.txt"}) {
            String template = AssistantPromptResources.languageTemplate("prompts/" + file);
            assertEquals(1, template.split("\\Q" + AssistantPromptResources.LANGUAGE_INSTRUCTION + "\\E", -1).length - 1);
        }
        var error = assertThrows(IllegalStateException.class,
                () -> AssistantPromptResources.languageTemplate("prompts/english-rewrite-v1.txt"));
        assertTrue(error.getMessage().contains("prompts/english-rewrite-v1.txt"));
    }

    @Test void bundledFragmentsContainTextWithoutConnectionWhitespace() {
        for (String file : new String[]{"english-rewrite-v1.txt", "memory-instructions-v1.txt",
                "memory-facts-label-v1.txt", "conversation-summary-label-v1.txt"}) {
            String text = AssistantPromptResources.load("prompts/" + file);
            assertFalse(text.isBlank());
            assertEquals(text.strip(), text);
        }
    }
}
