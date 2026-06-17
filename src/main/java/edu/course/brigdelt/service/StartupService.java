package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.Country;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Performs first-run setup before the main window is displayed.
 */
public class StartupService {

    public AppPaths initialize() {
        AppPaths paths = new AppPaths();
        createRuntimeDirectories(paths);
        DatabaseManager databaseManager = new DatabaseManager(paths);
        databaseManager.initializeSchema();
        loadCountryConfig(databaseManager);
        return paths;
    }

    private void createRuntimeDirectories(AppPaths paths) {
        createAndCheckDirectory(paths.rootDir());
        for (Path directory : paths.runtimeDirectories()) {
            createAndCheckDirectory(directory);
        }
    }

    private void createAndCheckDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("运行时目录创建失败：" + directory, exception);
        }
        assertReadableWritableDirectory(directory);
    }

    private void assertReadableWritableDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("运行时路径不是目录：" + directory);
        }
        if (!Files.isReadable(directory)) {
            throw new IllegalStateException("运行时目录不可读：" + directory);
        }
        if (!Files.isWritable(directory)) {
            throw new IllegalStateException("运行时目录不可写：" + directory);
        }
    }

    private void loadCountryConfig(DatabaseManager databaseManager) {
        List<Country> countries = new CountryConfigService().loadCountries();
        new CountryRepository(databaseManager).upsertAll(countries);
    }
}
