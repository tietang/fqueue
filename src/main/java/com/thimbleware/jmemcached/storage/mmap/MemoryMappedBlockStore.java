package com.thimbleware.jmemcached.storage.mmap;

import com.thimbleware.jmemcached.storage.bytebuffer.ByteBufferBlockStore;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import static java.nio.channels.FileChannel.MapMode.PRIVATE;

/**
 * Memory mapped block storage mechanism with a free-list maintained by TreeMap
 *
 * Allows memory for storage to be mapped outside of the VM's main memory, and outside the purvey
 * of the GC.
 *
 * Should offer O(Log(N)) search and free of blocks.
 */
public final class MemoryMappedBlockStore extends ByteBufferBlockStore {

    private RandomAccessFile fileStorage;
    private String fileName;

    /**
     * Construct a new memory mapped block storage against a filename, with a certain size
     * and block size.
     * @param maxBytes the number of bytes to allocate in the file
     * @param fileName the filename to use
     * @param blockSizeBytes the size of a block in the store
     * @throws java.io.IOException thrown on failure to open the store or map the file
     */
    public MemoryMappedBlockStore(long maxBytes, String fileName, int blockSizeBytes) throws IOException {
        super();
        storageBuffer = getMemoryMappedFileStorage(maxBytes, fileName);
        initialize(storageBuffer.capacity(), blockSizeBytes);
    }

    private MappedByteBuffer getMemoryMappedFileStorage(long maxBytes, String fileName) throws IOException {
        // open the file for read-write
        this.fileName = fileName;
        fileStorage = new RandomAccessFile(fileName, "rw");
        fileStorage.seek(maxBytes);
        fileStorage.getChannel().map(PRIVATE, 0, maxBytes);

        return fileStorage.getChannel().map(PRIVATE, 0, maxBytes);
    }

    @Override
    protected void freeResources() throws IOException {
        super.freeResources();

        // close the actual file
        fileStorage.close();

        // delete the file; it is no longer of any use
        new File(fileName).delete();

        fileStorage = null;
    }


}
