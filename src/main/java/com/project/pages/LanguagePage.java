package com.project.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class LanguagePage {
    private final AppiumDriver driver;
    private final WebDriverWait wait;

    // OLD: private final By languageHeader = AppiumBy.xpath("//*[@text='Language Settings' or @text='Language']");

    // NEW: Forces Appium to ONLY scan TextViews, ignoring 90% of the screen's background elements
    private final By languageHeader = AppiumBy.xpath("//android.widget.TextView[@text='Language Settings' or @text='Language']");

    private final By inAppBackButton = AppiumBy.accessibilityId("Back");
    private final By doneButton = AppiumBy.accessibilityId("Done");

    public LanguagePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public boolean isLanguageScreenLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(languageHeader)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickInAppBackButton() {
        driver.findElement(inAppBackButton).click();
    }

    /** Dynamically scrolls to the text and clicks it */
    public void selectLanguage(String languageName, boolean scrollDownTheList) {
        By exactLanguageLocator = AppiumBy.xpath("//*[@text='" + languageName + "' or contains(@text, '" + languageName + "')]");
        boolean isFound = false;

        for (int i = 0; i < 7; i++) {
            try {
                // 1. Check if the language is on screen
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
                driver.findElement(exactLanguageLocator).click();
                System.out.println("[ACTION] Selected language: " + languageName);
                isFound = true;
                break;

            } catch (Exception e) {
                // 2. Not found, initiate W3C directional swipe
                try {
                    org.openqa.selenium.Dimension size = driver.manage().window().getSize();
                    int startX = size.getWidth() / 2;
                    int startY, endY;

                    // DYNAMIC COORDINATE MATH
                    if (scrollDownTheList) {
                        // Finger drags from Middle to Top -> View scrolls DOWN toward Spanish
                        startY = (int) (size.getHeight() * 0.5);
                        endY = (int) (size.getHeight() * 0.2);
                    } else {
                        // Finger drags from Top to Bottom -> View scrolls UP toward English
                        // Start at 30% to avoid the top header, drag to 70% to avoid the bottom ad
                        startY = (int) (size.getHeight() * 0.3);
                        endY = (int) (size.getHeight() * 0.7);
                    }

                    org.openqa.selenium.interactions.PointerInput finger = new org.openqa.selenium.interactions.PointerInput(org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence swipe = new org.openqa.selenium.interactions.Sequence(finger, 1);

                    swipe.addAction(finger.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
                    swipe.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
                    swipe.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));

                    driver.perform(java.util.Arrays.asList(swipe));
                    Thread.sleep(1000); // Wait for bounce physics to stop
                } catch (Exception swipeFail) {
                    System.out.println("[WARNING] W3C Swipe failed.");
                }
            } finally {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); // Restore global wait
            }
        }

        if (!isFound) {
            System.out.println("❌ [ERROR] Could not find language: " + languageName + " even after safe scrolling.");
        } else {
            try { Thread.sleep(1000); } catch (Exception ignored) {}
        }
    }

    public void clickDone() {
        wait.until(ExpectedConditions.elementToBeClickable(doneButton)).click();
    }
}