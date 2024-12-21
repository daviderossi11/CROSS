package cross.user;

import java.util.*;
import java.util.concurrent.*;

public class UserManagement{
    private final ArrayList<User> users;
    private fina ArrayList<User> loggedInUsers;
    
    public UserManagement(){
        users = new ArrayList<User>();
        loggedInUsers = new ArrayList<User>();
    }

    public void addUser(User user){
        try{
            users.add(user);
        }catch(Exception e){
            System.out.println("Error: " + e);
        }
    }

    public int register(String username, String password){
        try{
            if(users.stream().anyMatch(user -> user.getUsername().equals(username))){
                return 101;
            }
            if(password.length() < 8){
                return 102;
            }
            if(username.length() < 4){
                return 103;
            }
            User user = new User(users.size(), username, password);
            users.add(user);
            return 100;
        }catch(Exception e){
            return 103;
        }

    }

    public int login(String username, String password){
        try{
            User user = users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
            if(user == null || !user.checkPassword(password)){
                return 101;
            }
            if(loggedInUsers.stream().anyMatch(u -> u.getUserId() == user.getUserId())){
                return 102;
            }
            loggedInUsers.add(user);
            return 100;
        }catch(Exception e){
            return 103;
        }
    }

    public int logout(int userId){
        try{
            User user = loggedInUsers.stream().filter(u -> u.getUserId() == userId).findFirst().orElse(null);
            if(user == null){
                return 101;
            }
            loggedInUsers.remove(user);
            return 100;
        }catch(Exception e){
            return 103;
        }
    }

    public int changePassword(int userId, String oldPassword, String newPassword){
        try{
            User user = loggedInUsers.stream().filter(u -> u.getUserId() == userId).findFirst().orElse(null);
            if(user == null || !user.checkPassword(oldPassword)){
                return 101;
            }
            if(newPassword.length() < 8 || user.getPassword().equals(newPassword)){
                return 103;
            }
            user.changePassword(newPassword);
            return 100;
        }catch(Exception e){
            return 104;
        }
    }



}