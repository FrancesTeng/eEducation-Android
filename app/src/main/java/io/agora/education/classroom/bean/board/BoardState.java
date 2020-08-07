package io.agora.education.classroom.bean.board;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class BoardState implements Parcelable {
    private int follow;
    private List<String> grantUsers;

    public BoardState() {
    }

    public BoardState(int follow, List<String> grantUsers) {
        this.follow = follow;
        this.grantUsers = grantUsers;
    }

    public int getFollow() {
        return follow;
    }

    public void setFollow(int follow) {
        this.follow = follow;
    }

    public List<String> getGrantUsers() {
        return grantUsers;
    }

    public void setGrantUsers(List<String> grantUsers) {
        this.grantUsers = grantUsers;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.follow);
        dest.writeStringList(this.grantUsers);
    }

    protected BoardState(Parcel in) {
        this.follow = in.readInt();
        this.grantUsers = in.createStringArrayList();
    }

    public static final Creator<BoardState> CREATOR = new Creator<BoardState>() {
        @Override
        public BoardState createFromParcel(Parcel source) {
            return new BoardState(source);
        }

        @Override
        public BoardState[] newArray(int size) {
            return new BoardState[size];
        }
    };
}
