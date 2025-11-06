package server.demo.dto.auth;

import jakarta.validation.constraints.Size;

/**
 * 更新个人资料请求
 */
public class UpdateProfileRequest {

    @Size(max = 100, message = "昵称长度不能超过100个字符")
    private String nickname;

    private String gender;

    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatar;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
