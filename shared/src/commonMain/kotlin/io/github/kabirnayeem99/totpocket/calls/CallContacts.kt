package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import totpocket.shared.generated.resources.Res
import totpocket.shared.generated.resources.avatar_gofu
import totpocket.shared.generated.resources.avatar_sultan
import totpocket.shared.generated.resources.avatar_unknown
import kotlin.jvm.JvmInline

@JvmInline
value class ContactId(val value: String)

/** What a contact's round avatar shows. No emoji faces. */
@Immutable
sealed interface ContactFace {
    /** A drawn portrait; [fillsCircle] when the artwork is already a full disc. */
    data class Picture(val resource: DrawableResource, val fillsCircle: Boolean) : ContactFace

    /** Just the first letter, as a phone shows a contact with no photo. Used for every woman. */
    data class Initial(val letter: String) : ContactFace
}

/** Someone the child can pretend to call, with what they say and how they sound. */
@Immutable
data class Contact(
    val id: ContactId,
    val name: String,
    val face: ContactFace,
    /** The avatar's background; every contact has its own. */
    val color: Color,
    val lines: CallLines,
    val voice: CallVoice,
    /** The letter's colour on [color], for [ContactFace.Initial]. */
    val ink: Color = Color(0xFF1B1B1B),
)

/**
 * Bangla rhymes (ছড়া) and short poems, each introduced by its title and poet ("লোকছড়া" for folk
 * rhymes), then said in one go so the voice flows from line to line. Only folk rhymes and poets
 * who died long enough ago that their work is free for anyone to use.
 */
