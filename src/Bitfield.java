import java.util.ArrayList;


/*
    bitfield contains 1s and 0s
    each peer maintains a piece array
    when a peer requests a piece the corresponding neighbor sends that individual piece

 */


public class Bitfield {
    int pieceNum;
    int pieceSize;
    byte[] hasPiece;


    Bitfield() {
        System.out.println("Bitfield created with no argument.");
        pieceNum = 10;
        for(int i = 0; i < pieceNum; i++){

        }
    }

    //initialize bitfield to false;
    Bitfield(int _pieceNum) {
        this.hasPiece = new byte[pieceNum];
        for(int i = 0; i < pieceNum; i++){
            this.hasPiece[i] = 0;
        }
    }

    // I think it's worth noting that this only checks for the sizes of the piece arrays, the number of pieces, and whtether pieceId
    //matches. it doesn't check each bit in the pieces to error check. passes back an Arraylist containing the indices of needed pieces.
    ArrayList<Integer> bitfieldSame(Bitfield input) {
        ArrayList<Integer> ret = new ArrayList<>();
        try {
            if (input.pieceNum != this.pieceNum) {
                System.out.println("bitfields are different sizes.");

                throw new Exception();
            }
            if (input.pieceSize != this.pieceSize) {
                throw new Exception();
            }
            for (int i = 0; i < pieceNum; i++) {
                if (!(hasPiece[i] == input.hasPiece[i])) {
                    ret.add(i);
                }
            }
        }
        catch (Exception e){
            System.out.println("met information doesn't match. ");
        }
        return ret;
    }


}
