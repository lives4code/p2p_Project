import java.io.File;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MyProcess {
    // From Peer Info Cfg
    private int myId;
    private String myHostName;
    int port;
    boolean hasFile;

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

    public MyProcess() {
        peers = new ArrayList<>();
        loadPeerInfo();
        loadCommonConfig();
    }

    public void start() throws Exception {
        System.out.println("Peer is running.");

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

        // Start client
    }

    public void loadPeerInfo() {
        try {
            File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/PeerInfo.cfg");
            Scanner fileReader = new Scanner(myObj);
            boolean isMe = true; // First peer is always me
            while (fileReader.hasNext()) {
                int peerId = Integer.valueOf(fileReader.next());
                String hostName = fileReader.next();
                int port = Integer.valueOf(fileReader.next());
                boolean hasFile = Integer.valueOf(fileReader.next()) == 1;
                if (isMe) {
                    this.myId = peerId;
                    this.myHostName = hostName;
                    this.port = port;
                    this.hasFile = hasFile;
                    isMe = false;
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
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
