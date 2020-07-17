package io.agora.education.impl.user.data.request

import io.agora.education.api.stream.data.VideoSourceType


/**@param streamUuid 同一appId下流的唯一id，rtc中uid
 * @param isPublish 1发布 0不发布*/
class EduPublishStreamReq constructor(var userId: String,
                                      var streamUuid: String,
                                      var streamName: String,
                                      var videoSourceType: VideoSourceType,
                                      var hasVideo: Int, var hasAudio: Int) {

    var audioSourceType: Int = 1

}