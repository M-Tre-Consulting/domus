package dev.domus.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun httpClientEngine(): HttpClient = HttpClient(CIO)
