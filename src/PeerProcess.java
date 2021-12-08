public class PeerProcess {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("Invalid: Must include peerID parameter");
			System.exit(0);
		}

		// Processes
		MyProcess myProcess = new MyProcess(Integer.valueOf(args[0]));
		myProcess.start();
		return;
	}
}
