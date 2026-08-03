package com.ediqa.api.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that writes an Extent HTML report into target/extent-reports/.
 * Registered in testng.xml so no annotation scanning is required.
 */
public class ExtentReportListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(ExtentReportListener.class);

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    @Override
    public void onStart(ITestContext context) {
        ExtentSparkReporter spark =
            new ExtentSparkReporter("target/extent-reports/ExtentReport.html");
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("EDI-QA API Test Report");
        spark.config().setReportName("API Test Results");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Framework", "EDI-QA-Framework");
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        log.info("Extent report initialised");
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = extent.createTest(
            result.getMethod().getMethodName(),
            result.getMethod().getDescription()
        );
        testThread.set(test);
        log.info("Test started: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testThread.get().pass("Test passed");
        log.info("Test passed: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        testThread.get().fail(result.getThrowable());
        log.error("Test FAILED: {}", result.getMethod().getMethodName(), result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        testThread.get().skip("Test skipped");
        log.warn("Test skipped: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
            log.info("Extent report written → target/extent-reports/ExtentReport.html");
        }
    }
}
