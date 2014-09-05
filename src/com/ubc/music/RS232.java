package com.ubc.music;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.TimerTask;

import com.ubc.music.R;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

//RS232 handshaking with DE2

public class RS232 extends TimerTask
{
	private final int TIME_OUT_DELAY = 20; // *100 ms
	private final int MAX_SIZE_SEND_BUFFER = 5;
	private MyApplication app;
	private boolean wasConnected;
	private boolean justAttemptedConnection;
	private MainActivity main;
	private Menu menu;
	private	ProgressDialog progressDialog;
	private Object ReadWriteLock;
	private ArrayDeque<String> sendBuffer;
	private ArrayDeque<String> readBuffer;
	private boolean readingDone;
	private int timeOutCounter;

	public RS232(MyApplication app, MainActivity main)
	{
		this.main = main;
		this.app = app;
		this.wasConnected = false;
		this.justAttemptedConnection = false;
		this.ReadWriteLock = new Object();
		this.sendBuffer = new ArrayDeque<String>();
		this.readBuffer = new ArrayDeque<String>();
		this.setReadyToSend(true);
		this.timeOutCounter = 0;
	}


	public boolean addToSendBuffer(String msg)
	{
		boolean success = false;

		synchronized(ReadWriteLock)
		{
			if (this.isConnected() && sendBuffer.size() < MAX_SIZE_SEND_BUFFER)
			{
				this.sendBuffer.add(msg);
				success = true;
			}
			return success;
		}
	}

	public void setMenu(Menu menu)
	{
		this.menu = menu;
	}

	//Has tried connecting, used for setting connection bar
	public void setHasAttemptedConncetion(boolean status)
	{
		this.justAttemptedConnection = status;
	}

	//Checks if there is any time out, new connection, read from buffer, and then send commands
	public void run() 
	{	
		synchronized(ReadWriteLock)
		{
			monitorTimeOut();
			connectionUpdate();	
			readFromSocket();
			readPendingData();
			if(isReadyToSend()) 
				sendPendingCommands();
		}
	}

	//If DE2 has not sent back ready bit after the time out delay specified
	//Forcefully set it
	private void monitorTimeOut()
	{
		if(readBuffer.isEmpty())
		{
			timeOutCounter++;
			if(timeOutCounter == TIME_OUT_DELAY)
			{
				clearReadBuffer();
				setReadyToSend(true);
			}
		}
	}
	
	private void clearReadBuffer()
	{
		for(int ii = 0; ii < sendBuffer.size(); ii++)
		{
			sendBuffer.removeFirst();
		}
	}

	private boolean isReadyToSend()
	{
		return (this.readingDone == true);

	}

	private void setReadyToSend(boolean status)
	{
		this.readingDone = status;
		
	}

	private void sendPendingCommands()
	{

		if(this.sendBuffer.size() > 0 )
		{

			sendMessage(this.sendBuffer.removeFirst());
			setReadyToSend(false);
			timeOutCounter = 0;

		}
	}


	private void readPendingData()
	{

		if(this.readBuffer.size() > 0 )
		{
			decipherInputStream(this.readBuffer.getFirst());
			timeOutCounter = 0;
		}
	}

	//Change the status of connection bar
	public void connectionUpdate()
	{

		//If it just disconnected reset variable
		if(wasConnected && !this.isConnected())
		{
			wasConnected = false;
			changeConnectionBar(false);
		}	

		//If just attempted a connection, check if it was successful or not
		else if(justAttemptedConnection)
		{
			justAttemptedConnection = false;
			int maxPings = 12;
			int numPings = 0;

			//Check if has connected within 3 seconds
			try{
				makeProgressBar("Connecting");
				while(!this.isConnected() && numPings < maxPings)
				{	
					numPings++;
					Thread.sleep(250);		

				}

				if(numPings < maxPings)
				{				
					wasConnected = true;
					changeConnectionBar(true);
					makeProgressBar("Connection Successful!");
					delay(1500);
					closeProgressBar();

				}
				else
				{				
					makeProgressBar("Connection Timed out. Correct Settings and Try again");
					delay(1500);
					closeProgressBar();
				}
			}
			catch(Exception ex)
			{
				Log.i("cTest", "thread interrupted!");
			}

		}
	}

	private void changeConnectionBar(final boolean status)
	{
		main.runOnUiThread(new Runnable() {
			public void run(){
				final String img;
				if(status)
				{
					img = "full_connection";
					Log.i("cTest", "got the msg");
				}
				else
					img = "no_connection";

				MenuItem item = menu.findItem(R.id.connection_status);
				int did = main.getResources().getIdentifier(img,"drawable", main.getPackageName());													
				item.setIcon(did);
				main.onPrepareOptionsMenu(menu);
			}
		});

	}
	
