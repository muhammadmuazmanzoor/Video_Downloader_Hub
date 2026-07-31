package com.avd.browserkit.util

import android.net.Uri

/**
 * Blocks known adult / NSFW hosts and URL keywords in the in-app browser.
 * YouTube and in-app drama platforms are always allowed.
 */
object AdultSiteBlocker {

    /**
     * Legitimate short-drama / reel hosts featured in the app.
     * Their episode paths often include romance title words (e.g. stepbrother)
     * that would otherwise false-positive the keyword scanner.
     */
    private val ALLOWED_DOMAINS = setOf(
        "dramabox.com",
        "dramaboxapp.com",
        "netshort.com",
        "flickreels.net",
    )

    /** Well-known adult domains (no www / m prefix). */
    private val BLOCKED_DOMAINS = setOf(
        // Major tubes
        "pornhub.com", "pornhub.org", "pornhub.net", "pornhubpremium.com", "pornhub.com.br",
        "xvideos.com", "xvideos.es", "xvideos2.com", "xvideos3.com", "xvideos4.com", "xvideos5.com",
        "xnxx.com", "xnxx.tv", "xnxx.health", "xnxx-cdn.com",
        "xhamster.com", "xhamster.desi", "xhamsterlive.com", "xhopen.com", "xhcdn.com",
        "redtube.com", "youporn.com", "tube8.com", "spankbang.com", "eporner.com", "hqporner.com",
        "sxyprn.com", "tnaflix.com", "porntube.com", "porn.com", "sex.com", "xxx.com", "ixxx.com",
        "beeg.com", "txxx.com", "hclips.com", "upornia.com", "drtuber.com", "sunporno.com",
        "pornoxo.com", "nuvid.com", "tubegalore.com", "pornmd.com", "gotporn.com", "pornburst.xxx",
        "anysex.com", "daftsex.com", "xgroovy.com", "porn4days.lc", "thisvid.com", "heavy-r.com",
        "empflix.com", "slutload.com", "keezmovies.com", "extremetube.com", "pornerbros.com",
        "4tube.com", "fapdu.com", "pornhd.com", "pornhdprime.com", "pornflip.com", "pornheed.com",
        "pornktube.com", "pornoxo.com", "porndig.com", "porntrex.com", "porngo.com", "porntop.com",
        "pornwild.com", "pornky.com", "pornzog.com", "porn00.org", "porn300.com", "porn555.com",
        "porndoe.com", "porndoe.tv", "porndish.com", "pornbox.com", "pornbest.org", "pornburst.com",
        "porndroids.com", "porngames.com", "pornhat.com", "pornhits.com", "pornkai.com",
        "pornlib.com", "pornl.com", "pornmax.com", "pornoxo.com", "pornrewind.com", "pornrox.com",
        "pornsavant.com", "pornsexer.com", "pornsocket.com", "pornteengirl.com", "porntrex.com",
        "pornwatchers.com", "pornwhit.com", "pornwex.com", "pornxbit.com", "pornxnxx.com",
        "alotporn.com", "alphaporno.com", "anyporn.com", "ashemaletube.com", "boyfriendtv.com",
        "bravotube.net", "definebabe.com", "efukt.com", "fapster.xxx", "fux.com", "gaytube.com",
        "hdzog.com", "hotmovs.com", "hqtube.xxx", "jizzbunker.com", "jizzonline.com",
        "lubetube.com", "madthumbs.com", "megatube.xxx", "moviefap.com", "nudevista.com",
        "nuvid.com", "perfectgirls.net", "porn.com", "pornhubselect.com", "pornrabbit.com",
        "pornshare.biz", "pornsos.com", "redtubepremium.com", "shooshtime.com", "slutload.com",
        "spankwire.com", "thenewporn.com", "tnaflix.com", "tubecup.com", "tubegalore.com",
        "tubepleasure.com", "vporn.com", "wankoz.com", "whoreshub.com", "xozilla.com", "xtube.com",
        "yespornplease.com", "youjizz.com", "zbporn.com", "zzgays.com",
        // Cam / live
        "chaturbate.com", "stripchat.com", "bongacams.com", "livejasmin.com", "cam4.com",
        "myfreecams.com", "camsoda.com", "flirt4free.com", "streamate.com", "imlive.com",
        "jasmin.com", "adultwork.com", "xcams.com", "camster.com", "camwhores.tv", "camwhoresbay.com",
        "camvideos.tv", "liveleakcam.com", "sexcamly.com",
        // Fan / paid
        "onlyfans.com", "fansly.com", "manyvids.com", "loyalfans.com", "justfor.fans", "fancentro.com",
        "faphouse.com", "adultdvdempire.com", "adultempire.com", "hotmovies.com", "aebn.com",
        // Studios / networks
        "brazzers.com", "bangbros.com", "realitykings.com", "digitalplayground.com", "mofos.com",
        "naughtyamerica.com", "adultfriendfinder.com", "teamskeet.com", "vixen.com", "blacked.com",
        "tushy.com", "deeper.com", "slayed.com", "milfed.com", "nubilefilms.com", "nubiles.net",
        "nubiles-porn.com", "realitykings.com", "fakehub.com", "bellesa.co", "bellesafilms.com",
        "kink.com", "evilangel.com", "julesjordan.com", "bang.com", "adulttime.com", "girlsway.com",
        "deviate.com", "puretaboo.com", "familystrokes.com", "pervmom.com", "momsteachsex.com",
        "propertysex.com", "publicagent.com", "fakeagent.com", "faketaxi.com", "sexart.com",
        "metart.com", "metartx.com", "sexlikereal.com", "czechvr.com", "virtualrealporn.com",
        "wankzvr.com", "vrbangers.com", "badoinkvr.com", "povr.com", "vrporn.com",
        // Image / gallery / leak
        "imagefap.com", "pornpics.com", "erome.com", "motherless.com", "thothub.to", "thothub.lol",
        "coomer.party", "coomer.su", "kemono.party", "kemono.su", "simpcity.su", "socialmediagirls.com",
        "redditnudes.com", "fapello.com", "thotslife.com", "thotsbay.com", "leakedmodels.com",
        "thefappeningblog.com", "thefappening.so", "celebjihad.com", "scandalplanet.com",
        "nudecelebforum.com", "analdin.com", "nsfw.xxx",
        // Hentai / anime / jav
        "nhentai.net", "hanime.tv", "hitomi.la", "e-hentai.org", "exhentai.org", "tsumino.com",
        "hentaifox.com", "simply-hentai.com", "hentai2read.com", "hentaiera.com", "hentairox.com",
        "fakku.net", "rule34.xxx", "rule34.paheal.net", "rule34.xxx", "gelbooru.com", "danbooru.donmai.us",
        "sankakucomplex.com", "chan.sankakucomplex.com", "iwara.tv", "f95zone.to",
        "javlibrary.com", "javdb.com", "javmost.com", "avgle.com", "missav.com", "missav.ws",
        "jable.tv", "javhd.com", "javhub.net", "javbangers.com", "javtrailers.com", "caribbeancom.com",
        "1pondo.tv", "heyzo.com", "pacopacomama.com", "tokyo-hot.com", "avdanyuwiki.com",
        // Desi / regional
        "doodhwali.com", "desiporn.tube", "desixnxx.net", "xxxdesi.net", "indianporn365.net",
        "cliphunter.com", "sexvid.xxx", "tubepornclassic.com", "hotpornfile.org",
        // More aggregators / mirrors
        "sexu.com", "sxyprn.net", "hqporner.com", "hqporner.net", "pornhits.com", "pornky.com",
        "ok.xxx", "ss.xxx", "hqbabes.com", "babes.com", "playboy.com", "playboyplus.com",
        "penthouse.com", "hustler.com", "scoreland.com", "bustyangelique.com",
        "xvideos-cdn.com", "phncdn.com", "trafficjunky.net", "ypncdn.com",
        "nude-gals.com", "nudecollect.com", "nudevista.tv", "freenudism.com", "pure-nudists.com",
        "nudostar.com", "nudostar.tv", "coedcherry.com", "babepedia.com", "indexxx.com",
        "boobpedia.com", "freeones.com", "iafd.com", "adultdvdtalk.com",
        "sexstories.com", "literotica.com", "asstr.org", "nifty.org",
        "pornhub.net", "xhamster.com", "youporn.com",
        "pornone.com", "porndish.com", "porntrex.com", "anyporn.com", "tubev.sex",
        "sex.com", "sexy.com", "xxxstreams.eu", "xxxstreams.org", "streamporn.pw",
        "netflav.com", "supjav.com", "javmenu.com", "javfree.me", "javfull.net",
        "watchporn.to", "watchmygf.me", "watchmygf.net", "pornktube.tv",
    )

