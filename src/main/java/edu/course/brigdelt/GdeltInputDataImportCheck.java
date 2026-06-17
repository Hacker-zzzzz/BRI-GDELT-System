package edu.course.brigdelt;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.domain.EventType;
import edu.course.brigdelt.domain.ImportBatchStatus;
import edu.course.brigdelt.domain.ImportResult;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.repository.GdeltEventRepository;
import edu.course.brigdelt.service.GdeltImportService;
import edu.course.brigdelt.service.StartupService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Imports real GDELT input files from the runtime input directory.
 */
public final class GdeltInputDataImportCheck {

    private GdeltInputDataImportCheck() {
    }

    public static void main(String[] args) {
        AppPaths paths = new StartupService().initialize();
        DatabaseManager databaseManager = new DatabaseManager(paths);
        GdeltImportService importService = new GdeltImportService(databaseManager);
        GdeltEventRepository eventRepository = new GdeltEventRepository(databaseManager);

        List<Path> inputFiles = findInputFiles(paths.inputDir());
        int successfulBatches = 0;

        System.out.println("Input directory: " + paths.inputDir().toAbsolutePath());
        System.out.println("Database file: " + paths.databaseFile().toAbsolutePath());
        System.out.println("Total files: " + inputFiles.size());

        for (Path file : inputFiles) {
            ImportResult result = importService.importFile(file);
            if (result.status() != ImportBatchStatus.FAILED) {
                successfulBatches++;
            }
            System.out.printf(
                    Locale.ROOT,
                    "Imported %-32s status=%s total=%d matched=%d inserted=%d skipped=%d durationMs=%d%n",
                    file.getFileName(),
                    result.status(),
                    result.totalRows(),
                    result.successRows(),
                    result.insertedRows(),
                    result.skippedRows(),
                    result.durationMillis()
            );
        }

        System.out.println("Successful import batches: " + successfulBatches);
        System.out.println("GDELT event total: " + eventRepository.countEvents());
        System.out.println("Cooperation events: " + eventRepository.countEventsByType(EventType.COOPERATION));
        System.out.println("Conflict events: " + eventRepository.countEventsByType(EventType.CONFLICT));
    }

    private static List<Path> findInputFiles(Path inputDir) {
        if (!Files.isDirectory(inputDir)) {
            return List.of();
        }
        try (var stream = Files.list(inputDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(GdeltInputDataImportCheck::isSupportedInputFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan input directory: " + inputDir, exception);
        }
    }

    private static boolean isSupportedInputFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") || name.endsWith(".tsv") || name.endsWith(".zip");
    }
}
