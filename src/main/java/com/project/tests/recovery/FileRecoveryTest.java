package com.project.tests;

import com.project.base.BaseTest;
import static com.project.utils.ReportManager.*;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;

public class FileRecoveryTest extends BaseTest {

    private final String APP_PACKAGE = "com.filerecovery.photovideo.restore";
    private final String SETTINGS_PACKAGE = "com.android.settings";
    private final String MIUI_SECURITY_PACKAGE = "com.miui.securitycenter";

    // ----------------------------------------------------------------------
    // TEST 1: Offline Boot & Recovery
    // ----------------------------------------------------------------------
    @Test(priority = 1)
    public void test01_OfflineBootAndAdCheck() {
        testLog = extent.createTest("Test 1: Offline Boot & Splash Ad Validation");

        // Force Offline
        setNetworkState(false);
        driver.activateApp(APP_PACKAGE);
        testLog.info("Disabled network and launched File Recovery app.");

        // Check for No Internet Error
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='No Internet Connection' or @text='Retry']")));
            testLog.pass("App successfully caught the offline state.");
        } catch (TimeoutException e) {
            reportBug("App failed to show No Internet Connection error on cold boot.", "Missing_Offline_Error");
        }

        // Restore Internet and check Ad
        setNetworkState(true);
        testLog.info("Internet restored. Waiting 8 seconds for network stabilization and Ad fill...");

        // ADDED FIX: Explicitly wait for network and AdMob to catch up
        try { Thread.sleep(8000); } catch (InterruptedException ignored) {}

        boolean adLoaded = handleSmartAdWall();
        if (!adLoaded) {
            reportBug("Splash screen loaded but no Ad wall was displayed.", "Missed_Ad_On_Launch");
        } else {
            testLog.pass("Ad wall successfully loaded and closed after network recovery.");
        }
    }

    // ----------------------------------------------------------------------
    // TEST 2: Language UI Trap
    // ----------------------------------------------------------------------
    @Test(priority = 2)
    public void test02_LanguageSelectionAndBackTrap() {
        testLog = extent.createTest("Test 2: Language UI & Broken Back Button Check");

        testLog.info("Scrolling language list...");
        scrollVertical(true);  // Scroll down using your boolean method
        scrollVertical(false); // Scroll up

        driver.findElement(AppiumBy.xpath("//*[@text='English']")).click();
        testLog.info("Selected English.");

        // Check Back Button Bug
        WebElement inAppBackBtn = driver.findElement(AppiumBy.xpath("//*[@content-desc='Navigate up' or @content-desc='Back']"));
        inAppBackBtn.click();

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='English']")));
            reportBug("Tapping '<-' on Language UI does nothing. User is trapped.", "Broken_InApp_BackButton");
        } catch (TimeoutException e) {
            testLog.pass("Back button correctly dismissed the UI.");
            driver.activateApp(APP_PACKAGE);
        }

        // Proceed via Top-Right Tick
        driver.findElement(AppiumBy.xpath("//android.widget.ImageView[@bounds='[995,1600][1036,1641]']")).click();
        testLog.info("Tapped Tick/Done button to proceed to FTUE.");
    }

    // ----------------------------------------------------------------------
    // TEST 3: FTUE Flow & Half-Screen Ads
    // ----------------------------------------------------------------------
    @Test(priority = 3)
    public void test03_FTUEOnboardingAds() {
        testLog = extent.createTest("Test 3: FTUE Flow & Native Bottom Ads");

        for (int page = 1; page <= 3; page++) {
            testLog.info("Validating FTUE Page " + page);

            boolean isBottomAdPresent = !driver.findElements(AppiumBy.xpath("//android.widget.FrameLayout[contains(@bounds, '[0,1000]')]//android.view.View")).isEmpty();
            if (!isBottomAdPresent) {
                reportBug("Bottom half screen ad missing on FTUE Page " + page, "Missing_FTUE_Ad");
            }

            if (page < 3) {
                driver.findElement(AppiumBy.xpath("//*[@text='Next']")).click();
                handleSmartAdWall(); // Catch interstitial ad between pages
            } else {
                driver.findElement(AppiumBy.xpath("//*[@text='Finish' or @text='Done']")).click();
            }
        }
        testLog.pass("FTUE flow completed.");
    }

    // ----------------------------------------------------------------------
    // TEST 4: Subscription Load & OS Permission Trap
    // ----------------------------------------------------------------------
    @Test(priority = 4)
    public void test04_SubscriptionAndPermissionTrap() {
        testLog = extent.createTest("Test 4: Subscription UI & Storage OS Trap");

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Premium' or @content-desc='Premium']")));
            testLog.pass("Subscription UI loaded successfully after FTUE.");
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Close' or @text='X']")).click();
        } catch (TimeoutException e) {
            reportBug("Subscription UI failed to load after FTUE Finish.", "Missing_Paywall");
        }

        testLog.info("Engaging Storage Permission Trap logic.");
        driver.findElement(AppiumBy.xpath("//*[@text='Grant Permission']")).click();
        driver.findElement(AppiumBy.id("com.android.permissioncontroller:id/permission_deny_button")).click();
        driver.findElement(AppiumBy.xpath("//*[@text='Grant Permission']")).click();
        driver.findElement(AppiumBy.id("com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button")).click();
        driver.findElement(AppiumBy.xpath("//*[@text='Grant Permission']")).click();

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='App info']")));
            testLog.pass("App successfully routed user to OS App Settings after permanent denial.");
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
        } catch (TimeoutException e) {
            reportBug("App failed to open Android Settings after 'Don't Ask Again' trap.", "Broken_Settings_Routing");
        }

        driver.findElement(AppiumBy.xpath("//*[@text='Not Now' or @content-desc='Close']")).click();
    }

    // ----------------------------------------------------------------------
    // TEST 5: App Settings, Capitalization Bug & Restore Toast
    // ----------------------------------------------------------------------
    @Test(priority = 5)
    public void test05_SubscriptionBugsAndToast() {
        testLog = extent.createTest("Test 5: OS Permissions, Sub Capitalization & Toast");

        // ADDED FIX: Deep Link directly to the File Recovery App Info page
        testLog.info("Deep routing to OS App Info settings...");
        driver.executeScript("mobile: shell", java.util.Map.of(
                "command", "am",
                "args", java.util.Arrays.asList("start", "-a", "android.settings.APPLICATION_DETAILS_SETTINGS", "-d", "package:" + APP_PACKAGE)
        ));

        // Wait for page to load, then click Permissions
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Permissions' or @text='App permissions']"))).click();

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Files and media' or @text='Storage']"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[contains(@text, 'Allow')]"))).click();
            testLog.pass("Granted storage permission natively.");
        } catch (TimeoutException e) {
            testLog.info("Storage already granted or UI differs.");
        }

        // Return to app
        driver.activateApp(APP_PACKAGE);
        handleSmartAdWall();

        driver.findElement(AppiumBy.xpath("//*[@content-desc='Premium']")).click();

        String secondOptionText = driver.findElement(AppiumBy.xpath("(//android.widget.TextView[contains(@text, 'weekly') or contains(@text, 'Weekly')])[2]")).getText();
        if (Character.isLowerCase(secondOptionText.charAt(0))) {
            reportBug("2nd Subscription option starts with lower case: '" + secondOptionText + "'", "Typo_Subscription_UI");
        }

        scrollVertical(true); // Scroll down
        driver.findElement(AppiumBy.xpath("//*[@text='Restore purchases']")).click();

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath("//android.widget.Toast[@text='Restore failed.']")));
            testLog.pass("Restore failed toast successfully captured.");
        } catch (Exception e) {
            reportBug("No 'Restore failed.' toast appeared after tapping restore.", "Missing_Toast_Error");
        }

        driver.findElement(AppiumBy.xpath("//*[@text='Continue']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@package='com.android.vending']")));
        driver.pressKey(new KeyEvent(AndroidKey.BACK));

        scrollVertical(false); // Scroll up
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Close' or @text='X']")).click();
    }

    // ----------------------------------------------------------------------
    // TEST 6: Device Storage vs. App Storage Verification
    // ----------------------------------------------------------------------
    @Test(priority = 6)
    public void test06_StorageDataParityCheck() {
        testLog = extent.createTest("Test 6: Deep Storage Math Validation");

        handleSmartAdWall();

        driver.activateApp(MIUI_SECURITY_PACKAGE);
        WebElement osStorageStr = wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id("com.miui.securitycenter:id/summary2")));
        String[] parts = osStorageStr.getText().split("\\|");
        String osOccupied = parts[0].replaceAll("[^0-9.]", "").trim();
        String osTotal = parts[1].replaceAll("[^0-9.]", "").trim();
        testLog.info("Device Native Data -> Occupied: " + osOccupied + "GB, Total: " + osTotal + "GB");

        driver.activateApp(APP_PACKAGE);
        handleSmartAdWall();
        driver.findElement(AppiumBy.xpath("//*[@text='Storage Summary']")).click();
        handleSmartAdWall();

        String appStorageText = driver.findElement(AppiumBy.xpath("//android.widget.TextView[contains(@text, 'GB')]")).getText();
        String appOccupied = appStorageText.split("/")[0].replaceAll("[^0-9.]", "").trim();
        String appTotal = appStorageText.split("/")[1].replaceAll("[^0-9.]", "").trim();

        if (!osOccupied.equals(appOccupied) || !osTotal.equals(appTotal)) {
            String bugDesc = String.format("Data Mismatch! Expected OS -> Occupied: %sGB/Total: %sGB. App observed -> Occupied: %sGB/Total: %sGB", osOccupied, osTotal, appOccupied, appTotal);
            reportBug(bugDesc, "Storage_Calculation_Mismatch");
        } else {
            testLog.pass("App Storage Summary perfectly matches OS Device Storage metrics.");
        }

        scrollVertical(true);
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Navigate up']")).click();
        handleSmartAdWall();
    }

    // ----------------------------------------------------------------------
    // TEST 7: Cross-App Routing & "Leaving App" Prompts
    // ----------------------------------------------------------------------
    @Test(priority = 7)
    public void test07_CrossAppRoutingAndPermissions() {
        testLog = extent.createTest("Test 7: Third-Party Routing & Interstitial Integrity");

        driver.findElement(AppiumBy.xpath("//*[@content-desc='Settings' or contains(@resource-id, 'settings')]")).click();

        driver.findElement(AppiumBy.xpath("//*[@text='Privacy Policy']")).click();
        driver.findElement(AppiumBy.xpath("//*[@text='Chrome' or @text='Browser']")).click();
        checkLeavingAppPrompt("Privacy Policy");

        driver.activateApp(APP_PACKAGE);
        handleSmartAdWall();

        driver.findElement(AppiumBy.xpath("//*[@text='Rate Us']")).click();
        driver.findElement(AppiumBy.xpath("//*[@text='Google Play' or @text='Play Store']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='File Recovery - Photo & Video']")));
        testLog.pass("Google Play routed to correct App Page.");

        driver.activateApp(APP_PACKAGE);
        handleSmartAdWall();

        driver.findElement(AppiumBy.xpath("//*[@text='About Us']")).click();
        driver.findElement(AppiumBy.xpath("//*[@text='Chrome']")).click();
        checkLeavingAppPrompt("About Us");

        driver.activateApp(APP_PACKAGE);
        handleSmartAdWall();

        driver.findElement(AppiumBy.xpath("//*[@text='Contact Us']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@package='com.google.android.gm']")));
        testLog.pass("Successfully launched Gmail Intent.");

        driver.pressKey(new KeyEvent(AndroidKey.BACK));
        driver.pressKey(new KeyEvent(AndroidKey.BACK));

        handleSmartAdWall();
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Navigate up']")).click();
        handleSmartAdWall();
    }

    // ======================================================================
    // HELPER METHODS
    // ======================================================================

    private void checkLeavingAppPrompt(String routingContext) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Leaving App' or contains(@text, 'continue to external')]")));
            driver.findElement(AppiumBy.xpath("//*[@text='Continue' or @text='Yes']")).click();
        } catch (TimeoutException e) {
            reportBug("App failed to warn user before routing to external browser for: " + routingContext, "Missing_Security_Prompt");
        }
    }

    private boolean handleSmartAdWall() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            boolean adExists = !driver.findElements(AppiumBy.xpath("//android.view.View[contains(@content-desc, 'Ad') or contains(@resource-id, 'ad_')]")).isEmpty();

            if (!adExists) return false;

            try {
                WebElement closeBtn = shortWait.until(ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.xpath("//*[@content-desc='Close' or @text='Skip' or @text='X' or @text='Continue to App']")
                ));
                closeBtn.click();
                return true;
            } catch (TimeoutException closeBtnMissing) {
                reportBug("Ad fails to display any close button. Forcing closure via hardware back button.", "Uncloseable_Ad_Trap");
                driver.pressKey(new KeyEvent(AndroidKey.BACK));
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}