    /**
     * Host / path tokens that strongly indicate adult content.
     * Longer NSFW words may match inside labels (e.g. nudetube).
     */
    private val BLOCKED_HOST_TOKENS = listOf(
        // Brands
        "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "spankbang",
        "chaturbate", "stripchat", "onlyfans", "fansly", "brazzers", "bangbros", "motherless",
        "nhentai", "hentai", "rule34", "eporner", "hqporner", "tnaflix", "porntube", "gotporn",
        "beeg", "xgroovy", "erome", "missav", "jable", "javlibrary", "javdb", "hanime",
        "kemono", "coomer", "f95zone", "fapello", "thothub", "faphouse", "livejasmin",
        "bongacams", "myfreecams", "camsoda", "camwhores", "adultfriendfinder", "manyvids",
        "nubiles", "metart", "kink", "evilangel", "teamskeet", "fakehub", "bellesa",
        "youjizz", "extremetube", "keezmovies", "slutload", "spankwire", "empflix",
        "analdin", "pornoxo", "porndoe", "porndig", "porngo", "porntop", "pornwild",
        "pornky", "pornzog", "porn300", "porn555", "alotporn", "alphaporno", "anyporn",
        "ashemaletube", "boyfriendtv", "bravotube", "definebabe", "efukt", "fapster",
        "gaytube", "hdzog", "hotmovs", "jizzbunker", "lubetube", "madthumbs", "megatube",
        "moviefap", "nudevista", "perfectgirls", "pornrabbit", "shooshtime", "tubecup",
        "tubepleasure", "vporn", "wankoz", "whoreshub", "xozilla", "xtube", "zbporn",
        "heavy-r", "cliphunter", "sexvid", "tubepornclassic", "watchmygf", "xxxstreams",
        "streamporn", "netflav", "supjav", "javmenu", "javfree", "javfull", "javhd",
        "javhub", "javbangers", "caribbeancom", "tokyo-hot", "hitomi", "exhentai",
        "e-hentai", "hentaifox", "hentairox", "hentaiera", "fakku", "gelbooru", "danbooru",
        "sankaku", "iwara", "simpcity", "socialmediagirls", "thefappening", "celebjihad",
        "scandalplanet", "nudostar", "boobpedia", "babepedia", "freeones", "literotica",
        "sexstories", "adultdvdempire", "adultempire", "hotmovies", "scoreland",
        // Generic adult
        "porn", "porno", "xxx", "nsfw", "xxxvideo", "xxxvideos", "xxxtube", "porntube",
        "pornvideos", "adultvideo", "adultvideos", "sexvideo", "sexvideos", "sextube",
        "fucktube", "fucktv", "hardcore", "softcore", "fucking", "intercourse", "pornfidelity",
        "pornfidility", "intarcourse", "incest", "stepsister", "stepbrother",
        "stepsis", "stepbro", "stepsiss", "stepbros",
        "stepfather", "stepmother", "stepdaughter", "stepson",
        "stepdad", "stepmom", "stepfamily", "stepsibling",
        // Body parts / acts (anatomical + common NSFW slang)
        "pussy", "penis", "vagina", "vulva", "labia", "clitoris", "clit",
        "nipple", "nipples", "areola", "areolas",
        "breast", "breasts", "boob", "boobs", "boobie", "boobies",
        "tits", "titties", "titty", "tittie", "jugs", "knockers", "melons", "hooters", "bosom",
        "booty", "butt", "buttocks", "asshole", "anus", "anal", "rectum",
        "cock", "dick", "balls", "ballsack", "scrotum", "testicle", "testicles",
        "shaft", "glans", "foreskin", "schlong", "pecker", "wiener", "dong",
        "crotch", "groin", "genitals", "genital", "genitalia", "privates",
        "cameltoe", "cleavage", "underboob", "sideboob", "downblouse", "upskirt",
        "cumshot", "creampie", "blowjob", "handjob", "footjob", "titjob", "titfuck",
        "deepthroat", "facefuck", "facesitting", "rimming", "pegging",
        "gangbang", "threesome", "foursome", "orgy", "bdsm", "bondage", "femdom",
        "cuckold", "squirting", "masturbat", "orgasm", "erotic", "erotica",
        "lingerie", "stripper", "striptease", "camgirl", "camboy", "webcamsex",
        "sexcam", "livesex", "liveporn", "freeporn", "freexxx",
        "teenporn", "milfporn", "gayporn", "lesbianporn", "shemale", "transporn",
        "hentai", "rule34", "ecchi", "yaoi", "yuri", "ahegao",
        "ass", "tit", "sex",
        // Body-part + video host fragments
        "pussyvideo", "pussyvideos", "boobvideo", "boobsvideo", "titsvideo",
        "assvideo", "assvideos", "cockvideo", "dickvideo", "nudebody",
        "bodypart", "bodyparts", "nakedbody", "sexybody", "hotbody",
        // Nude / naked (must match inside labels)
        "nude", "nudes", "naked", "nudism", "nudist", "nudity", "nudelive",
        "nudetube", "nudevideo", "nudevideos", "nudemodel", "nudemodels",
        "nudeceleb", "nudecelebs", "celebritynude", "celebnude", "leakednude",
        "onlynude", "freenude", "hotnude", "fullnude", "topless", "bottomless",
        // Slang / common NSFW host words
        "slut", "whore", "hooker", "escort", "brothel", "fetish", "kinky",
        "incest", "taboo", "xxxstream", "pornstream", "sexstream",
        "fap", "fapping", "jizz", "jerkoff", "wank", "wanking",
        "xxxpic", "xxxpics", "sexpic", "sexpics", "pornpic", "pornpics",
        "sexchat", "dirtytalk", "camsex", "sexdate", "sexdating", "adultfriend",
        "pornstar", "pornstars", "xxxstar", "adultstar",
    )

