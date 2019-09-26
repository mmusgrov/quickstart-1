
mvn clean package
java -jar stm/target/stm-quarkus-quickstart-5.9.9.Final-SNAPSHOT-runner.jar &
curl -XPOST http://localhost:8080/stm
java -jar stress/target/stm-quarkus-stress-5.9.9.Final-SNAPSHOT.jar requests=100 parallelism=50 url=/stm
curl -XGET http://localhost:8080/stm

[mmusgrov@dev1 quarkus] (JBTM-3197) $ java -jar stm/target/stm-quarkus-quickstart-5.9.9.Final-SNAPSHOT-runner.jar &
[1] 22434
[mmusgrov@dev1 quarkus] (JBTM-3197) $ 14:44:32 INFO  [io.quarkus] (main) Quarkus 0.21.1 started in 0.902s. Listening on: http://[::]:8080
14:44:32 INFO  [io.quarkus] (main) Installed features: [cdi, resteasy, vertx]

[mmusgrov@dev1 quarkus] (JBTM-3197) $ curl -XPOST http://localhost:8080/stm
14:44:37 INFO  [co.ar.at.arjuna] (ForkJoinPool.commonPool-worker-1) ARJUNA012170: TransactionStatusManager started on port 44931 and host 127.0.0.1 with service com.arjuna.ats.arjuna.recovery.ActionStatusService
ForkJoinPool.commonPool-worker-1:  Booking Count=1[mmusgrov@dev1 quarkus] (JBTM-3197) $ 
[mmusgrov@dev1 quarkus] (JBTM-3197) $ java -jar stress/target/stm-quarkus-stress-5.9.9.Final-SNAPSHOT.jar requests=100 parallelism=50 url=/stm
Waiting for 5000 requests
0 out of 5000 requests failed in 1608 ms
[mmusgrov@dev1 quarkus] (JBTM-3197) $ curl -XGET http://localhost:8080/stm
ForkJoinPool.commonPool-worker-1:  Booking Count=5001[mmusgrov@dev1 quarkus] (JBTM-3197) $ 
[mmusgrov@dev1 quarkus] (JBTM-3197) $ 


