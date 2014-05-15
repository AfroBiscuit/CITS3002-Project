package project;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.StringTokenizer;

import project.Message.COMMAND;
import project3002.CertTest;
import project3002.GenSig;
import project3002.VerSig;

/**
 * Main class for the client's side, contains methods to handle user input and output
 * @author Alex Guglielmino 20933584
 * @author Dominic Cockman 20927611
 *
 */
public class Client {
	
	private Socket socket;
	private ObjectOutputStream objectToServer;
	private ObjectInputStream objectFromServer;
	private boolean exit = false;
	public int sessionCirc = 1;
	
	/**
	 * Main method to run the client
	 * @param args
	 */
	public static void main(String[] args) {
		new Client().runClient();
	}
	
	/**
	 * Handles "GUI"
	 */
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
	
	/**
	 * Elaborates on usage for each flag
	 * @param str flag being used
	 */
	private void usage(String str) {
		switch (COMMAND.getCommand(str)) {
		case A:
			System.out.println("Usage: -a \"filename\"");
			break;
		case C:
			System.out.println("Usage: -c <number>");
			break;
		case F:
			System.out.println("Usage: -f \"filename\"");
			break;
		case H:
			System.out.println("Usage: -h <hostname:port>");
			break;
		case U:
			System.out.println("Usage: -u <certificate>");
			break;
		case V:
			System.out.println("Usage: -v \"filename\" \"certificate\"");
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
	
	/**
	 * Parses the input from the user
	 * @param str user's input
	 * @throws NullPointerException
	 */
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
			String[] filenames = null;

			switch(cmd) {
			case A:
				filenames = getFileNames(str);
				for(int i = 0; i < filenames.length; i++){
					System.out.println(filenames[i]);
				}
				addReplaceFile(COMMAND.A, filenames[0]);
				break;
			case C:
				int num = Integer.parseInt(strTokens.nextToken());
				circumference(num);
				break;
			case F:
				filenames = getFileNames(str);
				printFile(filenames[0]);
				break;
			case H:
				String hostAndPort = strTokens.nextToken();
				changeHost(hostAndPort);
				break;
			case L:
				listFiles();
				break;
			case U:
				filenames = getFileNames(str);
				uploadCertificate(filenames[0]);
				break;
			case V:
				filenames = getFileNames(str);
				vouch(filenames[0], filenames[1]);
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
			e.printStackTrace();
		}
	}

	/**
	 * Method that handles reception of ACK
	 * @return True if the message was received and processed successfully, otherwise false.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private boolean receiveAck() throws ClassNotFoundException, IOException{
		Message msg = (Message) objectFromServer.readObject();
		return !COMMAND.BLANK.equals(msg.getCommand());	
	}
	
	/**
	 * Method for handling hosts
	 * @param hostAndPort
	 * @throws UnknownHostException
	 * @throws IOException
	 */
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
		this.objectFromServer = new ObjectInputStream(socket.getInputStream());
		
		// Report success
		System.out.println("Client: Successfully connected to host '" + socket.getInetAddress()
				+ "' on port '" + socket.getPort() + "'");
	}
	
