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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

/**
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public abstract class AbstractRequestCallback<T> implements RequestCallback {
    private final Method method;
    protected MethodCallback<T> callback;
	
	public AbstractRequestCallback(Method method, MethodCallback<T> callback) {
        this.method = method;
		this.callback = callback;
	}

	final public void onError(Request request, Throwable exception) {
		this.method.request = request;
		callback.onFailure(this.method, exception);
	}

	final public void onResponseReceived(Request request, Response response) {
		this.method.request = request;
		this.method.response = response;
		if (response.getStatusCode() != this.method.expectedStatus) {
			callback.onFailure(this.method, new FailedStatusCodeException(
					response.getStatusText()));
		} else {
            T value;
            try {
                value = parseResult();
            } catch (Throwable e) {
                callback.onFailure(this.method, e);
                return;
            }
            callback.onSuccess(this.method, value );            
		}
	}
	
	abstract protected T parseResult() throws Exception;
}