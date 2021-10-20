import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class PeerProcess {
	private static int peerID;
	private static String hostname;
	private static int lPort = 8000;    //listen on this port
	private static boolean hasFile;
	public ObjectInputStream in;        //stream read from the scocket
	public ObjectOutputStream out;        //stream write to the socket


	public static void main(String[] args) throws Exception {
		// Don't actually need this info because first peer is always me
		// However, this parameter is required by specs
		if (args.length < 1) {
			System.out.println("Invalid: Must include peerID parameter");
			System.exit(0);
		}

		// Processes
		MyProcess myProcess = new MyProcess(Integer.valueOf(args[0]));
		myProcess.start();
	}
}
