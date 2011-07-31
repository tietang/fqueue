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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;

/**
 * Represents information about a cache entry.
 */
public final class LocalCacheElement implements CacheElement, Externalizable {
    private int expire ;
    private int flags;
    private byte[] data;
    private String keystring;
    private long casUnique = 0L;
    private boolean blocked = false;
    private long blockedUntil;

    public LocalCacheElement() {
    }

    public LocalCacheElement(String keystring) {
        this.keystring = keystring;
    }

    public LocalCacheElement(String keystring, int flags, int expire, long casUnique) {
        this.keystring = keystring;
        this.flags = flags;
        this.expire = expire;
        this.casUnique = casUnique;
    }

    /**
     * @return the current time in seconds
     */
    public static int Now() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public int size() {
        return getData().length;
    }

    public LocalCacheElement append(LocalCacheElement element) {
        int newLength = getData().length + element.getData().length;
        LocalCacheElement replace = new LocalCacheElement(getKeystring(), getFlags(), getExpire(), 0L);
        ByteBuffer b = ByteBuffer.allocateDirect(newLength);
        b.put(getData());
        b.put(element.getData());
        replace.setData(new byte[newLength]);
        b.flip();
        b.get(replace.getData());
        replace.setCasUnique(replace.getCasUnique() + 1);

        return replace;
    }

    public LocalCacheElement prepend(LocalCacheElement element) {
        int newLength = getData().length + element.getData().length;

        LocalCacheElement replace = new LocalCacheElement(getKeystring(), getFlags(), getExpire(), 0L);
        ByteBuffer b = ByteBuffer.allocateDirect(newLength);
        b.put(element.getData());
        b.put(getData());
        replace.setData(new byte[newLength]);
        b.flip();
        b.get(replace.getData());
        replace.setCasUnique(replace.getCasUnique() + 1);

        return replace;
    }

    public static class IncrDecrResult {
        int oldValue;
        LocalCacheElement replace;

        public IncrDecrResult(int oldValue, LocalCacheElement replace) {
            this.oldValue = oldValue;
            this.replace = replace;
        }
    }

    public IncrDecrResult add(int mod) {
        // TODO handle parse failure!
        int old_val = parseInt(new String(getData())) + mod; // change value
        if (old_val < 0) {
            old_val = 0;

        } // check for underflow

        byte[] newData = valueOf(old_val).getBytes();

        LocalCacheElement replace = new LocalCacheElement(getKeystring(), getFlags(), getExpire(), 0L);
        replace.setData(newData);
        replace.setCasUnique(replace.getCasUnique() + 1);

        return new IncrDecrResult(old_val, replace);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalCacheElement that = (LocalCacheElement) o;

        if (blocked != that.blocked) return false;
        if (blockedUntil != that.blockedUntil) return false;
        if (casUnique != that.casUnique) return false;
        if (expire != that.expire) return false;
        if (flags != that.flags) return false;
        if (!Arrays.equals(data, that.data)) return false;
        if (!keystring.equals(that.keystring)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = expire;
        result = 31 * result + flags;
        result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
        result = 31 * result + keystring.hashCode();
        result = 31 * result + (int) (casUnique ^ (casUnique >>> 32));
        result = 31 * result + (blocked ? 1 : 0);
        result = 31 * result + (int) (blockedUntil ^ (blockedUntil >>> 32));
        return result;
    }

    public static LocalCacheElement key(String key) {
        return new LocalCacheElement(key);
    }

    public int getExpire() {
        return expire;
    }

    public int getFlags() {
        return flags;
    }

    public byte[] getData() {
        return data;
    }

    public String getKeystring() {
        return keystring;
    }

    public long getCasUnique() {
        return casUnique;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public long getBlockedUntil() {
        return blockedUntil;
    }

    public void setCasUnique(long casUnique) {
        this.casUnique = casUnique;
    }

    public void block(long blockedUntil) {
        this.blocked = true;
        this.blockedUntil = blockedUntil;
    }


    public void setData(byte[] data) {
        this.data = data;
    }

    public void readExternal(ObjectInput in) throws IOException{
        expire = in.readInt() ;
        flags = in.readInt();

        final int length = in.readInt();
        int readSize = 0;
        data = new byte[length];
        while( readSize < length)
            readSize += in.read(data, readSize, length - readSize);

        keystring = in.readUTF();
        casUnique = in.readLong();
        blocked = in.readBoolean();
        blockedUntil = in.readLong();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(expire) ;
        out.writeInt(flags);
        out.writeInt(data.length);
        out.write(data);
        out.writeUTF(keystring);
        out.writeLong(casUnique);
        out.writeBoolean(blocked);
        out.writeLong(blockedUntil);
    }
}