import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Logger;


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
            // initialize Random
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

                //validate handshake
                try {
                    clientId = MessageHandler.validateHandshake(handshake, myId);
                    System.out.println("SERVER " + myId + ": handshake read from peer. ClientID: " + clientId);
                    log.info("Peer " + myId + " is connected from Peer " + clientId + ".");
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

                s = "SERVER " + myId + " received msg: ";
                //listener loop.
                mainloop:
                while (true) {

                    int peerIndex = MyProcess.getPeerIndexById(clientId);


                    /*
                    //check if it needs to send a choke or unchoke message.
                    //System.out.println("CLIENT CHOKER " + myId + ": connected to, index: " + clientId + ", " + peerIndex + "| is get change choke: " + MyProcess.peers.get(peerIndex).getChangeChoke() );
                    if(MyProcess.peers.get(peerIndex).getChangeChoke() == true){
                        MyProcess.peers.get(peerIndex).setChangeChoke(false);
                        if(MyProcess.peers.get(peerIndex).getIsChoked()){
                            System.out.println("CLIENT CHOKER " + myId + ": unchoke: " + clientId);
                            MyProcess.peers.get(peerIndex).setChoked(false);
                            message = MessageHandler.createunChokeMessage();
                        }
                        else {
                            System.out.println("CLIENT CHOKER " + myId + ": choke: " + clientId);
                            MyProcess.peers.get(peerIndex).setChoked(true);
                            message = MessageHandler.createChokeMessage();
                        }
                        MessageHandler.sendMessage(out, message);
                    }

                     */


                    if (in.available() > 0) {
                        //handle incoming messages
                        System.out.println("SERVER " + myId + ": beginning new loop iteration");
                        start = System.currentTimeMillis();
                        in.read(sizeB);
                        size = ByteBuffer.wrap(sizeB).getInt();
                        msg = new byte[size];
                        type = in.read();
                        in.read(msg);
                        cost = System.currentTimeMillis() - start;
                        // TODO Currently server is never receiving type 7 messages
                        if (type == 7) { // Only record download rate if receiving a piece
                            MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).downloadRate = cost != 0 ? size / cost : size / 0.0000001; // bytes per ms
                            System.out.println("SERVER " + myId + ": new rate: " + MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).downloadRate);
                        }
                        message = MessageHandler.handleMessage(msg, type, clientId, myId, 'S', log);
                        //if the message handler returns an interested or uninterested message then send it.
                        if (message != null && (message[4] == 2 || message[4] == 3 || message[4] == 7 || message[4] == 6)) {
                            System.out.println("SERVER " + myId + ": sending type " + message[4] + " to " + clientId);
                            MessageHandler.sendMessage(out, message);
                        }

                        //request Pieces!
                        //this no longer requests pieces we should consider removing it.
                        for(Peer peer : MyProcess.peers){
                            if(!peer.getIsChoked()){
                                if(MessageHandler.checkForInterest(peer.bitField, MyProcess.bitField)) {
                                    //msg = MessageHandler.createRequestMessage(MessageHandler.getRandomPiece(peer.bitField, MyProcess.bitField));
                                    //MessageHandler.sendMessage(out, msg);
                                } else {
                                    // tell my process we are done
                                    // todo may need to check bitfield here to see if the process of this server has all pieces
                                    // todo sometimes a server never hits here even though
                                    for(int i = 0; i < MyProcess.bitField.size(); i++){
                                        if(!MyProcess.bitField.get(i)){
                                            System.out.println("SERVER END " + myId + ": not done witn Client: " + clientId);
                                            continue mainloop;
                                        }
                                    }
                                    log.info("Peer " + myId + " has downloaded the complete file.");
                                    //MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setDone();
                                    MyProcess.done = true;
                                    MyProcess.checkDone = true;

                                    // tell client that we are done

                                    MessageHandler.sendMessage(out, MessageHandler.createMsg(8,new byte[]{}));

                                    System.out.println("SERVER END " + myId + ": connected to " + clientId + " download complete");
                                    System.out.println("SERVER END " + myId + ": Disconnect with Client: " + clientId);
//                                    in.close();
//                                    out.close();
//                                    connection.close();
                                    System.out.println("CLIENT END " + myId + ": connected to " + clientId + " TERMINATED");
                                    //System.exit(1);
                                    return;
                                }
                            }
                        }
                    }
                    //check if we are done
                    for(int i = 0; i < MyProcess.bitField.size(); i++){
                        if(!MyProcess.bitField.get(i)){
                            //System.out.println("SERVER END " + myId + ": not done witn Client: " + clientId);
                            continue mainloop;
                        }
                        MyProcess.done = true;
                    }
                    if (MyProcess.done){
                        //MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setDone();
                        // todo may need to check bitfield here to see if the process of this server has all pieces

                        MyProcess.checkDone = true;
                        MessageHandler.sendMessage(out, MessageHandler.createMsg(8,new byte[]{}));
                        System.out.println("SERVER END " + myId + ": connected to " + clientId + " download complete");
                        System.out.println("SERVER END " + myId + ": Disconnect with Client: " + clientId);
//                                    in.close();
//                                    out.close();
//                                    connection.close();
                        System.out.println("CLIENT END " + myId + ": connected to " + clientId + " TERMINATED");
                        //System.exit(1);
                        return;
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

