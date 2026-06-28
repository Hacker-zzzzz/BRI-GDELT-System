package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.GdeltEvent;
import edu.course.brigdelt.domain.ImportBatchStatus;
import edu.course.brigdelt.domain.ImportResult;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;
import edu.course.brigdelt.repository.ImportBatchRepository;
import edu.course.brigdelt.util.GdeltLineParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * GDELT 事件导入服务，负责文件校验、解析、过滤、清洗和批量入库。
 *
 * <p>导入流程支持 CSV/TSV/ZIP，按项目需要解析 GDELT 字段，筛选命中配置国家的
 * 事件，并将非法经纬度清理为空值。批量写入可以减少 SQLite 频繁提交带来的性能
 * 开销，适合一次导入较多 GDELT 文件的演示场景。</p>
 */
public class GdeltImportService {

    private static final int BATCH_SIZE = 1_000;
    private static final int MAX_ERROR_SAMPLES = 10;

    private final CountryRepository countryRepository;
    private final GdeltEventRepository gdeltEventRepository;
    private final ImportBatchRepository importBatchRepository;

    public GdeltImportService() {
        this(new DatabaseManager(new AppPaths()));
    }

    public GdeltImportService(DatabaseManager databaseManager) {
        this.countryRepository = new CountryRepository(databaseManager);
        this.gdeltEventRepository = new GdeltEventRepository(databaseManager);
        this.importBatchRepository = new ImportBatchRepository(databaseManager);
    }

    /**
     * 导入单个 GDELT 文件，统一处理普通文本和 ZIP 压缩包。
     */
    public ImportResult importFile(Path file) {
        Instant startedAt = Instant.now();
        ImportCounters counters = new ImportCounters(fileName(file));
        List<GdeltEvent> pendingEvents = new ArrayList<>(BATCH_SIZE);

        try {
            // 先做文件类型和存在性校验，避免后台任务进入半导入状态。
            validateFile(file);
            Set<String> countryCodes = countryRepository.findAllCameoCodes();
            if (countryCodes.isEmpty()) {
                counters.addSample("国家配置为空：countries 表中没有可用的 CAMEO code。");
                return finish(counters, startedAt);
            }

            if (isZip(file)) {
                importZip(file, countryCodes, counters, pendingEvents);
            } else {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    importReader(reader, file.getFileName().toString(), countryCodes, counters, pendingEvents);
                }
            }
            counters.insertedRows += flush(pendingEvents);
        } catch (IOException exception) {
            counters.addSample("读取文件失败：" + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            counters.addSample(exception.getMessage());
        }

        return finish(counters, startedAt);
    }

