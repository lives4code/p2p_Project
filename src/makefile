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
	rm *.txt
	rm ../Files_From_Prof/project_config_file_small/project_config_file_small/1002/theFile
	rm ../Files_From_Prof/project_config_file_small/project_config_file_small/1003/theFile
	rm ../Files_From_Prof/project_config_file_small/project_config_file_small/1004/theFile
	rm ../Files_From_Prof/project_config_file_small/project_config_file_small/1005/theFile
	rm ../Files_From_Prof/project_config_file_small/project_config_file_small/1006/theFile
	rm ../Files_From_Prof/project_config_file_small/project_config_file_small/1007/theFile
	rm ../Files_From_Prof/project_config_file_small/project_config_file_small/1008/theFile
