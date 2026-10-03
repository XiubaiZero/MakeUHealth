package com.example.ipd_sp_back_end.service;

import com.example.ipd_sp_back_end.assistant.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Documents the original routing limits; semantic routing must not reuse these gates. */
class AssistantIntentBaselineTests {
    @Test void legacyRulesRemainAvailableForLocalModeAndRollback() {
        var rules = new RuleBasedAssistantIntentClassifier();
        assertEquals(AssistantIntent.CAPABILITY, rules.classify("Can you help me lose weight?"));
        assertEquals(AssistantIntent.OUT_OF_SCOPE, rules.classify("I feel dizzy. What should I do?"));
        assertEquals(AssistantIntent.HEALTH, rules.classify("Please debug my Python training script."));
        assertEquals(AssistantIntent.OUT_OF_SCOPE, rules.classify("Could you explain that again?"));
    }
}