private fun rhyme(title: String, poet: String, text: String) = CallTalk(
    listOf("$title। $poet।", text.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")),
    waitsForChild = false,
)

private val Talks = listOf(
    rhyme(
        "আম পাতা জোড়া জোড়া", "লোকছড়া",
        """
        আম পাতা জোড়া জোড়া
        মারবো চাবুক চড়বো ঘোড়া।
        ওরে বুবু সরে দাঁড়া
        আসছে আমার পাগলা ঘোড়া।
        পাগলা ঘোড়া ক্ষেপেছে
        চাবুক ছুঁড়ে মেরেছে।
        """,
    ),
    rhyme(
        "আতা গাছে তোতা পাখি", "লোকছড়া",
        """
        আতা গাছে তোতা পাখি
        ডালিম গাছে মউ।
        এত ডাকি তবু কথা
        কও না কেন বউ।
        মাছরাঙা পাখি রে
        মাছ খাবি নাকি রে?
        খাবি তো আয়না
        মিছে কেন বায়না।
        আমাদের বিলেতে
        মাছ খায় চিলেতে
        তুই কেন খাবিনে?
        পরে এসে পাবিনে।
        """,
    ),
    rhyme(
        "নোটন নোটন পায়রাগুলি", "লোকছড়া",
        """
        নোটন নোটন পায়রাগুলি
        ঝোটন বেঁধেছে,
        ওপারেতে ছেলেমেয়ে
        নাইতে নেমেছে।
        দুই ধারে দুই রুই কাতলা
        ভেসে উঠেছে,
        কে দেখেছে কে দেখেছে
        দাদু দেখেছে
        দাদুর হাতে কলম ছিল
        ছুঁড়ে মেরেছে
        উঃ বড্ড লেগেছে।
        """,
    ),
    rhyme(
        "আগডুম বাগডুম", "লোকছড়া",
        """
        আগডুম বাগডুম ঘোড়াডুম সাজে
        ঢাক ঢোল ঝাঁজর বাজে।
        বাজতে বাজতে চললো ঢুলি
        ঢুলি গেলো কমলাফুলি।
        কমলাফুলির টিয়েটা
        সূর্য্যিমামার বিয়েটা।
        """,
    ),
    rhyme(
        "গোল করোনা", "লোকছড়া",
        """
        গোল করোনা গোল করোনা,
        ছোটন ঘুমায় খাটে।
        এই ঘুমকে কিনতে হলো,
        নওয়াব বাড়ির হাটে।
        সোনা নয় রূপা নয়,
        দিলাম মোতির মালা।
        তাই তো ছোটন ঘুমিয়ে আছে,
        ঘর করে উজালা।
        """,
    ),
    rhyme(
        "কাকাতুয়া", "যোগীন্দ্রনাথ সরকার",
        """
        কাকাতুয়া, কাকাতুয়া, আমার যাদুমণি,
        সোনার ঘড়ি কি বলিছে, বল দেখি শুনি?
        বলিছে সোনার ঘড়ি, টিক্ টিক্ টিক্,
        যা কিছু করিতে আছে, করে ফেল ঠিক।
        সময় চলিয়া যায়,
        নদীর স্রোতের প্রায়,
        যে জন না বুঝে, তারে ধিক্ শত ধিক।
        বলিছে সোনার ঘড়ি, টিক্ টিক্ টিক্।
        কাকাতুয়া, কাকাতুয়া, আমার যাদুধন,
        অন্য কোন কথা ঘড়ি বলে কি কখন?
        মাঝে মাঝে বলে ঘড়ি, টঙ্ টঙ্ টঙ্,
        মানুষ হইয়ে যেন হয়ো না ক সঙ।
        ফিটফিটে বাবু হলে,
        ভেবেছ কি লবে কোলে?
        পলাশে কে ভালবাসে দেখে রাঙা রঙ্।
        মাঝে মাঝে বলে ঘড়ি, টঙ্ টঙ্ টঙ্।
        """,
    ),
    rhyme(
        "আয়রে ভোলা", "সুকুমার রায়",
        """
        আয়রে ভোলা খেয়াল-খোলা
        স্বপনদোলা নাচিয়ে আয়,
        আয়রে পাগল আবোল তাবোল
        মত্ত মাদল বাজিয়ে আয়।
        আয় যেখানে ক্ষ্যাপার গানে
        নাইকো মানে নাইকো সুর।
        আয়রে যেথায় উধাও হাওয়ায়
        মন ভেসে যায় কোন সুদূর।
        আয় ক্ষ্যাপা-মন ঘুচিয়ে বাঁধন
        জাগিয়ে নাচন তাধিন্‌ ধিন্‌,
        আয় বেয়াড়া সৃষ্টিছাড়া
        নিয়মহারা হিসাবহীন।
        আজগুবি চাল বেঠিক বেতাল
        মাতবি মাতাল রঙ্গেতে–
        আয়রে তবে ভুলের ভবে
        অসম্ভবের ছন্দেতে।।
        """,
    ),
    rhyme(
        "পাখি সব করে রব", "মদনমোহন তর্কালঙ্কার",
        """
        পাখি সব করে রব রাতি পোহাইল।
        কাননে কুসুমকলি সকলি ফুটিল।।
        শীতল বাতাস বয় জুড়ায় শরীর।
        পাতায়-পাতায় পড়ে নিশির শিশির।।
        ফুটিল মালতী ফুল সৌরভ ছুটিল।
        পরিমল লোভে অলি আসিয়া জুটিল ॥
        গগনে উঠিল রবি সোনার বরণ।
        আলোক পাইয়া লোক পুলকিত মন ॥
        রাখাল গরুর পাল লয়ে যায় মাঠে।
        শিশুগণ দেয় মন নিজ নিজ পাঠে ॥
        উঠ শিশু মুখ ধোও পর নিজ বেশ।
        আপন পাঠেতে মন করহ নিবেশ ॥
        """,
    ),
    rhyme(
        "লেখাপড়া করে যেই", "মদনমোহন তর্কালঙ্কার",
        """
        লেখা পড়া করে যেই।
        গাড়ী ঘোড়া চড়ে সেই।।
        লেখা পড়া যেই জানে।
        সব লোক তারে মানে।।
        কটু ভাষী নাহি হবে।
        মিছা কথা নাহি কবে।।
        পর ধন নাহি লবে।
        চিরদিন সুখে রবে।।
        পিতামাতা গুরুজনে।
        সেবা কর কায় মনে।।
        """,
    ),
    rhyme(
        "কাজের লোক", "নবকৃষ্ণ ভট্টাচার্য",
        """
        মৌমাছি, মৌমাছি
        কোথা যাও নাচি নাচি
        দাঁড়াও না একবার ভাই।
        ওই ফুল ফোটে বনে
        যাই মধু আহরণে
        দাঁড়াবার সময় তো নাই।
        """,
    ),
    rhyme(
        "হনহন পনপন", "সুকুমার রায়",
        """
        চলে হনহন
        ছোটে পনপন
        ঘোরে বনবন
        কাজে ঠনঠন
        """,
    ),
    rhyme(
        "আবোল তাবোল", "সুকুমার রায়",
        """
        মেঘ মুলুকে ঝাপ‌্সা রাতে,
        রামধনুকের আবছায়াতে,
        তাল বেতালে খেয়াল সুরে,
        তান ধরেছি কন্ঠ পুরে।
        হেথায় নিষেধ নাইরে দাদা,
        নাইরে বাঁধন নাইরে বাধা।
        হেথায় রঙিন্ আকাশতলে
        স্বপন দোলা হাওয়ায় দোলে,
        সুরের নেশায় ঝরনা ছোটে,
        আকাশ কুসুম আপনি ফোটে,
        রঙিয়ে আকাশ, রঙিয়ে মন
        চমক জাগে ক্ষণে ক্ষণ।
        আজকে দাদা যাবার আগে
        বল্‌ব যা মোর চিত্তে লাগে-
        নাই বা তাহার অর্থ হোক্না
        ইবা বুঝুক বেবাক্ লোক।
        আপনাকে আজ আপন হতে
        ভাসিয়ে দিলাম খেয়াল স্রোতে।
        ছুট‌লে কথা থামায় কে?
        আজকে ঠেকায় আমায় কে?
        আজকে আমার মনের মাঝে
        ধাঁই ধপাধপ তব্‌লা বাজে-
        রাম-খটাখট ঘ্যাচাং ঘ্যাঁচ্
        কথায় কাটে কথায় প্যাঁচ্ ।
        আলোয় ঢাকা অন্ধকার,
        ঘন্টা বাজে গন্ধে তার।
        গোপন প্রাণে স্বপন দূত,
        মঞ্চে নাচেন পঞ্চ ভুত!
        হ্যাংলা হাতী চ্যাং দোলা,
        শূন্যে তাদের ঠ্যাং তোলা!
        মক্ষিরাণী পক্ষীরাজ-
        দস্যি ছেলে লক্ষ্মী আজ!
        আদিম কালের চাঁদিম হিম
        তোড়ায় বাঁধা ঘোড়ার ডিম।
        ঘনিয়ে এল ঘুমের ঘোর,
        গানের পালা সাঙ্গ মোর।
        """,
    ),
    rhyme(
        "আমাদের ছোট নদী", "রবীন্দ্রনাথ ঠাকুর",
        """
        আমাদের ছোটো নদী চলে বাঁকে বাঁকে
        বৈশাখ মাসে তার হাঁটু জল থাকে।
        পার হয়ে যায় গোরু, পার হয় গাড়ি,
        দুই ধার উঁচু তার, ঢালু তার পাড়ি।
        চিক্ চিক্ করে বালি, কোথা নাই কাদা,
        একধারে কাশবন ফুলে ফুলে সাদা।
        কিচিমিচি করে সেথা শালিকের ঝাঁক,
        রাতে ওঠে থেকে থেকে শেয়ালের হাঁক।
        """,
    ),
    rhyme(
        "স্বাধীনতার সুখ", "রজনীকান্ত সেন",
        """
        বাবুই পাখিরে ডাকি, বলিছে চড়াই-
        “কুঁড়ে ঘরে থেকে কর শিল্পের বড়াই;
        আমি থাকি মহাসুখে অট্টালিকা ‘পরে,
        তুমি কত কষ্ট পাও রোদ, বৃষ্টি, ঝড়ে।”
        বাবুই হাসিয়া কহে- “সন্দেহ কি তায় ?
        কষ্ট পাই, তবু থাকি নিজের বাসায়;
        পাকা হোক, তবু ভাই, পরের ও বাসা,
        নিজ হাতে গড়া মোর কাঁচা ঘর, খাসা।”
        """,
    ),
    rhyme(
        "সবার আমি ছাত্র", "সুনির্মল বসু",
        """
        আকাশ আমায় শিক্ষা দিল
        উদার হতে ভাই রে,
        কর্মী হবার মন্ত্র আমি
        বায়ুর কাছে পাই রে।
        পাহাড় শিখায় তাহার সমান-
        হই যেন ভাই মৌন-মহান,
        খোলা মাঠের উপদেশে-
        দিল-খোলা হই তাই রে।
        """,
    ),
    rhyme(
        "পারিব না", "কালীপ্রসন্ন ঘোষ",
        """
        ‘পারিব না’ একথাটি বলিও না আর,
        কেন পারিবে না তাহা ভাব একবার;
        পাঁচজনে পারে যাহা,
        তুমিও পারিবে তাহা,
        পার কি না পার কর যতন আবার
        একবার না পারিলে দেখ শতবার।
        পারিবে না বলে মুখ করিও না ভার,
        ও কথাটি মুখে যেন না শুনি তোমার।
        অলস অবোধ যারা
        কিছুই পারে না তারা,
        তোমায় তো দেখি নাক তাদের আকার
        তবে কেন ‘পারিব না’ বল বার বার ?
        """,
    ),
    rhyme(
        "আদর্শ ছেলে", "কুসুমকুমারী দাশ",
        """
        আমাদের দেশে হবে সেই ছেলে কবে
        কথায় না বড় হয়ে কাজে বড় হবে ?
        মুখে হাসি, বুকে বল তেজে ভরা মন
        “মানুষ হইতে হবে” — এই তার পণ,
        বিপদ আসিলে কাছে হও আগুয়ান,
        নাই কি শরীরে তব রক্ত মাংস প্রাণ ?
        হাত, পা সবারই আছে মিছে কেন ভয়,
        চেতনা রয়েছে যার সে কি পড়ে রয় ?
        সে ছেলে কে চায় বল কথায়-কথায়,
        আসে যার চোখে জল মাথা ঘুরে যায়
        সাদা প্রাণে হাসি মুখে কর এই পণ —
        “মানুষ হইতে হবে মানুষ যখন”
        কৃষকের শিশু কিংবা রাজার কুমার
        সবারি রয়েছে কাজ এ বিশ্ব মাঝার,
        হাতে প্রাণে খাট সবে শক্তি কর দান
        তোমরা মানুষ হলে দেশের কল্যাণ
        """,
    ),
    rhyme(
        "মোদের গরব", "অতুলপ্রসাদ সেন",
        """
        মোদের গরব, মোদের আশা,
        আ-মরি বাংলা ভাষা!
        তোমার কোলে,
        তোমার বোলে,
        কতই শান্তি ভালোবাসা!
        কি যাদু বাংলা গানে!
        গান গেয়ে দাঁড় মাঝি টানে,
        গেয়ে গান নাচে বাউল,
        গান গেয়ে ধান কাটে চাষা!
        """,
    ),
)

/**
 * Who the child can call: friendly characters, never the child's own family, so a pretend call
 * is never mistaken for a real relative. Each says a salam, one little story or rhyme, and
 * Allah Hafez. No pictures of women: Tuntuni and Moyna show their first letter.
 */
object CallContacts {
    val Tuntuni = Contact(
        ContactId("tuntuni"), "Tuntuni", ContactFace.Initial("T"), color = Color(0xFFF8BBD0), ink = Color(0xFFAD1457),
        lines = CallLines(
            greeting = "আসসালামু আলাইকুম, সোনা বাবু! আমি টুনটুনি!",
            talks = Talks,
            bye = "আল্লাহ হাফেজ, সোনা! টাটা!",
        ),
        voice = CallVoice(woman = true, pitch = 1.15f, rate = 0.9f),
    )
    val Gofu = Contact(
        ContactId("gofu"), "Gofu", ContactFace.Picture(Res.drawable.avatar_gofu, fillsCircle = false), color = Color(0xFFBBDEFB),
        lines = CallLines(
            greeting = "আসসালামু আলাইকুম, বাবু! আমি গোঁফু!",
            talks = Talks,
            bye = "আল্লাহ হাফেজ, বন্ধু! টাটা!",
        ),
        voice = CallVoice(woman = false, pitch = 0.78f, rate = 0.88f),
    )
    val Moyna = Contact(
        ContactId("moyna"), "Moyna", ContactFace.Initial("M"), color = Color(0xFFD1C4E9), ink = Color(0xFF4527A0),
        lines = CallLines(
            greeting = "আসসালামু আলাইকুম! ময়না পাখি বলছি। কেমন আছো, সোনা?",
            talks = Talks,
            bye = "আল্লাহ হাফেজ, সোনা! ফি আমানিল্লাহ!",
        ),
        voice = CallVoice(woman = true, pitch = 0.95f, rate = 0.8f),
    )
    val Sultan = Contact(
        ContactId("sultan"), "Sultan", ContactFace.Picture(Res.drawable.avatar_sultan, fillsCircle = true), color = Color(0xFFC8E6C9),
        lines = CallLines(
            greeting = "আসসালামু আলাইকুম, ছোট্ট বন্ধু! আমি সুলতান!",
            talks = Talks,
            bye = "আল্লাহ হাফেজ, ছোট্ট বন্ধু!",
        ),
        voice = CallVoice(woman = false, pitch = 0.68f, rate = 0.82f),
    )

    /** Who answers when the child dials a number on the keypad: a squeaky stranger with a rhyme. */
    val UnknownCaller = Contact(
        ContactId("unknown"), "Unknown", ContactFace.Picture(Res.drawable.avatar_unknown, fillsCircle = true), color = Color(0xFFFFF59D),
        lines = CallLines(
            greeting = "হ্যালো হ্যালো! কে? কে? কে?",
            talks = Talks,
            bye = "বাই বাই! টাটা!",
        ),
        voice = CallVoice(woman = false, pitch = 1.6f, rate = 1.05f),
    )

    /** Shown on the contacts screen, in this order. */
    val favourites: List<Contact> = listOf(Tuntuni, Gofu, Moyna, Sultan)

    fun find(id: String): Contact = (favourites + UnknownCaller).firstOrNull { it.id.value == id } ?: Tuntuni
}
