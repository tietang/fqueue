package com.thimbleware.jmemcached.storage.bytebuffer;

/**
     * Represents a number of allocated blocks in the store
 */
public final class Region {
    /**
     * Size in bytes of the requested area
     */
    public final int size;

    /**
     * Actual size the data rounded up to the nearest block.
     */
    public final long physicalSize;

    /**
     * Offset into the memory region
     */
    final long offset;

    /**
     * Flag which is true if the region is valid and in use.
     * Set to false on free()
     */
    public boolean valid = false;

    public Region(int size, long physicalSize, long offset) {
        this.size = size;
        this.physicalSize = physicalSize;
        this.offset = offset;
        this.valid = true;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Region)) return false;

        Region region = (Region) o;

        if (physicalSize != region.physicalSize) return false;
        if (offset != region.offset) return false;
        if (size != region.size) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + (int) (physicalSize ^ (physicalSize >>> 32));
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        return result;
    }
}
