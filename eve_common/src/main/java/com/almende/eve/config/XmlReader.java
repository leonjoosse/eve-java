/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * The Class YamlReader.
 */
public class XmlReader {
	private static final Logger	LOG	= Logger.getLogger(XmlReader.class
											.getName());

	/**
	 * Load.
	 * 
	 * @param is
	 *            the is
	 * @return the config
	 */
	public static Config load(final InputStream is) {
		final XmlMapper mapper = new XmlMapper();
		try {
			return Config.decorate((ObjectNode) mapper.readTree(is));
		} catch (final JsonProcessingException e) {
			LOG.log(Level.WARNING, "Couldn't parse Yaml file", e);
		} catch (final IOException e) {
			LOG.log(Level.WARNING, "Couldn't read Yaml file", e);
		}
		return null;
	}
}
