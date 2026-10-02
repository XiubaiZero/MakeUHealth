package com.example.ipd_sp_back_end.assistant;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AssistantResponseProcessor {
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\p{IsHan}]");

    public String clean(String answer) { return answer.trim(); }

    public boolean needsEnglishRewrite(boolean chinese, String answer) {
        return !chinese && containsCjk(answer);
    }

    private boolean containsCjk(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return CJK_PATTERN.matcher(text).find();
    }
}
