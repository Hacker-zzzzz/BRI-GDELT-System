package edu.course.brigdelt;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.service.StartupService;

/**
 * Console smoke check for runtime directory and database initialization.
 */
public final class StartupCheck {

    private StartupCheck() {
    }

    public static void main(String[] args) {
        AppPaths paths = new StartupService().initialize();
        System.out.println("Runtime root: " + paths.rootDir());
        System.out.println("Database file: " + paths.databaseFile());
        System.out.println("Country config: " + paths.countryConfigFile());
        System.out.println("Startup check passed.");
    }
}
