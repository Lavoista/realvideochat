import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class MainFrame extends JFrame implements Runnable{
	private static final long serialVersionUID = 6102172049657799287L;
	public static MainFrame mainFrame;
	
	private static final String SERVER_ERROR = "e000";
	private static final String CLIENT_ERROR = "e001";
	
	private static final String LOGIN = "l001";
	private static final String LOGIN_AGREEMENT = "l002";
	private static final String LOGIN_DISAGREEMENT = "l003";
	private static final String LOGIN_NO_ID = "l004";
	
	private static final String STATE_LOGIN = "s001";
	private static final String STATE_LOGOUT = "s002";
	
	
	//private Serv
	private JList list;
	private JTextArea textArea;
	
	private Connection connection;
	private Statement statement;
	private ServerSocket serverSocket;
	private final int serverPort = 10101;
	
	public MainFrame(){
		super("Server For Real Video Chat Application");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBounds(0, 0, 1000, 900);
		mainFrame = this;
		
		list = new JList();
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(150, 100));
		scrollPane.getVerticalScrollBar().setBlockIncrement(20);
		add(scrollPane, BorderLayout.WEST);
		
		textArea = new JTextArea("Start application.\n");
		textArea.setEditable(false);
		scrollPane = new JScrollPane(textArea);
		scrollPane.getVerticalScrollBar().setBlockIncrement(20);
		add(scrollPane, BorderLayout.CENTER);
		
		if(!setDatabase())
			return;
		
		new Thread(this).start();
		
		
	}
	
	public static void main(String[] args){
		new MainFrame().setVisible(true);
	}
	
	private boolean setDatabase(){
		try{
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/realvideochat", "root", "qldpfh");
			textArea.append("connected MySQL database.\n");
		} catch(SQLException e){
			textArea.append(e.getLocalizedMessage()+"\n\n");
			return false;
		}
		
		try{
			Class.forName("org.gjt.mm.mysql.Driver");
			textArea.append("ok driver.\n");
		} catch(ClassNotFoundException e){
			textArea.append(e.getLocalizedMessage()+"\n\n");
			return false;
		}
		
		try{
			statement = connection.createStatement();
			textArea.append("ok statement.\n");
		} catch(SQLException e){
			textArea.append(e.getLocalizedMessage()+"\n\n");
			return false;
		}
		
		return true;
	}

	public void run(){
		try {
			serverSocket = new ServerSocket(serverPort);
			textArea.append("Created server socket.\n");
		} catch (IOException e){
			textArea.append(e.getLocalizedMessage()+"\n\n");
			return;
		}		
		
		while(true){
			Socket socket = null;
			try{
				socket = serverSocket.accept();
				textArea.append("Connected " + socket.getRemoteSocketAddress() + ".\n");
			} catch(Exception e){
				textArea.append(e.getLocalizedMessage()+"\n\n");
				
				if(socket != null){
					try{
						socket.close();
					} catch(IOException e1){
						textArea.append(e.getLocalizedMessage()+"\n\n");
					}
				}
				continue;
			}
			
			BufferedReader reader = null;
			PrintWriter writer = null;
			try{
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				
				String message = reader.readLine();
				if(message.startsWith(LOGIN)){
					int index = message.indexOf("|");
					String id = message.substring(4, index);
					String pw = message.substring(index+1);
					
					String quary = "select pw, nicname from user_info where id='" + id + "'";
					ResultSet resultSet = null;
					try{
						resultSet = statement.executeQuery(quary);
						textArea.append("check id and pw.\n");
						
						// 해당하는 계정이 존재하는 경우, 비번 일치 여부 확인
						if(resultSet.next()){
							String pw2 = resultSet.getString(1);
							if(pw2.equals(pw)){
								String nicName = resultSet.getString(2);
								
								String friendIdList = "";
								String friendId;
								String friendNicName;
								
								quary = "select relation.friend_id, info.nicname "
									+ "from user_relation relation join user_info info on relation.friend_id = info.id "
									+ "where relation.user_id='" + id + "'";
								resultSet = statement.executeQuery(quary);
								
								Vector<ReadThread> readThreadList = ReadThread.readThreadList;
								ReadThread readThread;
								boolean existFriend;
								//ResultSet resultSet2;
								
								while(resultSet.next()){
									existFriend = false;
									friendId = resultSet.getString(1);
									friendNicName = resultSet.getString(2);
									
									//quary = "select nicname from user_info where id='" + friendId + "'";
									//resultSet2 = statement.executeQuery(quary);
									//if(resultSet2.next())
									//	friendNicName = resultSet2.getString(1);
									//else
									//	friendNicName = "NULL";
									
									for(int i=0; i<readThreadList.size(); i++){
										readThread = readThreadList.elementAt(i);
										if(readThread.isSameId(friendId)){
											existFriend = true;
											break;
										}
									}
									
									if(existFriend){
										friendIdList += "|" + STATE_LOGIN + "|" + friendId + "|" + friendNicName;
										
										// 해당 친구에게 로그인 메시지 보낸다
										
									}
									else{
										friendIdList += "|" + STATE_LOGOUT + "|" + friendId + "|" + friendNicName;
									}
								}
								
								writer.println(LOGIN_AGREEMENT + friendIdList);
								writer.flush();
								
								
								new ReadThread(socket, reader, writer, id, pw, nicName).start();
							}
							else{
								writer.println(LOGIN_DISAGREEMENT);
								writer.flush();
								
								reader.close();
								writer.close();
								socket.close();
							}
						}
						// 해당하는 계정이 존재하지 않는 경우
						else{
							writer.println(LOGIN_NO_ID);
							writer.flush();
							
							reader.close();
							writer.close();
							socket.close();
						}
						
						resultSet.close();
					} catch(SQLException e){
						textArea.append(e.getLocalizedMessage()+"\n\n");
						
						writer.println(SERVER_ERROR);
						writer.flush();
						
						reader.close();
						writer.close();
						socket.close();
						
						if(resultSet != null)
							resultSet.close();
					}
				}
				else{
					writer.println(CLIENT_ERROR);
					writer.flush();
					
					reader.close();
					writer.close();
					socket.close();
				}
				
			} catch(Exception e){
				textArea.append(e.getLocalizedMessage()+"\n\n");
				
				if(socket != null){
					try{
						socket.close();
					} catch(IOException e1){
						textArea.append(e.getLocalizedMessage()+"\n\n");
					}
				}
				
				if(reader != null){
					try{
						reader.close();
					} catch(IOException e1){
						textArea.append(e.getLocalizedMessage()+"\n\n");
					}
				}
				
				if(writer != null){
					writer.close();
				}
			}
		}
	}
	
	public void appendTextArea(String text){
		textArea.append(text);
	}
}
