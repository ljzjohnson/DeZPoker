package com.chenjy.test.revise;

import java.util.*;

/**
 * 2015.5.27
 * @author Chenjy
 * 八个线程
 *
 */

class CalcAction extends Thread
{
	
	private ArrayList<Poker> apoker= null;
	
	Poker poker = null;
	
	Data1 data1 = new Data1();
	Data2 data2 = new Data2();
	Data3 data3 = new Data3();
	
	int ourRank;				//根据我的两张手牌及公共牌计算我的排名
	int[] myCards = null;		//提取出自己的手牌	
	int[] boardCards = null;	//提取出公共牌
	int[][] pCard = null;				//根据提供的五张牌算出可能的任意两张组合
	
	public static void main(String[] args)
	{
		CalcAction c=new CalcAction();
 
		c.apoker = new ArrayList<Poker>();

		Poker poker=null;
		//测试时间
		poker = new Poker("A");
		poker.setColor("S");
		c.apoker.add(poker);
		poker = new Poker("K");
		poker.setColor("S");
		c.apoker.add(poker);
		poker = new Poker("J");
		poker.setColor("S");
		c.apoker.add(poker);
		poker = new Poker("Q");
		poker.setColor("S");
		c.apoker.add(poker);
		poker = new Poker("T");
		poker.setColor("S");
		c.apoker.add(poker);

		
		
	
		
		long startTime = System.currentTimeMillis();//获取当前时间
		
		c.setInitial(c.apoker);
		float s=c.EHS();
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("程序运行时间："+(endTime-startTime)+"ms");
		
		System.out.println("EHS："+s);

	}	
	
	private static final int[] pokerToken={
			69634,73730,81922,98306,135427,139523,147715,164099,266757,270853,279045,295429,529159,
			533255,541447,557831,1053707,1057803,1065995,1082379,2102541,2106637,2114829,2131213,4199953,
			4204049,4212241,4228625,8394515,8398611,8406803,8423187,16783383,16787479,16795671,16812055,
			33560861,33564957,33573149,33589533,67115551,67119647,67127839,67144223,134224677,134228773,
			134236965,134253349,268442665,268446761,268454953,268471337};

	private static final short[][] pokerCombine={
		{5,4,3,2,1},{5,4,3,2,0},{5,4,3,1,0},{5,4,2,1,0},{5,3,2,1,0},{4,3,2,1,0},{6,4,3,2,1},
		{6,4,3,2,0},{6,4,3,1,0},{6,4,2,1,0},{6,3,2,1,0},{6,5,3,2,1},{6,5,3,2,0},{6,5,3,1,0},
		{6,5,2,1,0},{6,5,4,2,1},{6,5,4,2,0},{6,5,4,1,0},{6,5,4,3,1},{6,5,4,3,0},{6,5,4,3,2}};
	
	public CalcAction()
	{	
		this.apoker = new ArrayList<Poker>(7);
	}
	public void setInitial(ArrayList<Poker> aPoker)
	{
		this.apoker = aPoker;
		this.myCards = aListToArray(aPoker,0,2);		//提取出自己的手牌		
		this.boardCards = aListToArray(aPoker,2,3);	//提取出公共牌
		this.ourRank=rank(this.myCards,this.boardCards);
		this.pCard = possibleCard(aPoker);
	}
	public float EHS()
	{
		float hs = handStrength();
		float ppot = handPotential();		
		return hs+(1-hs)*ppot;
	}
	public float handStrength()	//计算当前牌力
	{
		int ahead=0;	//领先的次数
		int tied=0;		//相当的次数
		int behand=0;	//落后的次数
		int opporientRank=0;	//对方手牌与公共牌形成组合的评分		
		
		for(int i=0;i<1081;i++)			//计算所有对方可能的手牌
		{
			opporientRank=rank(pCard[i],boardCards);

			if(this.ourRank<opporientRank) ahead+=1;
			else if(this.ourRank==opporientRank) tied+=1;
			else behand+=1;
		}
		return (ahead+tied/2.0f)/(ahead+tied+behand);
	}

