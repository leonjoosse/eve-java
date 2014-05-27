/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.almende.eve.capabilities.handler.Handler;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Router, outbound transport selection based on protocol scheme.
 * 
 */
public class Router implements Transport {
	
	private final Map<String, Transport>	transports	= new HashMap<String, Transport>();
	
	/**
	 * Register new transport. If a given protocol is already known, this will
	 * overwrite the reference.
	 * 
	 * @param transport
	 *            the transport
	 */
	public void register(final Transport transport) {
		for (final String protocol : transport.getProtocols()) {
			transports.put(protocol, transport);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#send(java.net.URI,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void send(final URI receiverUri, final String message,
			final String tag) throws IOException {
		final Transport transport = transports.get(receiverUri.getScheme()
				.toLowerCase());
		if (transport != null) {
			transport.send(receiverUri, message, tag);
		} else {
			throw new IOException("No transport known for scheme:"
					+ receiverUri.getScheme());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#send(java.net.URI, byte[],
	 * java.lang.String)
	 */
	@Override
	public void send(final URI receiverUri, final byte[] message,
			final String tag) throws IOException {
		final Transport transport = transports.get(receiverUri.getScheme()
				.toLowerCase());
		if (transport != null) {
			transport.send(receiverUri, message, tag);
		} else {
			throw new IOException("No transport known for scheme:"
					+ receiverUri.getScheme());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#connect()
	 */
	@Override
	public void connect() throws IOException {
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#disconnect()
	 */
	@Override
	public void disconnect() {
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#delete()
	 */
	@Override
	public void delete() {
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#getHandle()
	 */
	@Override
	public Handler<Receiver> getHandle() {
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#getAddress()
	 */
	@Override
	public URI getAddress() {
		return null;
	}
	
	/**
	 * Gets the addresses.
	 * 
	 * @return the addresses
	 */
	public List<URI> getAddresses() {
		final Set<URI> result = new HashSet<URI>(transports.size());
		for (final Transport transport : transports.values()) {
			result.add(transport.getAddress());
		}
		return new ArrayList<URI>(result);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.Transport#getProtocols()
	 */
	@Override
	public List<String> getProtocols() {
		return new ArrayList<String>(transports.keySet());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.capabilities.Capability#getParams()
	 */
	@Override
	public ObjectNode getParams() {
		final ArrayNode transportConfs = JOM.createArrayNode();
		for (final Transport transport : transports.values()) {
			transportConfs.add(transport.getParams());
		}
		final ObjectNode result = JOM.createObjectNode();
		result.put("transports", transportConfs);
		return result;
	}
	
}
