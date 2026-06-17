# GitHub 挂钩与进度可追溯需求

## 目标

本项目必须接入个人 GitHub 仓库，并通过阶段性 Git 提交保存开发进度，确保每次重要变更都可以追踪、回退和审查。

## 需求 1：项目与 GitHub 挂钩

- 项目目录必须初始化为 Git 仓库。
- Git 远端必须配置为 `origin`，指向个人 GitHub 仓库。
- 默认分支使用 `main`。
- 首次完成配置后，需要将本地提交推送到 GitHub。

当前建议远端：

```text
https://github.com/Hacker-zzzzz/BRI-GDELT-System.git
```

如果 GitHub 上还没有该仓库，需要先创建同名仓库，然后执行：

```powershell
git push -u origin main
```

## 需求 2：进度自动保存

- 每完成一个明确阶段，例如功能完成、文档更新、Bug 修复、测试通过后，必须执行一次 checkpoint 保存。
- checkpoint 保存必须生成 Git 提交，提交信息应包含时间戳和简短说明。
- 如果没有文件变更，不应生成空提交。
- 自动保存不替代最终整理提交；它用于保证过程可追溯。

推荐命令：

```powershell
.\scripts\checkpoint.ps1 -Message "完成阶段说明"
```

如果当前 Windows 执行策略阻止运行 `.ps1`，使用包装命令：

```powershell
.\scripts\checkpoint.cmd "完成阶段说明"
```

## 验收标准

- `git status --short --branch` 能显示当前位于 `main` 分支。
- `git remote -v` 能显示 `origin` 指向 GitHub 仓库。
- 执行 checkpoint 脚本后，若存在变更，会生成一条新的 Git 提交。
- `git log --oneline --decorate -5` 能看到最近的阶段性保存记录。
