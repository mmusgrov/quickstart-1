package service;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/")
public class Controller {
	@Inject
	ControllerBean service;

	@GET
	@Path("/local/{arg}")
	public String getLocalNextCount(@DefaultValue("") @PathParam("arg") String arg) {
		return "Next: " + service.getNext(true, arg);
	}

	@GET
	@Path("/remote/{jndiPort}")
	public Response getRemoteNextCount(@DefaultValue("0") @PathParam("jndiPort") int jndiPort) {
		return getRemoteNextCountWithASAndError(jndiPort, null, null);
	}

	@GET
	@Path("/remote/{jndiPort}/{as}/{failureType}")
	public Response getRemoteNextCountWithASAndError(
			@DefaultValue("0") @PathParam("jndiPort") int jndiPort,
			@DefaultValue("") @PathParam("as") String as,
			@PathParam("failureType") String failureType) {
		return Response.status(200)
				.entity("Next: " + service.getNext(false, as, jndiPort, failureType))
				.build();
	}
}
