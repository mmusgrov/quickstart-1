
OVERVIEW
--------
This example demonstrates usage of an STM object running with Quarkus .
The example is a JAX-RS resource that manages a global counter.
Concurrent reads an writes to the counter are protected using STM.

USAGE
-----
  mvn clean verify

This command with run an integration test.

If you want to stress test the example to verify that there is no corruption when multiple requests run concurrent the perform the following steps:

start the application:

  java -jar stm/target/stm-quarkus-quickstart-5.9.9.Final-SNAPSHOT-runner.jar &

perform multiple JAX-RS in parallel :

  java -jar stress/target/stm-quarkus-stress-5.9.9.Final-SNAPSHOT.jar requests=100 parallelism=50 url=/stm

This request starts 50 threads each of which issues 100 requests to update the counter.
The final value should be 5000 (100 times 50). You can verify this by issue an HTTP GET request.
For example if you have curl then execute the following command:

  curl -XGET http://localhost:8080/stm

and you should see output that reports the thread id and the booking count:

> ForkJoinPool.commonPool-worker-1:  Booking Count=5000

If you would like to remove the protection that STM offers then take a look at the patch file called
`without-stm.patch`. It just uses an unprotected instance of the FlightService object.

If you recompile and restart the application and issue the stress test command then the curl request should
report a value of the counter that is less than 5000. This is because some of the requests will interleave
and the counter will be overwritten.

Running on OpenShift
-------


