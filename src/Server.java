import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;

//handler thread class. handlers are spawned from the listening
//loop and are responsible for dealing with a single client's
//requests
public class Server extends Thread {

    private byte[] message;            //message received from the client
    private Socket connection;        //wait on a connection from client
    private int no;                //The index number of the client
    private DataInputStream in;    //stream read from the socket
    private DataOutputStream out;    //stream write to the socket
    private int peerId;
    int clientId;

    //debug
    private String s;

    public Server(Socket connection, int no, int peerId) {
        this.connection = connection;
        this.no = no;
        this.peerId = peerId;
    }

    public void run() {
        try {
            //initialize Input and Output streams
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            try {
                //perform handshake here to validate connection
                System.out.println("SERVER " + peerId + ": Creating and sending handshake to peer with ID: " + peerId);
                message = MessageHandler.createHandshake(peerId);
                MessageHandler.sendMessage(out, message);
                System.out.println("SERVER " + peerId + ": sent handshake to peer");

                //receive handshake and validate
                System.out.println("SERVER " + peerId + ": reading handshake from peer");
                MessageHandler.receiveHandshake(in, message);
                System.out.println("SERVER " + peerId + ": handshake read from peer");

                //validate handshake
                try{
                    clientId = MessageHandler.validateHandshake(message, peerId);
                }
                catch (Exception e){
                    currentThread().interrupt();
                    System.out.println("handshake invalid:" + e.getLocalizedMessage());

                }

                //receive bitfield
                message = MessageHandler.handleMessage(in);
                int index = MyProcess.getPeerIndexById(clientId);
                MyProcess.peers.get(index).bitField = BitSet.valueOf(message);
                s = "SERVER " + peerId + " bitfield msg DEBUG: ";
                printBitfield(message);

                // Interested or Not Interested
                boolean interested = checkForInterest(BitSet.valueOf(message), MyProcess.bitField);
                if (interested)
                    message = MessageHandler.createMsg(2, new byte[]{});
                else
                    message = MessageHandler.createMsg(3, new byte[] {});
                MessageHandler.sendMessage(out, message);

                while (true) {
                    MessageHandler.handleMessage(in);
                }
            } catch (Exception exception) {
                System.err.println("Data received in unknown format");
                exception.printStackTrace();
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

    private boolean checkForInterest(BitSet received, BitSet mine) {
        System.out.println();
        boolean interested = false;
        received.xor(mine);
        for (int i = 0; i < received.length(); i++) {
            if (received.get(i) == true) {
                interested = true;
                break;
            }
        }
        return interested;
    }

    private void printBitfield(BitSet bits) {
        byte[] bytes = bits.toByteArray();
        s = "";
        for (byte b : bytes) {
            s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
        }
        System.out.println(s);
    }

    private void printBitfield(byte[] bytes) {
        s = "";
        for (byte b : bytes) {
            s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
        }
        System.out.println(s);
    }
}

