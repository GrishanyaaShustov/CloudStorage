# Используем лёгкий JRE образ
FROM eclipse-temurin:21-jre

LABEL authors="Grisha"

# Рабочая директория внутри контейнера
WORKDIR /app

# Копируем jar-файл из target
COPY target/*.jar app.jar

# Открываем порт приложения
EXPOSE 8082

# Запуск Spring Boot приложения
ENTRYPOINT ["java", "-jar", "app.jar"]