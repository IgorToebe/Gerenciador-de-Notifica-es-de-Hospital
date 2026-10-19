FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:resolve

COPY src ./src
RUN mvn -B package -DskipTests

FROM tomcat:9.0-jre11-temurin

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Sobrescreve o logging.properties para enviar TODOS os logs de contexto e listeners para o ConsoleHandler (stdout)
RUN printf '.handlers = java.util.logging.ConsoleHandler\n\
.level = INFO\n\
java.util.logging.ConsoleHandler.level = ALL\n\
java.util.logging.ConsoleHandler.formatter = org.apache.juli.OneLineFormatter\n\
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].level = ALL\n\
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].handlers = java.util.logging.ConsoleHandler\n' > /usr/local/tomcat/conf/logging.properties

# Limpa todas as aplicações padrão do Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copia qualquer .war gerado no target e o renomeia para ROOT.war
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
