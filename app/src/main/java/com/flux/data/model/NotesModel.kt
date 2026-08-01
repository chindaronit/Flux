package com.flux.data.model

import androidx.compose.runtime.Composable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flux.R
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class NotesModel(
    @PrimaryKey
    val notesId: String = UUID.randomUUID().toString(),
    val workspaceId: String = "",
    val title: String = "",
    val description: String = "",
    val isPinned: Boolean = false,
    val socialLinks: List<SocialModel> = emptyList(),
    val labels: List<String> = emptyList(),
    val lastEdited: Long = System.currentTimeMillis()
)

@Serializable
data class SocialModel(
    val socialId: String = UUID.randomUUID().toString(),
    val notesId: String = "",
    val workspaceId: String = "",
    val category: Int = 0,
    val title: String = "",
    val link: String = "",
)

data class SocialCategory(
    val name: String,
    val icon: Int,
    val containerColor: Long,
    val contentColor: Long,
)

@Composable
fun getSocialCategory(): List<SocialCategory> {
    return listOf(
        SocialCategory(
            name = "Facebook",
            icon = R.drawable.ic_facebook,
            containerColor = 0xFF1877F2,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Instagram",
            icon = R.drawable.ic_instagram,
            containerColor = 0xFFFFFFFF,
            contentColor = 0xFF181717
        ),
        SocialCategory(
            name = "Twitter",
            icon = R.drawable.ic_twitter,
            containerColor = 0xFF000000,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Threads",
            icon = R.drawable.ic_threads,
            containerColor = 0xFF000000,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "WhatsApp",
            icon = R.drawable.ic_whatsapp,
            containerColor = 0xFF25D366,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Telegram",
            icon = R.drawable.ic_telegram,
            containerColor = 0xFF0088CC,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Discord",
            icon = R.drawable.ic_discord,
            containerColor = 0xFF5865F2,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Snapchat",
            icon = R.drawable.ic_snapchat,
            containerColor = 0xFFFFFC00,
            contentColor = 0xFF181717
        ),
        SocialCategory(
            name = "TikTok",
            icon = R.drawable.ic_tiktok,
            containerColor = 0xFF000000,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "YouTube",
            icon = R.drawable.ic_youtube,
            containerColor = 0xFFFF0000,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "LinkedIn",
            icon = R.drawable.ic_linkedin,
            containerColor = 0xFF0A66C2,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Pinterest",
            icon = R.drawable.ic_pinterest,
            containerColor = 0xFFBD081C,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Reddit",
            icon = R.drawable.ic_reddit,
            containerColor = 0xFFFF4500,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Twitch",
            icon = R.drawable.ic_twitch,
            containerColor = 0xFF9146FF,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "WeChat",
            icon = R.drawable.ic_wechat,
            containerColor = 0xFF07C160,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Skype",
            icon = R.drawable.ic_skype,
            containerColor = 0xFF00AFF0,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Slack",
            icon = R.drawable.ic_slack,
            containerColor = 0xFF4A154B,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "GitHub",
            icon = R.drawable.ic_github,
            containerColor = 0xFFFFFFFF,
            contentColor = 0xFF181717
        ),
        SocialCategory(
            name = "ChatGPT",
            icon = R.drawable.ic_chatgpt,
            containerColor = 0xFF10A37F,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Gemini",
            icon = R.drawable.ic_gemini,
            containerColor = 0x4285F4,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "AI",
            icon = R.drawable.ic_ai,
            containerColor = 0xFF7C3AED,
            contentColor = 0xFFFFFFFF
        ),
        SocialCategory(
            name = "Other",
            icon = R.drawable.ic_other,
            containerColor = 0xFFFFFFF,
            contentColor = 0xFF181717
        )
    )
}