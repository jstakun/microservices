package com.redhat.refarch.microservices.presentation;

import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ServiceProvider {

	private static final Logger logger = Logger.getLogger( ServiceProvider.class.getName() );
	private static final Properties properties = new Properties();
	private static final Map<String, String> routes = new HashMap<String, String>();
	private static final Map<String, String> services = new HashMap<String, String>();
	private static final Map<String, Integer> services_ports = new HashMap<String, Integer>();
	private static boolean initialized = false;
	
	private static final ServiceProvider instance = new ServiceProvider();
	
	public static ServiceProvider getInstance() {
		if (!initialized) {
			initialize();
		}
		return instance;
	}
	
	private ServiceProvider() {
        //Map<String, String> env = System.getenv();
        //for (String envName : env.keySet()) {
        //    logger.log(Level.INFO, "Env variable: " + envName);
        //}
		
		try {
			InputStream inputStream  = this.getClass().getResourceAsStream("osemaster.properties");
        	if (inputStream != null) {
        		properties.load(inputStream);
        	} else {
        		throw new NullPointerException("Input stream is null !!!");
        	}
        	
    		String[] routesArr = StringUtils.split(properties.getProperty("routes", "product,sales,billing,eventbus"), ",");
    		
    		for (int i=0;i<routesArr.length;i++) {
    			String name = routesArr[i];
    			logger.log(Level.INFO, "Creating route " + name);
    			routes.put(name, "localhost");
    			services.put(name, "127.0.0.1");
    			services_ports.put(name, 80);
    		}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}	
	}
		
	private static void initialize() {          
		try {
			SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy() {
	                public boolean isTrusted(X509Certificate[] certificate, String authType)
	                    throws CertificateException {
	                    //trust all certs
	                    return true;
	                }
	        }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	        
			HttpClient client = new DefaultHttpClient();
			client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 8443, sf));
	            
            HttpGet get = new HttpGet( getOSEv3ApiUrl(properties.getProperty("project", "microservices"), ServiceProvider.ApiEndpoint.Routes).build() );
    		get.addHeader("Authorization", "Bearer " + getToken()); 
    		logger.log(Level.INFO, "Executing " + get );
    		HttpResponse response = client.execute( get );
    		String responseString = EntityUtils.toString( response.getEntity() );
    		if (responseString.startsWith("{")) {
    			JSONObject root = new JSONObject( responseString );
    			JSONArray items = root.optJSONArray("items");
    			if (items != null) {
    				for (int i = 0 ;i < items.length(); i++) {
    					JSONObject spec = items.getJSONObject(i).getJSONObject("spec");
    					String host = spec.getString("host");
    					String name = spec.getJSONObject("to").getString("name");
    					routes.put(name, host);
    					logger.log(Level.INFO, "Found route " + name + " to " + host);
    				}
    			} else {
    				logger.log(Level.INFO, "Received following response: " + responseString);
    			}
    		} else {
    			logger.log(Level.INFO, "Received following response: " + responseString); 
    		}
    		
    		HttpGet get2 = new HttpGet( getOSEv3ApiUrl(properties.getProperty("project", "microservices"), ServiceProvider.ApiEndpoint.Services).build() );
    		get2.addHeader("Authorization", "Bearer " + getToken()); 
    		logger.log(Level.INFO, "Executing " + get2 );
    		HttpResponse response2 = client.execute( get2 );
    		String responseString2 = EntityUtils.toString( response2.getEntity() );
    		if (responseString2.startsWith("{")) {
    			JSONObject root = new JSONObject( responseString2 );
    			JSONArray items = root.optJSONArray("items");
    			if (items != null) {
    				for (int i = 0 ;i < items.length(); i++) {
    					JSONObject spec = items.getJSONObject(i).getJSONObject("spec");
    					String host = spec.getString("clusterIP");
    					Integer port = spec.getJSONArray("ports").getJSONObject(0).getInt("port");
    					String name = items.getJSONObject(i).getJSONObject("metadata").getString("name");
    					services.put(name, host);
    					services_ports.put(name, port);
    					logger.log(Level.INFO, "Found service " + name + " at " + host);
    				} 
    				initialized = true;
    			} else {
    				logger.log(Level.INFO, "Received following response: " + responseString2);
    			}
    		} else {
    			logger.log(Level.INFO, "Received following response: " + responseString2); 
    		}
    		
		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}	   
	}
	
	protected enum Service
	{
		Product, Sales, Billing, EventBus
	}
	
	private enum ApiEndpoint {
		Pods, Routes, Services;
	}

	protected URIBuilder getUriBuilder(Service service, Object... path)
	{
		//return getRouteUriBuilder(service, path);
		return getServiceUriBuilder(service, path);
	}
	
	private URIBuilder getRouteUriBuilder(Service service, Object... path)
	{
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme( "http" );
		StringWriter stringWriter = new StringWriter();
		switch( service )
		{
			case Product:
				uriBuilder.setHost( routes.get("product") );
				break;
	
			case Sales:
				uriBuilder.setHost( routes.get("sales") );
				break;
	
			case Billing:
				uriBuilder.setHost( routes.get("billing") );
				break;
				
			case EventBus:
				uriBuilder.setHost( routes.get("eventbus") );
				break;
				
			default:
				throw new IllegalStateException( "Unknown service" );
		}
		uriBuilder.setPort(Integer.valueOf(properties.getProperty("http_port", "80")));
		for( Object part : path )
		{
			stringWriter.append( '/' ).append( String.valueOf( part ) );
		}
		uriBuilder.setPath( stringWriter.toString() );
		return uriBuilder;
	};
	
	private URIBuilder getServiceUriBuilder(Service service, Object... path)
	{
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme( "http" );
		StringWriter stringWriter = new StringWriter();
		switch( service )
		{
			case Product:
				uriBuilder.setHost(services.get("product"));
				uriBuilder.setPort(services_ports.get("product"));
				break;
	
			case Sales:
				uriBuilder.setHost( services.get("sales") );
				uriBuilder.setPort(services_ports.get("sales"));
				break;
	
			case Billing:
				uriBuilder.setHost( services.get("billing") );
				uriBuilder.setPort(services_ports.get("billing"));
				break;
	       
			case EventBus:
				uriBuilder.setHost( services.get("eventbus") );
				uriBuilder.setPort(services_ports.get("eventbus"));
				break;	
				
			default:
				throw new IllegalStateException( "Unknown service" );
		}
		for( Object part : path )
		{
			stringWriter.append( '/' ).append( String.valueOf( part ) );
		}
		logger.log(Level.INFO, "Created service path: " + stringWriter.toString());
		uriBuilder.setPath( stringWriter.toString() );
		return uriBuilder;
	};

	private static URIBuilder getOSEv3ApiUrl(String namespace, ApiEndpoint apiEndpoint) {
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme("https");
		uriBuilder.setHost(properties.getProperty("host"));
		uriBuilder.setPort(Integer.valueOf(properties.getProperty("https_port", "443")));
		switch (apiEndpoint) {
			case Pods:
				uriBuilder.setPath("/api/v1/namespaces/" + namespace + "/pods");
				break;
			case Routes:
				uriBuilder.setPath("/oapi/v1/namespaces/" + namespace + "/routes");
				break;
			case Services:
				uriBuilder.setPath("/api/v1/namespaces/" + namespace + "/services");
				break;
			default:
				throw new IllegalStateException( "Unknown API endpoint" );	
		}
		return uriBuilder;
	}
	
	private static String getToken() {
		String token = System.getenv("API_TOKEN");
		if (token == null || token.length() == 0) {
			logger.log(Level.INFO, "Reading token from config file");
		    token = properties.getProperty("token"); 
		} else {
			logger.log(Level.INFO, "Reading token from environment variable");
		}
		return token;
	}
}
