package edu.course.brigdelt.ui;

import edu.course.brigdelt.domain.CountryMapMetric;
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
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

final class CountryThematicLayer {

    private static final String COUNTRY_RESOURCE = "/map/bri-country-lowres.tsv";

    private final List<CountryPolygon> polygons;

    private CountryThematicLayer(List<CountryPolygon> polygons) {
        this.polygons = List.copyOf(polygons);
    }

    static CountryThematicLayer loadDefault() {
        try (InputStream inputStream = CountryThematicLayer.class.getResourceAsStream(COUNTRY_RESOURCE)) {
            if (inputStream == null) {
                return new CountryThematicLayer(List.of());
            }
            return parse(inputStream);
        } catch (RuntimeException | IOException exception) {
            return new CountryThematicLayer(List.of());
        }
    }

    void draw(GraphicsContext graphics, List<CountryMapMetric> metrics,
              DoubleUnaryOperator screenX, DoubleUnaryOperator screenY) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        Map<String, CountryMapMetric> metricsByCode = metrics.stream()
                .collect(Collectors.toMap(
                        metric -> normalizeCode(metric.countryCode()),
                        metric -> metric,
                        (left, right) -> left
                ));
        int maxCooperationEvents = Math.max(1, metrics.stream()
                .mapToInt(CountryMapMetric::cooperationEvents)
                .max()
                .orElse(1));

        drawCountryFills(graphics, metricsByCode, screenX, screenY);
        drawCountryBubbles(graphics, metrics, maxCooperationEvents, screenX, screenY);
    }

    private void drawCountryFills(GraphicsContext graphics, Map<String, CountryMapMetric> metricsByCode,
                                  DoubleUnaryOperator screenX, DoubleUnaryOperator screenY) {
        graphics.setLineWidth(0.9);
        for (CountryPolygon polygon : polygons) {
            CountryMapMetric metric = metricsByCode.get(normalizeCode(polygon.countryCode()));
            if (metric == null) {
                continue;
            }
            graphics.setFill(countryFill(metric));
            graphics.setStroke(Color.web("#ffffff", 0.76));
            drawPolygon(graphics, polygon.points(), screenX, screenY);
        }
    }

    private void drawCountryBubbles(GraphicsContext graphics, List<CountryMapMetric> metrics, int maxCooperationEvents,
                                    DoubleUnaryOperator screenX, DoubleUnaryOperator screenY) {
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        for (CountryMapMetric metric : metrics) {
            if (metric.cooperationEvents() <= 0) {
                continue;
            }
            double x = screenX.applyAsDouble(metric.longitude());
            double y = screenY.applyAsDouble(metric.latitude());
            double ratio = Math.sqrt(metric.cooperationEvents() / (double) maxCooperationEvents);
            double radius = 4.5 + ratio * 15.5;
            graphics.setFill(Color.web("#2563eb", 0.22));
            graphics.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            graphics.setStroke(Color.web("#1d4ed8", 0.58));
            graphics.setLineWidth(1.1);
            graphics.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
            if (radius >= 11) {
                graphics.setFill(Color.web("#1e3a5f", 0.72));
                graphics.fillText(metric.countryCode(), x, y + 3.5);
            }
        }
    }

    private static Color countryFill(CountryMapMetric metric) {
        if (metric.totalEvents() <= 0) {
            return Color.web("#d7dee8", 0.34);
        }
        double index = clamp(metric.cooperationIndex(), 0, 100);
        if (index >= 70) {
            return Color.web("#72c987", 0.54);
        }
        if (index >= 45) {
            return Color.web("#f0d66d", 0.52);
        }
        return Color.web("#ee8378", 0.48);
    }

    private static CountryThematicLayer parse(InputStream inputStream) throws IOException {
        List<CountryPolygon> polygons = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\t");
                if (parts.length < 5 || !"COUNTRY".equals(parts[0])) {
                    continue;
                }
                polygons.add(new CountryPolygon(parts[1], parseDouble(parts[2]), parseDouble(parts[3]), parsePoints(parts[4])));
            }
        }
        return new CountryThematicLayer(polygons);
    }

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

    private static void drawPolygon(GraphicsContext graphics, List<MapPoint> points,
                                    DoubleUnaryOperator screenX, DoubleUnaryOperator screenY) {
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

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record CountryPolygon(String countryCode, double centerLongitude, double centerLatitude,
                                  List<MapPoint> points) {
    }

    private record MapPoint(double longitude, double latitude) {
    }
}
