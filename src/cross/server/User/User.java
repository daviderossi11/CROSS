package cross.server.User;


/*
 * User class una classe che rappresenta un utente
 * @param username: nome utente
 * @param password: password utente
 */

public class User{

    public String username;
    public String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
}
