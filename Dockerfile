FROM eclipse-temurin:21


#Add a user with name jswuser into user group jswuser
RUN groupadd -r jswuser && useradd -r -g jswuser jswuser
#Run Container as jswuser
USER jswuser

VOLUME /tmp
ADD central-commerce-application/target/central-commerce-application-0.0.1-SNAPSHOT.jar app.jar
#ENTRYPOINT ["java","-jar","/app.jar"]
ENTRYPOINT ["java","-jar","/app.jar"]