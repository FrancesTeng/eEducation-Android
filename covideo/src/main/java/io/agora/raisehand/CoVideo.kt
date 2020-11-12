package io.agora.raisehand

import java.io.Serializable

object CoVideoState {
    /**
     * 初始状态
     */
    const val DisCoVideo = 0

    /**
     * 申请中
     */
    const val Applying = 1

    /**
     * 连麦中
     */
    const val CoVideoing = 2
}

object CoVideoActionType {
    /**
     * student apply co-video
     */
    var APPLY = 1

    /**
     * teacher accept apply
     */
    var ACCEPT = 3

    /**
     * teacher reject apply
     */
    var REJECT = 4

    /**
     * student cancel apply
     */
    var CANCEL = 5

    /**
     * teacher abort co-video
     */
    var ABORT = 6

    /**
     * student exit co-video
     */
    var EXIT = 7
}

internal class CoVideoPeerMsg(
        val cmd: Int,
        val data: Any
) : Serializable {

    companion object {
        const val COVIDEOCMD: Int = 1
    }
}

internal class CoVideoMsg(
        val type: Int,
        val userUuid: String,
        val userName: String
) {
}