package project;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import project.Message;
import project.Message.COMMAND;

public class ServerThread extends Thread {
	private int circumference;
	private Socket socket;
	private ObjectInputStream objectFromClient;
	private ObjectOutputStream objectToClient;
	private boolean exit;
	
	ServerThread (Socket socket) throws IOException {
		this.circumference = 5;
		this.socket = socket;
		this.objectFromClient = new ObjectInputStream(socket.getInputStream());
		this.objectToClient = new ObjectOutputStream(socket.getOutputStream());
		this.exit = false;
	}
	
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
	
	private void parseMessage(Message msg) throws IllegalArgumentException {
		try {
			COMMAND cmd = msg.getCommand();
			switch(cmd) {
			case A:
				// Add method call
				addReplaceFile(msg.isLastMessage(), msg.getFilename(), msg.getDataLength(), msg.getData());
				break;
			case C:
				//System.out.println("Message received with command C, data length '" + msg.getDataLength() + 
				//		"', isLastCommand value '" + msg.isLastMessage() + "' and integer value: " + (int)msg.getDataAsInt());
				setCircumference(msg.getDataAsInt());
				break;
			case F:
				// Add method call
				// sendFile(msg.getFilename);
				break;
			case L:
				// Add method call
				// listFiles();
				break;
			case U:
				// Add method call
				// addCertificate(msg.isLastMessage(), msg.getFilename(), msg.getDataLength(), msg.getData());
				break;
			case V:
				// Add method call
				// verify
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
	
	private void addReplaceFile(boolean isLastMessage, String filename, int dataLength, byte[] data) {
		try {
			System.out.println("Server: Message received with command = '" + COMMAND.A + "for file '" + filename);
			// Open the file to write
			String fileOutputLocation = "ServerFiles/".concat(filename);
			File file = new File(fileOutputLocation);
			FileOutputStream fos = new FileOutputStream(file);
			
			// Write the data to file
			fos.write(data, 0, dataLength);
			
			// Listen for any other messages
			if(!isLastMessage) {
				boolean messageEnd = isLastMessage;
				Message msg = null;
				// Until we've received the last message
				while(!messageEnd) {
					// Cast & store the message, then write message data to file
					msg = (Message) objectFromClient.readObject();
					messageEnd = msg.isLastMessage();
					fos.write(msg.getData(), 0, msg.getDataLength());
				}
			}
			
			// Close and print method success
			fos.close();
			System.out.println("Server: Client file successfully written to Server folder");
		} catch(IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Error reading extra message in addReplaceFile");
			e.printStackTrace();
		}
	}
	
	private void setCircumference(int circumference) {	this.circumference = circumference;	}
	
	//private void addCertificate(boolean isLastMessage, String filename, long dataLength, byte[] data);
	//private void sendFile(String filename);
	//private void listFiles();
	
	public void close() { 	this.exit = true; 	}
}
