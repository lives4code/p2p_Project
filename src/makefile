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
	MyFormatter.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	rm *.class
	rm *.txt
	rm ../peers/1002/thefile
	rm ../peers/1003/thefile
	rm ../log/*.log
