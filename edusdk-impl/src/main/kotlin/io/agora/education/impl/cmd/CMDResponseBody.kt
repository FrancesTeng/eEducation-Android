package io.agora.education.impl.cmd

class CMDResponseBody<T>(
        val cmd: Int,
        val version: Int,
        val ts: Long,
        val data: T) {
}