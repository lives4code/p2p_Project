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

    public Peer() {

    }

    public Peer(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }

}
