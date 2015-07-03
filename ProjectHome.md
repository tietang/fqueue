#FQueue介绍
#labels
# Introduction #

FQueue是一个高性能、基于磁盘持久存储的队列消息系统。兼容memcached协议，能用memcached的语言都可以良好的与它通信。
FQueue为你提供一个不需要特别优化，高性能的一个消息系统。


# 特性 #

  * 基于磁盘持久化存储。
  * 支持memcached协议。
  * 支持多队列，密码验证功能。
  * 高性能，能达到数十万qps。
  * 低内存消耗。100-300M内存即可工作得很好。
  * 高效率IO读写算法，IO效率高。
  * 纯JAVA代码。支持进程内JVM级别的直接调用。
  * 在不需要强顺序的场景下，支持多机负载均衡。
# 不支持 #
  * 不支持topic方式的订阅功能。
  * 不支持主从复制。
# 使用 #
> 下载压缩包，解压后，chmode 755 run.sh
> > ./run.sh start 即可启动，默认监听12000端口
> > 测试使用
```
   //memcached协议入队
   memcache.set("key_abc",0,"message1");//key为队列名，abc为密码，在conf/config.properties中配置
   memcache.set("key_abc",0,"message2");
   //获取队列的数据
   memcache.get("key_abc");//取回的应该是message1
   memcache.get("key_abc");//取回的应该是message2

```
# Fqueue有多快 #
## 进程内 ##
Fqueue的底层存储非常高效。下面做个单线程测试（JAVA）：
```
 public static void main(String[] args) throws Exception {
        FQueue fQueue = new FQueue("/home/q/db/");
        StringBuilder sb = new StringBuilder();
        int length = Integer.parseInt(args[0]);
        for (int i = 0; i < length; i++) {
            sb.append("a");
        }
        byte[] data = sb.toString().getBytes();
        fQueue.add(data);// 预热一下
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
            fQueue.add(data);
        }
        System.out.println(100000000.0 / ((System.currentTimeMillis() - start) / 1000) + "qps");
        fQueue.close();
    }
```
运行后输出：
```
#每次写入10字节
~/memcachedbench-0.1.0]# ./test.sh 10
9090909qps
#每次写入1024字节
~/memcachedbench-0.1.0]# ./test.sh 1024
196078qps
```
## 服务模式 ##
多个客户端往Server端每次写入10byte的数据
用以下php脚本测试速度：
```
$mem=new Memcache();
$mem->connect("host",12000);
$start=microtime(true);
for($i=0;$i<30;$i++){
	$count=$mem->get("size|bbs|pass");
	echo microtime(true)."\t".$count."\r\n";
	sleep(1);
}
```
输出：
```
1315645933.5273	288426996
1315645934.5343	288750720
1315645935.5414	289080492
1315645936.5483	289412664
1315645937.5547	289727366
1315645938.5618	290053230
1315645939.5679	290380550
1315645940.5758	290697886
1315645941.5816	291025822
1315645942.5888	291349510
1315645943.5948	291671034
1315645944.6027	292005258
1315645945.6099	292336265
1315645946.6176	292663838
1315645947.6282	292988327
1315645948.6347	293317381
1315645949.6428	293624922
1315645950.6492	293944354
1315645951.6554	294269555
1315645952.6632	294595757
1315645953.6697	294924215
1315645954.677	295253636
1315645955.6829	295593660
1315645956.69	295927374
1315645957.6973	296254742
1315645958.7045	296585110
1315645959.7109	296916046
1315645960.7171	297246379
1315645961.7228	297577807
1315645962.7297	297913268
```
对应的每秒写入
```
323724
329772
332172
314702
325864
327320
317336
327936
323688
321524
334224
331007
327573
324489
329054
307541
319432
325201
326202
328458
329421
340024
333714
327368
330368
330936
330333
331428
335461

```




