# 1. (변경) 기존 openjdk 대신 amazoncorretto 사용
FROM amazoncorretto:17

# 2. 작업 폴더 설정
WORKDIR /app

# 3. jar 파일 복사
COPY build/libs/*.jar app.jar

# 4. 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]