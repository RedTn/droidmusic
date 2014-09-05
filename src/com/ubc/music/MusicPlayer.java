package com.ubc.music;

/*There is a read and send buffer, which is a dequeue of Strings
 *A command cannot be sent from the Android unless the Read buffer's
 *First element contains a special string from the DE2 that signals that it is ready to receive
 *The readBuffer is initialized with that string
 *Whenever a command is sent that special string is popped off
 *If the readBuffer is empty for the timeOutDelay specified below, the special string 
 *Will be automatically added into the readBuffer, allowing commands again.
 *Every signal received from the DE2 must have a special first character 
 *To distinguish it.
 */
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.util.Log;


public class MusicPlayer
{
	private RS232 rs232;

	public MusicPlayer(RS232 rs232)
	{
		this.rs232 = rs232;
	}

	//Sends to the index of the song to play next
	public boolean selectSong(int index){
		boolean isSuccessful = false;

		char bytesSelect[] = {0x01, 0x03, (char)index};
		String selectMsg = new String(bytesSelect);// + song;
		isSuccessful = rs232.addToSendBuffer(selectMsg);

		return isSuccessful;
	}
	
	public boolean selectSongNoClick(int index){
		boolean isSuccessful = false;

		char bytesSelect[] = {(char)index};
		String selectMsg = new String(bytesSelect);// + song;
		isSuccessful = rs232.addToSendBuffer(selectMsg);

		return isSuccessful;
	}
	
	/**
	 * Called when the user clicks the Play button 
	 * Sends the message to DE2 to say it is time to play
	 */
	public boolean play() {

		boolean isSuccessful = false;

		char bytesPlay[] = {0x01, 0x04};
		String playMsg = new String(bytesPlay);// + song;
		isSuccessful = rs232.addToSendBuffer(playMsg);

		return isSuccessful;
	} 

	/**
	 * Called when the user clicks the Play button 
	 * Sends the message to DE2 to say it is time to play
	 */
	public boolean playResume() {

		boolean isSuccessful = false;
		char bytes[] = {0x01, 0x04};
		String playResumeMsg = new String(bytes);// + song;
		isSuccessful = rs232.addToSendBuffer(playResumeMsg);	

		return isSuccessful;
	} 
	
	/**
	 * Called when the user clicks the Stop button 
	 */
	public boolean playStop() {
		char bytes[] = {0x01, 0x05};
		final String stopMsg = new String(bytes);
		return rs232.addToSendBuffer(stopMsg);	
	}
	
	public boolean playPause() {
		char bytes[] = {0x01, 0x06};
		final String pauseMsg = new String(bytes);
		return rs232.addToSendBuffer(pauseMsg);	
	}

	/**
	 * Called when the user clicks the Previous button 
	 */
	public boolean playPrevious() {
		char bytes[] = {0x01, 0x0E};
		final String previousMsg = new String(bytes);
		return rs232.addToSendBuffer(previousMsg);	
	}

	/**
	 * Called when the user clicks the Next button 
	 */
	public boolean playNext() {
		char bytes[] = {0x01, 0x0D};
		final String nextMsg = new String(bytes);
		return rs232.addToSendBuffer(nextMsg);	
	}

	/**
	 * Called when the user clicks the Volume Down button 
	 */
	public boolean volumeDown() {
		char bytes[] = {0x01, 0x10};
		final String previousMsg = new String(bytes);
		return rs232.addToSendBuffer(previousMsg);	
	}

	/**
	 * Called when the user clicks the Volume Up button 
	 */
	public boolean volumeUp() {
		char bytes[] = {0x01,0x0F};
		final String nextMsg = new String(bytes);
		return rs232.addToSendBuffer(nextMsg);	
	}
	
	/**
	 * Called when the user clicks the playlist Button
	 */
	public boolean refreshPlaylist() {
		char bytes[] = {0x01, 0x0C};
		final String nextMsg = new String(bytes);
		return rs232.addToSendBuffer(nextMsg);	
	}
	
	/**
	 * Called when the user clicks the Shuffle button 
	 */
	public boolean playShuffle() {
		char bytes[] = {0x01,0x11};
		final String shuffleMsg = new String(bytes);
		return rs232.addToSendBuffer(shuffleMsg);	
	}

	/**
	 * Called when the user clicks the Loop button 
	 */
	public boolean playLoop(char bytes[]) {
		final String loopMsg = new String(bytes);
		return rs232.addToSendBuffer(loopMsg);		
	}
	
	/**
	 * Called when the user clicks the clap button 
	 */
	public boolean playClap() {
		char bytes[] = {0x01,0x0E};
		final String clapMsg = new String(bytes);
		return rs232.addToSendBuffer(clapMsg);	
	}
	
	/**
	 * Called when the user clicks the bass button 
	 */
	public boolean playBass() {
		char bytes[] = {0x01,0x11, 0x01};
		final String bassMsg = new String(bytes);
		return rs232.addToSendBuffer(bassMsg);	
	}

	/**
	* Permutation function for shuffle function for the playlist
	*/
	public List<Integer> permutate(List<Integer> list) {
		Integer[] sourceArray = (Integer[])list.toArray();
		Integer[] shuffledCopy = new Integer[list.size()];
		shuffledCopy[0] = sourceArray[0]; //Copying the first element of the original array to an empty array.
		Random rand = new Random(System.currentTimeMillis());
		int j;
	    for (int i= 1; i< list.size(); i++)    
	    {   
	        j = rand.nextInt(i) ; // creates a random number less than  i
	        shuffledCopy[i] = shuffledCopy[j]; //use j as the random index for shuffledCopy and put the element at the index to i.
	        shuffledCopy[j] = sourceArray[i];  //fill the index j with an element from sourceArray at index i.
	    }        
	   return Arrays.asList(shuffledCopy);
	}
}