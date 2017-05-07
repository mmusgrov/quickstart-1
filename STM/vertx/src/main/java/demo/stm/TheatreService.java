package demo.stm;

import org.jboss.stm.annotations.Nested;
import org.jboss.stm.annotations.Optimistic;
import org.jboss.stm.annotations.Transactional;

@Transactional
@Nested
//@Optimistic
public interface TheatreService extends Activity {
}
