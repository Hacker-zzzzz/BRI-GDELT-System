# BRI-GDELT 数据目录约定

本项目运行时固定文件统一放在 `D:\Temp\BRI-GDELT-System`，用于满足课程要求中“固定文件统一放在 D 盘 Temp 下”的约束。仓库只保存源码、配置、文档和少量样例说明，不直接保存运行期数据库、导出结果和日志。

## 目录结构

```text
D:\Temp\BRI-GDELT-System
├─ input\          # 用户放入的原始 GDELT 文件，可为 .CSV/.tsv/.zip
├─ sample\         # 小规模样例数据
├─ database\       # SQLite 数据库文件
├─ exports\        # CSV/Excel 导出结果
├─ reports\        # TXT 分析报告
├─ logs\           # 导入、查询、异常日志
└─ cache\          # 临时缓存文件，可安全清理
```

## 推荐文件命名

```text
database\bri_gdelt.db
sample\YYYYMMDD.export.CSV.zip
exports\cooperation_rankings_YYYYMMDD_HHmmss.csv
exports\risk_rankings_YYYYMMDD_HHmmss.csv
reports\bri_gdelt_snapshot_YYYYMMDD_HHmmss.txt
logs\application_YYYYMMDD.log
```

## 数据边界

- `input` 保存用户手动下载的 GDELT 原始文件。
- `sample` 保存开发和测试用的小规模样例数据。
- `database` 保存程序自动创建和维护的 SQLite 数据库。
- `exports` 与 `reports` 保存用户主动导出的分析结果。
- `logs` 只保存运行日志，不参与业务分析。
- `cache` 只保存可再生成的临时文件。

## 配置文件来源

基础国家配置保存在仓库内：

```text
src\main\resources\config\countries.json
```

程序启动时读取该配置，初始化 `countries` 国家元数据表。后续如果需要达到任务书中的 150+ 沿线国家要求，应在该 JSON 文件中继续补齐国家记录，而不是把国家清单硬编码到 Java 类中。

## GDELT 字段约定

第一阶段只保留本项目分析需要的关键字段：

```text
GlobalEventID
SQLDATE
Actor1CountryCode
Actor2CountryCode
EventCode
EventBaseCode
EventRootCode
GoldsteinScale
NumMentions
AvgTone
ActionGeo_Lat
ActionGeo_Long
```

合作/冲突分类规则：

- `EventRootCode` 为 `04`、`05`、`06`：合作事件
- `EventRootCode` 为 `08` 到 `14`：冲突事件
- 其他编码：其他事件
