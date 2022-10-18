```sh
git log --pretty=short --oneline
```



工作区（working tree ）

暂存区（index/stage）

仓库（Repository）



```
git remote add upstream https://gitlab.dev.21vianet.com/sbg2-tenxcloud/tdsf/service-mesh.git

每次请求合并前：
git fetch upstream main
git merge upstream/main

git config --global user.name "tangming"
git config --global user.email  "tang.ming@sina.com"

回滚：
1. git reset --mixed
	只保留工作区的修改，暂存区的差异，会被全部放到工作区中，等同于不带任何参数的git reset
2. git reset --soft
    工作区和暂存区的内容不变，在三个命令中对现有版本库状态改动最小
3. git reset --hard   
	将暂存区与工作区都回到上一次版本，并删除之前的所有信息提交

git reset HEAD^            # 回退所有内容到上一个版本  
git reset HEAD^ hello.php  # 回退 hello.php 文件的版本到上一个版本  
git reset  052e            # 回退到指定版本


修改提交信息：git commit --amend
```



### git reabase

```
1. git rebase -i [startPonit] [endPoint]
   前开后闭 区间 这里的 [startPonit] 是指需要合并的commit的前一个commit
   举例：git rebase -i b3f8169
2. git rebase -i HEAD~2
3. git rebase --continue
4. git rebase --abort
   终止rebase

（-i 参数表示交互（interactive），该命令会进入到一个交互界面）
```

### git checkout

```
1. git checkout
　　表示核查工作区相对于版本库修改过的文件
2. git checkout  + 分支名 
　　表示切换分支
3. git checkout  -b  分支名
　　表示以当前分支的当前状态创建新分支并切换到新分支    -b 表示创建新分支
4. git checkout -b 分支名  commitID
　　表示以当前分支的commitID提交节点创建新的分支并切换到新分支。此时工作区的内容和切换分之前commitID提交节点的内容一样
5. git checkout  commitID
　　是以指定的提交节点创建了一个临时性分支，此临时性分支可用于做实验性修改
6．git checkout 　filename 
　　放弃工作区中某个文件的修改
   git checkout  . 
　　放弃工作区中全部的修改
7. git checkout <commit> filename 
　　当有提交版本号时，表示将工作区和暂存区都恢复到版本库指定提交版本的指定文件的状态,此时HEAD指针不变，此时的状态相当于把工作区的内容修改到指定版本的文件内容后，再把修改的内容添加到暂存区。因此git checkout <commit> filename后，可以直接执行git commit而不需要先执行git add
```

### git stash

```
1. git stash
   将所有未提交的修改（工作区和暂存区）保存至堆栈中，用于后续恢复当前工作目录
2. git stash list
3. git stash pop
4. git stash clear
```

