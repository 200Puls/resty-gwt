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
package com.hiramchirino.restygwt.rebind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.xml.client.Document;
import com.hiramchirino.restygwt.client.AbstractRequestCallback;
import com.hiramchirino.restygwt.client.JsonCallback;
import com.hiramchirino.restygwt.client.Method;
import com.hiramchirino.restygwt.client.MethodCallback;
import com.hiramchirino.restygwt.client.Resource;
import com.hiramchirino.restygwt.client.RestServiceProxy;
import com.hiramchirino.restygwt.client.TextCallback;
import com.hiramchirino.restygwt.client.XmlCallback;

/**
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class RestServiceClassCreator extends BaseSourceCreator {

    private static final String REST_SERVICE_PROXY_SUFFIX = "_Generated_RestServiceProxy_";

    private static final String METHOD_CLASS = Method.class.getName();
    private static final String RESOURCE_CLASS = Resource.class.getName();
    private static final String ABSTRACT_REQUEST_CALLBACK_CLASS = AbstractRequestCallback.class.getName();
    private static final String JSON_PARSER_CLASS = JSONParser.class.getName();
    private static final String REQUEST_EXCEPTION_CLASS = RequestException.class.getName();
    
    private static final String METHOD_PUT = "put";
    private static final String METHOD_POST = "post";
    private static final String METHOD_OPTIONS = "options";
    private static final String METHOD_HEAD = "head";
    private static final String METHOD_GET = "get";
    private static final String METHOD_DELETE = "delete";

    private static final HashSet<String> REST_METHODS = new HashSet<String>(8);
    static {
        REST_METHODS.add(METHOD_DELETE);
        REST_METHODS.add(METHOD_GET);
        REST_METHODS.add(METHOD_HEAD);
        REST_METHODS.add(METHOD_OPTIONS);
        REST_METHODS.add(METHOD_POST);
        REST_METHODS.add(METHOD_PUT);
    }


    private JClassType XML_CALLBACK_TYPE;
    private JClassType METHOD_CALLBACK_TYPE;
    private JClassType TEXT_CALLBACK_TYPE;
    private JClassType JSON_CALLBACK_TYPE;
    private JClassType DOCUMENT_TYPE;
    private JClassType METHOD_TYPE;
    private JClassType STRING_TYPE;
    private JClassType JSON_VALUE_TYPE;

    public RestServiceClassCreator(TreeLogger logger, GeneratorContext context, JClassType source) throws UnableToCompleteException {
        super(logger, context, source, REST_SERVICE_PROXY_SUFFIX);
    }

    protected ClassSourceFileComposerFactory createComposerFactory() {
        ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName, shortName);
        composerFactory.addImplementedInterface(source.getName());
        composerFactory.addImplementedInterface(RestServiceProxy.class.getName());
        return composerFactory;
    }

    protected void generate() throws UnableToCompleteException {
        this.XML_CALLBACK_TYPE = find(XmlCallback.class);
        this.METHOD_CALLBACK_TYPE = find(MethodCallback.class);
        this.TEXT_CALLBACK_TYPE = find(TextCallback.class);
        this.JSON_CALLBACK_TYPE = find(JsonCallback.class);
        this.DOCUMENT_TYPE = find(Document.class);
        this.METHOD_TYPE = find(Method.class);
        this.STRING_TYPE = find(String.class);
        this.JSON_VALUE_TYPE = find(JSONValue.class);

        if (source.isInterface() == null) {
            error("Type is not an interface.");
        }
        p("private " + RESOURCE_CLASS + " resource;");
        p();
        
        // TODO: init the resource if this is set:
        // Path pathAnnotation = source.getAnnotation(Path.class);
        // Path pathAnnotation = source.getAnnotation(RemoteServiceRelativePath.class);

        p("public void setResource(" + RESOURCE_CLASS + " resource) {").i(1);
        {
            p("this.resource = resource;");
        }
        i(-1).p("}");
        for (JMethod method : source.getMethods()) {
            writeMethodImpl(method);
        }
    }


    private void writeMethodImpl(JMethod method) throws UnableToCompleteException {
        if( method.getReturnType().isPrimitive() != JPrimitiveType.VOID ) {
            error("Invalid rest method. Method must have void return type: " + method.getReadableDeclaration());
        }
        
        p(method.getReadableDeclaration(false, false, false, false, true) + " {").i(1);
        {
            String restMethod = getRestMethod(method);
            LinkedList<JParameter> args = new LinkedList<JParameter>(Arrays.asList(method.getParameters()));

            // the last arg should be the callback.
            if (args.isEmpty()) {
                error("Invalid rest method. Method must declare at least a callback argument: " + method.getReadableDeclaration());
            }
            JParameter callbackArg = args.removeLast();
            JClassType callbackType = callbackArg.getType().isClassOrInterface();
            JClassType methodCallbackType = METHOD_CALLBACK_TYPE;
            if (callbackType == null || !callbackType.isAssignableTo(methodCallbackType)) {
                error("Invalid rest method. Last argument must be a " + methodCallbackType.getName() + " type: " + method.getReadableDeclaration());
            }
            JClassType resultType = getCallbackTypeGenericClass(callbackType);

            String pathExpression = null;
            Path pathAnnotation = method.getAnnotation(Path.class);
            if (pathAnnotation != null) {
                pathExpression = wrap(pathAnnotation.value());
            }

            JParameter contentArg=null;
            HashMap<String,JParameter> queryParams = new HashMap<String, JParameter>();
            HashMap<String,JParameter> headerParams = new HashMap<String, JParameter>();
            
            for (JParameter arg : args) {
                PathParam paramPath = arg.getAnnotation(PathParam.class);
                if (paramPath != null) {
                    if (pathExpression == null) {
                        error("Invalid rest method.  Invalid @PathParam annotation. Method is missing the @Path annotation: " + method.getReadableDeclaration());
                    }
                    pathExpression = pathExpression.replaceAll(Pattern.quote("{" + paramPath + "}"), "\"+" + toStringExpression(arg.getType(), arg.getName()) + "+\"");
                    continue;
                }
                
                QueryParam queryParam = arg.getAnnotation(QueryParam.class);
                if(queryParam!=null ) {
                    queryParams.put(queryParam.value(), arg);
                    continue;
                }
                
                HeaderParam headerParam = arg.getAnnotation(HeaderParam.class);
                if(headerParam!=null ) {
                    headerParams.put(headerParam.value(), arg);
                    continue;
                }

                if( contentArg!=null ) {
                    error("Invalid rest method. Only one content paramter is supported: " + method.getReadableDeclaration());
                }
                contentArg = arg;
            }

            String acceptTypeBuiltIn = null;
            if (callbackType.equals(TEXT_CALLBACK_TYPE)) {
                acceptTypeBuiltIn = "CONTENT_TYPE_TEXT";
            } else if (callbackType.equals(JSON_CALLBACK_TYPE)) {
                acceptTypeBuiltIn = "CONTENT_TYPE_JSON";
            } else if (callbackType.equals(XML_CALLBACK_TYPE)) {
                acceptTypeBuiltIn = "CONTENT_TYPE_XML";
            }
            
            if (acceptTypeBuiltIn == null) {
                p("final "+METHOD_CLASS+"  __method =");
            }

            p("this.resource");
            if (pathExpression != null) {
                // example: .resolve("path/"+arg0+"/id")
                p(".resolve(" + pathExpression + ")");
            }
            for (Map.Entry<String,JParameter> entry : queryParams.entrySet()) {
                String expr = entry.getValue().getName();
                p(".addQueryParam(" + wrap(entry.getKey()) + ", "+toStringExpression(entry.getValue().getType(), expr)+")");
            }
            // example: .get()
            p("." + restMethod + "()");
            
            for (Map.Entry<String,JParameter> entry : headerParams.entrySet()) {
                String expr = entry.getValue().getName();
                p(".header(" + wrap(entry.getKey()) + ", "+toStringExpression(entry.getValue().getType(), expr)+")");
            }
            
            if( contentArg!=null ) {
                if (contentArg.getType()==STRING_TYPE) {
                    p(".text("+contentArg.getName()+")");
                } else if (contentArg.getType() == JSON_VALUE_TYPE ) {
                    p(".json("+contentArg.getName()+")");
                } else if (contentArg.getType() == DOCUMENT_TYPE ) {
                    p(".xml("+contentArg.getName()+")");
                } else {
                    JClassType contentClass = contentArg.getType().isClass();
                    if( contentClass==null ) {
                        error("Content argument must be a class.");
                    }
                    // example: .json(Listings$_Generated_JsonEncoder_$.INSTANCE.encode(arg0) )
                    p(".json("+createJsonEncoder(contentClass)+".INSTANCE.encode("+contentArg.getName()+"))");
                }
            }
            
            if (acceptTypeBuiltIn != null) {
                p(".header(" + RESOURCE_CLASS + ".HEADER_ACCEPT, " + RESOURCE_CLASS + "." + acceptTypeBuiltIn + ");");
                p(".send(" + callbackArg.getClass() + ");");
            } else {
                p(".header(" + RESOURCE_CLASS + ".HEADER_ACCEPT, " + RESOURCE_CLASS + ".CONTENT_TYPE_JSON);");
                p("try {").i(1);
                {
                    p("__method.send(new "+ABSTRACT_REQUEST_CALLBACK_CLASS+"<"+resultType.getQualifiedSourceName()+">(__method, "+callbackArg.getName()+") {").i(1);
                    {
                        p("protected "+resultType.getQualifiedSourceName()+" parseResult() throws Exception {").i(1);
                        {
                            p("return "+createJsonEncoder(resultType)+".INSTANCE.decode("+JSON_PARSER_CLASS+".parse(__method.getResponse().getText()));");
                        }
                        i(-1).p("}");
                    }
                    i(-1).p("});");
                }
                i(-1).p("} catch ("+REQUEST_EXCEPTION_CLASS+" __e) {").i(1);
                {
                    p("callback.onFailure(__method,__e);");
                }
                i(-1).p("}");
            }
        }
        i(-1).p("}");
    }

    private String toStringExpression(JType type, String expr) {
        if ( type.isPrimitive()!=null ) {
            return "\"\"+"+expr;
        }
        if( STRING_TYPE == type ) {
            return expr;
        }
        return expr+".toString()";
    }

    private JClassType getCallbackTypeGenericClass(final JClassType callbackType) throws UnableToCompleteException {
        return branch("getCallbackTypeGenericClass()", new Branch<JClassType>(){
            public JClassType execute() throws UnableToCompleteException {
                
                for (JMethod method : callbackType.getMethods()) {
                    debug("checking method: "+method.getName());
                    if (method.getName().equals("onSuccess")) {
                        JParameter[] parameters = method.getParameters();
                        debug("checking method params: "+parameters.length);
                        if (parameters.length == 2) {
                            debug("checking first param: "+parameters[0].getType());
                            if (parameters[0].getType() == METHOD_TYPE) {
                                debug("checking 2nd param: "+parameters[1].getType());
                                JType param2Type = parameters[1].getType();
                                JClassType type = param2Type.isClass();
                                if (type == null) {
                                    error("The type of the callback not supported: " + param2Type.getJNISignature());
                                }
                                debug("match: "+type);
                                return type;
                            }
                        }
                    }
                }
                error("The type of the callback could not be determined: " + callbackType.getParameterizedQualifiedSourceName());
                return null;
                
            }
        });
    }

    private String getRestMethod(JMethod method) throws UnableToCompleteException {
        String restMethod = null;
        if (method.getAnnotation(DELETE.class) != null) {
            restMethod = METHOD_DELETE;
        } else if (method.getAnnotation(GET.class) != null) {
            restMethod = METHOD_GET;
        } else if (method.getAnnotation(HEAD.class) != null) {
            restMethod = METHOD_HEAD;
        } else if (method.getAnnotation(OPTIONS.class) != null) {
            restMethod = METHOD_OPTIONS;
        } else if (method.getAnnotation(POST.class) != null) {
            restMethod = METHOD_POST;
        } else if (method.getAnnotation(PUT.class) != null) {
            restMethod = METHOD_PUT;
        } else {
            restMethod = method.getName();
            if (!REST_METHODS.contains(restMethod)) {
                error("Invalid rest method. It must either have a lower case rest method name or have a javax rs method annotation: " + method.getReadableDeclaration());
            }
        }
        return restMethod;
    }

    private String createJsonEncoder(JClassType jsonEncoder) throws UnableToCompleteException {
        JsonEncoderDecoderClassCreator generator = new JsonEncoderDecoderClassCreator(logger, context, jsonEncoder);
        return generator.create();
    }

}