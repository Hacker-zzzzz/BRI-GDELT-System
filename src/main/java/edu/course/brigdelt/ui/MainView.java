package edu.course.brigdelt.ui;

import edu.course.brigdelt.config.AppPaths;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Builds the stage-1 application shell.
 */
public class MainView {

    private final AppPaths paths;

    public MainView(AppPaths paths) {
        this.paths = paths;
    }

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(createHeader());
        root.setCenter(createWorkspace());
        return root;
    }

    private Parent createHeader() {
        VBox header = new VBox();
        header.getStyleClass().add("app-header");

        Label title = new Label("一带一路沿线国家合作态势分析系统");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("阶段 1：JavaFX + Maven + SQLite 可运行骨架");
        subtitle.getStyleClass().add("app-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private Parent createWorkspace() {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(createNavigation(), createDashboardPlaceholder());
        splitPane.setDividerPositions(0.22);
        return splitPane;
    }

    private Parent createNavigation() {
        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().add("sidebar");

        Label sectionTitle = new Label("功能模块");
        sectionTitle.getStyleClass().add("section-title");

        ListView<String> modules = new ListView<>();
        modules.getItems().addAll(
                "首页仪表盘",
                "数据导入",
                "事件查询",
                "双边关系",
                "合作态势分析",
                "风险评估",
                "专题地图",
                "结果导出"
        );
        modules.getSelectionModel().selectFirst();
        VBox.setVgrow(modules, Priority.ALWAYS);

        sidebar.getChildren().addAll(sectionTitle, modules);
        return sidebar;
    }

    private Parent createDashboardPlaceholder() {
        VBox content = new VBox(18);
        content.getStyleClass().add("content");

        Label title = new Label("系统初始化完成");
        title.getStyleClass().add("content-title");

        Label description = new Label("当前版本已完成运行时目录创建、国家配置模板准备和 SQLite 数据库表结构初始化。");
        description.getStyleClass().add("content-description");

        GridPane grid = createPathGrid();

        TextArea nextSteps = new TextArea("""
                下一阶段建议：
                1. 实现 GDELT 文件解析与导入进度条。
                2. 将国家配置加载为内存索引，用于导入过滤。
                3. 实现事件主表批量写入与导入批次记录。
                """);
        nextSteps.setEditable(false);
        nextSteps.setWrapText(true);
        nextSteps.getStyleClass().add("next-steps");

        content.getChildren().addAll(title, description, grid, nextSteps);
        VBox.setVgrow(nextSteps, Priority.ALWAYS);
        return content;
    }

    private GridPane createPathGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("path-grid");
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_LEFT);

        addPathRow(grid, 0, "运行根目录", paths.rootDir().toString());
        addPathRow(grid, 1, "数据目录", paths.dataDir().toString());
        addPathRow(grid, 2, "数据库文件", paths.databaseFile().toString());
        addPathRow(grid, 3, "导出目录", paths.exportDir().toString());
        addPathRow(grid, 4, "国家配置", paths.countryConfigFile().toString());
        return grid;
    }

    private void addPathRow(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("path-label");

        Label value = new Label(valueText);
        value.getStyleClass().add("path-value");
        value.setWrapText(true);
        GridPane.setHgrow(value, Priority.ALWAYS);

        grid.add(label, 0, row);
        grid.add(value, 1, row);
        GridPane.setMargin(label, new Insets(0, 0, 0, 0));
    }
}
