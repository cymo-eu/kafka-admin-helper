FROM maven:3.9.6-eclipse-temurin-21-alpine as build
WORKDIR /app
COPY . /app
RUN mvn clean install
FROM amazoncorretto:21-alpine
COPY --from=build /app/run.sh /run.sh
RUN chmod +x /run.sh
COPY --from=build /app/target/libs/ /libs/
COPY --from=build /app/target/*.jar /KafkaAdminHelper.jar
ENTRYPOINT [ "/run.sh" ]