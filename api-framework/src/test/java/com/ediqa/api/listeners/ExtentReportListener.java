package com.ediqa.api.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.ediqa.api.utils.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that delegates Extent report lifecycle to
 * {@link ExtentReportManager}.  Registered in testng.xml.
 */
public class ExtentReportListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(ExtentReportListener.class);

    @Override
    public void onStart(ITestContext context) {
        ExtentReportManager.getInstance(); // eagerly initialise; stamps environment info
        log.info("Extent report initialised for suite: {}", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = ExtentReportManager.getInstance().createTest(
            result.getMethod().getMethodName(),
            result.getMethod().getDescription()
        );
        ExtentReportManager.setTest(test);
        log.info("Test started: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentReportManager.getTest().pass("Test passed");
        log.info("Test passed: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentReportManager.getTest().fail(result.getThrowable());
        log.error("Test FAILED: {}", result.getMethod().getMethodName(), result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentReportManager.getTest().skip("Test skipped");
        log.warn("Test skipped: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flush();
        log.info("Suite finished: {}", context.getName());
    }
}
