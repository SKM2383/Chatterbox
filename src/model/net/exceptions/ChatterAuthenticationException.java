package model.net.exceptions;

public class ChatterAuthenticationException extends Exception{
    public ChatterAuthenticationException(){
        super();
    }

    public ChatterAuthenticationException(String message){ super(message); }
}
