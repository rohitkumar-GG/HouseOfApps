package com.project.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.appium.java_client.android.AndroidDriver;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import java.io.File;

public class ReportManager {
    public static ExtentReports extent;
    public static ExtentTest suiteLog;
    private static ExtentTest currentStepLog;
    public static ExtentTest testLog;

    public static int totalBugCount = 0;
    private static int currentStepBugs = 0;

    public static void setupReport(String appTarget) {
        ExtentSparkReporter spark = new ExtentSparkReporter(System.getProperty("user.dir") + "/target/HouseOfApps_Report.html");
        spark.config().setDocumentTitle("House Of Apps - QA Dashboard");
        spark.config().setReportName("Automation Execution Suite");
        spark.config().setTheme(Theme.DARK);

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("QA Engineer", "Automation Triggered");

        suiteLog = extent.createTest(appTarget + " Identifier Execution", "Full End-to-End Suite for " + appTarget);
        testLog = suiteLog;
    }

    public static void resetStepCounters() {
        currentStepBugs = 0;
    }

    public static void beginTestStep(String stepName, String description) {
        logStep("\n===============================================");
        logStep("▶ STARTING: " + stepName);
        if (suiteLog != null) {
            currentStepLog = suiteLog.createNode(stepName, description);
            testLog = currentStepLog;
        }
        currentStepBugs = 0;
    }

    public static void endTestStep() {
        if (currentStepBugs == 0) {
            logStep("✅ PASS: No bugs found in this step.");
            if (currentStepLog != null) currentStepLog.pass("Step passed successfully.");
        } else {
            logStep("❌ FAIL: " + currentStepBugs + " bug(s) found in this step.");
            if (currentStepLog != null) currentStepLog.fail(currentStepBugs + " bug(s) recorded in this flow.");
        }
    }

    public static void logStep(String message) {
        String ANSI_RESET = "\u001B[0m";
        String ANSI_CYAN_BOLD = "\033[1;36m";
        String ANSI_YELLOW = "\033[0;33m";
        String ANSI_GREEN_BOLD = "\033[1;32m";
        String ANSI_PURPLE_BOLD = "\033[1;35m";

        if (message.contains("==========") || message.contains("[STEP") || message.contains("STARTING:")) {
            System.out.println(ANSI_CYAN_BOLD + message + ANSI_RESET);
        } else if (message.contains("[ACTION]")) {
            System.out.println(ANSI_YELLOW + message + ANSI_RESET);
        } else if (message.contains("✅") || message.contains("[TEST]")) {
            System.out.println(ANSI_GREEN_BOLD + message + ANSI_RESET);
        } else if (message.contains("[SYSTEM]")) {
            System.out.println(ANSI_PURPLE_BOLD + message + ANSI_RESET);
        } else {
            System.out.println(message);
        }

        if (testLog != null) testLog.info(message);
    }

    public static void reportBug(AndroidDriver driver, String bugMessage, String screenshotName) {
        totalBugCount++;
        currentStepBugs++;
        System.out.println("\u001B[31m" + "❌ BUG FOUND: " + bugMessage + "\u001B[0m");
        try {
            File scrFile = driver.getScreenshotAs(OutputType.FILE);
            String fileName = screenshotName + "_" + System.currentTimeMillis() + ".png";
            String filePath = System.getProperty("user.dir") + "/target/bug_screenshots/" + fileName;
            FileUtils.copyFile(scrFile, new File(filePath));

            if (currentStepLog != null) {
                currentStepLog.fail("BUG: " + bugMessage, MediaEntityBuilder.createScreenCaptureFromPath(filePath).build());
            } else if (testLog != null) {
                testLog.fail("BUG: " + bugMessage, MediaEntityBuilder.createScreenCaptureFromPath(filePath).build());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to take screenshot: " + e.getMessage());
        }
    }

    public static void flushReport() {
        if (extent != null) extent.flush();
    }
}