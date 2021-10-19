import java.util.Vector;

public class Bitfield {

    int pieceNum;
    int pieceSize;
    Piece[] pieces;




    Bitfield(){
        System.out.println("bitfield created with no argument");
        pieceNum = 10;
        for(int i = 0; i < pieceNum; i++){

        }
    }
    Bitfield(int _pieceNum, Piece[] _pieces ){
        this.pieces = new Piece[pieceNum];
        for(int i = 0; i < pieceNum; i++){
            this.pieces[i] = _pieces[i];
        }
    }

    // I think it's worth noting that this only checks for the sizes of the piece arrays, the number of pieces, and whtether pieceId
    //matches. it doesn't check each bit in the pieces to error check.
    boolean bitfieldSame(Bitfield input){
        if(input.pieceNum != this.pieceNum){
            return false;
        }
        if(input.pieceSize != this.pieceSize){
            return false;
        }
        for(int i = 0; i < pieceNum; i ++){
            if(!pieces[i].compareID(input.pieces[i])){
                return false;
            }
        }
        return true;
    }
    Vector<byte[]> findNeededPieces(Bitfield input){
        Vector<byte[]> ret = null;
        for ( int i = 0; i < pieceNum; i++){
            if(!this.pieces[i].compareContent(input.pieces[i])){
                ret.add(input.pieces[i].id);
            }
        }
        return ret;
    }

}