    /**
     * Core adult roots used for exact + fuzzy (typo) matching.
     * Catches misspellings like "vigina" → vagina.
     */
    private val ADULT_ROOTS = listOf(
        "porn", "porno", "xxx", "nsfw", "hentai", "nude", "nudes", "naked", "nudity",
        "pussy", "penis", "vagina", "vulva", "labia", "clitoris", "clit",
        "nipple", "nipples", "boob", "boobs", "boobie", "boobies",
        "tit", "tits", "titties", "titty", "booty", "asshole", "anus", "anal",
        "cock", "dick", "scrotum", "testicle", "genital", "genitals", "genitalia",
        "blowjob", "handjob", "footjob", "titjob", "cumshot", "creampie", "deepthroat",
        "gangbang", "orgy", "bdsm", "bondage", "fetish", "erotic", "erotica",
        "masturbate", "masturbation", "orgasm", "squirting",
        "pornhub", "xvideos", "xnxx", "xhamster", "youporn", "redtube", "spankbang",
        "onlyfans", "chaturbate", "stripchat", "brazzers", "youjizz",
        "camgirl", "sexcam", "livesex", "freeporn", "xxxvideo", "sexvideo",
        "topless", "bottomless", "upskirt", "cameltoe",
        // Acts / styles
        "intercourse", "fucking", "hardcore", "pornfidelity", "incest",
        // Step-family (compound forms; spaced forms handled by looksLikeStepFamilyPhrase)
        "stepbrother", "stepsister", "stepfather", "stepmother",
        "stepdaughter", "stepson", "stepmom", "stepdad", "stepsibling",
        "stepsis", "stepbro", "stepsiss", "stepbros", "stepfamily",
    )

