package demo.stm;

import org.jboss.stm.annotations.NestedTopLevel;
import org.jboss.stm.annotations.Optimistic;
import org.jboss.stm.annotations.Transactional;

@Transactional
@NestedTopLevel
//@Optimistic
public interface TaxiService extends Activity {
    void failingActivity() throws Exception;
}
