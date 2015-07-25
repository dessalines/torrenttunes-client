mvn clean install -DskipTests
java -Xmx1024m -jar target/torrenttunes-client.jar $@
