# Git 历史清理策略

## 为什么需要清理

Git 历史中可能包含：
- 内部邮箱地址（@tsign.cn, @timevale.cn）
- 内部域名和 IP
- 内部 Git 仓库 URL
- 开发者真实姓名
- 敏感的 commit message

这些信息一旦推送到 GitHub，即使后续删除也能通过历史找回。

## 策略对比

### 策略 1: Orphan Branch Squash（推荐）

将所有代码压缩为单个全新 commit，完全丢弃历史。

**优点：**
- 最彻底，历史完全干净
- 操作简单，不易出错
- 无信息泄露风险

**缺点：**
- 丢失所有开发历史
- 无法追溯代码演进

**适用场景：**
- 内部开发历史不适合公开
- 代码经过大量重构
- 首次开源

**操作步骤：**
```bash
# 1. 创建孤立分支
git checkout --orphan fresh-start

# 2. 暂存所有文件
git add -A

# 3. 创建干净的初始提交
git commit -m "feat: initial release of DeepCover - Java code coverage collection agent"

# 4. 推送到远程（需要 force push）
git push origin fresh-start:main --force
```

**注意事项：**
- `git add -A` 会包含所有文件，包括 .gitignore 中忽略的文件
- 必须检查暂存区，移除不应开源的文件（CLAUDE.md、内部文档等）
- 使用 `git rm --cached` 从暂存区移除已忽略的文件

### 策略 2: git filter-branch / git-filter-repo

保留部分历史，但重写敏感信息。

**优点：**
- 保留代码演进历史
- 可以精确替换特定字符串

**缺点：**
- 操作复杂
- 容易遗漏
- 处理大仓库耗时长

**适用场景：**
- 需要保留开发历史
- 历史信息相对干净

**操作步骤（git-filter-repo）：**
```bash
# 安装
pip install git-filter-repo

# 替换邮箱
git filter-repo --email-callback '
    email = email.replace(b"@tsign.cn", b"@users.noreply.github.com")
    email = email.replace(b"@timevale.cn", b"@users.noreply.github.com")
    return email
'

# 替换 commit message 中的敏感信息
git filter-repo --message-callback '
    message = message.replace(b"tsign.cn", b"***")
    return message
'
```

### 策略 3: 浅克隆

仅保留最近 N 个 commit。

**适用场景：** 中等敏感程度，最近历史较干净。

## 推荐流程

对于 DeepCover 系列项目，**推荐策略 1（Orphan Branch Squash）**：

1. 在原仓库完成所有代码修改（品牌替换、脱敏等）
2. 确认所有修改完成且编译测试通过
3. 创建 orphan 分支
4. 仔细检查暂存区内容
5. 创建干净的初始提交
6. 推送到 GitHub

## 清理后验证

```bash
# 检查作者邮箱
git log --all --format='%ae' | sort -u
# 预期：只包含 GitHub 邮箱或 noreply 邮箱

# 检查提交者邮箱
git log --all --format='%ce' | sort -u

# 检查 commit message
git log --all --oneline
# 预期：无内部域名、IP、项目名

# 检查文件内容（确保无内部文件混入）
git ls-files | grep -i "claude\|agent\|oss-\|内部\|分析报告"
# 预期：空
```
