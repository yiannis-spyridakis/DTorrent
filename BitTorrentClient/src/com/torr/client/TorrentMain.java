package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import com.torr.ui.ITorrentUI;
import com.torr.utils.SystemUtils;
import com.torr.bencode.TorrentFileDescriptor;

public class TorrentMain implements AutoCloseable, Runnable {
	
	private ITorrentUI torrentUI;
	private TCPServer tcpServer = new TCPServer(this);
	private WorkspaceManager wsm;
	private HashMap<String, TorrentFile> torrentFiles = new HashMap<String, TorrentFile>();
	private Thread backgroundThread = null;
	
	public TorrentMain(ITorrentUI torrentUI) throws Exception
	{
		this.torrentUI = torrentUI;		
		
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
		}
		catch(Exception ex)
		{
			this.torrentUI.Quit(ex);
		}
	}
	
	
	public File GetWorkspaceFolder()
	{
		return wsm.GetWorkspaceFolder();
	}
	public void OpenTorrentFile(final String filePath) throws Exception
	{
		// Validate descriptor file
		TorrentFileDescriptor descriptor = new TorrentFileDescriptor(filePath);
		if(!descriptor.IsValid())
			throw new Exception("Invalid torrent file");		
		
		final String info_hash = descriptor.InfoHash();
		File torrentFolder = this.wsm.GetTorrentFolder(info_hash);
		
		File sourceFile = new File(filePath);
		
		Path destinationFilePath = 
				Paths.get(torrentFolder.getAbsolutePath()).resolve(sourceFile.getName());
		File destinationFile = destinationFilePath.toFile();
		
		if(destinationFile.exists())
		{
			if(SystemUtils.FilesEqual(sourceFile, destinationFile))
			{
				torrentUI.PrintConsoleInfo("Torrent file already in workspace.");
			}
			else
			{
				torrentUI.PrintConsoleInfo("Replacing old version of torrent file in workspace");
				Files.copy(sourceFile.toPath(), destinationFile.toPath());
			}
		}
		else
		{
			torrentUI.PrintConsoleInfo("Moving torrent file into workspace");
			Files.copy(sourceFile.toPath(), destinationFile.toPath());
		}
		
		TorrentFile torrentFile = this.torrentFiles.get(info_hash);
		if(torrentFile != null)
		{
			torrentFile.InitializeTorrentFileUI();
		}
		else
		{
			torrentFile = new TorrentFile(this, descriptor, torrentFolder);
			torrentFiles.put(info_hash, torrentFile);
		}		
		
		torrentUI.PrintConsoleInfo("Successfully opened torrent file");
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
	
	public void HandleConnection(Socket connection) throws IOException {
		//
		//Peer t = new Peer(connection);		
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

