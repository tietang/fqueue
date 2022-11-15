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
package com.google.code.fqueue.memcached.storage;

import com.google.code.fqueue.FQueue;
import com.google.code.fqueue.exception.ConfigException;
import com.google.code.fqueue.util.Config;
import com.google.code.fqueue.util.JVMMonitor;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.protocol.exceptions.ClientException;
import com.thimbleware.jmemcached.protocol.exceptions.DatabaseException;
import com.thimbleware.jmemcached.storage.CacheStorage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Memcached的缓存存储实现，底层通过FQueue实现。 通过Memcached协议的key来实现二次协议
 *
 * @author sunli
 * @version $Id$
 * @date 2011-5-11
 */
public class FSStorage implements CacheStorage<String, LocalCacheElement> {
    /**
     * 存储所有的队列服务
     */
    private Map<String, AbstractQueue<byte[]>> queuemMap = new ConcurrentHashMap<String, AbstractQueue<byte[]>>();
    private final ReentrantLock lock = new ReentrantLock();
    private final static Log log = LogFactory.getLog(FSStorage.class);
    /**
     * 每个队列服务的单个日至存储的大小限制 配置文件中的单位为M
     */
    private final static int logSize = 1024 * 1024 * Integer.parseInt(Config.getSetting("logsize"));
    /**
     * 数据存储路径
     */
    private final static String dbpath = Config.getSetting("path").trim();
    /**
     * 安全验证map
     */
    private static Map<String, String> authorizationMap = new ConcurrentHashMap<String, String>(20);

    static {
        loadAuthorization();
    }

    private static void loadAuthorization() {
        String line = Config.getSetting("authorization");
        if (line != null) {
            if (line.indexOf("_") != -1) {
                log.error("权限配置不能包含'_'符号");
            }
            String[] group = line.split("@@");
            for (int i = 0, groupLen = group.length; i < groupLen; i++) {
                String[] item = group[i].split("\\|");
                if (item.length == 2) {
                    authorizationMap.put(item[0], item[1]);
                }
            }
        }
    }