	//Top level function to send command to DE2
	public boolean sendMessage(String msg) 
	{
		//only send if connected
		boolean success = false;

		if (this.isConnected())
		{
			success = true;
			byte buf[] = createMsgToSend(msg);
			sendMessageRS232(msg, buf);
		}	

		return success;
	}


	public boolean isConnected()
	{
		return (this.app.sock != null && this.app.sock.isConnected()
				&& !this.app.sock.isClosed());
	}

	//Convert string to byte stream
	private byte[] createMsgToSend(String msg)
	{

		// Create an array of bytes.  First byte will be the
		// message length, and the next ones will be the message

		byte buf[] = new byte[msg.length()];
		System.arraycopy(msg.getBytes(), 0, buf, 0, msg.length());

		return buf;
	}

	//Helper function to send byte stream to DE2
	private void sendMessageRS232(String msg, byte buf[])
	{
		// Send through the output stream of the socket

		OutputStream out;		
		try 
		{
			out = this.app.sock.getOutputStream();
			try
			{
				out.write(buf, 0, msg.length());
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}	
	}



	/**
	 * A TimerTask which reads from the socket connection
	 * every given interval.
	 * 
	 * Also checks if the input means anything
	 */
	public void readFromSocket()
	{

		if (app.sock != null && app.sock.isConnected() && !app.sock.isClosed()) {
			try {
				InputStream inputStream = app.sock.getInputStream();
				int bytesInPool = inputStream.available();
				if (bytesInPool > 0) {
					byte buf[] = new byte[bytesInPool];
					inputStream.read(buf);
					this.readBuffer.add(new String(buf, 0, bytesInPool, "US-ASCII"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}



	}

	//To decode what each byte received from DE2 means
	//And what to do next
	private void decipherInputStream(final String input)
	{
		
		final char flag = input.charAt(0);
		final char newSongTitle= 0x02;
		final char doneSong = 0x04;
		final char DONE_RECEIVING_FILE[] = {0x06};
		
		final char AMPLITUDE_LOW = 0x1e;
		final char AMPLITUDE_MED = 0x1f;
		final char AMPLITUDE_HIGH = 0x20;
		
		//Send to DE2
		final char DE2_DONE_SIGNAL_SONG = 0x05;
		final char DE2_DONE_SIGNAL_COMMAND = 0x06;

		
		main.runOnUiThread(new Runnable() {
			public void run() {
				
				final String inputData;
				final char index;
				
				switch(flag) //flag determines what the subsequent data means
				{
				//Send new file name and index to play list
				case(newSongTitle): 
					setReadyToSend(true);
					index = (char)(input.charAt(1) + 4);
					inputData = input.substring(2, input.length());
					sendBuffer.addFirst(new String(DONE_RECEIVING_FILE));
					main.getMusicFragment().addToPlaylist(inputData, index);
					break;
				
				//Song is done, move to next song play list
				case(doneSong):
					setReadyToSend(true);
					clearReadBuffer();
					main.getMusicFragment().setPlayingStatus(Constants.PLAY_NEXT);
					break;		
				
				
				case(DE2_DONE_SIGNAL_SONG):
					setReadyToSend(true);
					break;
					
				case(DE2_DONE_SIGNAL_COMMAND):
					setReadyToSend(true);
					if (input.length()>1) {
						try{
							char x = input.charAt(1);	
							main.getMusicFragment().resetSeekBar(x);
							main.getVisualFragment().resetBPM();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					break;
					
				case(AMPLITUDE_LOW):
					main.getVisualFragment().visualizeAmplitude("low");
					main.getVisualFragment().updateBPM(10);
					break;
					
				case(AMPLITUDE_MED):
					main.getVisualFragment().visualizeAmplitude("med");
					break;
					
				case(AMPLITUDE_HIGH):
					main.getVisualFragment().visualizeAmplitude("high");
					break;
				}
				
				if(readBuffer.size() > 0){
					readBuffer.removeFirst();
				}

			}
		});
	}

	//Connection progress dialog
	private void makeProgressBar(final String msg)
	{
		main.runOnUiThread(new Runnable() {
			public void run(){	
				if(progressDialog != null)
					progressDialog.dismiss();
				progressDialog = new ProgressDialog(main);
				progressDialog.setCancelable(true);
				progressDialog.setIndeterminate(true);
				progressDialog.setMessage(msg);
				progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				progressDialog.show();	
			}
		});

	}

	private void closeProgressBar()
	{
		main.runOnUiThread(new Runnable() {
			public void run(){	
				progressDialog.dismiss();
			}
		});
	}

	private void delay(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch(Exception ex)
		{
			Log.i("cTest", "thread interrupted!");
		}
	}

	public void popUpMsg(final String msg, final int X, final int Y)
	{
		main.runOnUiThread(new Runnable() {
			public void run(){

				CharSequence text = msg;
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(app, text, duration);
				toast.setGravity(Gravity.TOP|Gravity.LEFT, X, Y);
				toast.show();
			}
		});	
	}
}