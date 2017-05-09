package demo.domain;

import org.jboss.stm.annotations.Nested;
import org.jboss.stm.annotations.Transactional;

@Transactional
@Nested
//@Optimistic
public interface TheatreService extends Booking {
}
