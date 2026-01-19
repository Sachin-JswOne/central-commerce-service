FROM asia-south1-docker.pkg.dev/modular-bucksaw-305821/jopl/otel-java21:latest

#Add a user with name jswuser into user group jswuser
RUN groupadd -r jswuser && useradd -r -g jswuser jswuser
#Run Container as jswuser
USER jswuser

COPY central-commerce-application/target/central-commerce-application-0.0.1-SNAPSHOT.jar /app/app.jar

# Service configuration
ENV OTEL_SERVICE_NAME=central-commerce-service
ENV OTEL_RESOURCE_ATTRIBUTES=service.version=0.0.1,team=ccp,environment=qa

ENTRYPOINT ["java -javaagent:/opt/opentelemetry-javaagent.jar -jar /app/app.jar"]