package demo.domain;

import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.State;
import org.jboss.stm.annotations.WriteLock;

public class TaxiServiceImpl implements TaxiService {
    @State
    private int noOfCompletedActivities = 0;
    private String name;

    public TaxiServiceImpl() {
        this("");
    }

    public TaxiServiceImpl(String name) {
        this.name = name;
    }

    @Override
    @WriteLock
    public void failingActivity()  throws Exception {
//            book(); // TODO state changes made inside a TopLevelAction are not rolled back on exception
        throw new Exception();
    }

    @Override
    @WriteLock
    public void init() {
    }

    @Override
    @WriteLock
    public void book() {
        noOfCompletedActivities += 1;
    }

    @Override
    @ReadLock
    public int getBookings() {
        return noOfCompletedActivities;
    }
}
