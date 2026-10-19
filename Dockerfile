FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml .
# Utiliza dependency:resolve para evitar travamentos no download de plugins
RUN mvn -B dependency:resolve

COPY src ./src
RUN mvn -B package -DskipTests

FROM tomcat:9.0-jre11-temurin

# Permite que a JVM gerencie a memória adequadamente dentro do container (Máximo de 75% da RAM alocada no Code Engine)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Redireciona os erros de inicialização de listeners e contexto para o ConsoleHandler (stdout)
RUN echo "org.apache.catalina.core.ContainerBase.[Catalina].[localhost].handlers = java.util.logging.ConsoleHandler" >> /usr/local/tomcat/conf/logging.properties

# Limpa todas as aplicações padrão do Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copia qualquer .war gerado no target e o renomeia para ROOT.war
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# O CMD deve ser sempre a última instrução do Dockerfile
CMD ["catalina.sh", "run"]