    /**
     * Roles that after "step " / "step-" mark adult step-family content.
     * Includes common typos (dauther, etc.).
     */
    private val STEP_FAMILY_ROLES = setOf(
        "father", "mother", "dad", "mom", "daddy", "mommy",
        "daughter", "son", "brother", "sister", "sis", "bro",
        "sibling", "siblings", "parent", "parents", "child", "children",
        "uncle", "aunt", "niece", "nephew", "cousin",
        // typos / slang
        "dauther", "dauter", "daughtr", "daugter", "daugther",
        "fater", "fathe", "moter", "mothr", "brothr", "sistr", "daughtor",
    )

    private val STEP_FAMILY_ROLE_REGEX = Regex(
        """\bstep[\s_-]*(father|mother|dad|mom|daddy|mommy|daughter|son|brother|sister|sis|bro|sibling|siblings|parent|parents|child|children|uncle|aunt|niece|nephew|cousin|dauther|dauter|daughtr|daugter|daugther|fater|moter)\b""",
    )

    /**
     * Exact word hits only (no fuzzy) — too common alone for edit-distance matching.
     */
    private val ADULT_EXACT_WORDS = setOf(
        "loving", "kissing", "rough", "fuck", "fucking", "fucked", "fucker",
        "intercourse", "bdsm", "hardcore", "incest", "xxx",
    )

