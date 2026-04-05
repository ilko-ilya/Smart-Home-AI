package com.smarthome.smart_home_ai.dto;

import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.model.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceDto(

        Long id,

        @NotBlank(message = "Device name cannot be blank")
        String name,

        @NotNull(message = "Device type is required")
        DeviceType type,

        DeviceStatus status,
        String room,
        Double powerConsumption,
        Integer targetValue

) {
}
