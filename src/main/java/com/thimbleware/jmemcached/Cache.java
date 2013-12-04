package com.thimbleware.jmemcached;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.channel.ChannelHandlerContext;

import com.thimbleware.jmemcached.protocol.exceptions.DatabaseException;


/**
 */
public interface Cache<CACHE_ELEMENT extends CacheElement> {
	/**
	 * Enum defining response statuses from set/add type commands
	 */
	public enum StoreResponse {
		STORED, NOT_STORED, EXISTS, NOT_FOUND
	}

	/**
	 * Enum defining responses statuses from removal commands
	 */
	public enum DeleteResponse {
		DELETED, NOT_FOUND
	}

	/**
	 * Handle the deletion of an item from the cache.
	 * 
	 * @param key
	 *            the key for the item
	 * @param time
	 *            an amount of time to block this entry in the cache for further
	 *            writes
	 * @return the message response
	 */
	DeleteResponse delete(String key, int time) throws DatabaseException, Exception;

	/**
	 * Add an element to the cache
	 * 
	 * @param e
	 *            the element to add
	 * @return the store response code
	 */
	StoreResponse add(CACHE_ELEMENT e) throws DatabaseException, Exception;

	/**
	 * Replace an element in the cache
	 * 
	 * @param e
	 *            the element to replace
	 * @return the store response code
	 */
	StoreResponse replace(CACHE_ELEMENT e) throws DatabaseException, Exception;

	/**
	 * Append bytes to the end of an element in the cache
	 * 
	 * @param element
	 *            the element to append
	 * @return the store response code
	 */
	StoreResponse append(CACHE_ELEMENT element) throws DatabaseException, Exception;

	/**
	 * Prepend bytes to the end of an element in the cache
	 * 
	 * @param element
	 *            the element to append
	 * @return the store response code
	 */
	StoreResponse prepend(CACHE_ELEMENT element) throws DatabaseException, Exception;

	/**
	 * Set an element in the cache
	 * 
	 * @param e
	 *            the element to set
	 * @return the store response code
	 */
	StoreResponse set(CACHE_ELEMENT e) throws DatabaseException, Exception;

	/**
	 * Set an element in the cache but only if the element has not been touched
	 * since the last 'gets'
	 * 
	 * @param cas_key
	 *            the cas key returned by the last gets
	 * @param e
	 *            the element to set
	 * @return the store response code
	 */
	StoreResponse cas(Long cas_key, CACHE_ELEMENT e) throws DatabaseException, Exception;

	/**
	 * Increment/decremen t an (integer) element in the cache
	 * 
	 * @param key
	 *            the key to increment
	 * @param mod
	 *            the amount to add to the value
	 * @return the message response
	 */
	Integer get_add(String key, int mod) throws DatabaseException, Exception;

	/**
	 * Get element(s) from the cache
	 * 
	 * @param keys
	 *            the key for the element to lookup
	 * @return the element, or 'null' in case of cache miss.
	 */
	CACHE_ELEMENT[] get(String... keys);

	/**
	 * Flush all cache entries
	 * 
	 * @return command response
	 */
	boolean flush_all() throws DatabaseException, Exception;

	/**
	 * Flush all cache entries with a timestamp after a given expiration time
	 * 
	 * @param expire
	 *            the flush time in seconds
	 * @return command response
	 */
	boolean flush_all(int expire) throws DatabaseException, Exception;

	/**
	 * Close the cache, freeing all resources on which it depends.
	 * 
	 * @throws IOException
	 */
	void close() throws IOException;

	/**
	 * @return all keys currently held in the cache
	 */
	Set<String> keys();

	/**
	 * @return the # of items in the cache
	 */
	long getCurrentItems();

	/**
	 * @return the maximum size of the cache (in bytes)
	 */
	long getLimitMaxBytes();

	/**
	 * @return the current cache usage (in bytes)
	 */
	long getCurrentBytes();

	/**
	 * @return the number of get commands executed
	 */
	long getGetCmds();

	/**
	 * @return the number of set commands executed
	 */
	long getSetCmds();

	/**
	 * @return the number of get hits
	 */
	long getGetHits();

	/**
	 * @return the number of stats
	 */
	long getGetMisses();

	/**
	 * Retrieve stats about the cache. If an argument is specified, a specific
	 * category of stats is requested.
	 * 
	 * @param arg
	 *            a specific extended stat sub-category
	 * @return a map of stats
	 */
	Map<String, Set<String>> stat(String arg, ChannelHandlerContext channelHandlerContext);

	/**
	 * Called periodically by the network event loop to process any pending
	 * events. (such as delete queues, etc.)
	 */
	void asyncEventPing() throws DatabaseException, Exception;

}
