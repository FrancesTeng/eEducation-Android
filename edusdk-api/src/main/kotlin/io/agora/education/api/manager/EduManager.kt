package io.agora.education.api.manager

import android.app.Activity
import android.text.TextUtils
import io.agora.education.api.BuildConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.logger.DebugItem
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.manager.listener.EduManagerEventListener
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.RoomCreateOptions

/**0代表正常
 * 1-10 本地错误code
 * 101 RTM错误-通信错误
 * 201 RTC错误-媒体错误
 * 301 HTTP错误-网络错误*/
abstract class EduManager(
        val options: EduManagerOptions
) {
    companion object {
        val TAG = EduManager::class.java.simpleName

        /**创建EduManager 和 登录rtm
         * code:message
         * 1:parameter XXX is invalid
         * 2:internal error：可以内部订阅具体什么错误
         * 101:communication error:code，透传rtm错误code。
         * 301:network error，透传后台错误msg字段*/
        @JvmStatic
        fun init(options: EduManagerOptions, callback: EduCallback<EduManager>) {
            if (TextUtils.isEmpty(options.appId)) {
                callback.onFailure(EduError.parameterError("appId"))
                return
            }
            if (TextUtils.isEmpty(options.customerId)) {
                callback.onFailure(EduError.parameterError("customerId"))
                return
            }
            if (TextUtils.isEmpty(options.customerCertificate)) {
                callback.onFailure(EduError.parameterError("customerCertificate"))
                return
            }
            if (TextUtils.isEmpty(options.userUuid)) {
                callback.onFailure(EduError.parameterError("userUuid"))
                return
            }
            if (TextUtils.isEmpty(options.userName)) {
                callback.onFailure(EduError.parameterError("userName"))
                return
            }
            val cla = Class.forName("io.agora.education.impl.manager.EduManagerImpl")
            val eduManager = cla.getConstructor(EduManagerOptions::class.java).newInstance(options) as EduManager
            val methods = cla.methods
            val iterator = methods.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (element.name == "login") {
                    element.invoke(eduManager, options.userUuid, object : EduCallback<Unit> {
                        override fun onSuccess(res: Unit?) {
                            callback.onSuccess(eduManager)
                        }

                        override fun onFailure(error: EduError) {
                            callback.onFailure(error)
                        }
                    })
                }
            }
        }

        fun version(): String {
            return BuildConfig.VERSION_NAME
        }
    }

    var eduManagerEventListener: EduManagerEventListener? = null

    abstract fun createClassroom(config: RoomCreateOptions): EduRoom?

    abstract fun release()

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误*/
    abstract fun logMessage(message: String, level: LogLevel): EduError

    /**日志上传之后，会通过回调把serialNumber返回
     * serialNumber：日志序列号，可以用于查询日志
     * code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    abstract fun uploadDebugItem(item: DebugItem, callback: EduCallback<String>): EduError
}
