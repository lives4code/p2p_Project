import java.util.ArrayList;

public class Bitfield {
    int pieceNum;
    byte[] hasPiece;

    Bitfield() {
        System.out.println("Bitfield created with no argument.");
    }

    //initialize bitfield to false;
    Bitfield(int _pieceNum, boolean hasFile){
        this.pieceNum = _pieceNum;
        this.hasPiece = new byte[pieceNum];
        for(int i = 0; i < pieceNum; i++) {
            if (!hasFile) {
                this.hasPiece[i] = 0;
            }
            else {
                this.hasPiece[i] = 1;
            }
        }

    }

    Bitfield(int _pieceNum) {
        this.pieceNum = _pieceNum;
        this.hasPiece = new byte[pieceNum];
        for(int i = 0; i < pieceNum; i++){
            this.hasPiece[i] = 0;
        }
    }
}
