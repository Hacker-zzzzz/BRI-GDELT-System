package edu.course.brigdelt.ui;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.BilateralRelationSummary;
import edu.course.brigdelt.domain.CooperationHotspot;
import edu.course.brigdelt.domain.CooperationScore;
import edu.course.brigdelt.domain.Country;
import edu.course.brigdelt.domain.CountryClusterResult;
import edu.course.brigdelt.domain.CountryEventStat;
import edu.course.brigdelt.domain.CountryMapMetric;
import edu.course.brigdelt.domain.DashboardSummary;
import edu.course.brigdelt.domain.EventQueryCriteria;
import edu.course.brigdelt.domain.EventQueryResult;
import edu.course.brigdelt.domain.EventSubtypeStat;
import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.ExportResult;
import edu.course.brigdelt.domain.GeoEventPoint;
import edu.course.brigdelt.domain.ImportResult;
import edu.course.brigdelt.domain.MonthlyTrendPoint;
import edu.course.brigdelt.domain.RiskAssessment;
import edu.course.brigdelt.domain.RiskHotspot;
import edu.course.brigdelt.domain.RegionSummary;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.service.AnalysisService;
import edu.course.brigdelt.service.BilateralRelationService;
import edu.course.brigdelt.service.DashboardService;
import edu.course.brigdelt.service.EventQueryService;
import edu.course.brigdelt.service.GdeltImportService;
import edu.course.brigdelt.service.MapVisualizationService;
import edu.course.brigdelt.service.ReportExportService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds the JavaFX desktop UI for the BRI GDELT analysis system.
 */
@SuppressWarnings("unchecked")
public class MainView {

    private final AppPaths paths;
    private final StackPane contentHost = new StackPane();
    private final Map<String, Parent> pageCache = new LinkedHashMap<>();

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

        Label subtitle = new Label("v1.0 数据分析版 · GDELT 事件数据 · 本地化分析");
        subtitle.getStyleClass().add("app-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label("启动完成");
        status.getStyleClass().add("status-pill");

        header.getChildren().addAll(createAppIcon(), titleBox, spacer, status);
        return header;
    }

