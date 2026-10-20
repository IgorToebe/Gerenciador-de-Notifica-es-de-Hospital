FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:resolve

COPY src ./src
RUN mvn -B package -DskipTests

FROM tomcat:9.0-jre11-temurin

# 1. Configura a JVM para output detalhado de logs e gerenciamento de memória
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dorg.apache.juli.logging.UserDataHelper.SUPPRESS_ALL=false"

# 2. Configura o logging.properties para redirecionar TODAS as categorias para o Console (stdout)
RUN printf 'handlers = java.util.logging.ConsoleHandler\n\
.handlers = java.util.logging.ConsoleHandler\n\
.level = ALL\n\
java.util.logging.ConsoleHandler.level = ALL\n\
java.util.logging.ConsoleHandler.formatter = org.apache.juli.OneLineFormatter\n\
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].level = ALL\n\
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].handlers = java.util.logging.ConsoleHandler\n\
org.apache.catalina.level = ALL\n\
org.apache.catalina.handlers = java.util.logging.ConsoleHandler\n\
org.apache.jasper.level = ALL\n\
org.apache.jasper.handlers = java.util.logging.ConsoleHandler\n' > /usr/local/tomcat/conf/logging.properties

# 3. Limpa todas as aplicações padrão do Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# 4. Copia o arquivo .war gerado
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# 5. Executa o catalina.sh garantindo que o stdout e stderr sejam jogados no console do container
CMD ["catalina.sh", "run"]
