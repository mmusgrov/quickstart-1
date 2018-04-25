package lra.demo;

import org.eclipse.microprofile.lra.annotation.LRA;
import org.eclipse.microprofile.lra.client.LRAClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/app")
@LRA(LRA.Type.SUPPORTS)
public class Resource {

    @Inject
    LRAClient lraClient;

    private SpecIT test;

    @PostConstruct
    private void setup() {
        SpecIT.beforeClass(lraClient);
        test = new SpecIT();
    }

    @PreDestroy
    private void tearDown() {
        SpecIT.afterClass();
    }

    @GET
    @Path("/greet")
    @Produces("text/html")
    public String greet() {
        return System.currentTimeMillis() + "";
    }

    @PUT
    @Path("/test/{name}")
    public Response runTest(@PathParam("name") String testName) throws Exception {
        String result = "";

        test.before();

        if ("start".equalsIgnoreCase(testName))
            result = test.startLRA();
        else if ("joinLRAViaHeader".equalsIgnoreCase(testName))
            result = test.joinLRAViaHeader();

        test.after();

        return Response.ok(result).build();
    }

    @PUT
    @Path("/work")
    @LRA(LRA.Type.REQUIRED)
    public Response activityWithLRA(@HeaderParam(LRAClient.LRA_HTTP_RECOVERY_HEADER) String rcvId,
                                    @HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) {
        System.out.printf("lra: %s and lraClient: %s%n", lraId, lraClient);

        return Response.ok(lraId).build();
    }
}
