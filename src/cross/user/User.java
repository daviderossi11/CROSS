package cross.user;


public class User{
    private final int userId;
    private final String username;
    private String password;

    public User(int userId, String username, String password){
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    public int getUserId(){
        return userId;
    }

    public String getUsername(){
        return username;
    }

    public boolean checkPassword(String password){
        return this.password.equals(password);
    }

    public void changePassword(String password){
        this.password = password;
    }



}