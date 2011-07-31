package com.thimbleware.jmemcached.storage.bytebuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Memory mapped block storage mechanism with a free-list maintained by TreeMap
 *
 * Allows memory for storage to be mapped outside of the VM's main memory, and outside the purvey
 * of the GC.
 *
 * Should offer O(Log(N)) search and free of blocks.
 */
public class ByteBufferBlockStore {

    protected ByteBuffer storageBuffer;

    private long freeBytes;

    private long storeSizeBytes;
    private int blockSizeBytes;

    private SortedMap<Long, Long> freeList;
    private Set<Region> currentRegions;

    /**
     * Exception thrown on inability to allocate a new block
     */
    public static class BadAllocationException extends RuntimeException {
        public BadAllocationException(String s) {
            super(s);
        }
    }

    /**
     * Construct a new memory mapped block storage against a filename, with a certain size
     * and block size.
     * @param storageBuffer
     * @param blockSizeBytes the size of a block in the store
     * @throws java.io.IOException thrown on failure to open the store or map the file
     */
    public ByteBufferBlockStore(ByteBuffer storageBuffer, int blockSizeBytes) throws IOException {
        this.storageBuffer = storageBuffer;
        this.blockSizeBytes = blockSizeBytes;
        initialize(storageBuffer.capacity(), blockSizeBytes);
    }

    /**
     * Constructor used only be subclasses, allowing them to provide their own buffer.
     */
    protected ByteBufferBlockStore() {
    }

    protected void initialize(int storeSizeBytes, int blockSizeBytes) {
        // set region size used throughout
        this.blockSizeBytes = blockSizeBytes;

        // set the size of the store in bytes
        this.storeSizeBytes = storageBuffer.capacity();

        // the number of free bytes starts out as the entire store
        freeBytes = storeSizeBytes;

        // clear the buffer
        storageBuffer.clear();

        // the free list starts with one giant free region; we create a tree map as index, then set initial values
        // with one empty region.
        freeList = new TreeMap<Long, Long>();
        currentRegions = new HashSet<Region>();

        clear();
    }


    /**
     * Rounds up a requested size to the nearest block width.
     * @param size the requested size
     * @param blockSize the block size to use
     * @return the actual mount to use
     */
    public static long roundUp( long size, long blockSize ) {
        return size - 1L + blockSize - (size - 1L) % blockSize;
    }

    /**
     * Close the store, destroying all data and closing the backing file
     * @throws java.io.IOException thrown on failure to close file
     */
    public void close() throws IOException {
        // clear the region list
        clear();

        //
        freeResources();

        // null out the storage to allow the GC to get rid of it
        storageBuffer = null;
    }

    protected void freeResources() throws IOException {
        // noop
    }

    /**
     * Allocate a region in the block storage
     * @param desiredSize size (in bytes) desired for the region
     * @param data initial data to place in it
     * @return the region descriptor
     */
    public Region alloc(int desiredSize, byte[] data) {
        final long desiredBlockSize = roundUp(desiredSize, blockSizeBytes);

        /** Find a free entry.  headMap should give us O(log(N)) search
         *  time.
         */
        Iterator<Map.Entry<Long,Long>> entryIterator = freeList.tailMap(desiredBlockSize).entrySet().iterator();

        if (!entryIterator.hasNext()) {
            /** No more room.
             *  We now have three options - we can throw error, grow the store, or compact
             *  the holes in the existing one.
             *
             *  We usually want to grow (fast), and compact (slow) only if necessary
             *  (i.e. some periodic interval
             *  has been reached or a maximum store size constant hit.)
             *
             *  Incremental compaction is a Nice To Have.
             *
             *  TODO implement compaction routine; throw; in theory the cache on top of us should be removing elements before we fill
             */
            //    compact();

            throw new BadAllocationException("unable to allocate room; all blocks consumed");
        } else {
            Map.Entry<Long, Long> freeEntry = entryIterator.next();

            /** Don't let this region overlap a page boundary
             */
            // PUT CODE HERE

            long position = freeEntry.getValue();

            /** Size of the free block to be placed back on the free list
             */
            long newFreeSize = freeEntry.getKey() - desiredBlockSize;

            /** Split the free entry and add the entry to the allocated list
             */
            freeList.remove(freeEntry.getKey());

            /** Add it back to the free list if needed
             */
            if ( newFreeSize > 0L ) {
                freeList.put( newFreeSize, position + desiredBlockSize) ;
            }

            freeBytes -= desiredBlockSize;

            // get the buffer to it
            storageBuffer.rewind();
            storageBuffer.position((int)position);
            storageBuffer.put(data, 0, desiredSize);

            Region region = new Region(desiredSize, desiredBlockSize, position);

            currentRegions.add(region);

            return region;
        }
    }

    public byte[] get(Region region) {
        byte[] result = new byte[region.size];
        storageBuffer.position((int)region.offset);
        storageBuffer.get(result, 0, region.size);
        return result;
    }

    public void free(Region region) {
        freeList.put(region.physicalSize, region.offset);
        freeBytes += region.physicalSize;
        region.valid = false;
        currentRegions.remove(region);
    }

    public void clear()
    {
        // mark all blocks invalid
        clearRegions();

        // say goodbye to the region list
        freeList.clear();

        // Add a free entry for the entire thing at the start
        freeList.put(storeSizeBytes, 0L);

        // reset the # of free bytes back to the max size
        freeBytes = storeSizeBytes;
    }

    private void clearRegions() {
        // mark all blocks invalid
        for (Region currentRegion : currentRegions) {
            currentRegion.setValid(false);
        }

        // clear the list of blocks so we're not holding onto them
        currentRegions.clear();
    }

    public long getStoreSizeBytes() {
        return storeSizeBytes;
    }

    public int getBlockSizeBytes() {
        return blockSizeBytes;
    }

    public long getFreeBytes() {
        return freeBytes;
    }


}