package com.project.tests.stone;

import com.project.base.BaseTest;
import static com.project.utils.ReportManager.*;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.Duration;

public class NavigationTest extends BaseTest {

    @Test
    public void isolatedNavigationTest() throws Exception {
        SoftAssert softAssert = new SoftAssert();
        logStep("\n========== STARTING ISOLATED SEARCH & CAROUSEL TEST ==========");

        driver.activateApp(appPackage);
        collapseHalfScreenAd();
        closeInterstitialAds();

        logStep("[ACTION] Scrolling to 'Discover More Stones'...");
        boolean foundDiscoverBtn = false;
        for (int i = 0; i < 5; i++) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
            var discoverBtn = driver.findElements(AppiumBy.xpath("//*[@text='Discover More Stones' or contains(@text, 'Discover More')]"));
            if (!discoverBtn.isEmpty() && discoverBtn.getFirst().isDisplayed()) {
                discoverBtn.getFirst().click();
                foundDiscoverBtn = true;
                break;
            }
            scrollVertical(true); // Scroll View Down
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (!foundDiscoverBtn) {
            reportBug("Failed to find 'Discover More Stones' button on Home tab.", "Missing_Discover_Button");
        }

        closeInterstitialAds();

        logStep("[TEST] Verifying Discover Stones UI and Negative Search state...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Discover Stones']")));

        org.openqa.selenium.WebElement searchBox = driver.findElement(AppiumBy.className("android.widget.EditText"));
        searchBox.click();
        searchBox.sendKeys("invalidstone123");

        try { driver.hideKeyboard(); } catch (Exception ignored) {}

        if (driver.findElements(AppiumBy.xpath("//*[@text='No stones found']")).isEmpty()) {
            reportBug("Error message 'No stones found' not displayed for invalid search.", "Missing_Search_Error");
        }

        logStep("[ACTION] Clearing search and routing back to Home...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Clear search']")).click();
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
        Thread.sleep(1500);

        logStep("\n========== STARTING HORIZONTAL CAROUSEL TEST ==========");
        collapseHalfScreenAd();

        logStep("[ACTION] Locating 'Popular Rocks' or 'Zodiac Gemstones' category...");
        org.openqa.selenium.WebElement categoryHeader = null;
        for (int i = 0; i < 5; i++) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
            var headers = driver.findElements(AppiumBy.xpath("//*[@text='Popular Rocks' or @text='Zodiac Gemstones']"));
            if (!headers.isEmpty() && headers.getFirst().isDisplayed()) {
                categoryHeader = headers.getFirst();
                break;
            }
            scrollVertical(true);
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (categoryHeader != null) {
            logStep("[ACTION] Found category. Performing horizontal W3C swipe...");
            int carouselY = categoryHeader.getLocation().getY() + 250;
            scrollHorizontal(carouselY, true);

            logStep("[ACTION] Tapping an element inside the horizontally scrolled list...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int tapX = (int) (size.getWidth() * 0.7);
            performTap(tapX, carouselY);

            closeInterstitialAds();

            logStep("[TEST] Verifying Description page loaded from horizontal list...");
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Description' or contains(@text, 'Description')]")));
                logStep("✅ Successfully loaded description from carousel.");
                driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
            } catch (Exception e) {
                reportBug("Failed to load stone description after clicking element in horizontal carousel.", "Broken_Carousel_Link");
            }
        } else {
            reportBug("Failed to locate 'Popular Rocks' or 'Zodiac Gemstones' on Home Tab.", "Missing_Horizontal_Categories");
        }

        System.out.println("\n===============================================");
        logStep("✅ ISOLATED NAVIGATION TEST COMPLETE.");
        logStep("Total Bugs Logged: " + totalBugCount);
        System.out.println("===============================================\n");

        softAssert.assertAll();
        Assert.assertEquals(totalBugCount, 0, "Test completed with bugs.");
    }
}