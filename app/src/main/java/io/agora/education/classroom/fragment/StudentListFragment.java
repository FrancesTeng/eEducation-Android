package io.agora.education.classroom.fragment;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.base.BaseFragment;

public class StudentListFragment extends BaseFragment {
    private static final String TAG = StudentListFragment.class.getSimpleName();

    private String localUserUuid;

    @Override
    protected int getLayoutResId() {
        return 0;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

    }

    public void setLocalUserUuid(String userUuid) {
        localUserUuid = userUuid;
    }

    public void setStudentList(List<EduUserInfo> userList) {
        getActivity().runOnUiThread(() -> {
            /**过滤掉非学生和非摄像头流的user*/
            List<EduUserInfo> students = new ArrayList<>(userList.size());
            for (EduUserInfo userInfo : userList) {
                if (userInfo.getRole().equals(EduUserRole.STUDENT)) {
                    students.add(userInfo);
                }
            }
            /**本地用户始终在第一位*/
            if (!TextUtils.isEmpty(localUserUuid)) {
                for (int i = 0; i < students.size(); i++) {
                    EduUserInfo userInfo = students.get(i);
                    if (userInfo.getUserUuid().equals(localUserUuid)) {
                        if (i != 0) {
                            Collections.swap(students, 0, i);
                        }
                    }
                }
            }
        });
    }
}
