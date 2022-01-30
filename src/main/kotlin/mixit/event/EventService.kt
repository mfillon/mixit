package mixit.event

import mixit.event.model.CachedEvent
import mixit.event.model.Event
import mixit.user.cache.CachedOrganization
import mixit.user.cache.CachedSponsor
import mixit.user.cache.CachedStaff
import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : CacheTemplate<CachedEvent>() {

    /**
     * Cache is initialized on startup
     */
    fun initializeCache() {
        findAll().collectList().block()
    }

    fun cacheStats() = cacheList.stats()

    fun invalidateCache() = cacheList.invalidateAll()

    fun findOne(id: String): Mono<CachedEvent> =
        findAll().collectList().flatMap { events -> Mono.justOrEmpty(events.firstOrNull { it.id == id }) }

    fun findAll(): Flux<CachedEvent> =
        findAll { eventRepository.findAll().flatMap { event -> loadEventUsers(event) } }

    fun save(event: Event) =
        eventRepository.save(event).also { cacheList.invalidateAll() }


    private fun loadEventUsers(event: Event): Mono<CachedEvent> {
        val userIds = event.organizations.map { it.organizationLogin } +
                event.sponsors.map { it.sponsorId } +
                event.volunteers.map { it.volunteerLogin }

        return userRepository.findAllByIds(userIds).collectList().map { users ->
            CachedEvent(
                event.id,
                event.start,
                event.end,
                event.current,
                event.sponsors.map { eventSponsoring ->
                    val sponsor = users.first { it.login == eventSponsoring.sponsorId }
                    CachedSponsor(sponsor, eventSponsoring)
                },
                event.organizations.map { orga ->
                    CachedOrganization(users.first { it.login == orga.organizationLogin })
                },
                event.volunteers.map { volunteer ->
                    CachedStaff(users.first { it.login == volunteer.volunteerLogin })
                },
                event.photoUrls,
                event.videoUrl,
                event.schedulingFileUrl,
                event.year
            )
        }
    }
}