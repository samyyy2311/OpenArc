package `in`.caffeinelabs.archie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ArchieBlue,
    secondary = ArchieTeal,
    background = ArchieDarkBg,
    surface = ArchieDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ArchieTextPrimary,
    onSurface = ArchieTextPrimary
)

@Composable
fun ArchieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
