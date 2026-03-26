package com.smarthome.smart_home_ai.mapper;

import com.smarthome.smart_home_ai.dto.SensorDataDto;
import com.smarthome.smart_home_ai.model.SensorData;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SensorDataMapper {

    SensorDataDto toDto(SensorData entity);

    SensorData toEntity(SensorDataDto dto);

}
