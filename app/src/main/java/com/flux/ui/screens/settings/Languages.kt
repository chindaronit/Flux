package com.flux.ui.screens.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.flux.R
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.CircleWrapper
import com.flux.ui.components.MaterialText
import com.flux.ui.components.RenderRadio
import com.flux.ui.components.shapeManager
import com.flux.ui.state.Settings
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Languages(navController: NavController, settings: Settings) {
    val context = LocalContext.current
    val supportedLanguages = getSupportedLanguages(context)
    val currentLocale = AppCompatDelegate.getApplicationLocales()

    BasicScaffold(
        title = stringResource(R.string.Languages),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                LanguageItem(
                    title = stringResource(R.string.System_language),
                    isSelected = currentLocale.isEmpty,
                    shape = shapeManager(radius = settings.data.cornerRadius, isFirst = true),
                    icon = R.drawable.translate,
                    description = stringResource(R.string.System_language_desc),
                    onRadioClicked = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
                )
            }

            itemsIndexed(supportedLanguages.toList()) { index, (displayName, languageCode) ->
                val languageInfo =
                    getLanguageInfo(languageCode)

                LanguageItem(
                    title = displayName,
                    isSelected = !currentLocale.isEmpty && currentLocale[0]?.language == languageCode,
                    shape = shapeManager(
                        radius = settings.data.cornerRadius,
                        isLast = index == supportedLanguages.size - 1
                    ),
                    icon = languageInfo.iconRes,
                    description = languageInfo.description,
                    onRadioClicked = {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(
                                languageCode
                            )
                        )
                    }
                )
            }
        }
    }
}

private fun getSupportedLanguages(context: Context): Map<String, String> {
    val localeList = mutableListOf<CharSequence>()
    try {
        val xpp: XmlPullParser = context.resources.getXml(R.xml.locales_config)
        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
            if (xpp.eventType == XmlPullParser.START_TAG && xpp.name == "locale") {
                localeList.add(xpp.getAttributeValue(0))
            }
            xpp.next()
        }
    } catch (e: XmlPullParserException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return localeList.map { it.toString() }
        .associateBy { getLanguageInfo(it).displayName }
}

data class LanguageInfo(
    val displayName: String,
    val description: String,
    val iconRes: Int
)

@Suppress("DEPRECATION")
private fun getLanguageInfo(languageCode: String): LanguageInfo {
    val locale = Locale.forLanguageTag(languageCode)

    return when (languageCode) {
        "en" -> LanguageInfo(
            displayName = "English",
            description = "Change your language to English",
            iconRes = R.drawable.english
        )

        "hi" -> LanguageInfo(
            displayName = "हिंदी",
            description = "अपनी भाषा को हिंदी में बदलें",
            iconRes = R.drawable.hindi
        )

        "fr" -> LanguageInfo(
            displayName = "Français",
            description = "Changer la langue en français",
            iconRes = R.drawable.french
        )

        "es" -> LanguageInfo(
            displayName = "Español",
            description = "Cambiar el idioma a español",
            iconRes = R.drawable.spain
        )

        "ru" -> LanguageInfo(
            displayName = "Русский",
            description = "Сменить язык на русский",
            iconRes = R.drawable.russia
        )

        "de" -> LanguageInfo(
            displayName = "Deutsch",
            description = "Sprache auf Deutsch ändern",
            iconRes = R.drawable.german
        )

        "pt" -> LanguageInfo(
            displayName = "Português (Brasil)",
            description = "Mudar o idioma para português (Brasil)",
            iconRes = R.drawable.portuguese
        )

        "nl" -> LanguageInfo(
            displayName = "Nederlands",
            description = "Zet de taal naar Nederlands",
            iconRes = R.drawable.dutch
        )

        else -> LanguageInfo(
            displayName = locale.displayName,
            description = "Change language to ${locale.displayName}",
            iconRes = R.drawable.translate
        )
    }
}

@Composable
fun LanguageItem(
    shape: RoundedCornerShape,
    title: String,
    description: String? = null,
    icon: Int,
    size: Dp = 12.dp,
    isSelected: Boolean,
    onRadioClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(bottom = 3.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
            .clickable { onRadioClicked() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = size)
                .fillMaxWidth()
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                CircleWrapper(
                    size = 12.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) { Icon(painter = painterResource(icon), null, modifier = Modifier.size(24.dp)) }
                Spacer(modifier = Modifier.width(8.dp))
                MaterialText(title = title, description = description)
            }
            RenderRadio(enabled = isSelected, onRadioEnabled = onRadioClicked)
        }
    }
}