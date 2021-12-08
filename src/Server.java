import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Logger;

public class Server extends Thread {

    private byte[] message;
    private Socket connection;
    private int no;
    private DataInputStream in;
    private DataOutputStream out;
    private int myId;
    private int clientId;

    //debug
    private String s;
    private Logger log;

    public Server(Socket connection, int no, int myId, Logger log) {
        this.connection = connection;
        this.no = no;
        this.myId = myId;
        this.log = log;
    }

    public void run() {
        try {
            //initialize Input and Output streams
            Random rnd = new Random(System.currentTimeMillis());
            rnd.setSeed(System.currentTimeMillis());
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            try {
                //receive handshake and validate
                System.out.println("SERVER " + myId + ": reading handshake from peer");
                byte[] handshake = new byte[32];
                in.read(handshake, 0, 32);
                try {
                    clientId = MessageHandler.validateHandshake(handshake);
                    System.out.println("SERVER " + myId + ": handshake read from peer. ClientID: " + clientId);
                    log.info("Peer " + myId + " is connected from Peer " + clientId + ".");
                } catch (Exception e) {
                    currentThread().interrupt();
                    System.out.println("SERVER: " + myId + " handshake invalid:" + e.getLocalizedMessage());

                }

                // variables
                byte[] msg;
                byte[] sizeB = new byte[4];
                int type = -1;
                int size;
                long cost;
                long start;

                s = "SERVER " + myId + " received msg: ";
                //listener loop.
                mainloop:
                while (true) {
                    System.out.println("SERVER " + myId + ": connected to " + clientId + ": beginning new loop iteration");

                    if (in.available() > 0) {
                        //handle incoming messages
                        System.out.println("SERVER " + myId + ": new input");
                        // calculate download rate
                        start = System.nanoTime();
                        in.read(sizeB);
                        size = ByteBuffer.wrap(sizeB).getInt();
                        msg = new byte[size];
                        type = in.read();
                        in.read(msg);
                        cost = System.nanoTime() - start;
                        if (type == 7) { // record download rate if receiving a piece
                            MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).downloadRate = cost != 0 ? (double)size / (double)cost : size / 0.0000001; // bytes per ms
                            System.out.println("SERVER " + myId + ": new rate: " + MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).downloadRate);
                        }

                        // handle messages
                        message = MessageHandler.handleMessage(msg, type, clientId, myId, 'S', log);
                        if (message != null && (message[4] == 2 || message[4] == 3 || message[4] == 7 || message[4] == 6)) {
                            System.out.println("SERVER " + myId + ": sending type " + message[4] + " to " + clientId);
                            MessageHandler.sendMessage(out, message);
                        }
                    }

                    // check if have complete bitfield
                    for(int i = 0; i < MyProcess.bitField.size(); i++){
                        if(!MyProcess.bitField.get(i)){
                            System.out.println("SERVER END " + myId + ": not done witn Client: " + clientId);
                            continue mainloop;
                        }
                    }

                    // tell peer we're done
                    if(!MyProcess.done) {
                        log.info("Peer " + myId + " has downloaded the complete file.");
                    }
                    MyProcess.done = true;
                    MyProcess.checkDone = true;

                    MessageHandler.sendMessage(out, MessageHandler.createMsg(8,new byte[]{}));

                    String s = "SERVER END " + myId + ": connected to " + clientId + " download complete. print bitfield: ";
                    for (int i = 0; i < MyProcess.bitField.size(); i ++){
                        s += MyProcess.bitField.get(i) + ", ";
                    }
                    System.out.println(s);
                    return;
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

