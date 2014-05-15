package project;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import project.Message;
import project.Message.COMMAND;

/**
 * The ServerThread class handles a single client connection, listening to the connection and responding 
 * to the information and requests passed to the ServerThread as Messages.
 * @author Alex Guglielmino 20933584
 * @author Dominic Cockman 20927611
 */
public class ServerThread extends Thread {
	private static final String[] SERVERDIRECTORIES = null;
	private int circumference;
	private Socket socket;
	private ObjectInputStream objectFromClient;
	private ObjectOutputStream objectToClient;
	private boolean exit;
	
	/**
	 * Creates a single ServerThread from the socket associated with a single client.
	 * @param socket The socket that may be used to communicate with the client.
	 * @throws IOException
	 */
	ServerThread (Socket socket) throws IOException {
		this.circumference = 5;
		this.socket = socket;
		this.objectFromClient = new ObjectInputStream(socket.getInputStream());
		this.objectToClient = new ObjectOutputStream(socket.getOutputStream());
		this.exit = false;
	}
	
	/**
	 * This method executes when the thread has started, and contains the main loop of listening for messages 
	 * from the client requesting some information, then passing those messages onto a method to decide the appropriate actions to take. 
	 */
	public void run() {
		try {
			// alert inside thread
			System.out.println("Server: Server connected to a new client...");

			// process received message
			Message msg = null;
			do {
					msg = (Message)objectFromClient.readObject();
					
					if(msg != null) {
						parseMessage(msg);
						msg = null;
					}
			} while(!exit);
			
			// finalise and close
			objectFromClient.close();
			objectToClient.close();
			socket.close();
			System.out.println("Server: Server disconnected from a client...");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println(">>>The read object was not of type 'Message'");
			e.printStackTrace();
		}
	}
	
