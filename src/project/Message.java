package project;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * This message class is defined to give context to the information passed between the client and the server.
 * It defines a number of fields that are necessary to give complete information for any method calls.
 * The use of the Message class provides a definite type for all information passed between the client and the server, 
 * such that every object passed through the socket may be assumed to be of the message type.
 * @author Alex Guglielmino 20933584
 * @author Dominic Cockman 20927611
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	private final boolean isLastMessage;
	private final COMMAND command;
	private final int dataLength;
	private final String filename;
	private final byte[][] data;							// An n-dimensional array with each element being a single bit
	
	/**
	 * This embedded command enumeration simply takes the string commands and returns the matching enumerated type.
	 */
	public enum COMMAND {
		A("-a", 2), C("-c", 2), F("-f", 2), H("-h", 2), L("-l", 1), S("sign", 1),
		Q("-q", 1), U("-u", 2), V("-v", 3),  EXIT("exit", 1), BLANK("", 0);
		private final String cmd;
		private final int args;
		private COMMAND (String str, int args) { this.cmd = str; this.args = args;}
		// Find the matching enum type for the string argument. BLANK by default.
		public static COMMAND getCommand(String str) {
			if(str != null) {
				for(COMMAND cmdItem: COMMAND.values()) {
					if(str.startsWith(cmdItem.cmd) && !cmdItem.equals(S)) {
						return cmdItem;
					}
				}
			}
			return BLANK;
		}
		public int getArgs() { return this.args;	}
		public String getStr() { return this.cmd;	}
	}
	
	/**
	 * 
	 * @param isLastMessage True if the current message is the last message, or false if more messages are to follow.
	 * @param command The command associated with the message data and method call.
	 * @param dataLength The length of the data
	 * @param filename The name of the 
	 * @param data The actual data sent between the client and the host through object streams.
	 * @throws IllegalArgumentException
	 */
	public Message(boolean isLastMessage, COMMAND command, int dataLength, 
					String filename, byte[][] data) throws IllegalArgumentException {
		this.isLastMessage = isLastMessage;
		this.command = command;
		this.dataLength = dataLength;
		this.filename = filename;
		this.data = data;
		
		if(dataLength > data.length && dataLength > data[0].length) 
			throw new IllegalArgumentException("Message data length field must be equal to or less than array size.");
	}
	
	public boolean isLastMessage()	{	return isLastMessage;						}
	public COMMAND getCommand() 	{	return command;								}
	public int getDataLength()		{	return dataLength;							}
	public String getFilename()		{	return filename;							}
	public byte[][] getData()		{	return data;								}
	public int getDataAsInt() 		{	return ByteBuffer.wrap(data[0]).getInt();	}
	
	/**
	 * This method converts a string array to a two dimensional byte array. 
	 * This may primarily be used to translate strings into byte arrays, to reduce effort/work in sending messages.
	 * @param strings The strings to be converted.
	 * @return The strings as arrays of bytes.
	 */
	public static byte[][] StringArrayToData(String[] strings) {
		byte[][] data = new byte[strings.length][];
		for(int i = 0; i < strings.length; i++) {
			data[i] = strings[i].getBytes();
		}
		return data;
	}
	
	/**
	 * This method converts a two dimensional byte array to a string array. 
	 * This may primarily be used when the data carried in the message is assumed to be a string array.
	 * @param data The byte data to be converted.
	 * @return The data interpreted as an array of strings.
	 */
	public static String[] dataToStringArray(byte[][] data) {
		String[] strings = new String[data.length];
		for(int i = 0; i < data.length; i++) {
			strings[i] = new String(data[i]);	//.toString();
		}
		return strings;
	}
}