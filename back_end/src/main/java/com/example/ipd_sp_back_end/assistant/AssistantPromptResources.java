package com.example.ipd_sp_back_end.assistant;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Loads immutable prompt text; callers own separators and runtime data. */
public final class AssistantPromptResources {
    public static final String LANGUAGE_INSTRUCTION = "{{languageInstruction}}";

    private AssistantPromptResources() { }

    public static String load(String path) {
        return read(new ClassPathResource(path));
    }

    public static String languageTemplate(String path) {
        String text = load(path);
        int first = text.indexOf(LANGUAGE_INSTRUCTION);
        if (first < 0 || text.indexOf(LANGUAGE_INSTRUCTION, first + LANGUAGE_INSTRUCTION.length()) >= 0) {
            throw new IllegalStateException("Expected one language instruction placeholder in prompt: " + path);
        }
        return text;
    }

    static String read(Resource resource) {
        try (var stream = resource.getInputStream()) {
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) throw new IOException("UTF-8 BOM is not allowed.");
            int end = text.length();
            while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) end--;
            text = text.substring(0, end);
            if (text.isBlank()) throw new IOException("Prompt text must not be blank.");
            return text;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load assistant prompt: " + resource.getDescription(), exception);
        }
    }
}
