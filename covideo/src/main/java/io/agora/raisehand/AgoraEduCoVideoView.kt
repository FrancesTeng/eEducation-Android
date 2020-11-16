package io.agora.raisehand

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import io.agora.base.ToastManager
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.message.EduActionMessage
import io.agora.education.api.message.EduActionType
import io.agora.education.api.room.EduRoom
import io.agora.raisehand.CoVideoState.CoVideoing
import io.agora.raisehand.CoVideoState.DisCoVideo
import kotlinx.android.synthetic.main.view_covideo_layout.view.*

/**
 * 举手组件布局
 * */
class AgoraEduCoVideoView : LinearLayout {

    private lateinit var countDownImg: AppCompatImageView
    private lateinit var handImg: AppCompatImageView

    private lateinit var session: StudentCoVideoSession
    private var initialized = false
    private var countDownImgs: Array<Int> = arrayOf(R.drawable.ic_covideo_3, R.drawable.ic_covideo_2,
            R.drawable.ic_covideo_1)
    private var handImgs: Array<Int> = arrayOf(R.drawable.ic_hand_up, R.drawable.ic_hand_down)
    private var coVideoListener: AgoraEduCoVideoListener? = null
    private var countDownTimer: CountDownTimer = object : CountDownTimer(1000 * 3, 1000) {
        override fun onFinish() {
            countDownImg.visibility = View.INVISIBLE
            countDownImg.setBackgroundResource(countDownImgs[0])
            /*倒计时结束，发起举手申请*/
            applyCoVideo()
        }

        override fun onTick(millisUntilFinished: Long) {
            val index: Int = (3 - (millisUntilFinished / 1000)).toInt()
            countDownImg.setBackgroundResource(countDownImgs[index])
        }

    }

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initView(context)
    }

    /**初始化时，图片是默认*/
    private fun initView(context: Context) {
        inflate(context, R.layout.view_covideo_layout, this)
        countDownImg = findViewById(R.id.countDownImg)
        handImg = findViewById(R.id.handImg)
        handImg.setImageResource(handImgs[0])
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
        handImg.setOnTouchListener(object : OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                /*touchListener只处理举手逻辑*/
                if (session.curCoVideoState == DisCoVideo) {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            /*开启倒计时任务*/
                            countDownImg.visibility = View.VISIBLE
                            countDownTimer.start()
                        }
                        MotionEvent.ACTION_UP -> {
                            /*抬起时，倒计时任务强制结束*/
                            if (countDownImg.visibility != View.INVISIBLE) {
                                countDownTimer.cancel()
                            }
                        }
                        else -> {
                        }
                    }
                }
                return false
            }
        })
        handImg.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                /*clickListener只处理取消连麦逻辑*/
                if (session.curCoVideoState == CoVideoing) {
                    cancelCoVideo()
                }
            }
        })
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
        if (session.autoCoVideo) {
            /*允许举手即上台，直接回调允许上台接口*/
            coVideoListener?.onCoVideoAccepted()
            return
        }
        session.applyCoVideo(object : EduCallback<Unit> {
            override fun onSuccess(res: Unit?) {
                coVideoListener?.onApplyCoVideoComplete()
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
        session.onLinkMediaChanged(agree)
        handImg.setBackgroundResource(if (session.isCoVideoing()) handImgs[1] else handImgs[0])
    }

    fun abortCoVideoing() {
        if (session.abortCoVideoing()) {
            coVideoListener?.onCoVideoAborted()
        }
    }

    /**同步连麦状态;同步action消息过来的状态(包括accept和reject)*/
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

    /**同步举手即上台的开关状态*/
    fun syncAutoCoVideoState(enable: Boolean) {
        session.autoCoVideo = enable
    }

    fun destroy() {
        session.clear()
        if (animation != null) {
            animation.cancel()
            animation = null
        }
    }
}