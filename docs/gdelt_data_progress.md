# GDELT 数据准备进度

更新时间：2026-06-18

## 本地运行期数据位置

```text
D:\Temp\BRI-GDELT-System\input
D:\Temp\BRI-GDELT-System\database\bri_gdelt.db
```

## 当前已完成

- 已新增真实 GDELT 数据下载脚本：`scripts/download-gdelt-data.ps1`
- 已新增真实 GDELT 数据导入脚本：`scripts/import-gdelt-input.ps1`
- 已新增导入检查入口：`edu.course.brigdelt.GdeltInputDataImportCheck`
- 已新增真实数据说明：`docs/real_gdelt_data_guide.md`
- `.gitignore` 已忽略 `*.zip`，避免把 GDELT 原始压缩包提交到 Git

## 已下载数据

当前 `input` 目录已有：

```text
zip 文件数：1311
总大小：约 102 MB
最早文件：20250601000000.export.CSV.zip
最晚文件：20250614174500.export.CSV.zip
```

其中：

- 2025-06-01 到 2025-06-07：已完整下载 7 天，共 672 个 15 分钟文件
- 2025-06-08 到 2025-06-14：已下载大部分文件
- GDELT 服务器对部分 2025-06-12 和 2025-06-14 晚间文件返回 404/502，后续可重跑脚本补齐；已存在文件会自动跳过

## 已导入数据库

已导入 2025-06-01 到 2025-06-07 的 672 个文件。

导入后统计：

```text
GDELT 事件总数：169544
合作事件：71308
冲突事件：29309
```

2025-06-08 到 2025-06-14 已下载的大部分文件尚未导入。

## 明天建议继续

1. 先补下载 2025-06-15 到 2025-06-21，建议按天执行，避免单次命令时间过长。

```powershell
scripts/download-gdelt-data.ps1 -Dates 20250615
scripts/download-gdelt-data.ps1 -Dates 20250616
scripts/download-gdelt-data.ps1 -Dates 20250617
scripts/download-gdelt-data.ps1 -Dates 20250618
scripts/download-gdelt-data.ps1 -Dates 20250619
scripts/download-gdelt-data.ps1 -Dates 20250620
scripts/download-gdelt-data.ps1 -Dates 20250621
```

2. 再下载 2025-06-22 到 2025-06-30。

3. 下载完成后统一导入一次：

```powershell
scripts/import-gdelt-input.ps1
```

4. 如果时间紧，当前 7 天已导入数据已经足够继续做查询、仪表盘、合作/冲突统计和基础趋势展示。

## 下载脚本优化记录

`scripts/download-gdelt-data.ps1` 已优化为：

- 先读取 GDELT 官方 `masterfilelist.txt`
- 只下载官方清单中真实存在的 `.export.CSV.zip`
- 默认缓存官方清单 12 小时，避免每次重复拉取大清单
- 支持 `-ThrottleLimit` 并发下载，默认并发数为 8
- 支持 `-ListOnly` 只查看某个日期范围实际存在多少文件

示例：

```powershell
scripts/download-gdelt-data.ps1 -Dates 20250601 -ListOnly
scripts/download-gdelt-data.ps1 -Dates 20250601,20250602 -ThrottleLimit 8
scripts/download-gdelt-data.ps1 -Dates 20250601 -RefreshMasterList
```

注意：官方清单显示 `20250615` 和 `20250616` 当前没有对应的 Event 文件，因此这些日期不应继续硬跑枚举下载。
