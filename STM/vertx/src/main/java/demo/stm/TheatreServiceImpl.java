package demo.stm;

import com.arjuna.ats.arjuna.AtomicAction;
import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.State;
import org.jboss.stm.annotations.WriteLock;

public class TheatreServiceImpl implements TheatreService {
    @State
    private int noOfCompletedActivities = 0;

    public TheatreServiceImpl() {
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