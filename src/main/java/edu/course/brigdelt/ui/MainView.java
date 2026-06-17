package edu.course.brigdelt.ui;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.BilateralRelationSummary;
import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.CountryEventStat;
import edu.course.brigdelt.domain.DashboardSummary;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.ImportResult;
import edu.course.brigdelt.domain.MonthlyTrendPoint;
import edu.course.brigdelt.domain.RiskAssessment;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.service.AnalysisService;
import edu.course.brigdelt.service.BilateralRelationService;
import edu.course.brigdelt.service.DashboardService;
import edu.course.brigdelt.service.EventQueryService;
import edu.course.brigdelt.service.GdeltImportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import java.util.function.Function;

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

        Label subtitle = new Label("v0.7 展示增强 · JavaFX + Maven + SQLite");
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

        Label hint = new Label("核心链路已接入：导入、查询、双边关系、合作态势、风险评估。");
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
            case "双边关系" -> createBilateralPage();
            case "合作态势分析" -> createCooperationAnalysisPage();
            case "风险评估" -> createRiskAssessmentPage();
            default -> createPlaceholderPage(page);
        };
        contentHost.getChildren().setAll(content);
    }

    private Parent createDashboardPage(PageSpec page) {
        VBox body = createPageBase(page.title(), page.description());

        Label statusText = new Label("正在加载仪表盘数据...");
        statusText.getStyleClass().add("import-status");

        HBox firstRow = new HBox(14);
        firstRow.getStyleClass().add("summary-row");
        HBox secondRow = new HBox(14);
        secondRow.getStyleClass().add("summary-row");
        Label countryValue = metricValue("0");
        Label eventValue = metricValue("0");
        Label cooperationValue = metricValue("0");
        Label conflictValue = metricValue("0");
        Label importValue = metricValue("0");
        Label mentionValue = metricValue("0");
        Label goldsteinValue = metricValue("0.00");
        Label toneValue = metricValue("0.00");
        firstRow.getChildren().addAll(
                createMetricCard("沿线国家", countryValue, "当前配置国家数", "neutral-card"),
                createMetricCard("事件总量", eventValue, "已入库 GDELT 事件", "neutral-card"),
                createMetricCard("合作事件", cooperationValue, "EventRoot 04/05/06", "positive-card"),
                createMetricCard("冲突事件", conflictValue, "EventRoot 08-14", "negative-card")
        );
        secondRow.getChildren().addAll(
                createMetricCard("导入批次", importValue, "历史导入记录", "neutral-card"),
                createMetricCard("媒体关注度", mentionValue, "NumMentions 总和", "neutral-card"),
                createMetricCard("平均 Goldstein", goldsteinValue, "合作冲突强度", "neutral-card"),
                createMetricCard("平均 AvgTone", toneValue, "媒体语调均值", "neutral-card")
        );

        PieChart typePieChart = new PieChart();
        typePieChart.setTitle("事件类型结构");
        typePieChart.setLegendVisible(true);

        BarChart<String, Number> topCountryChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        topCountryChart.setTitle("国家事件量 TOP8");
        topCountryChart.setLegendVisible(false);

        LineChart<String, Number> monthlyTrendChart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        monthlyTrendChart.setTitle("月度事件趋势");
        monthlyTrendChart.setLegendVisible(false);
        monthlyTrendChart.setCreateSymbols(true);

        Label dashboardInsight = new Label("等待数据加载后生成总体研判。");
        dashboardInsight.getStyleClass().add("insight-text");
        dashboardInsight.setWrapText(true);
        VBox insightPanel = createInsightPanel("总体研判摘要", dashboardInsight);

        HBox chartRow = new HBox(14,
                wrapChart(typePieChart),
                wrapChart(topCountryChart)
        );
        chartRow.getStyleClass().add("chart-row");

        body.getChildren().addAll(statusText, firstRow, secondRow, insightPanel, chartRow, wrapChart(monthlyTrendChart));
        loadDashboard(statusText, countryValue, eventValue, cooperationValue, conflictValue, importValue,
                mentionValue, goldsteinValue, toneValue, dashboardInsight,
                typePieChart, topCountryChart, monthlyTrendChart);
        return wrapScrollable(body);
    }

    private void loadDashboard(Label statusText, Label countryValue, Label eventValue, Label cooperationValue,
                               Label conflictValue, Label importValue, Label mentionValue, Label goldsteinValue,
                               Label toneValue, Label dashboardInsight, PieChart typePieChart,
                               BarChart<String, Number> topCountryChart,
                               LineChart<String, Number> monthlyTrendChart) {
        Task<DashboardViewData> task = new Task<>() {
            @Override
            protected DashboardViewData call() {
                DashboardService service = new DashboardService(new DatabaseManager(paths));
                return new DashboardViewData(
                        service.loadSummary(),
                        service.topCountries(8),
                        service.monthlyTrend()
                );
            }
        };
        task.setOnSucceeded(event -> {
            DashboardViewData data = task.getValue();
            DashboardSummary summary = data.summary();
            countryValue.setText(String.valueOf(summary.countryCount()));
            eventValue.setText(String.valueOf(summary.totalEvents()));
            cooperationValue.setText(String.valueOf(summary.cooperationEvents()));
            conflictValue.setText(String.valueOf(summary.conflictEvents()));
            importValue.setText(String.valueOf(summary.importBatches()));
            mentionValue.setText(String.valueOf(summary.totalMentions()));
            goldsteinValue.setText("%.2f".formatted(summary.averageGoldstein()));
            toneValue.setText("%.2f".formatted(summary.averageAvgTone()));
            updateTypePieChart(typePieChart, summary);
            updateTopCountryChart(topCountryChart, data.topCountries());
            updateMonthlyTrendChart(monthlyTrendChart, data.monthlyTrend());
            dashboardInsight.setText(buildDashboardInsight(summary, data.topCountries(), data.monthlyTrend()));
            statusText.setText(summary.totalEvents() == 0
                    ? "暂无事件数据。请先通过数据导入或演示数据库准备分线写入数据。"
                    : "仪表盘已加载：" + summary.totalEvents() + " 条事件，"
                    + summary.importBatches() + " 个导入批次。");
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            statusText.setText("仪表盘加载失败：" + (exception == null ? "未知错误" : exception.getMessage()));
        });
        Thread thread = new Thread(task, "dashboard-load-task");
        thread.setDaemon(true);
        thread.start();
    }

    private VBox wrapChart(javafx.scene.Node chart) {
        VBox box = new VBox(chart);
        box.getStyleClass().add("chart-panel");
        VBox.setVgrow(chart, Priority.ALWAYS);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox createInsightPanel(String titleText, Node content) {
        VBox box = new VBox(8);
        box.getStyleClass().add("insight-panel");

        Label title = new Label(titleText);
        title.getStyleClass().add("insight-title");
        box.getChildren().addAll(title, content);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox createFormulaPanel(String titleText, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("insight-text");
        label.setWrapText(true);
        VBox panel = createInsightPanel(titleText, label);
        panel.getStyleClass().add("formula-panel");
        return panel;
    }

    private void updateTypePieChart(PieChart chart, DashboardSummary summary) {
        chart.setData(FXCollections.observableArrayList(
                new PieChart.Data("合作", summary.cooperationEvents()),
                new PieChart.Data("冲突", summary.conflictEvents()),
                new PieChart.Data("其他", summary.otherEvents())
        ));
    }

    private void updateTopCountryChart(BarChart<String, Number> chart, List<CountryEventStat> stats) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (CountryEventStat stat : stats) {
            series.getData().add(new XYChart.Data<>(stat.countryCode(), stat.eventCount()));
        }
        chart.getData().setAll(series);
    }

    private void updateMonthlyTrendChart(LineChart<String, Number> chart, List<MonthlyTrendPoint> trends) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (MonthlyTrendPoint point : trends) {
            series.getData().add(new XYChart.Data<>(point.month(), point.totalEvents()));
        }
        chart.getData().setAll(series);
    }

    private String buildDashboardInsight(DashboardSummary summary, List<CountryEventStat> topCountries,
                                         List<MonthlyTrendPoint> monthlyTrend) {
        if (summary.totalEvents() == 0) {
            return "当前数据库尚无事件记录。完成真实 GDELT 数据导入后，首页将自动汇总事件结构、国家热度和月度趋势。";
        }
        String leadingCountry = topCountries.isEmpty() ? "暂无" : topCountries.get(0).countryCode();
        String latestMonth = monthlyTrend.isEmpty() ? "暂无月份" : monthlyTrend.get(monthlyTrend.size() - 1).month();
        double cooperationRatio = summary.totalEvents() == 0 ? 0 : (double) summary.cooperationEvents() / summary.totalEvents();
        double conflictRatio = summary.totalEvents() == 0 ? 0 : (double) summary.conflictEvents() / summary.totalEvents();
        return "当前样本覆盖 " + summary.countryCount() + " 个配置国家，事件热度最高国家为 " + leadingCountry
                + "。合作事件占比 " + formatPercent(cooperationRatio)
                + "，冲突事件占比 " + formatPercent(conflictRatio)
                + "，最新趋势月份为 " + latestMonth
                + "。平均 Goldstein 与 AvgTone 可用于解释总体关系强度和媒体语调。";
    }

    private String buildCooperationInsight(List<CooperationScore> scores) {
        if (scores.isEmpty()) {
            return "暂无合作事件聚合结果。导入真实数据后可比较沿线国家合作热度、媒体关注度和合作质量。";
        }
        CooperationScore top = scores.get(0);
        int totalCooperation = scores.stream().mapToInt(CooperationScore::cooperationEvents).sum();
        return "合作指数最高国家为 " + top.countryCode()
                + "，指数 " + "%.1f".formatted(top.cooperationIndex())
                + "。当前排名国家合计合作事件 " + totalCooperation
                + " 条，可在答辩中用于说明合作热点国家和合作分布差异。";
    }

    private String buildRiskInsight(List<RiskAssessment> risks) {
        if (risks.isEmpty()) {
            return "暂无风险聚合结果。导入真实数据后可根据冲突占比、负向语调和事件强度形成预警排序。";
        }
        RiskAssessment top = risks.get(0);
        long highRiskCountries = risks.stream()
                .filter(risk -> "高".equals(risk.riskLevel()) || "极高".equals(risk.riskLevel()))
                .count();
        return "风险指数最高国家为 " + top.countryCode()
                + "，指数 " + "%.1f".formatted(top.riskIndex())
                + "，等级为" + top.riskLevel()
                + "。当前列表中高风险及以上国家 " + highRiskCountries
                + " 个，可作为后续重点解释和事件溯源对象。";
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

    private Parent createBilateralPage() {
        VBox body = createPageBase("中国与沿线国家双边关系分析", "围绕两个国家之间的 GDELT 事件，展示合作冲突结构、综合态势和月度变化。");

        GridPane form = new GridPane();
        form.getStyleClass().add("query-form");
        form.setHgap(12);
        form.setVgap(12);

        TextField countryAField = new TextField(BilateralRelationService.DEFAULT_COUNTRY_A);
        TextField countryBField = new TextField();
        countryBField.setPromptText("如 KAZ、PAK、KEN");
        Button searchButton = new Button("分析");
        searchButton.getStyleClass().add("primary-button");
        Button clearButton = new Button("清空");
        clearButton.getStyleClass().add("secondary-button");
        addFormField(form, 0, 0, "国家 A", countryAField);
        addFormField(form, 0, 1, "国家 B", countryBField);
        form.add(new HBox(10, searchButton, clearButton), 2, 0);

        Label statusText = new Label("默认以 CHN 为国家 A，输入沿线国家代码后开始分析。");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        HBox firstMetricRow = new HBox(14);
        firstMetricRow.getStyleClass().add("summary-row");
        HBox secondMetricRow = new HBox(14);
        secondMetricRow.getStyleClass().add("summary-row");
        Label totalValue = metricValue("0");
        Label cooperationValue = metricValue("0");
        Label conflictValue = metricValue("0");
        Label cooperationRatioValue = metricValue("0.0%");
        Label conflictRatioValue = metricValue("0.0%");
        Label goldsteinValue = metricValue("0.00");
        Label toneValue = metricValue("0.00");
        Label mentionsValue = metricValue("0");
        firstMetricRow.getChildren().addAll(
                createMetricCard("总事件数", totalValue, "双边互动总量", "neutral-card"),
                createMetricCard("合作事件", cooperationValue, "EventRoot 04/05/06", "positive-card"),
                createMetricCard("冲突事件", conflictValue, "EventRoot 08-14", "negative-card"),
                createMetricCard("合作占比", cooperationRatioValue, "合作事件 / 总事件", "positive-card")
        );
        secondMetricRow.getChildren().addAll(
                createMetricCard("冲突占比", conflictRatioValue, "冲突事件 / 总事件", "negative-card"),
                createMetricCard("平均 Goldstein", goldsteinValue, "关系正负强度", "neutral-card"),
                createMetricCard("平均 AvgTone", toneValue, "媒体语调均值", "neutral-card"),
                createMetricCard("媒体关注度", mentionsValue, "NumMentions 总和", "neutral-card")
        );

        TableView<MonthlyTrendPoint> trendTable = createTrendTable();
        ObservableList<MonthlyTrendPoint> trendItems = FXCollections.observableArrayList();
        trendTable.setItems(trendItems);

        TableView<EventQueryResult> eventTable = createEventTable();
        ObservableList<EventQueryResult> eventItems = FXCollections.observableArrayList();
        eventTable.setItems(eventItems);

        searchButton.setOnAction(event -> {
            String countryA = countryAField.getText();
            String countryB = countryBField.getText();
            Task<BilateralViewData> task = new Task<>() {
                @Override
                protected BilateralViewData call() {
                    BilateralRelationService service = new BilateralRelationService(new DatabaseManager(paths));
                    return new BilateralViewData(
                            service.summarize(countryA, countryB),
                            service.monthlyTrend(countryA, countryB),
                            service.events(countryA, countryB, BilateralRelationService.DEFAULT_EVENT_LIMIT)
                    );
                }
            };
            searchButton.setDisable(true);
            clearButton.setDisable(true);
            statusText.setText("正在分析双边关系，请稍候...");
            task.setOnSucceeded(workerEvent -> {
                BilateralViewData data = task.getValue();
                updateBilateralMetrics(data.summary(), totalValue, cooperationValue, conflictValue,
                        cooperationRatioValue, conflictRatioValue, goldsteinValue, toneValue, mentionsValue);
                trendItems.setAll(data.trends());
                eventItems.setAll(data.events());
                statusText.setText(data.summary().totalEvents() == 0
                        ? "暂无双边事件数据。"
                        : "分析完成：" + data.summary().countryA() + " - " + data.summary().countryB()
                        + " 共 " + data.summary().totalEvents() + " 条事件。");
                searchButton.setDisable(false);
                clearButton.setDisable(false);
            });
            task.setOnFailed(workerEvent -> {
                Throwable exception = task.getException();
                statusText.setText("分析失败：" + (exception == null ? "未知错误" : exception.getMessage()));
                searchButton.setDisable(false);
                clearButton.setDisable(false);
            });
            Thread thread = new Thread(task, "bilateral-analysis-task");
            thread.setDaemon(true);
            thread.start();
        });

        clearButton.setOnAction(event -> {
            countryAField.setText(BilateralRelationService.DEFAULT_COUNTRY_A);
            countryBField.clear();
            trendItems.clear();
            eventItems.clear();
            updateBilateralMetrics(BilateralRelationSummary.empty("CHN", ""), totalValue, cooperationValue,
                    conflictValue, cooperationRatioValue, conflictRatioValue, goldsteinValue, toneValue, mentionsValue);
            statusText.setText("筛选条件已清空。");
        });

        body.getChildren().addAll(form, statusText, firstMetricRow, secondMetricRow,
                createSectionTitle("月度趋势预览"), trendTable,
                createSectionTitle("双边事件明细"), eventTable);
        return wrapScrollable(body);
    }

    private Parent createCooperationAnalysisPage() {
        VBox body = createPageBase("沿线国家合作态势分析", "按国家聚合 GDELT 事件，综合合作事件量、Goldstein、媒体语调和关注度形成合作指数。");

        Label statusText = new Label("正在加载合作态势排名...");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        HBox metricRow = new HBox(14);
        metricRow.getStyleClass().add("summary-row");
        Label countryValue = metricValue("0");
        Label topCountryValue = metricValue("-");
        Label topIndexValue = metricValue("0.0");
        Label totalCooperationValue = metricValue("0");
        metricRow.getChildren().addAll(
                createMetricCard("覆盖国家", countryValue, "有事件记录的国家数", "neutral-card"),
                createMetricCard("合作首位", topCountryValue, "合作指数排名第一", "positive-card"),
                createMetricCard("最高合作指数", topIndexValue, "0-100 综合评分", "positive-card"),
                createMetricCard("合作事件合计", totalCooperationValue, "排名国家合作事件总和", "neutral-card")
        );

        Label insightText = new Label("等待合作排名加载后生成研判。");
        insightText.getStyleClass().add("insight-text");
        insightText.setWrapText(true);
        VBox insightPanel = createInsightPanel("合作态势研判", insightText);
        VBox formulaPanel = createFormulaPanel("合作指数口径",
                "合作指数 = 合作事件量 + 正向 Goldstein + 正向媒体语调 + 媒体关注度 - 冲突扣分，归一到 0-100。该口径适合课堂答辩解释“合作热度”和“合作质量”。");

        TableView<CooperationScore> table = createCooperationTable();
        ObservableList<CooperationScore> items = FXCollections.observableArrayList();
        table.setItems(items);

        Task<List<CooperationScore>> task = new Task<>() {
            @Override
            protected List<CooperationScore> call() {
                return new AnalysisService(new DatabaseManager(paths))
                        .cooperationRankings(AnalysisService.DEFAULT_RANK_LIMIT);
            }
        };
        task.setOnSucceeded(event -> {
            List<CooperationScore> results = task.getValue();
            items.setAll(results);
            updateCooperationMetrics(results, countryValue, topCountryValue, topIndexValue, totalCooperationValue);
            insightText.setText(buildCooperationInsight(results));
            statusText.setText(results.isEmpty()
                    ? "暂无合作态势数据。"
                    : "合作态势排名已加载，共显示 " + results.size() + " 个国家。");
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            statusText.setText("合作态势加载失败：" + (exception == null ? "未知错误" : exception.getMessage()));
        });
        Thread thread = new Thread(task, "cooperation-analysis-task");
        thread.setDaemon(true);
        thread.start();

        body.getChildren().addAll(statusText, metricRow, new HBox(14, insightPanel, formulaPanel),
                createSectionTitle("合作指数国家排名"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return wrapScrollable(body);
    }

    private Parent createRiskAssessmentPage() {
        VBox body = createPageBase("沿线国家风险评估", "按国家聚合冲突事件、冲突占比、Goldstein 和媒体语调，形成便于答辩解释的风险指数。");

        Label statusText = new Label("正在加载风险评估排名...");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        HBox metricRow = new HBox(14);
        metricRow.getStyleClass().add("summary-row");
        Label countryValue = metricValue("0");
        Label topCountryValue = metricValue("-");
        Label topIndexValue = metricValue("0.0");
        Label highRiskValue = metricValue("0");
        metricRow.getChildren().addAll(
                createMetricCard("覆盖国家", countryValue, "有事件记录的国家数", "neutral-card"),
                createMetricCard("风险首位", topCountryValue, "风险指数排名第一", "negative-card"),
                createMetricCard("最高风险指数", topIndexValue, "0-100 综合评分", "negative-card"),
                createMetricCard("高风险国家", highRiskValue, "风险等级为高或极高", "negative-card")
        );

        Label insightText = new Label("等待风险排名加载后生成研判。");
        insightText.getStyleClass().add("insight-text");
        insightText.setWrapText(true);
        VBox insightPanel = createInsightPanel("风险态势研判", insightText);
        VBox formulaPanel = createFormulaPanel("风险指数口径",
                "风险指数 = 冲突占比 + 负向 Goldstein + 负向媒体语调 + 冲突事件量，归一到 0-100，并划分为低、中、高、极高四档。");

        TableView<RiskAssessment> table = createRiskTable();
        ObservableList<RiskAssessment> items = FXCollections.observableArrayList();
        table.setItems(items);

        Task<List<RiskAssessment>> task = new Task<>() {
            @Override
            protected List<RiskAssessment> call() {
                return new AnalysisService(new DatabaseManager(paths))
                        .riskRankings(AnalysisService.DEFAULT_RANK_LIMIT);
            }
        };
        task.setOnSucceeded(event -> {
            List<RiskAssessment> results = task.getValue();
            items.setAll(results);
            updateRiskMetrics(results, countryValue, topCountryValue, topIndexValue, highRiskValue);
            insightText.setText(buildRiskInsight(results));
            statusText.setText(results.isEmpty()
                    ? "暂无风险评估数据。"
                    : "风险评估排名已加载，共显示 " + results.size() + " 个国家。");
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            statusText.setText("风险评估加载失败：" + (exception == null ? "未知错误" : exception.getMessage()));
        });
        Thread thread = new Thread(task, "risk-assessment-task");
        thread.setDaemon(true);
        thread.start();

        body.getChildren().addAll(statusText, metricRow, new HBox(14, insightPanel, formulaPanel),
                createSectionTitle("风险指数国家排名"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
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

    private TableView<MonthlyTrendPoint> createTrendTable() {
        TableView<MonthlyTrendPoint> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无月度趋势数据。"));
        table.getColumns().add(trendColumn("月份", "month", 100));
        table.getColumns().add(trendColumn("总数", "totalEvents", 90));
        table.getColumns().add(trendColumn("合作", "cooperationEvents", 90));
        table.getColumns().add(trendColumn("冲突", "conflictEvents", 90));
        table.getColumns().add(trendColumn("Avg Goldstein", "averageGoldstein", 130));
        table.getColumns().add(trendColumn("Avg Tone", "averageAvgTone", 120));
        table.setMinHeight(180);
        return table;
    }

    private TableView<CooperationScore> createCooperationTable() {
        TableView<CooperationScore> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无合作态势数据。"));
        table.getColumns().add(valueColumn("国家", 90, CooperationScore::countryCode));
        table.getColumns().add(valueColumn("总事件", 90, CooperationScore::totalEvents));
        table.getColumns().add(valueColumn("合作", 90, CooperationScore::cooperationEvents));
        table.getColumns().add(valueColumn("冲突", 90, CooperationScore::conflictEvents));
        table.getColumns().add(valueColumn("Avg Goldstein", 130,
                score -> "%.2f".formatted(score.averageGoldstein())));
        table.getColumns().add(valueColumn("Avg Tone", 110,
                score -> "%.2f".formatted(score.averageAvgTone())));
        table.getColumns().add(valueColumn("Mentions", 100, CooperationScore::totalMentions));
        table.getColumns().add(valueColumn("合作指数", 110,
                score -> "%.1f".formatted(score.cooperationIndex())));
        table.setMinHeight(420);
        return table;
    }

    private TableView<RiskAssessment> createRiskTable() {
        TableView<RiskAssessment> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无风险评估数据。"));
        table.getColumns().add(valueColumn("国家", 90, RiskAssessment::countryCode));
        table.getColumns().add(valueColumn("总事件", 90, RiskAssessment::totalEvents));
        table.getColumns().add(valueColumn("冲突", 90, RiskAssessment::conflictEvents));
        table.getColumns().add(valueColumn("冲突占比", 110,
                risk -> formatPercent(risk.conflictRatio())));
        table.getColumns().add(valueColumn("Avg Goldstein", 130,
                risk -> "%.2f".formatted(risk.averageGoldstein())));
        table.getColumns().add(valueColumn("Avg Tone", 110,
                risk -> "%.2f".formatted(risk.averageAvgTone())));
        table.getColumns().add(valueColumn("风险指数", 110,
                risk -> "%.1f".formatted(risk.riskIndex())));
        table.getColumns().add(valueColumn("风险等级", 100, RiskAssessment::riskLevel));
        table.setMinHeight(420);
        return table;
    }

    private TableColumn<EventQueryResult, Object> textColumn(String title, String property, double width) {
        TableColumn<EventQueryResult, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<MonthlyTrendPoint, Object> trendColumn(String title, String property, double width) {
        TableColumn<MonthlyTrendPoint, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private <T> TableColumn<T, Object> valueColumn(String title, double width, Function<T, Object> mapper) {
        TableColumn<T, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(mapper.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-heading");
        return label;
    }

    private Label metricValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("summary-value");
        return label;
    }

    private HBox createMetricCard(String labelText, Label value, String detailText, String semanticClass) {
        HBox card = createSummaryCard(labelText, value.getText(), detailText);
        value.getStyleClass().add("metric-value");
        VBox textBox = (VBox) card.getChildren().get(0);
        textBox.getChildren().set(1, value);
        card.getStyleClass().add(semanticClass);
        return card;
    }

    private void updateBilateralMetrics(BilateralRelationSummary summary, Label total, Label cooperation,
                                        Label conflict, Label cooperationRatio, Label conflictRatio,
                                        Label goldstein, Label tone, Label mentions) {
        total.setText(String.valueOf(summary.totalEvents()));
        cooperation.setText(String.valueOf(summary.cooperationEvents()));
        conflict.setText(String.valueOf(summary.conflictEvents()));
        cooperationRatio.setText(formatPercent(summary.cooperationRatio()));
        conflictRatio.setText(formatPercent(summary.conflictRatio()));
        goldstein.setText("%.2f".formatted(summary.averageGoldstein()));
        tone.setText("%.2f".formatted(summary.averageAvgTone()));
        mentions.setText(String.valueOf(summary.totalMentions()));
    }

    private void updateCooperationMetrics(List<CooperationScore> scores, Label country, Label topCountry,
                                          Label topIndex, Label totalCooperation) {
        country.setText(String.valueOf(scores.size()));
        if (scores.isEmpty()) {
            topCountry.setText("-");
            topIndex.setText("0.0");
            totalCooperation.setText("0");
            return;
        }
        CooperationScore top = scores.stream()
                .max((left, right) -> Double.compare(left.cooperationIndex(), right.cooperationIndex()))
                .orElse(scores.get(0));
        int cooperationEvents = scores.stream().mapToInt(CooperationScore::cooperationEvents).sum();
        topCountry.setText(top.countryCode());
        topIndex.setText("%.1f".formatted(top.cooperationIndex()));
        totalCooperation.setText(String.valueOf(cooperationEvents));
    }

    private void updateRiskMetrics(List<RiskAssessment> risks, Label country, Label topCountry,
                                   Label topIndex, Label highRisk) {
        country.setText(String.valueOf(risks.size()));
        if (risks.isEmpty()) {
            topCountry.setText("-");
            topIndex.setText("0.0");
            highRisk.setText("0");
            return;
        }
        RiskAssessment top = risks.stream()
                .max((left, right) -> Double.compare(left.riskIndex(), right.riskIndex()))
                .orElse(risks.get(0));
        long highRiskCountries = risks.stream()
                .filter(risk -> "高".equals(risk.riskLevel()) || "极高".equals(risk.riskLevel()))
                .count();
        topCountry.setText(top.countryCode());
        topIndex.setText("%.1f".formatted(top.riskIndex()));
        highRisk.setText(String.valueOf(highRiskCountries));
    }

    private String formatPercent(double ratio) {
        return "%.1f%%".formatted(ratio * 100);
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

    private record BilateralViewData(
            BilateralRelationSummary summary,
            List<MonthlyTrendPoint> trends,
            List<EventQueryResult> events
    ) {
    }

    private record DashboardViewData(
            DashboardSummary summary,
            List<CountryEventStat> topCountries,
            List<MonthlyTrendPoint> monthlyTrend
    ) {
    }
}
