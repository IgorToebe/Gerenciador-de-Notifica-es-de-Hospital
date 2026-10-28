FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# 1. Trocando a imagem para a versão da Eclipse Temurin (mais atualizada)
FROM tomcat:9.0-jre11-temurin

# Confia na CA autoassinada ("IBM Cloud Databases") do IBM Db2 on Cloud, exigida pela
# conexao SSL (DB2_SSL=true). Sem isso a JVM falha com SSLHandshakeException/PKIX.
COPY db2-ca.crt /tmp/db2-ca.crt
RUN keytool -importcert -noprompt -trustcacerts -cacerts -storepass changeit \
    -alias ibm-cloud-databases -file /tmp/db2-ca.crt && rm /tmp/db2-ca.crt

# 2. Injetando a variável para ignorar o cgroup v2
ENV JAVA_OPTS="-XX:-UseContainerSupport"

RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080