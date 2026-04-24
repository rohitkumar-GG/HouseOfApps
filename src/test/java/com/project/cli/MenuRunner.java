package com.project.cli;

import org.testng.TestNG;
import java.util.Scanner;

public class MenuRunner {
    public static void main(String[] args) {
        System.out.println("=====================================");
        System.out.println("   HOUSE OF APPS - AUTOMATION MENU   ");
        System.out.println("=====================================");
        System.out.println("1. Run Stone Identifier Tests");
        System.out.println("2. Run Coin Identifier Tests (Coming Soon)");
        System.out.print("Select an option (1 or 2): ");

        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine();

        if (choice.equals("1")) {
            System.setProperty("targetApp", "Stone");
        } else if (choice.equals("2")) {
            System.setProperty("targetApp", "Coin");
            System.out.println("Coin Identifier tests are not yet implemented. Exiting.");
            return;
        } else {
            System.out.println("Invalid selection. Exiting.");
            return;
        }

        System.out.println("\n[SYSTEM] Booting up TestNG Suite...");
        TestNG testng = new TestNG();
        // Dynamically pointing it to our test class
        testng.setTestClasses(new Class[] { com.project.tests.OnboardingTest.class });
        testng.run();
    }
}