package com.wakeroo.app.feature.about

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wakeroo.app.BuildConfig
import com.wakeroo.app.feature.settings.TopBar
import com.wakeroo.app.ui.components.SkyBackground
import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.theme.DawnLavender
import com.wakeroo.app.ui.theme.MorningGold
import com.wakeroo.app.ui.theme.Paper
import com.wakeroo.app.ui.theme.PaperMuted
import com.wakeroo.app.ui.theme.Sky
import com.wakeroo.app.ui.theme.Space

/** The two legal documents, bundled in assets/legal so they work offline. */
enum class LegalDoc(val id: String) {
    Privacy("privacy"), Licenses("licenses");

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id } ?: Privacy
    }
}

/** Where people can reach us; falls back to the store listing until the address is set. */
fun supportEmailOrListing(): String = BuildConfig.SUPPORT_EMAIL.ifBlank { "the email address on Wakeroo's Google Play listing" }

private sealed interface Block {
    data class Title(val text: String) : Block
    data class Heading(val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullet(val text: String) : Block
    data class Plain(val text: String) : Block
}

/** A tiny Markdown reader: headings, paragraphs, bullets, **bold** and bare links. */
private fun parse(markdown: String): List<Block> {
    val out = mutableListOf<Block>()
    val para = StringBuilder()
    fun flush() { if (para.isNotBlank()) out += Block.Paragraph(para.toString().trim()); para.clear() }
    for (line in markdown.lines()) {
        when {
            line.startsWith("# ") -> { flush(); out += Block.Title(line.removePrefix("# ")) }
            line.startsWith("## ") -> { flush(); out += Block.Heading(line.removePrefix("## ")) }
            line.startsWith("- ") -> { flush(); out += Block.Bullet(line.removePrefix("- ")) }
            line.isBlank() -> flush()
            else -> para.append(line).append(' ')
        }
    }
    flush()
    return out
}

private fun Context.asset(path: String) = assets.open(path).bufferedReader().use { it.readText() }

private fun load(context: Context, doc: LegalDoc): List<Block> = when (doc) {
    LegalDoc.Privacy -> parse(context.asset("legal/privacy_policy.md").replace("{SUPPORT_EMAIL}", supportEmailOrListing()))
    LegalDoc.Licenses -> parse(context.asset("legal/licenses.md")) + listOf(
        "Outfit: SIL Open Font License 1.1" to "legal/ofl_outfit.txt",
        "Inter: SIL Open Font License 1.1" to "legal/ofl_inter.txt",
        "Apache License 2.0" to "legal/apache_2.0.txt",
    ).flatMap { (title, path) ->
        // Long license texts go in paragraph-sized pieces so the list scrolls smoothly, and the
        // files' hard line breaks are reflowed so they read well on a phone.
        listOf(Block.Heading(title)) + context.asset(path).split(Regex("""\n\s*\n""")).filter { it.isNotBlank() }
            .map { Block.Plain(it.trim().replace(Regex("""\s+"""), " ")) }
    }
}

private val urlPattern = Regex("""https?://[^\s)]+[^\s).,]""")

@Composable
private fun rich(text: String): AnnotatedString {
    val linkStyle = TextLinkStyles(SpanStyle(color = DawnLavender))
    return buildAnnotatedString {
        // Bold runs first, then links inside each run.
        text.split("**").forEachIndexed { i, part ->
            val appendLinks: AnnotatedString.Builder.() -> Unit = {
                var last = 0
                for (m in urlPattern.findAll(part)) {
                    append(part.substring(last, m.range.first))
                    withLink(LinkAnnotation.Url(m.value, linkStyle)) { append(m.value) }
                    last = m.range.last + 1
                }
                append(part.substring(last))
            }
            if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Paper)) { appendLinks() } else appendLinks()
        }
    }
}

/** The privacy policy or the open-source licenses. English only, like most legal text. */
@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    val context = LocalContext.current
    val blocks = remember(doc) { load(context, doc) }
    // Legal text is English, so keep it left-to-right even in Arabic and Urdu.
    val body = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr)
    SkyBackground(Sky.Night, intensity = 0.6f) {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (doc == LegalDoc.Privacy) tr("Privacy policy") else tr("Open-source licenses"), onBack)
            LazyColumn(
                Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(start = Space.xl, end = Space.xl, top = Space.sm, bottom = Space.xxl),
            ) {
                items(blocks) { b ->
                    when (b) {
                        is Block.Title -> Unit // The top bar already shows it.
                        is Block.Heading -> Text(
                            b.text, style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr), color = MorningGold,
                            modifier = Modifier.padding(top = Space.xl, bottom = Space.sm),
                        )
                        is Block.Paragraph -> Text(rich(b.text), style = body, color = PaperMuted, modifier = Modifier.padding(bottom = Space.md))
                        is Block.Bullet -> Row(Modifier.padding(start = Space.sm, bottom = Space.sm)) {
                            Text("•", style = body, color = DawnLavender, modifier = Modifier.width(18.dp))
                            Text(rich(b.text), style = body, color = PaperMuted)
                        }
                        is Block.Plain -> Text(
                            b.text, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                            color = PaperMuted, modifier = Modifier.padding(bottom = Space.md),
                        )
                    }
                }
            }
        }
    }
}
