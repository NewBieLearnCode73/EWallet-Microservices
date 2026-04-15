FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /spring_app
COPY ./build/libs/wallet-service-1.0.0-SNAPSHOT.jar /spring_app/wallet-service.jar

RUN java -Djarmode=layertools -jar wallet-service.jar extract

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /spring_app

COPY --from=build /spring_app/dependencies/ ./
COPY --from=build /spring_app/spring-boot-loader/ ./  
COPY --from=build /spring_app/snapshot-dependencies/ ./
COPY --from=build /spring_app/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]