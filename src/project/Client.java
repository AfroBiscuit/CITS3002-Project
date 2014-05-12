package project;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import project.Message;
import project.Message.COMMAND;


public class Client {
	
	private Socket socket;
	private ObjectOutputStream objectToServer;
	private boolean exit = false;
	// private static final int PORTNUM = 4222;
	
	public static void main(String[] args) {
		new Client().runClient();
	}
	
	private void runClient() {
		try {
			System.out.println("Client: Ready for commands (hit 'enter' for command list)");			
			BufferedReader bufferedPromptReader = new BufferedReader(new InputStreamReader(System.in));
			
			// Loop taking input until exit
			while(!exit) {
				System.out.print(">>> ");
				parseInput(bufferedPromptReader.readLine());
			}
			
			// Close session
			if(socket != null) {
				socket.close();
				System.out.println("Client: Closing client connection and exiting...");
			} else {
				System.out.println("Client: Exiting client...");
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void usage(String str) {
		switch (COMMAND.getCommand(str)) {
		case A:
			System.out.println("Usage: -a <filename>");
			break;
		case C:
			System.out.println("Usage: -c <number>");
			break;
		case F:
			System.out.println("Usage: -f <filename>");
			break;
		case H:
			System.out.println("Usage: -h <hostname:port>");
			break;
		case U:
			System.out.println("Usage: -u <certificate>");
			break;
		case V:
			System.out.println("Usage: -v <filename> <certificate>");
			break;
		default:
			// List all commands available
			System.out.println("-a <filename>" + "\t\t\t add or replace a file to the trustcloud");
			System.out.println("-c <number>" + "\t\t\t provide the required circumference (length) of a ring of trust");
			System.out.println("-f <filename>" + "\t\t\t fetch an existing file from the trustcloud server (simply sent to stdout)");
			System.out.println("-h <hostname:port>" + "\t\t provide the remote address hosting the trustcloud server");
			System.out.println("-l	" + "\t\t\t list all stored files and how they are protected");
			System.out.println("-q	" + "\t\t\t quit the program and exit now");
			System.out.println("-u <certificate>" + "\t\t upload a certificate to the trustcloud server");
			System.out.println("-v <filename> <certificate>" + "\t vouch for the authenticity of an existing file in the trustcloud server using the indicated certificate");
			System.out.println("exit	" + "\t\t\t quit the program and exit now");
			break;
		}
	}
	
	private void parseInput(String str) throws NullPointerException {
		try {
			// Breakdown input into tokens separated by space, tab, new line...
			StringTokenizer strTokens = new StringTokenizer(str);
			
			// If string is empty go to usage, otherwise check correct number of inputs
			int strArgs = strTokens.countTokens();
			COMMAND cmd = COMMAND.getCommand(strTokens.nextToken());
			if(strArgs != cmd.getArgs() || cmd.getArgs() == 0) {
				throw new Exception();
			}
			
			// command valid with correct number of argument. Try calling methods.
			switch(cmd) {
			case A:
				// call method
				addReplaceFile(strTokens.nextToken());
				break;
			case C:
				// call method
				int num = Integer.parseInt(strTokens.nextToken());
				circumference(num);
				break;
			case F:
				// call method
				// String filename = strTokens.nextToken();
				// fetchFile (filename);
				break;
			case H:
				String hostAndPort = strTokens.nextToken();
				changeHost(hostAndPort);
				break;
			case L:
				// call method
				// listFiles();
				break;
			case U:
				// call method
				// String certificate = strTokens.nextToken();
				// uploadCertificate(certificate);
				break;
			case V:
				// call method
				//String filename = strTokens.nextToken();
				//String certificate = strTokens.nextToken();
				// vouch(filename, certificate);
				break;
			case Q:
				quit();
				break;
			case EXIT:
				quit();
				break;
			default:
				throw new Exception();	
			}
		} catch (Exception e) {
			if(e.getMessage() != null) {
				System.out.println(e.getMessage());
			}
			System.out.println("Input '" + str + "' not an accepted command.");
			usage(str);
		}
	}
	
	private void changeHost (String hostAndPort) throws UnknownHostException, IOException {
		// Check the argument format
		StringTokenizer strTokens = new StringTokenizer(hostAndPort, ":");
		if(strTokens.countTokens() != 2) {
			throw new IllegalArgumentException("Host:Port string not acceptable");
		}
		// Read the host and port from arguments
		String host = strTokens.nextToken();
		int port = Integer.parseInt(strTokens.nextToken());
		// Instantiate the socket and objectStream
		this.socket = new Socket(host, port);
		this.objectToServer = new ObjectOutputStream(socket.getOutputStream());
		// Report success
		System.out.println("Client: Successfully connected to host '" + socket.getInetAddress()
				+ "' on port '" + socket.getPort() + "'");
	}
	
	private void quit() {
		// Send quit message
		try {
			Message msg = new Message(true, COMMAND.EXIT, 0, "", new byte[1]);
			objectToServer.writeObject(msg);
			objectToServer.flush();
		} catch (IOException IOe) {
			IOe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Change exit status
		this.exit = true;
	}
	
	private void circumference (int circumference) throws IllegalArgumentException, Exception {
		// Illegal call or Illegal arguments
		if(socket == null) {
			throw new Exception("Client:>>>Please connect to a host before specifying trust ring circumference");
		} else if (circumference <= 3) {
			throw new IllegalArgumentException("Client:>>>Please specify a circumference greater than 3");
		}
		
		try {
			// Change the circumference to byte[] for the message
			ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE);
			bb.putInt(circumference);
			Message msg = new Message(true, COMMAND.C, Integer.SIZE, "", bb.array());
			
			// Send the message to the server
			objectToServer.writeObject(msg);
			objectToServer.flush();
			System.out.println("Client: Successfully changed circumference");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void addReplaceFile (String filename) {
		try {
			File file = new File(filename);
			if(!file.exists()) throw new FileNotFoundException();
			System.out.println("Client: File '" + filename + "' exists...");
			
			// convert file to byte[]
			FileInputStream fileInputStream = new FileInputStream(file);		// - for reading contents from file
			ByteArrayOutputStream baos = new ByteArrayOutputStream();			// - an intermediate for writing bytes to the byte[] 
			byte[] buffer = new byte[8192];										// - an array for the stream to read into
			int bitsRead = -1;													// - record how many bytes are read
			int totalRead = 0;													// - record the total number of bits read
			
			// Read data until the end of the file
			while((bitsRead = fileInputStream.read(buffer)) != -1) {
				baos.write(buffer, 0, bitsRead);
				System.out.println("Read: " + bitsRead + " bits read...");
				totalRead += bitsRead;
				bitsRead = -1;
			}
			// the final collection of data read from the file.
			// PROGRAM FLAW: final byte[] too big?
			byte[] data = baos.toByteArray();

			// create message from byte[]
			Message msg = new Message(true, COMMAND.A, totalRead, filename, data);
			
			// send message through socket
			objectToServer.writeObject(msg);
			objectToServer.flush();
			
			// close streams and print success
			fileInputStream.close();
			baos.close();
			System.out.println("Client: File upload successful!");
			
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: FileNotFound thrown in addRemoveFile (Did you include the file extension?)");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ERROR: IOException thrown in addRemoveFile");
			e.printStackTrace();
		}
	}
	
	/*
	private void fetchFile (String filename) {;}
	
	private void listFiles() {;}
	private void uploadCertificate (String certificate) {;}
	private void vouch (String filename, String certificate) {;}

	*/
}