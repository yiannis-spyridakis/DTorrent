package com.torr.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

//John
public class TCPServer implements Runnable {
	
	private ServerSocket providerSocket;
	private Socket	connection = null;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Thread backgroundThread;
	private volatile boolean portCreated = false;
	private TorrentMain torrentMain = null;


	TCPServer(TorrentMain torrentFiles) 
	{
		this.torrentMain = torrentFiles;
		backgroundThread = new Thread(this);
		backgroundThread.start();
	};

	FutureTask<Integer> GetPortNumber()
	{
		FutureTask<Integer> ret = 
		new FutureTask<Integer>(new Callable<Integer>(){
			@Override
			public Integer call()
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
				return providerSocket.getLocalPort();
			}
		});
		ret.run();
		return ret;
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
			providerSocket	= new ServerSocket(0, 10);
			portCreated = true;

			while(true) {
				// Wait for connection
				connection = providerSocket.accept();
				torrentMain.HandleConnection(connection);
			}

		}
		catch (Exception ex) {
			ex.printStackTrace();
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