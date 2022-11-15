package com.google.code.fqueue;

import java.util.Random;

public class Example {
    private static FileQueue fileFQueue;
    public static FileBlockableQueue queue;

    static {
        try {
            fileFQueue = new FileQueue("db", 1024);
            queue = new FileBlockableQueue(fileFQueue);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws InterruptedException {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    queue.poll();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.start();
        Random r = new Random();
        while (true) {
            queue.offer(("testqueueoffer" + r.nextDouble()).getBytes());
            Thread.sleep(5);
        }
    }
}
