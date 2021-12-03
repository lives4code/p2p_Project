import java.io.*;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.in;

public class MyProcess {
    // From Peer Info Cfg
    private static int myId;
    // put your ip and port no
    private String myHostName;
    int port;

    boolean hasFile;
    static BitSet bitField;

    // Handle peers
    static List<Peer> peers;
    static boolean checkDone;
    static boolean done;

    // Common variables
    static int numPrefNeighbors;
    static int unchokeInterval;
    static int optUnchokeInterval;
    static String filename;
    static long fileSize;
    static long pieceSize;
    //initialize piece array

    public MyProcess(int peerId) {
        myId = peerId;
        filename = "../Files_From_Prof/project_config_file_small/project_config_file_small/" + String.valueOf(myId) + "/";
        peers = new ArrayList<>();
        checkDone = false;

        loadCommonConfig();
        loadPeerInfo();

        done = hasFile;
        // start timers
        startDeterminingNeighbors();
        startDeterminingOptimistic();

        //debug
        System.out.println("PEER: piece size: " + pieceSize);
        System.out.println("PEER: file size: " + fileSize);
    }


    //wrote code to flip index.
    //talk to nick make sure this is right.
    public static void writePiece(byte[] pieceIndex, byte[] piece){
        try {
            String pieceRead = "";
            for(int i = -0; i< piece.length; i++){
                pieceRead += " " + String.valueOf(piece[i]);
            }
            System.out.println("filename:" + filename);
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            int skip = (int)pieceSize * index;
            file.seek(skip);
            file.write(piece);
            bitField.set(index);
            System.out.println("bitfield is now" + bitField);
            file.close();
            //System.out.println("write successful index:" + index  + " piece:" + pieceRead);

        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("error occurred");
        }
    }

    public static byte[] readPiece(byte[] pieceIndex ){
        System.out.println("filename:" + filename);
        byte[] piece = new byte[(int) pieceSize];
        int numPieces = (int) Math.ceil(fileSize/pieceSize);
        byte[] pieceInd = new byte[4];
        byte[] ret = new byte[(int)pieceSize + 4];
        for(int i = 0; i < 4; i++){
            pieceInd[i] = pieceIndex[i];
        }
        try{
            RandomAccessFile file = new RandomAccessFile(filename, "r");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            int skip = (int)pieceSize * index;
            file.seek(skip);
            if(index == numPieces){
                int lastPieces = (int) (fileSize - (Math.floor(fileSize/pieceSize) * pieceSize));
                byte[] lastPiece = new byte[lastPieces];
                file.read(lastPiece);
                for(int i = 0; i < lastPieces; i++){
                    piece[i] = lastPiece[i];
                }
                for(int i = lastPieces; i < (int) pieceSize; i++){
                    piece[i] = 0;
                }
            }
            else {
                file.read(piece);
            }
            file.close();
        }
        catch (Exception e){
            System.out.println("error occurred while reading piece");
        }
        for(int i = 0; i < 4; i++){
            ret[i] = pieceInd[i];
        }
        for(int i =4; i < ret.length; i++){
            ret[i] = piece[i - 4];
        }
        //System.out.println("read successful");
        String pieceRead = "";

        for(int i =0; i < ret.length; i++){
            pieceRead += String.valueOf(ret[i]) + "";
        }
        //System.out.println("read successful index:" + ByteBuffer.wrap(pieceIndex).getInt() + "size:" + ret.length +  "piece " + pieceRead);
        return ret;
    }

