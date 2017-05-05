package demo.stm;

import com.arjuna.ats.arjuna.AtomicAction;
import org.jboss.stm.Container;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NonVolatileActivityTest extends ActivityBase {
    @Test
    public void testAbortNonVolatileMandatoryActivity() throws Exception {
        testNonVolatileActivityImpl(true);
    }

    @Test
    public void testCommitNonVolatileMandatoryActivity() throws Exception {
        testNonVolatileActivityImpl(false);
    }

    private void testNonVolatileActivityImpl(boolean abortApplicationTransaction)  {
        Container<TheatreService> container = new Container<>(Container.TYPE.PERSISTENT, Container.MODEL.SHARED);
        // get a handle to an STM object for performing mandatory actions
        TheatreService mandatory = container.create(new TheatreServiceImpl()); // TheatreService is Nested
        // get a handle to an STM object for performing optional actions
        TaxiService optional = new Container<TaxiService>().create(new TaxiServiceImpl()); // TaxiService is NestedTopLevel

        // workaround for JBTM-1732
        initializeSTMObject(mandatory);

        AtomicAction A = new AtomicAction();

        A.begin();
        mandatory.activity(); // done as a sub transaction of A since mandatory is annotated wiht @Nested

        // A is still running: check that the changes made in activity are visible to all threads
        assertEquals(1, mandatory.getValue());

        // make another update whilst A is running but this time do it inside a NestedTopLevel action:
        try {
            optional.failingActivity(); // A is suspended since TaxiService is NestedTopLevel and failingActivity commits independently of A
            fail("optional failingActivity is implemented to throw an exception");
        } catch (Exception e) {
            // state changes made in failingActivity should roll back because of the exception TODO this is not true
            // A will have been resumed
            assertEquals(0, optional.getValue());
            // try an alternate strategy for activity2
            optional.activity(); // A will suspend since TaxiService is NestedTopLevel
            // activity will have commited independently of the mandatory activity and A will have been resumed
            assertEquals(1, optional.getValue());
        }

        if (abortApplicationTransaction) {
            A.abort();
            // work done by TheatreService#activity1 was inside a Nested transaction - ie its outcome is dependent
            // on the outcome of A. Hence the update should have been rolled back:
            assertEquals(0, mandatory.getValue());

            updateInDifferentThread(container, mandatory);
        } else {
            A.commit();
            // committing A will automatically commit any nested transactions ie the work done inside TheatreService#activity1:
            assertEquals(1, mandatory.getValue());

            updateInDifferentThread(container, mandatory);
        }
    }
}
