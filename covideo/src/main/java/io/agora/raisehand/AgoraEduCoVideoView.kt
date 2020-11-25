package io.agora.raisehand

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.base.ToastManager
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.message.EduActionMessage
import io.agora.education.api.message.EduActionType
import io.agora.education.api.room.EduRoom
import kotlinx.android.synthetic.main.view_covideo_layout.view.*

/**
 * 举手组件布局
 * */
class AgoraEduCoVideoView : LinearLayout {

    private lateinit var countdownLayout: RelativeLayout
    private lateinit var countDownTextView: AppCompatTextView
    private lateinit var handImg: AppCompatImageView

    private lateinit var session: StudentCoVideoSession
    private var initialized = false
    private var countDownTexts: Array<String> = arrayOf("4", "3", "2", "1")
    private var handImgs: Array<Int> = arrayOf(R.drawable.ic_hand_up, R.drawable.ic_hand_down)
    private var coVideoListener: AgoraEduCoVideoListener? = null

    /*举手倒计时*/
    private var coVideoCountDownTimer: CountDownTimer = object : CountDownTimer(1000 * 3, 1000) {
        override fun onFinish() {
            countdownLayout.visibility = View.INVISIBLE
            countDownTextView.setText(countDownTexts[0])
            /*倒计时结束，发起举手申请*/
            applyCoVideo()
            operaAlphaAnimation(false)
        }

        override fun onTick(millisUntilFinished: Long) {
            val index: Int = (3 - (millisUntilFinished / 1000)).toInt()
            countDownTextView.setText(countDownTexts[index])
        }

    }

    /*取消举手倒计时*/
    private var cancelCountDownTimer: CountDownTimer = object : CountDownTimer(1000 * 3, 1000) {
        override fun onFinish() {
            countdownLayout.visibility = View.INVISIBLE
            countDownTextView.setText(countDownTexts[0])
            /*倒计时结束，取消举手*/
            cancelCoVideo()
            operaAlphaAnimation(false)
        }

        override fun onTick(millisUntilFinished: Long) {
            val index: Int = (3 - (millisUntilFinished / 1000)).toInt()
            countDownTextView.setText(countDownTexts[index])
        }

    }
    private var countDownAlphaAnimation: AlphaAnimation? = null

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
        countdownLayout = findViewById(R.id.countdown_Layout)
        countDownTextView = findViewById(R.id.countDown_TextView)
        handImg = findViewById(R.id.handImg)
        handImg.setImageResource(handImgs[0])
        countdownLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                var width = countdownLayout.right - countdownLayout.left
                var height = countdownLayout.bottom - countdownLayout.top
                width /= 5
                height /= 5
                countDownTextView.textSize = (width + height) / 2.0f
                countdownLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    fun init(eduRoom: EduRoom) {
        if (!initialized) {
            session = StudentCoVideoHelper(context, eduRoom)
            /*检查老师是否打开举手开关*/
            visibility = if (session.enableCoVideo) View.VISIBLE else View.GONE
            if (context is AgoraEduCoVideoListener) {
                coVideoListener = context as AgoraEduCoVideoListener
            }
            operaAlphaAnimation(false)
            handImg.setOnTouchListener(object : OnTouchListener {
                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    /*检查当前状态下，是否允许举手*/
                    session.isAllowCoVideo(object : EduCallback<Unit> {
                        override fun onSuccess(res: Unit?) {
                            when (event?.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    /**/
                                    if (countdownLayout.visibility == View.INVISIBLE) {
                                        countdownLayout.visibility = View.VISIBLE
                                        operaAlphaAnimation(true)
                                        /*举手倒计时任务开启*/
                                        coVideoCountDownTimer.start()
                                    } else {
                                        operaAlphaAnimation(false)
                                        cancelCountDownTimer.cancel()
                                        countdownLayout.visibility = View.INVISIBLE
                                        countDownTextView.setText(countDownTexts[0])
                                    }
                                }
                                MotionEvent.ACTION_UP -> {
                                    if (countdownLayout.visibility == View.INVISIBLE) {
                                        /*松开之后如果举手倒计时结束，那么在未开启举手即上台和老师未同意上台的情况下，
                                        开始取消举手的倒计时*/
                                        if (session.autoCoVideo || !session.isCoVideoing()) {
                                            countdownLayout.visibility = View.VISIBLE
                                            operaAlphaAnimation(true)
                                            cancelCountDownTimer.start()
                                        }
                                    } else {
                                        /*松开之后如果举手倒计时没有结束，那么直接停止举手倒计时即可*/
                                        operaAlphaAnimation(false)
                                        coVideoCountDownTimer.cancel()
                                        countdownLayout.visibility = View.INVISIBLE
                                        countDownTextView.setText(countDownTexts[0])
                                    }
                                }
                                else -> {
                                }
                            }
                        }

                        override fun onFailure(error: EduError) {
                            ToastManager.showShort(error.msg)
                        }
                    })
                    return true
                }
            })
            initialized = true
        }
    }

    private fun operaAlphaAnimation(enable: Boolean) {
        if (countDownAlphaAnimation == null) {
            countDownAlphaAnimation = AlphaAnimation(1.0f, 0.3f)
            countDownAlphaAnimation?.repeatMode = Animation.REVERSE
            countDownAlphaAnimation?.repeatCount = Animation.INFINITE
            countDownAlphaAnimation?.duration = 300
            countDownTextView.animation = countDownAlphaAnimation
        }
        if (enable) {
            countDownAlphaAnimation?.startNow()
        } else {
            countDownAlphaAnimation?.cancel()
        }
    }

    /**申请连麦*/
    private fun applyCoVideo() {
        if (session.autoCoVideo) {
            session.onLinkMediaChanged(true)
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
     * 老师处理前主动取消*/
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
     * @param onStage 举手(连麦)请求是否被允许*/
    fun onLinkMediaChanged(onStage: Boolean) {
        session.onLinkMediaChanged(onStage)
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

    /**同步举手的开关状态和举手即上台的开关状态*/
    fun syncCoVideoSwitchState(properties: MutableMap<String, Any>?) {
        session.syncCoVideoSwitchState(properties)
        post {
            /*检查老师是否打开举手开关*/
            visibility = if (session.enableCoVideo) View.VISIBLE else View.GONE
        }
    }

    fun destroy() {
        session.clear()
        countDownAlphaAnimation?.let {
            countDownAlphaAnimation?.cancel()
            countDownAlphaAnimation = null
        }
    }
}