<!--
JBoss, Home of Professional Open Source Copyright 2008, Red Hat Middleware
LLC, and others contributors as indicated by the @authors tag. All rights
reserved. See the copyright.txt in the distribution for a full listing of
individual contributors. This copyrighted material is made available to anyone
wishing to use, modify, copy, or redistribute it subject to the terms and
conditions of the GNU Lesser General Public License, v. 2.1. This program
is distributed in the hope that it will be useful, but WITHOUT A WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
PURPOSE. See the GNU Lesser General Public License for more details. You
should have received a copy of the GNU Lesser General Public License, v.2.1
along with this distribution; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
-->

# Narayana standalone JMS recovery quickstart with Quarkus

## Overview

A transaction manager (TM) must be able to recover from failures during the commit phase of a transaction.
This example demonstrate that the TM is able to recover failed transactions.

## Usage

Build it:

```bash
mvn clean package
```

Run it via the pom:

```bash
mvn verify
```

or run it using the shell script:

```bash
./run.sh
```

or run each step manually:

```bash
 mvn clean package
 java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar &
 curl -w "\n" http://localhost:8080/send # send two messages in a JTA transaction
 curl -w "\n" http://localhost:8080/receive # receive two messages in a JTA transaction
 curl -w "\n" http://localhost:8080/fail # halt the VM after prepare but before the commit phase
 java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar & # the last command will have halted the VM
 curl -w "\n" http://localhost:8080/recover # recover the messages
```

## Expected output

