package project;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class Server {
	private final int PORTNUM = 4242;
	private final int TIMEOUT = 2000;
	private ArrayList<ServerThread> threadList = new ArrayList<ServerThread>();
	
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
	
	public static void main(String[] args) throws IOException {
		
		new Server().runServer();
		System.exit(0);
	}
	
	private void runServer() throws IOException {
		// pre startup message
		ServerSocket serverSocket = new ServerSocket(PORTNUM);
		serverSocket.setSoTimeout(TIMEOUT);
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
}