    private StackPane createAppIcon() {
        StackPane icon = new StackPane();
        icon.getStyleClass().add("app-icon");
        icon.setMinSize(56, 56);
        icon.setPrefSize(56, 56);
        icon.setMaxSize(56, 56);

        Rectangle background = new Rectangle(56, 56);
        background.setArcWidth(16);
        background.setArcHeight(16);
        background.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0f766e")),
                new Stop(0.55, Color.web("#145c74")),
                new Stop(1, Color.web("#1f3f6f"))
        ));

        Circle globe = new Circle(28, 25, 17);
        globe.setFill(Color.TRANSPARENT);
        globe.setStroke(Color.web("#d9fff4", 0.75));
        globe.setStrokeWidth(2.1);

        SVGPath latitude = new SVGPath();
        latitude.setContent("M12 25 C20 21 36 21 44 25 M12 31 C20 35 36 35 44 31");
        latitude.setFill(Color.TRANSPARENT);
        latitude.setStroke(Color.web("#d9fff4", 0.45));
        latitude.setStrokeWidth(1.5);

        SVGPath longitude = new SVGPath();
        longitude.setContent("M28 8 C20 18 20 34 28 42 C36 34 36 18 28 8");
        longitude.setFill(Color.TRANSPARENT);
        longitude.setStroke(Color.web("#d9fff4", 0.38));
        longitude.setStrokeWidth(1.4);

        SVGPath route = new SVGPath();
        route.setContent("M10 35 C17 30 21 25 27 27 C33 29 35 19 42 18 C46 17 49 18 52 20");
        route.setFill(Color.TRANSPARENT);
        route.setStroke(new LinearGradient(
                0, 0.7, 1, 0.3, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f7c948")),
                new Stop(0.55, Color.web("#f59e0b")),
                new Stop(1, Color.web("#ef4444"))
        ));
        route.setStrokeWidth(3.7);

        SVGPath chart = new SVGPath();
        chart.setContent("M18 42 L24 39 L30 41 L36 36 L44 40");
        chart.setFill(Color.TRANSPARENT);
        chart.setStroke(Color.web("#bff7ea"));
        chart.setStrokeWidth(2.4);

        StackPane nodes = new StackPane(
                nodeDot(10, 35, 2.8, "#f7c948"),
                nodeDot(27, 27, 2.3, "#fb7185"),
                nodeDot(42, 18, 2.6, "#38bdf8"),
                nodeDot(52, 20, 2.8, "#f59e0b")
        );
        nodes.setMouseTransparent(true);

        icon.getChildren().addAll(background, globe, latitude, longitude, route, chart, nodes);
        return icon;
    }

    private Circle nodeDot(double centerX, double centerY, double radius, String fill) {
        Circle dot = new Circle(radius);
        dot.setFill(Color.web(fill));
        dot.setTranslateX(centerX - 28);
        dot.setTranslateY(centerY - 28);
        return dot;
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
                new PageSpec("首页仪表盘", "汇总事件总量、合作冲突结构、国家热度、日度趋势和总体研判。"),
                new PageSpec("事件查询", "按日期、国家代码和事件类型检索已入库 GDELT 事件。"),
                new PageSpec("双边关系", "分析中国与沿线国家的合作冲突结构、月度趋势和事件明细。"),
                new PageSpec("合作态势分析", "按国家聚合合作事件、媒体关注度和 Goldstein 指标，形成合作指数排名。"),
                new PageSpec("风险评估", "按冲突占比、负向 Goldstein 和媒体语调形成国家风险指数。"),
                new PageSpec("区域分析", "按子区域汇总合作、冲突、语调、关注度和风险指标，支撑区域对比展示。"),
                new PageSpec("国家聚类", "基于合作指数、风险指数、冲突占比和语调特征进行四类 K-Means 聚类。"),
                new PageSpec("专题地图", "基于 ActionGeo 经纬度展示事件空间分布和地理事件明细。"),
                new PageSpec("结果导出", "生成系统分析报告、合作排名 CSV 和风险排名 CSV，便于结果归档和复核。"),
                new PageSpec("数据维护", "用于补充导入 GDELT CSV/TSV/ZIP 文件，支持后续查询、统计分析和数据更新。")
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
        modules.setFixedCellSize(40);
        modules.setPrefHeight(pages.size() * modules.getFixedCellSize() + 2);
        modules.setMaxHeight(Region.USE_PREF_SIZE);

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        Label hint = new Label("分析流程：事件检索、双边关系识别、合作态势评估、风险研判、空间可视化与结果导出。");
        hint.getStyleClass().add("sidebar-hint");
        hint.setWrapText(true);

        sidebar.getChildren().addAll(sectionTitle, modules, sidebarSpacer, hint);
        return sidebar;
    }

    private void showPage(PageSpec page) {
        Parent content = pageCache.computeIfAbsent(page.title(), title -> createPageContent(page));
        contentHost.getChildren().setAll(content);
    }

    private Parent createPageContent(PageSpec page) {
        return switch (page.title()) {
            case "首页仪表盘" -> createDashboardPage(page);
            case "数据维护" -> createImportPage(page);
            case "事件查询" -> createEventQueryPage(page);
            case "双边关系" -> createBilateralPage();
            case "合作态势分析" -> createCooperationAnalysisPage();
            case "风险评估" -> createRiskAssessmentPage();
            case "区域分析" -> createRegionAnalysisPage();
            case "国家聚类" -> createClusterAnalysisPage();
            case "专题地图" -> createInteractiveMapPage();
            case "结果导出" -> createExportPage();
            default -> createPlaceholderPage(page);
        };
    }

    private void invalidateDataPageCache() {
        pageCache.keySet().removeIf(title -> !"数据维护".equals(title));
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
        typePieChart.getStyleClass().add("premium-pie-chart");
        typePieChart.getStyleClass().add("event-type-pie-chart");
        typePieChart.setTitle("事件类型结构");
        typePieChart.setLegendVisible(true);

        CategoryAxis topCountryAxis = new CategoryAxis();
        topCountryAxis.setTickLabelRotation(0);
        BarChart<String, Number> topCountryChart = new BarChart<>(topCountryAxis, new NumberAxis());
        topCountryChart.getStyleClass().add("premium-bar-chart");
        topCountryChart.setTitle("国家事件量 TOP8");
        topCountryChart.setLegendVisible(false);
        topCountryChart.setAnimated(false);

        CategoryAxis dailyAxis = new CategoryAxis();
        dailyAxis.setTickLabelRotation(-25);
        LineChart<String, Number> dailyTrendChart = new LineChart<>(dailyAxis, new NumberAxis());
        dailyTrendChart.setTitle("日度事件趋势");
        dailyTrendChart.setLegendVisible(false);
        dailyTrendChart.setCreateSymbols(true);
        dailyTrendChart.setAnimated(false);

        Label dashboardInsight = new Label("等待数据加载后生成总体研判。");
        dashboardInsight.getStyleClass().add("insight-text");
        dashboardInsight.setWrapText(true);
        VBox insightPanel = createInsightPanel("总体研判摘要", dashboardInsight);

        HBox chartRow = new HBox(14,
                wrapChart(typePieChart),
                wrapChart(topCountryChart)
        );
        chartRow.getStyleClass().add("chart-row");

        body.getChildren().addAll(statusText, firstRow, secondRow, insightPanel, chartRow, wrapChart(dailyTrendChart));
        loadDashboard(statusText, countryValue, eventValue, cooperationValue, conflictValue, importValue,
                mentionValue, goldsteinValue, toneValue, dashboardInsight,
                typePieChart, topCountryChart, dailyTrendChart);
        return wrapScrollable(body);
    }

    private void loadDashboard(Label statusText, Label countryValue, Label eventValue, Label cooperationValue,
                               Label conflictValue, Label importValue, Label mentionValue, Label goldsteinValue,
                               Label toneValue, Label dashboardInsight, PieChart typePieChart,
                               BarChart<String, Number> topCountryChart,
                               LineChart<String, Number> dailyTrendChart) {
        Task<DashboardViewData> task = new Task<>() {
            @Override
            protected DashboardViewData call() {
                DashboardService service = new DashboardService(new DatabaseManager(paths));
                return new DashboardViewData(
                        service.loadSummary(),
                        service.topCountries(8),
                        service.dailyTrend()
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
            updateDailyTrendChart(dailyTrendChart, data.dailyTrend());
            refreshChartsAfterDataLoad(typePieChart, topCountryChart, dailyTrendChart);
            dashboardInsight.setText(buildDashboardInsight(summary, data.topCountries(), data.dailyTrend()));
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

    private void updateDailyTrendChart(LineChart<String, Number> chart, List<MonthlyTrendPoint> trends) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (MonthlyTrendPoint point : trends) {
            series.getData().add(new XYChart.Data<>(point.month(), point.totalEvents()));
        }
        chart.getData().setAll(series);
    }

    private void refreshChartsAfterDataLoad(Node... charts) {
        Platform.runLater(() -> {
            for (Node chart : charts) {
                chart.applyCss();
                chart.autosize();
                requestNodeLayout(chart);
            }
            Platform.runLater(() -> {
                for (Node chart : charts) {
                    chart.getParent().requestLayout();
                    requestNodeLayout(chart);
                }
            });
        });
    }

    private void requestNodeLayout(Node node) {
        if (node instanceof Parent parent) {
            parent.requestLayout();
        }
    }

    private void updateGeoScatterChart(ScatterChart<Number, Number> chart, List<GeoEventPoint> points) {
        XYChart.Series<Number, Number> cooperation = new XYChart.Series<>();
        cooperation.setName("合作");
        XYChart.Series<Number, Number> conflict = new XYChart.Series<>();
        conflict.setName("冲突");
        XYChart.Series<Number, Number> other = new XYChart.Series<>();
        other.setName("其他");
        for (GeoEventPoint point : points) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(point.longitude(), point.latitude());
            if (point.eventType() == EventType.COOPERATION) {
                cooperation.getData().add(data);
            } else if (point.eventType() == EventType.CONFLICT) {
                conflict.getData().add(data);
            } else {
                other.getData().add(data);
            }
        }
        chart.getData().setAll(cooperation, conflict, other);
    }

    private String buildDashboardInsight(DashboardSummary summary, List<CountryEventStat> topCountries,
                                         List<MonthlyTrendPoint> dailyTrend) {
        if (summary.totalEvents() == 0) {
            return "当前数据库尚无事件记录。完成真实 GDELT 数据导入后，首页将自动汇总事件结构、国家热度和日度趋势。";
        }
        String leadingCountry = topCountries.isEmpty() ? "暂无" : topCountries.get(0).countryCode();
        String latestDay = dailyTrend.isEmpty() ? "暂无日期" : dailyTrend.get(dailyTrend.size() - 1).month();
        double cooperationRatio = summary.totalEvents() == 0 ? 0 : (double) summary.cooperationEvents() / summary.totalEvents();
        double conflictRatio = summary.totalEvents() == 0 ? 0 : (double) summary.conflictEvents() / summary.totalEvents();
        return "当前样本覆盖 " + summary.countryCount() + " 个配置国家，事件热度最高国家为 " + leadingCountry
                + "。合作事件占比 " + formatPercent(cooperationRatio)
                + "，冲突事件占比 " + formatPercent(conflictRatio)
                + "，最新趋势日期为 " + latestDay
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
                + " 条，可用于识别合作热点国家和合作分布差异。";
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

    private String buildRegionInsight(List<RegionSummary> regions) {
        if (regions.isEmpty()) {
            return "暂无区域汇总结果。国家配置同步后，可按子区域比较合作、冲突、语调和风险差异。";
        }
        RegionSummary cooperationTop = regions.stream()
                .max(Comparator.comparingDouble(RegionSummary::cooperationIndex))
                .orElse(regions.get(0));
        RegionSummary riskTop = regions.stream()
                .max(Comparator.comparingDouble(RegionSummary::riskIndex))
                .orElse(regions.get(0));
        return "合作指数最高区域为 " + cooperationTop.region()
                + "，风险指数最高区域为 " + riskTop.region()
                + "。区域对比覆盖 " + regions.size()
                + " 个子区域，可用于说明一带一路不同区域的合作活跃度和风险差异。";
    }

    private String buildClusterInsight(List<CountryClusterResult> results) {
        if (results.isEmpty()) {
            return "暂无聚类结果。导入真实数据并同步国家配置后，可将国家分为四类合作/风险群组。";
        }
        long deep = results.stream().filter(result -> "深度合作伙伴".equals(result.clusterLabel())).count();
        long stable = results.stream().filter(result -> "稳定合作".equals(result.clusterLabel())).count();
        long risky = results.stream().filter(result -> "存在风险".equals(result.clusterLabel())).count();
        long tense = results.stream().filter(result -> "高度紧张".equals(result.clusterLabel())).count();
        return "聚类结果显示：深度合作伙伴 " + deep
                + " 个，稳定合作 " + stable
                + " 个，存在风险 " + risky
                + " 个，高度紧张 " + tense
                + " 个。该结果可用于识别不同合作类型国家，为分层观察、重点跟踪和风险研判提供参考。";
    }

    private String buildMapInsight(List<GeoEventPoint> points) {
        if (points.isEmpty()) {
            return "暂无有效地理点位。导入包含 ActionGeo 经纬度的 GDELT 文件后，可展示沿线事件空间分布。";
        }
        long cooperation = points.stream().filter(point -> point.eventType() == EventType.COOPERATION).count();
        long conflict = points.stream().filter(point -> point.eventType() == EventType.CONFLICT).count();
        double averageLatitude = points.stream().mapToDouble(GeoEventPoint::latitude).average().orElse(0);
        double averageLongitude = points.stream().mapToDouble(GeoEventPoint::longitude).average().orElse(0);
        return "当前点位样本共 " + points.size()
                + " 条，合作点位 " + cooperation
                + " 条，冲突点位 " + conflict
                + " 条。点位中心约为纬度 " + "%.2f".formatted(averageLatitude)
                + "、经度 " + "%.2f".formatted(averageLongitude)
                + "，反映沿线区域事件点位的空间分布重心与集聚特征。";
    }

    private Parent createImportPage(PageSpec page) {
        VBox body = createPageBase("GDELT 数据维护工具", page.description());

        VBox importBox = new VBox(14);
        importBox.getStyleClass().add("import-box");

        Label fileLabel = new Label("补充导入文件");
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
                invalidateDataPageCache();
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
        ComboBox<CountryOption> countryBField = createCountryCodeComboBox();
        Button searchButton = new Button("分析");
        searchButton.getStyleClass().add("primary-button");
        Button clearButton = new Button("清空");
        clearButton.getStyleClass().add("secondary-button");
        addFormField(form, 0, 0, "国家 A", countryAField);
        addFormField(form, 0, 1, "国家 B", countryBField);
        form.add(new HBox(10, searchButton, clearButton), 2, 0);

        Label statusText = new Label("默认以 CHN 为国家 A，输入或选择沿线国家代码后开始分析。");
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
            String countryB = extractCountryCode(countryBField);
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
            countryBField.getSelectionModel().clearSelection();
            countryBField.getEditor().clear();
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
                "合作指数 = 合作事件量 + 正向 Goldstein + 正向媒体语调 + 媒体关注度 - 冲突扣分，归一到 0-100。该口径用于综合衡量合作活跃度和合作质量。");

        TableView<CooperationScore> table = createCooperationTable();
        ObservableList<CooperationScore> items = FXCollections.observableArrayList();
        table.setItems(items);

        BarChart<String, Number> hotspotChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        hotspotChart.getStyleClass().add("premium-bar-chart");
        hotspotChart.getStyleClass().add("cooperation-bar-chart");
        hotspotChart.setTitle("新兴合作热点 TOP10（合作指数环比增长）");
        hotspotChart.setLegendVisible(false);
        hotspotChart.setAnimated(false);
        TableView<CooperationHotspot> hotspotTable = createHotspotTable();
        ObservableList<CooperationHotspot> hotspotItems = FXCollections.observableArrayList();
        hotspotTable.setItems(hotspotItems);

        Task<CooperationAnalysisViewData> task = new Task<>() {
            @Override
            protected CooperationAnalysisViewData call() {
                AnalysisService service = new AnalysisService(new DatabaseManager(paths));
                return new CooperationAnalysisViewData(
                        service.cooperationRankings(AnalysisService.DEFAULT_RANK_LIMIT),
                        service.cooperationHotspots(10)
                );
            }
        };
        task.setOnSucceeded(event -> {
            CooperationAnalysisViewData data = task.getValue();
            List<CooperationScore> results = data.rankings();
            items.setAll(results);
            hotspotItems.setAll(data.hotspots());
            updateCooperationMetrics(results, countryValue, topCountryValue, topIndexValue, totalCooperationValue);
            updateHotspotChart(hotspotChart, data.hotspots());
            insightText.setText(buildCooperationInsight(results));
            statusText.setText(results.isEmpty()
                    ? "暂无合作态势数据。"
                    : "合作态势排名已加载，共显示 " + results.size()
                    + " 个国家；热点追踪 " + data.hotspots().size() + " 个。");
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            statusText.setText("合作态势加载失败：" + (exception == null ? "未知错误" : exception.getMessage()));
        });
        Thread thread = new Thread(task, "cooperation-analysis-task");
        thread.setDaemon(true);
        thread.start();

        body.getChildren().addAll(statusText, metricRow, new HBox(14, insightPanel, formulaPanel),
                createSectionTitle("合作指数国家排名"), table,
                createSectionTitle("合作热点追踪 TOP10"), wrapChart(hotspotChart), hotspotTable);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(hotspotTable, Priority.ALWAYS);
        return wrapScrollable(body);
    }

    private Parent createRiskAssessmentPage() {
        VBox body = createPageBase("沿线国家风险评估", "按国家聚合冲突事件、冲突占比、Goldstein 和媒体语调，形成可用于风险比较和重点跟踪的风险指数。");

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

        BarChart<String, Number> riskHotspotChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        riskHotspotChart.getStyleClass().add("premium-bar-chart");
        riskHotspotChart.getStyleClass().add("risk-bar-chart");
        riskHotspotChart.setTitle("风险上升热点 TOP10（风险指数环比增长）");
        riskHotspotChart.setLegendVisible(false);
        riskHotspotChart.setAnimated(false);
        TableView<RiskHotspot> riskHotspotTable = createRiskHotspotTable();
        ObservableList<RiskHotspot> riskHotspotItems = FXCollections.observableArrayList();
        riskHotspotTable.setItems(riskHotspotItems);

        Task<RiskAssessmentViewData> task = new Task<>() {
            @Override
            protected RiskAssessmentViewData call() {
                AnalysisService service = new AnalysisService(new DatabaseManager(paths));
                return new RiskAssessmentViewData(
                        service.riskRankings(AnalysisService.DEFAULT_RANK_LIMIT),
                        service.riskHotspots(10)
                );
            }
        };
        task.setOnSucceeded(event -> {
            RiskAssessmentViewData data = task.getValue();
            List<RiskAssessment> results = data.rankings();
            items.setAll(results);
            riskHotspotItems.setAll(data.hotspots());
            updateRiskMetrics(results, countryValue, topCountryValue, topIndexValue, highRiskValue);
            updateRiskHotspotChart(riskHotspotChart, data.hotspots());
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
        body.getChildren().addAll(createSectionTitle("风险热点追踪 TOP10"), wrapChart(riskHotspotChart), riskHotspotTable);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(riskHotspotTable, Priority.ALWAYS);
        return wrapScrollable(body);
    }

    private Parent createRegionAnalysisPage() {
        VBox body = createPageBase("一带一路子区域对比分析", "按国家配置中的子区域聚合事件量、合作、冲突、媒体语调和风险指标。");

        Label statusText = new Label("正在加载区域汇总...");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        HBox metricRow = new HBox(14);
        metricRow.getStyleClass().add("summary-row");
        Label regionValue = metricValue("0");
        Label topCooperationValue = metricValue("-");
        Label topRiskValue = metricValue("-");
        Label totalEventValue = metricValue("0");
        metricRow.getChildren().addAll(
                createMetricCard("覆盖区域", regionValue, "配置中包含的子区域数", "neutral-card"),
                createMetricCard("合作最高区域", topCooperationValue, "合作指数最高", "positive-card"),
                createMetricCard("风险最高区域", topRiskValue, "风险指数最高", "negative-card"),
                createMetricCard("区域参与事件", totalEventValue, "按国家参与口径汇总", "neutral-card")
        );

        Label insightText = new Label("等待区域数据加载后生成研判。");
        insightText.getStyleClass().add("insight-text");
        insightText.setWrapText(true);
        VBox insightPanel = createInsightPanel("区域态势研判", insightText);
        VBox formulaPanel = createFormulaPanel("区域对比口径",
                "区域汇总按配置国家参与事件聚合。合作/风险指数采用比例、均值和区域规模归一化后的 0-100 相对评分，原始事件量保留在表格中用于解释。");

        CategoryAxis axis = new CategoryAxis();
        NumberAxis scoreAxis = new NumberAxis(0, 100, 10);
        scoreAxis.setLabel("归一化指数");
        BarChart<String, Number> chart = new BarChart<>(axis, scoreAxis);
        chart.getStyleClass().add("premium-region-bar-chart");
        chart.setTitle("区域合作/风险指数对比");
        chart.setAnimated(false);
        chart.setLegendVisible(true);

        Canvas radarChart = createRegionRadarChart();

        TableView<RegionSummary> table = createRegionTable();
        ObservableList<RegionSummary> items = FXCollections.observableArrayList();
        table.setItems(items);

        Task<List<RegionSummary>> task = new Task<>() {
            @Override
            protected List<RegionSummary> call() {
                return new AnalysisService(new DatabaseManager(paths)).regionSummaries();
            }
        };
        task.setOnSucceeded(event -> {
            List<RegionSummary> results = task.getValue();
            items.setAll(results);
            updateRegionMetrics(results, regionValue, topCooperationValue, topRiskValue, totalEventValue);
            updateRegionChart(chart, results);
            updateRegionRadarChart(radarChart, results);
            insightText.setText(buildRegionInsight(results));
            statusText.setText(results.isEmpty()
                    ? "暂无区域汇总数据。"
                    : "区域分析已加载，共显示 " + results.size() + " 个子区域。");
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            statusText.setText("区域分析加载失败：" + (exception == null ? "未知错误" : exception.getMessage()));
        });
        Thread thread = new Thread(task, "region-analysis-task");
        thread.setDaemon(true);
        thread.start();

        body.getChildren().addAll(statusText, metricRow, new HBox(14, insightPanel, formulaPanel),
                wrapChart(chart), wrapChart(radarChart), createSectionTitle("区域指标汇总"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return wrapScrollable(body);
    }

    private Parent createClusterAnalysisPage() {
        VBox body = createPageBase("沿线国家 K-Means 聚类分析", "基于合作指数、风险指数、冲突占比、Goldstein、AvgTone 和事件量归一化特征，将国家分为四类。");

        Label statusText = new Label("正在运行聚类分析...");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        Label insightText = new Label("等待聚类结果加载后生成解释。");
        insightText.getStyleClass().add("insight-text");
        insightText.setWrapText(true);
        VBox insightPanel = createInsightPanel("聚类结果解释", insightText);
        VBox formulaPanel = createFormulaPanel("K-Means 口径",
                "k=4，特征包括合作指数、风险指数、冲突占比、平均 Goldstein、平均 AvgTone 和事件量归一化。标签固定映射为深度合作伙伴、稳定合作、存在风险、高度紧张。");

        TableView<CountryClusterResult> table = createClusterTable();
        ObservableList<CountryClusterResult> items = FXCollections.observableArrayList();
        table.setItems(items);

        Task<List<CountryClusterResult>> task = new Task<>() {
            @Override
            protected List<CountryClusterResult> call() {
                return new AnalysisService(new DatabaseManager(paths)).countryClusters();
            }
        };
        task.setOnSucceeded(event -> {
            List<CountryClusterResult> results = task.getValue();
            items.setAll(results);
            insightText.setText(buildClusterInsight(results));
            statusText.setText(results.isEmpty()
                    ? "暂无聚类数据。"
                    : "聚类分析完成，共显示 " + results.size() + " 个配置国家。");
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            statusText.setText("聚类分析失败：" + (exception == null ? "未知错误" : exception.getMessage()));
        });
        Thread thread = new Thread(task, "cluster-analysis-task");
        thread.setDaemon(true);
        thread.start();

        body.getChildren().addAll(statusText, new HBox(14, insightPanel, formulaPanel),
                createSectionTitle("国家聚类结果"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return wrapScrollable(body);
    }

    private Parent createInteractiveMapPage() {
        VBox body = createPageBase("一带一路事件空间交互专题地图", "基于 GDELT ActionGeo 经纬度字段，支持缩放、平移、图层叠加和事件点位联动。");

        Label statusText = new Label("正在加载地理事件点位...");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        CheckBox cooperationLayer = new CheckBox("合作");
        cooperationLayer.setSelected(true);
        CheckBox conflictLayer = new CheckBox("冲突");
        conflictLayer.setSelected(true);
        CheckBox otherLayer = new CheckBox("其他");
        otherLayer.setSelected(true);
        CheckBox riskLayer = new CheckBox("风险热点");
        riskLayer.setSelected(true);
        CheckBox heatLayer = new CheckBox("热度层");
        heatLayer.setSelected(true);
        CheckBox countryThemeLayer = new CheckBox("国家专题");
        countryThemeLayer.setSelected(true);

        ComboBox<Integer> limitBox = new ComboBox<>(FXCollections.observableArrayList(500, 1000, 2000));
        limitBox.setValue(MapVisualizationService.DEFAULT_POINT_LIMIT);
        limitBox.setPrefWidth(94);
        TextField countryFilter = new TextField();
        countryFilter.setPromptText("国家代码");
        countryFilter.setPrefWidth(110);
        ComboBox<String> windowBox = new ComboBox<>(FXCollections.observableArrayList("单日", "7日", "30日", "全部"));
        windowBox.setValue("30日");
        windowBox.setPrefWidth(90);
        Slider dateSlider = new Slider(0, 0, 0);
        dateSlider.setPrefWidth(260);
        dateSlider.setShowTickMarks(false);
        dateSlider.setShowTickLabels(false);
        dateSlider.setDisable(true);
        Label dateLabel = new Label("时间：暂无");
        dateLabel.setMinWidth(190);
        Button playButton = new Button("播放");
        playButton.getStyleClass().add("secondary-button");
        playButton.setMinWidth(64);
        playButton.setDisable(true);
        Button searchButton = new Button("刷新");
        searchButton.getStyleClass().add("primary-button");
        searchButton.setMinWidth(64);
        Button resetButton = new Button("重置");
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setMinWidth(64);

        HBox layerFilterRow = new HBox(12,
                new Label("图层："), countryThemeLayer, cooperationLayer, conflictLayer, otherLayer, riskLayer, heatLayer,
                new Separator(), new Label("点位："), limitBox,
                new Label("Actor："), countryFilter, searchButton, resetButton);
        layerFilterRow.setAlignment(Pos.CENTER_LEFT);
        HBox timeFilterRow = new HBox(12,
                new Label("窗口："), windowBox, dateSlider, dateLabel, playButton);
        timeFilterRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dateSlider, Priority.ALWAYS);
        dateSlider.setMaxWidth(Double.MAX_VALUE);
        VBox filterRow = new VBox(10, layerFilterRow, timeFilterRow);
        filterRow.getStyleClass().add("query-form");

        Label insightText = new Label("等待点位加载后生成空间研判。");
        insightText.getStyleClass().add("insight-text");
        insightText.setWrapText(true);
        VBox insightPanel = createInsightPanel("空间分布研判", insightText);
        VBox formulaPanel = createFormulaPanel("交互地图说明",
                "支持滚轮缩放、拖拽平移、双击复位和点位详情查看。地图基于离线经纬度底图展示事件空间位置，并按合作、冲突、其他类型分层渲染，可叠加风险热点与热度分布辅助研判。");
        HBox insightRow = new HBox(14, insightPanel, formulaPanel);
        insightRow.getStyleClass().add("chart-row");

        TableView<GeoEventPoint> table = createGeoEventTable();
        ObservableList<GeoEventPoint> items = FXCollections.observableArrayList();
        table.setItems(items);

        InteractiveMapPane mapPane = new InteractiveMapPane(point -> {
            table.getSelectionModel().select(point);
            table.scrollTo(point);
        });
        mapPane.setLayerState(countryThemeLayer.isSelected(), cooperationLayer.isSelected(),
                conflictLayer.isSelected(), otherLayer.isSelected(), riskLayer.isSelected(), heatLayer.isSelected());
        VBox mapPanel = new VBox(mapPane);
        mapPanel.getStyleClass().addAll("chart-panel", "map-chart-panel");
        mapPanel.setMinHeight(560);
        mapPanel.setPrefHeight(600);
        VBox.setVgrow(mapPane, Priority.ALWAYS);

        Runnable refreshLayers = () -> mapPane.setLayerState(countryThemeLayer.isSelected(), cooperationLayer.isSelected(),
                conflictLayer.isSelected(), otherLayer.isSelected(), riskLayer.isSelected(), heatLayer.isSelected());
        countryThemeLayer.setOnAction(event -> refreshLayers.run());
        cooperationLayer.setOnAction(event -> refreshLayers.run());
        conflictLayer.setOnAction(event -> refreshLayers.run());
        otherLayer.setOnAction(event -> refreshLayers.run());
        riskLayer.setOnAction(event -> refreshLayers.run());
        heatLayer.setOnAction(event -> refreshLayers.run());

        List<LocalDate> availableDates = new ArrayList<>();
        Map<LocalDate, List<GeoEventPoint>> seriesCache = new LinkedHashMap<>();
        int[] currentPointLimit = {MapVisualizationService.DEFAULT_POINT_LIMIT};
        boolean[] updatingSlider = {false};
        boolean[] suppressPreload = {false};
        Timeline[] playback = {null};
        Runnable[] preloadMapSeries = new Runnable[1];
        Runnable[] applyCurrentFrame = new Runnable[1];

        applyCurrentFrame[0] = () -> {
            String window = windowBox.getValue() == null ? "30日" : windowBox.getValue();
            if (availableDates.isEmpty()) {
                items.clear();
                mapPane.setPoints(List.of());
                dateLabel.setText("时间：暂无");
                statusText.setText("暂无包含有效经纬度的事件。");
                insightText.setText(buildMapInsight(List.of()));
                dateSlider.setDisable(true);
                playButton.setDisable(true);
                return;
            }
            int selectedIndex = (int) Math.round(dateSlider.getValue());
            selectedIndex = Math.max(0, Math.min(selectedIndex, availableDates.size() - 1));
            LocalDate endDate = availableDates.get(selectedIndex);
            LocalDate startDate = mapWindowStart(window, availableDates, selectedIndex);
            if ("全部".equals(window)) {
                startDate = availableDates.get(0);
                endDate = availableDates.get(availableDates.size() - 1);
            }
            List<GeoEventPoint> points = mapCachedWindowPoints(seriesCache, startDate, endDate, currentPointLimit[0]);
            mapPane.setPoints(points);
            dateLabel.setText(formatMapWindowLabel(window,
                    "全部".equals(window) ? null : startDate,
                    "全部".equals(window) ? null : endDate,
                    availableDates));
            statusText.setText(points.isEmpty()
                    ? "暂无包含有效经纬度的事件。"
                    : "交互专题地图播放中，" + dateLabel.getText() + "，当前帧 " + points.size() + " 个有效事件点位。");
        };

        preloadMapSeries[0] = () -> {
            if (playback[0] != null) {
                playback[0].stop();
                playButton.setText("播放");
            }
            Set<String> selectedTypes = selectedMapEventTypes(cooperationLayer, conflictLayer, otherLayer);
            int limit = limitBox.getValue() == null ? MapVisualizationService.DEFAULT_POINT_LIMIT : limitBox.getValue();
            currentPointLimit[0] = limit;
            String countryCode = countryFilter.getText() == null ? "" : countryFilter.getText().trim();
            int requestedDateIndex = (int) Math.round(dateSlider.getValue());
            boolean keepDateIndex = !availableDates.isEmpty();
            Task<MapSeriesLoadResult> task = new Task<>() {
                @Override
                protected MapSeriesLoadResult call() {
                    MapVisualizationService service = new MapVisualizationService(new DatabaseManager(paths));
                    Map<LocalDate, List<GeoEventPoint>> series = service.geoEventPointSeries(limit, selectedTypes, countryCode);
                    List<CountryMapMetric> countryMetrics = service.countryCooperationMetrics();
                    return new MapSeriesLoadResult(new ArrayList<>(series.keySet()), series, countryMetrics);
                }
            };
            searchButton.setDisable(true);
            resetButton.setDisable(true);
            playButton.setDisable(true);
            dateSlider.setDisable(true);
            statusText.setText("正在准备时间序列...");
            task.setOnSucceeded(event -> {
                MapSeriesLoadResult result = task.getValue();
                availableDates.clear();
                availableDates.addAll(result.availableDates());
                seriesCache.clear();
                seriesCache.putAll(result.series());
                mapPane.setCountryMetrics(result.countryMetrics());
                updatingSlider[0] = true;
                dateSlider.setMin(0);
                dateSlider.setMax(Math.max(0, availableDates.size() - 1));
                if (availableDates.isEmpty()) {
                    dateSlider.setValue(0);
                } else if (!keepDateIndex) {
                    dateSlider.setValue(availableDates.size() - 1);
                } else {
                    dateSlider.setValue(Math.max(0, Math.min(requestedDateIndex, availableDates.size() - 1)));
                }
                String window = windowBox.getValue() == null ? "30日" : windowBox.getValue();
                dateSlider.setDisable(availableDates.isEmpty() || "全部".equals(window));
                playButton.setDisable(availableDates.size() < 2 || "全部".equals(window));
                updatingSlider[0] = false;
                applyCurrentFrame[0].run();
                List<GeoEventPoint> currentPoints = mapPane.pointsSnapshot();
                items.setAll(currentPoints);
                insightText.setText(buildMapInsight(currentPoints));
                int cachedPoints = seriesCache.values().stream().mapToInt(List::size).sum();
                statusText.setText(availableDates.isEmpty()
                        ? "暂无包含有效经纬度的事件。"
                        : "时间序列已准备，共 " + availableDates.size() + " 个日期、缓存 "
                        + cachedPoints + " 个代表点。");
                searchButton.setDisable(false);
                resetButton.setDisable(false);
            });
            task.setOnFailed(event -> {
                Throwable exception = task.getException();
                statusText.setText("专题地图时间序列准备失败：" + (exception == null ? "未知错误" : exception.getMessage()));
                searchButton.setDisable(false);
                resetButton.setDisable(false);
                playButton.setDisable(availableDates.size() < 2 || "全部".equals(windowBox.getValue()));
            });
            Thread thread = new Thread(task, "map-series-preload-task");
            thread.setDaemon(true);
            thread.start();
        };

        Runnable syncTableAndInsight = () -> {
            List<GeoEventPoint> currentPoints = mapPane.pointsSnapshot();
            items.setAll(currentPoints);
            insightText.setText(buildMapInsight(currentPoints));
        };

        dateSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
            if (!isChanging && !updatingSlider[0]) {
                applyCurrentFrame[0].run();
                syncTableAndInsight.run();
            }
        });
        dateSlider.setOnMouseReleased(event -> {
            if (!updatingSlider[0]) {
                applyCurrentFrame[0].run();
                syncTableAndInsight.run();
            }
        });
        windowBox.setOnAction(event -> {
            if (suppressPreload[0]) {
                return;
            }
            if (playback[0] != null) {
                playback[0].stop();
                playButton.setText("播放");
            }
            applyCurrentFrame[0].run();
            syncTableAndInsight.run();
        });
        playButton.setOnAction(event -> {
            if (playback[0] != null && playback[0].getStatus() == Timeline.Status.RUNNING) {
                playback[0].stop();
                playButton.setText("播放");
                syncTableAndInsight.run();
                return;
            }
            playback[0] = new Timeline(new KeyFrame(Duration.millis(280), frame -> {
                if (dateSlider.getValue() >= dateSlider.getMax()) {
                    playback[0].stop();
                    playButton.setText("播放");
                    syncTableAndInsight.run();
                    return;
                }
                updatingSlider[0] = true;
                dateSlider.setValue(dateSlider.getValue() + 1);
                updatingSlider[0] = false;
                applyCurrentFrame[0].run();
            }));
            playback[0].setCycleCount(Timeline.INDEFINITE);
            playback[0].play();
            playButton.setText("暂停");
        });

        searchButton.setOnAction(event -> preloadMapSeries[0].run());
        countryFilter.setOnAction(event -> preloadMapSeries[0].run());
        limitBox.setOnAction(event -> {
            if (!suppressPreload[0]) {
                preloadMapSeries[0].run();
            }
        });
        resetButton.setOnAction(event -> {
            if (playback[0] != null) {
                playback[0].stop();
                playButton.setText("播放");
            }
            suppressPreload[0] = true;
            countryThemeLayer.setSelected(true);
            cooperationLayer.setSelected(true);
            conflictLayer.setSelected(true);
            otherLayer.setSelected(true);
            riskLayer.setSelected(true);
            heatLayer.setSelected(true);
            limitBox.setValue(MapVisualizationService.DEFAULT_POINT_LIMIT);
            windowBox.setValue("30日");
            suppressPreload[0] = false;
            availableDates.clear();
            seriesCache.clear();
            countryFilter.clear();
            mapPane.resetView();
            preloadMapSeries[0].run();
        });
        preloadMapSeries[0].run();

        body.getChildren().addAll(statusText, filterRow, mapPanel, insightRow, createSectionTitle("地理事件明细"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return wrapScrollable(body);
    }

    private Set<String> selectedMapEventTypes(CheckBox cooperationLayer, CheckBox conflictLayer, CheckBox otherLayer) {
        Set<String> selectedTypes = new HashSet<>();
        if (cooperationLayer.isSelected()) {
            selectedTypes.add(EventType.COOPERATION.name());
        }
        if (conflictLayer.isSelected()) {
            selectedTypes.add(EventType.CONFLICT.name());
        }
        if (otherLayer.isSelected()) {
            selectedTypes.add(EventType.OTHER.name());
        }
        if (selectedTypes.isEmpty()) {
            selectedTypes.add("__NONE__");
        }
        return selectedTypes;
    }

    private LocalDate mapWindowStart(String window, List<LocalDate> availableDates, int selectedIndex) {
        if (availableDates == null || availableDates.isEmpty()) {
            return null;
        }
        int safeIndex = Math.max(0, Math.min(selectedIndex, availableDates.size() - 1));
        return switch (window) {
            case "7日" -> availableDates.get(Math.max(0, safeIndex - 6));
            case "30日" -> availableDates.get(Math.max(0, safeIndex - 29));
            case "全部" -> null;
            default -> availableDates.get(safeIndex);
        };
    }

    private String formatMapWindowLabel(String window, LocalDate startDate, LocalDate endDate,
                                        List<LocalDate> availableDates) {
        if (availableDates == null || availableDates.isEmpty()) {
            return "时间：暂无";
        }
        if ("全部".equals(window)) {
            return "时间：全部 " + availableDates.get(0) + " 至 " + availableDates.get(availableDates.size() - 1);
        }
        if (startDate == null || endDate == null || startDate.equals(endDate)) {
            return "时间：" + (endDate == null ? "-" : endDate);
        }
        return "时间：" + startDate + " 至 " + endDate;
    }

    private List<GeoEventPoint> mapCachedWindowPoints(Map<LocalDate, List<GeoEventPoint>> series,
                                                      LocalDate startDate, LocalDate endDate, int limit) {
        if (series == null || series.isEmpty() || startDate == null || endDate == null) {
            return List.of();
        }
        int effectiveLimit = limit <= 0 ? MapVisualizationService.DEFAULT_POINT_LIMIT : Math.min(limit, 2000);
        List<LocalDate> frameDates = new ArrayList<>();
        for (LocalDate date : series.keySet()) {
            if ((date.isEqual(startDate) || date.isAfter(startDate))
                    && (date.isEqual(endDate) || date.isBefore(endDate))) {
                frameDates.add(date);
            }
        }
        if (frameDates.isEmpty()) {
            return List.of();
        }
        int perDateLimit = Math.max(1, (effectiveLimit + frameDates.size() - 1) / frameDates.size());
        List<GeoEventPoint> results = new ArrayList<>();
        for (LocalDate date : frameDates) {
            List<GeoEventPoint> dailyPoints = series.getOrDefault(date, List.of());
            for (int index = 0; index < Math.min(perDateLimit, dailyPoints.size()); index++) {
                results.add(dailyPoints.get(index));
            }
        }
        results.sort(Comparator.comparing(GeoEventPoint::eventDate).reversed()
                .thenComparing(GeoEventPoint::globalEventId, Comparator.reverseOrder()));
        return results.size() <= effectiveLimit ? results : new ArrayList<>(results.subList(0, effectiveLimit));
    }

    private Parent createMapPage() {
        VBox body = createPageBase("一带一路事件空间散点专题图", "基于 GDELT ActionGeo 经纬度字段，展示涉及沿线国家事件的空间分布和代表性地理事件。");

        Label statusText = new Label("正在加载地理事件点位...");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        NumberAxis longitudeAxis = new NumberAxis(-180, 180, 30);
        longitudeAxis.setLabel("经度");
        NumberAxis latitudeAxis = new NumberAxis(-90, 90, 15);
        latitudeAxis.setLabel("纬度");
        ScatterChart<Number, Number> scatterChart = new ScatterChart<>(longitudeAxis, latitudeAxis);
        scatterChart.setTitle("事件空间分布（经度 / 纬度）");
        scatterChart.setLegendVisible(true);
        scatterChart.setAnimated(false);
        scatterChart.getStyleClass().add("map-scatter");
        scatterChart.setPrefHeight(520);
        scatterChart.setMinHeight(460);

        Label insightText = new Label("等待点位加载后生成空间研判。");
        insightText.getStyleClass().add("insight-text");
        insightText.setWrapText(true);
        VBox insightPanel = createInsightPanel("空间分布研判", insightText);
        VBox formulaPanel = createFormulaPanel("散点图口径说明",
                "本页使用 GDELT 的 ActionGeo_Lat 与 ActionGeo_Long 字段，默认显示最近有效点位。散点按合作、冲突、其他三类着色，用于展示事件空间集聚趋势，不等同于完整 GIS 底图。");

        VBox mapPanel = wrapChart(scatterChart);
        mapPanel.getStyleClass().add("map-chart-panel");
        HBox insightRow = new HBox(14, insightPanel, formulaPanel);
        insightRow.getStyleClass().add("chart-row");

        TableView<GeoEventPoint> table = createGeoEventTable();
        ObservableList<GeoEventPoint> items = FXCollections.observableArrayList();
        table.setItems(items);

        Task<List<GeoEventPoint>> task = new Task<>() {
            @Override
            protected List<GeoEventPoint> call() {
                return new MapVisualizationService(new DatabaseManager(paths))
                        .geoEventPoints(MapVisualizationService.DEFAULT_POINT_LIMIT);
            }
        };
        task.setOnSucceeded(event -> {
            List<GeoEventPoint> points = task.getValue();
            items.setAll(points);
            updateGeoScatterChart(scatterChart, points);
            insightText.setText(buildMapInsight(points));
            statusText.setText(points.isEmpty()
                    ? "暂无包含有效经纬度的事件。"
                    : "专题地图已加载，共显示 " + points.size() + " 个有效事件点位。");
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            statusText.setText("专题地图加载失败：" + (exception == null ? "未知错误" : exception.getMessage()));
        });
        Thread thread = new Thread(task, "map-visualization-task");
        thread.setDaemon(true);
        thread.start();

        body.getChildren().addAll(statusText, mapPanel, insightRow, createSectionTitle("地理事件明细"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return wrapScrollable(body);
    }

    private Parent createExportPage() {
        VBox body = createPageBase("分析结果导出", "生成系统分析报告和国家排名数据文件，便于后续归档、复核和二次分析。");

        HBox actionRow = new HBox(12);
        actionRow.getStyleClass().add("query-form");
        actionRow.setAlignment(Pos.CENTER_LEFT);
        Button exportButton = new Button("生成导出文件");
        exportButton.getStyleClass().add("primary-button");
        Label targetText = new Label("输出目录：" + paths.rootDir());
        targetText.getStyleClass().add("import-status");
        targetText.setWrapText(true);
        actionRow.getChildren().addAll(exportButton, targetText);
        HBox.setHgrow(targetText, Priority.ALWAYS);

        Label statusText = new Label("点击按钮后生成 TXT 汇总报告、合作排名 CSV 和风险排名 CSV 文件。");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        Label insightText = new Label("导出内容基于当前 SQLite 数据库，包含总体概况、合作热点国家和风险预警国家等核心分析结果。");
        insightText.getStyleClass().add("insight-text");
        insightText.setWrapText(true);
        VBox insightPanel = createInsightPanel("导出内容说明", insightText);
        TextArea resultArea = new TextArea("暂无导出结果。");
        resultArea.getStyleClass().add("result-summary");
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefRowCount(10);

        exportButton.setOnAction(event -> {
            Task<ExportResult> task = new Task<>() {
                @Override
                protected ExportResult call() {
                    return new ReportExportService(paths).exportSnapshot();
                }
            };
            exportButton.setDisable(true);
            statusText.setText("正在生成导出文件，请稍候...");
            resultArea.setText("导出任务运行中。");
            task.setOnSucceeded(workerEvent -> {
                ExportResult result = task.getValue();
                statusText.setText(result.displaySummary());
                resultArea.setText(formatExportResult(result));
                exportButton.setDisable(false);
            });
            task.setOnFailed(workerEvent -> {
                Throwable exception = task.getException();
                statusText.setText("导出失败：" + (exception == null ? "未知错误" : exception.getMessage()));
                resultArea.setText(exception == null ? "无异常详情。" : exception.toString());
                exportButton.setDisable(false);
            });
            Thread thread = new Thread(task, "report-export-task");
            thread.setDaemon(true);
            thread.start();
        });

        body.getChildren().addAll(actionRow, statusText, insightPanel,
                createSectionTitle("导出结果"), resultArea);
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
        ComboBox<CountryOption> anyCountryField = createCountryCodeComboBox(true);
        anyCountryField.setPromptText("代码/中文/英文");
        ComboBox<CountryOption> actor1Field = createCountryCodeComboBox(true);
        actor1Field.setPromptText("Actor1 代码/名称");
        ComboBox<CountryOption> actor2Field = createCountryCodeComboBox(true);
        actor2Field.setPromptText("Actor2 代码/名称");
        ComboBox<String> regionBox = createRegionComboBox();

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
        searchButton.setMinWidth(76);
        clearButton.setMinWidth(76);
        HBox actionButtons = new HBox(10, searchButton, clearButton);
        actionButtons.setMinWidth(170);

        addFormField(form, 0, 0, "开始日期", startDatePicker);
        addFormField(form, 0, 1, "结束日期", endDatePicker);
        addFormField(form, 0, 2, "任一国家", anyCountryField);
        addFormField(form, 1, 0, "Actor1", actor1Field);
        addFormField(form, 1, 1, "Actor2", actor2Field);
        addFormField(form, 1, 2, "子区域", regionBox);
        addFormField(form, 2, 0, "事件类型", eventTypeBox);
        form.add(actionButtons, 3, 0, 1, 2);

        Label statusText = new Label("设置筛选条件后点击查询。默认最多显示 500 条。");
        statusText.getStyleClass().add("import-status");
        statusText.setWrapText(true);

        TableView<EventQueryResult> table = createEventTable();
        ObservableList<EventQueryResult> tableItems = FXCollections.observableArrayList();
        table.setItems(tableItems);

        PieChart cooperationSubtypeChart = new PieChart();
        cooperationSubtypeChart.getStyleClass().add("premium-pie-chart");
        cooperationSubtypeChart.setTitle("合作事件子类分布（04-06）");
        cooperationSubtypeChart.setLegendVisible(true);
        PieChart conflictSubtypeChart = new PieChart();
        conflictSubtypeChart.getStyleClass().add("premium-pie-chart");
        conflictSubtypeChart.setTitle("冲突事件子类分布（08-14）");
        conflictSubtypeChart.setLegendVisible(true);
        HBox subtypeCharts = new HBox(14, wrapChart(cooperationSubtypeChart), wrapChart(conflictSubtypeChart));
        subtypeCharts.getStyleClass().add("chart-row");

        searchButton.setOnAction(event -> {
            EventTypeOption selectedType = eventTypeBox.getSelectionModel().getSelectedItem();
            if (hasUnmatchedCountryInput(anyCountryField)
                    || hasUnmatchedCountryInput(actor1Field)
                    || hasUnmatchedCountryInput(actor2Field)) {
                statusText.setText("未匹配国家，请输入三位国家代码或重新选择候选国家。");
                return;
            }
            EventQueryCriteria criteria = new EventQueryCriteria(
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    extractCountryCode(anyCountryField),
                    extractCountryCode(actor1Field),
                    extractCountryCode(actor2Field),
                    selectedRegion(regionBox),
                    selectedType == null ? null : selectedType.type(),
                    EventQueryCriteria.DEFAULT_LIMIT
            );
            Task<EventQueryViewData> queryTask = new Task<>() {
                @Override
                protected EventQueryViewData call() {
                    EventQueryService service = new EventQueryService(new DatabaseManager(paths));
                    return new EventQueryViewData(
                            service.search(criteria),
                            service.subtypeDistribution(criteria, EventType.COOPERATION),
                            service.subtypeDistribution(criteria, EventType.CONFLICT)
                    );
                }
            };
            searchButton.setDisable(true);
            clearButton.setDisable(true);
            statusText.setText("正在查询，请稍候...");
            queryTask.setOnSucceeded(workerEvent -> {
                EventQueryViewData data = queryTask.getValue();
                tableItems.setAll(data.events());
                updateSubtypePieChart(cooperationSubtypeChart, data.cooperationSubtypes());
                updateSubtypePieChart(conflictSubtypeChart, data.conflictSubtypes());
                statusText.setText(data.events().isEmpty()
                        ? "未查询到符合条件的事件。"
                        : "查询完成，共显示 " + data.events().size() + " 条事件，并已刷新合作/冲突子类分布。");
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
            clearCountryComboBox(anyCountryField);
            clearCountryComboBox(actor1Field);
            clearCountryComboBox(actor2Field);
            regionBox.getSelectionModel().selectFirst();
            eventTypeBox.getSelectionModel().selectFirst();
            tableItems.clear();
            updateSubtypePieChart(cooperationSubtypeChart, List.of());
            updateSubtypePieChart(conflictSubtypeChart, List.of());
            statusText.setText("筛选条件已清空。");
        });

        body.getChildren().addAll(form, statusText, subtypeCharts, table);
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

    private ComboBox<CountryOption> createCountryCodeComboBox() {
        return createCountryCodeComboBox(false);
    }

    private ComboBox<CountryOption> createCountryCodeComboBox(boolean includeChina) {
        ComboBox<CountryOption> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.setPromptText("可输入代码、中文名或英文名");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        ObservableList<CountryOption> allOptions = FXCollections.observableArrayList(loadCountryOptions(includeChina));
        FilteredList<CountryOption> filteredOptions = new FilteredList<>(allOptions, option -> true);
        comboBox.setItems(filteredOptions);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CountryOption option) {
                return option == null ? "" : option.code();
            }

            @Override
            public CountryOption fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                String code = normalizeCountryInput(text);
                return allOptions.stream()
                        .filter(option -> option.code().equals(code))
                        .findFirst()
                        .orElse(new CountryOption(code, "", "", ""));
            }
        });
        comboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CountryOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayText());
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CountryOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.code());
            }
        });
        comboBox.setOnMouseClicked(event -> {
            if (!comboBox.isShowing()) {
                comboBox.show();
            }
        });
        comboBox.getEditor().setOnMouseClicked(event -> {
            if (!comboBox.isShowing()) {
                comboBox.show();
            }
        });
        comboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (comboBox.getSelectionModel().getSelectedItem() != null
                    && comboBox.getSelectionModel().getSelectedItem().code().equals(newValue)) {
                return;
            }
            String query = normalizeSearchText(newValue);
            filteredOptions.setPredicate(option -> query.isBlank() || option.matches(query));
            if (!query.isBlank() && !comboBox.isShowing()) {
                comboBox.show();
            }
        });
        return comboBox;
    }

    private ComboBox<String> createRegionComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getItems().add("全部区域");
        comboBox.getItems().addAll(loadRegions());
        comboBox.getSelectionModel().selectFirst();
        return comboBox;
    }

    private List<String> loadRegions() {
        try {
            return new CountryRepository(new DatabaseManager(paths)).findRegions();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private String selectedRegion(ComboBox<String> comboBox) {
        String value = comboBox.getSelectionModel().getSelectedItem();
        if (value == null || value.isBlank() || "全部区域".equals(value)) {
            return null;
        }
        return value;
    }

    private List<CountryOption> loadCountryOptions(boolean includeChina) {
        try {
            return new CountryRepository(new DatabaseManager(paths)).findAllCountries().stream()
                    .filter(Country::briCountry)
                    .filter(country -> includeChina
                            || !BilateralRelationService.DEFAULT_COUNTRY_A.equalsIgnoreCase(country.cameoCode()))
                    .map(country -> new CountryOption(
                            normalizeCountryInput(country.cameoCode()),
                            country.nameCn(),
                            country.nameEn(),
                            country.region()
                    ))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private String extractCountryCode(ComboBox<CountryOption> comboBox) {
        CountryOption selected = comboBox.getSelectionModel().getSelectedItem();
        String rawText = comboBox.getEditor().getText();
        if ((rawText == null || rawText.isBlank()) && selected != null) {
            return selected.code();
        }
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        String normalizedInput = normalizeSearchText(rawText);
        if (selected != null && selected.matches(normalizedInput)) {
            return selected.code();
        }
        return comboBox.getItems().stream()
                .filter(option -> option.exactMatches(normalizedInput))
                .findFirst()
                .or(() -> comboBox.getItems().stream()
                        .filter(option -> option.matches(normalizedInput))
                        .findFirst())
                .map(CountryOption::code)
                .orElse(normalizeCountryInput(rawText));
    }

    private boolean hasUnmatchedCountryInput(ComboBox<CountryOption> comboBox) {
        String rawText = comboBox.getEditor().getText();
        if (rawText == null || rawText.isBlank()) {
            return false;
        }
        String normalizedInput = normalizeSearchText(rawText);
        if (normalizedInput.matches("[A-Z]{3}")) {
            return false;
        }
        return comboBox.getItems().stream().noneMatch(option -> option.matches(normalizedInput));
    }

    private void clearCountryComboBox(ComboBox<CountryOption> comboBox) {
        comboBox.getSelectionModel().clearSelection();
        comboBox.getEditor().clear();
        if (comboBox.getItems() instanceof FilteredList<?> filteredList) {
            @SuppressWarnings("unchecked")
            FilteredList<CountryOption> typedList = (FilteredList<CountryOption>) filteredList;
            typedList.setPredicate(option -> true);
        }
    }

    private String normalizeCountryInput(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        int separator = trimmed.indexOf(' ');
        if (separator > 0) {
            trimmed = trimmed.substring(0, separator);
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toUpperCase(Locale.ROOT);
    }

    private TableView<EventQueryResult> createEventTable() {
        TableView<EventQueryResult> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("未查询到符合条件的事件。"));

        table.getColumns().add(valueColumn("事件ID", 110, EventQueryResult::globalEventId));
        table.getColumns().add(valueColumn("日期", 95, EventQueryResult::eventDate));
        table.getColumns().add(valueColumn("Actor1", 80, EventQueryResult::actor1CountryCode));
        table.getColumns().add(valueColumn("Actor2", 80, EventQueryResult::actor2CountryCode));
        table.getColumns().add(valueColumn("类型", 95, EventQueryResult::eventType));
        table.getColumns().add(valueColumn("Root", 70, EventQueryResult::eventRootCode));
        table.getColumns().add(valueColumn("Goldstein", 90, result -> "%.2f".formatted(result.goldsteinScale())));
        table.getColumns().add(valueColumn("Mentions", 90, EventQueryResult::numMentions));
        table.getColumns().add(valueColumn("AvgTone", 85, result -> "%.2f".formatted(result.avgTone())));
        table.getColumns().add(valueColumn("来源文件", 180, EventQueryResult::sourceFile));
        applyRowStyle(table, result -> eventTypeRowStyle(result.eventType()));
        table.setMinHeight(360);
        return table;
    }

    private TableView<MonthlyTrendPoint> createTrendTable() {
        TableView<MonthlyTrendPoint> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无月度趋势数据。"));
        table.getColumns().add(valueColumn("月份", 100, MonthlyTrendPoint::month));
        table.getColumns().add(valueColumn("总数", 90, MonthlyTrendPoint::totalEvents));
        table.getColumns().add(valueColumn("合作", 90, MonthlyTrendPoint::cooperationEvents));
        table.getColumns().add(valueColumn("冲突", 90, MonthlyTrendPoint::conflictEvents));
        table.getColumns().add(valueColumn("Avg Goldstein", 130, point -> "%.2f".formatted(point.averageGoldstein())));
        table.getColumns().add(valueColumn("Avg Tone", 120, point -> "%.2f".formatted(point.averageAvgTone())));
        applyRowStyle(table, point -> trendRowStyle(point.cooperationEvents(), point.conflictEvents()));
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
        applyRowStyle(table, score -> score.cooperationIndex() >= 65 ? "row-cooperation" : "row-neutral");
        table.setMinHeight(420);
        return table;
    }

    private TableView<CooperationHotspot> createHotspotTable() {
        TableView<CooperationHotspot> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("数据跨度不足或暂无合作指数增长国家。"));
        table.getColumns().add(valueColumn("国家", 80, CooperationHotspot::countryCode));
        table.getColumns().add(valueColumn("名称", 120, CooperationHotspot::countryName));
        table.getColumns().add(valueColumn("区域", 120, CooperationHotspot::region));
        table.getColumns().add(valueColumn("上月", 90, CooperationHotspot::previousMonth));
        table.getColumns().add(valueColumn("本月", 90, CooperationHotspot::currentMonth));
        table.getColumns().add(valueColumn("上月指数", 100, hotspot -> "%.1f".formatted(hotspot.previousIndex())));
        table.getColumns().add(valueColumn("本月指数", 100, hotspot -> "%.1f".formatted(hotspot.currentIndex())));
        table.getColumns().add(valueColumn("环比增长", 100, hotspot -> "%.1f".formatted(hotspot.growth())));
        table.getColumns().add(valueColumn("合作增量", 90, CooperationHotspot::cooperationEventIncrease));
        applyRowStyle(table, hotspot -> "row-hotspot");
        table.setMinHeight(260);
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
        applyRowStyle(table, risk -> riskRowStyle(risk.riskIndex(), risk.riskLevel()));
        table.setMinHeight(420);
        return table;
    }

    private TableView<RiskHotspot> createRiskHotspotTable() {
        TableView<RiskHotspot> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无可比较的风险热点数据。"));
        table.getColumns().add(valueColumn("国家", 80, RiskHotspot::countryCode));
        table.getColumns().add(valueColumn("名称", 120, RiskHotspot::countryName));
        table.getColumns().add(valueColumn("区域", 120, RiskHotspot::region));
        table.getColumns().add(valueColumn("上月", 90, RiskHotspot::previousMonth));
        table.getColumns().add(valueColumn("本月", 90, RiskHotspot::currentMonth));
        table.getColumns().add(valueColumn("上月指数", 100, hotspot -> "%.1f".formatted(hotspot.previousIndex())));
        table.getColumns().add(valueColumn("本月指数", 100, hotspot -> "%.1f".formatted(hotspot.currentIndex())));
        table.getColumns().add(valueColumn("环比增长", 100, hotspot -> "%.1f".formatted(hotspot.growth())));
        table.getColumns().add(valueColumn("冲突增量", 90, RiskHotspot::conflictEventIncrease));
        applyRowStyle(table, hotspot -> "row-risk-high");
        table.setMinHeight(260);
        return table;
    }

    private TableView<RegionSummary> createRegionTable() {
        TableView<RegionSummary> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无区域汇总数据。"));
        table.getColumns().add(valueColumn("区域", 130, RegionSummary::region));
        table.getColumns().add(valueColumn("国家数", 80, RegionSummary::countryCount));
        table.getColumns().add(valueColumn("参与事件", 100, RegionSummary::totalEvents));
        table.getColumns().add(valueColumn("合作", 90, RegionSummary::cooperationEvents));
        table.getColumns().add(valueColumn("冲突", 90, RegionSummary::conflictEvents));
        table.getColumns().add(valueColumn("Avg Goldstein", 120, summary -> "%.2f".formatted(summary.averageGoldstein())));
        table.getColumns().add(valueColumn("Avg Tone", 110, summary -> "%.2f".formatted(summary.averageAvgTone())));
        table.getColumns().add(valueColumn("Mentions", 100, RegionSummary::totalMentions));
        table.getColumns().add(valueColumn("合作指数", 100, summary -> "%.1f".formatted(summary.cooperationIndex())));
        table.getColumns().add(valueColumn("风险指数", 100, summary -> "%.1f".formatted(summary.riskIndex())));
        applyRowStyle(table, summary -> summary.riskIndex() > summary.cooperationIndex()
                ? "row-conflict"
                : "row-cooperation");
        table.setMinHeight(320);
        return table;
    }

    private TableView<CountryClusterResult> createClusterTable() {
        TableView<CountryClusterResult> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无聚类结果。"));
        table.getColumns().add(valueColumn("国家", 90, CountryClusterResult::countryCode));
        table.getColumns().add(valueColumn("名称", 110, CountryClusterResult::countryName));
        table.getColumns().add(valueColumn("区域", 120, CountryClusterResult::region));
        table.getColumns().add(valueColumn("事件量", 90, CountryClusterResult::totalEvents));
        table.getColumns().add(valueColumn("合作指数", 100, result -> "%.1f".formatted(result.cooperationIndex())));
        table.getColumns().add(valueColumn("风险指数", 100, result -> "%.1f".formatted(result.riskIndex())));
        table.getColumns().add(valueColumn("冲突占比", 100, result -> formatPercent(result.conflictRatio())));
        table.getColumns().add(valueColumn("聚类类别", 130, CountryClusterResult::clusterLabel));
        table.getColumns().add(valueColumn("解释", 260, CountryClusterResult::explanation));
        applyRowStyle(table, result -> clusterRowStyle(result.clusterLabel()));
        table.setMinHeight(420);
        return table;
    }

    private TableView<GeoEventPoint> createGeoEventTable() {
        TableView<GeoEventPoint> table = new TableView<>();
        table.getStyleClass().add("event-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无地理事件点位。"));
        table.getColumns().add(valueColumn("事件ID", 110, GeoEventPoint::globalEventId));
        table.getColumns().add(valueColumn("日期", 95, GeoEventPoint::eventDate));
        table.getColumns().add(valueColumn("Actor1", 80, GeoEventPoint::actor1CountryCode));
        table.getColumns().add(valueColumn("Actor2", 80, GeoEventPoint::actor2CountryCode));
        table.getColumns().add(valueColumn("类型", 95, GeoEventPoint::eventType));
        table.getColumns().add(valueColumn("纬度", 90, point -> "%.4f".formatted(point.latitude())));
        table.getColumns().add(valueColumn("经度", 90, point -> "%.4f".formatted(point.longitude())));
        table.getColumns().add(valueColumn("Goldstein", 95, point -> "%.2f".formatted(point.goldsteinScale())));
        table.getColumns().add(valueColumn("AvgTone", 90, point -> "%.2f".formatted(point.avgTone())));
        applyRowStyle(table, point -> eventTypeRowStyle(point.eventType()));
        table.setMinHeight(320);
        return table;
    }

    private <T> void applyRowStyle(TableView<T> table, Function<T, String> classifier) {
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(rowStyleClasses());
                if (!empty && item != null) {
                    String styleClass = classifier.apply(item);
                    if (styleClass != null && !styleClass.isBlank()) {
                        getStyleClass().add(styleClass);
                    }
                }
            }
        });
    }

    private List<String> rowStyleClasses() {
        return List.of(
                "row-cooperation",
                "row-conflict",
                "row-neutral",
                "row-hotspot",
                "row-risk-high",
                "row-cluster-a",
                "row-cluster-b",
                "row-cluster-c",
                "row-cluster-d"
        );
    }

    private String eventTypeRowStyle(EventType eventType) {
        if (eventType == EventType.COOPERATION) {
            return "row-cooperation";
        }
        if (eventType == EventType.CONFLICT) {
            return "row-conflict";
        }
        return "row-neutral";
    }

    private String trendRowStyle(int cooperationEvents, int conflictEvents) {
        if (conflictEvents > cooperationEvents) {
            return "row-conflict";
        }
        if (cooperationEvents > conflictEvents) {
            return "row-cooperation";
        }
        return "row-neutral";
    }

    private String riskRowStyle(double riskIndex, String riskLevel) {
        if (riskIndex >= 70 || "高".equals(riskLevel) || "极高".equals(riskLevel)) {
            return "row-risk-high";
        }
        if (riskIndex >= 45) {
            return "row-conflict";
        }
        return "row-neutral";
    }

    private String clusterRowStyle(String clusterLabel) {
        if (clusterLabel == null) {
            return "row-neutral";
        }
        if (clusterLabel.contains("深度")) {
            return "row-cluster-a";
        }
        if (clusterLabel.contains("稳定")) {
            return "row-cluster-b";
        }
        if (clusterLabel.contains("风险")) {
            return "row-cluster-c";
        }
        if (clusterLabel.contains("紧张")) {
            return "row-cluster-d";
        }
        return "row-neutral";
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

    private void updateRegionMetrics(List<RegionSummary> regions, Label regionCount, Label topCooperation,
                                     Label topRisk, Label totalEvents) {
        regionCount.setText(String.valueOf(regions.size()));
        int eventSum = regions.stream().mapToInt(RegionSummary::totalEvents).sum();
        totalEvents.setText(String.valueOf(eventSum));
        topCooperation.setText(regions.stream()
                .max(Comparator.comparingDouble(RegionSummary::cooperationIndex))
                .map(RegionSummary::region)
                .orElse("-"));
        topRisk.setText(regions.stream()
                .max(Comparator.comparingDouble(RegionSummary::riskIndex))
                .map(RegionSummary::region)
                .orElse("-"));
    }

    private void updateRegionChart(BarChart<String, Number> chart, List<RegionSummary> regions) {
        XYChart.Series<String, Number> cooperation = new XYChart.Series<>();
        cooperation.setName("合作指数");
        XYChart.Series<String, Number> risk = new XYChart.Series<>();
        risk.setName("风险指数");
        for (RegionSummary region : regions) {
            cooperation.getData().add(new XYChart.Data<>(region.region(), region.cooperationIndex()));
            risk.getData().add(new XYChart.Data<>(region.region(), region.riskIndex()));
        }
        chart.getData().setAll(cooperation, risk);
    }

    private void updateSubtypePieChart(PieChart chart, List<EventSubtypeStat> stats) {
        List<PieChart.Data> data = stats.stream()
                .filter(stat -> stat.eventCount() > 0)
                .map(stat -> new PieChart.Data(stat.displayLabel(), stat.eventCount()))
                .toList();
        chart.setData(FXCollections.observableArrayList(data));
    }

    private void updateHotspotChart(BarChart<String, Number> chart, List<CooperationHotspot> hotspots) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("环比增长");
        for (CooperationHotspot hotspot : hotspots) {
            series.getData().add(new XYChart.Data<>(hotspot.countryCode(), hotspot.growth()));
        }
        chart.getData().setAll(series);
    }

    private void updateRiskHotspotChart(BarChart<String, Number> chart, List<RiskHotspot> hotspots) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("环比增长");
        for (RiskHotspot hotspot : hotspots) {
            series.getData().add(new XYChart.Data<>(hotspot.countryCode(), hotspot.growth()));
        }
        chart.getData().setAll(series);
    }

    private Canvas createRegionRadarChart() {
        Canvas canvas = new Canvas(960, 440);
        canvas.setWidth(960);
        canvas.setHeight(440);
        canvas.setManaged(true);
        updateRegionRadarChart(canvas, List.of());
        return canvas;
    }

    private void updateRegionRadarChart(Canvas canvas, List<RegionSummary> regions) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        graphics.clearRect(0, 0, width, height);
        graphics.setFill(Color.web("#ffffff"));
        graphics.fillRect(0, 0, width, height);

        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
        graphics.setFill(Color.web("#25364a"));
        graphics.fillText("区域多指标雷达图", width / 2.0, 30);

        if (regions == null || regions.isEmpty()) {
            graphics.setFont(Font.font("Microsoft YaHei", 14));
            graphics.setFill(Color.web("#6a7d90"));
            graphics.fillText("暂无区域数据。", width / 2.0, height / 2.0);
            return;
        }

        String[] axes = {"合作指数", "风险指数", "事件活跃度", "媒体关注度", "Avg Goldstein"};
        double centerX = width * 0.38;
        double centerY = height * 0.54;
        double radius = Math.min(width * 0.25, height * 0.34);

        drawRadarGrid(graphics, axes, centerX, centerY, radius);
        drawRadarRegions(graphics, regions, axes.length, centerX, centerY, radius);
        drawRadarLegend(graphics, regions, width * 0.68, 78);
        drawRadarFootnote(graphics, width / 2.0, height - 20);
    }

    private void drawRadarGrid(GraphicsContext graphics, String[] axes, double centerX, double centerY, double radius) {
        graphics.setStroke(Color.web("#d8e0e8"));
        graphics.setLineWidth(1);
        for (int level = 1; level <= 4; level++) {
            double currentRadius = radius * level / 4.0;
            double[] xPoints = new double[axes.length];
            double[] yPoints = new double[axes.length];
            for (int index = 0; index < axes.length; index++) {
                double angle = radarAngle(index, axes.length);
                xPoints[index] = centerX + Math.cos(angle) * currentRadius;
                yPoints[index] = centerY + Math.sin(angle) * currentRadius;
            }
            graphics.strokePolygon(xPoints, yPoints, axes.length);
        }

        graphics.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 12));
        graphics.setFill(Color.web("#455a6f"));
        for (int index = 0; index < axes.length; index++) {
            double angle = radarAngle(index, axes.length);
            double axisX = centerX + Math.cos(angle) * radius;
            double axisY = centerY + Math.sin(angle) * radius;
            graphics.strokeLine(centerX, centerY, axisX, axisY);
            graphics.fillText(axes[index], centerX + Math.cos(angle) * (radius + 42),
                    centerY + Math.sin(angle) * (radius + 26));
        }
    }

    private void drawRadarRegions(GraphicsContext graphics, List<RegionSummary> regions, int axisCount,
                                  double centerX, double centerY, double radius) {
        double maxEvents = Math.max(1, regions.stream().mapToInt(RegionSummary::totalEvents).max().orElse(1));
        double maxMentions = Math.max(1, regions.stream().mapToInt(RegionSummary::totalMentions).max().orElse(1));
        for (int regionIndex = 0; regionIndex < regions.size(); regionIndex++) {
            RegionSummary region = regions.get(regionIndex);
            double[] values = regionRadarValues(region, maxEvents, maxMentions);
            double[] xPoints = new double[axisCount];
            double[] yPoints = new double[axisCount];
            for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
                double valueRadius = radius * clampPercent(values[axisIndex]) / 100.0;
                double angle = radarAngle(axisIndex, axisCount);
                xPoints[axisIndex] = centerX + Math.cos(angle) * valueRadius;
                yPoints[axisIndex] = centerY + Math.sin(angle) * valueRadius;
            }
            Color color = radarColor(regionIndex);
            graphics.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.10));
            graphics.fillPolygon(xPoints, yPoints, axisCount);
            graphics.setStroke(color);
            graphics.setLineWidth(2);
            graphics.strokePolygon(xPoints, yPoints, axisCount);
        }
    }

    private double[] regionRadarValues(RegionSummary region, double maxEvents, double maxMentions) {
        return new double[]{
                region.cooperationIndex(),
                region.riskIndex(),
                ratioToPercent(region.totalEvents(), maxEvents),
                ratioToPercent(region.totalMentions(), maxMentions),
                clampPercent((region.averageGoldstein() + 10.0) * 5.0)
        };
    }

    private void drawRadarLegend(GraphicsContext graphics, List<RegionSummary> regions, double x, double y) {
        graphics.setTextAlign(TextAlignment.LEFT);
        graphics.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 13));
        graphics.setFill(Color.web("#25364a"));
        graphics.fillText("区域图例", x, y);
        graphics.setFont(Font.font("Microsoft YaHei", 12));
        for (int index = 0; index < regions.size(); index++) {
            double itemY = y + 24 + index * 24;
            Color color = radarColor(index);
            graphics.setFill(color);
            graphics.fillRoundRect(x, itemY - 10, 18, 10, 4, 4);
            graphics.setFill(Color.web("#455a6f"));
            graphics.fillText(regions.get(index).region(), x + 28, itemY);
        }
    }

    private void drawRadarFootnote(GraphicsContext graphics, double x, double y) {
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFont(Font.font("Microsoft YaHei", 12));
        graphics.setFill(Color.web("#6a7d90"));
        graphics.fillText("说明：五个维度均归一化到 0-100；风险指数为原风险评分，事件活跃度和媒体关注度按当前区域最大值折算。", x, y);
    }

    private double radarAngle(int index, int axisCount) {
        return -Math.PI / 2.0 + index * 2.0 * Math.PI / axisCount;
    }

    private Color radarColor(int index) {
        String[] colors = {
                "#1f77b4", "#2ca02c", "#d62728", "#9467bd", "#ff7f0e",
                "#17becf", "#8c564b", "#e377c2", "#4d7c0f", "#475569"
        };
        return Color.web(colors[index % colors.length]);
    }

    private double ratioToPercent(double value, double max) {
        return max <= 0 ? 0 : clampPercent(value / max * 100.0);
    }

    private double clampPercent(double value) {
        return Math.max(0, Math.min(100, value));
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

    private String formatExportResult(ExportResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(result.displaySummary()).append(System.lineSeparator()).append(System.lineSeparator());
        for (int index = 0; index < result.files().size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(result.files().get(index))
                    .append(System.lineSeparator());
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
            case "数据维护" -> new String[]{
                    "选择 GDELT CSV、TSV 或 ZIP 文件并启动后台导入。",
                    "显示导入进度、成功行数、跳过行数和批次状态。",
                    "展示错误样例和导入摘要，便于排查数据质量问题。"
            };
            case "事件查询" -> new String[]{
                    "按日期范围、任一国家、Actor1、Actor2 和事件类型筛选。",
                    "以表格展示事件编号、日期、国家、事件类型和指标字段。",
                    "查询为空或日期错误时给出中文提示。"
            };
            case "双边关系" -> new String[]{
                    "默认以 CHN 为国家 A，输入沿线国家代码进行分析。",
                    "展示总事件数、合作/冲突占比、Goldstein、AvgTone 和媒体关注度。",
                    "提供月度趋势与双边事件明细，用于追踪双边关系在不同月份的变化过程。"
            };
            case "合作态势分析" -> new String[]{
                    "按国家聚合合作事件、冲突事件、媒体关注度和关系强度。",
                    "展示合作指数排名、最高合作国家和合作事件合计。",
                    "提供合作指数口径说明，便于解释分析模型。"
            };
            case "风险评估" -> new String[]{
                    "按国家聚合冲突事件、冲突占比、负向 Goldstein 和媒体语调。",
                    "展示风险指数排名、最高风险国家和高风险国家数量。",
                    "提供低、中、高、极高四档风险等级说明。"
            };
            case "专题地图" -> new String[]{
                    "使用 ActionGeo_Lat 和 ActionGeo_Long 绘制事件空间散点。",
                    "按合作、冲突、其他事件分类着色并展示空间分布研判。",
                    "以表格列出地理事件明细和核心指标。"
            };
            case "结果导出" -> new String[]{
                    "一键生成 TXT 汇总报告、合作排名 CSV 和风险排名 CSV。",
                    "显示导出耗时和生成文件路径。",
                    "导出文件可用于结果归档、报告整理和表格化分析。"
            };
            default -> new String[]{
                    "页面提供筛选条件和操作按钮。",
                    "核心区域展示图表、表格或地图结果。",
                    "明细区域提供记录、解释说明和后续操作入口。"
            };
        };
    }

    private static class InteractiveMapPane extends StackPane {
        private static final double MIN_ZOOM = 1.0;
        private static final double MAX_ZOOM = 8.0;

        private final Canvas canvas = new Canvas();
        private final Label hoverLabel = new Label();
        private final Consumer<GeoEventPoint> selectionHandler;
        private final BasemapLayer basemapLayer = BasemapLayer.loadDefault();
        private final CountryThematicLayer countryThematicLayer = CountryThematicLayer.loadDefault();
        private final List<GeoEventPoint> points = new ArrayList<>();
        private final List<CountryMapMetric> countryMetrics = new ArrayList<>();
        private double zoom = 1.0;
        private double panX;
        private double panY;
        private double dragStartX;
        private double dragStartY;
        private GeoEventPoint selectedCardPoint;
        private double cardX;
        private double cardY;
        private double cardWidth;
        private double cardHeight;
        private boolean showCountryTheme = true;
        private boolean showCooperation = true;
        private boolean showConflict = true;
        private boolean showOther = true;
        private boolean showRisk = true;
        private boolean showHeat = true;

        InteractiveMapPane(Consumer<GeoEventPoint> selectionHandler) {
            this.selectionHandler = selectionHandler;
            getStyleClass().add("interactive-map-pane");
            setMinWidth(0);
            setPrefWidth(960);
            setMaxWidth(Double.MAX_VALUE);
            setMinHeight(540);
            setPrefHeight(580);
            canvas.setManaged(false);
            widthProperty().addListener((observable, oldValue, newValue) -> draw());
            heightProperty().addListener((observable, oldValue, newValue) -> draw());

            hoverLabel.getStyleClass().add("map-hover-label");
            hoverLabel.setVisible(false);
            hoverLabel.setManaged(false);
            hoverLabel.setMouseTransparent(true);
            getChildren().addAll(canvas, hoverLabel);

            setOnScroll(event -> {
                double oldZoom = zoom;
                double factor = event.getDeltaY() > 0 ? 1.18 : 0.85;
                zoom = clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
                double centerX = mapCenterX();
                double centerY = mapCenterY();
                double mapDeltaX = (event.getX() - centerX - panX) / oldZoom;
                double mapDeltaY = (event.getY() - centerY - panY) / oldZoom;
                panX = event.getX() - centerX - mapDeltaX * zoom;
                panY = event.getY() - centerY - mapDeltaY * zoom;
                hidePointCard();
                constrainPan();
                draw();
                event.consume();
            });
            setOnMousePressed(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    dragStartX = event.getX();
                    dragStartY = event.getY();
                }
            });
            setOnMouseDragged(event -> {
                if (event.isPrimaryButtonDown()) {
                    panX += event.getX() - dragStartX;
                    panY += event.getY() - dragStartY;
                    dragStartX = event.getX();
                    dragStartY = event.getY();
                    hidePointCard();
                    constrainPan();
                    draw();
                }
            });
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    hidePointCard();
                    resetView();
                    return;
                }
                GeoEventPoint hit = findPoint(event.getX(), event.getY());
                if (hit == null) {
                    hidePointCard();
                    return;
                }
                showPointCard(hit);
                if (selectionHandler != null) {
                    selectionHandler.accept(hit);
                }
            });
            setOnMouseMoved(event -> {
                setCursor(findPoint(event.getX(), event.getY()) == null
                        ? javafx.scene.Cursor.DEFAULT
                        : javafx.scene.Cursor.CROSSHAIR);
            });
        }

        @Override
        protected void layoutChildren() {
            double width = Math.max(1, getWidth());
            double height = Math.max(1, getHeight());
            canvas.setWidth(width);
            canvas.setHeight(height);
            canvas.relocate(0, 0);
            constrainPan();
            draw();
        }

        void setPoints(List<GeoEventPoint> newPoints) {
            points.clear();
            if (newPoints != null) {
                points.addAll(newPoints);
            }
            draw();
        }

        List<GeoEventPoint> pointsSnapshot() {
            return List.copyOf(points);
        }

        void setCountryMetrics(List<CountryMapMetric> newMetrics) {
            countryMetrics.clear();
            if (newMetrics != null) {
                countryMetrics.addAll(newMetrics);
            }
            draw();
        }

        void setLayerState(boolean showCountryTheme, boolean showCooperation, boolean showConflict, boolean showOther,
                           boolean showRisk, boolean showHeat) {
            this.showCountryTheme = showCountryTheme;
            this.showCooperation = showCooperation;
            this.showConflict = showConflict;
            this.showOther = showOther;
            this.showRisk = showRisk;
            this.showHeat = showHeat;
            draw();
        }

        void resetView() {
            zoom = 1.0;
            panX = 0;
            panY = 0;
            draw();
        }

        private void draw() {
            double width = canvas.getWidth();
            double height = canvas.getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            GraphicsContext graphics = canvas.getGraphicsContext2D();
            graphics.clearRect(0, 0, width, height);
            drawBaseMap(graphics, width, height);
            if (showCountryTheme) {
                drawInsideMap(graphics, () -> drawCountryThemeLayer(graphics));
            }
            if (showHeat) {
                drawInsideMap(graphics, () -> drawHeatLayer(graphics));
            }
            if (showRisk) {
                drawInsideMap(graphics, () -> drawRiskLayer(graphics));
            }
            drawInsideMap(graphics, () -> drawEventPoints(graphics));
            drawMapHud(graphics, width, height);
            drawPointCardCallout(graphics);
        }

        private void drawCountryThemeLayer(GraphicsContext graphics) {
            countryThematicLayer.draw(graphics, countryMetrics, this::screenX, this::screenY);
        }

        private void drawBaseMap(GraphicsContext graphics, double width, double height) {
            graphics.setFill(Color.web("#f8fbfd"));
            graphics.fillRect(0, 0, width, height);
            double left = mapLeft();
            double top = mapTop();
            double mapWidth = mapWidth();
            double mapHeight = mapHeight();

            graphics.setFill(Color.web("#eef5f8"));
            graphics.fillRect(left, top, mapWidth, mapHeight);
            graphics.setStroke(Color.web("#bfd0dc"));
            graphics.setLineWidth(1.2);
            graphics.strokeRoundRect(left, top, mapWidth, mapHeight, 8, 8);

            graphics.save();
            graphics.beginPath();
            graphics.rect(left, top, mapWidth, mapHeight);
            graphics.clip();
            basemapLayer.draw(graphics, this::screenX, this::screenY);

            graphics.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
            graphics.setTextAlign(TextAlignment.CENTER);
            for (int lon = -180; lon <= 180; lon += 30) {
                double x = screenX(lon);
                graphics.setStroke(lon == 0 ? Color.web("#91a6b8") : Color.web("#d6e0e8"));
                graphics.setLineWidth(lon == 0 ? 1.2 : 0.8);
                graphics.strokeLine(x, screenY(-90), x, screenY(90));
            }
            graphics.setTextAlign(TextAlignment.RIGHT);
            for (int lat = -60; lat <= 90; lat += 30) {
                double y = screenY(lat);
                graphics.setStroke(lat == 0 ? Color.web("#91a6b8") : Color.web("#d6e0e8"));
                graphics.setLineWidth(lat == 0 ? 1.2 : 0.8);
                graphics.strokeLine(screenX(-180), y, screenX(180), y);
            }

            drawRegionBox(graphics, 25, 105, -10, 55, "BRI重点区");
            drawRegionBox(graphics, 60, 150, -15, 60, "亚欧通道");
            graphics.restore();

            graphics.setStroke(Color.web("#bfd0dc"));
            graphics.setLineWidth(1.4);
            graphics.strokeRoundRect(left, top, mapWidth, mapHeight, 8, 8);
            drawCoordinateLabels(graphics, width, height);
        }

        private void drawInsideMap(GraphicsContext graphics, Runnable drawAction) {
            graphics.save();
            graphics.beginPath();
            graphics.rect(mapLeft(), mapTop(), mapWidth(), mapHeight());
            graphics.clip();
            drawAction.run();
            graphics.restore();
        }

        private void drawCoordinateLabels(GraphicsContext graphics, double width, double height) {
            graphics.setFill(Color.web("#6a7d90"));
            graphics.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
            graphics.setTextAlign(TextAlignment.CENTER);
            double bottomLabelY = Math.min(height - 14, mapTop() + mapHeight() + 22);
            for (int lon = -180; lon <= 180; lon += 30) {
                double x = screenX(lon);
                if (x < mapLeft() + 2 || x > mapLeft() + mapWidth() - 2) {
                    continue;
                }
                graphics.fillText(lon + "°", x, bottomLabelY);
            }
            graphics.setTextAlign(TextAlignment.RIGHT);
            double leftLabelX = Math.max(42, mapLeft() - 10);
            for (int lat = -60; lat <= 90; lat += 30) {
                double y = screenY(lat);
                if (y < mapTop() + 2 || y > mapTop() + mapHeight() - 2) {
                    continue;
                }
                graphics.fillText(lat + "°", leftLabelX, y + 4);
            }
        }

        private void drawRegionBox(GraphicsContext graphics, double west, double east, double south, double north,
                                   String label) {
            double x1 = screenX(west);
            double x2 = screenX(east);
            double y1 = screenY(north);
            double y2 = screenY(south);
            graphics.setStroke(Color.web("#7ea4bd", 0.65));
            graphics.setLineWidth(1.4);
            graphics.setLineDashes(7, 5);
            graphics.strokeRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
            graphics.setLineDashes();
            graphics.setFill(Color.web("#58758a"));
            graphics.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 11));
            graphics.fillText(label, Math.min(x1, x2) + 48, Math.min(y1, y2) + 16);
        }

        private void drawHeatLayer(GraphicsContext graphics) {
            int lonBuckets = 36;
            int latBuckets = 18;
            int[][] buckets = new int[lonBuckets][latBuckets];
            int max = 0;
            for (GeoEventPoint point : points) {
                if (!isVisibleType(point)) {
                    continue;
                }
                int lonIndex = (int) clamp(Math.floor((point.longitude() + 180.0) / 10.0), 0, lonBuckets - 1);
                int latIndex = (int) clamp(Math.floor((point.latitude() + 90.0) / 10.0), 0, latBuckets - 1);
                buckets[lonIndex][latIndex]++;
                max = Math.max(max, buckets[lonIndex][latIndex]);
            }
            if (max == 0) {
                return;
            }
            for (int lonIndex = 0; lonIndex < lonBuckets; lonIndex++) {
                for (int latIndex = 0; latIndex < latBuckets; latIndex++) {
                    int count = buckets[lonIndex][latIndex];
                    if (count == 0) {
                        continue;
                    }
                    double lon = lonIndex * 10.0 - 175.0;
                    double lat = latIndex * 10.0 - 85.0;
                    double intensity = 0.18 + 0.34 * count / max;
                    double radius = (10 + 20 * count / (double) max) * Math.sqrt(zoom);
                    graphics.setFill(Color.web("#22c7d8", intensity));
                    graphics.fillOval(screenX(lon) - radius, screenY(lat) - radius, radius * 2, radius * 2);
                }
            }
        }

        private void drawRiskLayer(GraphicsContext graphics) {
            for (GeoEventPoint point : points) {
                if (!isVisibleType(point) || !isRiskPoint(point)) {
                    continue;
                }
                double x = screenX(point.longitude());
                double y = screenY(point.latitude());
                double radius = 7.5 + Math.min(8, Math.abs(Math.min(point.avgTone(), point.goldsteinScale())));
                graphics.setStroke(Color.web("#ff8bc8", 0.56));
                graphics.setLineWidth(1.8);
                graphics.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
            }
        }

        private void drawEventPoints(GraphicsContext graphics) {
            for (GeoEventPoint point : points) {
                if (!isVisibleType(point)) {
                    continue;
                }
                double x = screenX(point.longitude());
                double y = screenY(point.latitude());
                if (x < mapLeft() - 20 || y < mapTop() - 20
                        || x > mapLeft() + mapWidth() + 20 || y > mapTop() + mapHeight() + 20) {
                    continue;
                }
                if (point.eventType() == EventType.COOPERATION) {
                    graphics.setFill(Color.web("#17894f", 0.9));
                    graphics.fillOval(x - 4.4, y - 4.4, 8.8, 8.8);
                } else if (point.eventType() == EventType.CONFLICT) {
                    graphics.setFill(Color.web("#c73737", 0.92));
                    graphics.fillPolygon(new double[]{x, x - 5.4, x + 5.4}, new double[]{y - 5.8, y + 4.6, y + 4.6}, 3);
                } else {
                    graphics.setFill(Color.web("#3e6fa3", 0.82));
                    graphics.fillRect(x - 3.8, y - 3.8, 7.6, 7.6);
                }
            }
        }

        private void drawMapHud(GraphicsContext graphics, double width, double height) {
            graphics.setFill(Color.web("#ffffff", 0.86));
            graphics.fillRoundRect(14, 14, 228, 176, 8, 8);
            graphics.setStroke(Color.web("#d8e0e8"));
            graphics.strokeRoundRect(14, 14, 228, 176, 8, 8);
            graphics.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 12));
            graphics.setFill(Color.web("#25364a"));
            graphics.setTextAlign(TextAlignment.LEFT);
            graphics.fillText("图例", 28, 36);
            drawLegendItem(graphics, 30, 56, Color.web("#17894f"), "合作事件", "circle");
            drawLegendItem(graphics, 30, 76, Color.web("#c73737"), "冲突事件", "triangle");
            drawLegendItem(graphics, 30, 96, Color.web("#3e6fa3"), "其他事件", "square");
            drawLegendItem(graphics, 30, 116, Color.web("#ff8bc8", 0.72), "风险热点圈", "ring");
            drawLegendItem(graphics, 30, 136, Color.web("#22c7d8"), "热度层", "heat");
            drawLegendItem(graphics, 30, 156, Color.web("#72c987", 0.68), "国家合作等级", "country");
            drawLegendItem(graphics, 30, 176, Color.web("#2563eb", 0.42), "合作事件气泡", "bubble");

            graphics.setFill(Color.web("#ffffff", 0.86));
            graphics.fillRoundRect(width - 260, height - 48, 244, 32, 8, 8);
            graphics.setStroke(Color.web("#d8e0e8"));
            graphics.strokeRoundRect(width - 260, height - 48, 244, 32, 8, 8);
            graphics.setFill(Color.web("#455a6f"));
            graphics.setFont(Font.font("Segoe UI", 12));
            graphics.setTextAlign(TextAlignment.LEFT);
            graphics.fillText("Zoom " + "%.2f".formatted(zoom) + "x  Center "
                    + "%.1f".formatted(centerLongitude()) + "°, "
                    + "%.1f".formatted(centerLatitude()) + "°", width - 246, height - 27);
        }

        private void drawLegendItem(GraphicsContext graphics, double x, double y, Color color, String label, String shape) {
            graphics.setFill(color);
            if ("triangle".equals(shape)) {
                graphics.fillPolygon(new double[]{x + 5, x, x + 10}, new double[]{y - 6, y + 5, y + 5}, 3);
            } else if ("square".equals(shape)) {
                graphics.fillRect(x, y - 6, 10, 10);
            } else if ("ring".equals(shape)) {
                graphics.setStroke(color);
                graphics.setLineWidth(2);
                graphics.strokeOval(x, y - 8, 11, 11);
            } else if ("heat".equals(shape)) {
                graphics.setFill(Color.web("#22c7d8", 0.42));
                graphics.fillOval(x - 2, y - 9, 15, 15);
            } else if ("country".equals(shape)) {
                graphics.setFill(Color.web("#72c987", 0.62));
                graphics.fillRect(x, y - 8, 5, 12);
                graphics.setFill(Color.web("#f0d66d", 0.62));
                graphics.fillRect(x + 5, y - 8, 5, 12);
                graphics.setFill(Color.web("#ee8378", 0.62));
                graphics.fillRect(x + 10, y - 8, 5, 12);
            } else if ("bubble".equals(shape)) {
                graphics.setFill(Color.web("#2563eb", 0.22));
                graphics.fillOval(x - 2, y - 10, 16, 16);
                graphics.setStroke(Color.web("#1d4ed8", 0.58));
                graphics.setLineWidth(1.1);
                graphics.strokeOval(x - 2, y - 10, 16, 16);
            } else {
                graphics.fillOval(x, y - 7, 10, 10);
            }
            graphics.setFill(Color.web("#455a6f"));
            graphics.setFont(Font.font("Microsoft YaHei", 11));
            graphics.fillText(label, x + 18, y + 2);
        }

        private GeoEventPoint findPoint(double mouseX, double mouseY) {
            if (mouseX < mapLeft() || mouseY < mapTop()
                    || mouseX > mapLeft() + mapWidth() || mouseY > mapTop() + mapHeight()) {
                return null;
            }
            GeoEventPoint best = null;
            double bestDistance = 14.0;
            for (GeoEventPoint point : points) {
                if (!isVisibleType(point)) {
                    continue;
                }
                double distance = Math.hypot(screenX(point.longitude()) - mouseX, screenY(point.latitude()) - mouseY);
                if (distance < bestDistance) {
                    best = point;
                    bestDistance = distance;
                }
            }
            return best;
        }

        private String formatHoverText(GeoEventPoint point) {
            return "事件 " + point.globalEventId() + "  " + displayValue(point.eventDate())
                    + "\n参与方 " + displayCountryCode(point.actor1CountryCode())
                    + " -> " + displayCountryCode(point.actor2CountryCode())
                    + "  类型 " + displayValue(point.eventType())
                    + "\n纬度 " + "%.4f".formatted(point.latitude())
                    + "  经度 " + "%.4f".formatted(point.longitude())
                    + "\nGoldstein " + "%.2f".formatted(point.goldsteinScale())
                    + "  AvgTone " + "%.2f".formatted(point.avgTone());
        }

        private String displayCountryCode(String value) {
            return displayValue(value, "未知");
        }

        private String displayValue(Object value) {
            return displayValue(value, "-");
        }

        private String displayValue(Object value, String fallback) {
            if (value == null) {
                return fallback;
            }
            String text = value.toString().trim();
            return text.isEmpty() || "null".equalsIgnoreCase(text) ? fallback : text;
        }

        private void drawPointCardCallout(GraphicsContext graphics) {
            if (selectedCardPoint == null) {
                return;
            }
            double pointX = screenX(selectedCardPoint.longitude());
            double pointY = screenY(selectedCardPoint.latitude());
            if (pointX < mapLeft() || pointY < mapTop()
                    || pointX > mapLeft() + mapWidth() || pointY > mapTop() + mapHeight()) {
                return;
            }
            double endX = pointX < cardX ? cardX : cardX + cardWidth;
            double endY = clamp(pointY, cardY + 16, cardY + cardHeight - 16);
            double elbowX = endX + (pointX < cardX ? -28 : 28);
            graphics.setStroke(Color.web("#111111", 0.92));
            graphics.setLineWidth(2.2);
            graphics.strokeLine(pointX, pointY, elbowX, endY);
            graphics.strokeLine(elbowX, endY, endX, endY);
            graphics.setFill(Color.web("#111111", 0.92));
            graphics.fillOval(pointX - 3, pointY - 3, 6, 6);

            graphics.setFill(Color.web("#ffffff", 0.97));
            graphics.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 6, 6);
            graphics.setStroke(Color.web("#111111"));
            graphics.setLineWidth(3.0);
            graphics.strokeRoundRect(cardX, cardY, cardWidth, cardHeight, 6, 6);

            String[] lines = formatHoverText(selectedCardPoint).split("\\R");
            graphics.setFill(Color.web("#1f2937"));
            graphics.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 12));
            graphics.setTextAlign(TextAlignment.LEFT);
            graphics.fillText(lines[0], cardX + 14, cardY + 23);
            graphics.setFont(Font.font("Microsoft YaHei", 12));
            for (int i = 1; i < lines.length; i++) {
                graphics.fillText(lines[i], cardX + 14, cardY + 23 + i * 20);
            }
        }

        private void showPointCard(GeoEventPoint point) {
            selectedCardPoint = point;
            hoverLabel.setVisible(false);
            cardWidth = 330;
            cardHeight = 106;
            double pointX = screenX(point.longitude());
            double pointY = screenY(point.latitude());
            double preferredX = pointX + 82;
            if (preferredX + cardWidth > canvas.getWidth() - 14) {
                preferredX = pointX - cardWidth - 82;
            }
            cardX = clamp(preferredX, 14, Math.max(14, canvas.getWidth() - cardWidth - 14));
            cardY = clamp(pointY - cardHeight - 36, 14, Math.max(14, canvas.getHeight() - cardHeight - 14));
            draw();
        }

        private void hidePointCard() {
            selectedCardPoint = null;
            hoverLabel.setVisible(false);
        }

        private boolean isVisibleType(GeoEventPoint point) {
            if (point.eventType() == EventType.COOPERATION) {
                return showCooperation;
            }
            if (point.eventType() == EventType.CONFLICT) {
                return showConflict;
            }
            return showOther;
        }

        private boolean isRiskPoint(GeoEventPoint point) {
            return point.eventType() == EventType.CONFLICT || point.avgTone() <= -3.0 || point.goldsteinScale() <= -2.0;
        }

        private double screenX(double longitude) {
            double base = mapLeft() + (longitude + 180.0) / 360.0 * mapWidth();
            return mapCenterX() + (base - mapCenterX()) * zoom + panX;
        }

        private double screenY(double latitude) {
            double base = mapTop() + (90.0 - latitude) / 180.0 * mapHeight();
            return mapCenterY() + (base - mapCenterY()) * zoom + panY;
        }

        private double centerLongitude() {
            double mapCenterDelta = (mapCenterX() - panX - mapCenterX()) / zoom;
            double baseX = mapCenterX() + mapCenterDelta;
            return clamp((baseX - mapLeft()) / mapWidth() * 360.0 - 180.0, -180.0, 180.0);
        }

        private double centerLatitude() {
            double mapCenterDelta = (mapCenterY() - panY - mapCenterY()) / zoom;
            double baseY = mapCenterY() + mapCenterDelta;
            return clamp(90.0 - (baseY - mapTop()) / mapHeight() * 180.0, -90.0, 90.0);
        }

        private void constrainPan() {
            double maxX = Math.max(0, (zoom - MIN_ZOOM) * mapWidth() / 2.0);
            double maxY = Math.max(0, (zoom - MIN_ZOOM) * mapHeight() / 2.0);
            panX = clamp(panX, -maxX, maxX);
            panY = clamp(panY, -maxY, maxY);
        }

        private double mapLeft() {
            return 64;
        }

        private double mapTop() {
            return 56;
        }

        private double mapWidth() {
            return Math.max(1, canvas.getWidth() - 112);
        }

        private double mapHeight() {
            return Math.max(1, canvas.getHeight() - 120);
        }

        private double mapCenterX() {
            return mapLeft() + mapWidth() / 2.0;
        }

        private double mapCenterY() {
            return mapTop() + mapHeight() / 2.0;
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private record PageSpec(String title, String description) {
    }

    private record EventTypeOption(String label, EventType type) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record CountryOption(String code, String nameCn, String nameEn, String region) {
        String displayText() {
            StringBuilder builder = new StringBuilder(code);
            if (nameCn != null && !nameCn.isBlank()) {
                builder.append(" - ").append(nameCn);
            }
            if (nameEn != null && !nameEn.isBlank()) {
                builder.append(" / ").append(nameEn);
            }
            if (region != null && !region.isBlank()) {
                builder.append("（").append(region).append("）");
            }
            return builder.toString();
        }

        boolean matches(String query) {
            if (query == null || query.isBlank()) {
                return true;
            }
            return normalizeForMatch(code).contains(query)
                    || normalizeForMatch(nameCn).contains(query)
                    || normalizeForMatch(nameEn).contains(query)
                    || normalizeForMatch(region).contains(query);
        }

        boolean exactMatches(String query) {
            if (query == null || query.isBlank()) {
                return false;
            }
            return normalizeForMatch(code).equals(query)
                    || normalizeForMatch(nameCn).equals(query)
                    || normalizeForMatch(nameEn).equals(query)
                    || normalizeForMatch(displayText()).equals(query);
        }

        private String normalizeForMatch(String value) {
            return value == null ? "" : value.toUpperCase(Locale.ROOT);
        }
    }

    private record BilateralViewData(
            BilateralRelationSummary summary,
            List<MonthlyTrendPoint> trends,
            List<EventQueryResult> events
    ) {
    }

    private record CooperationAnalysisViewData(
            List<CooperationScore> rankings,
            List<CooperationHotspot> hotspots
    ) {
    }

    private record RiskAssessmentViewData(
            List<RiskAssessment> rankings,
            List<RiskHotspot> hotspots
    ) {
    }

    private record DashboardViewData(
            DashboardSummary summary,
            List<CountryEventStat> topCountries,
            List<MonthlyTrendPoint> dailyTrend
    ) {
    }

    private record EventQueryViewData(
            List<EventQueryResult> events,
            List<EventSubtypeStat> cooperationSubtypes,
            List<EventSubtypeStat> conflictSubtypes
    ) {
    }

    private record MapSeriesLoadResult(
            List<LocalDate> availableDates,
            Map<LocalDate, List<GeoEventPoint>> series,
            List<CountryMapMetric> countryMetrics
    ) {
    }
}
