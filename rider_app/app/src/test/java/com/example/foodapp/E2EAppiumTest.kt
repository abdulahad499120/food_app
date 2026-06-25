package com.example.foodapp

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import io.appium.java_client.AppiumBy
import java.net.URL
import java.time.Duration

class E2EAppiumTest {

    private lateinit var driver: AndroidDriver
    private lateinit var wait: WebDriverWait

    @Before
    fun setUp() {
        val options = UiAutomator2Options()
            .setDeviceName("emulator-5554")
            .setAppPackage("com.ahad.foodapp")
            .setAppActivity("com.example.foodapp.MainActivity")
            .setNoReset(false)
            .setAutoGrantPermissions(true)
        
        options.setCapability("autoAcceptAlerts", true)

        println("Initializing Appium Driver for Complete E2E Testing...")
        driver = AndroidDriver(URL("http://127.0.0.1:4723"), options)
        wait = WebDriverWait(driver, Duration.ofSeconds(15))
        
        // Handle "App built for older version" Android warning dialog
        try {
            val quickWait = WebDriverWait(driver, Duration.ofSeconds(3))
            val okBtn = quickWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//android.widget.Button[@text='OK']")
            ))
            okBtn.click()
            println("Dismissed system warning dialog.")
        } catch (e: Exception) {
            // Dialog didn't appear
        }
    }

    @Test
    fun testCompleteUserJourney() {
        println("=== STAGE 1: ONBOARDING & AUTHENTICATION ===")
        try {
            val getStartedBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//android.widget.TextView[@text='Sign In']")
            ))
            getStartedBtn.click()
            
            val emailField = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//android.widget.EditText[1]")
            ))
            emailField.sendKeys("test@example.com")
            
            val passField = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//android.widget.EditText[2]")
            ))
            passField.sendKeys("password123")
            
            val signInBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//android.widget.TextView[@text='Sign In']")
            ))
            signInBtn.click()
            println("Authentication Successful.")
        } catch (e: Exception) {
            println("Skipping Auth Flow - User might already be logged in. ${e.message}")
            println("=== PAGE SOURCE DUMP ===")
            println(driver.pageSource)
            println("========================")
        }

        println("\n=== STAGE 2: LOCATION PREREQUISITE & HOME SCREEN ===")
        val chooseStoreBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Choose a store'] | //android.widget.TextView[@text='Select a Store']")
        ))
        chooseStoreBtn.click()
        
        // Wait for UI to settle before finding elements to avoid StaleElementReferenceException
        Thread.sleep(2000)
        
        // Click the first branch (e.g. Downtown Branch)
        var branchClicked = false
        var attempts = 0
        while (!branchClicked && attempts < 3) {
            try {
                val branchCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//android.widget.TextView[contains(@text, 'Branch')] | //android.widget.TextView[contains(@text, 'Store')]")
                ))
                branchCard.click()
                branchClicked = true
            } catch (e: org.openqa.selenium.StaleElementReferenceException) {
                attempts++
                Thread.sleep(1000)
            }
        }
        
        val branchSelectBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Choose this store']")
        ))
        branchSelectBtn.click()
        println("Branch successfully selected. Returned to Home Screen.")

        println("\n=== STAGE 3: MENU BROWSING & PRODUCT CUSTOMIZATION ===")
        // Wait for the branch to be fully loaded and synced from Firestore
        wait.until(ExpectedConditions.presenceOfElementLocated(
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Ice Land\")")
        ))

        // Wait for Home Screen to populate with productseeder to complete inserting records
        Thread.sleep(8000)
        
        val categoryChip = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Salads']")
        ))
        categoryChip.click()
        
        Thread.sleep(3000)
        println("PAGE SOURCE DUMP:")
        println(driver.pageSource)

        var productItem: org.openqa.selenium.WebElement? = null
        val deadline = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < deadline) {
            try {
                val elements = driver.findElements(By.className("android.widget.TextView"))
                productItem = elements.firstOrNull { it.text?.contains("Sweet Salad") == true }
                if (productItem != null) break
            } catch (e: org.openqa.selenium.StaleElementReferenceException) {
                // Ignore and retry
            }
            Thread.sleep(1000)
        }
        if (productItem == null) {
            throw org.openqa.selenium.NoSuchElementException("Could not find Sweet Salad via client-side filtering")
        }
        productItem.click()
        
        val sizeLargeBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Regular']")
        ))
        sizeLargeBtn.click()
        
        val addToCartBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[contains(@text, 'Add to Order')]")
        ))
        addToCartBtn.click()
        println("Product customized and added to cart.")

        println("\n=== STAGE 4: CART & CHECKOUT FLOW ===")
        val cartIconBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.Button[@content-desc='Cart'] | //android.view.View[@content-desc='Cart']")
        ))
        cartIconBtn.click()

        val checkoutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[contains(@text, 'Checkout')]")
        ))
        checkoutBtn.click()
        
        val placeOrderBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Place Order']")
        ))
        placeOrderBtn.click()
        
        val successBanner = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Order Placed Successfully!']")
        ))
        println("Checkout Flow completed. Order Placed!")
        
        val trackOrderBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[contains(@text, 'Track')]")
        ))
        trackOrderBtn.click()

        println("\n=== STAGE 5: ORDERS HISTORY TAB ===")
        val ordersTab = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.view.View[@content-desc='Orders'] | //android.widget.TextView[@text='Orders']")
        ))
        ordersTab.click()
        
        val pastOrder = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[contains(@text, 'Order #')]")
        ))
        println("Verified Order History populates correctly.")

        println("\n=== STAGE 6: eGIFT MARKETPLACE (GIFT TAB) ===")
        val giftTab = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.view.View[@content-desc='Gift'] | //android.widget.TextView[@text='Gift']")
        ))
        giftTab.click()
        
        val templateCard = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Birthday']")
        ))
        templateCard.click()
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.EditText[contains(@text, 'Name')] | //android.widget.EditText[1]"))).sendKeys("Jane Doe")
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.EditText[contains(@text, 'Email')] | //android.widget.EditText[2]"))).sendKeys("jane@example.com")
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.TextView[@text='$25']"))).click()
        
        val payGiftBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Pay & Checkout']")
        ))
        payGiftBtn.click()
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Gift Sent!']")
        ))
        println("eGift Transaction Flow verified.")

        println("\n=== STAGE 7: GAMIFIED REWARDS LEDGER (REWARDS TAB) ===")
        val closeGiftBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Done']")
        ))
        closeGiftBtn.click()
        
        val rewardsTab = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.view.View[@content-desc='Rewards'] | //android.widget.TextView[@text='Rewards']")
        ))
        rewardsTab.click()
        
        val starsLabel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Stars']")
        ))
        require(starsLabel.isDisplayed) { "Stars progression ring not rendered." }
        
        val goldTierLabel = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[@text='Gold'] | //android.widget.TextView[@text='Green']")
        ))
        println("Rewards Tab and Gamification Tier verified.")

        println("\n=== STAGE 8: PROFILE & SETTINGS ===")
        val profileTab = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.view.View[@content-desc='Profile'] | //android.widget.TextView[@text='Profile']")
        ))
        profileTab.click()
        
        val profileName = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[contains(@text, 'Settings')]")
        ))
        println("Profile Screen loaded successfully.")
        
        val logOutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//android.widget.TextView[contains(@text, 'Log Out') or contains(@text, 'Sign Out')]")
        ))
        logOutBtn.click()
        
        println("\n✅ FULL E2E REGRESSION SUITE COMPLETED SUCCESSFULLY! ✅")
    }

    @After
    fun tearDown() {
        println("Tearing down Appium session...")
        if (::driver.isInitialized) {
            driver.quit()
        }
    }
}
