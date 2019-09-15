FROM openjdk:8-jre-alpine

RUN wget https://repo.maven.apache.org/maven2/org/apache/meecrowave/meecrowave-core/1.2.8/meecrowave-core-1.2.8-runner.jar -O /opt/meecrowave-core-runner.jar
ADD target/address-validation-service.war /opt/address-validation-service.war

EXPOSE 4003
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/opt/meecrowave-core-runner.jar", "--host", "0.0.0.0", "--http", "4003", "--webapp", "/opt/address-validation-service.war"]
