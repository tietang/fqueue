package com.thimbleware.jmemcached.storage;


import java.io.IOException;
import java.util.Set;

import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.protocol.exceptions.DatabaseException;
import com.thimbleware.jmemcached.storage.hash.SizedItem;

/**
 * The interface for cache storage. Essentially a concurrent map but with
 * methods for investigating the heap state of the storage unit and with
 * additional support for explicit resource-cleanup (close()).
 */
public interface CacheStorage<K, V extends SizedItem> {
	/**
	 * @return the capacity (in bytes) of the storage
	 */
	long getMemoryCapacity();

	/**
	 * @return the current usage (in bytes) of the storage
	 */
	long getMemoryUsed();

	/**
	 * @return the capacity (in # of items) of the storage
	 */
	int capacity();

	/**
	 * Close the storage unit, deallocating any resources it might be currently
	 * holding.
	 * 
	 * @throws java.io.IOException
	 *             thrown if IO faults occur anywhere during close.
	 */
	void close() throws IOException;

	void clear() throws DatabaseException, Exception;

	LocalCacheElement remove(String key) throws DatabaseException, Exception;

	LocalCacheElement putIfAbsent(String keystring, LocalCacheElement e)
			throws DatabaseException, Exception;

	LocalCacheElement get(String keystring);

	boolean replace(String keystring, LocalCacheElement old,
			LocalCacheElement prepend) throws DatabaseException, Exception;

	LocalCacheElement put(String keystring, LocalCacheElement e)
			throws DatabaseException, Exception;

	long size();

	Set<String> keySet();

	LocalCacheElement replace(String key, LocalCacheElement placeHolder)
			throws DatabaseException, Exception;
}