	public float handPotential()	//计算当前扑克正潜力
	{
		int[][] hp = new int[4][3];
		
		int tt[]=new int[2];
		int opporientRank,ourBest,opporientBest=0;
		int index=0;
		
		HandPotential thread1=new HandPotential(pCard,130,160);
		HandPotential thread2=new HandPotential(pCard,290,160);
		HandPotential thread3=new HandPotential(pCard,450,160);
		HandPotential thread4=new HandPotential(pCard,610,150);
		HandPotential thread5=new HandPotential(pCard,760,150);
		HandPotential thread6=new HandPotential(pCard,910,170);

		thread1.start();
		thread2.start();
		thread3.start();
		thread4.start();
		thread5.start();
		thread6.start();
		
		for(int i=0;i<130;i++)			//计算对方所有可能的手牌:47*46/2 = 1081   分解计算
		{		
			if(isContained(pCard[i][0], apoker) || isContained(pCard[i][1], apoker)) continue;
			ourBest=rank(pCard[i][0], pCard[i][1], apoker);
			for(int j=51;j>0;j--)
			{							
				if(isContained(j,apoker ,pCard[i]))continue;
				for(int k=0;k<j;k++)
				{
					if(isContained(k,apoker, pCard[i]))continue;				
					tt[0]=pokerToken[j];tt[1]=pokerToken[k];
					opporientRank = rank(tt,boardCards);
					
					if(this.ourRank<opporientRank) index=0;
					else if(this.ourRank==opporientRank) index=1;
					else index=2;
					
					opporientBest=rank(pokerToken[j],pokerToken[k],apoker,pCard[i]);
					
					if(ourBest<opporientBest)
						hp[index][0]++;
					else if(ourBest==opporientBest)
						hp[index][1]++;
					else 
						hp[index][2]++;
					
					hp[3][index]++;			
					
				}
			
			}		

		}	
		try{
			while(!thread1.isCalcOver() || !thread2.isCalcOver() || !thread3.isCalcOver() ||
					!thread4.isCalcOver() || !thread5.isCalcOver() || !thread6.isCalcOver())
				Thread.sleep(100);
		}
		catch(InterruptedException e){}	
		
		for(int i=0;i<4;i++)
			for(int j=0;j<3;j++)
			{
				hp[i][j]=hp[i][j]+thread1.getHp()[i][j]+thread2.getHp()[i][j]
						+thread3.getHp()[i][j]+thread4.getHp()[i][j]
								+thread5.getHp()[i][j]+thread6.getHp()[i][j];
			}
		

		
		return ( (hp[2][0] + hp[2][1]/2.0f + hp[1][0]/2.0f)
					/ (hp[3][2] + hp[3][1]+hp[3][0]) );	//由落后到领先的概率
	}
	//将ArrayList中指定位置的手牌取出来，转换成数组
	public int[] aListToArray(ArrayList<Poker> aPoker,int start,int len)
	{
		int cnt = 0;
		int[] tab = new int[len];
		for(int i=start;i<start+len;i++)
		{	
			tab[cnt++]=pokerToken[aPoker.get(i).getIndex()];
		}
		return tab;
	}

