package com.thimbleware.jmemcached;

import com.thimbleware.jmemcached.storage.hash.SizedItem;

import java.io.Serializable;

/**
 */
public interface CacheElement extends Serializable, SizedItem {
    int THIRTY_DAYS = 60 * 60 * 24 * 30;

    int size();

    int hashCode();

    int getExpire();

    int getFlags();

    byte[] getData();

    void setData(byte[] data);

    String getKeystring();

    long getCasUnique();

    void setCasUnique(long casUnique);

    boolean isBlocked();

    void block(long blockedUntil);

    long getBlockedUntil();

    LocalCacheElement append(LocalCacheElement element);

    LocalCacheElement prepend(LocalCacheElement element);

    LocalCacheElement.IncrDecrResult add(int mod);
}
