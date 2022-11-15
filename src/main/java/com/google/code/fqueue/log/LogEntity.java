/*
 *  Copyright 2011 sunli [sunli1223@gmail.com][weibo.com@sunli1223]
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
package com.google.code.fqueue.log;

import com.google.code.fqueue.exception.FileEOFException;
import com.google.code.fqueue.exception.FileFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author sunli
 * @version $Id$
 * @date 2011-5-18
 */
public class LogEntity {
    private final Logger log = LoggerFactory.getLogger(LogEntity.class);
    public static final byte WRITESUCCESS = 1;
    public static final byte WRITEFAILURE = 2;
    public static final byte WRITEFULL = 3;
    public static final String MAGIC = "FQueuefs";
    public static int messageStartPosition = 20;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private File file;
    private RandomAccessFile raFile;
    private FileChannel fc;
    public MappedByteBuffer mappedByteBuffer;
    private int fileLimitLength = 1024 * 1024 * 40;

    private LogIndex db = null;
    /**
     * 文件操作位置信息
     */
    private String magicString = null;
    private int version = -1;
    private int readerPosition = -1;
    private int writerPosition = -1;
    private int nextFile = -1;
    private int endPosition = -1;
    private int currentFileNumber = -1;

    public LogEntity(String path, LogIndex db, int fileNumber,
                     int fileLimitLength) throws IOException, FileFormatException {
        this.currentFileNumber = fileNumber;
        this.fileLimitLength = fileLimitLength;
        this.db = db;
        file = new File(path);
        // 文件不存在，创建文件
        if (file.exists() == false) {
            createLogEntity();
            FileRunner.addCreateFile(Integer.toString(fileNumber + 1));
        } else {
            raFile = new RandomAccessFile(file, "rwd");
            if (raFile.length() < LogEntity.messageStartPosition) {
                throw new FileFormatException("file format error");
            }
            fc = raFile.getChannel();
            mappedByteBuffer = fc.map(MapMode.READ_WRITE, 0,
                    this.fileLimitLength);
            // magicString
            byte[] b = new byte[8];
            mappedByteBuffer.get(b);
            magicString = new String(b);
            if (magicString.equals(MAGIC) == false) {
                throw new FileFormatException("file format error");
            }
            // version
            version = mappedByteBuffer.getInt();
            // nextfile
            nextFile = mappedByteBuffer.getInt();
            endPosition = mappedByteBuffer.getInt();
            // 未写满
            if (endPosition == -1) {
                this.writerPosition = db.getWriterPosition();
            } else if (endPosition == -2) {// 预分配的文件
                this.writerPosition = LogEntity.messageStartPosition;
                db.putWriterPosition(this.writerPosition);
                mappedByteBuffer.position(16);
                mappedByteBuffer.putInt(-1);
                this.endPosition = -1;

            } else {
                this.writerPosition = endPosition;
            }
            if (db.getReaderIndex() == this.currentFileNumber) {
                this.readerPosition = db.getReaderPosition();
            } else {
                this.readerPosition = LogEntity.messageStartPosition;
            }
            fc.close();
        }
        executor.execute(new Sync());

    }

    public class Sync implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (mappedByteBuffer != null) {
                    try {
                        mappedByteBuffer.force();
                    } catch (Exception e) {
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    break;
                }
            }

        }

    }

    public int getCurrentFileNumber() {
        return this.currentFileNumber;
    }

    public int getNextFile() {
        return this.nextFile;
    }

    private boolean createLogEntity() throws IOException {
        if (file.createNewFile() == false) {
            return false;
        }
        raFile = new RandomAccessFile(file, "rwd");
        fc = raFile.getChannel();
        mappedByteBuffer = fc.map(MapMode.READ_WRITE, 0, this.fileLimitLength);
        mappedByteBuffer.put(MAGIC.getBytes());
        mappedByteBuffer.putInt(version);// 8 version
        mappedByteBuffer.putInt(nextFile);// 12next fileindex
        mappedByteBuffer.putInt(endPosition);// 16
        mappedByteBuffer.force();
        this.magicString = MAGIC;
        this.writerPosition = LogEntity.messageStartPosition;
        this.readerPosition = LogEntity.messageStartPosition;
        db.putWriterPosition(this.writerPosition);
        return true;
    }

    /**
     * 记录写位置
     *
     * @param pos
     */
    private void putWriterPosition(int pos) {
        db.putWriterPosition(pos);
    }

    private void putReaderPosition(int pos) {
        db.putReaderPosition(pos);
    }

    /**
     * write next File number id.
     *
     * @param number
     */
    public void putNextFile(int number) {
        mappedByteBuffer.position(12);
        mappedByteBuffer.putInt(number);
        this.nextFile = number;
    }

    public boolean isFull(int increment) {
        // confirm if the file is full
        if (this.fileLimitLength < this.writerPosition + increment) {
            return true;
        }
        return false;
    }

    public byte write(byte[] log) {
        int increment = log.length + 4;
        if (isFull(increment)) {
            mappedByteBuffer.position(16);
            mappedByteBuffer.putInt(this.writerPosition);
            this.endPosition = this.writerPosition;
            return WRITEFULL;
        }
        mappedByteBuffer.position(this.writerPosition);
        mappedByteBuffer.putInt(log.length);
        mappedByteBuffer.put(log);
        this.writerPosition += increment;
        putWriterPosition(this.writerPosition);
        return WRITESUCCESS;
    }

    public byte[] readNextAndRemove() throws FileEOFException {
        if (this.endPosition != -1 && this.readerPosition >= this.endPosition) {
            throw new FileEOFException("file eof");
        }
        // readerPosition must be less than writerPosition
        if (this.readerPosition >= this.writerPosition) {
            return null;
        }
        mappedByteBuffer.position(this.readerPosition);
        int length = mappedByteBuffer.getInt();
        byte[] b = new byte[length];
        this.readerPosition += length + 4;
        mappedByteBuffer.get(b);
        putReaderPosition(this.readerPosition);
        return b;
    }

    public void close() {
        try {
            if (mappedByteBuffer == null) {
                return;
            }
            mappedByteBuffer.force();
//			AccessController.doPrivileged(new PrivilegedAction<Object>() {
//				public Object run() {
//					try {
//						Method getCleanerMethod = mappedByteBuffer.getClass()
//								.getMethod("cleaner", new Class[0]);
//						getCleanerMethod.setAccessible(true);
//						 Cleaner cleaner = ( Cleaner) getCleanerMethod
//								.invoke(mappedByteBuffer, new Object[0]);
//						cleaner.clean();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					return null;
//				}
//			});
            mappedByteBuffer = null;
            executor.shutdown();
            fc.close();
            raFile.close();
        } catch (IOException e) {
            log.error("close logentity file error:", e);
        }
    }

    public String headerInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(" magicString:");
        sb.append(magicString);
        sb.append(" version:");
        sb.append(version);
        sb.append(" readerPosition:");
        sb.append(readerPosition);
        sb.append(" writerPosition:");
        sb.append(writerPosition);
        sb.append(" nextFile:");
        sb.append(nextFile);
        sb.append(" endPosition:");
        sb.append(endPosition);
        sb.append(" currentFileNumber:");
        sb.append(currentFileNumber);
        return sb.toString();
    }

}
