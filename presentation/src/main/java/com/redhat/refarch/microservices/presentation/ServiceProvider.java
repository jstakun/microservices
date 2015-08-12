package com.redhat.refarch.microservices.presentation;

import java.io.StringWriter;

import org.apache.http.client.utils.URIBuilder;

public class ServiceProvider {

	private static final String DOMAIN = ".cloudapps.osecloud.com";
	
	protected enum Service
	{
		Product, Sales, Billing
	}

	protected static URIBuilder getUriBuilder(Service service, Object... path)
	{
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme( "http" );
		StringWriter stringWriter = new StringWriter();
		switch( service )
		{
			case Product:
				uriBuilder.setHost( "product" + DOMAIN );
				//stringWriter.append( "/product" );
				break;
	
			case Sales:
				uriBuilder.setHost( "sales" + DOMAIN );
				//stringWriter.append( "/sales" );
				break;
	
			case Billing:
				uriBuilder.setHost( "billing" + DOMAIN );
				//stringWriter.append( "/billing" );
				break;
	
			default:
				throw new IllegalStateException( "Unknown service" );
		}
		uriBuilder.setPort( 80 );
		for( Object part : path )
		{
			stringWriter.append( '/' ).append( String.valueOf( part ) );
		}
		uriBuilder.setPath( stringWriter.toString() );
		return uriBuilder;
	};

	
	
}
