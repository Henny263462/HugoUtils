package dev.henny.hugoutils.api

class MarketApiClient {
    fun isConfigured(): Boolean = ClientSessionStore.hasToken()
}
