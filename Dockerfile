# Step 1: Gradle 빌드 스테이지
FROM gradle:7.6-jdk17 AS build
WORKDIR /app

# Gradle Wrapper와 소스 코드 복사
COPY . .

# JAR 파일 빌드
RUN ./gradlew bootJar

# Step 2: 실행 스테이지
FROM openjdk:17-jdk-slim
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/notify-me.jar app.jar

# 실행 명령어 지정
ENTRYPOINT ["java", "-jar", "app.jar"]