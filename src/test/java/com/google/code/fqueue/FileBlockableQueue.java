package com.google.code.fqueue;

import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author tietang
 * Created by tietang on 2015/7/9 12:32.
 */
public class FileBlockableQueue {
    final Lock lock = new ReentrantLock();
    final Condition notEmpty = lock.newCondition();
    private Queue<byte[]> queue = null;


    public FileBlockableQueue(Queue<byte[]> queue) {
        this.queue = queue;
    }

    public boolean offer(byte[] bytes) {
        if (!isAvailable()) {
            return false;
        }
        lock.lock();
        try {
            boolean v = queue.offer(bytes);
            notEmpty.signal();
            return v;
        } finally {
            lock.unlock();
        }
    }


    public byte[] take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == 0) {
                notEmpty.await();
            }
            byte[] bytes = queue.poll();
            return bytes;
        } finally {
            lock.unlock();
        }

    }

    public byte[] poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public boolean isAvailable() {
        if (queue instanceof FileQueue) {
            FileQueue q = (FileQueue) queue;
            return !q.isClose();
        }
        return true;
    }
}
