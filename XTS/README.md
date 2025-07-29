XTS Quickstarts
===============

These quickstarts demonstrate the various ways of distributing a transaction over Web Services. If you need to distribute a JTA
transaction over Web Services, you are most likely to be interested in looking at the "WS-AT to JTA" examples. These examples
demonstrate just how simple it is to achieve this functionality.

If you are interested in using WS-Business Activity for distributing a compensating transaction, you should look at our examples in
the "compensating-transactions" directory in the root of the Narayana quickstarts. Here we provide a user-friendly annotations-based
API for using compensating transactions.

Finally, if you are interested in a lower-level API for dealing with all aspects of the WS-AT and WS-BA protocol, you may find our
"Raw XTS API demo" to be useful.


WS-AT to JTA (Multi Hop)
------------------------
This example demonstrates a JTA client that invokes a remote EJB over Web services. The JTA transaction is distributed to the remote EJB using WS-AtomicTransaction.
The service also acts as a client to a second service. Again using WS-AT to distribute the transaction over Web services.


WS-AT to JTA (Multi Service)
----------------------------
This example is similar to "WS-AT to JTA (Multi Hop)". However, the client invokes two services instead of one.


Raw XTS API Demo
----------------
An example that demonstrates the usage of the low-level raw API. This example is good if you need to develop your own WS-AT
or WS-BA participants or if you need to use a particular feature not yet available in the higher-level APIs.

Using an XA datasource
----------------------
To test with an XA datasource I ran `mvn clean test -Parq -f wsat-jta-multi_hop/pom.xml` after making the following changes to standalone-xts.xml:

[mmusgrov: XTS] ((HEAD detached at 6.0.4.Final)) $ diff $JBOSS_HOME/docs/examples/configs/standalone-xts.xml $JBOSS_HOME/standalone/configuration/standalone-xts.xml
87a88,99
>             <logger category="org.jboss.jbossts.txbridge">
>                 <level name="TRACE"/>
>             </logger>
>             <logger category="jboss.jdbc.spy">
>                 <level name="TRACE"/>
>             </logger>
>             <logger category="org.jboss.jca.core.connectionmanager">
>                 <level name="TRACE"/>
>             </logger>
>             <logger category="org.jboss.jca">
>                 <level name="TRACE"/>
>             </logger>
128a141,172
>                 <xa-datasource jndi-name="java:jboss/datasources/H2XADS1" pool-name="java:jboss/datasources/H2XADS1" enabled="true" spy="true" use-ccm="false">
>                     <xa-datasource-property name="URL">
>                         jdbc:h2:file:~/xaqs1;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=${wildfly.h2.compatibility.mode:REGULAR}
>                     </xa-datasource-property>
>                     <driver>h2</driver>
>                     <xa-pool>
>                         <is-same-rm-override>false</is-same-rm-override>
>                         <interleaving>false</interleaving>
>                         <pad-xid>false</pad-xid>
>                         <wrap-xa-resource>true</wrap-xa-resource>
>                     </xa-pool>
>                     <security>
>                         <user-name>sa</user-name>
>                         <password>sa</password>
>                     </security>
>                     <recovery>
>                         <recover-credential>
>                             <user-name>sa</user-name>
>                             <password>sa</password>
>                         </recover-credential>
>                     </recovery>
>                     <validation>
>                         <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.novendor.JDBC4ValidConnectionChecker"/>
>                         <validate-on-match>true</validate-on-match>
>                         <background-validation>false</background-validation>
>                         <exception-sorter class-name="database_specific_ExceptionSorter_here"/>
>                     </validation>
>                     <statement>
>                         <prepared-statement-cache-size>0</prepared-statement-cache-size>
>                         <share-prepared-statements>false</share-prepared-statements>
>                     </statement>
>                 </xa-datasource>

