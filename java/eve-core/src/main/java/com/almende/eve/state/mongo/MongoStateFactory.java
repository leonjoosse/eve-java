package com.almende.eve.state.mongo;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.jongo.Find;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;
import com.mongodb.MongoClient;

/**
 * An implementation of state factory using MongoDB & Jongo as database connection
 * 
 * @author ronny
 *
 */
public class MongoStateFactory implements StateFactory {
	
	public static final String COLLECTION_NAME = "agents";
	
	private static final Logger log = LoggerFactory.getLogger(MongoStateFactory.class);
	
	private final Jongo jongo;
	
	/**
	 * default constructor which will connect to default mongodb client
	 * (localhost:27017) with "eve" as its default database
	 * 
	 * @throws UnknownHostException 
	 */
	public MongoStateFactory() throws UnknownHostException {
		this(new MongoClient(), "eve");
	}
	
	/**
	 * constructor with the URI & database name as its parameter
	 * 
	 * @param mongoUriHost
	 * @param mongoPort
	 * @param databaseName
	 * @throws UnknownHostException
	 */
	public MongoStateFactory(String mongoUriHost, int mongoPort, String databaseName) throws UnknownHostException {
		this(new MongoClient(mongoUriHost, mongoPort), databaseName);
	}
	
	/**
	 * constructor which uses readily available mongo client instance and database name
	 * @param mongoClient
	 * @param databaseName
	 */
	public MongoStateFactory(MongoClient mongoClient, String databaseName) {
		this(new Jongo(mongoClient.getDB(databaseName)));
	}
	
	/**
	 * constructor which uses jongo instantiated elsewhere
	 * @param jongo
	 */
	public MongoStateFactory(Jongo jongo) {
		this.jongo = jongo;
	}
	
	/**
	 * returns jongo connection to the underlying mongo database
	 * @return jongo
	 */
	public Jongo getJongo() {
		return jongo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#get(java.lang.String)
	 */
	@Override
	public State get(String agentId) {
		MongoState result = null;
		try {
			result = jongo.getCollection(COLLECTION_NAME).
							findOne("{_id: #}", agentId).as(MongoState.class);
			if (result!=null) {
				result.setConnection(jongo);
			}
		} catch (final Exception e) {
			log.warn("get error:"+e.getMessage());
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#create(java.lang.String)
	 */
	@Override
	public synchronized State create(String agentId) throws IOException {
		if (exists(agentId)) {
			throw new IllegalStateException("Cannot create state, "
					+ "state with id '" + agentId + "' already exists.");
		}
		
		MongoState state = new MongoState(agentId);
		try {
			jongo.getCollection(COLLECTION_NAME).insert(state);
		} catch (final Exception e) {
			log.warn("create error:"+e.getMessage());
		}
		state.setConnection(jongo);
		return state;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#delete(java.lang.String)
	 */
	@Override
	public void delete(String agentId) {
		try {
			jongo.getCollection(COLLECTION_NAME).remove("{_id: #}", agentId);
		} catch (final Exception e) {
			log.warn("delete error : "+e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#exists(java.lang.String)
	 */
	@Override
	public boolean exists(String agentId) {
		MongoState result = jongo.getCollection(COLLECTION_NAME).
								findOne("{_id: #}", agentId).as(MongoState.class);
		return (result != null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#getAllAgentIds(java.lang.String)
	 */
	@Override
	public Iterator<String> getAllAgentIds() {
		List<String> agentIDs = new ArrayList<String>();
		try {
			Find find = jongo.getCollection(COLLECTION_NAME).find().projection("{_id:1}");
			// :: there's probably a faster way to iterate over id fields
	        for (Object map : find.as(Object.class)) {
				String agentId = (String) ((LinkedHashMap) map).get("_id");
	        	agentIDs.add(agentId);
	        }
		} catch (final Exception e) {
			log.warn("getAllAgentIds error : "+e.getMessage());
		}
		return agentIDs.iterator();
	}


}
