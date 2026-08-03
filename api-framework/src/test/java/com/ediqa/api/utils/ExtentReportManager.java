package com.ediqa.api.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Singleton that owns the {@link ExtentReports} instance and per-thread
 * {@link ExtentTest} references.
 *
 * <p>{@link com.ediqa.api.listeners.ExtentReportListener} delegates all
 * lifecycle calls here, so test classes can also add step-level logging via
 * {@link #getTest()} without coupling directly to the listener.
 *
 * <p>The report path is {@code target/extent-reports/ExtentReport.html}.
 * System info automatically reflects the active environment as resolved by
 * {@link ConfigManager}.
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);
    /** Resolved on first initialisation; includes the active environment name. */
    private static String reportPath;

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    private ExtentReportManager() {}

    /**
     * Returns the shared {@link ExtentReports} instance, creating and
     * configuring it on the first call.
     * <p>The HTML report is written to
     * {@code reports/ExtentReport-<environment>.html} so qa and prod runs
     * never overwrite each other.
     */
    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            ConfigManager config = ConfigManager.getInstance();
            reportPath = "reports/ExtentReport-" + config.getEnvironment() + ".html";

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle("EDI-QA API Test Report");
            spark.config().setReportName("API Test Results");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Framework",    "EDI-QA-Framework");
            extent.setSystemInfo("Environment",  config.getEnvironment());
            extent.setSystemInfo("Base URL",     config.getBaseUrl());
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));

            log.info("ExtentReports initialised → {}", reportPath);
        }
        return extent;
    }

    /** Returns the {@link ExtentTest} bound to the current thread. */
    public static ExtentTest getTest() {
        return testThread.get();
    }

    /** Binds an {@link ExtentTest} to the current thread. */
    public static void setTest(ExtentTest test) {
        testThread.set(test);
    }

    /** Removes the {@link ExtentTest} binding for the current thread. */
    public static void removeTest() {
        testThread.remove();
    }

    /** Flushes the report to disk. */
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            log.info("Extent report flushed → {}", reportPath);
        }
    }
}
