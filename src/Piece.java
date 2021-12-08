public class Piece {
    byte[] id;
    byte[] pieceContent;

    Piece(byte[] _id, byte[] _pieceContent, int _pieceSize) {
        this.id = _id;
        this.pieceContent = new byte[_pieceSize];
        for (int i = 0; i < _pieceSize; i++) {
            pieceContent[i] = _pieceContent[i];
        }
    }
}
