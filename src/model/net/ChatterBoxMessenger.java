package model.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ChatterBoxMessenger {
    private final DataOutputStream connectionOutput;
    private final DataInputStream connectionInput;

    public ChatterBoxMessenger(Socket connection) throws IOException{
        connectionInput = new DataInputStream(connection.getInputStream());
        connectionOutput = new DataOutputStream(connection.getOutputStream());
    }

    public void sendMessage(String message) throws IOException{
        connectionOutput.writeUTF(message);
    }

    public String receiveMessage() throws IOException{
        return connectionInput.readUTF();
    }

    public void close() throws IOException{
        connectionInput.close();
        connectionOutput.close();
    }
}
