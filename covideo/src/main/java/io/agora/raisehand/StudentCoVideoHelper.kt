package io.agora.raisehand

import android.content.Context
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.customMsgError
import io.agora.education.api.base.EduError.Companion.internalError
import io.agora.education.api.message.EduActionType
import io.agora.education.api.room.EduRoom
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduStartActionConfig
import io.agora.education.api.user.data.EduStopActionConfig
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.raisehand.CoVideoState.Applying
import io.agora.raisehand.CoVideoState.CoVideoing
import io.agora.raisehand.CoVideoState.DisCoVideo

internal class StudentCoVideoHelper(
        context: Context,
        eduRoom: EduRoom) :
        StudentCoVideoSession(context, eduRoom) {

    init {
        val properties = eduRoom.roomProperties
        properties?.let {
            for ((key, value) in properties) {
                if (key == STATE) {
                    enableCoVideo = (value as Int) == 1
                } else if (key == APPLY) {
                    autoCoVideo = (value as Int) == 0
                }
            }
        }
    }

    fun getLocalUser(callback: EduCallback<EduUser>) {
        eduRoom?.let {
            eduRoom.get()?.getLocalUser(callback)
        }
        callback.onFailure(internalError("current eduRoom is null"))
    }

    override fun applyCoVideo(callback: EduCallback<Unit>) {
        if (curCoVideoState != DisCoVideo) {
            callback.onFailure(customMsgError("can not apply,because current CoVideoState is not DisCoVideo!"))
            return
        }
        eduRoom.get()?.getFullUserList(object : EduCallback<MutableList<EduUserInfo>> {
            override fun onSuccess(res: MutableList<EduUserInfo>?) {
                if (res != null) {
                    res.forEach {
                        if (it.role == EduUserRole.TEACHER) {
                            val payload = mutableMapOf<String, Any>(Pair(BusinessType.BUSINESS,
                                    BusinessType.RAISEHAND))
                            val config = EduStartActionConfig(processUuid, EduActionType.EduActionTypeApply,
                                    it.userUuid, null, 4, payload)
                            getLocalUser(object : EduCallback<EduUser> {
                                override fun onSuccess(res: EduUser?) {
                                    if (res != null) {
                                        res.startActionWithConfig(config, object : EduCallback<Unit> {
                                            override fun onSuccess(res: Unit?) {
                                                curCoVideoState = Applying
                                                callback.onSuccess(Unit)
                                            }

                                            override fun onFailure(error: EduError) {
                                                callback.onFailure(error)
                                            }
                                        })
                                    } else {
                                        callback.onFailure(customMsgError("local user is null!"))
                                    }
                                }

                                override fun onFailure(error: EduError) {
                                    callback.onFailure(error)
                                }
                            })
                            return
                        }
                    }
                    callback.onFailure(customMsgError(context.get()?.getString(R.string.there_is_no_teacher_disable_covideo)))
                } else {
                    callback.onFailure(customMsgError("current room no user!"))
                }
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    /**老师处理前主动取消*/
    override fun cancelCoVideo(callback: EduCallback<Unit>) {
        if (curCoVideoState == Applying) {
            /*老师处理前主动取消*/
            val payload = mutableMapOf<String, Any>(Pair(BusinessType.BUSINESS,
                    BusinessType.RAISEHAND))
            val config = EduStopActionConfig(processUuid, EduActionType.EduActionTypeCancel, payload)
            getLocalUser(object : EduCallback<EduUser> {
                override fun onSuccess(res: EduUser?) {
                    if (res != null) {
                        res.stopActionWithConfig(config, object : EduCallback<Unit> {
                            override fun onSuccess(res: Unit?) {
                                curCoVideoState = DisCoVideo
                                callback.onSuccess(Unit)
                                refreshProcessUuid()
                            }

                            override fun onFailure(error: EduError) {
                                callback.onFailure(error)
                                refreshProcessUuid()
                            }
                        })
                    } else {
                        callback.onFailure(customMsgError("local user is null!"))
                    }
                }

                override fun onFailure(error: EduError) {
                    callback.onFailure(error)
                }
            })
        }
//        else if (curCoVideoState == CoVideoing) {
//            /*举手连麦中学生主动退出*/
//            val payload = mutableMapOf<String, Any>(Pair(BusinessType.BUSINESS,
//                    BusinessType.RAISEHAND))
//            val config = EduStopActionConfig(processUuid, EduActionType.EduActionTypeCancel, payload)
//            getLocalUser(object : EduCallback<EduUser> {
//                override fun onSuccess(res: EduUser?) {
//                    if (res != null) {
//                        res.stopActionWithConfig(config, object : EduCallback<Unit> {
//                            override fun onSuccess(res: Unit?) {
//                                curCoVideoState = DisCoVideo
//                                callback.onSuccess(Unit)
//                                refreshProcessUuid()
//                            }
//
//                            override fun onFailure(error: EduError) {
//                                callback.onFailure(error)
//                                refreshProcessUuid()
//                            }
//                        })
//                    } else {
//                        callback.onFailure(customMsgError("local user is null!"))
//                    }
//                }
//
//                override fun onFailure(error: EduError) {
//                    callback.onFailure(error)
//                }
//            })
//        }
    }

    override fun onLinkMediaChanged(onStage: Boolean) {
        curCoVideoState = if (onStage) CoVideoing else DisCoVideo
        //TODO 重置UI
    }

    override fun abortCoVideoing(): Boolean {
        if (isCoVideoing()) {
            curCoVideoState = DisCoVideo
            return true
        }
        return false
    }
}