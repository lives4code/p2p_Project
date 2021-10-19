import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class PeerProcess {
	private static int peerID;
	private static String hostname;
	private static int lPort;
	private static boolean hasFile;

	// Common cfg variables
	private static int numPrefNeighbors;
	private static int unchokeInterval;
	private static int optUnchokeInterval;
	private static String fileName;
	private static long fileSize;
	private static long pieceSize;

	// Peer Info cfg information
	private static List<Peer> peers = new ArrayList<>();

	public static void main(String[] args) throws Exception {
//		if (args.length < 4) {
//			System.out.println("Invalid: Format should contain arguments peerID, hostName, port, file");
//		}
//		peerID = Integer.valueOf(args[0]);
//		hostname = args[1];
//		lPort = Integer.valueOf(args[2]);
//		hasFile = Integer.valueOf(args[3]) == 1;

		// Read Common Config
		try {
			File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/Common.cfg");
			Scanner fileReader = new Scanner(myObj);
			while (fileReader.hasNextLine()) {
				// Number of Preferred Neighbors
				String data = fileReader.next(); data = fileReader.next();
				numPrefNeighbors = Integer.valueOf(data);

				// Unchoking Interval
				data = fileReader.next(); data = fileReader.next();
				unchokeInterval = Integer.valueOf(data);

				// Optimistic Unchoking Interval
				data = fileReader.next(); data = fileReader.next();
				optUnchokeInterval = Integer.valueOf(data);

				// File Name
				data = fileReader.next(); data = fileReader.next();
				fileName = data;

				// File Size
				data = fileReader.next(); data = fileReader.next();
				fileSize = Long.valueOf(data);

				// Piece Size
				data = fileReader.next(); data = fileReader.next();
				pieceSize = Long.valueOf(data);
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		// Read Peer Info Cfg
		try {
			File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/PeerInfo.cfg");
			Scanner fileReader = new Scanner(myObj);
			while (fileReader.hasNext()) {
				int peerId = Integer.valueOf(fileReader.next());
				String hostName = fileReader.next();
				int port = Integer.valueOf(fileReader.next());
				boolean hasFile = Integer.valueOf(fileReader.next()) == 1;
				peers.add(new Peer(peerId, hostName, port, hasFile));
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		byte[] handshake = new byte[32];
		handshake = createHandshake();
		System.out.println("Peer is running."); 
		ServerSocket listener = new ServerSocket(lPort);
		int clientNum = 1;
		try {
			while(true) {
				new Handler(listener.accept(),clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
			}
		}
		finally {
			listener.close();
		}
	}

	//handler thread class. handlers are spawned from the listening
	//loop and are responsible for dealing with a single client's
	//requests
	private static class Handler extends Thread {
		private String message;    //message received from the client
		private Socket connection;	//wait on a connection from client
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int no;		//The index number of the client

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}

        public void run() {
			try {
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				try{
					//preform handshake here to validate connection
					while(true)
					{
						//receive the message sent from the client
						message = (String)in.readObject();
						//debug message to user
						System.out.println("Receive message: " + message + " from client " + no);
						//send MESSAGE back to the client
						//this is where we will handle the message
						//switch
						//test message
						String MESSAGE = "recived";
						sendMessage(MESSAGE);
					}
				}
				catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
			}
			finally {
				//Close connections
				try {
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException) {
					System.out.println("Disconnect with Client " + no);
				}
			}
		}

		//send a message to the output stream
		public void sendMessage(String msg)
		{
			try {
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			}
			catch(IOException ioException) {
				ioException.printStackTrace();
			}
		}

		private void handleMessage(byte[] msg) {
			byte[] msgLength  = new byte[4];
			System.arraycopy(msg, 0, msgLength, 4, 4);
			int mLength = ByteBuffer.wrap(msgLength).getInt();

			byte[] msgType = new byte[1];
			System.arraycopy(msg, 4, msgType, 5, 1);
			int mType = ByteBuffer.wrap(msgType).getInt();

			byte[] msgPayload = new byte[mLength];
			System.arraycopy(msg, 5, msgType, mLength + 5, mLength);

			switch(mType){
				case 0:
				//choke
				case 1:
				//unchoke
				case 2:
				//interested
				case 3:
				//not intrested
				case 4:
				//have
				case 5:
				//bitfield
				case 6:
				//request
				case 7:
				//piece
			}
		}
    }

	public static byte[] createHandshake() {
		byte[] bytes = new byte[32];
		String hexString = "P2PFILESHARINGPROJ";
		byte[] byteString = hexString.getBytes();
		for (int i = 0; i < byteString.length; i++) {
			bytes[i] = byteString[i];
		}
		for (int i = 18; i < 27; i++) {
			bytes[i] = 0x00;
		}

		//bytes
		//File.WriteAllBytes("input.txt", StringToByteArray(hexString));
		//obviously this is the absolute path and will need to be replaced with the relative path.
		try {
			File info = new File("../Files_From_Prof/project_config_file_large/project_config_file_large/PeerInfo.cfg");
			Scanner myReader = new Scanner(info);
			if (myReader.hasNextLine()) {
				String data = myReader.nextLine();
				for (int i = 0; i < 4; ++i) {
					byte l = (byte) Character.getNumericValue(data.charAt(i));
					System.out.println("data:" + data.charAt(i));
					bytes[28 + i] = l;
				}
			}
			while (myReader.hasNextLine()) {
				String data = myReader.nextLine();
				System.out.println(data);
			}
			myReader.close();

		}
		catch (FileNotFoundException e) {
			System.out.println("error");
			e.printStackTrace();
		}
		return bytes;
	}
}
