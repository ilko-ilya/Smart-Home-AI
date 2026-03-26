package com.smarthome.smart_home_ai.dto;

import java.time.LocalDateTime;

public record SensorDataDto(

        Long id,
        Double temperature,
        Double humidity,
        LocalDateTime timestamp

) {
}
