package edu.course.brigdelt.service;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.Country;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * 启动初始化服务。
 *
 * <p>在主窗口展示前创建运行目录、安装演示数据库、初始化 SQLite schema，
 * 并加载国家配置，保证现场演示可以直接进入可用状态。</p>
 */
public class StartupService {

    /**
     * 执行完整启动准备流程，返回 UI 和服务层共用的运行路径配置。
     */
    public AppPaths initialize() {
        AppPaths paths = new AppPaths();
        createRuntimeDirectories(paths);
        installSeedDatabaseIfNeeded(paths);
        DatabaseManager databaseManager = new DatabaseManager(paths);
        databaseManager.initializeSchema();
        loadCountryConfig(databaseManager);
        return paths;
    }

    /**
     * 创建课程要求的 D:\Temp 运行目录结构。
     */
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

    /**
     * 每次启动同步国家配置，保证资源文件更新后数据库国家表及时刷新。
     */
    private void loadCountryConfig(DatabaseManager databaseManager) {
        List<Country> countries = new CountryConfigService().loadCountries();
        new CountryRepository(databaseManager).upsertAll(countries);
    }

    /**
     * 如果运行目录没有可用数据库，则尝试安装打包附带的演示数据库。
     */
    private void installSeedDatabaseIfNeeded(AppPaths paths) {
        Path databaseFile = paths.databaseFile();
        if (hasUsableDatabase(databaseFile)) {
            return;
        }

        Optional<Path> seedDatabase = findSeedDatabase();
        if (seedDatabase.isEmpty()) {
            return;
        }

        try {
            Files.copy(seedDatabase.get(), databaseFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("棰勭疆 SQLite 鏁版嵁搴撳畨瑁呭け璐ワ細" + seedDatabase.get(), exception);
        }
    }

    private boolean hasUsableDatabase(Path databaseFile) {
        try {
            return Files.isRegularFile(databaseFile) && Files.size(databaseFile) > 0;
        } catch (IOException exception) {
            return false;
        }
    }

    private Optional<Path> findSeedDatabase() {
        String explicitSeed = System.getProperty("brigdelt.seedDatabase");
        if (explicitSeed != null && !explicitSeed.isBlank()) {
            Path seed = Path.of(explicitSeed);
            if (Files.isRegularFile(seed)) {
                return Optional.of(seed);
            }
        }

        for (Path appDir : candidateAppDirectories()) {
            Path seed = appDir.resolve("seed").resolve("database").resolve("bri_gdelt.db");
            if (Files.isRegularFile(seed)) {
                return Optional.of(seed);
            }
        }
        return Optional.empty();
    }

    private List<Path> candidateAppDirectories() {
        List<Path> candidates = new java.util.ArrayList<>();
        candidates.add(Path.of("").toAbsolutePath());

        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(File.pathSeparator)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Path path = Path.of(entry).toAbsolutePath();
            Path parent = Files.isDirectory(path) ? path : path.getParent();
            if (parent != null && !candidates.contains(parent)) {
                candidates.add(parent);
            }
        }
        return candidates;
    }
}
