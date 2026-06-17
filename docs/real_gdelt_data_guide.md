# GDELT 真实数据准备指南

本项目真实业务数据使用 GDELT 2.0 Event 文件，运行期数据统一放在：

```text
D:\Temp\BRI-GDELT-System
```

原始 GDELT 文件放在：

```text
D:\Temp\BRI-GDELT-System\input
```

SQLite 数据库放在：

```text
D:\Temp\BRI-GDELT-System\database\bri_gdelt.db
```

## 下载真实 GDELT 数据

按日期下载 GDELT 2.0 Event 文件。GDELT 2.0 真实文件是 15 分钟粒度；传入 `YYYYMMDD` 时，脚本会展开下载当天 96 个文件：

```powershell
scripts/download-gdelt-data.ps1 -Dates 20250601
```

多天数据可以用逗号分隔：

```powershell
scripts/download-gdelt-data.ps1 -Dates 20250601,20250602,20250603
```

如果只想下载一条 15 分钟记录，可以传入完整时间戳：

```powershell
scripts/download-gdelt-data.ps1 -Dates 20250601000000
```

脚本下载地址格式为：

```text
http://data.gdeltproject.org/gdeltv2/YYYYMMDDHHMMSS.export.CSV.zip
```

已存在文件默认跳过，避免重复下载。网络失败时，可以手动把 zip 放入 `D:\Temp\BRI-GDELT-System\input` 后继续导入。

下载脚本会对单个文件自动重试。若 GDELT 服务器临时返回 502 等错误，可以稍后重新执行同一条命令；已成功下载的文件会跳过，只补未完成的文件。

## 导入数据库

执行：

```powershell
scripts/import-gdelt-input.ps1
```

脚本会先编译项目，然后运行 `edu.course.brigdelt.GdeltInputDataImportCheck`，扫描 `input` 目录并复用现有 `GdeltImportService` 导入 `.CSV`、`.tsv`、`.zip` 文件。

控制台会输出：

- 总文件数
- 成功导入批次数
- GDELT 事件总数
- 合作事件数量
- 冲突事件数量

## 去重规则

`gdelt_events.global_event_id` 是主键，导入使用 `INSERT OR IGNORE`。因此重复导入同一批 GDELT 文件不会让事件数量翻倍。

## Git 边界

`.db`、`.sqlite`、`.zip` 都是运行期数据或大文件，不提交 Git。仓库只保存源码、脚本、配置和说明文档。

验证命令：

```powershell
mvn clean compile
mvn exec:java "-Dexec.mainClass=edu.course.brigdelt.StartupCheck"
git status --short
```
