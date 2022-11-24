### 命令大全

#### 文件管理

##### file

用来探测给定文件的类型

```
file install.log  ---> install.log: UTF-8 Unicode text
```

##### find

在磁盘中检索所有类型文件，效率比较低

```
find /home -name *.log  --- 查找home路径下 .log文件
find . -size +100M --- 查找当前路径大于100M文件, +代表大于 -代表小于 等于无前缀
find / -name k8s.yaml --- 不区分大小写全局查找文件
```

##### which

在环境变量$PATH设置的目录里查找可执行文件位置

```
which java  ---> /usr/bin/java
```

##### whereis

数据库中搜索二进制程序名

```
whereis java  ---> java: /usr/bin/java /usr/share/man/man1/java.1
```

##### locate

类似whereis，根据每天更新的数据库（/var/lib/mlocate）查找，速度快

```
locate mysql
```

##### mv（move file）

对文件或目录重新命名，或者将文件从一个目录移到另一个目录中

```
mv -v *.txt /home/office  -- 打印移动日志
mv -uv *.txt /home/office --- 源文件比目标文件新时才执行更新
mv -vn *.txt /home/office --- 不要覆盖任何已存在的文件
mv -bv *.txt /home/office --- 复制时创建备份
```

##### rm（remove）

删除给定的文件和目录

```
rm -rf *.html  --- 删除当前项目下 .html 结尾的文件
rm -rf icons/**/data --- 批量删除 icons 文件夹中的子文件夹中的 data 文件夹
find ./docs -name "*.html" -exec rm -rf {} \; --- 查找 .html 结尾的文件并删除
```

##### **split**

分割任意大小的文件

```
split -b 10k date.file -d -a 3 split_file --- 分割成大小为10KB的小文件，指定后缀长度
```

##### cat（concatenate）

一次性在终端中显示文件的所有内容

##### more

不能往后翻页，只能一路往前翻页

```
cat -n demo.txt --- 由 1 开始对所有输出的行数进行编号
ps -ef |less --- ps查看进程信息并通过less分页显示
```

##### less

more的升级版，分页显示文件内容，less命令会分一页一页地显示文件内容，cat会一次性全部显示

##### chmod（change mode）

用来变更文件或目录的权限

八进制语法：

| #    | 权限           | rwx  | 二进制 |
| ---- | -------------- | ---- | ------ |
| 7    | 读 + 写 + 执行 | rwx  | 111    |
| 6    | 读 + 写        | rw-  | 110    |
| 5    | 读 + 执行      | r-x  | 101    |
| 4    | 只读           | r--  | 100    |
| 3    | 写 + 执行      | -wx  | 011    |
| 2    | 只写           | -w-  | 010    |
| 1    | 只执行         | --x  | 001    |
| 0    | 无             | ---  | 000    |

```
chmod ugo+r file1.txt --- 将文件 file1.txt 设为所有人皆可读取
chmod -R a+r * --- 将目前目录下的所有文件与子目录皆设为任何人可读取
```

##### chown（change owner）

变更文件或目录的拥有者或所属群组

```
chown -R tang /usr/demo  --- 将目录/usr/demo 及其下面的所有文件、子目录的文件主改成tang
```

#### 磁盘管理

#####  df（disk free）

文件系统磁盘使用情况统计

```
-h, --human-readable  以K，M，G为单位，提高信息的可读性。

df -h
--- >
Filesystem            Size  Used Avail Use% Mounted on
C:/Program Files/Git  201G  128G   73G  64% /
D:                    101G   32G   70G  32% /d
E:                    174G   12G  162G   7% /e
```

##### du（disk usage）

文件或目录大小

```
du -h /home/note
--- >
221K    note
```

##### ls（list）

显示指定工作目录下之内容

```
ls -laR /bin --- 递归显示/bin下所有文件及目录 (. 开头的隐藏文件也会列出)包括详细信息
```

##### mkdir（make directory）

创建目录

```
mkdir -p runoob2/test --- 确保目录名称存在，不存在的就建一个。
```

##### stat（统计）

显示文件的状态信息，比 ls 更详细

```
stat myfile
```

#### 系统设置

##### env

显示系统中已存在的环境变量

```
env | grep mylove  --- 显示环境变量值
```

##### alias

定义或显示别名

```
alias --- 列出所有别名
alias ls  --- 列出单个已定义的别名
alias ls='ls --color=auto'  --- 设置别名
```

##### crontab

定期执行程序

