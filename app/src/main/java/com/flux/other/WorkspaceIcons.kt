package com.flux.other

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AreaChart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.CurrencyPound
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.CurrencyYen
import androidx.compose.material.icons.filled.CurrencyYuan
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face2
import androidx.compose.material.icons.filled.Face3
import androidx.compose.material.icons.filled.Face4
import androidx.compose.material.icons.filled.Face5
import androidx.compose.material.icons.filled.Face6
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WaterfallChart
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Workspaces

data class WorkspaceIcons(
    val title: String,
    val icons: List<Int>
)

val FINANCE = WorkspaceIcons(
    title = "Finance - Analytics",
    icons = listOf(
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23
    )
)

val EMOTICON = WorkspaceIcons(
    title = "Emoji",
    icons = listOf(
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        32,
        33,
        34,
        35,
        36,
        37,
        38,
        39,
        40,
        41,
        42,
        43,
        44,
        45,
        46,
        47
    )
)

val OTHERS = WorkspaceIcons(
    title = "Others",
    icons = listOf(80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95)
)

val DEFAULT = WorkspaceIcons(
    title = "Default",
    icons = listOf(48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63)
)

val FOOD = WorkspaceIcons(
    title = "Food",
    icons = listOf(64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79)
)

val workspaceIconList = listOf(DEFAULT, EMOTICON, FOOD, FINANCE, OTHERS)

val icons = listOf(
    // Finance
    Icons.Default.Business,
    Icons.Default.AreaChart,
    Icons.Default.PieChart,
    Icons.Default.WaterfallChart,
    Icons.Default.Analytics,
    Icons.Default.BarChart,
    Icons.Default.CandlestickChart,
    Icons.Default.SsidChart,
    Icons.AutoMirrored.Filled.ShowChart,
    Icons.Default.AccountBalance,
    Icons.Default.Calculate,
    Icons.Default.CreditCard,
    Icons.Default.Wallet,
    Icons.Default.Savings,
    Icons.Default.Money,
    Icons.Default.CurrencyBitcoin,
    Icons.Default.Paid,
    Icons.Default.AttachMoney,
    Icons.Default.CurrencyRupee,
    Icons.Default.CurrencyYen,
    Icons.Default.CurrencyYuan,
    Icons.Default.CurrencyPound,
    Icons.Default.Euro,
    Icons.Default.CurrencyExchange,

    // EMOTICON
    Icons.Default.Favorite,
    Icons.Default.HeartBroken,
    Icons.Default.Mood,
    Icons.Default.MoodBad,
    Icons.Default.SentimentNeutral,
    Icons.Default.SentimentSatisfied,
    Icons.Default.SentimentDissatisfied,
    Icons.Default.SentimentSatisfiedAlt,
    Icons.Default.SentimentVerySatisfied,
    Icons.Default.SentimentVeryDissatisfied,
    Icons.Default.Sick,
    Icons.Default.Face,
    Icons.Default.Face2,
    Icons.Default.Face3,
    Icons.Default.Face4,
    Icons.Default.Face5,
    Icons.Default.Face6,
    Icons.Default.Man,
    Icons.Default.Woman,
    Icons.Default.ThumbUp,
    Icons.Default.ThumbDown,
    Icons.Default.WavingHand,
    Icons.Default.Handshake,
    Icons.Default.VolunteerActivism,

    // DEFAULT
    Icons.Default.Workspaces,
    Icons.AutoMirrored.Default.Note,
    Icons.AutoMirrored.Default.Notes,
    Icons.Default.CalendarToday,
    Icons.Default.CalendarMonth,
    Icons.Default.Event,
    Icons.Default.EventAvailable,
    Icons.Default.TaskAlt,
    Icons.Default.Work,
    Icons.Default.AutoStories,
    Icons.Default.Settings,
    Icons.Default.PrivacyTip,
    Icons.Default.Shield,
    Icons.Default.Notifications,
    Icons.Default.Language,
    Icons.Default.Info,

    //FOOD
    Icons.Default.Coffee,
    Icons.Default.Fastfood,
    Icons.Default.Cake,
    Icons.Default.Icecream,
    Icons.Default.Restaurant,
    Icons.Default.Cookie,
    Icons.Default.Egg,
    Icons.Default.Dining,
    Icons.Default.LocalDrink,
    Icons.Default.LocalPizza,
    Icons.Default.Nightlife,
    Icons.Default.RiceBowl,
    Icons.Default.RamenDining,
    Icons.Default.Liquor,
    Icons.Default.RoomService,
    Icons.Default.EggAlt,

    //OTHERS
    Icons.Default.Code,
    Icons.Default.Edit,
    Icons.Default.Fingerprint,
    Icons.Default.Check,
    Icons.Default.Close,
    Icons.Default.Checklist,
    Icons.Default.FitnessCenter,
    Icons.Default.Flight,
    Icons.Default.MusicNote,
    Icons.Default.MusicOff,
    Icons.Default.AccessTime,
    Icons.Default.PlayArrow,
    Icons.Default.Pause,
    Icons.Default.Group,
    Icons.Default.Groups,
    Icons.Default.HourglassTop,

    )
