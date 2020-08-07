package io.agora.education.impl.cmd.bean

class CMDResponseBody<T>(
        val cmd: Int,
        val version: Int,
        val timestamp: Long,
        val requestId: String?,
        val data: T) {
}