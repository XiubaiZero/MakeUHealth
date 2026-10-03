package com.example.ipd_sp_back_end.assistant;

import java.util.*;

public record AssistantIntentDecision(AssistantIntent intent, String status, String reason, int historyMessages,
                                      long elapsedMillis, ChatCompletionResult completion) {
    public boolean degraded() { return "degraded".equals(status); }
    public static AssistantIntentDecision rules(AssistantIntent intent) { return new AssistantIntentDecision(intent,"rules","",0,0,null); }
    public Map<String,Object> metadata() {
        var result = new LinkedHashMap<String,Object>();
        result.put("category",intent == null ? "UNKNOWN" : intent.name()); result.put("status",status);
        result.put("reason",reason); result.put("promptVersion","rules".equals(status) ? "legacy-rules" : "intent-v1");
        result.put("historyMessages",historyMessages); result.put("elapsedMillis",elapsedMillis);
        result.put("usage",completion == null ? Map.of() : completion.usage());
        return Collections.unmodifiableMap(result);
    }
}
