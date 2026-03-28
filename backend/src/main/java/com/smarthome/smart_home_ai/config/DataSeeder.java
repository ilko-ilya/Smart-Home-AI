package com.smarthome.smart_home_ai.config;

import com.smarthome.smart_home_ai.model.Device;
import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import com.smarthome.smart_home_ai.model.enums.DeviceType;
import com.smarthome.smart_home_ai.repository.DeviceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.smarthome.smart_home_ai.model.enums.DeviceStatus.OFF;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.AC;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.APPLIANCE;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.COFFEE_MAKER;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.DISHWASHER;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.DOOR_SENSOR;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.HEATED_FLOOR;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.KETTLE;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.LIGHT;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.MOTION_SENSOR;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.SECURITY_SENSOR;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.STEREO;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.TV;
import static com.smarthome.smart_home_ai.model.enums.DeviceType.VACUUM;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedDatabase(DeviceRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                    // Порядок: id, name, type, status, room, powerConsumption, targetValue
                    new Device(null, "Основне світло", LIGHT, OFF, "Вітальня", 30.0, 100),
                    new Device(null, "Торшер біля дивана", LIGHT, OFF, "Вітальня", 15.0, 80),
                    new Device(null, "Стельове світло", LIGHT, OFF, "Спальня", 20.0, 100),
                    new Device(null, "Світло", LIGHT, OFF, "Кухня", 20.0, 100),
                    new Device(null, "Кавоварка", COFFEE_MAKER, OFF, "Кухня", 1200.0, null),
                    new Device(null, "Чайник", KETTLE, OFF, "Кухня", 2000.0, null),
                    new Device(null, "Посудомийна машина", DISHWASHER, OFF, "Кухня", 1800.0, null),
                    new Device(null, "Телевізор", TV, OFF, "Вітальня", 150.0, 50),
                    new Device(null, "Стереосистема", STEREO, OFF, "Вітальня", 100.0, 30),
                    new Device(null, "Датчик розбиття скла", SECURITY_SENSOR, DeviceStatus.ON, "Вітальня", 2.0, null),
                    new Device(null, "Кондиціонер", AC, OFF, "Спальня", 900.0, 22),
                    new Device(null, "Праска", APPLIANCE, OFF, "Спальня", 2200.0, null),
                    new Device(null, "Робот-пилосос", VACUUM, OFF, "Коридор", 40.0, null),
                    new Device(null, "Тепла підлога", HEATED_FLOOR, OFF, "Ванна", 500.0, 25),
                    new Device(null, "Датчик руху", MOTION_SENSOR, OFF, "Коридор", 5.0, null),
                    new Device(null, "Датчик відкриття дверей", DOOR_SENSOR, OFF, "Коридор", 5.0, null)
                ));
                System.out.println("✅ БАЗА ДАННЫХ УСПЕШНО ЗАПОЛНЕНА (16 устройств)!");
            }
        };
    }
}