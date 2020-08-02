package io.agora.education.impl.cmd

class CMDResponseBody<T>(
        val cmd: Int,
        val version: Int,
        val timestamp: Long,
        val requestId: String?,
        val data: T) {
}