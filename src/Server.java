import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//handler thread class. handlers are spawned from the listening
//loop and are responsible for dealing with a single client's
//requests
public class Server extends Thread {

    private byte[] message;            //message received from the client
    private Socket connection;        //wait on a connection from client
    private int no;                //The index number of the client
    private DataInputStream in;	//stream read from the socket
    private DataOutputStream out;    //stream write to the socket
    private int peerId = 1000;

    private int test;

    public Server(Socket connection, int no) {
        this.connection = connection;
        this.no = no;

        //debug
    }

    public void run() {
        try {
            //initialize Input and Output streams
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            try {
                //preform handshake here to validate connection
                //preform handshake here to validate connection
                System.out.println("SERVER: Creating and sending handshake to peer");
                message = MessageHandler.createHandshake(peerId);
                MessageHandler.sendMessage(out, message);
                System.out.println("SERVER: sent handshake to peer");

                //receive handshake and validate
                System.out.println("SERVER: reading handshake from peer");
                message = MessageHandler.receiveMessage(in, message);
                System.out.println("SERVER: handshake read from peer");

                //validate handshake
                test = MessageHandler.validateHandshake(message);
                System.out.println("SERVER: validation result: " + test);


                while (true) {
                    //receive the message sent from the client
                    in.read(message);
                    //debug message to user
                    System.out.println("Receive message: " + message + " from client " + no);
                    //this is where we will handle the message
                    handleMessage(message);
                    //not needed
//                    //test message
//                    sendMessage("received");
                }
            } catch (Exception exception) {
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

<<<<<<< HEAD
    //handle message
=======
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
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++){
            b[i] = msg[i + 27];
        }
        this.peerId = ByteBuffer.wrap(b).getInt();;
        return true;
    }

>>>>>>> ae547ed6e9f50f6dbc4b61216de347f9ee8f89ac
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

    private void choke() {
        byte[] bytes = new byte[5];
        byte[] messageLength = ByteBuffer.allocate(4).putInt(0).array();
        for ( int i = 0; i < 4; i++){
            bytes[i] = messageLength[i];
        }
        bytes[4] = 0;
        MessageHandler.sendMessage(out, bytes);
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