    //does this function cause each client to start from each computer?
    public void start() throws Exception {
        System.out.println("PEER " + myId + ": Peer is running");
        // Start client
        for (Peer peer: peers){
            if (peer.getPeerId() != myId) {
                new Client(myId, peer.getHostName(), peer.getPort(), peer.getPeerId()).start();
            }
        }
        startDeterminingNeighbors();

        // Start Server
        ServerSocket listener = new ServerSocket(port);
        int clientNum = 1;
        try {
            outerloop:
            while (true) {
                try {
                    listener.setSoTimeout(5000);
                    new Server(listener.accept(), clientNum, myId).start();
                    System.out.println("PEER " + myId + ": Client " + clientNum + " is connected!");
                    clientNum++;
                } catch (SocketTimeoutException e) {
                    System.out.println("PEER CHECK " + myId + ": socket timeout. Restart interval. done: " + done + " check done: " + checkDone);
                }

                //check for children done
                if (checkDone){
                    //am i done
                    if (!done){
                        System.out.println("PEER CHECK " + myId + ": not done");
                        checkDone = false;
                        continue outerloop;
                    }

                    //see if each peer is done if so quit
                    for (Peer peer: peers){
                        System.out.println("PEER CHECK " + myId + ": checking peer " + peer.getPeerId() + " is " + peer.getDone() + " has file " + peer.hasFile);
                        if (!peer.getDone()){
                            checkDone = false;
                            System.out.println("PEER CHECK " + myId + ": continue");
                            continue outerloop;
                        }
                        // all other peers are done
                    }
                    System.out.println("PEER " + myId + ": exiting process");
                    break;
                }
            }
        } finally {
            listener.close();
            System.exit(1);
        }
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
            System.out.println("empty peers are initialized");
            //bitfield is initialized to false by default if the file is present set all the values to true.
            bitField = new BitSet(numPieces);
            if(this.hasFile){
                for(int i = 0; i < bitField.size(); i++){
                        bitField.set(i);
                }
            }
            if(this.hasFile == false){
                File theFile = new File(filename);
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
                System.out.println("Successfully wrote to the file.  ID: " + myId);
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
                filename += fileReader.next();
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

    public static int getPeerIndexById(int id) {
        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).getPeerId() == id) {
                return i;
            }
        }
        return -1;
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    private void startDeterminingNeighbors(){
        System.out.println("starting Determining neighbors. my id is:" + myId);
        TimerTask redetermineNeighbors = new TimerTask() {
            public void run() {
                System.out.println("Redetermining neighbors...");
                if (numPrefNeighbors > peers.size()) {
                    System.out.println("Error: Num preferred neighbors is greater than num of peers");
                    cancel();
                }
                List<Integer> fastestIndices = new ArrayList<>();
                // Find fastest indices
                for (int k = 0; k < numPrefNeighbors; k++) {
                    int minIndex = 0;
                    double minRate = Double.MAX_VALUE;
                    for (int i = 0; i < peers.size(); i++) {
                        if (peers.get(i).downloadRate < minRate && !fastestIndices.contains(i) && peers.get(i).getIsInterested()) {
                            minIndex = i;
                            minRate = peers.get(i).downloadRate;
                        }
                    }
                    fastestIndices.add(minIndex);
                }
                // Set new neighbors
                for (int i = 0; i < peers.size(); i++) {
                    // Unchoke new neighbor // Choke old neighbor except optimistic
                    if ((fastestIndices.contains(i) && peers.get(i).getIsChoked())
                            || (!fastestIndices.contains(i) && !peers.get(i).getIsChoked() && !peers.get(i).getOptimistic())) {
                        System.out.println("PEER " + myId + ": CHANGE CHOKE for: " + peers.get(i).getPeerId());
                        peers.get(i).setChangeChoke(true);
                    }
                    // None of the fastest indices should be considered optimistic
                    if (fastestIndices.contains(i)) {
                        peers.get(i).setOptimistic(false);
                    }
                }

                // Debug
                for (int i = 0; i < peers.size(); i++) {
                    System.out.println("Peer " + peers.get(i).getPeerId() + ": " + peers.get(i).downloadRate + ", " + (peers.get(i).changeChoke ? "changing choke" : ""));
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(redetermineNeighbors, 0, unchokeInterval * 1000);
    }

    private void startDeterminingOptimistic(){
        TimerTask redetermineOptimistic = new TimerTask() {
            public void run() {
                System.out.println("Redetermining optimistic...");

                if (numPrefNeighbors + 1 > peers.size()) {
                    System.out.println("NumPrefNeighbors == num of peers => all neighbors always unchoked");
                    cancel();
                }
                // Get the previous optimistic
                int prevIndex = -1;
                for (int i = 0; i < peers.size(); i++) {
                    if (peers.get(i).getOptimistic()) {
                        prevIndex = i;
                    }
                }
                // Randomly select optimistic neighbor
                List<Integer> chokedIndices = new ArrayList<>();
                for (int i = 0; i < peers.size(); i++) {
                    if (peers.get(i).getIsChoked() && peers.get(i).getIsInterested()) {
                        chokedIndices.add(i);
                    }
                }
                if (prevIndex != -1)
                    chokedIndices.add(prevIndex);
                Random rand = new Random();
                int randomIndex = chokedIndices.size() > 0 ? chokedIndices.get(rand.nextInt(chokedIndices.size())) : -1;
                // Set new neighbors
                for (int i = 0; i < peers.size(); i++) {
                    // Unchoke the new optimistic peer
                    if (randomIndex == i) {
                        peers.get(i).setOptimistic(true);
                        if (peers.get(i).getIsChoked())
                            peers.get(i).setChangeChoke(true);
                    }
                }
                // Choke the old optimistic peer
                if (randomIndex != prevIndex && randomIndex != -1 && prevIndex != -1) {
                    peers.get(prevIndex).setChangeChoke(true);
                    peers.get(prevIndex).setOptimistic(false);
                }

                // Debug
                for (int i = 0; i < peers.size(); i++) {
                    System.out.println("Peer " + peers.get(i).getPeerId() + ": " + peers.get(i).downloadRate + ", " + (peers.get(i).changeChoke ? "changing choke" : ""));
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(redetermineOptimistic, 0, optUnchokeInterval * 1000);
    }
}
