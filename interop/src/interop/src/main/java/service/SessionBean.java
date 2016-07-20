package service;

import service.remote.ISessionHome;

import javax.annotation.PostConstruct;
import javax.ejb.RemoteHome;

import javax.ejb.Stateless;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.NamingException;

import java.util.concurrent.atomic.AtomicInteger;

@Stateless
//@Local(service.local.ISession.class)
//@Remote(service.remote.ISession.class)
@RemoteHome(ISessionHome.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SessionBean {
	static AtomicInteger counter = new AtomicInteger(0);
	boolean isWF;

	@PostConstruct
	public void init() {
		isWF = System.getProperty("jboss.node.name") != null;

		counter.set(isWF ? 8000 : 7000);

/*		try {
			TxnHelper.registerRecoveryResources(isWF);
		} catch (NamingException e) {
			System.out.printf("Recovery resource registration failure: %s%n", e.getMessage());
		}*/
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public String getNext() {
		return getNext(null);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public String getNext(String failureType) {
		try {
			TxnHelper.addResources(isWF, failureType);

			System.out.printf("%s returning next counter%n", this);
			return String.valueOf(counter.getAndIncrement());
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
