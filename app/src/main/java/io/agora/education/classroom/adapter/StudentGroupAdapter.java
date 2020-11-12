package io.agora.education.classroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.education.R;
import io.agora.education.classroom.bean.group.GroupInfo;

public class StudentGroupAdapter extends RecyclerView.Adapter<StudentGroupAdapter.ViewHolder> {

    private List<GroupInfo> groupInfoList = new ArrayList<>();
    private final int layoutId = R.layout.item_studentgroup_layout;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupInfo groupInfo = groupInfoList.get(position);
        holder.groupNameTextView.setText(groupInfo.getGroupName());
    }

    @Override
    public int getItemCount() {
        return groupInfoList.size();
    }

    public void updateGroupList(List<GroupInfo> groupInfos) {
        this.groupInfoList = groupInfos;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.reward_ImageView)
        AppCompatImageView rewardImageView;
        @BindView(R.id.coVideoing_ImageView)
        AppCompatImageView coVideoingImageView;
        @BindView(R.id.audio_ImageView)
        AppCompatImageView audioImageView;
        @BindView(R.id.groupName_TextView)
        AppCompatTextView groupNameTextView;
        @BindView(R.id.coVideoing)
        AppCompatTextView coVideoing;
        @BindView(R.id.members_RecyclerView)
        RecyclerView membersRecyclerView;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
