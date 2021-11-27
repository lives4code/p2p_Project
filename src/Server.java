import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


//handler thread class. handlers are spawned from the listening
//loop and are responsible for dealing with a single client's
//requests
public class Server extends Thread {

    private byte[] message;            //message received from the client
    private Socket connection;        //wait on a connection from client
    private int no;                //The index number of the client
    private DataInputStream in;    //stream read from the socket
    private DataOutputStream out;    //stream write to the socket
    private int myId;
    private int clientId;
    Timer timer;

    //debug
    private String s;

    public Server(Socket connection, int no, int myId) {
        this.connection = connection;
        this.no = no;
        this.myId = myId;
    }

    public void run() {
        try {
            //initialize Input and Output streams
            // initialize Random
            Random rnd = new Random(System.currentTimeMillis());
            rnd.setSeed(System.currentTimeMillis());
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            // start timer
            //startDeterminingNeighbors();

            try {

                //receive handshake and validate
                System.out.println("SERVER " + myId + ": reading handshake from peer");
                byte[] handshake = new byte[32];
                in.read(handshake, 0, 32);

                //validate handshake
                try {
                    clientId = MessageHandler.validateHandshake(handshake, myId);
                    System.out.println("SERVER " + myId + ": handshake read from peer. ClientID: " + clientId);
                } catch (Exception e) {
                    currentThread().interrupt();
                    System.out.println("SERVER: " + myId + " handshake invalid:" + e.getLocalizedMessage());

                }
                //yeah this is copy and pasted code from client.java but I can't use a method because
                //passing an inputstream causes a nullpointer exception.
                byte[] msg;
                byte[] sizeB = new byte[4];
                int type = -1;
                int size;
                long cost;
                long start;

                //System.out.println("SERVER " + myId + " DEBUG 1");
                s = "SERVER " + myId + " recived msg: ";
                //listener loop.
                while (true) {

                    if (in.available() > 0) {
                        //handle incoming messages
                        System.out.println("SERVER " + myId + ": beginning new loop iteration");
                        start = System.currentTimeMillis();
                        in.read(sizeB);
                        size = ByteBuffer.wrap(sizeB).getInt();
                        msg = new byte[size];
                        type = in.read();
                        int sizes = in.read(msg);
                        cost = System.currentTimeMillis() - start;
                        message = MessageHandler.handleMessage(msg, type, clientId, myId, 'S');
                        //if the message handler returns an interested or uninterested message then send it.
                        if (message != null && (message[4] == 2 || message[4] == 3 || message[4] == 7)) {
                            System.out.println("sending message");
                            MessageHandler.sendMessage(out, message);
                        }
                        //MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).downloadRate = size / cost; // bytes per ms

                        //request Pieces!
                        /*
                        for(Peer peer :MyProcess.peers){
                            if(!peer.getIsChoked()){
                                if(peer.getIsInterested()){
                                    System.out.println("we have a peer that is interestedn and unchoked");
                                    msg = MessageHandler.createRequestMessage(MessageHandler.getRandomPiece(peer.bitField, MyProcess.bitField));
                                    MessageHandler.sendMessage(out, msg);
                                }
                            }
                        }

                         */
                    }
                }

            } catch (Exception exception) {
                System.out.println(exception.getMessage());
                System.err.println("SERVER: Data received in unknown format");
                exception.printStackTrace();
            }
        } catch (IOException ioException) {
            System.out.println("Disconnect with Client " + no);
        } finally {
            //Close connections
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                connection.close();
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            }
        }
    }





}

