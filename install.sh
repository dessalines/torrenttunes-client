mvn clean install -DskipTests
java -Xmx2048m -jar target/torrenttunes-client.jar $@

