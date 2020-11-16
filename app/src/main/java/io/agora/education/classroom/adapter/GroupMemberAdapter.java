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
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.classroom.bean.group.GroupMemberInfo;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.ViewHolder> {

    private List<GroupMemberInfo> memberList = new ArrayList<>();
    private final int layoutId = R.layout.item_groupmember_layout;

    public GroupMemberAdapter(List<GroupMemberInfo> memberList) {
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupMemberInfo info = memberList.get(position);
        holder.memberNameTextView.setText(info.getUserInfo().getUserName());
        holder.integralTextView.setText(String.valueOf(info.getReward()));
        holder.memberNameTextView.setTextColor(holder.itemView.getResources().getColor(
                info.getOnline() ? R.color.gray_191919 : R.color.gray_CCCCCC));
        holder.integralImageView.setImageResource(
                info.getOnline() ? R.drawable.ic_integral_1 : R.drawable.ic_integral_0);
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.portrait_ImageView)
        AppCompatImageView portraitImageView;
        @BindView(R.id.memberName_TextView)
        AppCompatTextView memberNameTextView;
        @BindView(R.id.integral_ImageView)
        AppCompatImageView integralImageView;
        @BindView(R.id.integral_TextView)
        AppCompatTextView integralTextView;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
