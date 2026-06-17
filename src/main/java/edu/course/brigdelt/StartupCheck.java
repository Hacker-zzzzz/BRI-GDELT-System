package edu.course.brigdelt;

import edu.course.brigdelt.config.AppPaths;
import edu.course.brigdelt.repository.CountryRepository;
import edu.course.brigdelt.repository.DatabaseManager;
import edu.course.brigdelt.service.StartupService;

/**
 * Console smoke check for runtime directory and database initialization.
 */
public final class StartupCheck {

    private StartupCheck() {
    }

    public static void main(String[] args) {
        AppPaths paths = new StartupService().initialize();
        DatabaseManager databaseManager = new DatabaseManager(paths);
        CountryRepository countryRepository = new CountryRepository(databaseManager);
        System.out.println("运行根目录: " + paths.rootDir());
        System.out.println("输入目录: " + paths.inputDir());
        System.out.println("样例目录: " + paths.sampleDir());
        System.out.println("数据库目录: " + paths.databaseDir());
        System.out.println("导出目录: " + paths.exportDir());
        System.out.println("报告目录: " + paths.reportDir());
        System.out.println("日志目录: " + paths.logDir());
        System.out.println("缓存目录: " + paths.cacheDir());
        System.out.println("数据库文件: " + paths.databaseFile());
        System.out.println("国家配置资源: " + paths.countryConfigResource());
        System.out.println("国家配置数量: " + countryRepository.countCountries());
        System.out.println("中国配置存在: " + countryRepository.existsByCameoCode("CHN"));
        System.out.println("启动检查通过。");
    }
}
