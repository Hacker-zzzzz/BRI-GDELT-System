package edu.course.brigdelt.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * JavaFX 地图底图图层，负责加载低精度世界陆地轮廓并绘制基础地理参照。
 *
 * <p>底图数据来自 classpath 下的 TSV 资源；资源缺失或解析失败时使用内置简化轮廓，
 * 保证交互地图页面仍然可用。</p>
 */
final class BasemapLayer {

    private static final String BASEMAP_RESOURCE = "/map/world-land-lowres.tsv";

    private final List<Polygon> polygons;
    private final List<MapLabel> labels;

    private BasemapLayer(List<Polygon> polygons, List<MapLabel> labels) {
        this.polygons = List.copyOf(polygons);
        this.labels = List.copyOf(labels);
    }

    /**
     * 加载默认世界底图。这里做容错处理，避免地图资源文件损坏时影响主界面启动。
     */
    static BasemapLayer loadDefault() {
        try (InputStream inputStream = BasemapLayer.class.getResourceAsStream(BASEMAP_RESOURCE)) {
            if (inputStream == null) {
                return fallback();
            }
            return parse(inputStream);
        } catch (RuntimeException | IOException exception) {
            return fallback();
        }
    }

    /**
     * 将经纬度轮廓投影到 Canvas 坐标系，绘制陆地区块和洲际文字标签。
     */
    void draw(GraphicsContext graphics, DoubleUnaryOperator screenX, DoubleUnaryOperator screenY) {
        graphics.setFill(Color.web("#dfeadf", 0.95));
        graphics.setStroke(Color.web("#adc4ad", 0.96));
        graphics.setLineWidth(1.05);
        for (Polygon polygon : polygons) {
            drawPolygon(graphics, polygon, screenX, screenY);
        }

        graphics.setFill(Color.web("#789677", 0.72));
        graphics.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        graphics.setTextAlign(TextAlignment.CENTER);
        for (MapLabel label : labels) {
            graphics.fillText(label.text(), screenX.applyAsDouble(label.longitude()), screenY.applyAsDouble(label.latitude()));
        }
    }

    /**
     * 解析底图 TSV：LAND 行表示多边形，LABEL 行表示地图文字标注。
     */
    private static BasemapLayer parse(InputStream inputStream) throws IOException {
        List<Polygon> polygons = new ArrayList<>();
        List<MapLabel> labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\t");
                if (parts.length < 2) {
                    continue;
                }
                // LABEL 使用中心经纬度定位，主要用于增强地图识别度。
                if ("LABEL".equals(parts[0]) && parts.length >= 4) {
                    labels.add(new MapLabel(parts[1], parseDouble(parts[2]), parseDouble(parts[3])));
                } else if ("LAND".equals(parts[0]) && parts.length >= 3) {
                    // LAND 的点串格式为 lon,lat lon,lat，用于绘制封闭陆地轮廓。
                    polygons.add(new Polygon(parts[1], parsePoints(parts[2])));
                }
            }
        }
        if (polygons.isEmpty()) {
            return fallback();
        }
        return new BasemapLayer(polygons, labels);
    }

    /**
     * 将资源文件中的经纬度点串转换为地图多边形点集合。
     */
    private static List<MapPoint> parsePoints(String rawPoints) {
        List<MapPoint> points = new ArrayList<>();
        for (String rawPoint : rawPoints.split("\\s+")) {
            String[] lonLat = rawPoint.split(",");
            if (lonLat.length != 2) {
                continue;
            }
            points.add(new MapPoint(parseDouble(lonLat[0]), parseDouble(lonLat[1])));
        }
        return points;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    /**
     * 绘制单个陆地多边形；点数不足时跳过，避免 Canvas API 抛出无效图形。
     */
    private static void drawPolygon(GraphicsContext graphics, Polygon polygon,
                                    DoubleUnaryOperator screenX, DoubleUnaryOperator screenY) {
        List<MapPoint> points = polygon.points();
        if (points.size() < 3) {
            return;
        }
        double[] xPoints = new double[points.size()];
        double[] yPoints = new double[points.size()];
        for (int index = 0; index < points.size(); index++) {
            MapPoint point = points.get(index);
            xPoints[index] = screenX.applyAsDouble(point.longitude());
            yPoints[index] = screenY.applyAsDouble(point.latitude());
        }
        graphics.fillPolygon(xPoints, yPoints, points.size());
        graphics.strokePolygon(xPoints, yPoints, points.size());
    }

    /**
     * 内置简化世界轮廓，用作资源加载失败时的降级方案。
     */
    private static BasemapLayer fallback() {
        List<Polygon> fallbackPolygons = List.of(
                polygon("north_america", "-168,55 -150,70 -125,72 -105,62 -82,58 -62,50 -55,36 -74,18 -96,15 -116,26 -130,45 -150,50"),
                polygon("south_america", "-82,12 -70,10 -55,-5 -42,-20 -52,-48 -68,-55 -78,-36 -82,-12"),
                polygon("greenland", "-52,60 -42,75 -25,75 -20,62 -34,58"),
                polygon("europe", "-10,35 5,58 35,62 60,55 46,38 28,33 12,36"),
                polygon("africa", "-18,35 12,36 35,30 50,10 42,-20 28,-35 12,-35 -4,-18 -14,5"),
                polygon("asia", "35,35 52,55 88,67 126,58 150,46 142,20 116,8 104,-8 78,8 62,18 42,22"),
                polygon("australia", "112,-12 154,-12 150,-38 132,-44 114,-32"),
                polygon("antarctica", "-180,-64 -120,-68 -55,-63 12,-69 78,-64 160,-66 180,-64 180,-84 -180,-84")
        );
        List<MapLabel> fallbackLabels = List.of(
                new MapLabel("North America", -108, 48),
                new MapLabel("South America", -62, -22),
                new MapLabel("Europe", 18, 50),
                new MapLabel("Africa", 20, 4),
                new MapLabel("Asia", 92, 40),
                new MapLabel("Oceania", 136, -26)
        );
        return new BasemapLayer(fallbackPolygons, fallbackLabels);
    }

    private static Polygon polygon(String id, String rawPoints) {
        return new Polygon(id, parsePoints(rawPoints));
    }

    private record Polygon(String id, List<MapPoint> points) {
    }

    private record MapPoint(double longitude, double latitude) {
    }

    private record MapLabel(String text, double longitude, double latitude) {
    }
}
