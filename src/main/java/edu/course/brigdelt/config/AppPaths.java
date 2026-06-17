package edu.course.brigdelt.config;

import java.nio.file.Path;

/**
 * Central definition of runtime directories required by the course assignment.
 */
public final class AppPaths {

    public static final Path ROOT = Path.of("D:", "Temp", "BRI-GDELT-System");

    private final Path rootDir;
    private final Path dataDir;
    private final Path databaseDir;
    private final Path exportDir;
    private final Path logDir;
    private final Path configDir;
    private final Path databaseFile;
    private final Path countryConfigFile;

    public AppPaths() {
        this.rootDir = ROOT;
        this.dataDir = rootDir.resolve("data");
        this.databaseDir = rootDir.resolve("db");
        this.exportDir = rootDir.resolve("exports");
        this.logDir = rootDir.resolve("logs");
        this.configDir = rootDir.resolve("config");
        this.databaseFile = databaseDir.resolve("bri_gdelt.db");
        this.countryConfigFile = configDir.resolve("countries.json");
    }

    public Path rootDir() {
        return rootDir;
    }

    public Path dataDir() {
        return dataDir;
    }

    public Path databaseDir() {
        return databaseDir;
    }

    public Path exportDir() {
        return exportDir;
    }

    public Path logDir() {
        return logDir;
    }

    public Path configDir() {
        return configDir;
    }

    public Path databaseFile() {
        return databaseFile;
    }

    public Path countryConfigFile() {
        return countryConfigFile;
    }
}
