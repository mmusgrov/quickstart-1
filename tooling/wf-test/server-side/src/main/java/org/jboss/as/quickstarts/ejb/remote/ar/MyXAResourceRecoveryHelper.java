package org.jboss.as.quickstarts.ejb.remote.ar;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.AbstractRecord;
import com.arjuna.ats.arjuna.coordinator.RecordType;
import com.arjuna.ats.arjuna.coordinator.abstractrecord.RecordTypeManager;
import com.arjuna.ats.arjuna.coordinator.abstractrecord.RecordTypeMap;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.arjuna.thread.ThreadActionData;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.internal.jts.orbspecific.coordinator.ArjunaTransactionImple;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteHeuristicServerTransaction;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteHeuristicTransaction;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteServerTransaction;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteTransaction;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.util.Vector;

import com.arjuna.ats.jta.TransactionManager;

@Singleton // similar to ApplicationScoped
@Startup
public class MyXAResourceRecoveryHelper implements XAResourceRecoveryHelper {
    private XARecoveryModule xarm;

    @Override
    public boolean initialise(String p) throws Exception {
        return false;
    }

    @Override
    public XAResource[] getXAResources() throws Exception {
        return new XAResource[0];
    }

    @PostConstruct
    public void postConstruct() {
        System.out.printf("MyXAResourceRecoveryHelper postConstruct%n");

        xarm = null;

        for (RecoveryModule rm : ((Vector<RecoveryModule>) RecoveryManager.manager().getModules())) {
            if (rm instanceof XARecoveryModule) {
                xarm = (XARecoveryModule) rm;
                break;
            }
        }

 		RecordTypeManager.manager().add(new RecordTypeMap() {
			public Class<? extends AbstractRecord> getRecordClass() {
				return ExtendedCrashRecord.class;
			}

			public int getType() {
				return RecordType.USER_DEF_FIRST0;
			}
		});
    }

    /**
     * MC lifecycle callback, used to unregister the recovery module from the transaction manager.
     */
    @PreDestroy
    public void preDestroy() {
        System.out.printf("MyXAResourceRecoveryHelper preDestroy%n");

        if (xarm != null)
            xarm.removeXAResourceRecoveryHelper(this);
    }
}
