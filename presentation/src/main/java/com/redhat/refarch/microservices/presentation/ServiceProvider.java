package com.redhat.refarch.microservices.presentation;

import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static Logger logger = Logger.getLogger( ServiceProvider.class.getName() );
	
	private static Map<String, String> routes = new HashMap<String, String>();
	
	public ServiceProvider() {
		HttpClient client = new DefaultHttpClient();
		
		try {
			SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] certificate, String authType)
                    throws CertificateException {
                    //trust all certs
                    return true;
                }
            }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 8443, sf));
            
            HttpGet get = new HttpGet( ServiceProvider.getOSEv3ApiUrl("salesapp", ServiceProvider.ApiEndpoint.Routes).build() );
    		get.addHeader("Authorization", "Bearer " + ServiceProvider.TOKEN);
    		logger.log(Level.INFO, "Executing " + get );
    		HttpResponse response = client.execute( get );
    		String responseString = EntityUtils.toString( response.getEntity() );
    		if (responseString.startsWith("{")) {
    			JSONObject root = new JSONObject( responseString );
    			JSONArray items = root.getJSONArray("items");
    			for (int i = 0 ;i < items.length(); i++) {
    				JSONObject spec = items.getJSONObject(i).getJSONObject("spec");
    				String host = spec.getString("host");
    				String name = spec.getJSONObject("to").getString("name");
    				routes.put(name, host);
    			}
    		} else {
    			logger.log(Level.INFO, "Received following response: " + responseString); 
    		}
    		
		} catch (Exception ex) {
			ex.printStackTrace();
		}	   
	}
	
	private static final String TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJzYWxlc2FwcCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJlYXAtc2VydmljZS1hY2NvdW50LXRva2VuLXNzdTc1Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImVhcC1zZXJ2aWNlLWFjY291bnQiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJjYjAzMjk2ZC00NTk1LTExZTUtYjNhNi01MjU0MDAxNWUzZDEiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6c2FsZXNhcHA6ZWFwLXNlcnZpY2UtYWNjb3VudCJ9.gSGqaDsOD2xbxj7FbtumNiWrltg5CKsMTEig20REPANJkJv03BN5ydeYP5b4cBFoNvz8VQ55w1-fr3xCW6bN6QnBBZbJfQZfYEL4L75WpyjapSxGSfzwex1z5S0HF9roJ1Sx0kvmO3d58p8AfspQDTVdOt3s6AaDLT2DFqKEzy5J_P_ffascvZREPfZcZ5gaILbgLgywtiw1c2w8gLZ_1nmlhahejk_0ZLMxLkFUZ1OUxLxZT_d8yGdW7Z19v61gCi-ACAUny48zD_sLQz0pdxDloiGKvZILlj_l8C8mU9O69MIjX9dGInlW7a0fix4n5RWSNKfmJGTKXpyA0kaD0Q";
	
	protected enum Service
	{
		Product, Sales, Billing
	}
	
	protected enum ApiEndpoint {
		Pods, Routes;
	}

	protected static URIBuilder getUriBuilder(Service service, Object... path)
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

	private static URIBuilder getOSEv3ApiUrl(String namespace, ApiEndpoint apiEndpoint) {
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme("https");
		uriBuilder.setHost("master.osecloud.com");
		uriBuilder.setPort(8443);
		switch (apiEndpoint) {
			case Pods:
				uriBuilder.setPath("/api/v1/namespaces/" + namespace + "/pods");
				break;
			case Routes:
				uriBuilder.setPath("/oapi/v1/namespaces/" + namespace + "/routes");
				break;
			default:
				throw new IllegalStateException( "Unknown API endpoint" );	
		}
		return uriBuilder;
	}
	
}
