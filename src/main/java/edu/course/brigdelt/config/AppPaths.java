package edu.course.brigdelt.config;

import java.nio.file.Path;
import java.util.List;

/**
 * Central definition of runtime directories required by the course assignment.
 */
public final class AppPaths {

    public static final Path ROOT = Path.of("D:", "Temp", "BRI-GDELT-System");
    private static final String COUNTRY_CONFIG_RESOURCE = "/config/countries.json";

    private final Path rootDir;
    private final Path inputDir;
    private final Path sampleDir;
    private final Path databaseDir;
    private final Path exportDir;
    private final Path reportDir;
    private final Path logDir;
    private final Path cacheDir;
    private final Path databaseFile;

    public AppPaths() {
        this.rootDir = ROOT;
        this.inputDir = rootDir.resolve("input");
        this.sampleDir = rootDir.resolve("sample");
        this.databaseDir = rootDir.resolve("database");
        this.exportDir = rootDir.resolve("exports");
        this.reportDir = rootDir.resolve("reports");
        this.logDir = rootDir.resolve("logs");
        this.cacheDir = rootDir.resolve("cache");
        this.databaseFile = databaseDir.resolve("bri_gdelt.db");
    }

    public Path rootDir() {
        return rootDir;
    }

    public Path inputDir() {
        return inputDir;
    }

    public Path sampleDir() {
        return sampleDir;
    }

    /**
     * Backward-compatible alias for older UI labels. Runtime data now lives in input/.
     */
    public Path dataDir() {
        return inputDir;
    }

    public Path databaseDir() {
        return databaseDir;
    }

    public Path exportDir() {
        return exportDir;
    }

    public Path reportDir() {
        return reportDir;
    }

    public Path logDir() {
        return logDir;
    }

    public Path cacheDir() {
        return cacheDir;
    }

    public List<Path> runtimeDirectories() {
        return List.of(inputDir, sampleDir, databaseDir, exportDir, reportDir, logDir, cacheDir);
    }

    public Path databaseFile() {
        return databaseFile;
    }

    public String countryConfigResource() {
        return COUNTRY_CONFIG_RESOURCE;
    }

    /**
     * Backward-compatible display value. Country configuration is loaded from classpath resources.
     */
    public Path countryConfigFile() {
        return Path.of(COUNTRY_CONFIG_RESOURCE.substring(1));
    }
}
