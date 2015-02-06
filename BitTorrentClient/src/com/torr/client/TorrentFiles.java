package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.torr.ui.ITorrentUI;
import com.torr.utils.*;

public class TorrentFiles {
	ITorrentUI torrentUI;
	TCPServer tcpServer = new TCPServer();
	File mainFolder;
	WorkspaceManager wsm;
	
	public TorrentFiles(ITorrentUI torrentUI)
	{
		this.torrentUI = torrentUI;
		
		try
		{
			DoFileSystemBookKeeping();
		}
		catch(IOException ex)
		{
			// TODO: See what we'll do with this!!
		}
		//torrentUI.printConsoleInfo("Hello from TorrentFiles!");
	}
	
	public File GetWorkspaceFolder()
	{
		return wsm.GetWorkspaceFolder();
	}
	
	
	
	private void DoFileSystemBookKeeping() throws IOException
	{
		this.mainFolder = GetMainFolder();
		
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
	
	
	
	
}