	/**
	 * Handles quitting the client
	 */
	private void quit() {
		// Send quit message
		try {
			Message msg = new Message(true, COMMAND.EXIT, 0, "", new byte[1][0]);
			objectToServer.writeObject(msg);
			objectToServer.flush();
			
			// Check for success or failure
			if(!receiveAck()) {
				System.out.println("Client:>>>No acknowledgement received from Server.");
			} else {		
				System.out.println("Client: Successfully alerted server of disconnection");
			}
		} catch (IOException IOe) {
			IOe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Change exit status
		this.exit = true;
	}
	
	/**
	 * Handles setting the circumference for the Ring of Trust
	 * @param circumference
	 * @throws IllegalArgumentException
	 * @throws Exception
	 */
	private void circumference (int circumference) throws IllegalArgumentException, Exception {
		// Illegal call or Illegal arguments
		if(socket == null) {
			throw new Exception("Client:>>>Please connect to a host before specifying trust ring circumference");
		} else if (circumference == 0) {
			circumference = 1;
		}
		
		try {
			// Change the circumference to byte[] for the message
			ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE);
			bb.putInt(circumference);
			byte[][] data = new byte[1][bb.array().length];
			data[0] = bb.array();
			Message msg = new Message(true, COMMAND.C, Integer.SIZE, "", data);
			
			// Send the message to the server
			objectToServer.writeObject(msg);
			objectToServer.flush();
			
			// Check for success or failure
			if(!receiveAck()) {
				System.out.println("Client:>>>No acknowledgement received. Try again");
			} else {		
				System.out.println("Client: Successfully changed circumference");
			}
			
			sessionCirc = circumference;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds or replaces a file already in the server
	 * @param cmd what flag brought you here
	 * @param filename what file are you looking for
	 */
	private void addReplaceFile (COMMAND cmd, String filename) {
		try {
			// Check that the command is only to add a file or upload a certificate
			if(!cmd.equals(COMMAND.A) && !cmd.equals(COMMAND.U) && !cmd.equals(COMMAND.V))
				throw new IllegalArgumentException();
			
			// Check that the file exists
			File file = new File(filename);
			if(!file.exists()) 
				throw new FileNotFoundException();
			
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
			Message msg = new Message(true, cmd, totalRead, filename, data);
			
			// send message through socket
			objectToServer.writeObject(msg);
			objectToServer.flush();
			
			// Check for success of upload
			if(!receiveAck()) {
				System.out.println("Client:>>>No acknowledgement received. Try again");
			}
			
			// close streams and print success
			fileInputStream.close();
			baos.close();
			String str = null;
			if(cmd.equals(COMMAND.A)){
				str = "File";
			}
			else if(cmd.equals(COMMAND.U)){
				str = "Certificate";
			}
			else{
				str = "Signature";
			}
			System.out.println("Client: " + str +" upload successful!");
			
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: FileNotFoundException thrown in addRemoveFile (Did you include the file extension?)");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ERROR: IOException thrown in addRemoveFile");
			e.printStackTrace();
		} catch (IllegalArgumentException IAe) {
			IAe.printStackTrace();
		} catch (ClassNotFoundException CNFe) {
			System.out.println("ERROR: ClassNotFoundException - there was an error reading the message from the server");
			CNFe.printStackTrace();
		} catch (Exception e) {
			System.out.println("ERROR: An unknown error occurred while trying to upload a file to the server");
			e.printStackTrace();
		}
	}
	
	// NEEDS WORK
	private void uploadCertificate (String certificate) {
		// Check that the certificate is the right file type / legal
		//
		
		// then send the file to the server using the addReplaceFile method
		addReplaceFile(COMMAND.U, certificate);
	}
	
	/**
	 * Lists the files on the server
	 */
	private void listFiles() {
		try {	
			// Send blank message to server asking to list files
			Message msg = new Message(true, COMMAND.L, 0, "", new byte[0][0]);
			objectToServer.writeObject(msg);
			objectToServer.flush();
			
			// Print out the contents of each message for each folder
			do {
				msg = (Message) objectFromServer.readObject();
				System.out.println("Files on Server under '" + msg.getFilename() + "':");
				String[] fileList = Message.dataToStringArray(msg.getData());
				for(String s: fileList) {
					System.out.println("\t <" + s + ">");
				}
			} while (!msg.isLastMessage());		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fetches a file from the server
	 * @param filename name of the file being fetched
	 * @return returns the data in a byte array
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private byte[][] fetchFile (String filename) throws IOException, ClassNotFoundException {
			// Request the file from the server
			Message msg = new Message(true, COMMAND.F, 0, filename, new byte[0][0]);
			objectToServer.writeObject(msg);
			objectToServer.flush();
			
			// Check incoming message for acknowledgement
			if(!receiveAck()) {
				throw new FileNotFoundException();
			}
			// Add all byte[][] together
			ByteArrayOutputStream baos = new ByteArrayOutputStream();			// - an intermediate for writing bytes to the byte[]
			int totalRead = 0;													// - record the total number of bits read
			do {
				msg = (Message) objectFromServer.readObject();
				baos.write(msg.getData()[0], 0, msg.getDataLength());
				totalRead += msg.getDataLength();
			} while(!msg.isLastMessage());
			
			// Return as byte[][]
			byte[] byteArray = baos.toByteArray();
			byte[][] data = new byte[1][byteArray.length];
			data[0] = byteArray;
			return data;
	}
	
	/**
	 * Prints a file to stdout
	 * @param filename name of the file being printed
	 */
	private void printFile (String filename) {
		try {
			
			byte[][] data = null;
			//Check ring of trust first
			File file = null; //folder
			File file2 = null; //file
			file2 = new File("./ServerFiles/" + filename);
			file = new File("./ServerSignatures/" + file2.getAbsolutePath().replaceAll("[^\\p{L}\\p{Z}]",""));
			int ringSize = 0;
			if(file.isDirectory()){
				//grab the first signature there
				// Get list of contents as string[]
				File[] fileList = file.listFiles();
				ArrayList<String> fileNamesList = new ArrayList<String>();
				for(File f: fileList) {
					fileNamesList.add(f.getName());
					}
				String[] fileNames = fileNamesList.toArray(new String[fileNamesList.size()]);
				String[] parts = fileNames[0].split("_");
				
				String voucher = parts[1].substring(0, parts[1].lastIndexOf('.'));
				
				File cert = new File("./ServerCertificates/" + voucher + ".cer");
				if(file.exists()){
					ringSize = CertTest.getROTDiameter(cert.getName());
					if(ringSize >= sessionCirc){
						// store data from fetchFile
						data = fetchFile(filename);
						System.out.println("Client: Contents of file '" + filename + "' read as a String:");
						System.out.println();
						// read the data into string
						String[] strings = Message.dataToStringArray(data);

						// print the string
						for(String s: strings) {
							System.out.print(s);
						}
						System.out.println();
					}
					else{
						System.out.println("Ring is smaller than required. Required = " + sessionCirc + ", Actual = " + ringSize + ".");
					}
				}
				else{
					throw new FileNotFoundException("Could not find cert for " + voucher);
				}
			}
			else{
				System.out.println("File is unsigned, no Ring exists");
			}
			
			
			
		} catch (FileNotFoundException FNFe) {
			System.out.println("ERROR: File '"+ filename +"' was not be found on the server.");
			FNFe.printStackTrace();
		} catch (IOException IOe) {
			IOe.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Retrieves filenames from a string containing flag and filename, from the user input
	 * @param str User input
	 * @return array with flag and filename
	 * @throws IllegalArgumentException
	 */
	private String[] getFileNames(String str) throws IllegalArgumentException {
		
		// Tokens are split using quotation marks
		StringTokenizer strTok = new StringTokenizer(str, "\"");
		// An arraylist is used to store the tokens
		ArrayList<String> tokens = new ArrayList<String>();

		// 2 or 4 tokens required for legal commands
		if(strTok.countTokens() != 4 && strTok.countTokens() != 2)
			throw new IllegalArgumentException("ERROR: Wrong number of tokens");
		
		// add the tokens
		while(strTok.hasMoreTokens()) {
			tokens.add(strTok.nextToken());
		}
		
		// add legal tokens to return string array
		String[] filenames = null;
		// two filenames
		if(tokens.size() == 4) {
			filenames = new String[2];
			filenames[0] = tokens.get(1);
			filenames[1] = tokens.get(3);
			// only one filename
		} else if (tokens.size() == 2) {
			filenames = new String[1];
			filenames[0] = tokens.get(1);
			// wrong number of arguments --> error
		}
		
		return filenames;
	}
	
	/**
	 * Vouches for a file by signing for it with the provided certificate
	 * @param filename name of the file being vouched for
	 * @param certname name of the certificate on the client's machine that is being used
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	private void vouch (String filename, String certname) throws IOException, GeneralSecurityException{
		//create a read-only copy of the file
		File readOnlyCopy = new File(filename);
		readOnlyCopy.setWritable(false);
		String privkeyloc = null;
		
		//adapted from http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/security/apisign/examples/VerSig.java
    	@SuppressWarnings("resource")
		FileInputStream fis = new FileInputStream(certname);
    	ByteArrayInputStream bis = null;

    	byte value[] = new byte[fis.available()];
    	  fis.read(value);
    	  bis = new ByteArrayInputStream(value);
    	  
    	  CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    	  
    	  X509Certificate cert = (X509Certificate)certFactory.generateCertificate(bis);		
		
    	  privkeyloc = "./ClientPrivateKeys/" + cert.getSubjectDN().getName() + "_pri.key";
		File vouchSig = GenSig.generate(readOnlyCopy.getAbsolutePath(), certname, privkeyloc);
		try {
			if(VerSig.verifySign(certname, vouchSig.getAbsolutePath(), filename)){
				//pull up the file
				System.out.println(vouchSig.getName());
				addReplaceFile(COMMAND.V, vouchSig.getName());
				addReplaceFile(COMMAND.U, certname);
				vouchSig.delete();
			}
			else{
				System.out.println("Signature could not be verified, vouching failed");
			}
			
			/*if(readOnlyCopy.exists()){
				readOnlyCopy.delete();
			}*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}