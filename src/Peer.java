import java.util.ArrayList;

public class Peer {
    // From Peer Info Cfg
    private int peerId;
    private String hostName;
    int port;
    boolean hasFile;

    // Other
    boolean isInterested;
    Bitfield bitfield;
    ArrayList<byte[]> piecesNeeded;
    float downloadRate;
    boolean choked;

    public Peer(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }
    public int getPeerId(){
        return this.peerId;
    }
    public String getHostName(){
        return this.hostName;
    }
    public  int getPort(){
        return this.port;
    }

}
