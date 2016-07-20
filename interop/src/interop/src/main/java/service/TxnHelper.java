package service;

import org.jboss.narayana.ASFailureMode;
import org.jboss.narayana.ASFailureSpec;
import org.jboss.narayana.ASFailureType;
import org.jboss.narayana.DummyXAResource;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

public class TxnHelper {
    static private final String WL_TM = "weblogic.transaction.TransactionManager";
    static private final String EE_TM = "java:/TransactionManager";
    static private final String GF_TM = "java:appserver/TransactionManager";

    static void addResources(boolean isWF) throws NamingException, SystemException, RollbackException {
        addResources(isWF, null);
    }

    static void addResources(boolean isWF, String failureType) throws NamingException, SystemException, RollbackException {
//        if (failureType != null && failureType.contains("recovery"))
//            registerRecoveryResources(isWF);

        TransactionManager tm = getTransactionManager(isWF);

        assert tm.getTransaction() != null;

        tm.getTransaction().enlistResource(getResource(tm, failureType));
    }

/*    static void registerRecoveryResources(boolean isWF) throws NamingException {
        if (isWF)
            DummyXAResourceRecoveryHelper.registerRecoveryResources();

        TransactionManager tm = getTransactionManager(isWF);

        try {
            if (!isWF) {
                // com.sun.enterprise.transaction.api.JavaEETransactionManager
                java.lang.reflect.Method method = tm.getClass().getMethod("registerRecoveryResourceHandler", XAResource.class);
                method.invoke(tm, getResource(tm, null));
            }
        } catch (Exception e) {
            System.out.printf("Error registering recovery resources: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }*/

    static TransactionManager getTransactionManager(boolean isWF) throws NamingException {
        if (isWF)
            return (TransactionManager) new InitialContext().lookup(EE_TM);
        else
            return (TransactionManager) new InitialContext().lookup(GF_TM);
    }

    static Transaction getTransaction(boolean isWF) throws NamingException, SystemException {
        return getTransactionManager(isWF).getTransaction();
    }

    static private DummyXAResource getResource(TransactionManager tm, String failureType) {
        if (failureType == null || tm == null)
            return new DummyXAResource();

        System.out.printf("enlisting dummy resource with fault type %s%n", failureType);

        if (failureType.contains("halt"))
            return new DummyXAResource(new ASFailureSpec("fault", ASFailureMode.HALT, "", ASFailureType.XARES_COMMIT));
        else
            return new DummyXAResource(new ASFailureSpec("", ASFailureMode.NONE, "", ASFailureType.NONE));
    }
}
