package org.jboss.narayana.jta.quickstarts;

import org.jboss.narayana.jta.quickstarts.recovery.JmsRecovery;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("")
public class RecoveryResource {
    private static final Logger LOGGER = Logger.getLogger("RecoveryResource");

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("fail")
    public String fail() throws Exception {
        JmsRecovery.msgOp(new String[]{"-f"});
        return "should have failed";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("recover")
    public String recover() throws Exception {
        JmsRecovery.msgOp(new String[]{"-r"});
        return "recovered";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("send")
    public String send() throws Exception {
        JmsRecovery.msgOp(new String[]{"-s"});
        return "sent";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("receive")
    public String receive() throws Exception {
        JmsRecovery.msgOp(new String[]{"-a"});
        return "received";
    }

    void onStart(@Observes StartupEvent ev) throws Exception {
        LOGGER.info("The application is starting...");
        JmsRecovery.startServices();
    }

    void onStop(@Observes ShutdownEvent ev) throws Exception {
        LOGGER.info("The application is stopping...");
        JmsRecovery.stopServices();
    }
}
