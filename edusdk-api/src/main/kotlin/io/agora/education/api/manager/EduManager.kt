package io.agora.education.api.manager

import android.util.Base64
import io.agora.education.R
import io.agora.education.api.EduCallback
import io.agora.education.api.logger.DebugItem
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.RoomCreateOptions
import io.agora.education.api.util.CryptoUtil.getAuth

abstract class EduManager(
        val options: EduManagerOptions
) {
    companion object {
        @JvmStatic
        fun init(options: EduManagerOptions): EduManager {
            return Class.forName("io.agora.education.impl.manager.EduManagerImpl")
                    .getConstructor(EduManagerOptions::class.java).newInstance(options) as EduManager
        }
    }

    abstract fun createClassroom(config: RoomCreateOptions, callback: EduCallback<EduRoom>)

    abstract fun logMessage(message: String, level: LogLevel)

    abstract fun uploadDebugItem(item: DebugItem, callback: EduCallback<String>)
}