```
用户任务调度：
保存后系统会自动存放在/var/spool/cron/目录中，文件以用户名命名，crontab服务每隔一分钟去读取一次配置的内容。
crontab -e: 编辑当前用户的定时任务列表
crontab -l: 查看当前用户的定时任务列表
crontab -r: 删除当前用户的定时任务列表

系统任务调度：
/etc/crontab
系统周期性所要执行的工作
```

##### export

设置或显示环境变量

```
 export -p  --- 列出当前所有的环境变量
 export PATH=$PATH:/home/tang/test/bin  --- 设置环境变量（该种方式是会话级的，断开终端后失效）
 
 #永久生效方式：
 vim /etc/profile ---> 增加: export PATH=$PATH:/home/tang/test/bin
 source /etc/profile
```

##### rpm

RPM软件包的管理工具

```
rpm -ivh *.rpm --nodeps --force
rpm -e --nodeps your-package --- 卸载指定软件包（不带后缀.rpm）
rpm -qa | grep sql  --- 已安装软件包中查找包含sql的包
```

#### 系统管理

##### free

显示内存的使用情况

```
free -m --- 以MB为单位显示内存使用情况
```

##### kill

删除执行中的程序或工作

```
kill -9 PID --- 彻底杀死进程
```

##### ps（process status）

显示当前进程的状态，类似于 windows 的任务管理器

```
ps -aux | sort -rnk 4 # 按内存资源的使用量对进程进行排序
ps -ef # 显示所有进程信息，连同命令行

这两者的输出结果差别不大，但展示风格不同。aux是BSD风格，ef是System V风格
```

##### su（switch user）

变更为其他使用者的身份

```
su root //切换到root用户
```

##### sudo（super user do）

以系统管理者的身份执行指令

```
sudo -i  --- 一直执行某些只有超级用户才能执行的权限，不用每次输入密码
```

##### top

显示或管理执行中的程序

```
top -p 139 --- 显示进程号为139的进程信息，CPU、内存占用率等
top -n 10  --- 显示更新十次后退出
```

##### uname（unix name）

显示系统信息

```
 uname -a  --- 显示全部系统信息
```

##### who

显示当前登录系统的用户

##### whoami

显示自身用户名称

#### 文本处理

##### sed

功能强大的流式文本编辑器

```

```

##### awk

文本和数据进行处理的编程语言

```

```

##### grep（Globally search a Regular Expression and Print）

查找文件里符合条件的字符串

```
grep test *file  --- 查找后缀有 file 字样的文件中包含 test 字符串的文件
grep -v test *test*  --- 查找文件名中包含 test 的文件中不包含test 的行
grep -r update /etc/acpi --- 查找/etc/acpi及其子目录下所有文件中包含字符串"update"的文件，并打印出该字符串所在行的内容
```

##### wc（word count）

统计文件的字节数、字数、行数

```
-l # 统计行数
-m # 统计字符数
-w # 统计字数。
```

#### 网络通讯

##### telnet

登录远程主机和管理(测试ip端口是否连通)

telnet因为采用明文传送报文，安全性不好，很多Linux服务器都不开放telnet服务，而改用更安全的ssh方式了

```
telnet 118.10.6.128 88  --- 测试端口是否打开
```

##### curl（client URL）

利用URL规则在命令行下工作的文件传输工具

```
-X         指定 HTTP 请求的方法
-d         用于发送 POST 请求的数据体。
-H         添加 HTTP 请求的标头。
--cookies  用来向服务器发送 Cookie。

curl -X POST -H "Content-type: application/json" -d '{"phone":"13521389587","password":"test"}' http://192.168.100.2:8080/api/user -cookie "user=root;pass=123456"
```

##### ifconfig（network interfaces configuring）

配置和显示Linux系统网卡的网络参数

```
ifconfig eth0 up/down  --- 开启关闭指定网卡
ifconfig eth0 down
```

##### netstat

查看Linux中网络系统状态信息

```
-n或--numeric：直接使用ip地址，而不通过域名服务器；
-t或--tcp：显示TCP传输协议的连线状况；
-l或--listening 显示监控中的服务器的Socket。
-p或--programs：显示正在使用Socket的程序识别码和程序名称；
```

#### 归档压缩

##### gzip

压缩文件

-d或--decompress或----uncompress：解开压缩文件； 

-f或——force：强行压缩文件。不理会文件名称或硬连接是否存在以及该文件是否为符号连接；

