## Docker 基本操作

### 安装 Docker

安装docker

```sh
#安装Docker
sudo yum install docker-ce docker-ce-cli containerd.io
#启用docker
sudo systemctl start docker
sudo systemctl enable docker
```

修改配置

```sh
#创建文件夹
sudo mkdir -p /etc/docker
#编辑/etc/docker/daemon.json文件，并输入国内镜像源地址
sudo vi /etc/docker/daemon.json

{
  "registry-mirrors": ["https://registry.docker-cn.com"]
}
#重启服务
sudo systemctl daemon-reload
sudo systemctl restart docker

docker run --name myapi  -d -p 19121:19121 --restart=always -v /var/log/mysystem:/app/log mysystem
```

### 常用命令

```shell
#查看挂掉的容器：
docker ps -a
#查看指定容器的日志：
docker logs b1d05f65856f
#进入docker容器：
docker exec -it containerID /bin/bash
```

```shell
docker build -t demotest .    构建 demotest镜像
docker inspect demotest     查看 运行容器的详情
docker ps -a                      查看当前所有的容器
docker rm $(docker ps -aq)     删除所有容器
docker rmi $(docker images -q)   删除所有镜像
docker rmi $(docker images | grep "none" | awk '{print $3}') 删除所有满足条件的
docker cp /www/runoob 96f7f14e99ab:/www/  将主机/www/runoob目录拷贝到容器的/www目录下
docker commit f3aff5ca8aa3 mynetweb   将容器f3aff5ca8aa3生成镜像mynetweb
docker save busybox:1.0 > busybox.tar
docker save 3f43f72cb283 > busybox.tar
docker save busybox:1.0 |gzip > busybox.tgz
#如果save时使用的tag 则会保存 tag信息，如果使用image ID 则会丢失。
docker load -i nginx.tar
docker tag [镜像id] [新镜像名称]:[新镜像标签] 
```

### harbor push

```sh
docker login -u ${HARBOR_USER} -p ${HARBOR_PASS} 10.15.20.115:8000
docker tag ${IMAGEID} 10.15.20.115:8000/${HARBOR_REP}/busybox:1.0
docker push 10.15.20.115:8000/${HARBOR_REP}/busybox:1.0
```

