package io.agora.education.classroom.adapter;

import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.education.R;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.classroom.bean.group.GroupInfo;
import io.agora.education.classroom.bean.group.GroupMemberInfo;

public class StudentGroupAdapter extends RecyclerView.Adapter<StudentGroupAdapter.ViewHolder> {

    private List<GroupInfo> groupInfoList = new ArrayList<>();
    private List<EduUserInfo> allStudentList = new ArrayList<>();
    private final int layoutId = R.layout.item_studentgroup_layout;
    private int scrollViewWidth;

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
        List<GroupMemberInfo> curGroupMembers = new ArrayList<>();
        for (EduUserInfo userInfo : allStudentList) {
            if (groupInfo.getMembers().contains(userInfo.getUserUuid())) {
                curGroupMembers.add(new GroupMemberInfo(userInfo, 0));
            }
        }
        GroupMemberAdapter memberAdapter = new GroupMemberAdapter(curGroupMembers);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(holder.itemView.getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        holder.membersRecyclerView.setLayoutManager(linearLayoutManager);
        holder.membersRecyclerView.addItemDecoration(new AdapterDecoration(18));
        holder.membersRecyclerView.setAdapter(memberAdapter);
        holder.membersRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                holder.leftMargin += dx;
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.scrollView.getLayoutParams();
                layoutParams.leftMargin = holder.leftMargin;
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == getItemCount() - 1) {
                    layoutParams.rightMargin = 0;
                } else if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    holder.leftMargin = 0;
                    layoutParams.leftMargin = holder.leftMargin;
                }
                layoutParams.width = scrollViewWidth;
                holder.scrollView.setLayoutParams(layoutParams);
            }
        });
        holder.scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scrollViewWidth = holder.scrollView.getRight() - holder.scrollView.getLeft();
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return groupInfoList.size();
    }

    public void updateGroupList(List<GroupInfo> groupInfos, List<EduUserInfo> allStudents) {
        this.groupInfoList = groupInfos;
        this.allStudentList = allStudents;
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
        @BindView(R.id.view0)
        View view0;
        @BindView(R.id.members_RecyclerView)
        RecyclerView membersRecyclerView;
        @BindView(R.id.scrollView)
        View scrollView;

        private int leftMargin;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private class AdapterDecoration extends RecyclerView.ItemDecoration {
        private int left;

        public AdapterDecoration(int left) {
            this.left = left;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.left = left;
            }
            outRect.top = 0;
            outRect.right = 0;
            outRect.bottom = 0;
        }
    }
}
