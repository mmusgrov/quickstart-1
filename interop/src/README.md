Interoperability Testing (between glassfish and wildfly)
------------------------

Download and start glassfish:
----------------------------

Download glassfish (http://download.java.net/glassfish/4.1.1/release/glassfish-4.1.1.zip) and inlcude glassfish4/glassfish/bin in the path environment variable.

If you need to create a second domain use "asadmin create-domain --adminport 4948 domain2" and then manually
update glassfish/domains/domain2/config/domain.xml - I added 100 to each port
(jdbc-connection-pool, JMS_PROVIDER_PORT, iiop-listener, jmx-connector, debug-options, jvm-options, http-listener-1). If the server fails to start with "Cannot bind to port" then review the port values set in each server config)

Start the domain: asadmin start-domain domain1

(NB the browser console is at the url http://localhost:4848/common/index.jsf)

Build narayana with interop fixes
----------------------------

```
git clone https://github.com/jbosstm/narayana.git
cd narayana
./build.sh clean install -Prelease,community -DskipTests -Didlj-enabled=true
```

Build and start WildFly
----------------------------

```
git clone https://github.com/jbosstm/jboss-as.git
cd jboss-as
git co 5_BRANCH_JBTM-223
JAVA_OPTS="-Xms1303m -Xmx1303m -XX:MaxPermSize=512m $JAVA_OPTS" ./build.sh clean install -DskipTests -Dts.smoke=false -Dlicense.skipDownloadLicenses=true $IPV6_OPTS -Drelease=true
cd build/target/wildfly-10.1.0.Final-SNAPSHOT
```

Configure JTS and add some system properties:

```
    <!-- override the transaction propagation slot id to the standard one -->
    /system-property=com.arjuna.ats.jts.transactionServiceId:add(value=0)
    <!-- compile CORBA stubs on demand -->
    /system-property=com.sun.CORBA.ORBUseDynamicStub:add(value=true)
    <!-- To workaround GLASSFISH-21532 define -->
    /system-property=GLASSFISH-21532-WORKAROUND:add(value=true)
    /system-property=jboss.socket.binding.port-offset:add(value=0)

    /subsystem=iiop-openjdk/:write-attribute(name=transactions,value=full)
    /subsystem=transactions/:write-attribute(name=jts,value=true) 
    /subsystem=transactions/:write-attribute(name=node-identifier,value=1)
```

start the server (full config):

```
./bin/standalone.sh -c standalone-full.xml
```

Deploy test ejbs to glassfish and wildfly
----------------------------

```
cd <narayana branch>/interop
mvn install # build war and ejb
```

```
export JBOSS_HOME=<wildfly branch>/build/target/wildfly-10.1.0.Final-SNAPSHOT
```

```
./d.sh -a gf1 -f interop/target/ejbtest.war # deploy to glassfish
./d.sh -a gf1 -f recovery/target/dummy-resource-recovery.jar
./d.sh -a wf1 -f interop/target/ejbtest.war # deploy to wildfly
./d.sh -a wf1 -f recovery/target/dummy-resource-recovery.jar
```

dummy-resource-recovery registers a recovery resource for use by the recovery system and
ejbtest.war contains a JAX-RS service which invokes a local ejb which in turn invokes a remote ejb.

Make an ejb call from glassfish to wildfly 
----------------------------

```
./d.sh -a gf1 -t gfwf # curl http://localhost:7080/ejbtest/rs/remote/3528/wf/x 
```

This command issues a REST request which is handled by class service.Controller running on glassfish
(port 7080) which in turn invokes a local ejb (service.ControllerBean) which looks up and invokes a remote ejb
using the CORBA name service running on port 3528 host by wilfly.

Alternatively make an an ejb call from glassfish to wildfly and halt wildfly during commit

```
./d.sh -a gf1 -t gfwf -t haltafter # curl http://localhost:7080/ejbtest/rs/remote/3528/wf/haltafter
```

The dummy resource will print the following just before it halts the server during commit:

```
    21:11:33,935 INFO  [stdout] (p: default-threadpool; w: Idle) DummyXAResource: xa commit ... halting
```

Check that wildfly is not running using the command ./bin/jboss-cli.sh --connect controller=localhost:9990
To observe recovery taking place restart wildfly. When recovery has finished you should see a log message
showing the resource committing:

```
    21:12:10,950 INFO  [stdout] (Periodic Recovery) DummyXAResource: commit
```

The resource also logs what it is doing by writing to a file in the server run directory called xar.log 
and it saves pending xids in a file called xids.txt. The xid entry in xids.txt should be removed after
recovery is complete.

Make an ejb call from wildfly to glassfish
----------------------------

```
./d.sh -a wf1 -t wfgf # curl http://localhost:8080/ejbtest/rs/remote/7001/gf/x
```

or make an an ejb call from wildfly to glassfish and halt glassfish during commit

```
./d.sh -a wf1 -t wfgf -t haltafter # curl http://localhost:8080/ejbtest/rs/remote/7001/gf/haltafter
```

check that glassfish domain is not running using the command asadmin list-domains

Restart glassfish and wait (quite) a while for recovery to happen. If you tail -f either of

```
  <glassfish install dir>/glassfish4/glassfish/domains/domain2/config/xar.log
  <glassfish install dir>/glassfish4/glassfish/domains/domain2/config/xids.txt
```

then you will know when recovery is complete. You will need /system-property=GLASSFISH-21532-WORKAROUND:add(value=true) if GLASSFISH-21532 hasn't been fixed. Also eventually the interposed ServerTransaction on wildfly
will be moved to the AssumedCompleteServerTransaction part of the logs.

Note also that glassfish will recover with a heuristic. This happens because no recovery information is
associated with the recovery resource we registered in org.jboss.narayana.RecoveryBean so it should be
resolvable (but it's a low priority task).

