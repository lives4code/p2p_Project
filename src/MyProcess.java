import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.in;

public class MyProcess {
    // From Peer Info Cfg
    private int myId;
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
        loadPeerInfo();
        loadCommonConfig();
    }

    public void start() throws Exception {
        System.out.println("Peer is running.");
        // Start client
        new ClientSpawn().start();
        // Start Server
        ServerSocket listener = new ServerSocket(port);
        int clientNum = 1;
        try {
            while (true) {
                new Server(listener.accept(), clientNum).start();
                System.out.println("Client " + clientNum + " is connected!");
                clientNum++;
            }
        } finally {
            listener.close();
        }
    }
    public byte[] intToByte(int input){
        byte[] bytes = ByteBuffer.allocate(4).putInt(input).array();
        return bytes;
    }

    public void addPiece(Piece p, int index){
        b.hasPiece[index] = 1;
        pieceArray[index] = p;

    }
    // this should probably return a bitmap object.
    public void loadTheFile( Piece[] pieceArray, Bitfield b){
        try{
            int numPieces = (int) Math.ceil(fileSize / pieceSize);
            byte[] pieceContent = new byte[(int)pieceSize];
            b = new Bitfield(numPieces);
            //Piece[] pieceArray = new Piece[numPieces];

            //could be an error here is there like a more relative way to read the file?
            FileInputStream in = new FileInputStream("../Files_From_Prof/project_config_file_small/1001/thefile");
            int counter = 0;
            //this goes up until the last piece because I don't want to deal with the end of file exception breaking stuff.
            while (counter < numPieces - 1){
                in.read(pieceContent);
                addPiece(new Piece(intToByte((int) counter), pieceContent, (int)pieceSize), counter);
                counter++;

            }
            //the last piece will be the rest of the fill followed by leading zeros
            int finalPieceSize = (int) ((fileSize - ((numPieces - 1)) * (int) pieceSize));
            byte[] finalPiece = new byte[finalPieceSize];
            in.read(finalPiece);
            //just read the final piece now I want to put it into an array of the same size as the others and fill it with zeros.
            for(int i = 0; i < finalPieceSize; i++){
                pieceContent[i] = finalPiece[i];
            }
            for(int i = finalPieceSize; i < (int) pieceSize; i++){
                pieceContent[i] = 0x00;
            }
            //put it into the piece.
            addPiece(new Piece(intToByte((int) counter), pieceContent, (int)pieceSize), counter + 1);

        }
        catch (FileNotFoundException e){
            System.out.println("An error occured.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void loadPeerInfo() {
        try {
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
            fileReader.close();
        } catch (FileNotFoundException e) {
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
            int numPieces = (int) Math.ceil(fileSize / pieceSize);
            pieceArray = new Piece[numPieces];
            b = new Bitfield(numPieces);
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
