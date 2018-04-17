package lra.demo;

import org.eclipse.microprofile.lra.annotation.LRA;
import org.eclipse.microprofile.lra.client.LRAClient;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/app")
@LRA(LRA.Type.SUPPORTS)
public class Resource {

    @Inject
    LRAClient lraClient;

    @GET
    @Path("/greet")
    @Produces("text/html")
    public String greet() {
        return System.currentTimeMillis() + "";
    }

    @PUT
    @Path("/work")
    @LRA(LRA.Type.REQUIRED)
    public Response activityWithLRA(@HeaderParam(LRAClient.LRA_HTTP_RECOVERY_HEADER) String rcvId,
                                    @HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) {
        System.out.printf("lra: %s%n", lraId);
        System.out.printf("client: %s%n", lraClient == null ? null : lraClient.getClass().getCanonicalName());

//        assert lraId != null;
//        Activity activity = addWork(lraId, rcvId);

//        if (activity == null)
//            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        return Response.ok(lraId).build();
    }
}
