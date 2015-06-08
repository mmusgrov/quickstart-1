/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.ejb.remote.ar;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.internal.arjuna.thread.ThreadActionData;
import com.arjuna.ats.internal.jts.orbspecific.coordinator.ArjunaTransactionImple;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteHeuristicServerTransaction;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteHeuristicTransaction;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteServerTransaction;
import com.arjuna.ats.internal.jts.recovery.transactions.AssumedCompleteTransaction;
import com.arjuna.ats.jta.TransactionManager;
import org.jboss.as.quickstarts.ejb.remote.stateless.RemoteCalculator;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteRecordCreator.class)
public class RecordCreatorBean implements RemoteRecordCreator {
    @Override
    public void generateAssumedCompleteHeuristicTransaction() {
        generatedHeuristicHazard(new AssumedCompleteHeuristicTransaction(new Uid()));
    }
    @Override
    public void generateAssumedCompleteHeuristicServerTransaction() {
        generatedHeuristicHazard(new AssumedCompleteHeuristicServerTransaction(new Uid()));
    }
    @Override
    public void generateAssumedCompleteTransaction() {
        generatedHeuristicHazard(new AssumedCompleteTransaction(new Uid()));
    }
    @Override
    public void generateAssumedCompleteServerTransaction() {
        generatedHeuristicHazard(new AssumedCompleteServerTransaction(new Uid()));
    }

    @Override
    public void generateRecords() {
        generateAssumedCompleteHeuristicTransaction();
        generateAssumedCompleteHeuristicServerTransaction();
        generateAssumedCompleteTransaction();
        generateAssumedCompleteServerTransaction();
    }

    protected void generatedHeuristicHazard(ArjunaTransactionImple txn) {
        Transaction prevTxn = null;

        try {
            prevTxn = TransactionManager.transactionManager().suspend();
        } catch (SystemException e) {
            e.printStackTrace();
        }

        ThreadActionData.purgeActions();

		ExtendedCrashRecord recs[] = {
				new ExtendedCrashRecord(ExtendedCrashRecord.CrashLocation.NoCrash, ExtendedCrashRecord.CrashType.Normal),
                new ExtendedCrashRecord(ExtendedCrashRecord.CrashLocation.CrashInCommit, ExtendedCrashRecord.CrashType.HeuristicHazard),
				new ExtendedCrashRecord(ExtendedCrashRecord.CrashLocation.CrashInCommit, ExtendedCrashRecord.CrashType.HeuristicHazard)
		};

        txn.start();

        for (ExtendedCrashRecord rec : recs)
            txn.add(rec);

        txn.end(false);

        if (prevTxn != null)
            try {
                TransactionManager.transactionManager().resume(prevTxn);
            } catch (InvalidTransactionException e) {
                e.printStackTrace();
            } catch (SystemException e) {
                e.printStackTrace();
            }
    }
}
