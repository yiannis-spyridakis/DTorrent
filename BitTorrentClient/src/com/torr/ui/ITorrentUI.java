package com.torr.ui;


public interface ITorrentUI {
	
	public void setStatusBarText(final String text);

	public void setFileName(final String text);
	
	public void setInfoHash(final String text);

	public void setNumberOfPieces(final String text);

	public void setDownloadedPieces(final String text);

	public void setDownloadSpeed(final int bps);

	public void setPeersNumber(final String text);	
	
	public void printConsoleInfo(final String text);
	
}
