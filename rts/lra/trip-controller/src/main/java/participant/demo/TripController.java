/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package participant.demo;

import io.narayana.lra.annotation.Compensate;
import io.narayana.lra.annotation.CompensatorStatus;
import io.narayana.lra.annotation.Complete;
import io.narayana.lra.annotation.LRA;
import io.narayana.lra.annotation.Leave;
import io.narayana.lra.annotation.Status;
import io.narayana.lra.client.InvalidLRAId;
import io.narayana.lra.client.LRAClient;
import participant.model.Booking;
import participant.model.BookingStatus;
import participant.service.TripService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static io.narayana.lra.client.LRAClient.LRA_HTTP_HEADER;

@RequestScoped
@Path(TripController.TRIP_PATH)
@LRA(LRA.Type.SUPPORTS)
public class TripController {
    public static final String HOTEL_PATH = "/hotel";
    public static final String HOTEL_NAME_PARAM = "hotelName";
    public static final String HOTEL_BEDS_PARAM = "beds";
    public static final String FLIGHT_PATH = "/flight";
    public static final String FLIGHT_NUMBER_PARAM = "flightNumber";
    public static final String ALT_FLIGHT_NUMBER_PARAM = "altFlightNumber";
    public static final String FLIGHT_SEATS_PARAM = "flightSeats";
    public static final String TRIP_PATH = "/trip";

    private Client hotelClient;
    private Client flightClient;

    private WebTarget hotelTarget;
    private WebTarget flightTarget;

    @Inject
    private TripService tripService;
    @Context
    private UriInfo context;

    @Context
    private HttpServletRequest httpRequest;

    private Map<String, CompensatorStatus> compensatorStatusMap = new HashMap<>();

