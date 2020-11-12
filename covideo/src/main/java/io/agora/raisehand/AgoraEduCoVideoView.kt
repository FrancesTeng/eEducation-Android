package io.agora.raisehand

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.widget.AppCompatImageView
import com.google.gson.Gson
import io.agora.base.ToastManager
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.message.EduActionMessage
import io.agora.education.api.message.EduActionType
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.raisehand.CoVideoActionType.ABORT

/**
 * 举手组件布局
 * */
class AgoraEduCoVideoView : AppCompatImageView {

    private lateinit var session: StudentCoVideoSession
    private var initialized = false
    private var imgs: Array<Int> = arrayOf(R.drawable.ic_hand_up, R.drawable.ic_hand_down)
    private var downTime: Long = System.currentTimeMillis()
    private val longClickInternal: Long = 3000
    private var coVideoListener: AgoraEduCoVideoListener? = null

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initView()
    }

    /**初始化时，图片是默认*/
    private fun initView() {
        setImageResource(imgs[0])
    }

    fun init(eduRoom: EduRoom) {
        session = StudentCoVideoHelper(context, eduRoom)
        if (context is AgoraEduCoVideoListener) {
            coVideoListener = context as AgoraEduCoVideoListener
        }
        animation = AlphaAnimation(1.0f, 0.5f)
        animation.repeatMode = Animation.REVERSE
        animation.repeatCount = Animation.INFINITE
        animation.duration = 300
        animation.cancel()
        setOnTouchListener { v, motionEvent ->
            when (motionEvent?.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_UP -> {
                    /*长按三秒举手/取消举手/主动退出举手*/
                    if (System.currentTimeMillis() - downTime > longClickInternal) {
                        if (session.isCoVideoing()) {
                            cancelCoVideo()
                        } else {
                            applyCoVideo()
                        }
                    }
                }
                else -> {
                }
            }
            false
        }
        initialized = true
    }

    private fun operateAnimation(enable: Boolean) {
        if (enable) {
            animation.startNow()
        } else {
            animation.cancel()
        }
    }

    /**申请连麦*/
    private fun applyCoVideo() {
        session.applyCoVideo(object : EduCallback<Unit> {
            override fun onSuccess(res: Unit?) {
                coVideoListener?.onApplyCoVideoComplete()
                /*申请连麦接口访问成功之后，等待老师处理过程中，按钮闪烁*/
                operateAnimation(true)
            }

            override fun onFailure(error: EduError) {
                coVideoListener?.onApplyCoVideoFailed(error)
            }
        })
    }

    /**取消连麦
     * 老师处理前主动取消和老师处理后主动退出*/
    private fun cancelCoVideo() {
        session.cancelCoVideo(object : EduCallback<Unit> {
            override fun onSuccess(res: Unit?) {
                coVideoListener?.onCancelCoVideoSuccess()
            }

            override fun onFailure(error: EduError) {
                coVideoListener?.onCancelCoVideoFailed(error)
            }
        })
    }

    /**本地用户举手(连麦)被老师同意/(拒绝、打断)
     * @param agree 举手(连麦)请求是否被允许*/
    fun onLinkMediaChanged(agree: Boolean) {
        /**老师处理过后，动画消失*/
        operateAnimation(false)
        session.onLinkMediaChanged(agree)
    }

    /**同步连麦状态-数据来源有两处，所以有两个同步函数
     * syncCoVideoState(EduMsg)同步peer消息过来的状态(包括abort)
     * syncCoVideoState(EduActionMessage)同步action消息过来的状态(包括accept和reject*/
    fun syncCoVideoState(msg: EduMsg) {
        val peerMsg: CoVideoPeerMsg = Gson().fromJson(msg.message, CoVideoPeerMsg::class.java)
        if (peerMsg.cmd == CoVideoPeerMsg.COVIDEOCMD) {
            val coVideoMsg = Gson().fromJson(peerMsg.data.toString(), CoVideoMsg::class.java)
            when (coVideoMsg.type) {
                ABORT -> {
                    onLinkMediaChanged(false)
                    ToastManager.showShort(R.string.covideo_abort_interactive)
                    coVideoListener?.onCoVideoAborted()
                }
                else -> {
                }
            }
        }
    }

    /**同步连麦状态*/
    fun syncCoVideoState(actionMsg: EduActionMessage) {
        if (actionMsg.processUuid != session.processUuid) {
            return
        }
        when (actionMsg.action) {
            EduActionType.EduActionTypeAccept -> {
                onLinkMediaChanged(true)
                ToastManager.showShort(R.string.covideo_accept_interactive)
                coVideoListener?.onCoVideoAccepted()
            }
            EduActionType.EduActionTypeReject -> {
                onLinkMediaChanged(false)
                ToastManager.showShort(R.string.covideo_reject_interactive)
                coVideoListener?.onCoVideoRejected()
            }
            else -> {
            }
        }
    }

    fun destroy() {
        session.clear()
        if (animation != null) {
            animation.cancel()
            animation = null
        }
    }
}