import java.io.*;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.in;

public class MyProcess {
    // From Peer Info Cfg
    private int myId;
    // put your ip and port no
    private String myHostName;
    int port;

    boolean hasFile;
    Bitfield b;

    // Handle client, server, and peers
    Client client;
    Server server;
    List<Peer> peers;

    // Common variables
    int numPrefNeighbors;
    int unchokeInterval;
    int optUnchokeInterval;
    String fileName;
    long fileSize;
    long pieceSize;
    Piece[] pieceArray;
    //initialize piece array

    public MyProcess(int peerId) {
        myId = peerId;
        peers = new ArrayList<>();
        loadCommonConfig();
        loadPeerInfo();
    }
    //TODO write that we are no longer looking for this piece.
    public void writePiece(byte[] pieceIndex, byte[] piece){
        try {
            RandomAccessFile file = new RandomAccessFile("theFile", "w");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            int skip = (int)pieceSize * index;
            file.skipBytes(skip);
            file.write(piece);
            b.hasPiece[index] = 1;
            file.close();
        }
        catch (Exception e){
            System.out.println("error occured");
        }
    }
    //TODO fix error that can occur at the last byte of the file.
    public byte[] readPiece(byte[] pieceIndex ){
        byte[] ret = new byte[(int)pieceSize];
        try {
            RandomAccessFile file = new RandomAccessFile("theFile", "r");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            int skip = (int)pieceSize * index;
            file.skipBytes(skip);
            file.read(ret);
            file.close();
        }
        catch (Exception e){
            System.out.println("error occured while reading piece");
        }
        return ret;
    }

    public void start() throws Exception {
        System.out.println("PEER " + myId + ": Peer is running");
        // Start client
        for (Peer peer: peers){
            if (peer.getPeerId() != myId) {
                new Client(peer.getPeerId(), peer.getHostName(), peer.getPort()).start();
            }
        }

        //System.out.println("debug 1");
        //new ClientSpawn().start();
        // Start Server
        ServerSocket listener = new ServerSocket(port);
        int clientNum = 1;
        try {
            //while (true) {
                //System.out.println("debug 2");
                new Server(listener.accept(), clientNum, myId).start();
                System.out.println("PEER " + myId + ": Client " + clientNum + " is connected!");
                clientNum++;
                //System.out.println("debug 3");
            //}
        } finally {
            listener.close();
        }
    }
    public byte[] intToByte(int input){
        byte[] bytes = ByteBuffer.allocate(4).putInt(input).array();
        return bytes;
    }

    public void loadPeerInfo() {
        try {
            int numPieces = (int) Math.ceil(fileSize / pieceSize);
            File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/PeerInfo.cfg");
            Scanner fileReader = new Scanner(myObj);
            while (fileReader.hasNext()) {
                int peerId = Integer.valueOf(fileReader.next());
                String hostName = fileReader.next();
                int port = Integer.valueOf(fileReader.next());
                boolean hasFile = Integer.valueOf(fileReader.next()) == 1;
                if (peerId == myId) {
                    this.myHostName = hostName;
                    this.port = port;
                    this.hasFile = hasFile;
                } else {
                    peers.add(new Peer(peerId, hostName, port, hasFile));
                }
            }
            b = new Bitfield(numPieces, hasFile);
            if(this.hasFile == false){
                File theFile = new File("theFile");
                if (theFile.createNewFile()) {
                    System.out.println("File created: " + theFile.getName());
                } else {
                    System.out.println("File already exists.");
                }
                File outputFile = theFile;
                byte[] emptyFile = new byte[(int) fileSize];
                for(int i = 0; i < fileSize; i++){
                    emptyFile[i] = 0;
                }
                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    outputStream.write(emptyFile);
                }
                System.out.println("Successfully wrote to the file.");
            }
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void loadCommonConfig() {
        try {
            File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/Common.cfg");
            Scanner fileReader = new Scanner(myObj);
            while (fileReader.hasNextLine()) {
                // Number of Preferred Neighbors
                fileReader.next();
                numPrefNeighbors = Integer.valueOf(fileReader.next());
                // Unchoking Interval
                fileReader.next();
                unchokeInterval = Integer.valueOf(fileReader.next());
                // Optimistic Unchoking Interval
                fileReader.next();
                optUnchokeInterval = Integer.valueOf(fileReader.next());
                // File Name
                fileReader.next();
                fileName = fileReader.next();
                // File Size
                fileReader.next();
                fileSize = Long.valueOf(fileReader.next());
                // Piece Size
                fileReader.next();
                pieceSize = Long.valueOf(fileReader.next());
            }
            fileReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
