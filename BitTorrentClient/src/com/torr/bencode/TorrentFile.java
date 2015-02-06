package com.torr.bencode;

import java.util.Vector;


/**
 * A class used for storing the metadata of a .torrent file.
 * @author Robert S. Moore II
 */
public class TorrentFile
{
	/**
	 * Αποθηκεύει το URL του tracker ως HTTP-escaped String.
	 */
	public String tracker_url;
	
	/**
	 * Αποθηκεύει το SHA-1 hash των bencoded 'info' dictionary ως ένα 20-byte array.
	 */
	public byte[] info_hash_as_binary;
	
	/**
	 * Αποθηκεύει το SHA-1 hash των bencoded 'info' dictionary ως 40 hex digits σε ASCII.
	 */
	public String info_hash_as_hex;
	
	/**
	 * Αποθηκεύει το SHA-1 hash των bencoded 'info' dictionary ως HTTP-escaped string.
	 */
	public String info_hash_as_url;
	
	/**
	 * Ο αριθμός των bytes στο αρχείο για ένα single-file .torrent
        */
	public int file_length;
	
	/**
	 * Το μέγερος του κάθε κομματιού του αρχείου καθώς κατακερματίζεται απο τον tracker.
	 * Το τελευταίο κομμάτι του αρχείου μπορεί να είναι μικρότερο στην περίπτωση που το μέγεθος του αρχείου
         * σε bytes δεν είναι πολλαπλάσιο αυτής της τιμής.
	 */
        
	public int piece_length;
	
	/**
	 * Η συλλογή απο SHA-1 hash τιμές για κάθε κομμάτι του αρχείου, αποθηκεύεται ως byte arrays.
	 */
	public Vector piece_hash_values_as_binary;
	
	/**
	 * Η συλλογή απο SHA-1 hash τιμές για κάθε κομμάτι του αρχείου, αποθηκεύεται ως Strings of hexadecimal digits.
	 */
	public Vector piece_hash_values_as_hex;
	
	/**
	 * Η συλλογή απο SHA-1 hash τιμές για κάθε κομμάτι του αρχείου, αποθηκεύεται ως Strings of HTTP-escaped characters.
	 */
	public Vector piece_hash_values_as_url;
	
	/**
	 * Δημιουργεί εξ αρχής ένα TorrentFile object με άδεια πεδία.
	 *
	 */
	public TorrentFile()
	{
		super();
		tracker_url = new String(); //συγκεκριμενο url περασμένο απο το μTorrent
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
