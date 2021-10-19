import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class PeerProcess {
	private static int peerID;
	private static String hostname;
	private static int lPort = 8000;	//listen on this port
	private static boolean hasFile;
	public ObjectInputStream in;		//stream read from the scocket
	public ObjectOutputStream out;    	//stream write to the socket
	

	public static void main(String[] args) throws Exception {
		// Don't actually need this info because first peer is always me
		// However, this parameter is required by specs
		if (args.length < 1) {
			System.out.println("Invalid: Must include peerID parameter");
		}

		// Processes
		Peer myProcess = new Peer();
		List<Peer> peers = new ArrayList<>();

		// Common variables
		int numPrefNeighbors;
		int unchokeInterval;
		int optUnchokeInterval;
		String fileName;
		long fileSize;
		long pieceSize;

		// Read Peer Info Cfg
		try {
			File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/PeerInfo.cfg");
			Scanner fileReader = new Scanner(myObj);
			boolean isMe = true; // First peer is always me
			while (fileReader.hasNext()) {
				int peerId = Integer.valueOf(fileReader.next());
				String hostName = fileReader.next();
				int port = Integer.valueOf(fileReader.next());
				boolean hasFile = Integer.valueOf(fileReader.next()) == 1;
				if (isMe) {
					myProcess = new Peer(peerId, hostName, port, hasFile);
					isMe = false;
				}
				else {
					peers.add(new Peer(peerId, hostName, port, hasFile));
				}
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		// Read Common Config
		try {
			File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/Common.cfg");
			Scanner fileReader = new Scanner(myObj);
			while (fileReader.hasNextLine()) {
				// Number of Preferred Neighbors
				fileReader.next();
				numPrefNeighbors = Integer.valueOf(fileReader.next());
				// Unchoking Interval
				fileReader.next();
				unchokeInterval = Integer.valueOf(fileReader.next());
				// Optimistic Unchoking Interval
				fileReader.next();
				optUnchokeInterval = Integer.valueOf(fileReader.next());
				// File Name
				fileReader.next();
				fileName = fileReader.next();
				// File Size
				fileReader.next();
				fileSize = Long.valueOf(fileReader.next());
				// Piece Size
				fileReader.next();
				pieceSize = Long.valueOf(fileReader.next());
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		byte[] handshake = new byte[32];
		handshake = createHandshake();
		System.out.println("Peer is running."); 
		ServerSocket listener = new ServerSocket(myProcess.port);
		int clientNum = 1;
		try {
			while(true) {
				new Server(listener.accept(),clientNum).start();
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
	private static class Server extends Thread {
		private String message;    		//message received from the client
		private Socket connection;		//wait on a connection from client
		private int no;				//The index number of the client

		public Server(Socket connection, int no) {
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

        private static class Client extends Thread {

		private Socket requestSocket;           //socket connect to the server
		private String message;                //message send to the server
		private String MESSAGE;                //capitalized message read from the server
	
                public Client() {}

	void run()
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket("localhost", 8000);
			System.out.println("Connected to localhost in port 8000");
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			while(true)
			{
				System.out.print("Hello, please input a sentence: ");
				//read a sentence from the standard input
				message = bufferedReader.readLine();
				//Send the sentence to the server
				sendMessage(message);
				//Receive the upperCase sentence from the server
				MESSAGE = (String)in.readObject();
				//show the message to the user
				System.out.println("Receive message: " + MESSAGE);
			}
		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
            		System.err.println("Class not found");
        	} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	
	//send a message to the output stream
	public void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
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