    /** Common intentional misspellings / leetspeak / typos for adult terms. */
    private val ADULT_MISSPELLINGS = setOf(
        // vagina typos
        "vigina", "vagine", "vagyna", "vagaina", "vagna", "vajina", "vagena", "vagenia",
        "vag1na", "vagin", "vajyna", "vagyina", "vaginia", "vaggina", "vajgina", "vigyna",
        "vegina", "vjgina",
        // pussy
        "pusy", "pussi", "pussie", "puzzy", "puzzie", "pussey", "pussyy", "pussay",
        "pussii", "puusy", "pusssy", "pussiee",
        // penis
        "penus", "penas", "penes", "peniss", "peniis", "peenis",
        // nude / naked
        "nued", "nuude", "nudd", "nide", "nakd", "nakked", "nakid", "nacked", "n4ked",
        "n00d", "n00ds", "nud3", "nud3s",
        // porn / tubes
        "prn", "pr0n", "p0rn", "porrn", "pornn", "porne", "pron", "porm",
        "pornhb", "xvidoes", "xvidios", "xhamstr", "xhamsters", "xnxxx",
        // sex / intercourse
        "seks", "sexx", "sexxx", "s3x", "5ex",
        "intarcourse", "intercouse", "intercours", "intercorse", "intercorse",
        "entercourse", "intercource", "intercorse", "intercorse",
        // fucking / hardcore
        "fuking", "fuckin", "fukkin", "fcking", "f*cking", "fuckingg", "fukcing",
        "hardcor", "hardcoree", "h4rdcore",
        // pornfidelity typo
        "pornfidility", "pornfidelity", "pornfidelty", "pornfidelitiy", "pornfidility",
        "porn fidelity",
        // boobs / tits
        "b00bs", "boobz", "t1ts", "tittz", "titz", "boobss",
        // cock / dick
        "cok", "c0ck", "cokc", "dik", "d1ck", "dicc", "dikk",
        // ass
        "azz", "a55", "arse",
        // other
        "xxxx", "hentay", "henti", "onlyfan",
        "blowjb", "blowj0b", "handjb", "cumsh0t",
        "incst", "incesst", "incset",
        // step-family slang / typos
        "stepsis", "stepbro", "stepsiss", "stepbros", "stepsi", "stepbruh",
        "stpsister", "stpbrother", "stpsis", "stpbro",
        "stepfater", "stepfathe", "stepmoter", "stepdaugter", "stepdauther",
        "stepdauter", "stepdaugther", "stepdaughtor", "stepbrothr", "stepsistr",
        "stpfather", "stpmother", "stpdaughter", "stpson", "stpdad", "stpmom",
    )

    /**
     * Short / ambiguous tokens: exact DNS label only
     * (avoids analytics←anal, classic←ass, massachusetts←ass).
     */
    private val EXACT_LABEL_ONLY_TOKENS = setOf(
        "ass", "tit", "sex", "fap", "jizz", "wank", "cum", "gay", "milf",
        "clit", "boob", "anal", "butt", "dong", "jugs",
    )

    /** Tokens that must also match URL path / query (not only host). */
    private val PATH_BLOCK_TOKENS = listOf(
        "pornhub", "xvideos", "xnxx", "xhamster", "onlyfans", "chaturbate",
        "/porn", "/xxx", "/nsfw", "/nude", "/nudes", "/naked", "/hentai",
        "/sex/", "/xxx/", "nude-video", "nudevideo", "nude_video", "nudes-",
        "porn-video", "pornvideo", "xxx-video", "xxxvideo", "adult-video",
        "freenude", "free-nude", "watch-nude", "nude-tube", "nudetube",
        "fullnude", "celebrity-nude", "leaked-nude", "naked-girl", "naked-women",
        "naked-video", "sex-video", "sexvideo", "hardcore-porn", "free-porn",
        "freeporn", "live-sex", "livesex", "cam-sex", "webcam-sex",
        "pussy-video", "pussyvideo", "boobs-video", "boobsvideo", "tits-video",
        "ass-video", "assvideo", "cock-video", "dick-video", "vagina-video",
        "penis-video", "breast-video", "booty-video", "naked-body", "nude-body",
        "vigina", "vagine", "/pussy", "/boobs", "/tits", "/asshole", "/blowjob", "/handjob",
        "intercourse", "intarcourse", "fucking", "bdsm", "hardcore",
        "pornfidelity", "pornfidility", "making-love", "makinglove",
        "brother-sister", "stepsister", "stepbrother", "incest",
        "stepsis", "stepbro", "stepsiss", "stepbros",
        "stepfather", "stepmother", "stepdaughter", "stepson",
        "stepdad", "stepmom", "step-father", "step-mother",
        "step-daughter", "step-son", "step-brother", "step-sister",
        "step-dad", "step-mom", "stepfamily",
        "feeling-each-other", "feelingeachother",
        // Adult media filenames / query fragments (not bare ".mp4")
        "porn.mp4", "xxx.mp4", "sex.mp4", "nude.mp4", "nudes.mp4",
        "naked.mp4", "hentai.mp4", "nsfw.mp4", "adult.mp4",
        "porn-mp4", "xxx-mp4", "sex-mp4", "nude-mp4",
        "stepsis.mp4", "stepbro.mp4", "stepsister.mp4", "stepbrother.mp4",
    )

