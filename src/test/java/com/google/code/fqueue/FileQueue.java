package com.google.code.fqueue;


public class FileQueue extends FQueue {
    private boolean isClose = false;

    public FileQueue(String path) throws Exception {
        super(path);
    }

    public FileQueue(String path, int logsize) throws Exception {
        super(path, logsize);
    }

    @Override
    public void close() {
        super.close();
        this.isClose = true;
    }

    public boolean isClose() {
        return isClose;
    }


}
