package io.agora.education.classroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.education.R;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.classroom.bean.group.GroupMemberInfo;

public class StudentListAdapter extends RecyclerView.Adapter<StudentListAdapter.ViewHolder> {
    private static final String TAG = StudentListAdapter.class.getSimpleName();
    private final int layoutId = R.layout.item_student_layout;
    private List<GroupMemberInfo> students = new ArrayList<>();

    public StudentListAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupMemberInfo memberInfo = students.get(position);
        holder.textView.setText(memberInfo.getUserName());
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public void updateStudentList(List<GroupMemberInfo> students) {
        this.students = students;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.textView)
        AppCompatTextView textView;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
