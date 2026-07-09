package com.avd.ui.main.home.browser.social

import android.net.Uri
import com.avd.R

enum class SocialPlatform(
    val displayName: String,
    val iconRes: Int,
    val appSubtitleRes: Int,
    val step1TitleRes: Int,
    val step1SubtitleRes: Int,
    val step2TitleRes: Int,
    val step2SubtitleRes: Int,
    val step3TitleRes: Int,
    val step3SubtitleRes: Int,
  //  val step4TitleRes: Int ,
  //  val step4SubtitleRes: Int,
    val step1ImageRes: Int,
    val step2ImageRes: Int,
    val step3ImageRes: Int,
  //  val step4ImageRes: Int,
    val packageName: String,
    val webUrl: String
) {
    FACEBOOK(
        displayName = "Facebook",
        iconRes = R.drawable.icon_facebook,
        appSubtitleRes = R.string.social_app_subtitle_facebook,
        step1TitleRes = R.string.social_step1_title_facebook,
        step1SubtitleRes = R.string.social_step1_subtitle_facebook,
        step2TitleRes = R.string.social_step2_title,
        step2SubtitleRes = R.string.social_step2_subtitle_facebook,
        step3TitleRes = R.string.social_step3_title,
        step3SubtitleRes = R.string.social_step3_subtitle,
      //  step4TitleRes = R.string.social_step4_title,
      //  step4SubtitleRes = R.string.social_step4_subtitle,
        step1ImageRes = R.drawable.fb1,
        step2ImageRes = R.drawable.fb2,
        step3ImageRes = R.drawable.download_guidance3,
       // step4ImageRes = R.drawable.fb4,
        packageName = "com.facebook.katana",
        webUrl = "https://www.facebook.com/"
    ),
    INSTAGRAM(
        displayName = "Instagram",
        iconRes = R.drawable.ic_instagram,
        appSubtitleRes = R.string.social_app_subtitle_instagram,
        step1TitleRes = R.string.social_step1_title_instagram,
        step1SubtitleRes = R.string.social_step1_subtitle_instagram,
        step2TitleRes = R.string.social_step2_title,
        step2SubtitleRes = R.string.social_step2_subtitle_instagram,
        step3TitleRes = R.string.social_step3_title,
        step3SubtitleRes = R.string.social_step3_subtitle,
      //  step4TitleRes = R.string.social_step4_title,
      //  step4SubtitleRes = R.string.social_step4_subtitle,
        step1ImageRes = R.drawable.insta1,
        step2ImageRes = R.drawable.fb2,
        step3ImageRes = R.drawable.download_guidance3,
       // step4ImageRes = R.drawable.fb4,
                packageName = "com.instagram.android",
        webUrl = "https://www.instagram.com/"
    ),
    TIKTOK(
        displayName = "TikTok",
        iconRes = R.drawable.ic_tiktok,
        appSubtitleRes = R.string.social_app_subtitle_tiktok,
        step1TitleRes = R.string.social_step1_title_tiktok,
        step1SubtitleRes = R.string.social_step1_subtitle_tiktok,
        step2TitleRes = R.string.social_step2_title,
        step2SubtitleRes = R.string.social_step2_subtitle_tiktok,
        step3TitleRes = R.string.social_step3_title,
        step3SubtitleRes = R.string.social_step3_subtitle,
      //  step4TitleRes = R.string.social_step4_title,
      //  step4SubtitleRes = R.string.social_step4_subtitle,
        step1ImageRes = R.drawable.tiktok1,
        step2ImageRes = R.drawable.fb2,
        step3ImageRes = R.drawable.download_guidance3,
       // step4ImageRes = R.drawable.fb4,
        packageName = "com.zhiliaoapp.musically",
        webUrl = "https://www.tiktok.com/"
    ),
    TWITTER(
        displayName = "Twitter",
        iconRes = R.drawable.ic_twitter,
        appSubtitleRes = R.string.social_app_subtitle_twitter,
        step1TitleRes = R.string.social_step1_title_twitter,
        step1SubtitleRes = R.string.social_step1_subtitle_twitter,
        step2TitleRes = R.string.social_step2_title,
        step2SubtitleRes = R.string.social_step2_subtitle_twitter,
        step3TitleRes = R.string.social_step3_title,
        step3SubtitleRes = R.string.social_step3_subtitle,
      //  step4TitleRes = R.string.social_step4_title,
      //  step4SubtitleRes = R.string.social_step4_subtitle,
        step1ImageRes = R.drawable.download_guidance1,
        step2ImageRes = R.drawable.download_guidance2,
        step3ImageRes = R.drawable.download_guidance3,
       // step4ImageRes = R.drawable.fb4,
        packageName = "com.twitter.android",
        webUrl = "https://x.com/"
    ),
    DAILYMOTION(
        displayName = "Dailymotion",
        iconRes = R.drawable.ic_dailymotion,
        appSubtitleRes = R.string.social_app_subtitle_dailymotion,
        step1TitleRes = R.string.social_step1_title_dailymotion,
        step1SubtitleRes = R.string.social_step1_subtitle_dailymotion,
        step2TitleRes = R.string.social_step2_title,
        step2SubtitleRes = R.string.social_step2_subtitle_dailymotion,
        step3TitleRes = R.string.social_step3_title,
        step3SubtitleRes = R.string.social_step3_subtitle,
      //  step4TitleRes = R.string.social_step4_title,
      //  step4SubtitleRes = R.string.social_step4_subtitle,
        step1ImageRes = R.drawable.download_guidance1,
        step2ImageRes = R.drawable.download_guidance2,
        step3ImageRes = R.drawable.download_guidance3,
       // step4ImageRes = R.drawable.fb4,
        packageName = "com.dailymotion.dailymotion",
        webUrl = "https://www.dailymotion.com/"
    ),
    IMDB(
        displayName = "IMDb",
        iconRes = R.drawable.ic_imdb,
        appSubtitleRes = R.string.social_app_subtitle_imdb,
        step1TitleRes = R.string.social_step1_title_imdb,
        step1SubtitleRes = R.string.social_step1_subtitle_imdb,
        step2TitleRes = R.string.social_step2_title,
        step2SubtitleRes = R.string.social_step2_subtitle_imdb,
        step3TitleRes = R.string.social_step3_title,
        step3SubtitleRes = R.string.social_step3_subtitle,
      //  step4TitleRes = R.string.social_step4_title,
      //  step4SubtitleRes = R.string.social_step4_subtitle,
        step1ImageRes = R.drawable.download_guidance1,
        step2ImageRes = R.drawable.download_guidance2,
        step3ImageRes = R.drawable.download_guidance3,
       // step4ImageRes = R.drawable.fb4,
        packageName = "com.imdb.mobile",
        webUrl = "https://www.imdb.com/"
    ),
    VIMEO(
        displayName = "Vimeo",
        iconRes = R.drawable.ic_vimeo,
        appSubtitleRes = R.string.social_app_subtitle_vimeo,
        step1TitleRes = R.string.social_step1_title_vimeo,
        step1SubtitleRes = R.string.social_step1_subtitle_vimeo,
        step2TitleRes = R.string.social_step2_title,
        step2SubtitleRes = R.string.social_step2_subtitle_vimeo,
        step3TitleRes = R.string.social_step3_title,
        step3SubtitleRes = R.string.social_step3_subtitle,
      //  step4TitleRes = R.string.social_step4_title,
      //  step4SubtitleRes = R.string.social_step4_subtitle,
        step1ImageRes = R.drawable.download_guidance1,
        step2ImageRes = R.drawable.download_guidance2,
        step3ImageRes = R.drawable.download_guidance3,
       // step4ImageRes = R.drawable.fb4,
        packageName = "com.vimeo.android.videoapp",
        webUrl = "https://vimeo.com/"
    );

    companion object {
        private val facebookPattern =
            Regex("""(?:https?://)?(?:www\.|m\.)?(?:facebook\.com|fb\.com|fb\.watch)(?:/.*)?""", RegexOption.IGNORE_CASE)
        private val instagramPattern =
            Regex("""(?:https?://)?(?:www\.)?(?:instagram\.com|instagr\.am)(?:/.*)?""", RegexOption.IGNORE_CASE)
        private val threadsPattern =
            Regex("""(?:https?://)?(?:www\.)?(?:threads\.com|threads\.net)(?:/.*)?""", RegexOption.IGNORE_CASE)
        private val tiktokPattern =
            Regex("""(?:https?://)?(?:www\.|vm\.|vt\.)?tiktok\.com(?:/.*)?""", RegexOption.IGNORE_CASE)
        private val twitterPattern =
            Regex("""(?:https?://)?(?:www\.)?(?:twitter\.com|x\.com|t\.co)(?:/.*)?""", RegexOption.IGNORE_CASE)
        private val dailymotionPattern =
            Regex("""(?:https?://)?(?:www\.)?dailymotion\.com(?:/.*)?""", RegexOption.IGNORE_CASE)
        private val imdbPattern =
            Regex("""(?:https?://)?(?:www\.)?imdb\.com(?:/.*)?""", RegexOption.IGNORE_CASE)
        private val vimeoPattern =
            Regex("""(?:https?://)?(?:www\.)?vimeo\.com(?:/.*)?""", RegexOption.IGNORE_CASE)

        fun fromInput(input: String): SocialPlatform? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null
            val candidate = if (trimmed.startsWith("http", ignoreCase = true)) {
                trimmed
            } else {
                "https://$trimmed"
            }
            return when {
                facebookPattern.containsMatchIn(candidate) -> FACEBOOK
                instagramPattern.containsMatchIn(candidate) -> INSTAGRAM
                threadsPattern.containsMatchIn(candidate) -> INSTAGRAM
                tiktokPattern.containsMatchIn(candidate) -> TIKTOK
                twitterPattern.containsMatchIn(candidate) -> TWITTER
                dailymotionPattern.containsMatchIn(candidate) -> DAILYMOTION
                imdbPattern.containsMatchIn(candidate) -> IMDB
                vimeoPattern.containsMatchIn(candidate) -> VIMEO
                else -> null
            }
        }

        fun isSupportedSocialMediaUrl(text: String): Boolean {
            return fromInput(text) != null
        }

        fun isPlatformHomeUrl(text: String): Boolean {
            if (!isSupportedSocialMediaUrl(text)) return false
            val trimmed = text.trim()
            val candidate = if (trimmed.startsWith("http", ignoreCase = true)) {
                trimmed
            } else {
                "https://$trimmed"
            }
            return try {
                val uri = Uri.parse(candidate)
                val path = uri.path?.trim('/') ?: ""
                path.isEmpty() || (path == "watch" && uri.getQueryParameter("v").isNullOrBlank())
            } catch (e: Exception) {
                false
            }
        }
    }
}
