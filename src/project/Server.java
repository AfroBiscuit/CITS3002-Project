package project;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * 
 * @author Alex Guglielmino 20933584
 * @author Dominic Cockman 20927611
 *
 */
public class Server {
	private final int PORTNUM = 4444;
	private final int TIMEOUT = 1500;
	protected static final String[] SERVERDIRECTORIES = {"ServerFiles", "ServerCertificates", "ServerSignatures"};
	private ArrayList<ServerThread> threadList = new ArrayList<ServerThread>();
	
	/**
	 * Exits the server
	 */
	public enum EXIT_COMMAND {
		Q("-q"), QUIT("quit"), EXIT("exit"), CONTINUE("");
		
		private String cmd;
		private EXIT_COMMAND (String cmd) { this.cmd = cmd; }
		public static EXIT_COMMAND getCommand(String str) {
			for(EXIT_COMMAND exit: EXIT_COMMAND.values()) {
				if(exit.cmd.equalsIgnoreCase(str)) {
					return exit;
				}
			}
			return CONTINUE;
		}
	}
	
	/**
	 * Main method
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		new Server().runServer();
		System.exit(0);
	}
	
	/**
	 * starts the server up
	 * @throws IOException
	 */
	private void runServer() throws IOException {
		// pre startup message
		ServerSocket serverSocket = new ServerSocket(PORTNUM);
		serverSocket.setSoTimeout(TIMEOUT);
		serverDirectoryCheck();
		
		System.out.println("Server: Server has started broadcast on port " + PORTNUM + " and is waiting for connections...");
		System.out.println("Server: Type '-q' or 'quit' or 'exit' anytime to shutdown server");
		
		// Set up variables for reading quit message
		BufferedReader bufferedPromptReader = new BufferedReader(new InputStreamReader(System.in));
		EXIT_COMMAND cmd = EXIT_COMMAND.CONTINUE;
		
		// Input / client accepting loop
		do {
			try {
				Socket socket = serverSocket.accept();
				ServerThread thread = new ServerThread(socket);
				thread.start();
				threadList.add(thread); 
			} catch (SocketTimeoutException e) {;} // Do nothing
		
			if(bufferedPromptReader.ready()) {
				cmd = EXIT_COMMAND.getCommand(bufferedPromptReader.readLine());
				if(cmd.equals(EXIT_COMMAND.CONTINUE)) {
					System.out.println("Server: Type 'exit', '-q' or 'quit' to close server");
				}
			}
		} while(cmd.equals(EXIT_COMMAND.CONTINUE));

		// server close message
		System.out.println("Server: Closing all client connections...");
		for(ServerThread thread: threadList) {
			thread.close();
		}
		
		serverSocket.close();
		System.out.println("Server: Server shut down...");
	}

	/**
	 * Checks that all of the directories necessary for storing files on the server separately exist. 
	 * These directories are listed in the directories string array SERVERDIRECTORIES.
	 */
	private void serverDirectoryCheck() {
		try {
			System.out.println("Server: Checking server directories...");
			for(String s: SERVERDIRECTORIES) {
				File file = new File(s);
				if(!file.exists()) {
					if(!file.mkdir()) {
						throw new Exception("Error creating server directories");
					}
				}
				if(!file.isDirectory()) {
						throw new Exception("Server directory names must be for directories only!");		
				}
			}
			System.out.println("Server: Server directories checked.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
