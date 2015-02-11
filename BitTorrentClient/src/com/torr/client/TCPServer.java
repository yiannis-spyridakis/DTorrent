package com.torr.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

//John
public class TCPServer implements Runnable, AutoCloseable {
	
	private ServerSocket providerSocket;
	private Socket	connection = null;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Thread backgroundThread;
	private volatile boolean portCreated = false;
	private TorrentMain torrentMain = null;
	private volatile boolean shutdownRequested = false;


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
	
	@Override
	public void close()
	{
		try
		{
			this.shutdownRequested = true;
			if(this.providerSocket != null)
			{
				this.providerSocket.close();
			}
		}
		catch(Exception ex)
		{
		}
	}

	void openServer() 
	{
		try
		{
			// creating a server socket
			providerSocket	= new ServerSocket(0, 10);
			portCreated = true;

			while(!this.shutdownRequested) 
			{
				// Wait for connection
				connection = providerSocket.accept();
				torrentMain.HandleIncommingConnection(connection);
			}
		}
		catch (Exception ex) 
		{
			if(!this.shutdownRequested)
			{
				ex.printStackTrace();
			}
		}
		finally
		{
			close();
		}
	}
}