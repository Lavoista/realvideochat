import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;


public class ReadThread extends Thread{
	public static Vector<ReadThread> readThreadList = new Vector<ReadThread>();
	
	private WriteThread writeThread;
	
	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;
	private String id;
	private String pw;
	private String nicName;
	
	private String sendingMessage = "";
	
	public ReadThread(Socket socket, BufferedReader reader, PrintWriter writer, String id, String pw, String nicName){
		this.socket = socket;
		this.reader = reader;
		this.writer = writer;
		this.id = id;
		this.pw = pw;
		this.nicName = nicName;
		
		//writeThread = new WriteThread(writer);
		//writeThread.start();
		
		readThreadList.add(this);
	}
	
	public boolean isSameId(String id){
		if(id.equals(this.id))
			return true;
		else
			return false;
	}
	
	public void run(){
		while(true){
			if(sendingMessage.length() > 0){
				writer.println(sendingMessage);
				writer.flush();
				
				sendingMessage = "";
			}
			else{
				try {
					if(!reader.ready()){
						Thread.sleep(100);
						continue;
					}
				} catch(IOException e){
					MainFrame.mainFrame.appendTextArea(e.getLocalizedMessage()+"\n\n");
				} catch(InterruptedException e){
					MainFrame.mainFrame.appendTextArea(e.getLocalizedMessage()+"\n\n");
				}
				
				try {
					reader.readLine();
				} catch(IOException e){
					MainFrame.mainFrame.appendTextArea(e.getLocalizedMessage()+"\n\n");
				}
			}
		}
	}
	
	public void sendMessage(String message){
		sendingMessage = message;
	}
}

class WriteThread extends Thread{
	private PrintWriter writer;
	
	public WriteThread(PrintWriter writer){
		this.writer = writer;
	}
	
	public void run(){
		this.suspend();
	}
}
