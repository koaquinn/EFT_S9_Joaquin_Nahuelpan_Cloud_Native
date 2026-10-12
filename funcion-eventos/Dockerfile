FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
COPY host.json .
RUN mvn -q -DskipTests clean package

FROM mcr.microsoft.com/azure-functions/java:4-java17
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

COPY --from=build /src/target/azure-functions/funcion-eventos/ /home/site/wwwroot

EXPOSE 80
