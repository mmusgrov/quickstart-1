OVERVIEW
--------

Another example of STM and Vert.x integration. The quickstart is divided into 4 parts each introducing
a different facet of STM and Vert.x integration.

Demo 1 shows a single JVM running multiple vertx instances sharing the same volatile STM object.

Demo 2 uses the same STM object but makes it persistent and shows how to share it across address
spaces.

These two examples illustrate the different ways in which an application can be scaled whilst
maintaining data consistency:

 - verticle scaling by using better hardware so that more threads can do be used to service the
   workload;
 - horizontal scaling by using more servers so that the workload can be distributed to multiple JVMs

Demos 3 and 4 show services running on different endpoints communicating via HTTP and shared memory
respectively.

First build a fat jar that contains all the classes needed to run both demos in a single jar:

  mvn clean package

Use Case 1: Scaling by adding more threads
==========================================

  Charactertics:

  - RECOVERABLE and EXCLUSIVE
  - creates multiple vertx instances each instance using a handle to the same STM object
  
  What it's good for:

  - vertical scaling where adding better h/w is an option in order to support more threads in one JVM

  Features:

   This example shows a Theatre booking service:
   - an STM object is used to maintain all Theatre bookings
     * the STM object is volatile
   - multiple vertx instances each listening on the same HTTP endpoint
   - each vertx instance shares the same STM object
   - all vertx instances run in the same address space
     * concurrency is managed by the STM runtime
   - shows vertical scaling

Usage:
------

  java -cp target/stm-vertx-demo-5.6.0.Final-SNAPSHOT-fat.jar demo.demo1.VolatileTheatreVerticle 

  This will start You can change the port (8080) on which the verticle event loops will listen and the
     number of instances (10) of the verticles. Note that the version of the jar (5.6.0.Final-SNAPSHOT
     in this example) will change depending upon which narayana release you have.

Create 2 bookings:

  curl -X POST http://localhost:8080/api/theatre/A
  curl -X POST http://localhost:8080/api/theatre/A

observe how each request is serviced on a different verticle instance

Similarly performing GETs will show the same booking counts regardless of which verticle services it:

  curl -X GET http://localhost:8080/api/theatre 
  curl -X GET http://localhost:8080/api/theatre 

Use Case 2: Scaling by distributing the workload across JVMs
==========================================

Similar to use case 1 but uses persistent STM objects spread across different JVMs.

  Charactertics:

  - PERSISTENT and SHARED
  - theatre service verticles running in different JVMs sharing the same STM object
  - each JVM hosting multiple vertx instances each instance using a handle to the same STM object

  What it's good for:

  - horizontal scaling by using better hardware so that more threads can do be used to service the
    workload;

Usage:
------

  java -cp target/stm-vertx-demo-5.6.0.Final-SNAPSHOT-fat.jar demo.demo1.NonVolatileTheatreVerticle

  java -cp target/stm-vertx-demo-5.6.0.Final-SNAPSHOT-fat.jar demo.demo1.NonVolatileTheatreVerticle 0:ffffc0a80008:ae2f:5911a03e:1

Create two bookings using services running in different JVMs:

  curl -X POST http://localhost:8080/api/theatre/A
  curl -X POST http://localhost:8082/api/theatre/A

Check that each JVM reports the correct number of bookings (namely 2):

  curl -X GET http://localhost:8080/api/theatre
  curl -X GET http://localhost:8082/api/theatre

The final two use cases includes a verticle that shows a trip booking service (trip actor sending
messages to theatre and taxi actors) in two flavours:


Use Case 3: Three services running in different JVMs
===============================================

  Charactertics:

  - RECOVERABLE and EXCLUSIVE
  - creates multiple vertx instances each instance using a handle to the same STM object
  - one trip service verticle using http to send messages to the theatre and taxi verticles
    running in separate address spaces (no sharing of STM objects across address spaces)
  
  What it's good for:

  - isolating services from each other

Usage:
------

Start theatre and taxi services on two endpoints and then a trip service on another. The trip
service makes REST calls to fulfil booking requests. Services can be started in different JVMs or
or they can all run in the same JVM using different http endpoints:

  java -cp target/stm-vertx-demo-5.6.0.Final-SNAPSHOT-fat.jar demo.demo2.TripVerticle

This will start trip, theatre and taxi services on endpoints 8080, 8082 and 8084 respectively.
(If you wish you can run the theatre service in a separate JVM by passing the arg theatre.local=false
and starting it java -cp target/stm-vertx-demo-5.6.0.Final-SNAPSHOT-fat.jar demo.demo2.TheatreVerticle
and similarly for the TaxiVerticle).

Make a trip booking:

  curl -X POST http://localhost:8080/api/trip/Odeon/ABC

and a single taxi booking:

  curl -X POST http://localhost:8084/api/taxi/1

and check that the bookings were made (should be 2 theatre bookings and 3 taxi bookings):

  curl -X GET http://localhost:8082/api/theatre
  curl -X GET http://localhost:8084/api/taxi

Use Case 4: Similar to Use Case 3 except STM is used to interact with other services
===============================================

  Charactertics:
  - RECOVERABLE and EXCLUSIVE
  - verticles communicating by sending (HTTP) messages

  What it's good for:
  - composing transactional operations across different STM objects 


The trip service fulfils booking requests by updating shared STM objects representing the theatre and
taxi booking services respectively.

  java -cp target/stm-vertx-demo-5.6.0.Final-SNAPSHOT-fat.jar demo.demo2.TripSTMVerticle

Make two trip bookings:

  curl -X POST http://localhost:8080/api/trip/Odeon/ABC
  curl -X POST http://localhost:8080/api/trip/Odeon/ABC

observe that each booking is serviced by a different verticle. Check that number of theatre and taxi
bookings is correct:

  curl -X GET http://localhost:8080/api/trip/theatre
  curl -X GET http://localhost:8080/api/trip/taxi

