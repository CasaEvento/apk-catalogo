package com.example

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CompanyConfig
import com.example.ui.canvas.CatalogCanvasRenderer
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun catalog_canvas_screenshot() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val defaultBranding = CompanyConfig(
            name = "Test Brand S.A.",
            primaryColorHex = "#2B2D42",
            secondaryColorHex = "#8D99AE",
            ctaColorHex = "#EF233C",
            textColorHex = "#EDF2F4"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    CatalogCanvasRenderer.drawCatalogOnCanvas(
                        canvas = nativeCanvas,
                        width = size.width.toInt(),
                        height = size.height.toInt(),
                        productBitmap = null,
                        logoBitmap = null,
                        companyConfig = defaultBranding,
                        productName = "Adorno Topper Mariposas",
                        productCode = "TOP-MP-882",
                        dimHeight = "18",
                        dimWidth = "12",
                        dimDepth = "5",
                        material = "Cartulina Craft & Opalina",
                        finish = "Metalizado",
                        presentation = "Set de 6 unidades"
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
