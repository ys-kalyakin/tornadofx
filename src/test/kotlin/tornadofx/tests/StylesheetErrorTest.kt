package tornadofx.tests

import javafx.stage.Stage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testfx.api.FxAssert.verifyThat
import org.testfx.api.FxToolkit
import org.testfx.matcher.control.LabeledMatchers.hasText
import tornadofx.*
import tornadofx.testapps.Styles
import tornadofx.testapps.StylesheetErrorView

//@Ignore
class StylesheetErrorTest {

	companion object {

		val app = App(StylesheetErrorView::class, Styles::class)
		@JvmStatic
		@BeforeAll
		fun before() {
			val primaryStage: Stage = FxToolkit.registerPrimaryStage()
			/**
			 * This will cause a stylesheet error, but should not crash the application.
			 */
			FX.registerApplication(FxToolkit.setupApplication {
				app
			}, primaryStage)
		}

		@JvmStatic
		@AfterAll
		fun after() {
			FxToolkit.cleanupApplication(app)
			FX.stylesheets.clear()
		}
	}

	@Test
	fun shouldStartApplicationWithWrongStylesheetWithoutCrashing() {
		verifyThat(".my-button", hasText("Click here"))
	}

}
