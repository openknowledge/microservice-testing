FROM jboss/wildfly:17.0.1.Final

ADD src/main/wildfly/modules/org/postgresql /opt/jboss/wildfly/modules/system/layers/base/org/postgresql
ADD target/postgresql/postgresql-42.2.5.jar /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/
ADD src/main/wildfly/configuration/standalone-postgresql.xml /opt/jboss/wildfly/standalone/configuration/

ADD target/delivery-service.war /opt/jboss/wildfly/standalone/deployments/

EXPOSE 4002

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "--server-config=standalone-postgresql.xml", "-Djboss.http.port=4002", "-Ddatabase.url=${env.DELIVERY_DB_URL}"]
