package com.chenjy.test;
/**
 * Only do simple action "call"
 * not use any algorithm
 * used to checkout Tcp
 * 2015.5.22 
 */

import java.io.*;
import java.net.*;

public class game
{
	
	Socket s = null;						
	PrintWriter pw = null;				
	BufferedReader br = null;				
	private boolean isOnTable=false;	

	char[] cbuf = new char[300];		

	int cnt,number=0;

	static int myId;
	int myIndex;
	int[] bet;
	
	public static void main(String[] args) 
	{
		new game(args);	
		
	}

	public game(String[] str)
	{
		
		try{			
			SocketAddress hostAddress = new InetSocketAddress(
				InetAddress.getByName(str[2]), Integer.valueOf(str[3]));
			SocketAddress serverAddress = new InetSocketAddress(
				InetAddress.getByName(str[0]), Integer.valueOf(str[1]));
			s = new Socket();
			s.setReuseAddress(true);
			s.bind(hostAddress);
			s.connect(serverAddress);
		
			pw = new PrintWriter(s.getOutputStream(),true);									
			
			
			myId = Integer.valueOf(str[4]);
			
			
			
			//System.out.println("reg successfully!");

			isOnTable=true;
			pw.print("reg:"+str[4]+" Wolf"+" \n");
			pw.flush();
			
			while(isOnTable)
			{	
				String string = receiveMsg();
				if(string.contains("inquire"))
					{sendAction("call");cnt++;}
				else if(string.contains("game-over"))isOnTable=false;
			
			}
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}finally
		{
			shutDownResource();
		}

	}

	public String receiveMsg()
	{
		String info=null;
		int n=0;
		try {
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			while((n=br.read(cbuf))!=-1)
			{
				info=new String(cbuf,0,n);
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return info;
	}
	public void sendAction(int num)
	{
		pw.print("raise "+num+" \n");
		pw.flush();			
	}
	public void sendAction(String action)
	{
		pw.print(action+" \n");
		pw.flush();			
		
		if(action.equals("fold"))	
		{
			isOnTable = false;				
		}
	}

	public void shutDownResource()
	{
		try {
			s.close();					
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}









