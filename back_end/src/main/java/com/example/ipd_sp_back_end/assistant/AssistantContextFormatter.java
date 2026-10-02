package com.example.ipd_sp_back_end.assistant;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
@Component
public class AssistantContextFormatter {
    public String formatContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "No user context provided.";
        }

        StringBuilder builder = new StringBuilder();
        context.forEach((key, value) -> builder.append(key).append(": ").append(value).append('\n'));
        return builder.toString().trim();
    }

    public String formatConstraints(List<String> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return "None";
        }

        StringBuilder builder = new StringBuilder();
        for (String constraint : constraints) {
            builder.append("- ").append(constraint).append('\n');
        }
        return builder.toString().trim();
    }
}
