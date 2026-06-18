# 一带一路沿线国家合作态势分析系统

Java 程序设计期末大作业。系统基于 GDELT 2.0 Event 数据，使用 JavaFX + SQLite 实现一带一路沿线国家事件导入、查询、双边关系分析、合作态势分析、风险评估、专题地图和结果导出。

## 当前功能

- GDELT `.CSV/.tsv/.zip` 文件导入、清洗、国家过滤和批量入库
- 首页仪表盘：事件结构、国家热度、日度趋势和总体研判
- 事件查询：日期、国家代码、事件类型、子区域筛选
- 双边关系：中国与沿线国家合作/冲突结构、趋势和明细
- 合作态势分析：国家合作指数排名和口径说明
- 风险评估：国家风险指数、风险等级和口径说明
- 区域分析：子区域合作、冲突、语调、关注度和风险对比
- 国家聚类：轻量 K-Means 四类聚类结果
- 专题地图：基于 ActionGeo 经纬度的事件空间散点分布
- 结果导出：TXT 汇总报告、合作排名 CSV、风险排名 CSV、XLSX 工作簿

## 运行方式

请先进入项目目录：

```powershell
cd D:\Code\BRI-GDELT-System
mvn javafx:run
```

如果 Maven 插件前缀解析失败，使用完整插件命令：

```powershell
mvn org.openjfx:javafx-maven-plugin:0.0.8:run
```

非界面启动自检：

```powershell
mvn exec:java "-Dexec.mainClass=edu.course.brigdelt.StartupCheck"
```

分析与导出自检：

```powershell
mvn exec:java "-Dexec.mainClass=edu.course.brigdelt.AnalysisCheck"
```

## 运行期目录

所有运行期数据统一放在：

```text
D:\Temp\BRI-GDELT-System
```

主要目录：

```text
input      GDELT 原始 zip/csv/tsv 输入文件
sample     小型样例文件
database   SQLite 数据库 bri_gdelt.db
exports    CSV 导出结果
reports    TXT 报告导出结果
logs       日志目录
cache      缓存目录
```

## PPT 答辩素材建议

推荐按以下顺序准备截图和结果页：

1. 首页仪表盘
2. 事件查询
3. 双边关系
4. 合作态势分析
5. 风险评估
6. 区域分析
7. 国家聚类
8. 专题地图
9. 结果导出

`数据维护` 页面保留补充导入能力，但不建议作为 PPT 重点截图；数据导入、清洗和入库过程可在技术路线或数据流程页说明。

详细 PPT 素材和提交材料说明见 `docs/demo_submission_guide.md`。
