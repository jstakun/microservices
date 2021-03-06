package com.redhat.refarch.microservices.billing.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.redhat.refarch.microservices.billing.model.Result;
import com.redhat.refarch.microservices.billing.model.Result.Status;
import com.redhat.refarch.microservices.billing.model.Transaction;

@Path("/")
public class BillingService
{

	private Logger logger = Logger.getLogger( getClass().getName() );

	private static final Random random = new Random();
	
	private static final String VERSION = "1.0.3b38";

	@POST
	@Path("/process")
	@Consumes({"application/json", "application/xml"})
	@Produces({"application/json", "application/xml"})
	public Result process(Transaction transaction)
	{
		Result result = new Result();
		result.setName( transaction.getCustomerName() );
		result.setOrderNumber( transaction.getOrderNumber() );
		logInfo( "Asked to process credit card transaction: " + transaction );
		Calendar now = Calendar.getInstance();
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set( transaction.getExpYear(), transaction.getExpMonth(), 1 );
		if( calendar.after( now ) )
		{
			result.setTransactionNumber( random.nextInt( 9000000 ) + 1000000 );
			result.setTransactionDate( now.getTime() );
			result.setStatus( Status.SUCCESS );
		}
		else
		{
			result.setStatus( Status.FAILURE );
		}
		return result;
	}

	@POST
	@Path("/refund/{transactionNumber}")
	@Consumes({"*/*"})
	@Produces({"application/json", "application/xml"})
	public void refund(@PathParam("transactionNumber") int transactionNumber)
	{
		logInfo( "Asked to refund credit card transaction: " + transactionNumber );
	}
	
	@GET
	@Path("/info")
	@Produces({"application/xml"})
	public Response info() {
		String addr = null;
		try {
			addr = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			addr = "unknown";
		}
			  	
		return Response.status(200).entity("<info><name>Billing service</name><version>" + VERSION + "</version><ip>" + addr + "</ip></info>").build();
	}
	
	@GET
	@Produces({"application/xml"})
	public Response root() {
	    return info();
	}

	private void logInfo(String message)
	{
		logger.log(Level.INFO, message);
	}
}