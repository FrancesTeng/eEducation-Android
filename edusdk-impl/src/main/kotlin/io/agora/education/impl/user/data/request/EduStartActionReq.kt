package io.agora.education.impl.user.data.request

internal class EduStartActionReq(
        val action: Int,
        val toUserUuid: String,
        val fromUserUuid: String,
        val timeout: Long,
        val limit: Int,
        var payload: Map<String, Any>?
) {
}