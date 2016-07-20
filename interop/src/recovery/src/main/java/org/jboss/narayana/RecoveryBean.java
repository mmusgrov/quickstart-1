package org.jboss.narayana;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.jta.recovery.SerializableXAResourceDeserializer;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import org.jboss.narayana.DummyXAResource;

@Singleton
@Startup
public class RecoveryBean implements RecoveryRegistration {
    static private final String EE_TM = "java:/TransactionManager";
    static private final String GF_TM = "java:appserver/TransactionManager";
    static private final String WL_TM = "weblogic.transaction.TransactionManager";
    private static final String XAR_NAME = "Dummy";

    private boolean registered;
    private boolean isWF;
    private SerializableXAResourceDeserializer deserializer;
    private XAResourceRecoveryHelper xarHelper;

    @PostConstruct
    private void init() {
        System.out.printf("registering RecoveryBean%n");
        isWF = System.getProperty("jboss.node.name") != null;

        if (isWF) {
            deserializer = new SerializableXAResourceDeserializer() {
                @Override
                public boolean canDeserialze(String className) {
                    return true;
                }

                @Override
                public XAResource deserialze(ObjectInputStream ois) throws IOException, ClassNotFoundException {
                    return new DummyXAResource();
                }
            };

            xarHelper = new XAResourceRecoveryHelper() {
                public boolean initialise(String p) throws Exception {
                    return true;
                }

                public XAResource[] getXAResources() throws Exception {
                    return new XAResource[]{new DummyXAResource()};
                }
            };
        }
//        register();
    }

    @PreDestroy
    private void fini() {
        System.out.printf("unregistering RecoveryBean%n");
        unregister();
    }

    @Schedule(second="*/59", minute="*", hour="*", persistent=false)
    public void register() {
        try {
            if (!registered) {
                registerRecoveryResources(isWF);
                registered = true;
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unregister() {
        System.out.printf("register registered=%b%n", registered);
        if (!registered)
            return;

        if (isWF) {
            RecoveryManager manager = RecoveryManager.manager(RecoveryManager.INDIRECT_MANAGEMENT);
            Vector recoveryModules = manager.getModules();

            if (recoveryModules != null) {
                Enumeration modules = recoveryModules.elements();

                while (modules.hasMoreElements()) {
                    RecoveryModule m = (RecoveryModule) modules.nextElement();

                    if (m instanceof com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) m;
                        // no deserializer remove op
                        xarm.removeXAResourceRecoveryHelper(xarHelper);
                    } else if (m instanceof com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) m;
                        // no deserializer remove op
                        xarm.removeXAResourceRecoveryHelper(xarHelper);
                    }
                }
            }
        }

        registered = false;
    }

    private void registerRecoveryResources(boolean isWF) throws NamingException {
        if (isWF) {
            RecoveryManager manager = RecoveryManager.manager(RecoveryManager.INDIRECT_MANAGEMENT);
            Vector recoveryModules = manager.getModules();

            if (recoveryModules != null) {
                Enumeration modules = recoveryModules.elements();

                while (modules.hasMoreElements()) {
                    RecoveryModule m = (RecoveryModule) modules.nextElement();

                    if (m instanceof com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.jts.XARecoveryModule) m;

                        xarm.addSerializableXAResourceDeserializer(deserializer);
                        xarm.addXAResourceRecoveryHelper(xarHelper);
                    } else if (m instanceof com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) {
                        com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule xarm = (com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule) m;

                        xarm.addSerializableXAResourceDeserializer(deserializer);
                        xarm.addXAResourceRecoveryHelper(xarHelper);
                    }
                }
            }
        } else {
            registerGFRecoveryResources3();
//            (TransactionManager) new InitialContext().lookup(WL_TM).registerStaticResource(XAR_NAME, new DummyXAResource());
        }
    }

    private void registerGFRecoveryResources() {

        try {
            Class clazz = Class.forName("com.sun.jts.CosTransactions.RecoveryManager");
            Method method = clazz.getMethod("recoverXAResources", Enumeration.class);
            DummyXAResource[] xars = {new DummyXAResource()};
            Enumeration e = Collections.enumeration(Arrays.asList(xars));

            method.invoke(null, e);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            System.out.printf("Error registering recovery resources: %s%n", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void registerGFRecoveryResources2() {
        try {
            TransactionManager tm = (TransactionManager) new InitialContext().lookup(GF_TM);
            //ResourceRecoveryManagerImpl.registerRecoveryResourceHandler(xaResource);

            final String recoverMethod = "recover"; // "registerRecoveryResourceHandler"
            // com.sun.enterprise.transaction.api.JavaEETransactionManager
            java.lang.reflect.Method method = tm.getClass().getMethod(recoverMethod, XAResource.class);
            DummyXAResource[] xars = {new DummyXAResource()};
            method.invoke(tm, xars[0]);
        } catch (Exception e) {
            System.out.printf("Error registering recovery resources: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * The following technique of delaying/retrying registring recovery resources is to work
     * around a race in how the server name is set:
     * - a glassfish xid contains the server name which initally takes the default value of
     *   xxxP100 but after the orb interceptors, TransactionIIOPInterceptorFactory, have been
     *   invoked it morphs into xxxP3700 (where 3700 is the iiop-listener port). 
     * - this becomes an issue because the resulting registration xid and the recovery xid will
     *   often be different
     */
    private void registerGFRecoveryResources3() {
        try {
            TransactionManager tm = (TransactionManager) new InitialContext().lookup(GF_TM);
            //ResourceRecoveryManagerImpl.registerRecoveryResourceHandler(xaResource);

            // access private field JavaEETransactionManager transactionManager
            Field f = tm.getClass().getDeclaredField("transactionManager");
            f.setAccessible(true);
            Object actualTM = f.get(tm); // JavaEETransactionManager

            final String recoverMethod = "recover"; // "registerRecoveryResourceHandler"
            // com.sun.enterprise.transaction.api.JavaEETransactionManager
            java.lang.reflect.Method method = actualTM.getClass().getMethod(
                recoverMethod, new Class[] { XAResource[].class} );
            XAResource[] xars = new XAResource[1];
            xars[0] = new DummyXAResource();
            method.invoke(actualTM, new Object[] { xars });
        } catch (Exception e) {
            System.out.printf("Error registering recovery resources: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }
}
