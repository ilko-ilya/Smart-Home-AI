package com.smarthome.smart_home_ai.service;

import com.smarthome.smart_home_ai.model.Device;
import com.smarthome.smart_home_ai.model.enums.DeviceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final DeviceService deviceService;

    // Використовуємо Set для миттєвого пошуку O(1)
    private static final Set<String> SECURITY_TYPES = Set.of(
            "SECURITY_SENSOR", "MOTION_SENSOR", "DOOR_SENSOR", "VACUUM"
    );

    @Transactional
    public Optional<String> handleScenario(String text) {
        // Переводимо все в нижній регістр один раз для надійності
        String lowerText = text.toLowerCase();

        if (isLeavingHome(lowerText)) return Optional.of(handleLeavingHome());
        if (isComingHome(lowerText)) return Optional.of(handleComingHome());

        return Optional.empty();
    }

    private boolean isLeavingHome(String text) {
        // Додані російські варіанти, які присилає мікрофон
        return text.contains("йду") || text.contains("иду") ||
                text.contains("виходжу") || text.contains("ухожу") ||
                text.contains("пішов") || text.contains("ушел") ||
                text.contains("охорону") || text.contains("я пошёл") || text.contains("пошел");
    }

    private boolean isComingHome(String text) {
        // Додані російські варіанти, які присилає мікрофон
        return text.contains("вдома") || text.contains("дома") || text.contains("дому") ||
                text.contains("прийшов") || text.contains("пришел") ||
                text.contains("повернувся") || text.contains("вернулся");
    }

    private String handleLeavingHome() {
        // Отримуємо сутності через сервіс-прошарок
        List<Device> devices = deviceService.getAllEntities();

        for (Device device : devices) {
            if (SECURITY_TYPES.contains(device.getType().name())) {
                device.setStatus(DeviceStatus.ON);
            } else {
                // ЖОРСТКО вимикаємо все інше
                device.setStatus(DeviceStatus.OFF);
            }
        }
        // Зберігаємо через сервіс-прошарок
        deviceService.saveAllEntities(devices);
        return "Протокол захисту активовано, сер. Будинок під охороною, прилади знеструмлено.";
    }

    private String handleComingHome() {
        List<Device> devices = deviceService.getAllEntities();

        for (Device device : devices) {
            String type = device.getType().name();
            // Беремо кімнату (toUpperCase для надійного порівняння)
            String room = device.getRoom() != null ? device.getRoom().toUpperCase() : "";

            if (SECURITY_TYPES.contains(type)) {
                // Вимикаємо охорону та пилосос
                device.setStatus(DeviceStatus.OFF);
            } else if (type.equals("STEREO")) {
                // Вмикаємо музику
                device.setStatus(DeviceStatus.ON);
                device.setTargetValue(20);

                // 🔥 Пункт 2.3: Надійний пошук! Вмикаємо СВІТЛО, яке знаходиться у ВІТАЛЬНІ
            } else if (type.equals("LIGHT") && room.contains("ВІТАЛЬНЯ") && !device.getName().toLowerCase().contains("торшер")) {
                device.setStatus(DeviceStatus.ON);
                device.setTargetValue(100);
            } else {
                // ЖОРСТКО вимикаємо абсолютно все інше
                device.setStatus(DeviceStatus.OFF);
            }
        }
        deviceService.saveAllEntities(devices);
        return "З поверненням, сер! Охорону знято. Вмикаю світло та легку музику.";
    }
}