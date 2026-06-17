package edu.course.brigdelt.ui;

import edu.course.brigdelt.config.AppPaths;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Builds the v0.1 JavaFX application shell.
 */
public class MainView {

    private final AppPaths paths;
    private final StackPane contentHost = new StackPane();

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
        HBox header = new HBox(16);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("一带一路沿线国家合作态势分析系统");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("v0.1 展示骨架 · JavaFX + Maven + SQLite");
        subtitle.getStyleClass().add("app-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label("启动完成");
        status.getStyleClass().add("status-pill");

        header.getChildren().addAll(titleBox, spacer, status);
        return header;
    }

    private Parent createWorkspace() {
        BorderPane workspace = new BorderPane();
        workspace.getStyleClass().add("workspace");
        workspace.setLeft(createNavigation());
        workspace.setCenter(contentHost);
        return workspace;
    }

    private Parent createNavigation() {
        VBox sidebar = new VBox(14);
        sidebar.getStyleClass().add("sidebar");

        Label sectionTitle = new Label("功能模块");
        sectionTitle.getStyleClass().add("section-title");

        List<PageSpec> pages = List.of(
                new PageSpec("首页仪表盘", "展示系统启动状态、运行路径和下一步建设重点。"),
                new PageSpec("数据导入", "预留 GDELT 文件选择、导入校验、进度监控和批次记录。"),
                new PageSpec("事件查询", "预留按时间、国家、事件类型和关键词检索事件的查询区。"),
                new PageSpec("双边关系", "预留双边国家选择、关系指标概览和事件明细联动。"),
                new PageSpec("合作态势分析", "预留合作热度、趋势变化、主题分布和对比分析组件。"),
                new PageSpec("风险评估", "预留风险指数、异常事件、预警等级和处置建议展示。"),
                new PageSpec("专题地图", "预留沿线国家空间分布、事件聚合和专题图层控制。"),
                new PageSpec("结果导出", "预留报告、图表、查询结果和分析结论导出入口。")
        );

        ListView<PageSpec> modules = new ListView<>(FXCollections.observableArrayList(pages));
        modules.getStyleClass().add("navigation-list");
        modules.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PageSpec item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
            }
        });
        modules.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showPage(newValue);
            }
        });
        modules.getSelectionModel().selectFirst();
        VBox.setVgrow(modules, Priority.ALWAYS);

        Label hint = new Label("占位界面用于答辩演示，不连接真实业务数据。");
        hint.getStyleClass().add("sidebar-hint");
        hint.setWrapText(true);

        sidebar.getChildren().addAll(sectionTitle, modules, hint);
        return sidebar;
    }

    private void showPage(PageSpec page) {
        Parent content = "首页仪表盘".equals(page.title())
                ? createDashboardPage(page)
                : createPlaceholderPage(page);
        contentHost.getChildren().setAll(content);
    }

    private Parent createDashboardPage(PageSpec page) {
        VBox body = createPageBase(page.title(), page.description());

        HBox cards = new HBox(14);
        cards.getStyleClass().add("summary-row");
        cards.getChildren().addAll(
                createSummaryCard("运行状态", "已完成初始化", "运行目录、配置目录与数据库目录已就绪"),
                createSummaryCard("数据层", "SQLite", "数据库文件路径已生成，等待导入任务写入"),
                createSummaryCard("配置", "国家清单", "国家配置文件用于后续国家过滤与映射")
        );

        GridPane pathGrid = createPathGrid();
        VBox placeholder = createComponentPlaceholder("后续首页组件",
                "导入批次概览、事件数量统计、合作热度趋势、风险提示摘要");

        body.getChildren().addAll(cards, pathGrid, placeholder);
        return wrapScrollable(body);
    }

    private Parent createPlaceholderPage(PageSpec page) {
        VBox body = createPageBase(page.title(), page.description());
        body.getChildren().addAll(
                createComponentPlaceholder("筛选与操作区", componentHintsFor(page.title())[0]),
                createComponentPlaceholder("核心展示区", componentHintsFor(page.title())[1]),
                createComponentPlaceholder("明细与说明区", componentHintsFor(page.title())[2])
        );
        return wrapScrollable(body);
    }

    private VBox createPageBase(String titleText, String descriptionText) {
        VBox body = new VBox(18);
        body.getStyleClass().add("content");

        VBox heading = new VBox(6);
        Label title = new Label(titleText);
        title.getStyleClass().add("content-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("content-description");
        description.setWrapText(true);
        heading.getChildren().addAll(title, description);

        Separator separator = new Separator();
        body.getChildren().addAll(heading, separator);
        return body;
    }

    private Parent wrapScrollable(VBox body) {
        ScrollPane scrollPane = new ScrollPane(body);
        scrollPane.getStyleClass().add("content-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scrollPane;
    }

    private HBox createSummaryCard(String labelText, String valueText, String detailText) {
        HBox card = new HBox(12);
        card.getStyleClass().add("summary-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(4);
        Label label = new Label(labelText);
        label.getStyleClass().add("summary-label");

        Label value = new Label(valueText);
        value.getStyleClass().add("summary-value");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("summary-detail");
        detail.setWrapText(true);
        text.getChildren().addAll(label, value, detail);

        card.getChildren().add(text);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createComponentPlaceholder(String titleText, String detailText) {
        VBox box = new VBox(10);
        box.getStyleClass().add("placeholder-box");

        Label title = new Label(titleText);
        title.getStyleClass().add("placeholder-title");

        Label detail = new Label(detailText);
        detail.getStyleClass().add("placeholder-detail");
        detail.setWrapText(true);

        box.getChildren().addAll(title, detail);
        return box;
    }

    private GridPane createPathGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("path-grid");
        grid.setHgap(18);
        grid.setVgap(12);
        grid.setAlignment(Pos.TOP_LEFT);

        addPathRow(grid, 0, "运行根目录", paths.rootDir().toString());
        addPathRow(grid, 1, "数据目录", paths.dataDir().toString());
        addPathRow(grid, 2, "数据库文件", paths.databaseFile().toString());
        addPathRow(grid, 3, "国家配置路径", paths.countryConfigFile().toString());
        addPathRow(grid, 4, "导出目录", paths.exportDir().toString());
        addPathRow(grid, 5, "日志目录", paths.logDir().toString());
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
        GridPane.setMargin(label, new Insets(0));
    }

    private String[] componentHintsFor(String title) {
        return switch (title) {
            case "数据导入" -> new String[]{
                    "文件选择、目录扫描、导入参数和国家过滤规则占位。",
                    "导入进度、校验结果、成功失败数量和批次状态占位。",
                    "导入日志、错误样例、批次元数据和重试入口占位。"
            };
            case "事件查询" -> new String[]{
                    "时间范围、国家、事件类型、Goldstein 分值和关键词筛选占位。",
                    "事件列表、分页、排序和重点字段展示占位。",
                    "事件详情、原始字段、来源链接和备注说明占位。"
            };
            case "双边关系" -> new String[]{
                    "源国家、目标国家、时间窗口和指标口径选择占位。",
                    "双边互动强度、合作冲突对比和趋势图占位。",
                    "代表性事件、指标解释和数据质量提示占位。"
            };
            case "合作态势分析" -> new String[]{
                    "区域、国家组、主题类别和统计周期选择占位。",
                    "合作热度趋势、主题分布、国家排名和对比图占位。",
                    "分析结论摘要、异常波动说明和后续研判占位。"
            };
            case "风险评估" -> new String[]{
                    "风险类型、国家范围、预警阈值和时间窗口设置占位。",
                    "风险指数卡片、等级分布、趋势变化和预警列表占位。",
                    "风险事件明细、触发原因和建议措施占位。"
            };
            case "专题地图" -> new String[]{
                    "图层类型、时间范围、事件类型和国家筛选占位。",
                    "沿线国家地图、事件聚合点、热力分布和图例占位。",
                    "区域统计、地图选择结果和专题说明占位。"
            };
            case "结果导出" -> new String[]{
                    "导出对象、文件格式、时间范围和报告模板选择占位。",
                    "导出任务队列、生成状态、文件大小和完成时间占位。",
                    "历史导出记录、保存路径和操作说明占位。"
            };
            default -> new String[]{
                    "页面筛选条件和操作按钮占位。",
                    "主要图表、表格或地图展示占位。",
                    "明细记录、解释说明和后续操作占位。"
            };
        };
    }

    private record PageSpec(String title, String description) {
    }
}
