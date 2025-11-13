package server.demo.dto.auth;

import server.demo.dto.StoreDTO;

import java.util.List;

/**
 * 登录响应DTO
 */
public class LoginResponse {

    private String token;
    private UserDTO user;
    private List<StoreDTO> stores;

    public LoginResponse() {
    }

    public LoginResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    public LoginResponse(String token, UserDTO user, List<StoreDTO> stores) {
        this.token = token;
        this.user = user;
        this.stores = stores;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public List<StoreDTO> getStores() {
        return stores;
    }

    public void setStores(List<StoreDTO> stores) {
        this.stores = stores;
    }
}
