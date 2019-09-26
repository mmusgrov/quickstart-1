FROM fabric8/java-alpine-openjdk8-jre
# to get more info about the service in console you can add switch
# -Dquarkus.logging=TRACE
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV AB_ENABLED=jmx_exporter
COPY target/lib/* /deployments/lib/
COPY target/*-runner.jar /deployments/app.jar
ENTRYPOINT [ "/deployments/run-java.sh" ]
EXPOSE 8080