    fun isBlocked(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (trimmed == "about:blank" || trimmed.startsWith("data:", ignoreCase = true)) return false
        // YouTube must always remain open in this app.
        if (YoutubeUrlUtils.isYouTubeUrl(trimmed)) return false

        // Plain search text (address bar / home search)
        if (!trimmed.contains("://") && (trimmed.contains(' ') || !trimmed.contains('.'))) {
            if (phraseLooksAdult(trimmed)) return true
        }

        val host = normalizedHost(trimmed) ?: return isBlockedBareHost(trimmed)
        // In-app drama platforms (Drama Box, NetShort, etc.) — never block.
        if (isAllowedDomain(host)) return false
        if (isBlockedDomain(host)) return true
        if (BLOCKED_HOST_TOKENS.any { token -> hostContainsToken(host, token) }) return true

        // Google/Bing/etc. search results for adult queries (e.g. q=vigina+videos)
        extractSearchQuery(trimmed)?.let { query ->
            if (phraseLooksAdult(query)) return true
        }

        if (pathLooksAdult(trimmed)) return true
        return false
    }

    private fun isAllowedDomain(host: String): Boolean {
        if (host in ALLOWED_DOMAINS) return true
        return ALLOWED_DOMAINS.any { domain -> host == domain || host.endsWith(".$domain") }
    }

    private fun isBlockedBareHost(raw: String): Boolean {
        val host = raw.lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .removePrefix("www.")
            .removePrefix("m.")
            .trimEnd('.')
        if (host.isBlank() || host.contains(' ')) {
            return phraseLooksAdult(raw.lowercase())
        }
        if (isBlockedDomain(host)) return true
        if (BLOCKED_HOST_TOKENS.any { hostContainsToken(host, it) }) return true
        return phraseLooksAdult(raw.lowercase())
    }

    private fun isBlockedDomain(host: String): Boolean {
        if (host in BLOCKED_DOMAINS) return true
        return BLOCKED_DOMAINS.any { domain -> host == domain || host.endsWith(".$domain") }
    }

    private fun hostContainsToken(host: String, token: String): Boolean {
        val t = token.lowercase().trim()
        if (t.isBlank() || !host.contains(t)) return false
        val parts = host.split('.')
        val exactOnly = t in EXACT_LABEL_ONLY_TOKENS || t.length <= 3
        return parts.any { part ->
            when {
                part == t -> true
                exactOnly -> false
                part.startsWith(t) || part.endsWith(t) || part.contains(t) -> true
                else -> false
            }
        }
    }

    private fun pathLooksAdult(url: String): Boolean {
        val lower = url.lowercase()
        val host = normalizedHost(url).orEmpty()
        if (host.contains("youtube") || host.contains("youtu.be")) return false
        if (PATH_BLOCK_TOKENS.any { token -> lower.contains(token) }) return true
        // Also scan decoded query blob for typos
        return phraseLooksAdult(lower)
    }

