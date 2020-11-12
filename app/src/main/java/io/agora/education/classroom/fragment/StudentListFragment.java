package io.agora.education.classroom.fragment;

import android.text.TextUtils;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import io.agora.education.R;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.base.BaseFragment;
import io.agora.education.classroom.adapter.StudentListAdapter;

public class StudentListFragment extends BaseFragment {
    private static final String TAG = StudentListFragment.class.getSimpleName();

    @BindView(R.id.rcv_students)
    RecyclerView rcvStudents;

    private String localUserUuid;
    private StudentListAdapter studentListAdapter;

    public StudentListFragment() {
    }

    public StudentListFragment(String localUserUuid) {
        this.localUserUuid = localUserUuid;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_studentlist_layout;
    }

    @Override
    protected void initData() {
        studentListAdapter = new StudentListAdapter();
    }

    @Override
    protected void initView() {
        rcvStudents.setAdapter(studentListAdapter);
    }

    public void setLocalUserUuid(String userUuid) {
        localUserUuid = userUuid;
    }

    public void setStudentList(List<EduUserInfo> studentList) {
        getActivity().runOnUiThread(() -> {
            /**本地用户始终在第一位*/
            if (!TextUtils.isEmpty(localUserUuid)) {
                for (int i = 0; i < studentList.size(); i++) {
                    EduUserInfo userInfo = studentList.get(i);
                    if (userInfo.getUserUuid().equals(localUserUuid)) {
                        if (i != 0) {
                            Collections.swap(studentList, 0, i);
                            break;
                        }
                    }
                }
            }
            getActivity().runOnUiThread(() -> {
                if (rcvStudents.isComputingLayout()) {
                    rcvStudents.postDelayed(() -> {
                        studentListAdapter.updateStudentList(studentList);
                    }, 300);
                } else {
                    rcvStudents.post(() -> studentListAdapter.updateStudentList(studentList));
                }
            });
        });
    }
}
