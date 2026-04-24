package com.project.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class SplashPage {
    private final AppiumDriver driver;
    private final WebDriverWait wait;

    private final By loadingSpinner = AppiumBy.className("android.widget.ProgressBar");

    // =========================================
    // The Ultimate Ad Arsenal
    // =========================================
    private final By continueToAppBtn = AppiumBy.xpath("//*[contains(@text, 'Continue to app') or contains(@content-desc, 'Continue to app')]");
    private final By genericCloseTextBtn = AppiumBy.xpath("//*[@text='Close' or @text='CLOSE']");
    private final By dismissButtonId = AppiumBy.xpath("//*[@resource-id='dismiss-button']");
    private final By adCloseBtnX = AppiumBy.accessibilityId("Close");
    private final By adCloseBtn2 = AppiumBy.xpath("//*[@text='Skip' or @text='SKIP']");
    private final By adCloseBtn3 = AppiumBy.accessibilityId("Close ad");

    // Put them all in a list so we can rapidly loop through them
    private final List<By> allAdLocators = Arrays.asList(
            continueToAppBtn,
            genericCloseTextBtn,
            dismissButtonId,
            adCloseBtnX,
            adCloseBtn2,
            adCloseBtn3
    );

    public SplashPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void waitForSplashToDisappear() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(loadingSpinner));
        } catch (Exception e) {
            System.out.println("[TEST] Spinner already gone or not found.");
        }
    }

    public void handleAdWallIfPresent() {
        System.out.println("[TEST] Waiting 5 seconds for ad UI to render...");
        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

        // TEMPORARILY DISABLE IMPLICIT WAIT (The Secret Sauce)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        boolean adHandled = false;

        System.out.println("[TEST] Scanning for ad close buttons...");
        for (By locator : allAdLocators) {
            try {
                if (!driver.findElements(locator).isEmpty()) {
                    System.out.println("[TEST] Found ad close button: " + locator.toString());
                    driver.findElement(locator).click();
                    adHandled = true;
                    break; // Exit the loop as soon as we click one!
                }
            } catch (Exception e) {
                // Stale element or generic error, ignore and keep scanning
            }
        }

        if (!adHandled) {
            System.out.println("[TEST] No known ad close buttons found. Attempting to proceed...");
        }

        // RESTORE IMPLICIT WAIT for the rest of the test
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
}