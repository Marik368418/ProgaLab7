package transfers;

import java.io.Serializable;
import java.util.Objects;

public class AuthenticationData implements Serializable {
    private String username;
    private String password;

    public AuthenticationData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationData data = (AuthenticationData) o;
        return Objects.equals(username, data.username) && Objects.equals(password, data.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}