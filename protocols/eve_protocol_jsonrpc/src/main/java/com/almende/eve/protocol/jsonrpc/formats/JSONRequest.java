/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.protocol.jsonrpc.formats;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.util.AnnotationUtil;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.almende.util.AnnotationUtil.AnnotatedParam;
import com.almende.util.AnnotationUtil.CachedAnnotation;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class JSONRequest.
 */
public final class JSONRequest extends JSONMessage {
	private static final Logger			LOG					= Logger.getLogger(JSONRequest.class
																	.getCanonicalName());
	private static final long			serialVersionUID	= 1970046457233622444L;
	private static final ObjectReader	READER				= JOM.getInstance()
																	.reader(JSONRequest.class);
	private static final ObjectNode		OBJECT				= JOM.getInstance()
																	.createObjectNode();
	transient private AsyncCallback<?>	callback			= null;

	private String						method				= null;
	private ObjectNode					params				= null;

	/**
	 * Instantiates a new jSON request.
	 */
	public JSONRequest() {
		init(null, null, null, null);
	}

	/**
	 * Instantiates a new JSON request.
	 * 
	 * @param json
	 *            the json
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public JSONRequest(final String json) throws IOException {
		init(JOM.getInstance().readTree(json));
	}

	/**
	 * Instantiates a new JSON request.
	 * 
	 * @param request
	 *            the request
	 */
	public JSONRequest(final JsonNode request) {
		init(request);
	}

	/**
	 * Instantiates a new JSON request.
	 *
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param callback
	 *            the callback
	 */
	public <T> JSONRequest(final String method, final ObjectNode params,
			final AsyncCallback<T> callback) {
		init(null, method, params, callback);
	}

	/**
	 * Instantiates a new JSON request.
	 *
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 */
	public JSONRequest(final String method, final ObjectNode params) {
		init(null, method, params, null);
	}

	/**
	 * Instantiates a new jSON request.
	 *
	 * @param id
	 *            the id
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param callback
	 *            the callback
	 */
	public <T> JSONRequest(final JsonNode id, final String method,
			final ObjectNode params, final AsyncCallback<T> callback) {
		init(id, method, params, callback);
	}

	/**
	 * Create a JSONRequest from a java method and arguments.
	 *
	 * @param method
	 *            the method
	 * @param args
	 *            the args
	 * @param callback
	 *            the callback
	 */
	public <T> JSONRequest(final Method method, final Object[] args,
			final AsyncCallback<T> callback) {
		AnnotatedMethod annotatedMethod = null;
		try {
			annotatedMethod = new AnnotationUtil.AnnotatedMethod(method);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Method can't be used as annotated method",
					e);
			throw new IllegalArgumentException("Method '" + method.getName()
					+ "' can't be used as annotated method.", e);
		}
		final List<AnnotatedParam> annotatedParams = annotatedMethod
				.getParams();

		final ObjectNode params = JOM.createObjectNode();

		for (int i = 0; i < annotatedParams.size(); i++) {
			final AnnotatedParam annotatedParam = annotatedParams.get(i);
			if (i < args.length && args[i] != null) {
				final CachedAnnotation nameAnnotation = annotatedParam
						.getAnnotation(Name.class);
				if (nameAnnotation != null && nameAnnotation.value() != null) {
					final String name = (String) nameAnnotation.value();
					final JsonNode paramValue = JOM.getInstance().valueToTree(
							args[i]);
					params.set(name, paramValue);
				} else {
					throw new IllegalArgumentException("Parameter " + i
							+ " in method '" + method.getName()
							+ "' is missing the @Name annotation.");
				}
			} else if (isRequired(annotatedParam)) {
				throw new IllegalArgumentException("Required parameter " + i
						+ " in method '" + method.getName() + "' is null.");
			}
		}
		if (callback != null) {
			final JsonNode id = OBJECT.textNode(new UUID().toString());
			init(id, method.getName(), params, callback);
		} else {
			init(null, method.getName(), params, null);
		}
	}

	/**
	 * Test if a parameter is required Reads the parameter annotation @Required.
	 * Returns True if the annotation is not provided.
	 * 
	 * @param param
	 *            the param
	 * @return required
	 */
	@SuppressWarnings("deprecation")
	static boolean isRequired(final AnnotatedParam param) {
		boolean required = true;
		final CachedAnnotation requiredAnnotation = param
				.getAnnotation(com.almende.eve.protocol.jsonrpc.annotation.Required.class);
		if (requiredAnnotation != null) {
			required = (boolean) requiredAnnotation.value();
		}
		if (param.getAnnotation(Optional.class) != null) {
			required = false;
		}
		return required;
	}

	/**
	 * Inits the.
	 * 
	 * @param request
	 *            the request
	 */
	public void init(final JsonNode request) {
		super.init(request);
		try {
			READER.withValueToUpdate(this).readValue(request);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't parse incoming request", e);
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					e.getLocalizedMessage(), e);
		}
		if (getMethod() == null) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Member 'method' missing in request");
		}
	}

	/**
	 * Inits the.
	 * 
	 * @param id
	 *            the id
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 */
	private <T> void init(final JsonNode id, final String method,
			final ObjectNode params, final AsyncCallback<T> callback) {
		if (callback != null && (id == null || id.isNull())) {
			setId(OBJECT.textNode(new UUID().toString()));
		} else {
			setId(id);
		}
		setMethod(method);
		setParams(params);
		setCallback(callback);
	}

	/**
	 * Sets the method.
	 * 
	 * @param method
	 *            the new method
	 */
	public void setMethod(final String method) {
		this.method = method;
	}

	/**
	 * Gets the method.
	 * 
	 * @return the method
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * Sets the params.
	 * 
	 * @param params
	 *            the new params
	 */
	public void setParams(final ObjectNode params) {
		this.params = params;
	}

	/**
	 * Gets the params.
	 * 
	 * @return the params
	 */
	public ObjectNode getParams() {
		if (this.params == null) {
			return JOM.createObjectNode();
		}
		return this.params;
	}

	/**
	 * Put param.
	 * 
	 * @param name
	 *            the name
	 * @param value
	 *            the value
	 */
	public void putParam(final String name, final Object value) {
		this.params.set(name, OBJECT.pojoNode(value));
	}

	/**
	 * Gets the param.
	 * 
	 * @param name
	 *            the name
	 * @return the param
	 */
	public Object getParam(final String name) {
		if (params.has(name)) {
			return JOM.getInstance().convertValue(params.get(name),
					Object.class);
		}
		return null;
	}

	/**
	 * Checks for param.
	 * 
	 * @param name
	 *            the name
	 * @return the object
	 */
	public Object hasParam(final String name) {
		return this.params.has(name);
	}

	/**
	 * Gets the callback.
	 *
	 * @return the callback
	 */
	@JsonIgnore
	public AsyncCallback<?> getCallback() {
		return callback;
	}

	/**
	 * Sets the callback.
	 *
	 * @param callback
	 *            the new callback
	 */
	@JsonIgnore
	public <T> void setCallback(AsyncCallback<T> callback) {
		this.callback = callback;
	}

	@Override
	@JsonIgnore
	public boolean isRequest() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final ObjectMapper mapper = JOM.getInstance();
		try {
			ObjectNode tree = mapper.valueToTree(this);
			if (tree.get(ID) == null || tree.get(ID).isNull()) {
				tree.remove(ID);
			}
			if (tree.get(EXTRA) == null || tree.get(EXTRA).isNull()) {
				tree.remove(EXTRA);
			}
			return tree.toString();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return null;
	}
}