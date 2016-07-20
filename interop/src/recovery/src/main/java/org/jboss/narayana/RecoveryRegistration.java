package org.jboss.narayana;
 
import javax.ejb.Remote;

//@Remote
public interface RecoveryRegistration {
    void register();
    void unregister();
}
