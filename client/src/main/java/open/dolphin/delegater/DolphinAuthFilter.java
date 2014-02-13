package open.dolphin.delegater;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

/**
 * DolphinAuthFilter
 *
 * @author masuda, Masuda Naika
 */
public class DolphinAuthFilter implements ClientRequestFilter {

    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";

    private String userName;
    private String password;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        ctx.getHeaders().add(USER_NAME, userName);
        ctx.getHeaders().add(PASSWORD, password);
    }
}
