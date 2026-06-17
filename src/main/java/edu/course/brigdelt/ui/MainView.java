package edu.course.brigdelt.ui;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.ImportResult;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.service.EventQueryService;
import edu.course.brigdelt.service.GdeltImportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
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

        Label subtitle = new Label("v0.3 查询检索 · JavaFX + Maven + SQLite");
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

        Label hint = new Label("数据导入已接入，其余模块保留答辩展示占位。");
        hint.getStyleClass().add("sidebar-hint");
        hint.setWrapText(true);

        sidebar.getChildren().addAll(sectionTitle, modules, hint);
        return sidebar;
    }

    private void showPage(PageSpec page) {
        Parent content = switch (page.title()) {
            case "首页仪表盘" -> createDashboardPage(page);
            case "数据导入" -> createImportPage(page);
            case "事件查询" -> createEventQueryPage(page);
            default -> createPlaceholderPage(page);
        };
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

    private Parent createImportPage(PageSpec page) {
        VBox body = createPageBase(page.title(), page.description());

        VBox importBox = new VBox(14);
        importBox.getStyleClass().add("import-box");

        Label fileLabel = new Label("导入文件");
        fileLabel.getStyleClass().add("form-label");

        TextField filePathField = new TextField();
        filePathField.setPromptText("请选择或输入 GDELT 数据文件路径（csv、tsv、zip）");
        HBox.setHgrow(filePathField, Priority.ALWAYS);

        Button chooseButton = new Button("选择文件");
        chooseButton.getStyleClass().add("secondary-button");

        Button importButton = new Button("开始导入");
        importButton.getStyleClass().add("primary-button");

        HBox fileRow = new HBox(10, filePathField, chooseButton, importButton);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar();
        progressBar.getStyleClass().add("import-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        Label statusText = new Label("请选择文件后开始导入。");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        TextArea resultSummary = new TextArea("暂无导入结果。");
        resultSummary.getStyleClass().add("result-summary");
        resultSummary.setEditable(false);
        resultSummary.setWrapText(true);
        resultSummary.setPrefRowCount(8);

        TextArea errorSamples = new TextArea("暂无错误样例。");
        errorSamples.getStyleClass().add("error-samples");
        errorSamples.setEditable(false);
        errorSamples.setWrapText(true);
        errorSamples.setPrefRowCount(8);

        chooseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择 GDELT 导入文件");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("GDELT 文件", "*.csv", "*.CSV", "*.tsv", "*.zip"),
                    new FileChooser.ExtensionFilter("CSV 文件", "*.csv", "*.CSV"),
                    new FileChooser.ExtensionFilter("TSV 文件", "*.tsv"),
                    new FileChooser.ExtensionFilter("ZIP 压缩包", "*.zip")
            );
            File selectedFile = fileChooser.showOpenDialog(currentWindow());
            if (selectedFile != null) {
                filePathField.setText(selectedFile.toPath().toString());
                statusText.setText("已选择文件：" + selectedFile.getName());
            }
        });

        importButton.setOnAction(event -> {
            String rawPath = filePathField.getText() == null ? "" : filePathField.getText().trim();
            if (rawPath.isEmpty()) {
                statusText.setText("请先选择或输入要导入的文件路径。");
                return;
            }

            Path selectedPath = Path.of(rawPath);
            Task<ImportResult> importTask = new Task<>() {
                @Override
                protected ImportResult call() {
                    return new GdeltImportService(new DatabaseManager(paths)).importFile(selectedPath);
                }
            };

            chooseButton.setDisable(true);
            importButton.setDisable(true);
            filePathField.setDisable(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            statusText.setText("正在后台导入，请稍候...");
            resultSummary.setText("导入任务运行中。");
            errorSamples.setText("暂无错误样例。");

            importTask.setOnSucceeded(workerEvent -> {
                ImportResult result = importTask.getValue();
                statusText.setText("导入完成：" + result.displaySummary());
                resultSummary.setText(formatImportResult(result));
                errorSamples.setText(formatErrorSamples(result));
                restoreImportControls(chooseButton, importButton, filePathField, progressBar);
            });
            importTask.setOnFailed(workerEvent -> {
                Throwable exception = importTask.getException();
                statusText.setText("导入失败：" + (exception == null ? "未知错误" : exception.getMessage()));
                resultSummary.setText("导入任务异常终止，未返回导入结果。");
                errorSamples.setText(exception == null ? "无异常详情。" : exception.toString());
                restoreImportControls(chooseButton, importButton, filePathField, progressBar);
            });

            Thread thread = new Thread(importTask, "gdelt-import-task");
            thread.setDaemon(true);
            thread.start();
        });

        importBox.getChildren().addAll(fileLabel, fileRow, progressBar, statusText);

        HBox resultRow = new HBox(14);
        resultRow.getStyleClass().add("import-result-row");
        VBox summaryBox = createTextPanel("导入结果摘要", resultSummary);
        VBox errorBox = createTextPanel("错误样例", errorSamples);
        resultRow.getChildren().addAll(summaryBox, errorBox);
        HBox.setHgrow(summaryBox, Priority.ALWAYS);
        HBox.setHgrow(errorBox, Priority.ALWAYS);

        body.getChildren().addAll(importBox, resultRow);
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

    private Parent createEventQueryPage(PageSpec page) {
        VBox body = createPageBase(page.title(), page.description());

        GridPane form = new GridPane();
        form.getStyleClass().add("query-form");
        form.setHgap(12);
        form.setVgap(12);

        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField anyCountryField = new TextField();
        anyCountryField.setPromptText("如 CHN");
        TextField actor1Field = new TextField();
        actor1Field.setPromptText("Actor1");
        TextField actor2Field = new TextField();
        actor2Field.setPromptText("Actor2");

        ComboBox<EventTypeOption> eventTypeBox = new ComboBox<>();
        eventTypeBox.getItems().addAll(
                new EventTypeOption("全部", null),
                new EventTypeOption("合作", EventType.COOPERATION),
                new EventTypeOption("冲突", EventType.CONFLICT),
                new EventTypeOption("其他", EventType.OTHER)
        );
        eventTypeBox.getSelectionModel().selectFirst();

        Button searchButton = new Button("查询");
        searchButton.getStyleClass().add("primary-button");
        Button clearButton = new Button("清空");
        clearButton.getStyleClass().add("secondary-button");

        addFormField(form, 0, 0, "开始日期", startDatePicker);
        addFormField(form, 0, 1, "结束日期", endDatePicker);
        addFormField(form, 0, 2, "任一国家", anyCountryField);
        addFormField(form, 1, 0, "Actor1", actor1Field);
        addFormField(form, 1, 1, "Actor2", actor2Field);
        addFormField(form, 1, 2, "事件类型", eventTypeBox);
        form.add(new HBox(10, searchButton, clearButton), 3, 0, 1, 2);

        Label statusText = new Label("设置筛选条件后点击查询。默认最多显示 500 条。");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        TableView<EventQueryResult> table = createEventTable();
        ObservableList<EventQueryResult> tableItems = FXCollections.observableArrayList();
        table.setItems(tableItems);

        searchButton.setOnAction(event -> {
            EventTypeOption selectedType = eventTypeBox.getSelectionModel().getSelectedItem();
            EventQueryCriteria criteria = new EventQueryCriteria(
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    anyCountryField.getText(),
                    actor1Field.getText(),
                    actor2Field.getText(),
                    selectedType == null ? null : selectedType.type(),
                    EventQueryCriteria.DEFAULT_LIMIT
            );
            Task<List<EventQueryResult>> queryTask = new Task<>() {
                @Override
                protected List<EventQueryResult> call() {
                    return new EventQueryService(new DatabaseManager(paths)).search(criteria);
                }
            };
            searchButton.setDisable(true);
            clearButton.setDisable(true);
            statusText.setText("正在查询，请稍候...");
            queryTask.setOnSucceeded(workerEvent -> {
                List<EventQueryResult> results = queryTask.getValue();
                tableItems.setAll(results);
                statusText.setText(results.isEmpty()
                        ? "未查询到符合条件的事件。"
                        : "查询完成，共显示 " + results.size() + " 条事件。");
                searchButton.setDisable(false);
                clearButton.setDisable(false);
            });
            queryTask.setOnFailed(workerEvent -> {
                Throwable exception = queryTask.getException();
                statusText.setText("查询失败：" + (exception == null ? "未知错误" : exception.getMessage()));
                searchButton.setDisable(false);
                clearButton.setDisable(false);
            });
            Thread thread = new Thread(queryTask, "gdelt-query-task");
            thread.setDaemon(true);
            thread.start();
        });

        clearButton.setOnAction(event -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            anyCountryField.clear();
            actor1Field.clear();
            actor2Field.clear();
            eventTypeBox.getSelectionModel().selectFirst();
            tableItems.clear();
            statusText.setText("筛选条件已清空。");
        });

        body.getChildren().addAll(form, statusText, table);
        VBox.setVgrow(table, Priority.ALWAYS);
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

    private void addFormField(GridPane grid, int row, int column, String labelText, javafx.scene.Node control) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        box.getChildren().addAll(label, control);
        GridPane.setHgrow(box, Priority.ALWAYS);
        grid.add(box, column, row);
    }

    private TableView<EventQueryResult> createEventTable() {
        TableView<EventQueryResult> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("未查询到符合条件的事件。"));

        table.getColumns().add(textColumn("事件ID", "globalEventId", 110));
        table.getColumns().add(textColumn("日期", "eventDate", 95));
        table.getColumns().add(textColumn("Actor1", "actor1CountryCode", 80));
        table.getColumns().add(textColumn("Actor2", "actor2CountryCode", 80));
        table.getColumns().add(textColumn("类型", "eventType", 95));
        table.getColumns().add(textColumn("Root", "eventRootCode", 70));
        table.getColumns().add(textColumn("Goldstein", "goldsteinScale", 90));
        table.getColumns().add(textColumn("Mentions", "numMentions", 90));
        table.getColumns().add(textColumn("AvgTone", "avgTone", 85));
        table.getColumns().add(textColumn("来源文件", "sourceFile", 180));
        table.setMinHeight(360);
        return table;
    }

    private TableColumn<EventQueryResult, Object> textColumn(String title, String property, double width) {
        TableColumn<EventQueryResult, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
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

    private VBox createTextPanel(String titleText, TextArea textArea) {
        VBox box = new VBox(10);
        box.getStyleClass().add("text-panel");

        Label title = new Label(titleText);
        title.getStyleClass().add("placeholder-title");

        VBox.setVgrow(textArea, Priority.ALWAYS);
        box.getChildren().addAll(title, textArea);
        return box;
    }

    private String formatImportResult(ImportResult result) {
        return """
                文件名：%s
                totalRows：%d
                successRows：%d
                skippedRows：%d
                insertedRows：%d
                status：%s
                duration：%d ms
                errorSummary：%s
                """.formatted(
                result.fileName(),
                result.totalRows(),
                result.successRows(),
                result.skippedRows(),
                result.insertedRows(),
                result.status(),
                result.durationMillis(),
                result.errorSummary()
        );
    }

    private String formatErrorSamples(ImportResult result) {
        if (result.errorSamples().isEmpty()) {
            return "无错误样例。";
        }
        StringBuilder builder = new StringBuilder();
        List<String> samples = result.errorSamples();
        for (int index = 0; index < samples.size(); index++) {
            builder.append(index + 1).append(". ").append(samples.get(index)).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private void restoreImportControls(Button chooseButton, Button importButton, TextField filePathField,
                                       ProgressBar progressBar) {
        chooseButton.setDisable(false);
        importButton.setDisable(false);
        filePathField.setDisable(false);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
    }

    private Window currentWindow() {
        return contentHost.getScene() == null ? null : contentHost.getScene().getWindow();
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

    private record EventTypeOption(String label, EventType type) {
        @Override
        public String toString() {
            return label;
        }
    }
}