    /**
     * 重新加载安全验证数据，可用于在线动态增加队列，修改密码等操作
     */
    private static void reloadAuthorization() {
        Config.reload();
        loadAuthorization();
    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public void clear() throws DatabaseException, Exception {
    }

    @Override
    public void close() throws IOException {
        for (String key : queuemMap.keySet()) {
            ((FQueue) queuemMap.get(key)).close();
        }
        log.info("close queue");
    }

    /**
     * get方式的二次协议实现
     *
     * @param keystring
     * @return
     * @throws ClientException
     */
    private LocalCacheElement getProtocol(String keystring) throws ClientException {
        // 获取队列中元素的个数
        if (keystring.startsWith("size")) {
            try {
                // size|bbs
                // size操作无密码验证，只需要队列名称正确即可
                String[] clientInfo = QueueClient.parse(keystring, '|');
                if (clientInfo.length < 2 || authorizationMap.containsKey(clientInfo[1]) == false) {
                    return null;
                }
                AbstractQueue<byte[]> sizeQueue = getClientQueue(clientInfo[1]);
                if (sizeQueue == null) {
                    return null;
                }
                int size = sizeQueue.size();
                LocalCacheElement element = new LocalCacheElement(keystring, 0, 0, 0);
                element.setData(String.valueOf(size).getBytes());
                return element;
            } catch (Exception e) {
                log.error("getsize " + keystring + "error", e);
                return null;
            }
        }
        // 清空队列中所有的元素
        if (keystring.startsWith("clear")) {
            try {
                // clear|bbs|pass
                String[] clientInfo = QueueClient.parse(keystring, '|');
                if (clientInfo.length < 3 || valid(clientInfo[1], clientInfo[2]) == false) {
                    throw new ClientException("Authorization error");
                }
                AbstractQueue<byte[]> queue = getClientQueue(clientInfo[1]);
                queue.clear();
                LocalCacheElement element = new LocalCacheElement(keystring, 0, 0, 0);
                element.setData(String.valueOf(queue.size()).getBytes());
                return element;
            } catch (Exception e) {
                log.error("getsize " + keystring + "error", e);
                return null;
            }
        }
        // 重新加载权限信息
        if (keystring.startsWith("reload")) {
            try {
                // reload|bbs|pass
                String[] clientInfo = QueueClient.parse(keystring, '|');
                if (clientInfo.length < 3 || valid(clientInfo[1], clientInfo[2]) == false) {
                    throw new ClientException("Authorization error");
                }
                reloadAuthorization();
                LocalCacheElement element = new LocalCacheElement(keystring, 0, 0, 0);
                element.setData("reloadAuthorization".getBytes());
                return element;
            } catch (ConfigException e) {
                log.error(e.getMessage(), e);
            } catch (Exception e) {
                log.error("reloadAuthorization error", e);
                return null;
            }
        }
        // 重新加载权限信息
        if (keystring.startsWith("monitor")) {
            try {
                // size|bbs
                // size操作无密码验证，只需要队列名称正确即可
                String[] clientInfo = QueueClient.parse(keystring, '|');
                if (clientInfo.length < 2) {
                    return null;
                }
                String items = clientInfo[1];
                StringBuilder stats = new StringBuilder();
                if (items != null) {
                    String[] itemList = StringUtils.split(items, ",");
                    for (int i = 0, len = itemList.length; i < len; i++) {
                        if (i > 0) {
                            stats.append("\r\n");
                        }
                        String data = JVMMonitor.getMonitorStats(itemList[i]);
                        stats.append(data);
                    }
                } else {
                    stats.append("need items");
                }

                LocalCacheElement element = new LocalCacheElement(keystring, 0, 0, 0);
                element.setData(stats.toString().getBytes());
                return element;
            } catch (Exception e) {
                log.error("monitor " + keystring + "error", e);
                return null;
            }
        }
        throw new ClientException(keystring + " command Unsupported now");
    }

    @Override
    public LocalCacheElement get(String keystring) {
        try {
            if (keystring.indexOf("_") == -1) {
                return getProtocol(keystring);
            }
            String[] clientInfo = QueueClient.parseWithCache(keystring);
            if (valid(clientInfo[0], clientInfo[1])) {
                byte[] data;
                data = getClientQueue(clientInfo[0]).poll();
                if (data != null) {
                    LocalCacheElement element = new LocalCacheElement(keystring, 0, 0, 0);
                    element.setData(data);
                    return element;
                } else {
                    log.info("queue empty");
                }
            } else {
                log.error("unvalid " + keystring);
                throw new ClientException("Authorization error");
            }
        } catch (Exception e) {
            log.error("get queue " + keystring + " error", e);
            return null;
        }
        return null;
    }

    @Override
    public long getMemoryCapacity() {
        return 0;
    }

    @Override
    public long getMemoryUsed() {
        return 0;
    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public LocalCacheElement put(String keystring, LocalCacheElement e) throws DatabaseException, Exception {
        String[] clientInfo = QueueClient.parseWithCache(keystring);
        if (valid(clientInfo[0], clientInfo[1])) {// 先进行密码验证
            getClientQueue(clientInfo[0]).add(e.getData());
            return null;
        } else {
            throw new ClientException("Authorization error");
        }
    }

    @Override
    public LocalCacheElement putIfAbsent(String keystring, LocalCacheElement e) throws DatabaseException, Exception {
        String[] clientInfo = QueueClient.parseWithCache(keystring);
        if (valid(clientInfo[0], clientInfo[1])) {// 先进行密码验证
            getClientQueue(clientInfo[0]).add(e.getData());
            return null;
        } else {
            throw new ClientException("Authorization error");
        }

    }

    @Override
    public LocalCacheElement remove(String key) throws DatabaseException, Exception {
        return null;
    }

    @Override
    public boolean replace(String keystring, LocalCacheElement old, LocalCacheElement prepend)
            throws DatabaseException, Exception {
        return false;
    }

    @Override
    public LocalCacheElement replace(String key, LocalCacheElement placeHolder) throws DatabaseException, Exception {
        return null;
    }

    @Override
    public long size() {
        return 0;
    }

    private boolean valid(String appid, String pwd) {
        return pwd.equals(authorizationMap.get(appid));
    }

    /**
     * 获取指定名称的队列存储实例 如果不存存在，根据create参数决定是否创建
     *
     * @param name
     * @return
     * @throws Exception
     */
    private AbstractQueue<byte[]> getClientQueue(String name, boolean create) throws Exception {
        AbstractQueue<byte[]> queue = queuemMap.get(name);
        if (queue == null) {
            if (create == true) {
                lock.lock();
                try {
                    queue = queuemMap.get(name);
                    if (queue == null) {
                        queue = new FQueue(dbpath + "/" + name, logSize);
                        queuemMap.put(name, queue);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        return queue;
    }

    /**
     * 获取或者创建指定名称的队列存储实例
     *
     * @param name
     * @return
     * @throws Exception
     */
    private AbstractQueue<byte[]> getClientQueue(String name) throws Exception {
        return getClientQueue(name, true);
    }
}
