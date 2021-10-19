public class Piece {
    byte[] id = new byte[4];
    byte[] pieceContent;
    int pieceSize;

    Piece(byte[] _id, byte[] _pieceContent, int _pieceSize) {
        this.id = _id;
        this.pieceContent = new byte[_pieceSize];
        for (int i = 0; i < _pieceSize; i++){
            pieceContent[i] = _pieceContent[i];
        }
    }

    boolean compareID(Piece inputPiece) {
        for (int i = 0; i < 4; i++){
            if (this.id[i] != inputPiece.id[i]){
                return false;
            }
        }
        return true;
    }

    boolean compareContent(Piece inputPiece) {
        if (this.pieceSize != inputPiece.pieceSize) {
            return false;
        }
        for (int i = 0; i < pieceSize; i++) {
            if(pieceContent[i] != inputPiece.pieceContent[i]) {
                return false;
            }
        }
        return true;
    }
}
