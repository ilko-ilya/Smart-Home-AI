package com.smarthome.smart_home_ai.mapper;

import com.smarthome.smart_home_ai.dto.DeviceDto;
import com.smarthome.smart_home_ai.model.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeviceMapper {

    DeviceDto toDto(Device device);

    @Mapping(target = "status", source = "status", defaultValue = "OFF")
    Device toEntity(DeviceDto dto);

}
