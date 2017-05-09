package demo.domain;

import org.jboss.stm.annotations.NestedTopLevel;
import org.jboss.stm.annotations.Transactional;

@Transactional
@NestedTopLevel
//@Optimistic
public interface TaxiService extends Booking {
    void failingActivity() throws Exception;
}
