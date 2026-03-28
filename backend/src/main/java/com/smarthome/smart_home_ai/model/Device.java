package com.smarthome.smart_home_ai.model;

import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.model.enums.DeviceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private DeviceType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.OFF;

    private String room;
    private Double powerConsumption;
    private Integer targetValue;

}
