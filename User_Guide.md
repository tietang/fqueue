# 协议介绍 #
> FQueue的协议基于Memcached协议。
注意：目前Fqueue会自动忽略flag参数，也就是说不支持客户端的压缩、自动序列化和反序列化。
## 入队 ##
```
add queuename_password[_其他任意字符] flags exptime <bytes>\r\n
<data block>\r\n
```
或者：
```
set queuename_password[_其他任意字符] flags exptime <bytes>\r\n
<data block>\r\n
```
[_其他任意字符]是可选的，对于最终的存储并不会有任何影响，在FQueue内部会自动忽略。比如：
```
add queuename_password_123333 flags exptime <bytes>\r\n
<data block>\r\n
```
通过使用增加后缀的方式，可以实现client根据key做hash，实现分布式。
flags exptime参数在0.1版本中会被忽略。即不支持memcached的过期，自动序列化和反序列化，压缩。
## 出队 ##
```
get queuename_password\r\n
```
## 获取队列大小 ##
获取队列大小只需队列名，无需密码：
```
get size|queuename\r\n
```
## 清空 ##
清空某个队列的所有数据：
```
get clear|queuename|password\r\n
```
## 重新加载权限配置 ##
重新加载配置文件设置的权限信息：
```
get reload|queuename|password\r\n
```
只需要任何一个queuename,password即可。在运行期，可以方便增加新的队列或者更改密码。
## JVM监控信息 ##
获取可以监控JVM的监控信息
```
get monitor|items\r\n
```
items选项可以是
```
fileDescriptor,tomcat,load,allThreadsCount,peakThreadCount,daemonThreadCount,totalStartedThreadCount,deadLockCount,heapMemory,noHeapMemory,memory,classCount,GCTime,memoryPoolCollectionUsage,memoryPoolUsage,memoryPoolPeakUsage
```
## stats ##
与memcached同
```
stats\r\n
```
比如会输出：
```
stats
STAT bytes_written 80513657
STAT connection_structures 0
STAT bytes 0
STAT total_items 0
STAT total_connections 8986
STAT rusage_system 0.0
STAT rusage_user 21
STAT uptime 89923
STAT current_bytes 0
STAT pid 14
STAT get_hits 693687
STAT curr_items 0
STAT free_bytes 300950568
STAT version 0.1
STAT cmd_get 719106
STAT time 1313230151
STAT cmd_set 686142
STAT threads 16
STAT limit_maxbytes 0
STAT bytes_read 57971278
STAT curr_connections 37
STAT system_load 0.67
STAT get_misses 25419
END
```
# 使用 #
## PHP使用 ##
```
$mem=new Memcache();
$mem->connect('127.0.0.1',12000);
$mem->add('queuename_password',$message,0,0);
$msg=$mem->get('queuename_password');
echo $msg;
```
## JAVA使用 ##
## JAVA进程内使用（性能最高） ##
Fqueue的底层FSQueue可以直接在java应用中，作为嵌入式的持久化队列使用
# 关于性能 #_

# 监控 #

# 高可用设计 #