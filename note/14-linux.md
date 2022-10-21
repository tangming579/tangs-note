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



#### 磁盘管理

#### 文本处理

#### 网络通讯

#### 系统设置

#### 系统管理

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

