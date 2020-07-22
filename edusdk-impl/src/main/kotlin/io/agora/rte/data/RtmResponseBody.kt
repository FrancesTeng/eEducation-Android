package io.agora.rte.data

class RtmResponseBody<T>(
        val cmd: Int,
        val version: Int,
        val ts: Int,
        val data: T) {
}