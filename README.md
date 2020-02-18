# TrafficCounter
Application, which reads the volume of captured traffic and sends alert message to Kafka

## Installation
To create an executable jar run `mvn clean install`.

## Before run
Before application execution you have to configure datasource in the `{path_to_spark}/spark/src/main/resources/appliication.properties` file 
and configure Apache Kafka properties in the `{path_to_spark}/spark/src/main/resources/kafka.properties` file.
Also, using other `.properties` files you can configure proccessess schedule and the alert message, that 
will be sent to the alert topic.

Also, before application execution, please make sure, that your database and Apache Kafka server 
are running on your machine.

## Eecution
To start the application run command `java -jar spark-0.0.1.jar <ip>`,
where `<ip>` parameter is optional, and represents the ip-addres you want to listen to.
If no ip is provided, then application will proccess all in\out traffic.

While working, application will write all logs to the console and in case of error
you can find logs under `{path_to_spark}/spark/logs`. These log files will be archived
every time applictaion runs in the folder `{path_to_spark}/spark/logs/archived`.
