# GDELT 数据准备进度

更新时间：2026-06-18

## 本地运行期数据位置

```text
D:\Temp\BRI-GDELT-System\input
D:\Temp\BRI-GDELT-System\database\bri_gdelt.db
D:\Temp\BRI-GDELT-System\exports
D:\Temp\BRI-GDELT-System\reports
```

## 当前已完成

- 已完成真实 GDELT 数据下载，原始 zip 文件保存在 `input` 目录。
- 已完成真实 GDELT 数据导入，SQLite 数据库保存在 `database\bri_gdelt.db`。
- 已生成最终导出结果：TXT 汇总报告、合作排名 CSV、风险排名 CSV。
- `.gitignore` 已忽略 `.zip`、`.db`、`.sqlite` 等运行期大文件。

## 已下载数据

当前 `input` 目录已有：

```text
zip 文件数：2934
总大小：约 240 MB
最早文件：20250601000000.export.CSV.zip
最晚文件：20250718234500.export.CSV.zip
```

下载脚本已改为读取 GDELT 官方 `masterfilelist.txt`，只下载官方清单中真实存在的 `.export.CSV.zip` 文件，避免无效枚举请求。

## 已导入数据库

最新启动自检统计：

```text
国家配置数量：35
GDELT 事件总数：733018
合作事件：303893
冲突事件：127992
其他事件：301133
导入批次数量：3612
```

说明：

- `gdelt_events.global_event_id` 为主键，重复导入不会导致事件数量翻倍。
- 导入批次多为 `PARTIAL`，原因是系统会过滤非一带一路相关事件，被过滤或格式异常的行会计入跳过行。
- 事件日期使用 GDELT 原始 `SQLDATE` 字段，可能与文件名时间戳不完全相同。

## 最新导出结果

本轮已生成：

```text
D:\Temp\BRI-GDELT-System\reports\bri_gdelt_snapshot_20260618_113048.txt
D:\Temp\BRI-GDELT-System\exports\cooperation_rankings_20260618_113048.csv
D:\Temp\BRI-GDELT-System\exports\risk_rankings_20260618_113048.csv
```

这些文件可作为 PPT 和实验报告中的数据来源。

## 下载脚本能力

`scripts/download-gdelt-data.ps1` 支持：

- 读取官方 `masterfilelist.txt`
- 缓存官方清单，默认 12 小时
- 并发下载，默认并发数为 8
- `-ListOnly` 查看某日期实际存在的文件
- 已存在文件自动跳过

示例：

```powershell
scripts/download-gdelt-data.ps1 -Dates 20250601 -ListOnly
scripts/download-gdelt-data.ps1 -Dates 20250601,20250602 -ThrottleLimit 8
scripts/download-gdelt-data.ps1 -Dates 20250601 -RefreshMasterList
```
