package demo.stm;

import org.jboss.stm.annotations.Nested;
import org.jboss.stm.annotations.Transactional;

@Transactional
@Nested
public interface TheatreService extends Activity {
}
