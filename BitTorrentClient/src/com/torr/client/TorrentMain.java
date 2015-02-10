package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;

import com.torr.ui.ITorrentUI;
import com.torr.utils.*;
import com.torr.bencode.TorrentFileDescriptor;
import com.torr.msgs.PeerMessage;

public class TorrentMain extends TasksQueue implements AutoCloseable, Runnable, IPeerRegistrar {
	
	private ITorrentUI torrentUI;
	private TCPServer tcpServer = new TCPServer(this);
	private WorkspaceManager wsm;
	private HashMap<String, TorrentFile> torrentFiles = new HashMap<String, TorrentFile>();
	private Thread backgroundThread = null;
	private String peerId = null;
	private volatile boolean shutdownRequested = false;
	
	public TorrentMain(ITorrentUI torrentUI) throws Exception
	{
		this.torrentUI = torrentUI;		
		
		UUID test = UUID.randomUUID();
		this.peerId = Consts.PEER_ID_PREFIX + new Long(Math.abs(test.getMostSignificantBits())).toString();
		this.peerId = peerId.substring(0, 20);		
		
		// Create and start the background thread
		backgroundThread = new Thread(this);
		backgroundThread.start();		
	}
	
	@Override
	public void run()
	{
		try
		{
			DoFileSystemBookKeeping();
			
			while(!this.shutdownRequested)
			{
				this.processOutstandingTasks();
				Thread.yield();
			}			
			
		}
		catch(Exception ex)
		{
			this.torrentUI.Quit(ex);
		}				
	}
	
	@Override
	public TorrentFile RegisterPeer(Peer peer, PeerMessage.HandshakeMessage handshakeMsg)
	{
		Log("Accepted request from Peer [" + peer.GetPeerId() + 
				"] for file [" + handshakeMsg.getInfoHash() + "]");
		
		return this.torrentFiles.get(handshakeMsg.getInfoHash());
	}
	
	public String GetPeerId()
	{
		return this.peerId;
	}
	
	public void Log(String message)
	{
		this.torrentUI.PrintConsoleInfo(message);
	}
	public void Log(String message, Exception ex)
	{
		Log(message);
		Log(ex.getMessage());
		ex.printStackTrace();
	}
	
	
	public File GetWorkspaceFolder()
	{
		return wsm.GetWorkspaceFolder();
	}
	public void OpenTorrentFile(final String filePath) throws Exception
	{
		// Validate descriptor file
		final TorrentFileDescriptor descriptor = new TorrentFileDescriptor(filePath);
		if(!descriptor.IsValid())
			throw new Exception("Invalid torrent file");		
		
		final TorrentMain pThis = this;
		this.addTask(new FutureTask<Void>(new Callable<Void>()
		{							
			@Override
			public Void call()// throws Exception
			{
				try
				{
					final String info_hash = descriptor.InfoHash();
					File torrentFolder = pThis.wsm.GetTorrentFolder(info_hash);
					
					File sourceFile = new File(filePath);
					
					Path destinationFilePath = 
							Paths.get(torrentFolder.getAbsolutePath()).resolve(sourceFile.getName());
					File destinationFile = destinationFilePath.toFile();
					
					if(destinationFile.exists())
					{
						if(SystemUtils.FilesEqual(sourceFile, destinationFile))
						{
							Log("Torrent file already in workspace.");
						}
						else
						{
							Log("Replacing old version of torrent file in workspace");
							Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					}
					else
					{
						Log("Moving torrent file into workspace");
						Files.copy(sourceFile.toPath(), destinationFile.toPath());
					}
					
					TorrentFile torrentFile = torrentFiles.get(info_hash);
					if(torrentFile != null)
					{
						Log("Torrent file already initialized in memory");
						torrentFile.InitializeTorrentFileUI();
					}
					else
					{
						Log("Creating new torrent file object");
						torrentFile = new TorrentFile(pThis, descriptor, torrentFolder);
						Log("Putting new torrent file object in collection");
						torrentFiles.put(info_hash, torrentFile);
						
						
						Log("torrentfiles object count = [" + torrentFiles.size() + "]");
						
					}		
					
					Log("Successfully opened torrent file [" + torrentFile.getInfoHash() + "]");				
				}
				catch(Exception ex)
				{
					pThis.Log("Unable to open file:", ex);
				}
				
				return null;
			}
		}));
	}
	public ITorrentUI TorrentUI()
	{
		return this.torrentUI;
	}
	
	private void DoFileSystemBookKeeping() throws Exception
	{
		wsm = new WorkspaceManager(GetMainFolder());
	}
	
	// Returns the program's folder. Creates it if it doesn't exist
	private File GetMainFolder()
	{
		String defaultDir = SystemUtils.GetDefaultDirectory();
		Path folderPath = Paths.get(defaultDir).resolve(Consts.PROGRAM_FOLDER);
		
		File ret = folderPath.toFile();
		// Create folder if it doesn't exist
		ret.mkdirs();
		
		return ret;
	}
	
	public void HandleConnection(Socket connection) throws Exception {
		Peer t = new Peer(this, connection);		
	}
	
	public int GetTCPServerPortNumber()
	{
		try
		{
			return this.tcpServer.GetPortNumber().get();
		}
		catch(Exception ex)
		{
			return 0;
		}
	}
	
	
	@Override
	public void close()
	{
		wsm.close();
		for(TorrentFile tf : torrentFiles.values())
		{
			tf.close();
		}
	}
	
	
	
}

