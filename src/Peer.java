public class Peer {
    int peerId;
    String hostName;
    int port;
    boolean hasFile;

    public Peer(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }

}
