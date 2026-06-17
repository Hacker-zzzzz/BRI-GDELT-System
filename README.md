# 一带一路沿线国家合作态势分析系统

Java 程序设计期末大作业阶段 1 骨架版本。

## 当前范围

- Maven 标准工程结构
- JavaFX 主窗口骨架
- SQLite 数据库初始化
- `D:\Temp\BRI-GDELT-System` 运行时目录约定
- 基础一带一路国家配置模板

## 运行方式

```powershell
mvn javafx:run
```

非界面启动自检：

```powershell
mvn exec:java "-Dexec.mainClass=edu.course.brigdelt.StartupCheck"
```

首次启动会创建以下运行时目录：

```text
D:\Temp\BRI-GDELT-System
  data
  db
  exports
  logs
  config
```

SQLite 数据库文件：

```text
D:\Temp\BRI-GDELT-System\db\bri_gdelt.db
```

国家配置文件：

```text
D:\Temp\BRI-GDELT-System\config\countries.json
```