    /**
     * 逐个读取 ZIP 内部条目，复用普通文本导入逻辑处理每个 GDELT 文件。
     */
    private void importZip(Path file, Set<String> countryCodes, ImportCounters counters, List<GdeltEvent> pendingEvents)
            throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String sourceFile = file.getFileName() + "!" + entry.getName();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(nonClosing(zipInputStream), StandardCharsets.UTF_8));
                    importReader(reader, sourceFile, countryCodes, counters, pendingEvents);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    /**
     * 逐行解析 GDELT 数据：解析失败、国家不匹配的行都会计入跳过统计。
     */
    private void importReader(BufferedReader reader, String sourceFile, Set<String> countryCodes,
                              ImportCounters counters, List<GdeltEvent> pendingEvents) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            counters.totalRows++;
            GdeltLineParser.ParseResult parseResult = GdeltLineParser.parseDetailed(line, sourceFile);
            if (!parseResult.success()) {
                counters.skippedRows++;
                counters.addSample("第 " + counters.totalRows + " 行解析失败：" + parseResult.errorMessage());
                continue;
            }

            // 清洗后再判断国家命中，保证入库数据中的地图字段始终可控。
            GdeltEvent cleanedEvent = cleanCoordinates(parseResult.event().orElseThrow());
            if (!matchesConfiguredCountry(cleanedEvent, countryCodes)) {
                counters.skippedRows++;
                counters.addSample("第 " + counters.totalRows + " 行已跳过：Actor1/Actor2 未命中国家配置。");
                continue;
            }

            pendingEvents.add(cleanedEvent);
            counters.successRows++;
            if (pendingEvents.size() >= BATCH_SIZE) {
                counters.insertedRows += flush(pendingEvents);
            }
        }
    }

    /**
     * 达到批量阈值后写入数据库，减少 SQLite 单行提交的性能开销。
     */
    private int flush(List<GdeltEvent> pendingEvents) {
        int insertedRows = gdeltEventRepository.insertIgnoreBatch(pendingEvents);
        pendingEvents.clear();
        return insertedRows;
    }

    /**
     * 清理非法经纬度：超出地球范围的坐标置空，避免地图绘制异常。
     */
    private GdeltEvent cleanCoordinates(GdeltEvent event) {
        Double latitude = isLatitudeValid(event.actionGeoLat()) ? event.actionGeoLat() : null;
        Double longitude = isLongitudeValid(event.actionGeoLon()) ? event.actionGeoLon() : null;
        if (latitude == event.actionGeoLat() && longitude == event.actionGeoLon()) {
            return event;
        }
        return new GdeltEvent(
                event.globalEventId(),
                event.eventDate(),
                event.actor1CountryCode(),
                event.actor2CountryCode(),
                event.eventCode(),
                event.eventBaseCode(),
                event.eventRootCode(),
                event.eventType(),
                event.goldsteinScale(),
                event.numMentions(),
                event.avgTone(),
                latitude,
                longitude,
                event.sourceFile()
        );
    }

    /**
     * 只保留 Actor1 或 Actor2 命中配置国家的事件，聚焦一带一路沿线分析范围。
     */
    private boolean matchesConfiguredCountry(GdeltEvent event, Set<String> countryCodes) {
        return containsCode(countryCodes, event.actor1CountryCode())
                || containsCode(countryCodes, event.actor2CountryCode());
    }

    private boolean containsCode(Set<String> countryCodes, String code) {
        return code != null && countryCodes.contains(code.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isLatitudeValid(Double latitude) {
        return latitude == null || (latitude >= -90.0 && latitude <= 90.0);
    }

    private boolean isLongitudeValid(Double longitude) {
        return longitude == null || (longitude >= -180.0 && longitude <= 180.0);
    }

    /**
     * 校验导入文件，只允许课程数据来源中常见的 CSV、TSV 和 ZIP。
     */
    private void validateFile(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("导入文件不能为空。");
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("导入文件不存在或不是普通文件：" + file);
        }
        String lowerName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".tsv") && !lowerName.endsWith(".zip")) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 .CSV、.tsv、.zip：" + file.getFileName());
        }
    }

    private boolean isZip(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    /**
     * 汇总导入结果并写入导入批次表，便于首页和报告展示导入历史。
     */
    private ImportResult finish(ImportCounters counters, Instant startedAt) {
        ImportBatchStatus status = statusOf(counters);
        ImportResult result = new ImportResult(
                counters.fileName,
                counters.totalRows,
                counters.successRows,
                counters.skippedRows,
                counters.insertedRows,
                status,
                errorSummary(counters, status),
                Duration.between(startedAt, Instant.now()).toMillis(),
                counters.errorSamples
        );
        importBatchRepository.insert(result);
        return result;
    }

    private ImportBatchStatus statusOf(ImportCounters counters) {
        if (counters.successRows == 0) {
            return ImportBatchStatus.FAILED;
        }
        if (counters.skippedRows > 0) {
            return ImportBatchStatus.PARTIAL;
        }
        return ImportBatchStatus.SUCCESS;
    }

    private String errorSummary(ImportCounters counters, ImportBatchStatus status) {
        if (status == ImportBatchStatus.SUCCESS) {
            return "导入成功。";
        }
        if (counters.successRows == 0) {
            return "导入失败：没有成功导入的有效事件。";
        }
        return "部分导入成功：跳过 " + counters.skippedRows + " 行。";
    }

    private String fileName(Path file) {
        return file == null || file.getFileName() == null ? "" : file.getFileName().toString();
    }

    private InputStream nonClosing(InputStream inputStream) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                return inputStream.read(bytes, offset, length);
            }

            @Override
            public void close() {
                // ZipInputStream ownership stays with importZip so the next entry can be read.
            }
        };
    }

    private static final class ImportCounters {
        private final String fileName;
        private final List<String> errorSamples = new ArrayList<>();
        private int totalRows;
        private int successRows;
        private int skippedRows;
        private int insertedRows;

        private ImportCounters(String fileName) {
            this.fileName = fileName;
        }

        private void addSample(String message) {
            // 只保留少量错误样例，避免大文件导入时错误文本占用过多内存。
            if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                errorSamples.add(message);
            }
        }
    }
}
