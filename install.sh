mvn clean install -DskipTests
java -Xmx768m -jar target/torrenttunes-client.jar $@
