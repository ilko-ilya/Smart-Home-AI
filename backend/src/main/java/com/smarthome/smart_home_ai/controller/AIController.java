package com.smarthome.smart_home_ai.controller;

import com.smarthome.smart_home_ai.dto.AiResponseDto;
import com.smarthome.smart_home_ai.dto.VoiceCommandDto;
import com.smarthome.smart_home_ai.service.AIService;
import com.smarthome.smart_home_ai.service.JarvisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final JarvisService jarvisService;

    @PostMapping("/recommendation")
    public AiResponseDto getRecommendation() {
        return aiService.getRecommendation();
    }

    @PostMapping("/voice")
    public String executeVoiceCommand(@RequestBody VoiceCommandDto command) {
        return jarvisService.processVoiceCommand(command.text());
    }

}
