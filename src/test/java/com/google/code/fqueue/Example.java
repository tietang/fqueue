package com.google.code.fqueue;

import java.util.Random;

public class Example {
    private static FQueue queue;

    static {
        try {
            queue = new FQueue("db", 1024);
            queue.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws InterruptedException {
        Random r = new Random();
        while (true) {
            queue.offer(("testqueueoffer" + r.nextDouble()).getBytes());
            Thread.sleep(1);
        }
    }
}
