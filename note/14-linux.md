### 命令大全

#### 文件管理

**file**：用来探测给定文件的类型

```
file install.log  ---> install.log: UTF-8 Unicode text
```

**find**：在磁盘中检索所有类型文件，效率比较低

```
find /home -name *.log  --- 查找home路径下 .log文件
find . -size +100M --- 查找当前路径大于100M文件, +代表大于 -代表小于 等于无前缀
find / -iname k8s.yaml --- 不区分大小写全局查找文件
```

**which**：在环境变量$PATH设置的目录里查找可执行文件位置

```
which java  ---> /usr/bin/java
```

**whereis**：数据库中搜索二进制程序名

```
whereis java  ---> java: /usr/bin/java /usr/share/man/man1/java.1
```

**locate**：类似whereis，根据每天更新的数据库（/var/lib/mlocate）查找，速度快

```
locate mysql
```

**mv**：对文件或目录重新命名，或者将文件从一个目录移到另一个目录中

```
mv -v *.txt /home/office  -- 打印移动日志
mv -uv *.txt /home/office --- 源文件比目标文件新时才执行更新
mv -vn *.txt /home/office --- 不要覆盖任何已存在的文件
mv -bv *.txt /home/office --- 复制时创建备份
```

**rm**：删除给定的文件和目录

```
rm -rf *.html  --- 删除当前项目下 .html 结尾的文件
rm -rf icons/**/data --- 批量删除 icons 文件夹中的子文件夹中的 data 文件夹
find ./docs -name "*.html" -exec rm -rf {} \; --- 查找 .html 结尾的文件并删除
```

**split**：分割任意大小的文件

```
split -b 10k date.file -d -a 3 split_file --- 分割成大小为10KB的小文件，指定后缀长度
```

**cat**：一次性在终端中显示文件的所有内容

**less**：分页显示文件内容，less命令会分一页一页地显示文件内容，cat会一次性全部显示

**more**：和less命令相似，但没有less命令强大，不能往后翻页，只能一路往前翻页

```
cat -n demo.txt --- 由 1 开始对所有输出的行数进行编号
ps -ef |less --- ps查看进程信息并通过less分页显示
```

**chmod**：用来变更文件或目录的权限

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

**chown**：变更文件或目录的拥有者或所属群组

```
chown -R tang /usr/demo  --- 将目录/usr/demo 及其下面的所有文件、子目录的文件主改成tang
```

#### 磁盘管理

 **df**：文件系统磁盘使用情况统计(disk free)

```
df -h
--- >
Filesystem            Size  Used Avail Use% Mounted on
C:/Program Files/Git  201G  128G   73G  64% /
D:                    101G   32G   70G  32% /d
E:                    174G   12G  162G   7% /e
```

**du**：文件或目录大小(disk usage)

```
du -h /home/note
--- >
221K    note
```

**ls**：显示指定工作目录下之内容

```
ls -laR /bin --- 递归显示/bin下所有文件及目录 (. 开头的隐藏文件也会列出)包括详细信息
```

**mkdir**：创建目录

```
mkdir -p runoob2/test --- 确保目录名称存在，不存在的就建一个。
```

#### 系统设置

**env**：显示系统中已存在的环境变量

```
env | grep mylove  --- 显示环境变量值
```

**alias**：定义或显示别名

```
alias --- 列出所有别名
alias ls  --- 列出单个已定义的别名
alias ls='ls --color=auto'  --- 设置别名
```

**crontab**：定期执行程序

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

**export**：设置或显示环境变量

```
 export -p  --- 列出当前所有的环境变量
 export PATH=$PATH:/home/tang/test/bin  --- 设置环境变量（该种方式是会话级的，断开终端后失效）
 
 #永久生效方式：
 vim /etc/profile ---> 增加: export PATH=$PATH:/home/tang/test/bin
 source /etc/profile
```

**rpm**：RPM软件包的管理工具

```
rpm -ivh *.rpm --nodeps --force
rpm -e --nodeps your-package --- 卸载指定软件包（不带后缀.rpm）
rpm -qa | grep sql  --- 已安装软件包中查找包含sql的包
```



#### 系统管理

**free**：显示内存的使用情况

```
free -m --- 以MB为单位显示内存使用情况
```

**kill**：删除执行中的程序或工作

```
kill -9 PID --- 彻底杀死进程
```

**ps**：显示当前进程的状态，类似于 windows 的任务管理器（process status）

```
ps aux | sort -rnk 4 # 按内存资源的使用量对进程进行排序
ps aux | sort -nk 3  # 按 CPU 资源的使用量对进程进行排序
ps -ef # 显示所有进程信息，连同命令行
```



#### 文本处理

#### 网络通讯

#### journalctl

```
journalctl -n 15 --- 显示尾部指定行数的日志
journalctl -f --- 实时滚动显示最新日志
journalctl -u nginx.service --- 查看某个 Unit 的日志
journalctl --since 07:30 --until "2 hour ago"
```

#### find

```
#在磁盘中检索所有类型文件，效率比较低
find /home -name *.log  --- 查找home路径下 .log文件
find . -size +100M --- 查找当前路径大于100M文件, +代表大于 -代表小于 等于无前缀
find / -iname k8s.yaml --- 不区分大小写全局查找文件

#在环境变量$PATH设置的目录里查找运行中的文件
which java
->/usr/bin/java

#在数据库索引中查找软件的安装目录
whereis java
->java: /usr/bin/java /usr/share/man/man1/java.1
```

### 快捷键篇

```
Ctrl+A --- 快速移动光标到行首
Ctrl+E --- 快速移动光标到行尾
Ctrl+[←]/[→] --- 将光标按照单词进行移动(以空格、标点为界)

Ctrl+Insert ---复制命令行内容
shift+Insert ---粘贴命令行内容
Ctrl+K --- 将光标之后的内容进行全部删除(剪切)
Ctrl+U --- 将光标之前的内容进行全部删除(剪切)
Ctrl+Y --- 将剪切或复制的内容进行粘贴
Ctrl+W --- 将光标之前的字符串进行删除(剪切)

Ctrl+L --- 清屏操作
Ctrl+D --- 注销
Ctrl+S --- 进入远程连接锁屏状态
Ctrl+Q --- 解除远程连接锁屏状态
Ctrl+Z --- 可以暂停程序运行过程

Ctrl+Shift+[+] --- 放大终端
Ctrl+[-] --- 缩小终端
```

### Vim

vim中的三种模式：(1) 命令模式；(2) 编辑模式（输入模式）；(3) 底行模式

(1) 命令模式 -----> (3) 底行模式：冒号

(2) 编辑模式 -----> (1) 命令模式 ："ESC键"

(3) 底行模式 -----> (1) 命令模式："ESC键"

```
G --- 将光标快速移动到最后一行
gg --- 将光标快速移动到第一行
$(shift+4) --- 将光标移动到一行的行尾
^(shift+6) --- 将光标移动到一行的行首
yy --- 复制光标所在行内容
p --- 粘贴复制或剪切内容
dd --- 删除光标所在行内容
ndd --- 删除光标所在行以及以下n行内容
r --- 将光标所在位置内容直接做替换
R --- 进入替换模式
u --- 撤销编辑操作内容
Ctrl + r --- 恢复撤销操作内容
数字n+↑，数字n+↓ --- 以当前光标为准向上/向下移动n行
数字n+←，数字n+→ --- 以当前光标为准想左/向右移动n个字符
```