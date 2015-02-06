package com.torr.bencode;

import java.util.Vector;


/**
 * A class used for storing the metadata of a .torrent file.
 * @author Robert S. Moore II
 */
public class TorrentFile
{
	/**
	 * ���������� �� URL ��� tracker �� HTTP-escaped String.
	 */
	public String tracker_url;
	
	/**
	 * ���������� �� SHA-1 hash ��� bencoded 'info' dictionary �� ��� 20-byte array.
	 */
	public byte[] info_hash_as_binary;
	
	/**
	 * ���������� �� SHA-1 hash ��� bencoded 'info' dictionary �� 40 hex digits �� ASCII.
	 */
	public String info_hash_as_hex;
	
	/**
	 * ���������� �� SHA-1 hash ��� bencoded 'info' dictionary �� HTTP-escaped string.
	 */
	public String info_hash_as_url;
	
	/**
	 * � ������� ��� bytes ��� ������ ��� ��� single-file .torrent
        */
	public int file_length;
	
	/**
	 * �� ������� ��� ���� ��������� ��� ������� ����� ���������������� ��� ��� tracker.
	 * �� ��������� ������� ��� ������� ������ �� ����� ��������� ���� ��������� ��� �� ������� ��� �������
         * �� bytes ��� ����� ����������� ����� ��� �����.
	 */
        
	public int piece_length;
	
	/**
	 * � ������� ��� SHA-1 hash ����� ��� ���� ������� ��� �������, ������������ �� byte arrays.
	 */
	public Vector piece_hash_values_as_binary;
	
	/**
	 * � ������� ��� SHA-1 hash ����� ��� ���� ������� ��� �������, ������������ �� Strings of hexadecimal digits.
	 */
	public Vector piece_hash_values_as_hex;
	
	/**
	 * � ������� ��� SHA-1 hash ����� ��� ���� ������� ��� �������, ������������ �� Strings of HTTP-escaped characters.
	 */
	public Vector piece_hash_values_as_url;
	
	/**
	 * ���������� �� ����� ��� TorrentFile object �� ����� �����.
	 *
	 */
	public TorrentFile()
	{
		super();
		tracker_url = new String(); //������������ url ��������� ��� �� �Torrent
		piece_hash_values_as_binary = new Vector();
		piece_hash_values_as_url = new Vector();
		piece_hash_values_as_hex = new Vector();
		info_hash_as_binary = new byte[20];
		info_hash_as_url = new String();
		info_hash_as_hex = new String();
		file_length = -1;
		piece_length = -1;
	}	
}
