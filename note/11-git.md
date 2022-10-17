```sh
git log --pretty=short --oneline
```





```
git remote add upstream https://gitlab.dev.21vianet.com/sbg2-tenxcloud/tdsf/service-mesh.git

每次请求合并前：
git fetch upstream main
git merge upstream/main

git config --global user.name "tangming"
git config --global user.email  "tang.ming@sina.com"

Rebase操作：
git rebase -i b3f8169
git rebase --continue
git push -f

git rebase --interactive HEAD~2

回滚：
git reset --hard  66b9f9481b45b562a1949898504f74e3695a
git push -f

终止rebase：git rebase --abort
修改提交信息：git commit --amend
```



### git reabase

```
1. git rebase -i [startPonit] [endPoint]
   前开后闭 区间 这里的 [startPonit] 是指需要合并的commit的前一个commit
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
　　当没有提交版本号时将工作区的指定文件的内容恢复到暂存区的状态
     git checkout  . 
　　将工作区的所有文件的内容恢复到暂存区的状态
7. git checkout <commit> filename 
　　当有提交版本号时，表示将工作区和暂存区都恢复到版本库指定提交版本的指定文件的状态,此时HEAD指针不变，此时的状态相当于把工作区的内容修改到指定版本的文件内容后，再把修改的内容添加到暂存区。因此git checkout <commit> filename后，可以直接执行git commit而不需要先执行git add
```

