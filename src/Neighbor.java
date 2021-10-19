import java.util.Vector;

public class Neighbor {
    int neighborId;
    boolean isInterested;
    Bitfield bitfield;
    Vector<byte[]> piecesNeeded;
    float downloadRate;
    boolean choked;

    Neighbor( int _neighborId, boolean _isInterested, Bitfield _bitfield){
        this.neighborId = _neighborId;
        this.isInterested = _isInterested;
        this.bitfield = _bitfield;
    }
    Neighbor(Bitfield _bitfield, int _neigborId){
        this.neighborId = _neigborId;
        this.bitfield = _bitfield;
    }

}