-r或——recursive：递归处理，将指定目录下的所有文件及子目录一并处理；

-v或——verbose：显示指令执行过程；

-l或——list：列出压缩文件的相关信息；

```
gzip *  --- 压缩所有文件
gzip -dv * --- 解压
gzip -rv test6  --- 递归压缩目录
gzip -r log.tar  --- 压缩一个tar备份文件，此时压缩文件的扩展名为.tar.gz
```

##### tar

把一大堆的文件和目录全部打包成一个文件，Linux中很多压缩程序只能针对一个文件进行压缩

```
打包：tar -cvf [目标文件名].tar [原文件名/目录名]
解包：tar -xvf [原文件名].tar
```

> 注：c参数代表create（创建），x参数代表extract（解包），v参数代表verbose（详细信息），f参数代表filename（文件名），所以f后必须接文件名。

**tar文件压缩：**

```
压缩：gzip [原文件名].tar
解压：gunzip [原文件名].tar.gz
```

**打包并压缩/解包 gzip格式：**

```
打包并压缩： tar -zcvf [目标文件名].tar.gz [原文件名/目录名]
解压并解包： tar -zxvf [原文件名].tar.gz
```

> z代表用gzip算法来压缩/解压。
>
> j代表用bzip2算法来压缩/解压

**jar格式**

```
压缩：jar -cvf [目标文件名].jar [原文件名/目录名]
解压：jar -xvf [原文件名].jar
```

##### zip

可以用来解压缩文件

```
zip -r html.zip /home/html --- 将/home/html/目录下所有文件和文件夹打包为当前目录下的html.zip
unzip test.zip  --- 解压test.zip文件至当前目录
```

#### 其他

##### journalctl

```
journalctl -n 15 --- 显示尾部指定行数的日志
journalctl -f --- 实时滚动显示最新日志
journalctl -u nginx.service --- 查看某个 Unit 的日志
journalctl --since 07:30 --until "2 hour ago"
```

##### yum（Yellow dog Updater, Modified）

在 Fedora 和 RedHat 以及 SUSE 中的 Shell 前端 RPM软件包管理器

```
yum update  --- 更新所有软件
yum list  --- 列出所有可安裝的软件清单
yum list installed  --- 列出已安装的包
yum remove <package_name>  --- 移除包
yum search <keyword>  --- 查找包
yum install  --downloadonly --downloaddir=路径  安装包名  --- 仅下载 rpm 包到指定路径
```



### 快捷键篇

```
Ctrl+A --- 快速移动光标到行首
Ctrl+E --- 快速移动光标到行尾
Ctrl+[←]/[→] --- 将光标按照单词进行移动(以空格、标点为界)

Ctrl+K --- 将光标之后的内容进行全部删除(剪切)
Ctrl+U --- 将光标之前的内容进行全部删除(剪切)
Ctrl+W --- 将光标之前的字符串进行删除(剪切)
Ctrl+Y --- 将[上面三个操作]剪切或复制的内容进行粘贴

Ctrl+L --- 清屏操作
Ctrl+D --- 注销
Ctrl+S --- 进入远程连接锁屏状态
Ctrl+Q --- 解除远程连接锁屏状态
Ctrl+Z --- 可以暂停程序运行过程

Ctrl+Shift+[+] --- 放大终端
Ctrl+[-] --- 缩小终端

Ctrl+Insert ---复制命令行选中的内容
shift+Insert ---粘贴内容
```

### vim

vim中的三种模式：(1) 命令模式；(2) 编辑模式（输入模式）；(3) 底行模式

(1) 命令模式 -----> (3) 底行模式：冒号

(2) 编辑模式 -----> (1) 命令模式 ："ESC键"

(3) 底行模式 -----> (1) 命令模式："ESC键"

```
yy --- 复制光标所在行内容
p --- 粘贴复制或剪切内容
dd --- 删除光标所在行内容
ndd --- 删除光标所在行以及以下n行内容
u --- 撤销编辑操作内容
Ctrl + r --- 恢复撤销操作内容

G --- 将光标快速移动到最后一行
gg --- 将光标快速移动到第一行
$(shift+4) --- 将光标移动到一行的行尾
^(shift+6) --- 将光标移动到一行的行首

r --- 将光标所在位置内容直接做替换
R --- 进入替换模式

数字n+↑，数字n+↓ --- 以当前光标为准向上/向下移动n行
数字n+←，数字n+→ --- 以当前光标为准想左/向右移动n个字符
```