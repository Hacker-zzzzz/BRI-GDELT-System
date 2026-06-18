# v1.0 演示与提交说明

## 运行检查

在项目根目录执行：

```powershell
cd D:\Code\BRI-GDELT-System
mvn clean compile
mvn exec:java "-Dexec.mainClass=edu.course.brigdelt.StartupCheck"
mvn javafx:run
```

如果 `mvn javafx:run` 报插件前缀错误，执行：

```powershell
mvn org.openjfx:javafx-maven-plugin:0.0.8:run
```

## 课堂演示流程

1. 打开首页仪表盘，说明数据规模、合作/冲突结构、国家热度和月度趋势。
2. 打开事件查询，选择国家代码或事件类型，展示底层事件明细可检索。
3. 打开双边关系，输入 `CHN` 与一个沿线国家代码，说明双边合作/冲突占比和趋势。
4. 打开合作态势分析，解释合作指数口径和国家排名。
5. 打开风险评估，解释风险指数口径、风险等级和重点国家。
6. 打开专题地图，展示 ActionGeo 经纬度事件分布。
7. 打开结果导出，生成 TXT 报告和 CSV 排名文件，说明系统形成完整闭环。

## 答辩 PPT 大纲

1. 选题背景：GDELT 数据和一带一路合作态势分析价值。
2. 需求分析：数据导入、查询、双边关系、合作指数、风险评估、可视化、导出。
3. 技术路线：Java 17、JavaFX、SQLite、Maven、GDELT 2.0 Event。
4. 系统架构：UI 层、Service 层、Repository 层、Domain 模型、SQLite 数据库。
5. 数据流程：文件导入、解析清洗、国家过滤、批量入库、指标聚合、图表展示、结果导出。
6. 核心功能演示：按课堂演示流程截图。
7. 指标说明：合作指数、风险指数、Goldstein、AvgTone、NumMentions。
8. 测试结果：编译、自检、数据量、功能页面验证。
9. 总结与分工：完成情况、亮点、不足和后续扩展。

## 实验报告大纲

1. 项目背景与目标
2. 需求分析
3. 总体设计与模块划分
4. 数据库设计与数据目录约定
5. GDELT 数据解析与清洗
6. 关键功能实现
7. 测试过程与结果
8. 问题与解决方案
9. 总结

## 提交材料清单

- 源代码：整个 Maven 项目目录
- 运行说明：`README.md` 与本文件
- 数据说明：`docs/gdelt_data_progress.md`、`docs/real_gdelt_data_guide.md`
- 示例或真实数据：`D:\Temp\BRI-GDELT-System\input` 与 `database\bri_gdelt.db`
- 导出结果：`D:\Temp\BRI-GDELT-System\reports` 与 `exports`
- 实验报告：按实验报告大纲整理
- 答辩 PPT：按 PPT 大纲整理

## 注意事项

- 不要把 GDELT 原始 zip 文件提交到 GitHub。
- 运行程序前先确认当前目录是项目根目录。
- 正在下载数据时，不要删除或移动 `D:\Temp\BRI-GDELT-System\input`。
- 大数据导入耗时较长，课堂演示优先使用已导入数据库。
