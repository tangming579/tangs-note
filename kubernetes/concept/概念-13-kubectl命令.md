## 安装

```sh
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

### 启用 shell 自动补全功能

zsh：下面内容加入到文件 ~/.zshrc 中

```
source <(kubectl completion zsh)
```

bash：

```sh
apt-get install bash-completion 或 yum install bash-completion

# 将下面内容添加到文件 ~/.bashrc
ource /usr/share/bash-completion/bash_completion
# kubectl 补全脚本已经导入（sourced）到 Shell 会话中
kubectl completion bash | sudo tee /etc/bash_completion.d/kubectl > /dev/null
sudo chmod a+r /etc/bash_completion.d/kubectl

# 如果 kubectl 有关联的别名
echo 'alias k=kubectl' >>~/.bashrc
echo 'complete -o default -F __start_kubectl k' >>~/.bashrc
```

