package com.example.ipd_sp_back_end.assistant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({DeepSeekProperties.class, AssistantIntentProperties.class, com.example.ipd_sp_back_end.assistant.memory.AssistantMemoryProperties.class})
public class AssistantConfiguration { }
