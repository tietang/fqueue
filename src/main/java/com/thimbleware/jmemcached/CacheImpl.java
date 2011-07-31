/**
 *  Copyright 2008 ThimbleWare Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.thimbleware.jmemcached;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.thimbleware.jmemcached.protocol.exceptions.DatabaseException;
import com.thimbleware.jmemcached.storage.CacheStorage;

/**
 * Default implementation of the cache handler, supporting local memory cache
 * elements.
 */
public final class CacheImpl extends AbstractCache<LocalCacheElement> implements Cache<LocalCacheElement> {

	final CacheStorage<String, LocalCacheElement> storage;
	final DelayQueue<DelayedMCElement> deleteQueue;
	final boolean isReadOnly = false;
	final boolean forbiddenflush = false;
	// record lock
	private final int lockNum = 100;
	private Lock[] writeLocks = new Lock[lockNum];
	private Lock[] readLocks = new Lock[lockNum];

	/**
	 * @inheritDoc
	 */
	public CacheImpl(CacheStorage<String, LocalCacheElement> storage) {
		super();
		this.storage = storage;
		deleteQueue = new DelayQueue<DelayedMCElement>();
		for (int i = 0; i < lockNum; i++) {
			ReadWriteLock lock = new ReentrantReadWriteLock();
			writeLocks[i] = lock.writeLock();
			readLocks[i] = lock.readLock();
		}
	}