    @PostConstruct
    private void initController() {
        try {
            URL HOTEL_SERVICE_BASE_URL = new URL("http://" + System.getProperty("hotel.service.http.host", "localhost") + ":" + Integer.getInteger("hotel.service.http.port", 8082));
            URL FLIGHT_SERVICE_BASE_URL = new URL("http://" + System.getProperty("flight.service.http.host", "localhost") + ":" + Integer.getInteger("flight.service.http.port", 8083));

            hotelClient = ClientBuilder.newClient();
            flightClient = ClientBuilder.newClient();

            hotelTarget = hotelClient.target(URI.create(new URL(HOTEL_SERVICE_BASE_URL, HOTEL_PATH).toExternalForm()));
            flightTarget = flightClient.target(URI.create(new URL(FLIGHT_SERVICE_BASE_URL, FLIGHT_PATH).toExternalForm()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void finiController() {
        hotelClient.close();
        flightClient.close();
    }

    /**
     * The quickstart scenario is:
     *
     * start LRA 1
     *   Book hotel
     *   start LRA 2
     *     start LRA 3
     *       Book flight option 1
     *     start LRA 4
     *       Book flight option 2
     *
     * @param hotelName hotel name
     * @param hotelGuests number of beds required
     * @param flightSeats number of people flying
     */
    @POST
    @Path("/book")
    @Produces(MediaType.APPLICATION_JSON)
    // delayClose because we want the LRA to be associated with a booking until the user confirms the booking
    @LRA(value = LRA.Type.REQUIRED, delayClose = true)
    public Response bookTrip( @HeaderParam(LRA_HTTP_HEADER) String lraId,
                              @QueryParam(HOTEL_NAME_PARAM) @DefaultValue("") String hotelName,
                              @QueryParam(HOTEL_BEDS_PARAM) @DefaultValue("1") Integer hotelGuests,
                              @QueryParam(FLIGHT_NUMBER_PARAM) @DefaultValue("") String flightNumber,
                              @QueryParam(ALT_FLIGHT_NUMBER_PARAM) @DefaultValue("") String altFlightNumber,
                              @QueryParam(FLIGHT_SEATS_PARAM) @DefaultValue("1") Integer flightSeats,
                              @QueryParam("mstimeout") @DefaultValue("500") Long timeout) throws BookingException {

        Booking hotelBooking = bookHotel(hotelName, hotelGuests);
        Booking flightBooking1 = bookFlight(flightNumber, flightSeats);
        Booking flightBooking2 = bookFlight(altFlightNumber, flightSeats);

        Booking tripBooking = new Booking(lraId, "Trip", hotelBooking, flightBooking1, flightBooking2);

        return Response.status(Response.Status.CREATED).entity(tripBooking).build();
    }

    @PUT
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.SUPPORTS) // the confirmation could be part of an enclosing LRA
    public Booking confirmTrip(Booking booking) throws BookingException {
        tripService.confirmBooking(booking);

        booking.setStatus(BookingStatus.CONFIRMED);

        return booking;
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.SUPPORTS) // the confirmation could be part of an enclosing LRA
    public Booking cancelTrip(Booking booking) throws BookingException {
        tripService.cancelBooking(booking);

        booking.setStatus(BookingStatus.CANCELLED);

        return booking;
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response status(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        Booking booking = tripService.get(lraId);

        return Response.ok(booking.getStatus().name()).build(); // TODO convert to a CompensatorStatus if we we're enlisted in an LRA
    }

    private Booking bookHotel(String name, int beds) throws BookingException {
        if (name == null || name.length() == 0 || beds <= 0)
            return null;

        WebTarget webTarget = hotelTarget
                .path("book")
                .queryParam(HOTEL_NAME_PARAM, name).queryParam(HOTEL_BEDS_PARAM, beds);

        Response response = webTarget.request().post(Entity.text(""));

        if (response.getStatus() != Response.Status.OK.getStatusCode())
            throw new BookingException(response.getStatus(), "flight booking problem");

        return response.readEntity(Booking.class);
    }

    private Booking bookFlight(String flightNumber, int seats) throws BookingException {
        if (flightNumber == null || flightNumber.length() == 0 || seats <= 0)
            return null;

        WebTarget webTarget = flightTarget
                .path("book")
                .queryParam(FLIGHT_NUMBER_PARAM, flightNumber)
                .queryParam(FLIGHT_SEATS_PARAM, seats);

        Response response = webTarget.request().post(Entity.text(""));

        if (response.getStatus() != Response.Status.OK.getStatusCode())
            throw new BookingException(response.getStatus(), "flight booking problem");

        return response.readEntity(Booking.class);
    }

    @GET
    @Path("/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.SUPPORTS)
    public Booking getBooking(@PathParam("bookingId") String bookingId) {
        return tripService.get(bookingId);
    }

    /**
     * Tell the compensator to move to the requested state.
     *
     * @param status the next state to move to
     * @param bookingId the current LRA context
     * @return the state that compensator achieved
     */
    protected CompensatorStatus updateCompensator(CompensatorStatus status, String bookingId) {
        switch (status) {
            case Completed:
                tripService.updateBookingStatus(bookingId, BookingStatus.CONFIRMED);
                return status;
            case Compensated:
                tripService.updateBookingStatus(bookingId, BookingStatus.CANCELLED);
                return status;
            default:
                return status;
        }
    }

    protected String getCompensatorData(String activityId) {
        return null;
    }
    /**
     * Get the LRA context of the currently running method.
     * Note that @HeaderParam(LRA_HTTP_HEADER) does not match the header (done't know why) so we the httpRequest
     *
     * @return the LRA context of the currently running method
     */
    protected String getCurrentActivityId() {
        return httpRequest.getHeader(LRA_HTTP_HEADER);
    }

    @POST
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    public Response completeWork() throws NotFoundException {
        return updateState(CompensatorStatus.Completed, getCurrentActivityId());
    }

    @POST
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork() throws NotFoundException {
        return updateState(CompensatorStatus.Compensated, getCurrentActivityId());
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response status() throws NotFoundException {
        String lraId = getCurrentActivityId();

        if (lraId == null)
            throw new InvalidLRAId("null", "not present on CompletionHandler#status request", null);

        if (!compensatorStatusMap.containsKey(lraId))
            throw new InvalidLRAId(lraId, "CompletionHandler#status request: unknown lra id", null);

        // return status ok together with optional completion data or one of the other codes with a url that
        // returns

        /*
         * the compensator will either return a 200 OK code (together with optional completion data) or a URL which
         * indicates the outcome. That URL can be probed (via GET) and will simply return the same (implicit) information:
         *
         * <URL>/cannot-compensate
         * <URL>/cannot-complete
         *
         * TODO I am returning the status url instead. And if the status is compensated or completed then performing
         * GET on it will return 200 OK together with a compensator specific string that the business operation can
         * reason about, otherwise some other suitable status code is returned together with one of he valid
         * compensator states.
         */
        return updateState(compensatorStatusMap.get(lraId), lraId);
    }

    @PUT
    @Path("/leave")
    @Produces(MediaType.APPLICATION_JSON)
    @Leave
    public Response leaveWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        return Response.ok().build();
    }

    /**
     * If the compensator was successful return a 200 status code and optionally an application specific string
     * that can be used by whoever closed the LRA (that triggered this compensator).
     * <p>
     * Otherwise return a status url that can be probed to obtain the final outcome when it is ready
     *
     * @param status
     * @param activityId
     * @return
     */
    private Response updateState(CompensatorStatus status, String activityId) {

        CompensatorStatus newStatus = updateCompensator(status, activityId);

        compensatorStatusMap.put(activityId, newStatus); // NB in the demo we never remove completed activities

        switch (newStatus) {
            case Completed:
            case Compensated:
                String data = getCompensatorData(activityId);

                return data == null ? Response.ok().build() : Response.ok(data).build();
            default:
                String statusUrl = getStatusUrl(activityId);

                return Response.status(Response.Status.ACCEPTED).entity(Entity.text(statusUrl)).build();
        }
    }

    private String getStatusUrl(String lraId) {
        return String.format("%s/%s/activity/status", context.getBaseUri(), LRAClient.getLRAId(lraId));
    }
}

