# One Dockerfile for both services: docker build --build-arg SERVICE=product-service .
FROM eclipse-temurin:17-jdk-alpine AS build
ARG SERVICE
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
RUN sh ./mvnw -q -B -pl ${SERVICE} -am dependency:go-offline
COPY product-service/src product-service/src
COPY order-service/src order-service/src
RUN sh ./mvnw -q -B -pl ${SERVICE} -am package -DskipTests && cp ${SERVICE}/target/${SERVICE}.jar /app/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/app.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
