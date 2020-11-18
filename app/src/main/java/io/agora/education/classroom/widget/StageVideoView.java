package io.agora.education.classroom.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.education.R;

public class StageVideoView extends ConstraintLayout {

    @BindView(R.id.tv_name)
    protected TextView tv_name;
    @BindView(R.id.ic_audio)
    protected RtcAudioView ic_audio;
    @Nullable
    @BindView(R.id.ic_video)
    protected ImageView ic_video;
    @BindView(R.id.layout_place_holder)
    protected FrameLayout layout_place_holder;
    @BindView(R.id.layout_video)
    protected FrameLayout layout_video;
    @BindView(R.id.rewardAnim_ImageView)
    protected ImageView rewardAnimImageView;

    public StageVideoView(Context context) {
        super(context);
        init(context);
    }

    public StageVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StageVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        inflate(context, R.layout.layout_video_stage, this);
        ButterKnife.bind(this);
    }

    public void setViewVisibility(int visibility) {
        ((Activity) getContext()).runOnUiThread(() -> setVisibility(visibility));
    }

    public void setName(String name) {
        ((Activity) getContext()).runOnUiThread(() -> tv_name.setText(name));
    }

    public void muteAudio(boolean muted) {
        ((Activity) getContext()).runOnUiThread(() -> {
            ic_audio.setState(muted ? RtcAudioView.State.CLOSED : RtcAudioView.State.OPENED);
        });

    }

    public boolean isAudioMuted() {
        return ic_audio.getState() == RtcAudioView.State.CLOSED;
    }

    public void muteVideo(boolean muted) {
        ((Activity) getContext()).runOnUiThread(() -> {
            if (ic_video != null) {
                ic_video.setSelected(!muted);
            }
            layout_video.setVisibility(muted ? GONE : VISIBLE);
            layout_place_holder.setVisibility(muted ? VISIBLE : GONE);
            Log.e("RtcVideoView", "muteVideo：" + muted);
        });
    }

    public boolean isVideoMuted() {
        if (ic_video != null) {
            return !ic_video.isSelected();
        }
        return true;
    }

    public FrameLayout getVideoLayout() {
        return layout_video;
    }

    public TextView getTv_name() {
        return tv_name;
    }

    //    public SurfaceView getSurfaceView() {
//        if (layout_video.getChildCount() > 0) {
//            return (SurfaceView) layout_video.getChildAt(0);
//        }
//        return null;
//    }
//
//    public void setSurfaceView(SurfaceView surfaceView) {
//        layout_video.removeAllViews();
//        if (surfaceView != null) {
//            layout_video.addView(surfaceView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
//        }
//    }

    public void setOnClickAudioListener(OnClickListener listener) {
        ic_audio.setOnClickListener(listener);
    }

    public void setOnClickVideoListener(OnClickListener listener) {
        if (ic_video != null) {
            ic_video.setOnClickListener(listener);
        }
    }

//    public void showLocal() {
//        VideoMediator.setupLocalVideo(this);
//    }
//
//    public void showRemote(int uid) {
//        VideoMediator.setupRemoteVideo(this, uid);
//    }

    /**
     * 显示奖励动画
     */
    public void showRewardAnim() {
        rewardAnimImageView.setVisibility(VISIBLE);
        Glide.with(getContext()).asGif().skipMemoryCache(true)
                .load(R.drawable.img_reward_anim).listener(new RequestListener<GifDrawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                        Target<GifDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GifDrawable resource, Object model,
                                           Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                resource.setLoopCount(1);
                resource.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                    public void onAnimationEnd(Drawable drawable) {
                        rewardAnimImageView.setVisibility(GONE);
                    }
                });
                return false;
            }
        }).into(rewardAnimImageView);
    }

}