	public  int rank(int[] privateCards, int[] boardCard)
	{
				
		int r = privateCards[0] & privateCards[1] & boardCard[0] & boardCard[1] & boardCard[2] & 0xf000; 
		int q = (privateCards[0] | privateCards[1] | boardCard[0] | boardCard[1] | boardCard[2])>>16;
		//System.out.println("r="+r+"	"+"q="+q);
		if(r!=0)return(data1.rankTable[q]);					//能够形成顺子或同花
		else if(data1.rankTable2[q]!=0)return data1.rankTable2[q];
		else {
			int p = (privateCards[0] & 0xff)*(privateCards[1] & 0xff)*(boardCard[0] & 0xff)*(boardCard[1] & 0xff)*(boardCard[2] & 0xff);
			//System.out.println(binarySearch(data3.products,p,0,data3.products.length-1)+" ");
			return data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
		}
	}
	//重载一个方法，用来计算七张牌的情形
	//？？？七张牌如何计算
	public int rank(int a,int b,ArrayList<Poker> aPoker)
	{
		int[] tab = new int[7];
		int r,q,p;
		int temp=0;
		int min ;
		for(int i=0;i<5;i++)
		{
			tab[i]=pokerToken[aPoker.get(i).getIndex()];
		}
		tab[5]=a;
		tab[6]=b;
		
		r = (tab[pokerCombine[0][0]] & tab[pokerCombine[0][1]] & tab[pokerCombine[0][2]] & tab[pokerCombine[0][3]] & tab[pokerCombine[0][4]])& 0xf000; 
		q = (tab[pokerCombine[0][0]] | tab[pokerCombine[0][1]] | tab[pokerCombine[0][2]] | tab[pokerCombine[0][3]] | tab[pokerCombine[0][4]])>>16;
		if(r!=0)min=(data1.rankTable[q]);					//能够形成顺子或同花
		else if(data1.rankTable2[q]!=0)min=data1.rankTable2[q];
		else {
			p = (tab[pokerCombine[0][0]] & 0xff)*(tab[pokerCombine[0][1]] & 0xff)*(tab[pokerCombine[0][2]] & 0xff)*(tab[pokerCombine[0][3]] & 0xff)*(tab[pokerCombine[0][4]] & 0xff);
	
			min = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
		}
		
		for(int j=1;j<21;j++)
		{
			r = (tab[pokerCombine[j][0]] & tab[pokerCombine[j][1]] & tab[pokerCombine[j][2]] & tab[pokerCombine[j][3]] & tab[pokerCombine[j][4]])& 0xf000; 
			q = (tab[pokerCombine[j][0]] | tab[pokerCombine[j][1]] | tab[pokerCombine[j][2]] | tab[pokerCombine[j][3]] | tab[pokerCombine[j][4]])>>16;
			if(r!=0)temp=(data1.rankTable[q]);					//能够形成顺子或同花
			else if(data1.rankTable2[q]!=0)temp=data1.rankTable2[q];
			else {
				p = (tab[pokerCombine[j][0]] & 0xff)*(tab[pokerCombine[j][1]] & 0xff)*(tab[pokerCombine[j][2]] & 0xff)*(tab[pokerCombine[j][3]] & 0xff)*(tab[pokerCombine[j][4]] & 0xff);
				temp = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
			}
			if(temp<min)min=temp;
		}
		return temp;
	}
	public int rank(int a,int b,ArrayList<Poker> aPoker,int[] pcard)
	{
		int[] tab = new int[7];
		int temp=0;
		int p,q,r;
		int min;
		tab[0] = pcard[0];
		tab[1] = pcard[1];
		for(int i=2;i<5;i++)
		{
			tab[i]=pokerToken[aPoker.get(i).getIndex()];
		}
		tab[5]=a;
		tab[6]=b;
		r = (tab[pokerCombine[0][0]] & tab[pokerCombine[0][1]] & tab[pokerCombine[0][2]] & tab[pokerCombine[0][3]] & tab[pokerCombine[0][4]])& 0xf000; 
		q = (tab[pokerCombine[0][0]] | tab[pokerCombine[0][1]] | tab[pokerCombine[0][2]] | tab[pokerCombine[0][3]] | tab[pokerCombine[0][4]])>>16;
		if(r!=0)min=(data1.rankTable[q]);					//能够形成顺子或同花
		else if(data1.rankTable2[q]!=0)min=data1.rankTable2[q];
		else {
			p = (tab[pokerCombine[0][0]] & 0xff)*(tab[pokerCombine[0][1]] & 0xff)*(tab[pokerCombine[0][2]] & 0xff)*(tab[pokerCombine[0][3]] & 0xff)*(tab[pokerCombine[0][4]] & 0xff);
	
			min = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
		}
		for(int j=1;j<21;j++)
		{
			r = (tab[pokerCombine[j][0]] & tab[pokerCombine[j][1]] & tab[pokerCombine[j][2]] & tab[pokerCombine[j][3]] & tab[pokerCombine[j][4]])& 0xf000; 
			q = (tab[pokerCombine[j][0]] | tab[pokerCombine[j][1]] | tab[pokerCombine[j][2]] | tab[pokerCombine[j][3]] | tab[pokerCombine[j][4]])>>16;
			if(r!=0)temp=(data1.rankTable[q]);					//能够形成顺子或同花
			else if(data1.rankTable2[q]!=0)temp=data1.rankTable2[q];
			else {
				p = (tab[pokerCombine[j][0]] & 0xff)*(tab[pokerCombine[j][1]] & 0xff)*(tab[pokerCombine[j][2]] & 0xff)*(tab[pokerCombine[j][3]] & 0xff)*(tab[pokerCombine[j][4]] & 0xff);
				temp = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
				return data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
			}
			if(temp<min)min=temp;
		}
		return temp;
	}
	//判断指定元素在之前是否出现过
	public  boolean isContained(int a,ArrayList<Poker> aPoker)
	{
		boolean flag = false;
		for(int i=0;i<aPoker.size();i++)
		{
			if(aPoker.get(i).getIndex()==a)
			{
				flag = true;
				break;
			}
		}
		return flag;
	}
	//判断指定元素在之前是否出现过
	public  boolean isContained(int a,ArrayList<Poker> aPoker,int []t)
		{
			boolean flag = false;
			for(int i=0;i<aPoker.size();i++)
			{
				if(aPoker.get(i).getIndex()==a)
				{
					flag = true;
					break;
				}
			}
			if(!flag)
				if(t[0]==pokerToken[a])flag=true;
				else if(t[1]==pokerToken[a])flag=true;
				else flag= false;
			
			return flag;
		}
	//做一个函数生成除已出现牌之外的两张组合
	public  int[][] possibleCard(ArrayList<Poker> aPoker)
	{
		int[][] tab = new int[1081][2];
		int cnt = 0;		
	   for(int i=51;i>0;i--)		//j,k均是纸牌的索引，因此唯一
		{							//调试注意：两张纸牌唯一add by me！
			if(isContained(i,aPoker))continue;
			for(int j=0;j<i;j++)
			{
				if(isContained(j,aPoker))continue;
				tab[cnt][0] = pokerToken[i];
				tab[cnt][1] = pokerToken[j];
				cnt++;
			}
		}
		return tab;
	}
	public int binarySearch(int[] dataset,int data,int beginIndex,int endIndex){    
	       int midIndex = (beginIndex+endIndex)/2;    
	       if(data <dataset[beginIndex]||data>dataset[endIndex]||beginIndex>endIndex){  
	           return -1;    
	       }  
	       if(data <dataset[midIndex]){    
	           return binarySearch(dataset,data,beginIndex,midIndex-1);    
	       }else if(data>dataset[midIndex]){    
	           return binarySearch(dataset,data,midIndex+1,endIndex);    
	       }else {    
	           return midIndex;    
	       }    
	   }
	


