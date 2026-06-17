package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.repository.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Performs first-run setup before the main window is displayed.
 */
public class StartupService {

    private static final String DEFAULT_COUNTRY_CONFIG = "/config/countries.json";

    public AppPaths initialize() {
        AppPaths paths = new AppPaths();
        createRuntimeDirectories(paths);
        copyDefaultCountryConfig(paths);
        new DatabaseManager(paths).initializeSchema();
        return paths;
    }

    private void createRuntimeDirectories(AppPaths paths) {
        List<java.nio.file.Path> directories = List.of(
                paths.rootDir(),
                paths.dataDir(),
                paths.databaseDir(),
                paths.exportDir(),
                paths.logDir(),
                paths.configDir()
        );
        for (java.nio.file.Path directory : directories) {
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                throw new IllegalStateException("运行时目录创建失败：" + directory, exception);
            }
        }
    }

    private void copyDefaultCountryConfig(AppPaths paths) {
        if (Files.exists(paths.countryConfigFile())) {
            return;
        }
        try (InputStream inputStream = StartupService.class.getResourceAsStream(DEFAULT_COUNTRY_CONFIG)) {
            if (inputStream == null) {
                throw new IllegalStateException("缺少默认国家配置：" + DEFAULT_COUNTRY_CONFIG);
            }
            Files.copy(inputStream, paths.countryConfigFile(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("默认国家配置复制失败：" + paths.countryConfigFile(), exception);
        }
    }
}
