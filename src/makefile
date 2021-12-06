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
	CleanRemote.java \
	RemotePeerInfo.java \
	StartRemotePeers.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	rm *.class
	rm *.txt
	rm ../peers/1002/thefile
	rm ../peers/1003/thefile
	rm ../peers/1004/thefile
	rm ../peers/1005/thefile
	rm ../peers/1006/thefile
	rm ../log/log*
