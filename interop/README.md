
# Demonstrates transaction propagation between glassfish and wildfly

export QS_DIR=$PWD

1. Patch and build narayana

``` shell
git clone https://github.com/jbosstm/narayana.git $QS_DIR/tmp/narayana
cd $QS_DIR/tmp/narayana
git checkout 939ba40 # TODO change this after the next release
git apply $QS_DIR/JBTM-223.narayana.diff
./build.sh clean install -DskipTests
cd $QS_DIR
```

2. Patch and build glassfish

``` shell
svn checkout https://svn.java.net/svn/glassfish~svn/trunk/main $QS_DIR/tmp/glassfish
cd $QS_DIR/tmp/glassfish
patch -p0 -i $QS_DIR/GLASSFISH-21532.diff 
mvn install -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -DskipTests
cd $QS_DIR
```

The patch fixes GLASSFISH-21532

3. Patch and build wildfly

``` shell
git clone https://github.com/wildfly/wildfly $QS_DIR/tmp/wildfly
cd $QS_DIR/tmp/wildfly
git checkout 10.0.0.Final
git apply $QS_DIR/JBTM-223.wildfly.diff # make sure the narayana version corresponds to step 1
./build.sh clean install -DskipTests
export JBOSS_HOME=$QS_DIR/tmp/wildfly/build/target/wildfly-10.0.0.Final
```

The patch fixes removes the EJB interceptor that blocks the import of foreign transactions.

4. Start application servers

### Start a wildfly server

``` shell
cd $QS_DIR/tmp/wildfly/build/target/wildfly-10.0.0.Final
./bin/standalone.sh -c standalone-full.xml -Djboss.tx.node.id=1 &
```

put it into JTS mode (to interoperate with other application servers)
``` shell
./bin/jboss-cli.sh --connect --file=$QS_DIR/configure-jts-transactions.cli
```

### Start a glassfish server

``` shell
cd $QS_DIR/tmp/glassfish # start a glassfish domain
export PATH=$PWD/appserver/distributions/glassfish/target/stage/glassfish4/bin:$PATH #include the glassfish asadmin command in the path:

asadmin start-domain domain1
asadmin set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.port=7080
```

The corresponding web consoles are:

glassfish admin console: http://localhost:4848/

glassfish web console: http://localhost:7080/
wildfly web console: http://localhost:8080/

## Deploy test EJBs to glassfish for testing transaction propagation

First build the EJBs:

``` shell
cd $QS_DIR/src
mvn clean install
```

This command builds a jar and a war. The jar, dummy-resource-recovery, registers a recovery resource
for use by the recovery system and the war, ejbtest.war, contains a JAX-RS service which invokes a
local ejb which in turn invokes a remote ejb. Deploy these to glassfish:

``` shell
./d.sh -a gf1 -f interop/target/ejbtest.war
```

This command should produce the output:

> Application deployed with name ejbtest.
> Command deploy executed successfully.

and similarly deploy a recovery ejb:

``` shell
./d.sh -a gf1 -f recovery/target/dummy-resource-recovery.jar
```

You can also check for deployed applications by looking for the names ejbtest and dummy-resource-recovery
in the output of the command:

> asadmin list-applications --type ejb

## Deploy test EJBs to wildfly for testing transaction propagation

Similarly deploy to wildfly. Make sure you set JBOSS_HOME (see the earlier section on building wildfly):

``` shell
./d.sh -a wf1 -f interop/target/ejbtest.war
./d.sh -a wf1 -f recovery/target/dummy-resource-recovery.jar 
```
Check the wildfly console for any errors. You can also check that the EJBs deployed successfuly using
the wildfly CLI:

``` shell
$JBOSS_HOME/bin/jboss-cli.sh --connect "ls deployment" 
```
Check that the output contains dummy-resource-recovery.jar and ejbtest.war

## Make an ejb call from glassfish to wildfly 

``` shell
./d.sh -a gf1 -t gfwf
```

This command issues a REST request (curl http://localhost:7080/ejbtest/rs/remote/3528/wf/x) which is
handled by class service.Controller running on glassfish (port 7080) which in turn invokes a local ejb
(service.ControllerBean) which looks up and invokes a remote ejb using the CORBA name service running
on port 3528 host by wilfly.

Alternatively make an an ejb call from glassfish to wildfly and halt wildfly during commit

```
./d.sh -a gf1 -t gfwf -t haltafter
```

The dummy resource will print the following just before it halts the server during commit:

```
    21:11:33,935 INFO  [stdout] (p: default-threadpool; w: Idle) DummyXAResource: xa commit ... halting
```

Check that wildfly has halted using the command
``` shell
./bin/jboss-cli.sh --connect controller=localhost:9990
```
To observe recovery taking place restart wildfly. When recovery has finished you should see a log message
showing the resource committing:

```
    21:12:10,950 INFO  [stdout] (Periodic Recovery) DummyXAResource: commit
```

The resource also logs what it is doing by writing to a file in the server run directory called xar.log
and it saves pending xids in a file called xids.txt. The xid entry in xids.txt should be removed after
recovery is complete.


========== Status ===
./d.sh -t gfwf -t haltafter

gf is the coordinator and makes a call to wf.
gf commits the transaction
gf commits the gf resource
gf commits the wf resource
wf halts during resource commit.
Ends up as a heuristic hazard.
