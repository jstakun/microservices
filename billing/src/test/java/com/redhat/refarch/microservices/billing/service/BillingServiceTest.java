package com.redhat.refarch.microservices.billing.service;

import static org.junit.Assert.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.junit.Test;

public class BillingServiceTest {
	
	private Logger logger = Logger.getLogger( getClass().getName() );

	@Test
	public void test() {
		Response info = new BillingService().info();
		assertNotNull("Info is empty!", info);
		logger.log(Level.INFO, "Executing test: " + getClass().getName());
	}

}
