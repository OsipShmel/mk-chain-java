package me.osipsmel.api;

public interface ViewAPI {
    public void showToken(String token);
    public void updateStatus(String msg);
    public void alertError(String error);
    public void clearOutput();
    
}
