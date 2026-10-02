package com.example.ipd_sp_back_end.assistant.memory;

import com.example.ipd_sp_back_end.assistant.AssistantPrompt;
import java.util.List;

public record MemorySnapshot(boolean enabled, long accountRevision, long settingsRevision, long contentRevision,
                             String summary, long coveredSequence, List<Round> rounds, List<Round> olderRounds,
                             int totalRounds, List<String> facts) {
    public MemorySnapshot { rounds=List.copyOf(rounds); olderRounds=List.copyOf(olderRounds); facts=List.copyOf(facts); }
    public static MemorySnapshot empty() { return new MemorySnapshot(false,0,0,0,"",0,List.of(),List.of(),0,List.of()); }
    public boolean hasContext() { return enabled && (!rounds.isEmpty() || !summary.isBlank() || !facts.isEmpty()); }
    public List<AssistantPrompt.Message> history() {
        return rounds.stream().flatMap(r -> java.util.stream.Stream.of(new AssistantPrompt.Message("user",r.question()),new AssistantPrompt.Message("assistant",r.answer()))).toList();
    }
    public record Round(long questionSequence, long answerSequence, String question, String answer) { }
}
