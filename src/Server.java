import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//handler thread class. handlers are spawned from the listening
//loop and are responsible for dealing with a single client's
//requests
public class Server extends Thread {

    private String message;            //message received from the client
    private Socket connection;        //wait on a connection from client
    private int no;                //The index number of the client
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
    private int peerId;

    public Server(Socket connection, int no) {
        this.connection = connection;
        this.no = no;
    }

    public void run() {
        try {
            //initialize Input and Output streams
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());

            try {
                //preform handshake here to validate connection

                while (true) {
                    //receive the message sent from the client
                    message = (String) in.readObject();
                    //debug message to user
                    System.out.println("Receive message: " + message + " from client " + no);
                    //this is where we will handle the message
                    handleMessage(message.getBytes());
                    //not needed
//                    //test message
//                    sendMessage("received");
                }
            } catch (ClassNotFoundException classnot) {
                System.err.println("Data received in unknown format");
            }
        } catch (IOException ioException) {
            System.out.println("Disconnect with Client " + no);
        } finally {
            //Close connections
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            }
        }
    }

    //validate handshake
    private boolean validateHandshake(byte[] msg) {
        byte[] header = ("P2PFILESHARINGPROJ").getBytes();
        // Check for appropriate header
        if (!Arrays.equals(header, Arrays.copyOfRange(msg, 0, 18))) {
            return false;
        }
        // Check for zeros
        for (int i = 18; i < 28; i++) {
            if (msg[i] != 0x00)
                return false;
        }
        // Check for peer id
        this.peerId = (msg[28]*1000) + (msg[29]*100) + (msg[30]*10) + msg[31];
        return true;
    }

    //handle message
    private void handleMessage(byte[] msg) {
        byte[] msgLength  = new byte[4];
        System.arraycopy(msg, 0, msgLength, 4, 4);
        int mLength = ByteBuffer.wrap(msgLength).getInt();

        byte[] msgType = new byte[1];
        System.arraycopy(msg, 4, msgType, 5, 1);
        int mType = ByteBuffer.wrap(msgType).getInt();

        byte[] msgPayload = new byte[mLength];
        System.arraycopy(msg, 5, msgType, mLength + 5, mLength);

        switch(mType){
            case 0:
                //choke
            case 1:
                //unchoke
            case 2:
                //interested
            case 3:
                //not intrested
            case 4:
                //have
            case 5:
                //bitfield
            case 6:
                //request
            case 7:
                //piece
        }
    }

    //not used
//    //send a message to the output stream
//    private void sendMessage(String msg)
//    {
//        try{
//            //stream write the message
//            out.writeObject(msg);
//            out.flush();
//        }
//        catch(IOException ioException){
//            ioException.printStackTrace();
//        }
//    }
}
