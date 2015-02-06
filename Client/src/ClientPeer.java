import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;


public class ClientPeer extends Thread{
	Socket requestSocket;
	ObjectOutputStream out;
	ObjectInputStream in;
	InetAddress IP;

	
	public ClientPeer(Socket connection) {
		try
		{
			out	= new ObjectOutputStream(connection.getOutputStream());
			in = new ObjectInputStream(connection.getInputStream());
			IP = connection.getInetAddress()/*.getHostAddress()*/ ;
			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public	ClientPeer(InetAddress IP, String peer_id, int port) {
		try {
			requestSocket =	new	Socket(IP, port);
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void run() {
		try {
			
			out.writeInt(12);
			out.flush();
			int num = in.readInt();
			System.out.println(num);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		finally
		{
			try
			{
				in.close();
				out.close();
			}
			catch
			(IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
	
	
}
