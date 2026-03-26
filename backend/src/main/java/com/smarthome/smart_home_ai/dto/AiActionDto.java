package com.smarthome.smart_home_ai.dto;

public record AiActionDto(

        Long deviceId,
        String targetStatus,
        Integer targetValue

) {
}
