package com.hch.koulovesu;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import android.graphics.Bitmap;
import android.util.Log;

public class ConnectionHelper {
	
	public static final String TAG_CONNECTION = "connection";
	public static final int HTTP_CONNECTION_TIMEOUT = 10000;
	public static final String SERVER_IP = "http://hch.no-ip.org:85/";
	
	private static HttpClient httpClient = getHttpClient();
	
	private static HttpClient getHttpClient() {
		if(httpClient == null) {
			HttpParams httpParams = new BasicHttpParams();
			
			httpParams.setParameter("http.protocol.content-charset", HTTP.UTF_8);
            httpParams.setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
			
            HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpParams, HTTP_CONNECTION_TIMEOUT);
            HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
            HttpProtocolParams.setHttpElementCharset(httpParams, HTTP.UTF_8);

            httpClient = new DefaultHttpClient(httpParams);
		}
		return httpClient;
	}
	
	public static class HttpResult {
		boolean success;
        String responsedString;
    }
	
	protected interface HttpRequestDelegate<T> {
        public void didGetResponse(boolean success, String responsedString);
    }
	
	protected static <T> void sendGetRequest(final String request, final HashMap<String, Object> params, final HttpRequestDelegate<T> delegate) {
        new Thread(new Runnable() {
            public void run() {
            	synchronized (httpClient) {
            		try {
                        String urlWithParams = SERVER_IP + request + "?";
                        
                        if(params != null) {
    	                    Set<String> keys = params.keySet();
    	                    for(String key : keys) {
    	                    	String value = params.get(key).toString();
    	                    	urlWithParams = urlWithParams + key + "=" + value + "&";
    	                    }
                        }

                        urlWithParams = urlWithParams.substring(0, urlWithParams.length()-1);

                        Log.i(TAG_CONNECTION, "SEND GET REQUEST: " + urlWithParams);

            			HttpGet httpGet = new HttpGet(urlWithParams);
            			HttpResponse response = httpClient.execute(httpGet);

                        int statusCode = response.getStatusLine().getStatusCode();   
                        
                        if(statusCode != 200 && statusCode != 304) {
	                        throw new Exception("Server responsed error " + String.valueOf(statusCode));
	                    }

                        final String responsedString = EntityUtils.toString(response.getEntity(), "utf-8");
                        
                        Log.i(TAG_CONNECTION, "Received: " + responsedString);
                        
                        delegate.didGetResponse(true, responsedString);
                    } catch (Exception e) {
                    	Log.e(TAG_CONNECTION, "GET REQUEST EXCEPTION: " + e.getMessage());

                        delegate.didGetResponse(false, e.getMessage());
                    }
				}
            }
        }).start();
    }
	
	protected static <T> void sendPostRequestAsync(final String request, final HashMap<String, Object> params, final HttpRequestDelegate<T> delegate) {
        new Thread(new Runnable() {
            public void run() {
            	HttpResult result = sendPostRequest(request, params);
            	delegate.didGetResponse(result.success, result.responsedString);
            }
        }).start();
    }

	protected static HttpResult sendPostRequest(final String request, final HashMap<String, Object> params) {
		synchronized (httpClient) {
			HttpResult result = new HttpResult();
            try {
            	
            	String url = SERVER_IP + request;
            	
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

                if(params != null) {
                    Set<String> keys = params.keySet();
                    for(String key : keys) {
                    	String value = params.get(key).toString();
                        nameValuePairs.add(new BasicNameValuePair(key, value));
                    }
        		}
        		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8);
                
        		HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(entity);

                Log.i(TAG_CONNECTION, "SEND POST REQUEST: " + url + nameValuePairs);

                // Execute HTTP Post Request
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity responseEntity = httpResponse.getEntity();
                
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                
                InputStream instream = responseEntity.getContent();
                String responsedString = instream != null ? convertStreamToString(instream) : null;
                
                if(statusCode == 200 || statusCode == 304) {
                	if (responsedString != null) {
                        
                        Log.i(TAG_CONNECTION, "Received: " + responsedString);
                        
                        if(responsedString.startsWith("SC")) {
                        	result.success = true;
                        } else {
                        	result.success = false;
                        }
                        
                        result.responsedString = responsedString;
                    } else {
                    	result.success = false;
                        result.responsedString = null;
                    }
                }
                else {
                	result.success = false;
                    result.responsedString = String.format("Error %d, %s", statusCode, responsedString);
                }
            } catch (Exception e) {
            	Log.e(TAG_CONNECTION, "POST REQUEST EXCEPTION: " + e.getMessage());

            	result.success = false;
                result.responsedString = e.getMessage();
            }
            return result;
    	}
    }
    
    protected static <T> void sendMultipartPostRequest(final String request, final HashMap<String, Object> params, final HttpRequestDelegate<T> delegate) {
        new Thread(new Runnable() {
            public void run() {
		    	try {
		    		final String url = SERVER_IP + request;
		    		final String boundary = "gc0p4Jq0M2Yt08jU534c0p";
		    		final String eol = "\r\n";
			    	URL urlObject = new URL(url);
			    	HttpURLConnection urlconn = (HttpURLConnection)urlObject.openConnection();
			    	urlconn.setRequestMethod("POST");
			    	urlconn.setRequestProperty("Connection", "Keep-Alive");
			    	urlconn.setRequestProperty("Content-Type","multipart/form-data; boundary=" + boundary);
			    	urlconn.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
			    	urlconn.setUseCaches(false);
			    	urlconn.setDoOutput(true);
			    	urlconn.setDoInput(true);
			    	urlconn.connect();
			    	
			    	OutputStream outstream = new BufferedOutputStream(urlconn.getOutputStream());
			    	
			    	Set<String> keys = params.keySet();
			    	for(String name : keys) {
			    		outstream.write(String.format("--%s%s", boundary, eol).getBytes());
			    		
			    		Object value = params.get(name);
			    		if(value.getClass().equals(Bitmap.class)) {
			    			outstream.write(
			    				String.format(
		    						"Content-Disposition: form-data; name=\"%s\"; filename=\"tmp.png\" %s" + 
		    						"Content-type: image/png %s%s", name, eol, eol, eol).getBytes());
			    			
			    			Bitmap bitmap = (Bitmap)value;
			    			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			    			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			    			
			    			outstream.write(stream.toByteArray());
			    			
			    		} else {
			    			outstream.write(
								String.format(
									"Content-Disposition: form-data; name=\"%s\" %s" + 
									"Content-type: text/plain %s%s", name, eol, eol, eol).getBytes());
			    			outstream.write( String.format(value.toString()).getBytes() );
			    		}
			    		outstream.write( eol.getBytes() );
			    	}
			    	
			    	outstream.write( String.format("--%s--%s", boundary, eol).getBytes());
			    	
			    	outstream.flush();
			    	outstream.close();
			    	
			    	//Get returned data
			    	String responsedString = "";
			    	InputStream instream = new BufferedInputStream(urlconn.getInputStream());
			    	byte[] buf = new byte[2048];
			    	int blen = -1;
			    	while (  (blen = instream.read(buf)) >=0 ) {
			    		responsedString += new String(buf,0,blen);
			    	}
			    	instream.close();
			    	
			    	//Close connection
			    	urlconn.disconnect();
			    	
			    	//Handle returned data
                    
                    Log.i(TAG_CONNECTION, "Received: " + responsedString);
                    
                    delegate.didGetResponse(true, responsedString);
                    
			    	
		    	} catch (Exception e) {
		    		Log.e(TAG_CONNECTION, "POST REQUEST EXCEPTION: " + e.getMessage());
		    		delegate.didGetResponse(false, e.getMessage());
		    	}
            }
        }).start();
    }
    
    private static String convertStreamToString(InputStream is) {
        try {
            if (is != null) {
                Writer writer = new StringWriter();

                char[] buffer = new char[1024];
                try {
                    Reader reader = new BufferedReader(
                            new InputStreamReader(is, "UTF-8"));
                    int n;

                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } finally {
                    is.close();
                }
                return writer.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
