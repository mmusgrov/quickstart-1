
#mvn clean package
java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar &
sleep 6
curl -w "\n" http://localhost:8080/send # send two messages in a JTA transaction
curl -w "\n" http://localhost:8080/receive # receive two messages in a JTA transaction
curl -w "\n" http://localhost:8080/fail # halt the VM after prepare but before the commit phase
java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar & # the last command will have halted the VM
pid=$!
sleep 6
curl -w "\n" http://localhost:8080/recover # recover the messages
kill $pid