	private void checkWritePermission() throws Exception {
		if (isReadOnly) {
			throw new Exception("slave write is forbidden");
		}
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public DeleteResponse delete(String key, int time) throws DatabaseException, Exception {
		checkWritePermission();
		boolean removed = false;
		// delayed remove
		if (time != 0) {
			// block the element and schedule a delete; replace its entry with a
			// blocked element
			LocalCacheElement placeHolder = new LocalCacheElement(key, 0, 0, 0L);
			placeHolder.setData(new byte[] {});
			placeHolder.block(Now() + (long) time);

			storage.replace(key, placeHolder);

			// this must go on a queue for processing later...
			deleteQueue.add(new DelayedMCElement(placeHolder));
		} else
			removed = storage.remove(key) != null;

		if (removed)
			return DeleteResponse.DELETED;
		else
			return DeleteResponse.NOT_FOUND;

	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public StoreResponse add(LocalCacheElement e) throws DatabaseException, Exception {
		checkWritePermission();
		return storage.putIfAbsent(e.getKeystring(), e) == null ? StoreResponse.STORED : StoreResponse.NOT_STORED;
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public StoreResponse replace(LocalCacheElement e) throws DatabaseException, Exception {
		checkWritePermission();
		return storage.replace(e.getKeystring(), e) != null ? StoreResponse.STORED : StoreResponse.NOT_STORED;
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public StoreResponse append(LocalCacheElement element) throws DatabaseException, Exception {
		checkWritePermission();
		int index = hash(element.getKeystring().hashCode()) & (lockNum - 1);
		writeLocks[index].lock();
		try {
			LocalCacheElement old = storage.get(element.getKeystring());
			if (old == null || isBlocked(old) || isExpired(old)) {
				getMisses.incrementAndGet();
				return StoreResponse.NOT_FOUND;
			} else {
				return storage.replace(old.getKeystring(), old, old.append(element)) ? StoreResponse.STORED : StoreResponse.NOT_STORED;
			}

		} finally {
			writeLocks[index].unlock();
		}
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public StoreResponse prepend(LocalCacheElement element) throws DatabaseException, Exception {
		checkWritePermission();
		int index = hash(element.getKeystring().hashCode()) & (lockNum - 1);
		writeLocks[index].lock();
		try {
			LocalCacheElement old = storage.get(element.getKeystring());
			if (old == null || isBlocked(old) || isExpired(old)) {
				getMisses.incrementAndGet();
				return StoreResponse.NOT_FOUND;
			} else {
				return storage.replace(old.getKeystring(), old, old.prepend(element)) ? StoreResponse.STORED : StoreResponse.NOT_STORED;
			}
		} finally {
			writeLocks[index].unlock();
		}
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public StoreResponse set(LocalCacheElement e) throws DatabaseException, Exception {
		checkWritePermission();
		setCmds.incrementAndGet();// update stats

		e.setCasUnique(casCounter.getAndIncrement());

		storage.put(e.getKeystring(), e);

		return StoreResponse.STORED;
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public StoreResponse cas(Long cas_key, LocalCacheElement e) throws DatabaseException, Exception {
		checkWritePermission();
		int index = hash(e.getKeystring().hashCode()) & (lockNum - 1);
		writeLocks[index].lock();
		try {
			// have to get the element
			LocalCacheElement element = storage.get(e.getKeystring());
			if (element == null || isBlocked(element)) {
				getMisses.incrementAndGet();
				return StoreResponse.NOT_FOUND;
			}

			if (element.getCasUnique() == cas_key) {
				// casUnique matches, now set the element
				if (storage.replace(e.getKeystring(), element, e))
					return StoreResponse.STORED;
				else {
					getMisses.incrementAndGet();
					return StoreResponse.NOT_FOUND;
				}
			} else {
				// cas didn't match; someone else beat us to it
				return StoreResponse.EXISTS;
			}
		} finally {
			writeLocks[index].unlock();
		}
	}

	static int hash(int h) {
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public Integer get_add(String key, int mod) throws DatabaseException, Exception {
		checkWritePermission();
		int index = hash(key.hashCode()) & (lockNum - 1);
		writeLocks[index].lock();
		try {
			LocalCacheElement old = storage.get(key);
			if (old == null || isBlocked(old) || isExpired(old)) {
				getMisses.incrementAndGet();
				return null;
			} else {
				LocalCacheElement.IncrDecrResult result = old.add(mod);
				return storage.replace(old.getKeystring(), old, result.replace) ? result.oldValue : null;
			}
		} finally {
			writeLocks[index].unlock();
		}
	}

	protected boolean isBlocked(CacheElement e) {
		return e.isBlocked() && e.getBlockedUntil() > Now();
	}

	protected boolean isExpired(CacheElement e) {
		return e.getExpire() != 0 && e.getExpire() < Now();
	}

	/**
	 * @inheritDoc
	 */
	public LocalCacheElement[] get(String... keys) {
		getCmds.incrementAndGet();// updates stats

		LocalCacheElement[] elements = new LocalCacheElement[keys.length];
		int x = 0;
		int hits = 0;
		int misses = 0;
		for (String key : keys) {
			LocalCacheElement e = storage.get(key);
			if (e == null || isExpired(e) || e.isBlocked()) {
				misses++;

				elements[x] = null;
			} else {
				hits++;

				elements[x] = e;
			}
			x++;

		}
		getMisses.addAndGet(misses);
		getHits.addAndGet(hits);

		return elements;

	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public boolean flush_all() throws DatabaseException, Exception {
		checkWritePermission();
		if (!forbiddenflush) {
			return flush_all(0);
		} else {
			return false;
		}
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	public boolean flush_all(int expire) throws DatabaseException, Exception {
		checkWritePermission();
		// TODO implement this, it isn't right... but how to handle efficiently?
		// (don't want to linear scan entire cacheStorage)
		if (!forbiddenflush) {
			storage.clear();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @inheritDoc
	 */
	public void close() throws IOException {
		storage.close();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Set<String> keys() {
		return storage.keySet();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public long getCurrentItems() {
		return storage.size();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public long getLimitMaxBytes() {
		return storage.getMemoryCapacity();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public long getCurrentBytes() {
		return storage.getMemoryUsed();
	}

	/**
	 * @throws Exception
	 * @throws DatabaseException
	 * @inheritDoc
	 */
	@Override
	public void asyncEventPing() throws DatabaseException, Exception {
		DelayedMCElement toDelete = deleteQueue.poll();
		if (toDelete != null) {
			storage.remove(toDelete.element.getKeystring());
		}
	}

	/**
	 * Delayed key blocks get processed occasionally.
	 */
	protected static class DelayedMCElement implements Delayed {
		private CacheElement element;

		public DelayedMCElement(CacheElement element) {
			this.element = element;
		}

		public long getDelay(TimeUnit timeUnit) {
			return timeUnit.convert(element.getBlockedUntil() - Now(), TimeUnit.MILLISECONDS);
		}

		public int compareTo(Delayed delayed) {
			if (!(delayed instanceof CacheImpl.DelayedMCElement))
				return -1;
			else
				return element.getKeystring().compareTo(((DelayedMCElement) delayed).element.getKeystring());
		}
	}
}
