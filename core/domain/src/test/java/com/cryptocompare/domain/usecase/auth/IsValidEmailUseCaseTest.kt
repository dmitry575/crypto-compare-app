package com.cryptocompare.domain.usecase.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsValidEmailUseCaseTest {
    private val useCase = IsValidEmailUseCase()

    @Test
    fun `ordinary addresses are accepted`() {
        listOf(
            "user@example.com",
            "user.name+tag@example.co.uk",
            "USER@EXAMPLE.COM",
            "u@e.io",
        ).forEach { email ->
            assertTrue(email, useCase(email))
        }
    }

    @Test
    fun `blank input is rejected`() {
        assertFalse(useCase(""))
        assertFalse(useCase("   "))
    }

    @Test
    fun `addresses without at sign or domain dot are rejected`() {
        listOf(
            "userexample.com",
            "user@example",
            "@example.com",
            "user@.com",
            "user@example.",
        ).forEach { email ->
            assertFalse(email, useCase(email))
        }
    }

    @Test
    fun `whitespace inside the address is rejected`() {
        listOf(
            "user name@example.com",
            "user@exa mple.com",
            " user@example.com",
            "user@example.com ",
        ).forEach { email ->
            assertFalse(email, useCase(email))
        }
    }

    @Test
    fun `several at signs are rejected`() {
        assertFalse(useCase("user@@example.com"))
        assertFalse(useCase("user@host@example.com"))
    }
}
