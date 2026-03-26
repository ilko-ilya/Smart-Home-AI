package com.smarthome.smart_home_ai.dto;

import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.model.enums.DeviceType;

public record DeviceDto(

        Long id,
        String name,
        DeviceType type,
        DeviceStatus status,
        String room,
        Double powerConsumption,
        Integer targetValue

) {
}
