package io.agora.education.impl.room.data.request

class EduUpdateRoomPropertyReq(
        val properties: MutableMap<String, String>,
        val cause: MutableMap<String, String>
) {}