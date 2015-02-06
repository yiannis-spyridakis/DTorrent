
public class Client {
	
	public static void main(String args[]) {
		
		TCPServer clientServer = new TCPServer();
		clientServer.WaitForPortCreation();
		//clientServer.openServer();
		
		new ClientTracker(clientServer.socketId()).start();
		
		
		
	}
}
