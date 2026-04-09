# 踩坑记录与经验总结

> 基于 DeepCover 采集器 Agent 开源实践，持续更新。

## 关键教训

### 1. Git 历史是最大的信息泄露源

**问题：** 即使代码中已替换所有内部信息，Git 历史中仍然保留了：
- 内部邮箱（wuchen@tsign.cn, yingzhu@tsign.cn 等）
- 内部 Git URL（git@git.timevale.cn）
- 开发者真实姓名
- 内部项目代号

**解决方案：** 使用 orphan branch squash 创建全新历史。

**教训：** 代码修改完成后，在推送到 GitHub 之前，必须先做 Git 历史审计。

### 2. .gitignore 的时机问题

**问题：** 修改了 .gitignore 添加 `CLAUDE.md`，但文件已经被 Git 跟踪，所以修改 .gitignore 后文件仍在仓库中。

**解决方案：** 需要 `git rm --cached` 从 Git 索引中移除已跟踪的文件。

```bash
git rm --cached CLAUDE.md AGENTS.md
git commit -m "chore: remove internal files from repository"
```

**教训：** .gitignore 只影响未跟踪的文件。已跟踪的文件需要额外操作。

### 3. git add -A 会包含 .gitignore 中的文件

**问题：** 创建 orphan 分支后执行 `git add -A`，CLAUDE.md、AGENTS.md 等文件被包含在提交中。

**解决方案：** 提交前检查 `git status`，使用 `git rm --cached` 移除不应提交的文件。

**教训：** orphan 分支不继承 .gitignore 的效果（虽然文件存在，但 add -A 会全部暂存）。

### 4. 中文文件名的 Git 操作问题

**问题：** 使用 `git rm --cached` 删除中文文件名文件时，编码不匹配导致找不到文件。

**解决方案：**
- 目录级别的删除用 `git rm --cached -r docs/`
- 单个文件用 `git ls-files --cached` 查看编码后的文件名再操作

### 5. Edit 工具的精确匹配

**问题：** 使用 Edit 工具替换代码时，由于 tab/空格混用或编码问题，old_string 匹配失败。

**解决方案：**
- 先用 Read 工具读取文件的精确内容
- 用 python 脚本执行替换（处理编码更可靠）
- 或使用 `cat -A` 查看不可见字符

### 6. SkyWalking 兼容性必须在文档中说明

**问题：** DeepCover 与 SkyWalking APM 存在加载顺序冲突，用户容易踩坑。

**解决方案：** 在 README 和部署指南中明确标注加载顺序要求。

### 7. 沙盒二进制的处理

**问题：** JVM Sandbox 的 JAR 文件是编译依赖，但自身不开源。

**解决方案：**
- sandbox/ 目录加入 .gitignore
- README 中说明获取方式（指向官方仓库）
- pom.xml 中用 system scope 引用本地 JAR

### 8. 多语言文档的维护成本

**问题：** 6 种语言的文档需要同步更新，容易遗漏。

**建议：**
- 以中文为主文档，其他语言为翻译
- 每次更新中文文档时，列出变更点，便于翻译同步
- 技术术语、代码块不翻译，减少出错概率

## 最佳实践

1. **先审计再动手** — 运行安全扫描后再开始代码修改
2. **分支策略** — 在独立分支上完成所有修改，确认无误后再合并
3. **频繁编译验证** — 每次大的替换后立即编译测试
4. **渐进式提交** — 按功能分阶段提交，便于回滚
5. **最终验收** — 推送到 GitHub 前，clone 到新目录验证完整性

## 经验数据

DeepCover 采集器开源统计数据：
- Java 源文件：34 个
- 测试用例：52 个
- 文档文件：6 (README) + 12 (用户手册) + 6 (demo) = 24 个
- 社区文件：7 个
- 总修改文件：59 个
- 新增代码行数：~1900 行
- 总耗时：约 3 个工作日（含踩坑调试时间）

数据中心和分析中心预计：
- 代码量更大，依赖更复杂
- 信息安全审计范围更广
- 建议预留 5-7 个工作日/模块
