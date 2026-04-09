# 社区规范与发布流程

## 社区规范文件

### 1. CODE_OF_CONDUCT.md

采用 Contributor Covenant v2.1，包含：
- 承诺标准
- 责任与权限
- 执行方针
- 归属说明

### 2. SECURITY.md

安全漏洞报告策略：
- 报告方式（GitHub Issues 或指定邮箱）
- 报告内容要求（版本、复现步骤、影响范围）
- 响应时间承诺（如 48 小时内确认，7 天内修复）
- 披露政策

### 3. Issue 模板

#### Bug Report
- Description
- Steps to Reproduce
- Expected Behavior
- Actual Behavior
- Environment（Java 版本、相关组件版本）
- Version
- Additional Context

#### Feature Request
- Problem Description
- Proposed Solution
- Alternatives Considered
- Additional Context

### 4. PR 模板

- Summary（一句话说明）
- Changes（变更列表）
- Test Plan（测试方案）
- Checklist（阅读 CONTRIBUTING、测试通过、文档更新）

### 5. config.yml

Issue 模板配置：
```yaml
blank_issues_enabled: false
contact_links:
  - name: Documentation
    url: https://github.com/{owner}/{repo}#readme
    about: Please check the README first.
```

## 发布流程

### 版本号规范

遵循语义化版本 `MAJOR.MINOR.PATCH`：
- MAJOR：不兼容的 API 变更
- MINOR：向后兼容的功能新增
- PATCH：向后兼容的 Bug 修复

### 发布检查清单

- [ ] 所有测试通过
- [ ] CHANGELOG.md 已更新
- [ ] 版本号已在 pom.xml 中更新
- [ ] Git tag 已创建
- [ ] GitHub Release 已创建
- [ ] Release Asset（JAR）已上传

### GitHub Release 步骤

```bash
# 1. 构建 JAR
mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true

# 2. 创建 GitHub Release
gh release create v1.x.0 \
  target/deepcover-agent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --title "v1.x.0" \
  --notes "## What's New

### New Features
- ...

### Bug Fixes
- ...

**Full Changelog**: https://github.com/{owner}/{repo}/compare/v1.0.0...v1.x.0"
```

### Release Notes 模板

```markdown
## What's New in v1.x.0

### New Features
- Feature description

### Improvements
- Improvement description

### Bug Fixes
- Fix description

### Documentation
- Doc changes

### Breaking Changes (if any)
- Breaking change description and migration guide

**Full Changelog**: https://github.com/{owner}/{repo}/compare/v1.0.0...v1.x.0
```

## 仓库设置

GitHub 仓库推荐设置：

- **可见性**：Public
- **分支保护**：main 分支要求 PR review + CI pass
- **Issues**：启用，使用模板
- **Discussions**：启用（社区讨论）
- **Wiki**：可选（复杂项目推荐）
- **License**：Apache 2.0
