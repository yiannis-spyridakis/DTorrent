package com.torr.tracker;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.torr.trackermsgs.MessageToClient;

public class Tracker 
{
	ServerSocket providerSocket;
	Socket	connection = null;
	ObjectOutputStream out;
	ObjectInputStream in;
	public static Map<String, List<MessageToClient>> peers 
		= new HashMap<String, List<MessageToClient>>(100);
	
	Tracker() {}
	
	void openServer() 
	{
		try
		{
			// creating a server socket + test comment
			providerSocket	= new ServerSocket(20000, 10);
			System.out.print(
					"Listening at IP: [" + Inet4Address.getLocalHost().getHostAddress() + "],\n" +
					"Port: [" + providerSocket.getLocalPort() + "]\n");
			
			Thread d = new Delete(); d.start();
			while(true) 
			{
				// Wait for connection
				connection = providerSocket.accept();
				Thread t = new actionsForClients(connection);
				t.start();
			}
			
		}
		catch (IOException ioException) 
		{
			ioException.printStackTrace();
		}
		finally
		{
			// Closing connection
			try
			{
				providerSocket.close();
			}
			catch(IOException ioException) 
			{
				ioException.printStackTrace();
			}
		}
	}
}