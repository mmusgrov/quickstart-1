package demo.domain;

import org.jboss.stm.annotations.Nested;
import org.jboss.stm.annotations.NestedTopLevel;
import org.jboss.stm.annotations.Transactional;

@Transactional
@Nested
public interface TaxiService extends Booking {
    void init();

    void book();

    void failToBook() throws Exception;

}
