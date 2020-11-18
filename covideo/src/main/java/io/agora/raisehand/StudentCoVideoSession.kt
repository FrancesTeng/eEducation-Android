package io.agora.raisehand

import android.content.Context
import io.agora.education.api.EduCallback
import io.agora.education.api.room.EduRoom
import io.agora.raisehand.CoVideoState.Applying
import io.agora.raisehand.CoVideoState.CoVideoing
import java.lang.ref.WeakReference
import kotlin.random.Random

internal abstract class StudentCoVideoSession(
        context: Context,
        eduRoom: EduRoom
) {
    var context: WeakReference<Context> = WeakReference(context)
    var eduRoom: WeakReference<EduRoom> = WeakReference(eduRoom)
    var curCoVideoState = CoVideoState.DisCoVideo
        protected set
    var processUuid: String = Random.nextLong().toString()
        private set

    /*是否允许举手
    * 1:允许  0:不允许*/
    var enableCoVideo = true

    /*是否允许举手即上台
    * 0:允许  1:不允许*/
    var autoCoVideo: Boolean = false

    companion object {
        const val STATE = "state"
        const val APPLY = "apply"
    }

    protected fun refreshProcessUuid() {
        processUuid = Random.nextLong().toString()
    }

    fun isApplying(): Boolean {
        return curCoVideoState == Applying
    }

    fun isCoVideoing(): Boolean {
        return curCoVideoState == CoVideoing
    }

    /**申请连麦*/
    abstract fun applyCoVideo(callback: EduCallback<Unit>)

    /**取消连麦
     * 老师处理前主动取消*/
    abstract fun cancelCoVideo(callback: EduCallback<Unit>)

    /**本地用户(举手、连麦)被老师同意/(拒绝、打断)
     * @param onStage 是否连麦*/
    abstract fun onLinkMediaChanged(onStage: Boolean)

    /**连麦中被老师打断*/
    abstract fun abortCoVideoing(): Boolean

    fun clear() {
        context.clear()
        eduRoom.clear()
    }
}