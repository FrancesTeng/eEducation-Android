package io.agora.education.classroom.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import java.util.Iterator;
import java.util.List;

import io.agora.education.R;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.VideoSourceType;
import io.agora.education.api.user.EduUser;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.base.BaseActivity;
import io.agora.education.classroom.BaseClassActivity;
import io.agora.education.classroom.bean.channel.User;
import io.agora.education.classroom.mediator.VideoMediator;
import io.agora.education.classroom.widget.RtcVideoView;

public class ClassVideoAdapter extends BaseQuickAdapter<EduStreamInfo, ClassVideoAdapter.ViewHolder> {

    public ClassVideoAdapter() {
        super(0);
        setDiffCallback(new DiffUtil.ItemCallback<EduStreamInfo>() {
            @Override
            public boolean areItemsTheSame(@NonNull EduStreamInfo oldItem, @NonNull EduStreamInfo newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull EduStreamInfo oldItem, @NonNull EduStreamInfo newItem) {
                return oldItem.getHasVideo() == newItem.getHasVideo()
                        && oldItem.getHasAudio() == newItem.getHasAudio()
                        && oldItem.getStreamUuid().equals(newItem.getStreamUuid());
            }

            @Nullable
            @Override
            public Object getChangePayload(@NonNull EduStreamInfo oldItem, @NonNull EduStreamInfo newItem) {
                if (oldItem.getHasVideo() == newItem.getHasVideo()
                        || oldItem.getHasAudio() == newItem.getHasAudio()
                        || oldItem.getStreamUuid().equals(newItem.getStreamUuid())) {
                    return true;
                } else {
                    return null;
                }
            }
        });
    }

    @NonNull
    @Override
    protected ViewHolder onCreateDefViewHolder(@NonNull ViewGroup parent, int viewType) {
        RtcVideoView item = new RtcVideoView(getContext());
        item.init(R.layout.layout_video_small_class, false);
        int width = getContext().getResources().getDimensionPixelSize(R.dimen.dp_95);
        int height = parent.getHeight() - parent.getPaddingTop() - parent.getPaddingBottom();
        item.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        return new ViewHolder(item);
    }

    @Override
    protected void convert(@NonNull ViewHolder viewHolder, EduStreamInfo item, @NonNull List<?> payloads) {
        super.convert(viewHolder, item, payloads);
        if (payloads.size() > 0) {
            viewHolder.convert(item);
        }
    }

    @Override
    protected void convert(@NonNull ViewHolder viewHolder, EduStreamInfo item) {
        viewHolder.convert(item);
        ((BaseClassActivity) viewHolder.view.getContext()).renderStream(item,
                viewHolder.view.getVideoLayout());
    }

    public void setNewList(@Nullable List<EduStreamInfo> data) {
        /**过滤掉非Camera的流*/
        Iterator<EduStreamInfo> streamInfoIterator = data.iterator();
        while (streamInfoIterator.hasNext()) {
            if(!streamInfoIterator.next().getVideoSourceType().equals(VideoSourceType.CAMERA)) {
                streamInfoIterator.remove();
            }
        }
        setDiffNewData(data);
    }

    static class ViewHolder extends BaseViewHolder {
        private RtcVideoView view;

        ViewHolder(RtcVideoView view) {
            super(view);
            this.view = view;
        }

        void convert(EduStreamInfo item) {
            view.muteVideo(!item.getHasVideo());
            view.muteAudio(!item.getHasAudio());
            view.setName(item.getPublisher().getUserName());
        }
    }

}
