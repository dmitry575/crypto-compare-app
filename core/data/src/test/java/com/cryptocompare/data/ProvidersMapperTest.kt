package com.cryptocompare.data

import com.cryptocompare.data.local.entity.ProviderEntity
import com.cryptocompare.data.mapper.toDomainFromEntity
import com.cryptocompare.data.mapper.toEntityFromDto
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.ProviderDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProvidersMapperTest {
    @Test
    fun `ProviderEntity toDomainFromEntity maps all fields`() {
        val entity =
            ProviderEntity(
                id = 42,
                name = "Provider",
                website = "https://example.com",
                status = ProviderStatus.Enabled.name,
                syncedAtMillis = 123L,
            )

        val domain = entity.toDomainFromEntity()

        assertEquals(42, domain.id)
        assertEquals("Provider", domain.name)
        assertEquals("https://example.com", domain.webSite)
        assertEquals(ProviderStatus.Enabled, domain.status)
    }

    @Test
    fun `ProviderDto toEntityFromDomain maps nullable values`() {
        val dto =
            ProviderDto(
                id = 1,
                name = null,
                webSite = null,
                baseUrl = null,
                status = ProviderStatus.None,
            )

        val entity = dto.toEntityFromDto(syncedAtMillis = 777L)

        assertEquals(1, entity.id)
        assertNull(entity.name)
        assertNull(entity.website)
        assertEquals(ProviderStatus.None.name, entity.status)
        assertEquals(777L, entity.syncedAtMillis)
    }

    @Test
    fun `List ProviderEntity toDomainFromEntity maps each element`() {
        val entities =
            listOf(
                ProviderEntity(1, "A", "a", ProviderStatus.Enabled.name, 1L),
                ProviderEntity(2, "B", "b", ProviderStatus.Disables.name, 1L),
            )

        val domain = entities.toDomainFromEntity()

        assertEquals(2, domain.size)
        assertEquals("A", domain[0].name)
        assertEquals(ProviderStatus.Enabled, domain[0].status)
        assertEquals("B", domain[1].name)
        assertEquals(ProviderStatus.Disables, domain[1].status)
    }
}
