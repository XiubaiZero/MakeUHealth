package com.example.ipd_sp_back_end.assistant.memory;

import com.example.ipd_sp_back_end.assistant.AssistantPrompt;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Component
public class AssistantContextBudget {
    private final AssistantMemoryProperties properties;
    public AssistantContextBudget(AssistantMemoryProperties properties) { this.properties=properties; properties.validate(); }
    // UTF-8 byte units deliberately overestimate token usage; API usage is the measurement.
    public static int estimate(String text) { return text.getBytes(StandardCharsets.UTF_8).length; }
    public static int estimate(AssistantPrompt prompt) {
        return 32 + estimate(prompt.systemPrompt()) + estimate(prompt.userPrompt())
            + prompt.history().stream().mapToInt(m -> 16+estimate(m.content())).sum();
    }
    public AssistantPrompt fit(AssistantPrompt prompt, int outputTokens) {
        int limit=Math.min(properties.getInputBudget(),properties.getContextWindow()-outputTokens);
        var history=new ArrayList<>(prompt.history());
        var result=new AssistantPrompt(prompt.systemPrompt(),prompt.userPrompt(),history);
        while (estimate(result)>limit && !history.isEmpty()) {
            history.remove(0);
            if (!history.isEmpty() && "assistant".equals(history.get(0).role())) history.remove(0);
            result=new AssistantPrompt(prompt.systemPrompt(),prompt.userPrompt(),history);
        }
        if (estimate(result)>limit) throw new IllegalArgumentException("Required context exceeds the input budget. Shorten your question or reduce saved memory.");
        return result;
    }
}
