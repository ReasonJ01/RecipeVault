package com.example.recipevault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.recipevault.R

val EBGaramond = FontFamily(
    Font(R.font.eb_garamond, weight = FontWeight.Normal)
)


val customTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = EBGaramond,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = EBGaramond,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = EBGaramond,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp
    ),
)


// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

val Typography.headlineLargeGaramond: TextStyle
    get() = TextStyle(
        fontFamily = EBGaramond,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp
    )

val Typography.headlineMediumGaramond: TextStyle
    get() = TextStyle(
        fontFamily = EBGaramond,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    )

val Typography.headlineSmallGaramond: TextStyle
    get() = TextStyle(
        fontFamily = EBGaramond,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    )