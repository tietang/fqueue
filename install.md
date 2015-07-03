# 部署 #
## JAVA环境 ##
安装jdk6
## 部署Fqueue ##
  1. 直接从google code下载压缩包，解压后./run.sh start 即可运行
  1. svn check out出源代码，用maven 的mvn package即可打包出上一步使用的压缩包
# 配置 #
Fqueue的配置文件位于conf/config.properties,默认配置：
```
port=12000
path=db
logsize=40
authorization=key|abc@@bbs|pass
```
  1. port Fqueue的启动端口
  1. path Fqueue的数据在磁盘上的存储目录
  1. logsize Fqueue在存储数据到磁盘上时，每个文件的最大大小（单位MB）,logsize不要设置太大，一般100M以内吧。推荐用40左右的大小。
  1. authorization 权限配置信息格式
```
队列1|队列1的密码@@队列2|队列2的密码@@……
```
注意：队列名和密码不能包含“@”、"|"、,"