	/**
	 * This method takes the message received from the server, read which command call is associated with the message and calls the 
	 * appropriate methods to respond to the client.
	 * @param msg The message received from the client.
	 * @throws IllegalArgumentException If the command held in the message is not a legal command (BLANK or null).
	 */
	private void parseMessage(Message msg) throws IllegalArgumentException {
		try {
			COMMAND cmd = msg.getCommand();
			switch(cmd) {
			case A:
				addReplaceFile(msg.getCommand(), msg.isLastMessage(), msg.getFilename(), msg.getDataLength(), msg.getData());
				break;
			case C:
				setCircumference(msg.getDataAsInt());
				break;
			case F:
				sendFile(msg.getFilename());
				break;
			case L:
				listFiles();
				break;
			case U:
				addCertificate(msg.isLastMessage(), msg.getFilename(), msg.getDataLength(), msg.getData());
				break;
			case V:
				// Add method call
				// verify(filename, certificate)
				addReplaceFile(msg.getCommand(), msg.isLastMessage(), msg.getFilename(), msg.getDataLength(), msg.getData());
				break;
			case Q:
				this.exit = true;
				break;
			case EXIT:
				this.exit = true;
				break;
			default:
				// Command is BLANK or null
				throw new IllegalArgumentException("Command '" + cmd + "' is not an accepted message command.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is built around reading all necessary file data from the client, and given that the whole file is present 
	 * as a stream of bytes, to write that data to the appropriate location on the server under the name suggested in the message.
	 * @param cmd The command associated with the method call. Legal commands are add or upload file.
	 * @param isLastMessage True if no more messages are expected from the client, otherwise false.
	 * @param filename The name of the file to be added to the server from the message data.
	 * @param dataLength The length of the data to be read.
	 * @param data The data contained within the message - the file as a stream of bytes.
	 */
	private void addReplaceFile(COMMAND cmd, boolean isLastMessage, String filename, int dataLength, byte[][] data) {
		try {
			File file = null;
			
			// Open the file to write
			if(cmd.equals(COMMAND.A)) {
				String fileOutputLocation = Server.SERVERDIRECTORIES[0].concat("/" + filename);
				file = new File(fileOutputLocation);
				if(file.exists()) {
					// file modified --> delete the signatures
					deleteSigns(filename);
				}
			} else if(cmd.equals(COMMAND.U)) {
				String fileOutputLocation = Server.SERVERDIRECTORIES[1].concat("/" + new File(filename).getName());
				file = new File(fileOutputLocation);
			} else if (cmd.equals(COMMAND.V)){
				String[] parts = filename.split("_");
				
				
				String fileOutputLocation = Server.SERVERDIRECTORIES[2].concat("/" + parts[0]);
				File folder = new File(fileOutputLocation);
				folder.mkdir();
				System.out.println(folder.getName());
				file = new File("./ServerSignatures/" + folder.getName() + "/" + filename);
			} else {
				throw new IllegalArgumentException("ERROR: Message command in addReplaceFile call is not '-u', '-a' or '-v'");
			}
			System.out.println(">>>Server: Message received with command = '" + cmd + "' for file '" + filename + "'");
			
			//########################### Assuming all files are read/write and can be written over 
			FileOutputStream fos = new FileOutputStream(file);
			
			// Write the data to file
			fos.write(data[0], 0, dataLength);
			
			// Listen for any other messages
			if(!isLastMessage) {
				boolean messageEnd = isLastMessage;
				Message msg = null;
				// Until we've received the last message
				while(!messageEnd) {
					// Cast & store the message, then write message data to file
					msg = (Message) objectFromClient.readObject();
					messageEnd = msg.isLastMessage();
					fos.write(msg.getData()[0], 0, msg.getDataLength());
				}
			}
			
			// Send acknowledgement to the client
			sendAck(COMMAND.C, true);
			
			// Close and print method success
			fos.close();
			System.out.println("Server: Client file successfully written to Server folder");
		} catch(IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Error reading extra message in addReplaceFile");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is called only when a file is overwritten on the server. 
	 * In such a case, any old signatures are erased to ensure a consistency.
	 * @param filename The name of the file that was overwritten
	 * @throws Exception If the method was unable to erase all relevant signatures.
	 */
	private void deleteSigns(String filename) throws Exception {
		// check that the folder "filename" under the certificate folder is legal
		String pathname = (Server.SERVERDIRECTORIES[2] + "/" + filename);
		File file = new File(pathname);
		// Signature folder doesn't exist or path doesn't exist
		if(file == null || !file.exists()) {
			System.out.println("Server: No signatures deleted for '" + filename + "'");
		// Signature folder does exist
		} else {
			if(!file.isDirectory()) {
				throw new IllegalArgumentException("ERROR: Certificates for filename '" + pathname + "' can't be deleted: '" +
						pathname + "' is not a directory name.");
			}
			
			// Find all child files in the folder and delete them
			System.out.println("Server: Deleting signatures for '" + filename + "'");
			File[] fileSigns = file.listFiles();
			boolean success = false;
			for(File f: fileSigns) {
				success = f.delete();
				if(!success) {
					throw new Exception("ERROR: Could not delete file '" + f.getAbsolutePath() + "' on Server");
				}
			}
		}
	}
	
	/**
	 * Sets the circumference for the session
	 * @param circumference size of the circumference for a ring of trust
	 * @throws IOException 
	 */
	private void setCircumference(int circumference) throws IOException {	
		System.out.println("Server: Circumference changed to: " + circumference);
		this.circumference = circumference;	
		sendAck(COMMAND.C, true);
	}
	
	/**
	 * Uploads a specified certificate
	 * @param isLastMessage
	 * @param filename name of the certificate
	 * @param dataLength length of the data being uploaded
	 * @param data data being uploaded
	 */
	private void addCertificate(boolean isLastMessage, String filename, int dataLength, byte[][] data) {
		// Check that the file is a certificate
		addReplaceFile(COMMAND.U, isLastMessage, filename, dataLength, data);
	}
	
	/**
	 * This method sends an empty message reporting whether the desired action was performed successfully or whether it failed.
	 * @param cmd The command made on the client, indicating what was requested of the server.
	 * @param success True if the request was successfully completed or false if some errors were encountered.
	 */
	private void sendAck(COMMAND cmd, boolean success) {
		try {
			COMMAND msgCmd = (success ? cmd : COMMAND.BLANK);
			Message msg = new Message(true, msgCmd, 1, "", new byte[1][0]);
			
			objectToClient.writeObject(msg);
			objectToClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a file across a stream
	 * @param filename file being sent
	 */
	private void sendFile(String filename) {
		try {
			// Check that the file exists and report existence to client
			File file = new File(Server.SERVERDIRECTORIES[0].concat("/" + filename));
			if(!file.exists()) {
				sendAck(COMMAND.F, false);
				throw new FileNotFoundException();
			} else {
				sendAck(COMMAND.F, true);
			}
				
			// Convert file to byte array
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
			byte[] byteArray = baos.toByteArray();
			byte[][] data = new byte[1][byteArray.length];
			data[0] = byteArray;
			
			// create message from byte[]
			Message msg = new Message(true, COMMAND.F, totalRead, filename, data);
			
			// send message through socket
			objectToClient.writeObject(msg);
			objectToClient.flush();
			
			// close streams and print success
			fileInputStream.close();
			baos.close();
			System.out.println("Server: File '" + filename + "' uploaded to client!");
			
		} catch (FileNotFoundException FNFe) {
			System.out.println("Server: Client requested a file that was not found");
		} catch (IOException IOe) {
			IOe.printStackTrace();
		}
	}
	
	/**
	 * This method retrieves the information about the files listed on the server and sends that information to the client.
	 */
	private void listFiles() {
		File file = null;
		Message msg = null;
		try {
			// Send all folder info separately
			for(int i = 0; i < 3; i++) {
				switch(i) {
				case 0:
					file = new File(Server.SERVERDIRECTORIES[0]);
					break;
				case 1:
					file = new File(Server.SERVERDIRECTORIES[1]);
					break;
				default:
					file = new File(Server.SERVERDIRECTORIES[2]);
					break;
				}
				
				// Get list of contents as string[]
				File[] fileList = file.listFiles();
				ArrayList<String> fileNamesList = new ArrayList<String>();
				for(File f: fileList) {
					if(f.isDirectory()) {
						String[] filesReturned = recursiveListFiles(file.getName(), f);
						for(String s: filesReturned) {
							fileNamesList.add(s);
						}
						//recursiveListFiles(file.getName(), f);
					} else {
						fileNamesList.add(f.getName());
					}
				}
				
				// Convert string[] to byte[][] for message
				String[] fileNames = fileNamesList.toArray(new String[fileNamesList.size()]);
				byte[][] fileData = Message.StringArrayToData(fileNames);
				int listLength = fileNames.length;
				
				// Checking values
				for(String s: fileNames) {
					System.out.println("Server:>>> File folder '" + file.getName() + "' has name '" + s + "'");
				}
				
				// Send the message to the client
				msg = new Message(i==2, COMMAND.L, listLength, file.getName(), fileData);
				objectToClient.writeObject(msg);
				objectToClient.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Lists files in the server recursively
	 * @param pathname path to check for files
	 * @param file 
	 * @throws IOException
	 */
	private String[] recursiveListFiles(String pathname, File file) throws IOException {
		// Name of the current subdirectory
		Message msg = null;
		pathname += "/" + file.getName();
		System.out.println("Current subdirectory: " + pathname);
		
		// Get list of contents as string[]
		File[] fileList = file.listFiles();
		ArrayList<String> fileNamesList = new ArrayList<String>();
		for(File f: fileList) {
			if(f.isDirectory()) {
				String[] filesReturned = recursiveListFiles(pathname, f);
				for(String s: filesReturned) {
					fileNamesList.add(s);
				}
				// recursiveListFiles(pathname, f);
			} else {
				fileNamesList.add(pathname.concat("/" +  f.getName()));
			}
		}
		
		// Convert string[] to byte[][] for message
		String[] fileNames = fileNamesList.toArray(new String[fileNamesList.size()]);
		return fileNames;
	}
	
	/**
	 * Indicates to the thread that is should close its connection. 
	 * Only called when the message received from the client indicates that the client has closed their connection.
	 */
	public void close() { 	this.exit = true; 	}
}