	class HandPotential extends Thread{
		

			
		private int start,length ;
		private int pCard[][]=null;
		private int hp[][]=new int[4][3];
		boolean calcOver;
		
		public HandPotential(int[][] card,int start, int length)
		{
			this.pCard=card;
			this.start = start;
			this.length=length;
		}
		
		public int[][] getHp() {
			return hp;
		}

		public boolean isCalcOver() {
			return calcOver;
		}

		public void run(){
			
//			long startTime = System.currentTimeMillis();//获取当前时间
			
			this.calcOver=false;
			int tt[]=new int[2];
			int opporientRank,ourBest,opporientBest=0;
			int index=0;
		
			ourRank=rank(myCards,boardCards);	
			
			for(int i=start;i<start+length-1;i++)			//计算对方所有可能的手牌:47*46/2 = 1081     转牌河牌
			{	
				if(isContained(pCard[i][0], apoker) || isContained(pCard[i][1], apoker)) continue;
				ourBest=rank(pCard[i][0], pCard[i][1], apoker);	
				
				for(int j=51;j>0;j--)
				{							
					if(isContained(j,apoker ,pCard[i]))continue;
					for(int k=0;k<j;k++)
					{
						if(isContained(k,apoker, pCard[i]))continue;				
						tt[0]=pokerToken[j];tt[1]=pokerToken[k];
						opporientRank = rank(tt,boardCards);
						
						if(ourRank<opporientRank) index=0;
						else if(ourRank==opporientRank) index=1;
						else index=2;
						
						
						opporientBest=rank(pokerToken[j],pokerToken[k],apoker,pCard[i]);
						
						if(ourBest<opporientBest)
							hp[index][0]++;
						else if(ourBest==opporientBest)
							hp[index][1]++;
						else 
							hp[index][2]++;
						
						hp[3][index]++;			
						
					}
				
				}		
	
			}
			this.calcOver = true;
//			long endTime = System.currentTimeMillis();
//			
//			System.out.println("程序运行时间："+(endTime-startTime)+"ms");
		}
		

		
	}

}