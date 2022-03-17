package mixit.talk.model

import mixit.event.handler.AdminEventHandler.Companion.TIMEZONE
import mixit.talk.handler.TalkDto
import mixit.talk.model.Language.FRENCH
import mixit.user.model.Link
import mixit.user.model.User
import mixit.util.cache.Cached
import mixit.util.formatTalkDate
import mixit.util.formatTalkTime
import mixit.util.toSlug
import mixit.util.toVimeoPlayerUrl
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class CachedTalk(
    override val id: String,
    val format: TalkFormat,
    val event: String,
    val title: String,
    val summary: String,
    val speakers: List<User>,
    val language: Language,
    val addedAt: LocalDateTime,
    val description: String,
    val topic: String,
    val video: String?,
    val room: Room,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val photoUrls: List<Link>,
    val slug: String
) : Cached {

    constructor(talk: Talk, speakers: List<User>) : this(
        talk.id!!,
        talk.format,
        talk.event,
        talk.title,
        talk.summary,
        speakers,
        talk.language,
        talk.addedAt,
        talk.description ?: "",
        talk.topic ?: Topic.OTHER.value,
        talk.video,
        talk.room ?: Room.UNKNOWN,
        talk.start,
        talk.end,
        talk.photoUrls,
        talk.slug
    )

    fun toDto(language: Language, favorite: Boolean = false) = TalkDto(
        id,
        title.toSlug(),
        format,
        event,
        title(language),
        summary(language),
        speakers,
        language.name.lowercase(),
        addedAt,
        if (isRandomHidden()) "" else description,
        topic,
        video,
        video.toVimeoPlayerUrl(),
        "rooms.${room.name.lowercase()}",
        start?.formatTalkTime(language),
        end?.formatTalkTime(language),
        start?.formatTalkDate(language),
        favorite,
        photoUrls
    )

    fun toTalk() = Talk(
        format,
        event,
        title,
        summary,
        speakers.map { it.login },
        language,
        addedAt,
        description,
        topic,
        video,
        room,
        start,
        end,
        photoUrls,
        id = id
    )

    fun sanitizeForApi(): Talk = this.copy(
        title = title(language),
        summary = summary(language),
        description = if (isRandomHidden()) "" else description
    ).toTalk()

    private fun isRandomHidden(): Boolean =
        // Only for some format
        (format == KEYNOTE_SURPRISE || format == CLOSING_SESSION || format == RANDOM) &&
            // Only if start is in the past
            (start == null || start < Instant.now().atZone(ZoneId.of(TIMEZONE)).toLocalDateTime())

    // TODO use  val messageSource: MessageSource
    private fun title(language: Language): String =
        if (isRandomHidden()) {
            if (language == FRENCH) "Une surprise.... est une surprise" else "A surprise... is a surprise"
        } else {
            title
        }

    fun summary(language: Language): String =
        if (isRandomHidden()) {
            if (format == RANDOM) {
                if (language == Language.ENGLISH)
                    "This is a \"Random\" talk. For this track we choose the program for you. You are in a room, " +
                        "and a speaker comes to speak about a subject for which you ignore the content. Don't be " +
                        "afraid it's only for 20 minutes. As it's a surprise we don't display the session summary" +
                        " before...   "
                else
                    "Ce talk est de type \"random\". Pour cette track, nous choisissons le programme pour vous." +
                        "Vous êtes dans une pièce et un speaker vient parler d'un sujet dont vous ignorez le " +
                        "contenu. N'ayez pas peur, c'est seulement pour 20 minutes. Comme c'est une surprise, " +
                        "nous n'affichons pas le résumé de la session avant ..."
            } else {
                if (language == Language.ENGLISH)
                    "This is a \"surprise\" talk. For our keynote we choose the programm for you. You are in a room, " +
                        "and a speaker come to speak about a subject for which you ignore the content. Don't be " +
                        "afraid it's only for 30 minutes. As it's a surprise we don't display the " +
                        "session summary before...   "
                else
                    "Ce talk est une \"surprise\". Pour cette track, nous choisissons le programme pour vous. " +
                        "Vous êtes dans une pièce et un speaker vient parler d'un sujet dont vous ignorez le " +
                        "contenu. N'ayez pas peur, c'est seulement pour 30 minutes. Comme c'est une surprise, " +
                        "nous n'affichons pas le résumé de la session avant ..."
            }
        } else {
            summary
        }
}
