# Шаг 1: Сборка jar-файла (используем образ с Maven и Java 21)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Сначала копируем только pom.xml и качаем зависимости.
# Докер закеширует этот слой. Это сэкономит кучу времени при следующих сборках!
COPY pom.xml .
RUN mvn dependency:go-offline

# Теперь копируем исходный код и собираем проект (без тестов для скорости)
COPY src ./src
RUN mvn clean package -DskipTests

# Шаг 2: Финальный легкий образ (берем только JRE, без тяжелого Maven)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем готовый jar из первого шага (builder)
COPY --from=builder /build/target/*.jar app.jar

# Открываем порт бэкенда
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["java", "-jar", "app.jar"]