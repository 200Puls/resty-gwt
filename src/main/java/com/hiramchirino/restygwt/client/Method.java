/**
 * Copyright (C) 2009  Hiram Chirino
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hiramchirino.restygwt.client;

import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;

/**
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Method {

    /**
     * GWT hides the full spectrum of methods because safari
     * has a bug: http://bugs.webkit.org/show_bug.cgi?id=3812
     *
     * We extend assume the server side will also check the 
     * X-HTTP-Method-Override header.
     * 
     * TODO: add an option to support using this approach to bypass
     * restrictive firewalls even if the browser does support the
     * setting all the method types.
     * 
     * @author chirino
     */
	static private class MethodRequestBuilder extends RequestBuilder {
		public MethodRequestBuilder(String method, String url) {
			super(method, url);
	        setHeader("X-HTTP-Method-Override", method);
		}
	}

	RequestBuilder builder;
	int expectedStatus = 200;
	Request request;
	Response response;

	public Method(Resource resource, String method) {
		String uri = resource.getUri();
		if( resource.getQuery()!=null ) {
		    uri += "?"+resource.getQuery();
		}
        builder = new MethodRequestBuilder(method, uri);
	}

	public Method user(String user) {
		builder.setUser(user);
		return this;
	}

	public Method password(String password) {
		builder.setPassword(password);
		return this;
	}

	public Method header(String header, String value) {
		builder.setHeader(header, value);
		return this;
	}

	public Method headers(Map<String, String> headers) {
		if (headers != null) {
			for (Entry<String, String> entry : headers.entrySet()) {
				builder.setHeader(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}

	public Method text(String data) {
		defaultContentType(Resource.CONTENT_TYPE_TEXT);
		builder.setRequestData(data);
		return this;
	}

	public Method json(JSONValue data) {
		defaultContentType(Resource.CONTENT_TYPE_JSON);
		builder.setRequestData(data.toString());
		return this;
	}

	public Method xml(Document data) {
		defaultContentType(Resource.CONTENT_TYPE_XML);
		builder.setRequestData(data.toString());
		return this;
	}

	public Method timeout(int timeout) {
		builder.setTimeoutMillis(timeout);
		return this;
	}
	
    public void expect(int status) throws RequestException {
        this.expectedStatus = status;
    }

	public void send(RequestCallback callback) throws RequestException {
		builder.setCallback(callback);
        GWT.log("Sending http request: "+builder.getHTTPMethod()+" "+builder.getUrl(), null);
        GWT.log(builder.getRequestData(), null);
        request = builder.send();
	}

	public void send(final TextCallback callback) {
		defaultAcceptType(Resource.CONTENT_TYPE_TEXT);
        try {
            send(new AbstractRequestCallback<String>(this, callback) {
                protected String parseResult() throws Exception {
                    GWT.log("Received http response for: "+builder.getHTTPMethod()+" "+builder.getUrl(), null);
                    GWT.log(response.getText(), null);
                    return response.getText();
                }
            });
        } catch (RequestException e) {
            GWT.log("Received http error for: "+builder.getHTTPMethod()+" "+builder.getUrl(), e);
            callback.onFailure(this, e);
        }
	}

	public void send(final JsonCallback callback) {
		defaultAcceptType(Resource.CONTENT_TYPE_JSON);
        try {
            send(new AbstractRequestCallback<JSONValue>(this, callback) {
                protected JSONValue parseResult() throws Exception {
                    GWT.log("Received http response for: "+builder.getHTTPMethod()+" "+builder.getUrl(), null);
                    GWT.log(response.getText(), null);
                    return JSONParser.parse(response.getText());
                }
            });
        } catch (RequestException e) {
            GWT.log("Received http error for: "+builder.getHTTPMethod()+" "+builder.getUrl(), e);
            callback.onFailure(this, e);
        }
	}

	public void send(final XmlCallback callback) {
		defaultAcceptType(Resource.CONTENT_TYPE_XML);
        try {
            send(new AbstractRequestCallback<Document>(this, callback) {
                protected Document parseResult() throws Exception {
                    GWT.log("Received http response for: "+builder.getHTTPMethod()+" "+builder.getUrl(), null);
                    GWT.log(response.getText(), null);
                    return XMLParser.parse(response.getText());
                }
            });
        } catch (RequestException e) {
            GWT.log("Received http error for: "+builder.getHTTPMethod()+" "+builder.getUrl(), e);
            callback.onFailure(this, e);
        }
	}

	public Request getRequest() {
		return request;
	}

	public Response getResponse() {
		return response;
	}

	protected void defaultContentType(String type) {
		if (builder.getHeader(Resource.HEADER_CONTENT_TYPE) == null) {
		    header(Resource.HEADER_CONTENT_TYPE, type);
		}
	}
	protected void defaultAcceptType(String type) {
		if (builder.getHeader(Resource.HEADER_ACCEPT) == null) {
			header(Resource.HEADER_ACCEPT, type);
		}
	}

	
}
