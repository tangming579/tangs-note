参考：https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md

1. 拉取镜像

   ```
   docker pull togettoyou/ghcr.io.kubernetes-client.java.crd-model-gen:v1.0.6
   docker tag togettoyou/ghcr.io.kubernetes-client.java.crd-model-gen:v1.0.6 ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6
   	
   docker pull togettoyou/ghcr.io.yue9944882.crd-model-gen-base:v1.0.0
   docker tag togettoyou/ghcr.io.yue9944882.crd-model-gen-base:v1.0.0 ghcr.io/yue9944882/crd-model-gen-base:v1.0.0
   ```

2. 运行代码生成镜像

   ```
   -u: CRD 的下载 URL 或文件路径
   -n: 目标 CRD 的组名
   -p: 输出 Java 包名
   -o: 输出路径
   ```

   示例：

   ```sh
   LOCAL_CRD=/tmp/crds
   mkdir -p /tmp/java && cd /tmp/java
   docker run \
     --rm \
     -v "$LOCAL_CRD":"$LOCAL_CRD" \
     -v /var/run/docker.sock:/var/run/docker.sock \
     -v "$(pwd)":"$(pwd)" \
     -ti \
     --network host \
     ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6 \
     /generate.sh \
     -u /tmp/crds/crontab-crd.yaml \
     -n com.example.stable \
     -p com.example.stable \
     -o "$(pwd)"
   ```

   
