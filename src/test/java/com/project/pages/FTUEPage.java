package com.project.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class FTUEPage {
    private final AppiumDriver driver;
    private final WebDriverWait wait;

    private final By nextButton = AppiumBy.xpath("//android.widget.TextView[@text='Next']");
    private final By finishButton = AppiumBy.xpath("//android.widget.TextView[@text='Finish']");

    public FTUEPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void clickNext() {
        wait.until(ExpectedConditions.elementToBeClickable(nextButton)).click();
    }

    public void clickFinish() {
        wait.until(ExpectedConditions.elementToBeClickable(finishButton)).click();
    }
}