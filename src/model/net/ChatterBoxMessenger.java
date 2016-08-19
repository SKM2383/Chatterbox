package model.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ChatterBoxMessenger {
    private final DataOutputStream OUTPUT;
    private final DataInputStream INPUT;

    public ChatterBoxMessenger(Socket connection) throws IOException{
        INPUT = new DataInputStream(connection.getInputStream());
        OUTPUT = new DataOutputStream(connection.getOutputStream());
    }

    public void sendMessage(String message) throws IOException{
        OUTPUT.writeUTF(message);
    }

    public String receiveMessage() throws IOException{
        return INPUT.readUTF();
    }

    public void close() throws IOException{
        INPUT.close();
        OUTPUT.close();
    }
}
