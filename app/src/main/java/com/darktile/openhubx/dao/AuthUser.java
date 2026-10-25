package com.darktile.openhubx.dao;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

/** Room entity mapped to table "AUTH_USER" (migrated from greenDAO). */
@Entity(tableName = "AUTH_USER")
public class AuthUser implements Parcelable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ACCESS_TOKEN")
    private String accessToken;

    @NonNull
    @ColumnInfo(name = "AUTH_TIME")
    private Date authTime;

    @ColumnInfo(name = "EXPIRE_IN")
    private int expireIn;

    @NonNull
    @ColumnInfo(name = "SCOPE")
    private String scope;

    @ColumnInfo(name = "SELECTED")
    private boolean selected;

    @NonNull
    @ColumnInfo(name = "LOGIN_ID")
    private String loginId;

    @ColumnInfo(name = "NAME")
    private String name;

    @ColumnInfo(name = "AVATAR")
    private String avatar;

    public AuthUser() {
    }

    @NonNull
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(@NonNull String accessToken) {
        this.accessToken = accessToken;
    }

    @NonNull
    public Date getAuthTime() {
        return authTime;
    }

    public void setAuthTime(@NonNull Date authTime) {
        this.authTime = authTime;
    }

    public int getExpireIn() {
        return expireIn;
    }

    public void setExpireIn(int expireIn) {
        this.expireIn = expireIn;
    }

    @NonNull
    public String getScope() {
        return scope;
    }

    public void setScope(@NonNull String scope) {
        this.scope = scope;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @NonNull
    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(@NonNull String loginId) {
        this.loginId = loginId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.accessToken);
        dest.writeLong(this.authTime != null ? this.authTime.getTime() : -1);
        dest.writeInt(this.expireIn);
        dest.writeString(this.scope);
        dest.writeByte(this.selected ? (byte) 1 : (byte) 0);
        dest.writeString(this.loginId);
        dest.writeString(this.name);
        dest.writeString(this.avatar);
    }

    @Ignore
    protected AuthUser(Parcel in) {
        this.accessToken = in.readString();
        long tmpAuthTime = in.readLong();
        this.authTime = tmpAuthTime == -1 ? null : new Date(tmpAuthTime);
        this.expireIn = in.readInt();
        this.scope = in.readString();
        this.selected = in.readByte() != 0;
        this.loginId = in.readString();
        this.name = in.readString();
        this.avatar = in.readString();
    }

    public static final Parcelable.Creator<AuthUser> CREATOR = new Parcelable.Creator<AuthUser>() {
        @Override
        public AuthUser createFromParcel(Parcel source) {
            return new AuthUser(source);
        }

        @Override
        public AuthUser[] newArray(int size) {
            return new AuthUser[size];
        }
    };
}
