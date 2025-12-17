// src/main/kotlin/Main.kt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lockedfog.airi.di.appModule
import com.lockedfog.airi.ui.App
import org.koin.core.context.startKoin
import java.awt.Dimension

const val MINIMUM_WIDTH = 400
const val MINIMUM_HEIGHT = 600

fun main() {
    startKoin {
        modules(appModule)
    }

    application {
        val windowState = rememberWindowState(width = 450.dp, height = 800.dp)

        Window(
            onCloseRequest = ::exitApplication,
            title = "AiRi - Project Fog Walker",
            state = windowState
        ) {
            window.minimumSize = Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT)

            App()
        }
    }
}
