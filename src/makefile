.SUFFIXES: .java .class
.java.class:
	javac -g $*.java

CLASSES = \
	Bitfield.java \
	Client.java \
	MessageHandler.java \
	MyProcess.java \
	Peer.java \
	PeerProcess.java \
	Piece.java \
	Server.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	rm *.class
	rm theFile
