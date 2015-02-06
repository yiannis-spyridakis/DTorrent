package com.torr.client;

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
	private TorrentFiles torrentFiles = null;


	TCPServer(TorrentFiles torrentFiles) 
	{
		this.torrentFiles = torrentFiles;
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
			providerSocket	= new ServerSocket(0, 10);
			portCreated = true;

			while(true) {
				// Wait for connection
				connection = providerSocket.accept();
				torrentFiles.ConnectionCkeck(connection);
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