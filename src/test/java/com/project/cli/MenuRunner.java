package com.project.cli;

import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MenuRunner {

    // ==========================================
    // ANSI ESCAPE CODES FOR TERMINAL COLORS
    // ==========================================
    public static final String RESET = "\033[0m";
    public static final String BOLD = "\033[1m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String PURPLE = "\033[35m";
    public static final String CYAN = "\033[36m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. Draw the Main Header
        System.out.println(CYAN + BOLD + "======================================================" + RESET);
        System.out.println(CYAN + BOLD + "      🚀 HOUSE OF APPS - AUTOMATION TERMINAL 🚀      " + RESET);
        System.out.println(CYAN + BOLD + "======================================================" + RESET);

        System.out.println(YELLOW + "\n[TARGET SELECTION]" + RESET);
        System.out.println("1. Stone Identifier");
        System.out.println("2. Coin Identifier " + PURPLE + "(Coming Soon)" + RESET);
        System.out.print(GREEN + "\nEnter choice (1 or 2): " + RESET);

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
            System.out.println(RED + "\nCoin Identifier suite is under construction. Exiting." + RESET);
        } else {
            System.out.println(RED + "\nInvalid selection. Exiting." + RESET);
        }
    }

    // ==========================================
    // STONE IDENTIFIER SUB-MENU
    // ==========================================
    private static void runStoneMenu(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n--- \uD83E\uDEA8 STONE IDENTIFIER SUITE ---" + RESET);
        System.out.println(YELLOW + "Select execution mode:" + RESET);
        System.out.println("1. " + BOLD + "Master Onboarding Flow" + RESET + " (Full 23-Step Suite - Wipes App Data)");
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

        // 4. Pre-Condition Handlers
        switch (testChoice) {
            case 1:
                System.out.println(PURPLE + "\n[PRE-CONDITION] App will be forcibly wiped and cold-started offline." + RESET);
                targetClass = "com.project.tests.OnboardingTest";
                break;
            case 2:
                System.out.println(PURPLE + "\n[PRE-CONDITION REQUIRED] 🛑" + RESET);
                System.out.println("1. Ensure the app is installed and past the FTUE pages.");
                System.out.println("2. The app MUST be resting on the 'Home' Tab.");
                System.out.println("3. Ensure the internet is ON.");
                waitForUser(scanner);
                targetClass = "com.project.tests.IdentifyFeatureTest";
                break;
            case 3:
                System.out.println(PURPLE + "\n[PRE-CONDITION REQUIRED] 🛑" + RESET);
                System.out.println("1. The app MUST have at least 1 stone saved in 'My Rocks'.");
                System.out.println("2. The app MUST be resting on the 'Home' Tab.");
                waitForUser(scanner);
                targetClass = "com.project.tests.CollectionTest";
                break;
            case 4:
                System.out.println(PURPLE + "\n[PRE-CONDITION REQUIRED] 🛑" + RESET);
                System.out.println("1. The app MUST be resting on the 'Home' Tab.");
                System.out.println("2. Ensure the internet is ON.");
                waitForUser(scanner);
                targetClass = "com.project.tests.NavigationTest";
                break;
            default:
                System.out.println(RED + "Invalid choice." + RESET);
                return;
        }

        // 5. Fire TestNG Dynamically
        executeTestNGClass(targetClass);
    }

    private static void waitForUser(Scanner scanner) {
        System.out.println(YELLOW + "\nPress [ENTER] when the device is set up and ready to begin..." + RESET);
        scanner.nextLine();
        System.out.println(GREEN + "Device confirmed ready. Booting Appium..." + RESET);
    }

    private static void executeTestNGClass(String className) {
        System.out.println(CYAN + "\n[SYSTEM] Initializing TestNG Engine for: " + className + RESET);
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
    }
}