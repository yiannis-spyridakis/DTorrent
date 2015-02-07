package com.torr.ui;

import java.util.concurrent.FutureTask;

// All methods must be thread safe
public interface ITorrentUI {
	
	public void Quit();
	
	public void Quit(Exception ex);
	
	public void SetStatusBarText(final String text);

	public void SetFileName(final String text);
	
	public void SetInfoHash(final String text);

	public void SetNumberOfPieces(final String text);

	public void SetDownloadedPieces(final String text);

	public void SetDownloadSpeed(final int bps);

	public void SetPeersNumber(final String text);	
	
	public void PrintConsoleInfo(final String text);
	
	public FutureTask<Integer> ShowMessageBox(final String message, final String title, final int options);
	
}
