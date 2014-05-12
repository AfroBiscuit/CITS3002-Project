package project;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	private final boolean isLastMessage;
	private final COMMAND command;
	private final int dataLength;
	private final String filename;
	private final byte[] data;							// Each element is a single bit
	
	public enum COMMAND {
		A("-a", 2), C("-c", 2), F("-f", 2), H("-h", 2), L("-l", 1), 
		Q("-q", 1), U("-u", 2), V("-v", 3),  EXIT("exit", 1), BLANK("", 0);
		private final String cmd;
		private final int args;
		private COMMAND (String str, int args) { this.cmd = str; this.args = args;}
		public static COMMAND getCommand(String str) {
			if(str != null) {
				for(COMMAND cmdItem: COMMAND.values()) {
					if(str.startsWith(cmdItem.cmd)) {
						return cmdItem;
					}
				}
			}
			return BLANK;
		}
		public int getArgs() { return this.args;	}
		public String getStr() { return this.cmd;	}
	}
	
	// Possible security problem? Data & array size
	public Message(boolean isLastMessage, COMMAND command, int dataLength, 
					String filename, byte[] data) throws IllegalArgumentException {
		this.isLastMessage = isLastMessage;
		this.command = command;
		this.dataLength = dataLength;
		this.filename = filename;
		this.data = data;
		
		if(dataLength > data.length) 
			throw new IllegalArgumentException("Message data length field must be equal to or less than array size.");
	}
	
	public boolean isLastMessage()	{	return isLastMessage;					}
	public COMMAND getCommand() 	{	return command;							}
	public int getDataLength()		{	return dataLength;						}
	public String getFilename()		{	return filename;						}
	public byte[] getData()			{	return data;							}
	public int getDataAsInt() 		{	return ByteBuffer.wrap(data).getInt();	}
}