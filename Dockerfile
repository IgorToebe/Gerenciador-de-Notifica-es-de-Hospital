FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:resolve

COPY src ./src
RUN mvn -B package -DskipTests

FROM tomcat:9.0-jre11-temurin

# 1. Configura a JVM para output detalhado de logs e gerenciamento de memória
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dorg.apache.juli.logging.UserDataHelper.SUPPRESS_ALL=false"

# Ajuste no Dockerfile para focar em erros e avisos
RUN printf '.handlers = java.util.logging.ConsoleHandler\n\
.level = INFO\n\
java.util.logging.ConsoleHandler.level = INFO\n\
java.util.logging.ConsoleHandler.formatter = org.apache.juli.OneLineFormatter\n\
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].level = INFO\n' > /usr/local/tomcat/conf/logging.properties

# 3. Limpa todas as aplicações padrão do Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# 4. Copia o arquivo .war gerado
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# 5. Executa o catalina.sh garantindo que o stdout e stderr sejam jogados no console do container
CMD ["catalina.sh", "run"]