```
java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar &                                                                                                                                                   
sleep 1                                                                                                                                                                                                            
curl -w "\n" http://localhost:8080/send # send two messages in a JTA transaction                                                                                                                                   
curl -w "\n" http://localhost:8080/receive # receive two messages in a JTA transaction                                                                                                                             
curl -w "\n" http://localhost:8080/fail # halt the VM after prepare but before the commit phase                                                                                                                    
java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar & # the last command will have halted the VM                                                                                                        
sleep 1                                                                                                                                                                                                            
curl -w "\n" http://localhost:8080/recover # recover the messages                                                                                                                                                  
[mmusgrov@dev1 jms] (jms) $ java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar &                                                                                                                       
[1] 6297                                                                                                                                                                                                           
[mmusgrov@dev1 jms] (jms) $ 2020-02-28 12:26:35,663 INFO  [RecoveryResource] (main) The application is starting...                                                                                                 
2020-02-28 12:26:35,701 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) live server is starting with configuration HornetQ Configuration (clustered=false,backup=false,sharedStore=true,journalDirectory=targe
t/data/hornetq,bindingsDirectory=target/data/hornetq/bindings,largeMessagesDirectory=target/data/hornetq/largemessages,pagingDirectory=data/paging)                                                                
2020-02-28 12:26:35,702 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) Waiting to obtain live lock                                                                                                           
2020-02-28 12:26:35,726 INFO  [org.hor.cor.per.imp.jou.JournalStorageManager] (main) Using NIO Journal                                                                                                             
2020-02-28 12:26:35,735 WARNING [org.hor.cor.ser.imp.HornetQServerImpl] (main) Security risk! It has been detected that the cluster admin user and password have not been changed from the installation default. Pl
ease see the HornetQ user guide, cluster chapter, for instructions on how to do this.                                                                                                                              
2020-02-28 12:26:35,780 INFO  [org.hor.cor.ser.imp.FileLockNodeManager] (main) Waiting to obtain live lock                                                                                                         
2020-02-28 12:26:35,780 INFO  [org.hor.cor.ser.imp.FileLockNodeManager] (main) Live Server Obtained live lock                                                                                                      
2020-02-28 12:26:35,974 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) trying to deploy queue jms.queue.queue1                                                                                               
2020-02-28 12:26:36,009 INFO  [org.hor.cor.rem.imp.net.NettyAcceptor] (main) Started Netty Acceptor version 3.2.3.Final-r${buildNumber} localhost:5445 for CORE protocol                                           
2020-02-28 12:26:36,010 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) Server is now live                                                                                                                    
2020-02-28 12:26:36,011 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) HornetQ Server version 2.2.2.Final (super-hornetq-fighter, 122) [78d758c1-5a25-11ea-a848-d4258bb66a1b] started                        
Embedded JMS Server is running                                                                                                                                                                                     
2020-02-28 12:26:36,019 INFO  [io.quarkus] (main) Quarkus 0.22.0 started in 1.117s. Listening on: http://[::]:8080                                                                                                 
2020-02-28 12:26:36,021 INFO  [io.quarkus] (main) Installed features: [cdi, narayana-jta, resteasy]                                                                                                                
                                                                                                                                                                                                                   
[mmusgrov@dev1 jms] (jms) $ curl -w "\n" http://localhost:8080/send # send two messages in a JTA transaction                                                                                                       
DummyXAResource commit() called, fault: NONE xid: < formatId=131077, gtrid_length=35, bqual_length=36, tx_uid=0:ffffac11824d:ac55:5e5906fb:2, node_name=quarkus, branch_uid=0:ffffac11824d:ac55:5e5906fb:6, subordinatenodename=null, eis_name=0 >
DummyXAResource commit() called, fault: NONE xid: < formatId=131077, gtrid_length=35, bqual_length=36, tx_uid=0:ffffac11824d:ac55:5e5906fb:9, node_name=quarkus, branch_uid=0:ffffac11824d:ac55:5e5906fb:a, subordinatenodename=null, eis_name=0 >
sent
[mmusgrov@dev1 jms] (jms) $ curl -w "\n" http://localhost:8080/receive # receive two messages in a JTA transaction
Message received: hello
Message received: world
DummyXAResource commit() called, fault: NONE xid: < formatId=131077, gtrid_length=35, bqual_length=36, tx_uid=0:ffffac11824d:ac55:5e5906fb:10, node_name=quarkus, branch_uid=0:ffffac11824d:ac55:5e5906fb:14, subor
dinatenodename=null, eis_name=0 >
Message Count after receiving 2
received
[mmusgrov@dev1 jms] (jms) $ curl -w "\n" http://localhost:8080/fail # halt the VM after prepare but before the commit phase
DummyXAResource commit() called, fault: NONE xid: < formatId=131077, gtrid_length=35, bqual_length=36, tx_uid=0:ffffac11824d:ac55:5e5906fb:17, node_name=quarkus, branch_uid=0:ffffac11824d:ac55:5e5906fb:1b, subor
dinatenodename=null, eis_name=0 >
DummyXAResource commit() called, fault: HALT xid: < formatId=131077, gtrid_length=35, bqual_length=36, tx_uid=0:ffffac11824d:ac55:5e5906fb:1e, node_name=quarkus, branch_uid=0:ffffac11824d:ac55:5e5906fb:1f, subor
dinatenodename=null, eis_name=0 >

curl: (52) Empty reply from server
[1]+  Exit 1                  java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar
[mmusgrov@dev1 jms] (jms) $ java -jar target/jms-recovery-5.10.5.Final-SNAPSHOT-runner.jar & # the last command will have halted the VM
[1] 6911
[mmusgrov@dev1 jms] (jms) $ 2020-02-28 12:27:02,430 INFO  [RecoveryResource] (main) The application is starting...
2020-02-28 12:27:02,471 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) live server is starting with configuration HornetQ Configuration (clustered=false,backup=false,sharedStore=true,journalDirectory=targe
t/data/hornetq,bindingsDirectory=target/data/hornetq/bindings,largeMessagesDirectory=target/data/hornetq/largemessages,pagingDirectory=data/paging)
2020-02-28 12:27:02,471 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) Waiting to obtain live lock
2020-02-28 12:27:02,500 INFO  [org.hor.cor.per.imp.jou.JournalStorageManager] (main) Using NIO Journal
2020-02-28 12:27:02,509 WARNING [org.hor.cor.ser.imp.HornetQServerImpl] (main) Security risk! It has been detected that the cluster admin user and password have not been changed from the installation default. Pl
ease see the HornetQ user guide, cluster chapter, for instructions on how to do this.
2020-02-28 12:27:02,553 INFO  [org.hor.cor.ser.imp.FileLockNodeManager] (main) Waiting to obtain live lock
2020-02-28 12:27:02,553 INFO  [org.hor.cor.ser.imp.FileLockNodeManager] (main) Live Server Obtained live lock
2020-02-28 12:27:02,768 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) trying to deploy queue jms.queue.queue1
2020-02-28 12:27:02,802 INFO  [org.hor.cor.rem.imp.net.NettyAcceptor] (main) Started Netty Acceptor version 3.2.3.Final-r${buildNumber} localhost:5445 for CORE protocol
2020-02-28 12:27:02,803 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) Server is now live
2020-02-28 12:27:02,804 INFO  [org.hor.cor.ser.imp.HornetQServerImpl] (main) HornetQ Server version 2.2.2.Final (super-hornetq-fighter, 122) [78d758c1-5a25-11ea-a848-d4258bb66a1b] started
Embedded JMS Server is running
2020-02-28 12:27:02,812 INFO  [io.quarkus] (main) Quarkus 0.22.0 started in 1.136s. Listening on: http://[::]:8080
2020-02-28 12:27:02,814 INFO  [io.quarkus] (main) Installed features: [cdi, narayana-jta, resteasy]

[mmusgrov@dev1 jms] (jms) $ curl -w "\n" http://localhost:8080/recover # recover the messages                                                                                                                      
                            
Message received: hello
Message received: world
DummyXAResource commit() called, fault: NONE xid: < formatId=131077, gtrid_length=35, bqual_length=36, tx_uid=0:ffffac11824d:b22f:5e590716:2, node_name=quarkus, branch_uid=0:ffffac11824d:b22f:5e590716:6, subordi
natenodename=null, eis_name=0 >
Message Count after running recovery: 2
recovered
[mmusgrov@dev1 jms] (jms) $ 
```

##  What just happened

The JMS recovery example starts a transaction and enlists two resources. One of the resources is a JMS XA session.
Two messages are generated within a transactional XA session. Before sending the messages two
resources are enlisted into the transaction, an XA resource corresponding to the JMS session used to send the
messages and a dummy XA resource. On the first run in the recovery example the dummy resource is configured
to halt the VM during the second, commit, phase of the transaction completion protocol.
This will cause the JMS messages to require recovery.

During the second run the example manually requests a "recovery scan" (normally the recovery subsystem runs
automatically). This scan will cause the the two messages to be committed hence making them available for consumption.
The 2 messages are then consumed.
