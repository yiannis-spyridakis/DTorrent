import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPServer implements Runnable {
	ServerSocket providerSocket;
	Socket	connection = null;
	ObjectOutputStream out;
	ObjectInputStream in;
	private Thread backgroundThread;
	volatile boolean portCreated = false;
	

	TCPServer() 
	{
		backgroundThread = new Thread(this);
		backgroundThread.start();
	};
	
	public void WaitForPortCreation()
	{
		while(!portCreated)
		{
			try
			{
				Thread.sleep(50);
			}
			catch(InterruptedException ex)
			{
				
			}
		}
	}
	
	public int socketId()
	{
		if(providerSocket != null)
		{
			return providerSocket.getLocalPort();
		}
		
		return 0;
	}
	
	@Override
	public void run()
	{
		openServer();
	}
	
	void openServer() {
		try
		{
			// creating a server socket
			providerSocket	= //new ServerSocket(50004, 10);
				new ServerSocket(0, 10);
			portCreated = true;
			
			while(true) {
				// Wait for connection
				connection = providerSocket.accept();
				Thread t = new ClientPeer(connection);
				t.start();
			}
			
		}
		catch (IOException ioException) {
			ioException.printStackTrace();
		}
		finally
		{
			// Closing connection
			try
			{
				providerSocket.close();
			}
			catch(IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
}