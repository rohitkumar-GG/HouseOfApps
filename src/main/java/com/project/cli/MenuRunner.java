// File: src/main/java/com/project/cli/MenuRunner.java
package com.project.cli;

import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MenuRunner {

    public static final String RESET = "\033[0m";
    public static final String BOLD = "\033[1m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String PURPLE = "\033[35m";
    public static final String CYAN = "\033[36m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println(CYAN + BOLD + "======================================================" + RESET);
        System.out.println(CYAN + BOLD + "      🚀 HOUSE OF APPS - AUTOMATION TERMINAL 🚀      " + RESET);
        System.out.println(CYAN + BOLD + "======================================================" + RESET);

        System.out.println(YELLOW + "\n[TARGET SELECTION]" + RESET);
        System.out.println("1. Stone Identifier");
        System.out.println("2. Coin Identifier");
        System.out.println("3. File Recovery" + RESET);
        System.out.print(GREEN + "\nEnter choice (1-3): " + RESET);

        int appChoice;
        try {
            appChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            System.out.println(RED + "Invalid input. Exiting." + RESET);
            return;
        }

        if (appChoice == 1) {
            System.setProperty("targetApp", "Stone");
            runStoneMenu(scanner);
        } else if (appChoice == 2) {
            System.setProperty("targetApp", "Coin");
            System.out.println(PURPLE + "\n[PRE-CONDITION] App will be forcibly wiped and cold-started offline." + RESET);
            executeTestNGClass("com.project.tests.coin.CoinMasterTest");
        } else if (appChoice == 3) {
            System.setProperty("targetApp", "FileRecovery");
            System.out.println(PURPLE + "\n[PRE-CONDITION] Network will be dropped on start to verify Offline UI." + RESET);
            executeTestNGClass("com.project.tests.recovery.FileRecoveryTest");
        } else {
            System.out.println(RED + "\nInvalid selection. Exiting." + RESET);
        }
    }

    private static void runStoneMenu(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n--- \uD83E\uDEA8 STONE IDENTIFIER SUITE ---" + RESET);
        System.out.println(YELLOW + "Select execution mode:" + RESET);
        System.out.println("1. " + BOLD + "Master Onboarding Flow" + RESET + " (Full 25-Step Suite - Wipes App Data)");
        System.out.println("2. Camera & AI Identification Test (Isolated)");
        System.out.println("3. Collection & Deletion Routing Test (Isolated)");
        System.out.println("4. Search & Carousel Navigation Test (Isolated)");
        System.out.print(GREEN + "\nEnter choice (1-4): " + RESET);

        int testChoice;
        try {
            testChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            System.out.println(RED + "Invalid input. Exiting." + RESET);
            return;
        }

        String targetClass = "";

        switch (testChoice) {
            case 1:
                System.out.println(PURPLE + "\n[PRE-CONDITION] App will be forcibly wiped and cold-started offline." + RESET);
                targetClass = "com.project.tests.stone.StoneOnboardingTest"; // UPDATED PACKAGE
                break;
            case 2:
                System.out.println(PURPLE + "\n[PRE-CONDITION REQUIRED] 🚧" + RESET);
                System.out.println("1. Ensure the app is installed and past the FTUE pages.");
                System.out.println("2. The app MUST be resting on the 'Home' Tab.");
                System.out.println("3. Ensure the internet is ON.");
                waitForUser(scanner);
                targetClass = "com.project.tests.stone.IdentifyFeatureTest";
                break;
            case 3:
                System.out.println(PURPLE + "\n[PRE-CONDITION REQUIRED] 🚧" + RESET);
                System.out.println("1. The app MUST have at least 1 stone saved in 'My Rocks'.");
                System.out.println("2. The app MUST be resting on the 'Home' Tab.");
                waitForUser(scanner);
                targetClass = "com.project.tests.stone.CollectionTest";
                break;
            case 4:
                System.out.println(PURPLE + "\n[PRE-CONDITION REQUIRED] 🚧" + RESET);
                System.out.println("1. The app MUST be resting on the 'Home' Tab.");
                System.out.println("2. Ensure the internet is ON.");
                waitForUser(scanner);
                targetClass = "com.project.tests.stone.NavigationTest";
                break;
            default:
                System.out.println(RED + "Invalid choice." + RESET);
                return;
        }

        executeTestNGClass(targetClass);
    }

    private static void waitForUser(Scanner scanner) {
        System.out.println(YELLOW + "\nPress [ENTER] when the device is set up and ready to begin..." + RESET);
        scanner.nextLine();
        System.out.println(GREEN + "Device confirmed ready. Booting Appium..." + RESET);
    }

    private static void executeTestNGClass(String className) {
        System.out.println(CYAN + "\n[SYSTEM] Initializing Execution Engine for: " + className + RESET);

        // 🔥 THE FIX: Explicitly start the Server and the HTML Reporter BEFORE TestNG begins
        com.project.utils.AppiumServerManager.startServer();
        String appTarget = System.getProperty("targetApp", "Stone");
        com.project.utils.ReportManager.setupReport(appTarget);

        // Build and run the TestNG suite dynamically
        TestNG testng = new TestNG();
        XmlSuite suite = new XmlSuite();
        suite.setName("HouseOfApps_DynamicSuite");
        XmlTest test = new XmlTest(suite);
        test.setName("Isolated_Feature_Test");
        List<XmlClass> classes = new ArrayList<>();
        classes.add(new XmlClass(className));
        test.setXmlClasses(classes);
        List<XmlSuite> suites = new ArrayList<>();
        suites.add(suite);
        testng.setXmlSuites(suites);
        testng.run();

        // 🔥 THE FIX: Flush the report and kill the Server AFTER TestNG finishes
        com.project.utils.ReportManager.flushReport();
        com.project.utils.AppiumServerManager.stopServer();
    }
}