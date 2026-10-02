package com.example.ipd_sp_back_end.assistant;


public interface AssistantIntentClassifier {
    AssistantIntent classify(String question);
}