    /** Pull q/query from Google, Bing, DDG, Yahoo, etc. */
    private fun extractSearchQuery(url: String): String? {
        val uri = runCatching { Uri.parse(if (url.contains("://")) url else "https://$url") }.getOrNull()
            ?: return null
        val host = uri.host?.lowercase().orEmpty()
        val isSearchHost = host.contains("google.") ||
            host.contains("bing.") ||
            host.contains("duckduckgo.") ||
            host.contains("yahoo.") ||
            host.contains("search.yahoo.") ||
            host.contains("yandex.") ||
            host.contains("ecosia.") ||
            host.contains("startpage.") ||
            host.contains("brave.") ||
            host.contains("search.brave.")
        if (!isSearchHost) return null
        val keys = listOf("q", "query", "p", "text", "wd", "search")
        for (key in keys) {
            val value = uri.getQueryParameter(key)?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }

    private fun phraseLooksAdult(text: String): Boolean {
        val decoded = runCatching {
            java.net.URLDecoder.decode(text, Charsets.UTF_8.name())
        }.getOrDefault(text)
        val compact = decoded.lowercase()
            .replace('+', ' ')
            .replace('%', ' ')
            .replace('-', ' ')
            .replace('_', ' ')
            .replace('/', ' ')
            .replace('?', ' ')
            .replace('=', ' ')
            .replace('&', ' ')
            .replace('.', ' ')
            .replace(',', ' ')
        val words = compact.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        val joined = words.joinToString(" ")

        val phrases = listOf(
            "nude video", "nude videos", "nudes video", "naked video", "naked videos",
            "porn video", "porn videos", "xxx video", "xxx videos", "sex video", "sex videos",
            "free porn", "free xxx", "free nudes", "free nude", "watch porn", "watch xxx",
            "adult video", "adult videos", "hot nudes", "leaked nudes", "celebrity nudes",
            "nude girls", "nude girl", "naked girls", "naked girl", "nude women", "naked women",
            "cam sex", "live sex", "sex cam", "webcam sex", "hentai video", "rule34",
            "pussy video", "pussy videos", "boobs video", "boob video", "boobs videos",
            "tits video", "tits videos", "ass video", "ass videos", "booty video",
            "cock video", "dick video", "penis video", "vagina video", "breast video",
            "nipple video", "naked body", "nude body", "body parts video", "sexy body video",
            "blowjob video", "handjob video", "anal video", "cumshot video",
            "vigina video", "vigina videos", "vagine video", "vagine videos",
            "pusy video", "pusy videos", "pussi video",
            // User-requested phrases / acts
            "brother and sister making love",
            "brother and sister",
            "brother sister",
            "sister and brother",
            "sister brother",
            "making love",
            "make love",
            "makes love",
            "intercourse",
            "intarcourse",
            "pornfidelity",
            "pornfidility",
            "porn fidelity",
            "loving sex",
            "loving video",
            "loving videos",
            "kissing sex",
            "kissing porn",
            "kissing video",
            "kissing videos",
            "kissing nude",
            "rough sex",
            "rough porn",
            "rough video",
            "rough videos",
            "rough fucking",
            "hardcore sex",
            "hardcore porn",
            "hardcore video",
            "hardcore videos",
            "bdsm sex",
            "bdsm porn",
            "bdsm video",
            "bdsm videos",
            "fucking video",
            "fucking videos",
            "fucking porn",
            "step brother",
            "step sister",
            "stepbrother",
            "stepsister",
            "step father",
            "step mother",
            "step daughter",
            "step son",
            "step dad",
            "step mom",
            "step daddy",
            "step mommy",
            "stepfather",
            "stepmother",
            "stepdaughter",
            "stepson",
            "stepdad",
            "stepmom",
            "step sis",
            "step bro",
            "stepsis",
            "stepbro",
            "stepsiss",
            "stepbros",
            "stepsis and stepbro",
            "stepbro and stepsis",
            "stepsis stepbro",
            "stepbro stepsis",
            "step sis and step bro",
            "step bro and step sis",
            "step father and step daughter",
            "step daughter and step father",
            "stepfather and stepdaughter",
            "stepdaughter and stepfather",
            "feeling each other",
            "feel each other",
            "feeling eachother",
            "stepsis feeling",
            "stepbro feeling",
            "incest video",
            "incest videos",
            "incest porn",
            // Adult + media file / download wording
            "adult mp4", "porn mp4", "xxx mp4", "nude mp4", "nudes mp4",
            "naked mp4", "sex mp4", "hentai mp4", "nsfw mp4",
            "porn video mp4", "xxx video mp4", "sex video mp4",
            "download porn", "download xxx", "download nudes", "download nude",
            "porn download", "xxx download", "mp4 porn", "mp4 xxx", "mp4 sex",
        )
        if (phrases.any { joined.contains(it) }) return true

        // General: "step father", "step dauther", stepfather, …
        if (looksLikeStepFamilyPhrase(words, joined)) return true

        // Exact wording hits (loving / kissing / rough / fucking / …)
        if (words.any { it in ADULT_EXACT_WORDS }) return true

        // Any word that is adult / misspelling / close typo of an adult root
        // (covers "stepsis mp4", "porn.mp4" after '.' → space, etc.)
        if (words.any { wordLooksAdult(it) }) return true

        // Mild words only blocked with a media file word: "adult mp4", "erotic video"
        val hasMedia = words.any { it in MEDIA_EXT_WORDS }
        if (hasMedia && words.any { it in ADULT_MEDIA_CONTEXT_WORDS }) return true

        return false
    }

    /** File/media words that with an adult term mean adult download intent. */
    private val MEDIA_EXT_WORDS = setOf(
        "video", "videos", "vids", "clip", "clips",
        "mp4", "webm", "mkv", "avi", "mov", "mpeg", "mpg", "wmv", "flv",
    )

    /**
     * Too broad alone (e.g. "adult education") — only block when paired with
     * [MEDIA_EXT_WORDS] like mp4 / video.
     */
    private val ADULT_MEDIA_CONTEXT_WORDS = setOf(
        "adult", "erotic", "erotica", "lewd", "nsfw",
        "xxx", "porn", "porno", "hentai", "nude", "nudes", "naked",
    )

    /**
     * Blocks any "step + family role" wording (spaced, hyphen, or compound),
     * including typos like "step dauther".
     */
    private fun looksLikeStepFamilyPhrase(words: List<String>, joined: String): Boolean {
        for (i in words.indices) {
            val w = words[i].filter { it.isLetterOrDigit() }
            if (w.startsWith("step") && w.length > 4) {
                val role = w.removePrefix("step")
                if (isStepFamilyRole(role)) return true
            }
            if (w == "step" && i + 1 < words.size) {
                val role = words[i + 1].filter { it.isLetterOrDigit() }
                if (isStepFamilyRole(role)) return true
            }
        }
        // Hyphen / sticky forms still in blob: step-father, step_daughter
        return STEP_FAMILY_ROLE_REGEX.containsMatchIn(joined)
    }

    private fun isStepFamilyRole(role: String): Boolean {
        if (role.isBlank()) return false
        if (role in STEP_FAMILY_ROLES) return true
        // Fuzzy for longer role typos (dauther ≈ daughter)
        return STEP_FAMILY_ROLES.any { root ->
            if (root.length < 4) return@any false
            val maxDist = if (root.length >= 7) 2 else 1
            kotlin.math.abs(role.length - root.length) <= maxDist &&
                levenshtein(role, root) <= maxDist
        }
    }

    /**
     * Common URL/search words that must never fuzzy-match adult brands.
     * Example bug: "video" ≈ "xvideos" (edit distance 2) blocked every /video/ URL
     * including legitimate hosts like dailymotion.com.
     */
    private val INNOCENT_WORDS = setOf(
        "video", "videos", "vids", "clip", "clips", "movie", "movies",
        "watch", "free", "live", "daily", "motion", "media", "stream",
        "online", "download", "share", "channel", "content", "tube",
        "https", "http", "www", "com", "net", "org", "html", "php",
        "mp4", "webm", "m3u8", "embed", "player", "short", "shorts",
    )

    private fun wordLooksAdult(raw: String): Boolean {
        val word = raw.lowercase().filter { it.isLetterOrDigit() }
        if (word.length < 3) return false
        if (word in ADULT_MISSPELLINGS) return true
        if (word in ADULT_EXACT_WORDS) return true
        if (word in ADULT_ROOTS) return true
        // Never treat plain media words as adult via fuzzy/brand containment.
        if (word in INNOCENT_WORDS) return false
        // Compound step*family* without listing every form
        if (word.startsWith("step") && word.length > 4 && isStepFamilyRole(word.removePrefix("step"))) {
            return true
        }
        // Compact forms without spaces (pornfidelity)
        if (word.contains("pornfidel") || word.contains("intercourse") || word.contains("intarcourse")) {
            return true
        }
        // Host-token hits for longer brand/words (word must contain token, not reverse —
        // reverse made "video" match "xxxvideo"/"sexvideo" brand tokens).
        if (word.length >= 4 && BLOCKED_HOST_TOKENS.any { token ->
                token.length >= 4 && (word == token || word.contains(token))
            }
        ) {
            if (ADULT_ROOTS.any { root -> word == root || (root.length >= 4 && word.contains(root)) }) {
                return true
            }
            if (BLOCKED_HOST_TOKENS.any { token -> token.length >= 5 && word.contains(token) }) {
                return true
            }
        }
        // Fuzzy: edit distance ≤ 1 for roots length ≥ 5, ≤ 2 for length ≥ 7.
        // Require same first letter so "video" never matches "xvideos".
        return ADULT_ROOTS.any { root ->
            if (root.length < 4) return@any false
            if (word.first() != root.first()) return@any false
            val maxDist = when {
                root.length >= 7 -> 2
                root.length >= 5 -> 1
                else -> 0
            }
            if (maxDist == 0) return@any word == root
            kotlin.math.abs(word.length - root.length) <= maxDist &&
                levenshtein(word, root) <= maxDist
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost,
                )
            }
            for (j in prev.indices) prev[j] = curr[j]
        }
        return prev[b.length]
    }

    private fun normalizedHost(url: String): String? {
        val candidate = if (url.contains("://")) url else "https://$url"
        return runCatching {
            Uri.parse(candidate).host
                ?.lowercase()
                ?.trimEnd('.')
                ?.removePrefix("www.")
                ?.removePrefix("m.")
                ?.removePrefix("mobile.")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
