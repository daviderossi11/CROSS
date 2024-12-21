package cross.user;

public class User{
    private final int userId;
    private string username;
    private string password;

    public User(int userId, string username, string password){
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    public int getUserId(){
        return userId;
    }

    public string getUsername(){
        return username;
    }

    public string checkPassword(string password){
        return this.password.equals(password);
    }

    public void changePassword(string password){
        this.password = password;
    }

    public void changeUsername(string username){
        this.username = username;
    }



}