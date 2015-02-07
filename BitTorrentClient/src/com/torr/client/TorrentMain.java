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

public class TorrentMain implements AutoCloseable {
	
	private ITorrentUI torrentUI;
	private TCPServer tcpServer = new TCPServer(this);
	private WorkspaceManager wsm;
	private HashMap<String, TorrentFile> torrentFiles = new HashMap<String, TorrentFile>();
	
	public TorrentMain(ITorrentUI torrentUI) throws Exception
	{
		this.torrentUI = torrentUI;
		
		DoFileSystemBookKeeping();
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
				torrentUI.printConsoleInfo("Torrent file already in workspace.");
			}
			else
			{
				torrentUI.printConsoleInfo("Replacing old version of torrent file in workspace");
				Files.copy(sourceFile.toPath(), destinationFile.toPath());
			}
		}
		else
		{
			torrentUI.printConsoleInfo("Moving torrent file into workspace");
			Files.copy(sourceFile.toPath(), destinationFile.toPath());
		}
		
		TorrentFile torrentFile = new TorrentFile(this, descriptor, torrentFolder);
		torrentFiles.put(info_hash, torrentFile);
		
		torrentUI.printConsoleInfo("Successfully opened torrent file");
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

