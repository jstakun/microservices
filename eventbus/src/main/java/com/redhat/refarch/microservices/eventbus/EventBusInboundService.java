package com.redhat.refarch.microservices.eventbus;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/")
public class EventBusInboundService {

	private Logger logger = Logger.getLogger( getClass().getName() );
	
	@POST
	@Path("/event/customer/{id}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/xml"})
	public Response newCustomerEvent(Long id) {
		logInfo("New customer " + id + " created");
		return Response.status(200).entity("<status>newCustomer " + id + " event received</status>").build();
	}
	
	@POST
	@Path("/event/order/{id}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/xml"})
	public Response newOrderEvent(Long id) {
		logInfo("New order " + id + " created");
		return Response.status(200).entity("<status>newOrder " + id + " event received</status>").build();
	}
	
	@POST
	@Path("/event/product/{id}")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/xml"})
	public Response newProductEvent(Long id) {
		logInfo("New product " + id + " created");
		return Response.status(200).entity("<status>newProduct " + id + " event received</status>").build();
	}
	
	@GET
	@Path("/info")
	@Consumes({"*/*"})
	@Produces({"application/xml"})
	public Response info() {
		return Response.status(200).entity("<info><name>Event bus service</name><version>1.0.1</version></info>").build();
	}
	
	private void logInfo(String message)
	{
		logger.log( Level.INFO, message );
	}
}