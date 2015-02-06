package com.torr.client;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class WorkspaceManager implements AutoCloseable {
	
	private static final String FILE_PREFIX = "BT";
	private RandomAccessFile raf;
	private File workspaceFolder;
	
	public WorkspaceManager(File programFolder) throws IOException
	{
		workspaceFolder = GetWorkspace(programFolder);
		
		if(workspaceFolder != null)
		{
			Path instanceFolderPath = Paths.get(workspaceFolder.getPath());
			
			// Create and lock the lock file
			Path lockFilePath = instanceFolderPath.resolve(Consts.PROCESS_LOCK_FILE_NAME);
			File lockFile = lockFilePath.toFile();
			lockFile.createNewFile();	
			
			// Will throw if the file wasn't created successfully
			// The file is locked for as long as the process is running
			raf = new RandomAccessFile(lockFilePath.toFile(), "rw");
		}
	}
	
	
	public File GetWorkspaceFolder()
	{
		return this.workspaceFolder;
	}
	
	private File GetWorkspace(File programFolder) throws IOException
	{
		File ret = CheckForEmptySlots(programFolder);
		if(ret == null)
		{
			ret = CreateWorkspace(programFolder);
		}
		
		return ret;
	}
	
	private File CheckForEmptySlots(File programFolder)
	{
		File[] files = programFolder.listFiles();
		//File file;
		for(File file : files)
		{
			if(file.isDirectory())
			{			
				if(file.getName().startsWith(FILE_PREFIX))
				{
					Path lockFilePath = Paths.get(file.getPath())
							.resolve(Consts.PROCESS_LOCK_FILE_NAME);
					File lockFile = lockFilePath.toFile();
					
					if(!lockFile.exists() || lockFile.delete())
					{
						return file;
					}
				}
			}
		}		
		
		return null;
	}
	
	private File CreateWorkspace(File programFolder) throws IOException
	{
		// Create new folder for the instance
		UUID test = UUID.randomUUID();
		String uuid = new Long(Math.abs(test.getMostSignificantBits())).toString();
		Path newFolderPath = Paths.get(programFolder.getPath()).resolve(FILE_PREFIX + uuid);
		
		workspaceFolder = newFolderPath.toFile();
		workspaceFolder.mkdir();	
				
		return null;
	}
	
	
	@Override
	public void close()
	{
		try
		{
		if(raf != null)
			raf.close();
		}
		catch(IOException ex)
		{
			// Not much to do here
		}
	}
	
	//private void Delete
}
