package demo.stm;

import com.arjuna.ats.arjuna.AtomicAction;
import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.State;
import org.jboss.stm.annotations.WriteLock;

public class TaxiServiceImpl implements TaxiService {
    @State
    private int noOfCompletedActivities = 0;

    public TaxiServiceImpl() {
        // workaround for JBTM-1732
        AtomicAction A = new AtomicAction();

        A.begin();
        init();
        A.commit();
    }

    @Override
    @WriteLock
    public void failingActivity()  throws Exception {
//            activity(); // TODO state changes made inside a TopLevelAction are not rolled back on exception
        throw new Exception();
    }

    @Override
    @WriteLock
    public void init() {
    }

    @Override
    @WriteLock
    public void activity() {
        noOfCompletedActivities += 1;
    }

    @Override
    @ReadLock
    public int getValue() {
        return noOfCompletedActivities;
    }
}
