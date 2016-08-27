/**
 * @version 3.0.1_0530_Alpha
 * basic function have checkout
 * basic bug no exist
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;



public class game
{
	
	Socket s = null;						
	PrintWriter pw = null;				
	BufferedReader br = null;				
	private boolean isOnTable=false;	
	char[] cbuf = new char[4096];		//readbuffer
	
	short TotalContest = 0;
	
	Poker poker = null;
	Player player = null;
	InPlayer inPlayer =null;
	CalcAction calc = null;
	CalcInquire calcInq =null;

	int cnt=0;
	String msg = new String();
	
	ArrayList<Poker> plist= null;
	ArrayList<Player> alist= null;
	ArrayList<InPlayer> iplist= null;
	
	//fold:2/10 call:3/10 chack:4/10
	private static final String[] ActionTab1 ={
		"call","call","fold","call","check","fold","call","check","fold","call"
	};
	// call:6/10 chack:4/10
	private static final String[] ActionTab2={
		"fold","call","call","call","check","check","call","fold","check","call"
	};
	
	
	String myId;
	int totalPot;
	int myIndex;		//My seat number
	int[] bet;
	int calc2Card=0;
	float handPotential=0;
	float handStrength=0;
	int cntFollowNum = 0;
	
	public String msgQueue[]=new String[10];
	public int frontMsgQueue,rearMsgQueue=0;
	boolean isInquire = false;
	String ImBlind ="0";
	int totalPlayer = 0;
	
	boolean isSingleOver = false;
	String msgType = null;
	
	//test actionmsg,will not use when contest
	BufferedWriter writer = null;
	BufferedWriter writer2 = null;	
	String sendMsgAction = "";		
	
	public static void main(String[] args) 
	{
		new game(args);	
		
	}

	public game(String[] str)
	{
		this.plist = new ArrayList<Poker>(7);
		this.iplist = new ArrayList<InPlayer>(8);
		this.alist = new ArrayList<Player>(8);
		
		this.bet = new int[2];
		
		calc = new CalcAction();
		calcInq=new CalcInquire();
		
		try{			
			SocketAddress hostAddress = new InetSocketAddress(
				InetAddress.getByName(str[2]), Integer.valueOf(str[3]));
			SocketAddress serverAddress = new InetSocketAddress(
				InetAddress.getByName(str[0]), Integer.valueOf(str[1]));
			s = new Socket();
			s.setReuseAddress(true);
			s.bind(hostAddress);
			s.connect(serverAddress);
		
			writer=new BufferedWriter(new FileWriter("runInformation.txt"));	// log runinformation
			writer2=new BufferedWriter(new FileWriter("receiveInformation.txt"));	// log runinformation
			
			pw = new PrintWriter(s.getOutputStream(),true);			
			
			myId = str[4];
			
			isOnTable=true;	
			pw.print("reg:"+str[4]+" Wolf"+" \n");
			pw.flush();
			
			
			while(isOnTable)
			{	
				msg = receiveMsg();

				if(msg!=null)splitMsg(msg);
				else isOnTable = false;
				
				while(frontMsgQueue!=rearMsgQueue)
				{
					parse(msgQueue[frontMsgQueue++%msgQueue.length]);
					
					if(isInquire)
					{
						calc.setInitial(this.plist);	//set initial when receive inquireMsg
						switch(plist.size())
						{
						case 2:cntFollowNum++;
							   calc2Card = calc.calc2Poker();
							//if I'm bigblind,and no allin exist ,the first time should not fold
							if(cntFollowNum<2 && ImBlind.contains("bb") && (!calcInq.isAllinExist(iplist)))
							{
								if(calc2Card>5){
									sendAction("check");System.out.println("check !");
								}else {
									sendAction("fold");System.out.println("fold !");
								}
							}
							//if I'm smallblind,and bigblind exists and no allin exist ,
								//if my poker isn't very bad,the first time should not fold
							else if(cntFollowNum<2 && totalPlayer>2 && ImBlind.contains("sb") && (!calcInq.isAllinExist(iplist)))
							{
								if(calc2Card>=7){
									sendAction("check");
									System.out.println("check !");
								}else{
									sendAction("fold");
									System.out.println("fold !");
								}
							}
							//or too many player choose fold,i will not fold
							else if(calcInq.getFoldNumber(iplist)>(0.6*totalPlayer)){
								sendAction("check");System.out.println("check !");
							}//可以在一个判断9的界限，check
							//else if less 3 player left,i will more initiative
							else if(totalPlayer<3){
								if(calc2Card>=12){sendAction("call");System.out.println("call!");}
								else if(calc2Card>=5){
									sendAction("check");
									System.out.println("check");
								}
								else {
									sendAction("fold");
									System.out.println("fold !");
								}
							}
							else{
								if(calc2Card>=15){sendAction("call");System.out.println("call!");}
								else if(calc2Card>=12){
									sendAction("check");
									System.out.println("check");
								}
								else {
									sendAction("fold");
									System.out.println("fold !");
								}
							}
							break;
						case 5:
							{	handStrength = calc.handStrength();
								//if all player choose fold,i will not fold
								if(totalPlayer-calcInq.getFoldNumber(iplist) <2){
									sendAction("check");System.out.println("check !");
								}
								//else i will make decision depend on my poker
								// if all_in exist , my handstrength is 1,i will call
								else if(calcInq.isAllinExist(iplist))
								{
									if(handStrength>=1){sendAction("call");System.out.println("all_in!");}
									else {sendAction("fold");System.out.println("fold !");}
								}else{
									if(totalPlayer<3){									
										if(handStrength>=1){sendAction("all_in");System.out.println("all_in!");}
										else if(handStrength>0.98){sendAction(40);System.out.println("raise 40!");}
										else if(handStrength>0.95){sendAction("call");System.out.println("call");}
										else if(handStrength>0.70){sendAction("check");System.out.println("check");}
										else {sendAction("fold");System.out.println("fold !");}
									}
									else{
										if(handStrength>=1){sendAction("all_in");System.out.println("all_in!");}
										else if(handStrength>0.98){sendAction("call");System.out.println("call");}
										else if(handStrength>0.80){sendAction("check");System.out.println("check");}
										else {sendAction("fold");System.out.println("fold !");}
									}
								}
							}break;
						case 6:{
							handStrength = calc.calc6Poker();
							if(totalPlayer<3){									
								if(handStrength>=1){sendAction("all_in");System.out.println("all_in!");}
								else if(handStrength>0.98){sendAction(40);System.out.println("raise 40!");}
								else if(handStrength>0.85){sendAction("call");System.out.println("call");}
								else if(handStrength>0.75){sendAction("check");System.out.println("check");}
								else {sendAction("fold");System.out.println("fold !");}
							}
							else{
								if(handStrength>=1){sendAction("all_in");System.out.println("all_in!");}
								else if(handStrength>0.95){sendAction("call");System.out.println("call");}
								else if(handStrength>0.80){sendAction("check");System.out.println("check");}
								else {sendAction("fold");System.out.println("fold !");}
							}
							}
							break;
						case 7:{
							handStrength = calc.calc7Poker();
								if(handStrength>=1){
									sendAction("call");System.out.println("call !");
								}else if(handStrength>0.98){sendAction("call");System.out.println("call");}
								else if(handStrength>0.90){sendAction("check");System.out.println("check");}
								else {sendAction("fold");System.out.println("fold !");}			
							}//再细分一下，0.95/0.97这样
							break;
							
						}
						isInquire = false;
												
					}
					else if(isSingleOver)
					{
						print();
						isSingleOver=false;
					}
				}
			}
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}finally
		{
			try {
				writer2.close();
				writer.close();
			} catch (Exception e2) {
			}
			shutDownResource();
		}

	}
	

	//print PokerCard information & Player information
	//write to disk
	public void print() throws IOException
	{
		 int num=0;
		 writer.write("/**********************new game "+TotalContest+"********************************");
		 writer.newLine();writer.flush();
		 for(int i=0;i<plist.size();i++)
		 {
			num++;				
			writer.write("Poker "+num+":"+"point-"+plist.get(i).getPoint()+"	color-"+plist.get(i).getColor());
			writer.newLine();writer.flush();						
		 }
		 writer.write("calc2Card:"+calc2Card+"	handStrength:"+handStrength);
		 writer.newLine();writer.flush();
		 writer.write("SendAction:"+sendMsgAction);
		 writer.newLine();writer.flush();
		 
		 
		 handStrength = 0;
		 calc2Card  = 0;
		 sendMsgAction="";
		 ImBlind = "0";
		 cntFollowNum = 0;
		 
		plist.clear();			//clear ArrayList<Poker>
		alist.clear();			//clear ArrayList<Player>
		iplist.clear();			//clear ArrayList<InPlayer>
		
	}
	public void print(String intN) throws IOException
	{
		 writer2.write("inquire info player size:"+intN);
		 writer2.newLine();writer.flush();
	}	
	public void splitMsg(String info){
		String indexs="";
		String index[]=null;
		
		for(int i=0; i<info.length(); i++){ 
			if(info.charAt(i)=='/') {
				if( (i>7 && info.substring(i-6, i).compareTo("common")==0)
						|| (i<info.length()-7 && info.substring(i+1,i+7).compareTo("common" )==0) )
					continue;
				else
					indexs+=i+" ";
			}
		}
		index=indexs.split(" ");

		index[0]="0";
		for(int i=1;  i<index.length; i++){			
			if(i%2==1)
				index[i]=Integer.toString(info.indexOf("\n",Integer.parseInt(index[i])));
			else
				index[i]=Integer.toString(info.indexOf("\n", Integer.parseInt(index[i-1]) ) );
		}

		if(index.length==2 || (info.split(" \n",2)[0]=="showdown-msg" && index.length==4) )
			msgQueue[rearMsgQueue++%msgQueue.length]=info;
		else{		
			int begin,end=0;
			for(int i=0; i<index.length-1; ){
				begin= (i==0?Integer.parseInt(index[i++]) : Integer.parseInt(index[i++])+1);
				end=Integer.parseInt(index[i++]);	
				msgQueue[rearMsgQueue++%msgQueue.length]=info.substring(begin, end);		
			}					
		}	
	}
	public void parse(String s)
	{
		String regex="";
		Pattern p=null;
		Matcher m=null;
		String str="seat/game-overblind/hold/inquire/flop/turn/river/showdown/pot-win/";
		int cc=str.indexOf(s.split(" \n",2)[0]); 
		switch(cc)
		{
		case 0: 	//seat-msg
			int cnt=0;
			msgType="seat-msg";
			//alist.clear();			//clear ArrayList<Player>
			//plist.clear();			//clear ArrayList<Poker>
			regex=".+\\s+button:\\s*([0-9]+)\\s*([0-9]+)\\s*([0-9]+)\\s*[\\s\\S]+\\s*"; 
			p=Pattern.compile(regex);		
		    m=p.matcher(s);
			if(m.matches()){
				totalPlayer++;	//buttonMsg:totalPlayer + 1
				player = new Player();
				player.setNum(cnt);
				player.setRole("button");	
				player.setPid(Integer.valueOf(m.group(1)));
				player.setJetton(Integer.valueOf(m.group(2)));
				player.setMoney(Integer.valueOf(m.group(3)));
				alist.add(player);
				if(m.group(1).equals(myId))myIndex=cnt;
				cnt++;
				//System.out.println(cnt);
			}
			regex="[\\s\\S]+\\s+small blind:\\s*([0-9]+)\\s*([0-9]+)\\s*([0-9]+)\\s*[\\s\\S]+\\s*";
			p=Pattern.compile(regex);		
		    m=p.matcher(s);
			if(m.matches()){
				totalPlayer++;	//smallBlindMsg:totalPlayer + 1
				player = new Player();
				player.setNum(cnt);	
				player.setRole("sBlind");
				player.setPid(Integer.valueOf(m.group(1)));
				player.setJetton(Integer.valueOf(m.group(2)));
				player.setMoney(Integer.valueOf(m.group(3)));
				alist.add(player);
				if(m.group(1).equals(myId)){myIndex=cnt;ImBlind = "sb";System.out.println("I'm sblind");}
				cnt++;
				//System.out.println(cnt);
			}
			regex="[\\s\\S]+\\s+big blind:\\s*([0-9]+)\\s*([0-9]+)\\s*([0-9]+)\\s*[\\s\\S]+\\s*";
			p=Pattern.compile(regex);		
		    m=p.matcher(s);
			if(m.matches()){
				totalPlayer++;	//bigBlindMsg:totalPlayer + 1
				player = new Player();
				player.setNum(cnt);
				player.setRole("bBlind");
				player.setPid(Integer.valueOf(m.group(1)));
				player.setJetton(Integer.valueOf(m.group(2)));
				player.setMoney(Integer.valueOf(m.group(3)));
				alist.add(player);	
				if(m.group(1).equals(myId)){myIndex=cnt;ImBlind="bb";System.out.println("I'm bblind");}
				cnt++;
				//System.out.println(cnt);
			}
			regex="\\n{1}([0-9]+) ([0-9]+) ([0-9]+) ";
			p=Pattern.compile(regex);		
		    m=p.matcher(s);
			while(m.find()){
				totalPlayer++;	//ordinaryPlayerMsg:totalPlayer + 1
				player = new Player();
				player.setNum(cnt);
				player.setRole("player");
				player.setPid(Integer.valueOf(m.group(1)));
				player.setJetton(Integer.valueOf(m.group(2)));
				player.setMoney(Integer.valueOf(m.group(3)));
				alist.add(player);	
				if(m.group(1).equals(myId))myIndex=cnt;
				cnt++;
			}
			break;
		case 5:			//game-over msg
			msgType="game-over msg";
			isOnTable =false;
			break;
		case 14:	//blind-msg
			msgType="blind-msg";
			cnt=0;
			Player myPlay = alist.get(myIndex);
			regex="\\n{1}([0-9]+): ([0-9]+) ";
			p=Pattern.compile(regex);
			m=p.matcher(s);
			while(m.find()){
			//	bet[cnt++]= Integer.valueOf(m.group(2));
//					if(m.group(1).equals(myId)) {
//						ImBlind = "0";
//					//	myPlay.setJetton(myPlay.getJetton()-Integer.valueOf(m.group(2)));
//					}
//								
			}															//if i'm blind,calcaulate my bet
			break;
		case 20:	//hold-card-msg
			msgType="hold-card-msg";
			cnt=0;
			regex="\\n{1}([A-Z]+) ([0-9]+|J|Q|K|A) ";
			p=Pattern.compile(regex);
			m=p.matcher(s);
			while(m.find()){
				poker = new Poker();
				poker.setColor(m.group(1).substring(0,1));
				if(m.group(2).equals("10"))poker.setPoint("T");
				else poker.setPoint(m.group(2));
				plist.add(poker);
			}
			break;
		case 25:		//inquire-msg
			msgType="inquire-msg";
			isInquire=true;cnt=0;
			regex="\\n{1}([0-9]+) ([0-9]+) ([0-9]+) ([0-9]+) ([a-z]+_*[a-z]*) ";
			p=Pattern.compile(regex);
			m=p.matcher(s);
			while(m.find()){	
				cnt++;
				inPlayer = new InPlayer();
				inPlayer.setPid(Integer.valueOf(m.group(1)));
				inPlayer.setJetton(Integer.valueOf(m.group(2)));
				inPlayer.setMoney(Integer.valueOf(m.group(3)));
				inPlayer.setBet(Integer.valueOf(m.group(4)));
				inPlayer.setAction(m.group(5));
				iplist.add(inPlayer);
			}
			regex="[\\s\\S]+\\s+total pot:\\s*([0-9]+)\\s*[\\s\\S]+\\s*";
			p=Pattern.compile(regex);
			m=p.matcher(s);
			if(m.matches()){
				totalPot=Integer.valueOf(m.group(1));
//				inPlayer.setTotalPot(totalPot);
//				iplist.add(inPlayer);
			}
			break;
		case 33:		//flop-msg
			msgType="flop-msg";
			regex="\\n{1}([A-Z]+) ([0-9]+|J|Q|K|A) ";
			p=Pattern.compile(regex);
			m=p.matcher(s);
			while(m.find()){				
				poker = new Poker();
				poker.setColor(m.group(1).substring(0,1));
				if(m.group(2).equals("10"))poker.setPoint("T");
				else poker.setPoint(m.group(2));
				plist.add(poker);				
			}
			break;
		case 38:	//turn-msg
			msgType="turn-msg";
			regex="\\n{1}([A-Z]+) ([0-9]+|J|Q|K|A) ";
			p=Pattern.compile(regex);		
		    m=p.matcher(s);
		    while(m.find()){				
				poker = new Poker();
				poker.setColor(m.group(1).substring(0,1));
				if(m.group(2).equals("10"))poker.setPoint("T");
				else poker.setPoint(m.group(2));
				plist.add(poker);			
			}
			break;
		case 43:	//river-msg
			msgType="river-msg";			
			regex="\\n{1}([A-Z]+) ([0-9]+|J|Q|K|A) ";
			p=Pattern.compile(regex);		
		    m=p.matcher(s);
		    while(m.find()){
				poker = new Poker();
				poker.setColor(m.group(1).substring(0,1));
				if(m.group(2).equals("10"))poker.setPoint("T");
				else poker.setPoint(m.group(2));
				plist.add(poker);		
			}
			break;
		case 49:	//showdown-msg
			msgType="showdown-msg";
			/*regex="\\n{1}([A-Z]+) ([0-9]+|J|Q|K|A) ";//牌
			p=Pattern.compile(regex);
			m=p.matcher(s);
			while(m.find()){
				System.out.println("showdown poker matches!");
				System.out.println(m.group(1));
				System.out.println(m.group(2));				
			}
			regex="\\n{1}rank: ([0-9]+) ([A-Z]+) ([0-9]+|J|Q|K|A) ([A-Z]+) ([0-9]+|J|Q|K|A) ([A-Z]+_*[A-Z]*) ";//rank
			p=Pattern.compile(regex);
			m=p.matcher(s);
			while(m.find()){
				System.out.println("showdown rank matches!");
				System.out.println(m.group(1));
				System.out.println(m.group(2));		
				System.out.println(m.group(3));	
				System.out.println(m.group(4));	
				System.out.println(m.group(5));	
				System.out.println(m.group(6));	
			}*/
			break;
		case 58:	//potwin-msg
			msgType="potwin-msg";
			isSingleOver = true;
			TotalContest++;
			System.out.println("I have contest "+TotalContest+" times!");
			/*msgType="potWin";int Jetton=0;
			myPlay = alist.get(myIndex);
			regex="\\n{1}([0-9]+): ([0-9]+) ";
			p=Pattern.compile(regex);
			m=p.matcher(s);
			while(m.find()){
				if(Integer.valueOf(m.group(1))==myId) 
					Jetton = Integer.valueOf(m.group(2)) + myPlay.getJetton();
				myPlay.setMoney(Jetton + myPlay.getMoney());
				myPlay.setJetton(0);	
			}*/
			break;
		default:
			break;	
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
			isOnTable = false;
			return "game-over \n";
			//e.printStackTrace();
		}
		return info;
	}
	public void sendAction(int num)
	{
		pw.print("raise "+num+" \n");
		pw.flush();
		sendMsgAction =sendMsgAction+"	"+num;
	}
	public void sendAction(String action)
	{
		pw.print(action+" \n");
		pw.flush();	
		sendMsgAction =sendMsgAction+"	"+action;
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


class Poker			
{	
	private String color;	//花色
	private String point;	//点数

	private String reg1="SHDC";
	private String reg2="23456789TJQKA";
	
	private int index;		//索引号
	private short number;		//点数的整数值
		
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getPoint() {
		return point;
	}
	public void setPoint(String point) {
		this.point = point;
	}
	public short getNum()
	{		
		return (short) (reg2.indexOf(this.point) + 2);
	}
	public int getIndex()
	{
		//根据数组指定索引号
		index = reg2.indexOf(this.point) * 4 + reg1.indexOf(this.color);
		return index;
	}
}

class InPlayer
{
	private int pid;
	private int jetton;
	private int money;
	private int bet;
	private String action;
//	static int totalPot;
	
	public int getPid() {
		return pid;
	}
	public void setPid(int pid) {
		this.pid = pid;
	}
	public int getJetton() {
		return jetton;
	}
	public void setJetton(int jetton) {
		this.jetton = jetton;
	}
	public int getMoney() {
		return money;
	}
	public void setMoney(int money) {
		this.money = money;
	}
	public int getBet() {
		return bet;
	}
	public void setBet(int bet) {
		this.bet = bet;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
//	public int getTotalPot() {
//		return totalPot;
//	}
//	public void setTotalPot(int totalPot) {
//		this.totalPot = totalPot;
//	}
		
}
class Player extends InPlayer
{
	private int num;
	private String role;
	
	public int getNum() {
		return num;
	}
	public void setNum(int num) {
		this.num = num;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}	
}
class CalcInquire
{
//	ArrayList<InPlayer> aInquire = null;
//	
//	public CalcInquire()
//	{
//		aInquire = new ArrayList<InPlayer>(8);		
//	}
	public int getMinBetToCall(ArrayList<InPlayer> aInquire)	//calcuate the minimum bet if i follow
	{
		int minBet=0;
		for(int i = 0;i<aInquire.size();i++)
		{
			minBet=aInquire.get(i).getBet();
			if(minBet!=0)break;
		}
		return minBet;
	}
	public boolean isAllinExist(ArrayList<InPlayer> aInquire)	//calcuate if allIn exist
	{
		boolean isAllin=false;
		for(int i = 0;i<aInquire.size();i++)
		{
			if(aInquire.get(i).getAction().contains("all_in"))
			{
				isAllin=true;
				break;
			}
		}
		return isAllin;
	}
	public int getFoldNumber(ArrayList<InPlayer> aInquire)
	{
		int cntNum = 0;
		for(int i = 0;i<aInquire.size();i++)
		{
			if(aInquire.get(i).getAction().equals("fold"))
			{
				cntNum++;
			}
		}
		return cntNum;
	}
}

class CalcAction
{
	private boolean calcOver=false;
	
	private ArrayList<Poker> apoker= null;
	Poker poker = null;
	
	
	int ourRank;				//根据我的两张手牌及公共牌计算我的排名
	int[] myCards = null;		//提取出自己的手牌	
	int[] boardCards = null;	//提取出公共牌
	int[][] pCard = null;				//根据提供的五张牌算出可能的任意两张组合
	
	Data1 data1 = null;
	Data2 data2 = null;
	Data3 data3 = null;
	Data4 data4 = null;
	
	private static final int[] pokerToken={
			69634,73730,81922,98306,135427,139523,147715,164099,266757,270853,279045,295429,529159,
			533255,541447,557831,1053707,1057803,1065995,1082379,2102541,2106637,2114829,2131213,4199953,
			4204049,4212241,4228625,8394515,8398611,8406803,8423187,16783383,16787479,16795671,16812055,
			33560861,33564957,33573149,33589533,67115551,67119647,67127839,67144223,134224677,134228773,
			134236965,134253349,268442665,268446761,268454953,268471337};

	private static final short[][] pokerCombine={{ 0, 1, 2, 3, 4 },	//7张牌的组合
		  { 0, 1, 2, 3, 5 }, { 0, 1, 2, 3, 6 }, { 0, 1, 2, 4, 5 },{ 0, 1, 2, 4, 6 },{ 0, 1, 2, 5, 6 },
		  { 0, 1, 3, 4, 5 }, { 0, 1, 3, 4, 6 }, { 0, 1, 3, 5, 6 },{ 0, 1, 4, 5, 6 },{ 0, 2, 3, 4, 5 },
		  { 0, 2, 3, 4, 6 }, { 0, 2, 3, 5, 6 }, { 0, 2, 4, 5, 6 },{ 0, 3, 4, 5, 6 },{ 1, 2, 3, 4, 5 },
		  { 1, 2, 3, 4, 6 }, { 1, 2, 3, 5, 6 }, { 1, 2, 4, 5, 6 },{ 1, 3, 4, 5, 6 },{ 2, 3, 4, 5, 6 }
		};
	private static final short[][] pokerCombine1={{ 0, 1, 2, 3, 4 },	//6张牌的组合
		  { 0, 1, 2, 3, 5 }, { 0, 1, 3, 4, 5 }, { 0, 1, 2, 4, 5 },{ 0, 2, 3, 4, 5 },{ 1, 2, 3, 4, 5 }};
	public CalcAction()
	{
//		this.alPoker=new ArrayList<Poker>();
		

		apoker = new ArrayList<Poker>(7);

		
		data1 = new Data1();
		data2 = new Data2();
		data3 = new Data3();
		data4 = new Data4();
		
		//long startTime = System.currentTimeMillis();//获取当前时间
		
		//float s=EHS();
		
		//long endTime = System.currentTimeMillis();
		//System.out.println("程序运行时间："+(endTime-startTime)+"ms");
		
		//System.out.println("EHS："+EHS());
		

	}
	public void setInitial(ArrayList<Poker> aPoker)
	{
		this.apoker = aPoker;
		if(aPoker.size()>=5)
		{
			this.myCards = aListToArray(aPoker,0,2);		//提取出自己的手牌		
			this.boardCards = aListToArray(aPoker,2,3);	//提取出公共牌
			this.ourRank=rank(this.myCards,this.boardCards);
			this.pCard = possibleCard(aPoker);
		}
	}
	public float EHS()
	{
		float hs = handStrength();
		float ppot = handPotential();
		return hs+(1-hs)*ppot;
	}
	public int calc2Poker()
	{
		double totalScore = 0.0;
		
		short num1 = apoker.get(0).getNum();
		short num2 = apoker.get(1).getNum();
		short maxNum =num1;
		if(num2>maxNum)maxNum = num2;
		switch(maxNum)
		{
		case 14: totalScore = 10;break;	//最大为A
		case 13: totalScore = 8 ;break;	//最大为K
		case 12: totalScore = 7 ;break;	//最大为Q
		case 11: totalScore = 6	;break;	//最大为J
		default: totalScore = maxNum/2.0;	//其余情况数除以2
		}
		
		if(apoker.get(0).getColor().equals(
				apoker.get(1).getColor()))totalScore+=2;
		
		switch(Math.abs(num2-num1))
		{
		case 0:{totalScore = totalScore*2;if(totalScore<5)totalScore=5;}break;
		case 1:break;
		case 2:totalScore-=1;break;
		case 3:totalScore-=2;break;
		case 4:totalScore-=4;break;
		default:totalScore-=5;
		}
		return (int) Math.ceil(totalScore) ;
	}
	public float calc6Poker()
	{
		ArrayList<Poker> aPoker = new ArrayList<Poker>();
		Poker temp = new Poker();
		
		int oRank,oPRank;
		int ahead=0;	//领先的次数
		int tied=0;		//相当的次数
		int behand=0;	//落后的次数
		
		float maxPro=0;
		float result;	
		
		for(int i=0;i<6;i++)
		{
	
			for(int j=0;j<5;j++)
			{
				temp = this.apoker.get(pokerCombine1[i][j]);
				aPoker.add(temp);
			}
			oRank=rank(aListToArray(aPoker,0,2),aListToArray(aPoker,2,3));	
			
			int[][] tempArray =possibleCard(aPoker);
			
			for(int j=0;j<1035;j++)			//计算所有对方可能的手牌
			{
				oPRank=rank(tempArray[j],boardCards);

				if(oRank<oPRank) ahead+=1;
				else if(oRank==oPRank) tied+=1;
				else behand+=1;
			}
			result= (ahead+tied/2.0f)/(ahead+tied+behand);
			if(result > maxPro)
				maxPro = result;
		}
		return maxPro;
	}
	
	public float calc7Poker()
	{
		ArrayList<Poker> aPoker = new ArrayList<Poker>();
		Poker temp = new Poker();
		
		int oRank,oPRank;
		int ahead=0;	//领先的次数
		int tied=0;		//相当的次数
		int behand=0;	//落后的次数
		
		float maxPro=0;
		float result;	
		
		for(int i=0;i<21;i++)
		{
			for(int j=0;j<5;j++)
			{
				temp = this.apoker.get(pokerCombine[i][j]);
				aPoker.add(temp);
			}
			oRank=rank(aListToArray(aPoker,0,2),aListToArray(aPoker,2,3));
			
			int[][] tempArray =possibleCard(aPoker);
			
			for(int j=0;j<990;j++)			//计算所有对方可能的手牌
			{
				oPRank=rank(tempArray[j],boardCards);

				if(oRank<oPRank) ahead+=1;
				else if(oRank==oPRank) tied+=1;
				else behand+=1;
			}
			result= (ahead+tied/2.0f)/(ahead+tied+behand);
			if(result > maxPro)
				maxPro = result;
		}
		return maxPro;
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

		int opporientRank,ourBest,opporientBest=0;
		int index=0;
		
		HandPotential thread1=new HandPotential(pCard,100,150);
		HandPotential thread2=new HandPotential(pCard,250,150);
		HandPotential thread3=new HandPotential(pCard,400,150);
		HandPotential thread4=new HandPotential(pCard,550,150);
		HandPotential thread5=new HandPotential(pCard,700,150);
		HandPotential thread6=new HandPotential(pCard,850,150);
		HandPotential thread7=new HandPotential(pCard,1000,80);

		thread1.start();
		thread2.start();
		thread3.start();
		thread4.start();
		thread5.start();
		thread6.start();
		thread7.start();
		
		for(int i=0;i<100;i++)			//计算对方所有可能的手牌:47*46/2 = 1081   分解计算
		{		
			if(isContained(pCard[i][0], apoker) || isContained(pCard[i][1], apoker)) continue;
			
			opporientRank = rank(pCard[i],boardCards);
			if(opporientRank-this.ourRank>500)continue;
			else if(opporientRank-this.ourRank>0) index=0;
			else if(opporientRank-this.ourRank==0) index=1;
			else index=2;
			
			for(int j=51;j>0;j--)
			{							
				if(isContained(j,apoker ,pCard[i]))continue;
				for(int k=0;k<j;k++)
				{
					if(isContained(k,apoker, pCard[i]))continue;				
					
					ourBest=rank(pokerToken[j],pokerToken[k], apoker);
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
						+thread3.getHp()[i][j]+thread4.getHp()[i][j]+thread5.getHp()[i][j]
								+thread6.getHp()[i][j]+thread7.getHp()[i][j];
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
	public int rank(short[] pokercombine12)
	{
		int[] tab = new int[7];	
		for(int i=0;i<5;i++)
		{
			tab[i]=pokerToken[this.apoker.get(pokercombine12[i]).getIndex()];
		}
		int r = tab[0] & tab[1] & tab[2]  & tab[3] & tab[4] & 0xf000; 
		int q = (tab[0] | tab[1] | tab[2] | tab[3] | tab[4])>>16;

		if(r!=0)return(data1.rankTable[q]);					//能够形成顺子或同花
		else if(data4.rankTable2[q]!=0)return data4.rankTable2[q];
		else {
			int p = (tab[0] & 0xff)*(tab[1] & 0xff)*(tab[2] & 0xff)*(tab[3] & 0xff)*(tab[4] & 0xff);
			return data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
		}
	}public int rank(int[] privateCards, int[] boardCard)
	{
				
		int r = privateCards[0] & privateCards[1] & boardCard[0] & boardCard[1] & boardCard[2] & 0xf000; 
		int q = (privateCards[0] | privateCards[1] | boardCard[0] | boardCard[1] | boardCard[2])>>16;
		//System.out.println("r="+r+"	"+"q="+q);
		if(r!=0)return(data1.rankTable[q]);					//能够形成顺子或同花
		else if(data4.rankTable2[q]!=0)return data4.rankTable2[q];
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
		else if(data4.rankTable2[q]!=0)min=data4.rankTable2[q];
		else {
			p = (tab[pokerCombine[0][0]] & 0xff)*(tab[pokerCombine[0][1]] & 0xff)*(tab[pokerCombine[0][2]] & 0xff)*(tab[pokerCombine[0][3]] & 0xff)*(tab[pokerCombine[0][4]] & 0xff);
	
			min = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
		}
		
		for(int j=1;j<21;j++)
		{
			r = (tab[pokerCombine[j][0]] & tab[pokerCombine[j][1]] & tab[pokerCombine[j][2]] & tab[pokerCombine[j][3]] & tab[pokerCombine[j][4]])& 0xf000; 
			q = (tab[pokerCombine[j][0]] | tab[pokerCombine[j][1]] | tab[pokerCombine[j][2]] | tab[pokerCombine[j][3]] | tab[pokerCombine[j][4]])>>16;
			if(r!=0)temp=(data1.rankTable[q]);					//能够形成顺子或同花
			else if(data4.rankTable2[q]!=0)temp=data4.rankTable2[q];
			else {
				p = (tab[pokerCombine[j][0]] & 0xff)*(tab[pokerCombine[j][1]] & 0xff)*(tab[pokerCombine[j][2]] & 0xff)*(tab[pokerCombine[j][3]] & 0xff)*(tab[pokerCombine[j][4]] & 0xff);
				temp = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
			}
			if(temp<min)min=temp;
		}
		return min;
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
		else if(data4.rankTable2[q]!=0)min=data4.rankTable2[q];
		else {
			p = (tab[pokerCombine[0][0]] & 0xff)*(tab[pokerCombine[0][1]] & 0xff)*(tab[pokerCombine[0][2]] & 0xff)*(tab[pokerCombine[0][3]] & 0xff)*(tab[pokerCombine[0][4]] & 0xff);
	
			min = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
		}
		for(int j=1;j<21;j++)
		{
			r = (tab[pokerCombine[j][0]] & tab[pokerCombine[j][1]] & tab[pokerCombine[j][2]] & tab[pokerCombine[j][3]] & tab[pokerCombine[j][4]])& 0xf000; 
			q = (tab[pokerCombine[j][0]] | tab[pokerCombine[j][1]] | tab[pokerCombine[j][2]] | tab[pokerCombine[j][3]] | tab[pokerCombine[j][4]])>>16;
			if(r!=0)temp=(data1.rankTable[q]);					//能够形成顺子或同花
			else if(data4.rankTable2[q]!=0)temp=data4.rankTable2[q];
			else {
				p = (tab[pokerCombine[j][0]] & 0xff)*(tab[pokerCombine[j][1]] & 0xff)*(tab[pokerCombine[j][2]] & 0xff)*(tab[pokerCombine[j][3]] & 0xff)*(tab[pokerCombine[j][4]] & 0xff);
				temp = data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
				return data2.values[binarySearch(data3.products,p,0,data3.products.length-1)];
			}
			if(temp<min)min=temp;
		}
		return min;
	}
	//判断指定元素在之前是否出现过
	public boolean isContained(int a,ArrayList<Poker> aPoker)
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
	public boolean isContained(int a,ArrayList<Poker> aPoker,int []t)
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
	public int[][] possibleCard(ArrayList<Poker> aPoker)
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
	public int binarySearch(int[] dataset,int data,int beginIndex,int endIndex)
	{    
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
	class HandPotential extends Thread
	{
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
			
			this.calcOver=false;
			int opporientRank,ourBest,opporientBest=0;
			int index=0;
		
			ourRank=rank(myCards,boardCards);	
			
			for(int i=start;i<start+length-1;i++)			//计算对方所有可能的手牌:47*46/2 = 1081     转牌河牌
			{	
				if(isContained(pCard[i][0], apoker) || isContained(pCard[i][1], apoker)) continue;
				opporientRank = rank(pCard[i],boardCards);
				
				if(ourRank<opporientRank) index=0;
				else if(ourRank==opporientRank) index=1;
				else index=2;
				for(int j=51;j>0;j--)
				{							
					if(isContained(j,apoker ,pCard[i]))continue;
					for(int k=0;k<j;k++)
					{
						if(isContained(k,apoker, pCard[i]))continue;
						ourBest=rank(pokerToken[j], pokerToken[k], apoker);	
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
		}		
	}
	
}

class Data1
{
	public static final short rankTable[]={	
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,1599,0,0,0,0,0,0,0,1598,0,0,0,1597,0,1596,8,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,1595,0,0,0,0,0,0,0,1594,0,0,0,1593,0,1592,1591,0,0,0,0,0,0,0,0,1590,0,
		0,0,1589,0,1588,1587,0,0,0,0,1586,0,1585,1584,0,0,1583,1582,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,1581,0,0,0,0,0,0,0,1580,0,0,0,1579,0,1578,1577,0,0,0,0,0,0,0,0,1576,0,0,0,1575,0,1574,1573,
		0,0,0,0,1572,0,1571,1570,0,0,1569,1568,0,1567,0,0,0,0,0,0,0,0,0,0,1566,0,0,0,1565,0,1564,1563,0,0,0,
		0,1562,0,1561,1560,0,0,1559,1558,0,1557,0,0,0,0,0,0,1556,0,1555,1554,0,0,1553,1552,0,1551,0,0,0,0,1550,1549,0,1548,
		0,0,0,6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1547,0,0,0,0,0,0,0,1546,
		0,0,0,1545,0,1544,1543,0,0,0,0,0,0,0,0,1542,0,0,0,1541,0,1540,1539,0,0,0,0,1538,0,1537,1536,0,0,1535,1534,
		0,1533,0,0,0,0,0,0,0,0,0,0,1532,0,0,0,1531,0,1530,1529,0,0,0,0,1528,0,1527,1526,0,0,1525,1524,0,1523,0,
		0,0,0,0,0,1522,0,1521,1520,0,0,1519,1518,0,1517,0,0,0,0,1516,1515,0,1514,0,0,0,1513,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,1512,0,0,0,1511,0,1510,1509,0,0,0,0,1508,0,1507,1506,0,0,1505,1504,0,1503,0,0,0,0,0,0,1502,
		0,1501,1500,0,0,1499,1498,0,1497,0,0,0,0,1496,1495,0,1494,0,0,0,1493,0,0,0,0,0,0,0,0,0,0,1492,0,1491,1490,
		0,0,1489,1488,0,1487,0,0,0,0,1486,1485,0,1484,0,0,0,1483,0,0,0,0,0,0,0,0,1482,1481,0,1480,0,0,0,1479,0,
		0,0,0,0,0,0,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,1478,0,0,0,0,0,0,0,1477,0,0,0,1476,0,1475,1474,0,0,0,0,0,0,0,0,1473,0,0,0,1472,0,1471,1470,0,
		0,0,0,1469,0,1468,1467,0,0,1466,1465,0,1464,0,0,0,0,0,0,0,0,0,0,1463,0,0,0,1462,0,1461,1460,0,0,0,0,
		1459,0,1458,1457,0,0,1456,1455,0,1454,0,0,0,0,0,0,1453,0,1452,1451,0,0,1450,1449,0,1448,0,0,0,0,1447,1446,0,1445,0,
		0,0,1444,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1443,0,0,0,1442,0,1441,1440,0,0,0,0,1439,0,1438,1437,0,0,
		1436,1435,0,1434,0,0,0,0,0,0,1433,0,1432,1431,0,0,1430,1429,0,1428,0,0,0,0,1427,1426,0,1425,0,0,0,1424,0,0,0,
		0,0,0,0,0,0,0,1423,0,1422,1421,0,0,1420,1419,0,1418,0,0,0,0,1417,1416,0,1415,0,0,0,1414,0,0,0,0,0,0,
		0,0,1413,1412,0,1411,0,0,0,1410,0,0,0,0,0,0,0,1409,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,1408,0,0,0,1407,0,1406,1405,0,0,0,0,1404,0,1403,1402,0,0,1401,1400,0,1399,0,0,0,0,0,0,1398,0,
		1397,1396,0,0,1395,1394,0,1393,0,0,0,0,1392,1391,0,1390,0,0,0,1389,0,0,0,0,0,0,0,0,0,0,1388,0,1387,1386,0,
		0,1385,1384,0,1383,0,0,0,0,1382,1381,0,1380,0,0,0,1379,0,0,0,0,0,0,0,0,1378,1377,0,1376,0,0,0,1375,0,0,
		0,0,0,0,0,1374,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1373,0,1372,1371,0,0,1370,1369,0,1368,0,
		0,0,0,1367,1366,0,1365,0,0,0,1364,0,0,0,0,0,0,0,0,1363,1362,0,1361,0,0,0,1360,0,0,0,0,0,0,0,1359,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1358,1357,0,1356,0,0,0,1355,0,0,0,0,0,0,0,1354,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1353,0,0,0,0,0,0,0,1352,0,0,
		0,1351,0,1350,1349,0,0,0,0,0,0,0,0,1348,0,0,0,1347,0,1346,1345,0,0,0,0,1344,0,1343,1342,0,0,1341,1340,0,1339,
		0,0,0,0,0,0,0,0,0,0,1338,0,0,0,1337,0,1336,1335,0,0,0,0,1334,0,1333,1332,0,0,1331,1330,0,1329,0,0,0,
		0,0,0,1328,0,1327,1326,0,0,1325,1324,0,1323,0,0,0,0,1322,1321,0,1320,0,0,0,1319,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,1318,0,0,0,1317,0,1316,1315,0,0,0,0,1314,0,1313,1312,0,0,1311,1310,0,1309,0,0,0,0,0,0,1308,0,1307,
		1306,0,0,1305,1304,0,1303,0,0,0,0,1302,1301,0,1300,0,0,0,1299,0,0,0,0,0,0,0,0,0,0,1298,0,1297,1296,0,0,
		1295,1294,0,1293,0,0,0,0,1292,1291,0,1290,0,0,0,1289,0,0,0,0,0,0,0,0,1288,1287,0,1286,0,0,0,1285,0,0,0,
		0,0,0,0,1284,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1283,0,0,0,1282,0,1281,1280,
		0,0,0,0,1279,0,1278,1277,0,0,1276,1275,0,1274,0,0,0,0,0,0,1273,0,1272,1271,0,0,1270,1269,0,1268,0,0,0,0,1267,
		1266,0,1265,0,0,0,1264,0,0,0,0,0,0,0,0,0,0,1263,0,1262,1261,0,0,1260,1259,0,1258,0,0,0,0,1257,1256,0,1255,
		0,0,0,1254,0,0,0,0,0,0,0,0,1253,1252,0,1251,0,0,0,1250,0,0,0,0,0,0,0,1249,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,1248,0,1247,1246,0,0,1245,1244,0,1243,0,0,0,0,1242,1241,0,1240,0,0,0,1239,0,0,
		0,0,0,0,0,0,1238,1237,0,1236,0,0,0,1235,0,0,0,0,0,0,0,1234,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,1233,1232,0,1231,0,0,0,1230,0,0,0,0,0,0,0,1229,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1228,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,1227,0,0,0,1226,0,1225,1224,0,0,0,0,1223,0,1222,1221,0,0,1220,1219,0,1218,0,0,0,0,0,0,1217,0,1216,1215,
		0,0,1214,1213,0,1212,0,0,0,0,1211,1210,0,1209,0,0,0,1208,0,0,0,0,0,0,0,0,0,0,1207,0,1206,1205,0,0,1204,
		1203,0,1202,0,0,0,0,1201,1200,0,1199,0,0,0,1198,0,0,0,0,0,0,0,0,1197,1196,0,1195,0,0,0,1194,0,0,0,0,
		0,0,0,1193,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1192,0,1191,1190,0,0,1189,1188,0,1187,0,0,0,
		0,1186,1185,0,1184,0,0,0,1183,0,0,0,0,0,0,0,0,1182,1181,0,1180,0,0,0,1179,0,0,0,0,0,0,0,1178,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,1177,1176,0,1175,0,0,0,1174,0,0,0,0,0,0,0,1173,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,1172,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,1171,0,1170,1169,0,0,1168,1167,0,1166,0,0,0,0,1165,1164,0,1163,0,0,0,1162,0,0,0,
		0,0,0,0,0,1161,1160,0,1159,0,0,0,1158,0,0,0,0,0,0,0,1157,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,1156,1155,0,1154,0,0,0,1153,0,0,0,0,0,0,0,1152,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1151,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1150,1149,0,1148,
		0,0,0,1147,0,0,0,0,0,0,0,1146,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1145,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1144,0,
		0,0,0,0,0,0,1143,0,0,0,1142,0,1141,1140,0,0,0,0,0,0,0,0,1139,0,0,0,1138,0,1137,1136,0,0,0,0,1135,
		0,1134,1133,0,0,1132,1131,0,1130,0,0,0,0,0,0,0,0,0,0,1129,0,0,0,1128,0,1127,1126,0,0,0,0,1125,0,1124,1123,
		0,0,1122,1121,0,1120,0,0,0,0,0,0,1119,0,1118,1117,0,0,1116,1115,0,1114,0,0,0,0,1113,1112,0,1111,0,0,0,1110,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,1109,0,0,0,1108,0,1107,1106,0,0,0,0,1105,0,1104,1103,0,0,1102,1101,0,1100,
		0,0,0,0,0,0,1099,0,1098,1097,0,0,1096,1095,0,1094,0,0,0,0,1093,1092,0,1091,0,0,0,1090,0,0,0,0,0,0,0,
		0,0,0,1089,0,1088,1087,0,0,1086,1085,0,1084,0,0,0,0,1083,1082,0,1081,0,0,0,1080,0,0,0,0,0,0,0,0,1079,1078,
		0,1077,0,0,0,1076,0,0,0,0,0,0,0,1075,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,1074,0,0,0,1073,0,1072,1071,0,0,0,0,1070,0,1069,1068,0,0,1067,1066,0,1065,0,0,0,0,0,0,1064,0,1063,1062,0,0,
		1061,1060,0,1059,0,0,0,0,1058,1057,0,1056,0,0,0,1055,0,0,0,0,0,0,0,0,0,0,1054,0,1053,1052,0,0,1051,1050,0,
		1049,0,0,0,0,1048,1047,0,1046,0,0,0,1045,0,0,0,0,0,0,0,0,1044,1043,0,1042,0,0,0,1041,0,0,0,0,0,0,
		0,1040,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1039,0,1038,1037,0,0,1036,1035,0,1034,0,0,0,0,1033,
		1032,0,1031,0,0,0,1030,0,0,0,0,0,0,0,0,1029,1028,0,1027,0,0,0,1026,0,0,0,0,0,0,0,1025,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,1024,1023,0,1022,0,0,0,1021,0,0,0,0,0,0,0,1020,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,1019,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,1018,0,0,0,1017,0,1016,1015,0,0,0,0,1014,0,1013,1012,0,0,1011,1010,0,1009,0,
		0,0,0,0,0,1008,0,1007,1006,0,0,1005,1004,0,1003,0,0,0,0,1002,1001,0,1000,0,0,0,999,0,0,0,0,0,0,0,0,
		0,0,998,0,997,996,0,0,995,994,0,993,0,0,0,0,992,991,0,990,0,0,0,989,0,0,0,0,0,0,0,0,988,987,0,
		986,0,0,0,985,0,0,0,0,0,0,0,984,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,983,0,982,981,
		0,0,980,979,0,978,0,0,0,0,977,976,0,975,0,0,0,974,0,0,0,0,0,0,0,0,973,972,0,971,0,0,0,970,0,
		0,0,0,0,0,0,969,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,968,967,0,966,0,0,0,965,0,0,0,0,
		0,0,0,964,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,963,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,962,0,961,960,0,0,959,958,0,957,0,0,0,0,956,955,
		0,954,0,0,0,953,0,0,0,0,0,0,0,0,952,951,0,950,0,0,0,949,0,0,0,0,0,0,0,948,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,947,946,0,945,0,0,0,944,0,0,0,0,0,0,0,943,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,942,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,941,940,0,939,0,0,0,938,0,0,0,0,0,0,0,937,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,936,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,935,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,934,
		0,0,0,933,0,932,931,0,0,0,0,930,0,929,928,0,0,927,926,0,925,0,0,0,0,0,0,924,0,923,922,0,0,921,920,
		0,919,0,0,0,0,918,917,0,916,0,0,0,915,0,0,0,0,0,0,0,0,0,0,914,0,913,912,0,0,911,910,0,909,0,
		0,0,0,908,907,0,906,0,0,0,905,0,0,0,0,0,0,0,0,904,903,0,902,0,0,0,901,0,0,0,0,0,0,0,900,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,899,0,898,897,0,0,896,895,0,894,0,0,0,0,893,892,0,
		891,0,0,0,890,0,0,0,0,0,0,0,0,889,888,0,887,0,0,0,886,0,0,0,0,0,0,0,885,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,884,883,0,882,0,0,0,881,0,0,0,0,0,0,0,880,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,879,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,878,0,877,876,0,0,875,874,0,873,0,0,0,0,872,871,0,870,0,0,0,869,0,0,0,0,0,0,0,
		0,868,867,0,866,0,0,0,865,0,0,0,0,0,0,0,864,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,863,862,
		0,861,0,0,0,860,0,0,0,0,0,0,0,859,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,858,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,857,856,0,855,0,0,0,854,
		0,0,0,0,0,0,0,853,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,852,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,851,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,850,0,849,848,0,0,847,846,0,845,0,0,0,0,844,843,0,842,
		0,0,0,841,0,0,0,0,0,0,0,0,840,839,0,838,0,0,0,837,0,0,0,0,0,0,0,836,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,835,834,0,833,0,0,0,832,0,0,0,0,0,0,0,831,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,830,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,829,828,0,827,0,0,0,826,0,0,0,0,0,0,0,825,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,824,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,823,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,822,821,0,820,0,0,0,819,0,
		0,0,0,0,0,0,818,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,817,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,816,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,10,0,0,0,0,0,0,0,815,0,0,0,814,0,813,812,0,0,0,
		0,0,0,0,0,811,0,0,0,810,0,809,808,0,0,0,0,807,0,806,805,0,0,804,803,0,802,0,0,0,0,0,0,0,0,
		0,0,801,0,0,0,800,0,799,798,0,0,0,0,797,0,796,795,0,0,794,793,0,792,0,0,0,0,0,0,791,0,790,789,0,
		0,788,787,0,786,0,0,0,0,785,784,0,783,0,0,0,782,0,0,0,0,0,0,0,0,0,0,0,0,0,0,781,0,0,0,
		780,0,779,778,0,0,0,0,777,0,776,775,0,0,774,773,0,772,0,0,0,0,0,0,771,0,770,769,0,0,768,767,0,766,0,
		0,0,0,765,764,0,763,0,0,0,762,0,0,0,0,0,0,0,0,0,0,761,0,760,759,0,0,758,757,0,756,0,0,0,0,
		755,754,0,753,0,0,0,752,0,0,0,0,0,0,0,0,751,750,0,749,0,0,0,748,0,0,0,0,0,0,0,747,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,746,0,0,0,745,0,744,743,0,0,0,0,742,0,741,740,
		0,0,739,738,0,737,0,0,0,0,0,0,736,0,735,734,0,0,733,732,0,731,0,0,0,0,730,729,0,728,0,0,0,727,0,
		0,0,0,0,0,0,0,0,0,726,0,725,724,0,0,723,722,0,721,0,0,0,0,720,719,0,718,0,0,0,717,0,0,0,0,
		0,0,0,0,716,715,0,714,0,0,0,713,0,0,0,0,0,0,0,712,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,711,0,710,709,0,0,708,707,0,706,0,0,0,0,705,704,0,703,0,0,0,702,0,0,0,0,0,0,0,0,701,700,
		0,699,0,0,0,698,0,0,0,0,0,0,0,697,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,696,695,0,694,0,
		0,0,693,0,0,0,0,0,0,0,692,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,691,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,690,0,0,0,689,
		0,688,687,0,0,0,0,686,0,685,684,0,0,683,682,0,681,0,0,0,0,0,0,680,0,679,678,0,0,677,676,0,675,0,0,
		0,0,674,673,0,672,0,0,0,671,0,0,0,0,0,0,0,0,0,0,670,0,669,668,0,0,667,666,0,665,0,0,0,0,664,
		663,0,662,0,0,0,661,0,0,0,0,0,0,0,0,660,659,0,658,0,0,0,657,0,0,0,0,0,0,0,656,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,655,0,654,653,0,0,652,651,0,650,0,0,0,0,649,648,0,647,0,0,0,
		646,0,0,0,0,0,0,0,0,645,644,0,643,0,0,0,642,0,0,0,0,0,0,0,641,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,640,639,0,638,0,0,0,637,0,0,0,0,0,0,0,636,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,635,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,634,0,633,632,0,0,631,630,0,629,0,0,0,0,628,627,0,626,0,0,0,625,0,0,0,0,0,0,0,0,624,623,0,
		622,0,0,0,621,0,0,0,0,0,0,0,620,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,619,618,0,617,0,0,
		0,616,0,0,0,0,0,0,0,615,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,614,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,613,612,0,611,0,0,0,610,0,0,0,0,
		0,0,0,609,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,608,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,607,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,606,0,0,0,605,0,604,603,0,0,0,0,602,0,601,600,0,0,
		599,598,0,597,0,0,0,0,0,0,596,0,595,594,0,0,593,592,0,591,0,0,0,0,590,589,0,588,0,0,0,587,0,0,0,
		0,0,0,0,0,0,0,586,0,585,584,0,0,583,582,0,581,0,0,0,0,580,579,0,578,0,0,0,577,0,0,0,0,0,0,
		0,0,576,575,0,574,0,0,0,573,0,0,0,0,0,0,0,572,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,571,0,570,569,0,0,568,567,0,566,0,0,0,0,565,564,0,563,0,0,0,562,0,0,0,0,0,0,0,0,561,560,0,559,
		0,0,0,558,0,0,0,0,0,0,0,557,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,556,555,0,554,0,0,0,
		553,0,0,0,0,0,0,0,552,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,551,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,550,0,549,548,0,0,547,546,0,545,0,
		0,0,0,544,543,0,542,0,0,0,541,0,0,0,0,0,0,0,0,540,539,0,538,0,0,0,537,0,0,0,0,0,0,0,536,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,535,534,0,533,0,0,0,532,0,0,0,0,0,0,0,531,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,530,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,529,528,0,527,0,0,0,526,0,0,0,0,0,0,0,525,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,524,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,523,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		522,0,521,520,0,0,519,518,0,517,0,0,0,0,516,515,0,514,0,0,0,513,0,0,0,0,0,0,0,0,512,511,0,510,0,
		0,0,509,0,0,0,0,0,0,0,508,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,507,506,0,505,0,0,0,504,
		0,0,0,0,0,0,0,503,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,502,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,501,500,0,499,0,0,0,498,0,0,0,0,0,0,
		0,497,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,496,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,495,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,494,493,0,492,0,0,0,491,0,0,0,0,0,0,0,490,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,489,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,488,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,487,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,486,0,0,0,485,0,484,483,0,
		0,0,0,482,0,481,480,0,0,479,478,0,477,0,0,0,0,0,0,476,0,475,474,0,0,473,472,0,471,0,0,0,0,470,469,
		0,468,0,0,0,467,0,0,0,0,0,0,0,0,0,0,466,0,465,464,0,0,463,462,0,461,0,0,0,0,460,459,0,458,0,
		0,0,457,0,0,0,0,0,0,0,0,456,455,0,454,0,0,0,453,0,0,0,0,0,0,0,452,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,451,0,450,449,0,0,448,447,0,446,0,0,0,0,445,444,0,443,0,0,0,442,0,0,0,
		0,0,0,0,0,441,440,0,439,0,0,0,438,0,0,0,0,0,0,0,437,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,436,435,0,434,0,0,0,433,0,0,0,0,0,0,0,432,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,431,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,430,0,
		429,428,0,0,427,426,0,425,0,0,0,0,424,423,0,422,0,0,0,421,0,0,0,0,0,0,0,0,420,419,0,418,0,0,0,
		417,0,0,0,0,0,0,0,416,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,415,414,0,413,0,0,0,412,0,0,
		0,0,0,0,0,411,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,410,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,409,408,0,407,0,0,0,406,0,0,0,0,0,0,0,405,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,404,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,403,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,402,0,401,400,0,0,399,398,0,397,0,0,0,0,396,395,0,394,0,0,0,393,0,0,0,0,
		0,0,0,0,392,391,0,390,0,0,0,389,0,0,0,0,0,0,0,388,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,387,386,0,385,0,0,0,384,0,0,0,0,0,0,0,383,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,382,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,381,380,0,379,0,
		0,0,378,0,0,0,0,0,0,0,377,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,376,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,375,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,374,373,0,372,0,0,0,371,0,0,0,0,0,0,0,370,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,369,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,368,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,367,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,366,0,365,364,
		0,0,363,362,0,361,0,0,0,0,360,359,0,358,0,0,0,357,0,0,0,0,0,0,0,0,356,355,0,354,0,0,0,353,0,
		0,0,0,0,0,0,352,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,351,350,0,349,0,0,0,348,0,0,0,0,
		0,0,0,347,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,346,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,345,344,0,343,0,0,0,342,0,0,0,0,0,0,0,341,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,340,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,339,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,338,337,0,336,0,0,0,335,0,0,0,0,0,0,0,334,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,333,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,332,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,331,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,330,329,0,328,0,0,0,327,0,0,0,0,0,0,0,326,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,325,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,324,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,323,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};

}

class Data2
{
    public static final short values[]  = {
    	166,322,165,310,164,2467,154,2466,163,3325,321,162,3324,2464,2401,
    	161,2465,3314,160,2461,159,2400,320,3323,153,2457,6185,2463,3303,2452,
    	158,3322,157,298,2460,2446,152,3292,156,2398,3321,2462,5965,155,6184,
    	309,2456,3320,2439,3313,2395,2459,2431,2335,2451,6181,3319,3281,2422,151,
    	2391,2445,6183,2399,2455,319,3291,2412,5964,6175,2386,3318,5745,150,2450,
    	6180,3312,3317,297,6165,2458,2438,5961,2430,2380,142,2444,3311,308,3316,
    	318,286,149,6150,5963,6174,3259,5525,3315,2421,2397,2454,5955,148,6182,
    	2373,3302,6164,2437,5960,2411,5744,2449,2365,3310,5945,6178,2429,6129,2334,
    	2394,2453,6179,6101,147,141,3309,6149,5741,2448,2356,2443,3215,2269,5930,
    	2420,2396,5954,3290,3248,3280,2346,6065,6172,2390,2410,3308,317,146,6173,
    	2442,5944,3258,6128,3270,2393,6020,3301,6162,145,3289,5735,2436,2385,5958,
    	2447,6100,5909,2333,6169,6163,2428,2332,5881,5725,6177,316,5929,3307,3300,
    	6159,144,2435,6147,3204,285,3306,2379,6064,2441,2389,6148,2427,5524,2329,
    	2419,307,143,5845,3288,5952,3214,3257,2268,6019,5710,5962,3160,2440,6144,
    	2384,2409,5305,5908,3269,5800,3305,3287,6171,5942,5521,3299,6126,2418,5743,
    	2392,6155,5880,2372,2434,5949,6176,6127,6098,5959,3304,2331,6161,2364,2426,
    	315,2325,2408,3298,3094,6099,2378,5689,140,2433,6168,5939,3286,6123,5740,
    	5927,306,5661,5844,6140,2425,3213,2320,130,6095,3279,2328,6062,6158,2355,
    	5515,2417,2388,6146,5085,5304,2267,5799,3297,6063,3149,6170,6135,274,2432,
    	5953,5924,5523,6017,3247,2371,2345,5625,2407,5505,2416,2383,3285,2424,3278,
    	6018,5906,2314,6059,5742,3159,5935,6160,2363,6119,5734,2387,6143,5943,3237,
    	3284,296,5878,5580,6167,2406,3256,6091,3017,5520,2324,6125,6014,5957,6154,
    	3083,3296,6114,5724,2382,314,5490,5903,2415,6097,5739,2377,139,6157,3295,
    	2354,5920,6086,6145,5084,2319,5738,2423,129,3093,5928,2307,3283,5875,5842,
    	3212,3277,6122,2405,2266,6055,3203,3246,313,2344,2299,305,6139,5915,2203,
    	6108,3282,5709,6094,2376,5522,3158,5797,138,6061,3255,3294,5514,6010,6142,
    	3276,5951,6050,3193,5303,5469,6080,284,2414,2370,2313,5839,4865,2381,6134,
    	262,5899,2263,5733,6124,5956,6016,6153,3236,5441,5907,2413,3254,2362,3293,
    	2290,5504,6005,5732,5941,5301,5871,2404,3006,6096,5519,5794,6058,2330,6166,
    	304,5879,6118,5894,5948,5723,2929,3092,3275,5688,2403,2369,6044,2280,5722,
    	6090,6121,2375,3016,5866,137,3202,6013,5737,6073,4645,5660,6156,2306,5405,
    	2361,6138,312,2353,6113,5729,5938,3253,5081,5489,6093,5999,2265,5835,2327,
    	5926,6060,3211,2830,2298,5843,2259,6085,5950,2374,5083,3226,136,273,128,
    	5888,5360,5708,2402,4864,2343,6133,5295,5719,5513,5790,6054,6015,5707,5830,
    	3192,5302,3157,3274,5860,3210,6037,5798,5624,2352,3148,2254,6141,5940,2137,
    	2202,2368,6107,2262,311,5923,6057,3268,3273,6029,5285,6117,2289,5947,6009,
    	5503,5518,5785,5731,3252,6049,3245,5468,6152,2360,6079,5992,303,5579,5905,
    	135,2342,3138,5934,6089,3015,2323,2367,6012,5704,3251,3156,295,2918,4644,
    	5440,5687,5984,5824,5877,2279,6112,3209,5937,6004,5721,5300,2248,4425,3091,
    	2359,3267,5925,5686,5715,5853,3082,5659,3272,2720,6084,3182,5728,6120,2318,
    	5270,3201,6151,2928,5488,5902,5779,2351,6043,5658,6137,5075,2819,2258,5919,
    	6053,6092,5082,3225,2326,3250,6072,2366,3072,3271,134,5404,5874,5975,3147,
    	5841,5512,3244,5718,5080,2200,6106,3090,2341,5922,5683,5998,2264,5706,2350,
    	4861,2829,6132,2358,5065,5817,133,5623,6008,5700,2253,3208,250,5914,6048,
    	261,3249,2241,6078,2201,5359,5904,2312,5655,2599,4863,5796,6136,5933,5622,
    	5502,5294,5809,3243,3266,3207,5517,2340,5249,294,6056,3235,2233,5467,5772,
    	6036,5876,5578,5838,5509,3137,6116,6003,5695,5946,3155,2136,5298,5898,4424,
    	2261,5703,5221,4855,5577,302,6131,3081,5439,5764,6028,2349,5284,132,6088,
    	3265,3014,5050,2322,6011,2927,5299,2247,5870,5901,5991,3005,4641,6042,5685,
    	5793,5619,5499,5714,6111,2357,5936,3089,5918,2709,5679,5487,5893,3181,3206,
    	5736,3242,6071,4205,4643,2305,2224,5873,5983,2339,5657,131,6115,5840,3200,
    	6083,301,5078,2317,5651,5997,127,2995,5865,3154,5574,5185,2828,3071,2297,
    	5403,5755,2719,6087,238,5511,3013,5913,5674,2321,6052,3205,5269,5079,2199,
    	2214,4635,3264,5682,5834,3127,5795,3146,6110,5074,5292,3985,3199,2348,2257,
    	118,5484,5699,6105,5029,5646,2071,3191,5921,3224,6130,5140,2240,5887,6035,
    	5358,5654,2588,5837,5974,4862,5621,6082,6007,5501,2134,5293,2316,6047,2347,
    	5897,126,5466,5789,6077,5001,5615,3241,2311,5829,5495,4860,2232,5932,5859,
    	2338,5064,6027,5282,2288,5508,2252,6051,5730,5694,4845,2135,5297,5869,3088,
    	272,5990,3004,5668,5438,3153,5792,2598,3240,3145,5576,6002,2337,5283,2197,
    	6104,5892,5570,4421,3198,5516,5784,5248,5610,4204,3061,3263,5982,5640,3080,
    	3152,2278,3012,5618,293,6006,5498,6046,5720,4625,5463,300,5678,2926,4423,
    	6076,5864,5486,5900,2310,6041,6109,5220,4965,4854,5931,2917,4642,3262,2223,
    	5823,5480,2718,5727,5917,5049,5565,5267,5077,3234,2246,5435,5650,6070,5833,
    	2994,4640,2304,4830,5402,5872,5573,6081,3011,5072,3239,3984,2315,5852,6001,
    	125,3171,2336,3765,2005,4415,5673,3180,5996,283,4920,5268,3087,5886,2907,
    	2213,3079,2827,5778,5973,3126,5604,2296,3151,5475,5073,5291,5717,2818,5912,
    	2925,5788,117,5483,3197,5645,5357,249,6040,5705,5828,4858,3238,3086,5184,
    	5858,5633,5062,292,2193,3261,6103,299,124,5916,5510,2133,3190,2198,6069,
    	5465,4634,2597,2303,5399,5559,3196,5614,6034,3150,5494,5836,4859,6045,2808,
    	5063,5281,5816,5459,2131,6075,226,5896,2309,5028,5995,2260,5783,5246,2070,
    	3144,5139,2239,4610,2826,5667,5437,3260,4809,2295,3545,6026,3136,2188,6102,
    	2287,5911,5500,3233,5808,5431,2984,2196,5868,5354,5569,5989,5702,3003,5000,
    	5218,4852,5247,5609,5791,6000,2916,3060,2231,3085,5639,5289,5771,5822,5597,
    	4781,4405,5454,5507,6074,5047,5891,2308,4844,260,5296,123,3078,5462,4201,
    	4422,4638,6033,5684,5981,5219,3195,4853,2277,5713,5851,106,2924,5763,5589,
    	3232,5479,3764,5895,5426,6039,282,4420,5048,5863,5564,5266,4203,3084,5434,
    	5777,5552,4639,6025,5656,5279,3143,5401,2286,2717,4390,5071,5497,2817,5726,
    	6068,2182,3170,3010,4624,2708,2302,5395,5867,237,5988,3002,5485,5832,3194,
    	4964,5182,4589,2906,3070,5069,3981,2222,5544,5603,2923,5994,2256,4745,5474,
    	5890,6038,5076,271,2825,5448,3009,4195,4632,2294,5681,5885,5980,291,5356,
    	4829,2276,5972,4857,5910,4561,5183,3983,5632,5061,5815,2192,5716,5754,5350,
    	6067,5698,2698,2004,5026,4414,2068,2301,5390,5862,5787,4919,5137,3231,5827,
    	122,5420,3116,2212,4633,5653,5857,3544,5059,5398,5558,3125,4700,2716,5620,
    	5993,2251,3189,5290,2807,5807,5264,5458,2130,6032,1939,2824,116,5482,4998,
    	5027,5831,2293,5245,2069,2596,5138,121,2127,3077,5770,3975,3142,2587,2255,
    	5535,2187,5345,5693,4842,2132,3223,5782,2175,2922,5430,2983,6024,5884,5464,
    	5275,3008,5353,4999,2285,5217,5971,4851,5575,5493,3135,5762,4525,5288,3188,
    	5280,5596,3141,5987,3001,5453,4418,6031,5786,5046,5701,5826,4843,2896,2167,
    	4849,6066,4609,2915,2300,4637,5384,5856,2122,5436,4808,2577,5617,5821,5889,
    	2250,5044,105,4185,4622,5588,2707,5677,5979,2195,5425,3007,2245,2275,6023,
    	4419,3050,2595,4962,3230,2284,5413,4202,2823,3059,4480,5712,120,5850,2292,
    	5551,4780,5278,4404,5861,3761,5986,3000,3179,5781,5243,2181,4369,4623,5649,
    	5461,5339,5394,4200,2993,4827,2715,5572,5776,3229,4963,3134,5181,2797,3076,
    	5260,5068,2816,5543,5753,5478,3763,4170,2002,3140,4412,5672,5978,4917,3187,
    	2274,5265,5215,214,3105,3965,5447,4341,2914,119,2158,4631,6030,5433,281,
    	3069,5820,4828,5400,4389,5070,3075,3222,3982,2116,5883,3169,5349,115,2244,
    	2697,2003,5025,5644,4413,5970,2067,4629,5389,5680,4918,2714,5136,2921,4588,
    	5419,3115,5711,290,5377,5849,6022,3980,5255,2586,5058,5814,2283,3139,3755,
    	4744,5473,5697,5825,259,5023,2065,5263,5855,2148,5055,4194,5985,2238,225,
    	3950,4997,5613,5775,5355,2249,5652,3541,4856,2822,4560,3228,2126,2291,5060,
    	5369,2815,3221,2191,5806,5534,5882,2594,5344,4995,5969,4841,2174,4149,4607,
    	5179,5332,5666,5977,2230,5274,3068,4806,4305,3543,5769,5397,2273,4699,5506,
    	202,5780,5239,289,5692,3074,5457,4839,2129,2194,1938,5854,5568,3039,4417,
    	3186,5244,248,5608,2895,2166,280,4848,3227,2920,4608,5324,5638,3974,5383,
    	2121,4778,5813,4807,5761,4402,2713,2576,2186,5696,2109,5211,2061,2593,2973,
    	5043,2913,4621,5134,5429,2237,4198,2982,4260,5819,5352,3185,3049,3535,5216,
    	4961,4850,5412,5040,5616,3929,6021,5496,3073,5234,4524,5287,2243,2282,2687,
    	5805,4779,4403,5452,4619,2706,5676,5045,2101,5563,3220,5242,3133,5848,4959,
    	2919,2999,2229,5338,4199,4636,5768,5968,4826,2221,3745,4387,3178,2796,5259,
    	5691,2821,5206,4835,104,4184,3168,2281,3762,2912,2001,5774,5424,4411,5648,
    	2992,4916,5818,4824,5214,1873,3104,4586,5571,2814,2905,5976,2998,5035,2157,
    	3978,4479,2272,5315,5760,5602,5277,4742,2242,5752,3760,4388,1999,4409,5671,
    	2115,5175,4914,4192,2180,4368,3067,5847,5393,2592,2211,4628,3124,3730,3184,
    	4121,4558,5180,4587,5631,3177,2820,5376,5067,2190,3979,5254,2712,2271,4615,
    	4169,2705,5675,4743,5481,5773,5228,5022,5643,2064,2092,3964,5446,2147,5054,
    	4340,4193,5812,4630,2813,2566,2220,5557,4697,3132,2585,5019,94,3901,4559,
    	2806,5368,5130,2236,2128,2711,5170,1936,5348,288,5647,3525,236,5024,2991,
    	3219,2066,5388,5200,4820,4994,5612,3183,5135,2911,5492,4606,5178,5418,5331,
    	3114,3972,5804,5967,4805,2997,3542,5057,2185,5751,4698,3754,4991,1995,1807,
    	2962,5238,5670,2082,2228,5262,4838,279,5767,1937,3949,4604,2210,3038,4996,
    	5665,5811,3218,3123,4803,3540,5690,5846,5014,2056,4085,2125,5323,4522,5286,
    	3973,5595,5966,4777,5125,4401,3709,2235,2270,114,3176,5343,2108,5210,5642,
    	2060,3510,5567,2972,4840,2173,5607,4148,5133,4197,5759,3058,2591,2996,5273,
    	4304,5637,5803,2584,4775,4399,5039,2812,4986,103,5233,4182,4523,5587,2686,
    	2227,4618,190,5460,5766,2885,4416,2100,5611,5491,5164,2894,2165,4958,4847,
    	4040,4477,3066,5550,2590,5382,3028,2120,5276,2704,3131,287,5477,3758,4386,
    	4955,3865,5042,5205,4834,5562,2179,4183,4366,4620,2219,4600,5664,4259,5432,
    	5758,5193,4799,3048,3534,4960,4823,3217,213,4585,5411,3928,4384,5066,5034,
    	3977,4478,5810,5542,5314,4167,3130,2710,4741,2990,270,5008,3759,2050,1998,
    	5566,4408,5241,5119,5174,5606,4913,3962,2234,4338,4191,3057,4367,4583,5337,
    	2904,5636,3489,5750,2786,4825,3744,4771,1990,4395,5601,2703,5669,2910,4557,
    	4739,2795,5472,4910,3820,5258,5802,4950,3681,2209,4614,2696,4168,2000,3175,
    	4189,4410,247,4980,2218,5227,4915,3216,5213,2091,1872,3103,2226,3113,3963,
    	4339,5765,4555,2156,2565,5630,5056,2589,4696,113,5476,3752,5018,5641,93,
    	2811,2989,4815,2114,5129,5561,5261,3645,5169,1935,3947,3174,2583,4627,5199,
    	3538,4819,5396,5556,5749,5157,3729,82,4694,4120,4380,2124,3065,3971,5375,
    	5757,4905,2805,5253,5533,5456,258,3753,4990,2208,3129,1994,1933,201,2961,
    	3122,5021,2172,2063,2081,4146,4579,2146,5053,2903,5272,3948,4603,4302,3969,
    	178,4802,5600,3539,5149,4735,112,5471,3900,5013,3064,2055,2909,4521,5367,
    	4595,5124,2702,5663,5428,2874,2043,2981,3524,5351,2582,4944,5112,4993,278,
    	2164,4846,4147,4605,4551,5177,5330,2217,5629,2119,3461,4804,4303,4519,2189,
    	2575,5594,4774,3128,4398,5451,1806,5237,4985,5605,5041,5801,4181,3056,4837,
    	5635,4257,4973,1741,224,2035,3037,2884,2951,3047,3532,3173,5555,5104,4690,
    	2225,5163,3926,2908,4476,4084,5322,2804,3425,3027,4776,5748,5455,102,4179,
    	4400,3708,5586,1984,3757,1929,5662,5423,4794,2107,4899,5209,4954,5240,2059,
    	3509,2810,2971,4365,5132,2207,4196,4599,2775,4258,4474,3121,3742,5192,4798,
    	5549,3533,2184,277,5038,5560,5257,2676,3927,4383,5756,5232,3063,2685,4166,
    	5427,235,111,3600,2980,4363,4617,5007,5634,2049,5392,3172,4766,2099,5212,
    	1870,4375,3102,5118,3961,4957,4337,2155,4039,4582,4515,3167,2581,5593,2785,
    	3743,4770,5541,1989,4394,5450,4164,4385,4738,4909,2113,2809,3864,4574,5204,
    	4949,4833,2701,2902,3959,5445,4335,4188,4626,4979,5599,4937,2026,5470,3727,
    	4118,4822,1871,4584,5095,2216,5033,4554,3976,3062,5252,5313,4175,5585,3380,
    	3751,4740,5422,5347,2695,1997,5020,4407,2062,4814,5387,4546,5173,4912,2940,
    	2700,2145,5628,5052,4190,3946,2988,5417,269,4470,4788,5548,3488,4929,3537,
    	3166,5156,3728,3898,81,4693,4119,3749,4556,4379,2215,3819,4904,5747,3680,
    	1977,2178,4359,4613,2901,3522,5391,5554,1932,3944,4892,2016,4992,5226,5598,
    	4145,4730,2090,2555,3055,5176,2206,4578,2803,2987,3120,2123,4301,2564,4760,
    	3968,5540,1675,1924,4695,4160,5148,5017,4734,1804,5532,5236,92,3899,5342,
    	5128,4836,5746,4594,3644,110,3955,5444,1969,5168,4143,1934,4331,2873,5627,
    	3036,2042,3523,4884,2183,4299,5198,4943,5111,4818,4082,2205,4550,3970,2580,
    	3119,2979,4518,3706,5346,2694,4989,1993,2106,5208,1805,2960,2058,3507,5386,
    	5553,2970,4685,2080,5131,2893,109,4510,5416,3112,4256,4972,189,5592,2802,
    	4602,2034,2950,5381,3531,5449,2118,4801,5103,4689,2574,1918,5037,2665,3925,
    	5012,5231,2054,4083,4520,2579,276,3165,5123,4178,3707,4616,1983,1928,3940,
    	2098,4254,4793,4898,3508,268,3529,4956,4568,4037,2900,5410,101,2863,3923,
    	2774,5584,3460,4473,3741,2986,5421,4724,2978,4773,5531,4397,5341,2675,4984,
    	3862,5203,4832,4180,2171,4139,4465,2699,5547,4362,1740,1960,5271,5336,2883,
    	4295,5591,4765,4821,3739,1869,4374,4875,3054,4540,5162,5626,5032,4038,2794,
    	4475,4753,2204,2177,4514,3424,4354,3026,3118,3756,4163,1996,4406,4953,5172,
    	3863,4911,4573,2892,2163,1867,4364,3101,3958,4598,5539,4334,3486,108,5380,
    	2985,100,4155,5191,4936,4797,5583,4679,2025,3726,2573,4117,3053,5094,3817,
    	2801,4382,2764,5443,3678,2112,4326,4174,4612,4165,70,2578,3599,1950,5006,
    	4250,5546,5225,2048,3046,2544,2089,5117,4545,3960,3724,5409,2939,4115,4336,
    	3919,4581,275,4469,4787,5374,3487,3117,2784,4928,2176,2693,4769,4348,1988,
    	5016,4393,91,3897,5385,3748,4737,4908,5127,3818,3164,5415,4948,3642,246,
    	5167,3679,223,1976,4358,3521,107,5051,5335,4187,4978,3943,4891,5538,5197,
    	2015,4817,3735,2852,4729,212,2554,2793,3895,4504,5256,4553,5590,4759,5366,
    	4717,177,1923,3935,5442,3379,3750,4320,4159,4988,1992,1803,2959,3519,2079,
    	4813,3163,1863,257,3643,3954,1968,4142,3945,4601,4330,2154,5329,4883,5530,
    	4800,4298,3536,5340,4533,5155,2692,80,4692,2899,5011,4378,2053,4081,3052,
    	1801,2170,99,4134,4903,5582,5122,3705,4709,5414,3111,4290,1931,3506,3035,
    	4684,3720,4144,4111,4577,4459,4509,3458,5373,5545,4079,4300,5321,3967,4672,
    	5251,1674,4772,4396,3703,1917,2753,5147,2664,4733,2800,4983,2891,2105,2162,
    	2057,3504,267,1911,4593,5379,1738,2144,2117,2872,3939,2882,2041,2572,4253,
    	4942,5110,5529,5161,3528,4567,4036,3891,3051,5036,4549,2862,3922,3422,3025,
    	5365,5537,3459,2169,4517,4664,4128,4245,4723,2684,3045,3515,4284,4952,200,
    	3861,5408,2097,3914,2977,1903,4138,4464,4597,3162,5328,4034,4255,4971,1739,
    	1959,5190,2033,4796,4294,2949,3530,3738,5102,4874,4688,4539,3924,4381,1797,
    	4497,5235,2898,4752,3423,3859,4353,2890,2161,4831,5334,3597,4177,2691,1982,
    	5005,1927,2047,2654,5378,256,4792,4897,2571,5116,2792,2976,3110,1866,4580,
    	4075,5320,3485,2773,5031,2783,4472,3740,4154,4768,1987,4678,5312,4392,3699,
    	4736,4239,4907,3816,4489,2674,98,5207,1858,234,245,3500,5581,4947,2969,
    	2763,3677,4325,5407,2153,3161,69,3908,4186,3598,4977,1949,4361,4249,3483,
    	4764,2543,1868,4373,3723,4452,2111,4114,4552,3918,2897,5230,3814,4513,3377,
    	2683,5528,3675,4347,4655,4611,5333,4162,4812,3715,97,4106,2168,2799,2841,
    	4572,3641,5372,2088,2791,4030,3957,5250,1894,4333,2563,4935,3734,5154,2024,
    	3725,2851,79,4691,4116,4377,5015,4444,5093,90,3894,5536,4902,4503,3855,
    	5202,1852,2143,3100,4173,4716,3934,3378,3639,4319,2152,1930,3518,3886,2889,
    	2160,4816,4313,1862,4544,4576,2938,5364,2975,2110,3966,4468,4786,1672,5311,
    	2570,4927,5146,2533,4732,4532,3896,3747,4987,1991,1800,2958,2798,4133,4592,
    	2643,5171,5327,4100,2078,2690,4708,1975,2871,4357,2040,1884,4289,5371,3520,
    	3942,3044,4890,3479,4941,5109,2014,1792,5406,3109,3719,4728,2742,4110,2553,
    	4548,4458,3457,5010,3810,2052,4078,4516,4758,4671,3671,1673,1922,2142,3034,
    	4158,3702,2752,1802,5224,3503,96,4070,1910,5319,3880,2689,3953,2974,1967,
    	4970,1737,4141,4329,2032,5363,2948,3694,2562,3455,4882,4297,5101,4687,2790,
    	2104,3108,89,3495,3890,2968,4080,3421,4982,4435,5126,5527,4176,4663,3704,
    	4127,3635,1981,5166,4244,5326,1926,1735,3514,4791,4896,4283,3505,266,5196,
    	1845,3099,4683,3913,1902,1786,2151,5229,4277,4508,2772,4033,4471,2682,3419,
    	3024,1916,2663,2096,233,2673,1796,4496,255,4951,95,4025,3858,5526,3596,
    	4360,4064,5318,3938,2653,4596,4763,4252,211,4372,3688,2159,4795,4093,3527,
    	4566,4035,3850,5370,2103,5201,2051,4269,4074,2522,2861,4512,3921,2967,2569,
    	5121,3698,4722,4161,3594,4238,5004,4488,2046,1857,3860,3499,4571,2141,5030,
    	4137,3956,4232,4463,3907,4332,5310,188,3043,3451,1958,4934,4293,2023,2681,
    	3482,2888,265,3737,4767,4873,3873,1986,5092,4391,4538,4451,5362,3107,2095,
    	4906,4751,3813,4172,2568,4352,3376,4946,3674,4019,3474,4654,1731,2881,4976,
    	3714,4105,4543,2840,2937,5160,3805,5325,1865,4224,4029,4467,4785,3666,1893,
    	3844,3484,3042,3415,3023,4926,4153,4677,2789,3374,3746,1779,5223,4443,3815,
    	2087,3854,2762,4811,3676,1851,1974,4324,4356,68,2561,3638,3033,2688,3941,
    	1948,4889,4248,2013,5309,5189,58,3098,2542,3885,4727,2552,4312,2150,3722,
    	4057,5317,78,3106,4113,3917,4376,4757,3630,5165,1671,1921,4901,2632,4157,
    	4346,2532,3590,199,2102,5195,2045,3468,222,2642,5115,3640,4099,3952,1966,
    	4140,4328,1883,4575,3799,4881,4296,3478,3660,2782,1837,3733,3097,1985,1669,
    	2850,1791,2957,2887,2741,2149,4731,2077,3893,5222,4502,3809,2680,2086,3670,
    	4715,3933,4591,2567,4318,2870,2560,2094,2039,3517,4682,4940,2140,5009,1861,
    	4012,88,4069,3879,4507,4547,5120,4215,3693,3454,3624,3041,2731,3370,1915,
    	2662,4531,3494,5361,3837,1799,5194,4810,4434,4132,3634,4707,3446,3937,4288,
    	4251,4969,1734,2031,2947,3526,1844,4565,5153,2886,3718,2139,4981,77,4686,
    	4109,1785,2956,2860,3920,4457,4276,3456,5308,4077,2076,4670,3418,4721,1726,
    	176,1771,2880,3701,2751,1980,1925,2788,5159,4790,4895,3502,4024,4136,4462,
    	1909,3032,3410,1736,244,4063,1957,2511,4292,2771,3040,1665,3736,4872,3687,
    	4092,4537,5145,1828,5316,3096,3889,3792,3849,4750,4268,2521,264,3420,4351,
    	3653,4590,4662,4126,4243,46,254,5188,2038,3593,3440,2966,3513,4282,2085,
    	5108,4762,1864,3912,4371,1901,4231,3031,2559,4032,3450,4152,4676,3585,4511,
    	5003,87,3872,1720,4049,2787,2879,1795,4495,5114,2761,2679,4323,3617,3857,
    	5158,4570,67,3595,4018,1947,3473,4247,2093,1730,2781,2652,2030,3404,232,
    	2965,2946,2541,4933,5100,2022,1818,3095,3721,4112,3804,3916,4223,2138,4945,
    	4073,3665,3843,3414,4345,4171,3697,4975,1979,3373,1778,221,4237,3829,5187,
    	4789,4487,2075,1856,3498,2678,4542,3906,2936,253,3365,4466,3732,4784,57,
    	2849,3481,4925,3579,4004,5002,3892,4450,2044,4056,4501,2672,5307,3629,3812,
    	5113,4714,2631,3932,3375,4317,3673,1973,3589,4653,4355,3516,3467,1762,5152,
    	2780,4888,3713,4761,76,2012,4104,1860,4370,2839,4726,263,4900,4028,3433,
    	3798,1892,3030,3659,4756,1836,4530,1668,1920,4156,3784,4974,1798,4442,4131,
    	2621,5306,3853,4569,1850,4706,4287,1713,3637,3951,1965,2878,1660,4327,2084,
    	5144,4880,2021,3359,3717,2964,3884,4108,4311,4011,4456,5091,2558,4076,3397,
    	3022,4669,4214,1670,2869,3623,86,3700,2730,3369,2750,2531,1752,4939,5107,
    	3836,3501,3609,5151,1908,2641,4541,4681,4098,187,2935,3445,1882,4506,3029,
    	5186,4783,3477,2083,1790,3888,2740,1914,2661,3995,2557,3808,4661,4125,3669,
    	4242,3572,4968,1725,1972,85,1770,2955,243,3512,4281,3936,2074,5099,2011,
    	1654,2963,3911,2610,5143,1900,4725,2551,4068,3878,4564,4031,3409,2510,2859,
    	2779,3692,4755,3453,1978,1664,1919,2868,4720,2037,1827,4894,1794,4494,3493,
    	3791,4938,5106,3856,4433,3652,2677,3633,2770,1964,4135,4461,2651,45,2954,
    	3439,1733,1956,2073,4291,1843,2671,4871,2500,1784,4536,4072,4275,4749,3352,
    	3584,3696,3417,4350,4236,4967,1719,2029,4048,4486,1855,2945,3497,3775,5098,
    	4680,3616,4023,1705,3905,210,4505,4062,3403,3480,5150,4151,3686,75,4675,
    	4091,1817,4449,1913,252,3848,3389,3021,4893,4267,3811,2520,2556,2760,3672,
    	4322,4652,66,4932,2769,3592,84,1946,3828,3712,4246,4103,2838,5090,2540,
    	4563,4027,1696,4230,2670,1891,2877,2858,3915,3449,1647,3364,5142,4719,3578,
    	3871,4344,4003,4441,2489,3020,3852,1849,2934,3564,3636,2867,4460,4017,2036,
    	3472,1729,4924,1955,1761,2953,5105,3883,3731,4310,2072,2848,4535,3803,4222,
    	3432,2778,3664,175,4748,3842,4500,1971,3413,4349,2530,4713,3931,4887,3372,
    	83,1777,4316,3783,4931,2020,2620,2550,2640,4097,3555,5089,1859,1881,4966,
    	1712,2028,1659,220,3476,4150,56,5097,4674,1789,3358,2739,4529,4055,3807,
    	3396,198,3628,3344,2759,3668,1963,4130,2630,4321,231,65,4705,3588,4879,
    	1945,4286,4782,1751,2952,3466,4923,3608,251,4067,3877,3716,4107,2768,3797,
    	4455,3691,34,3452,2876,3658,74,4668,1835,4343,1667,3492,2669,2749,4886,
    	2010,3994,4432,3335,3019,3632,2549,3571,1907,1732,4754,1842,1653,1912,2660,
    	2847,2609,1783,4010,4274,3887,4499,1639,4213,3416,5141,4712,3622,3930,73,
    	4660,2729,4124,3368,4315,4241,4878,3511,3835,4280,4562,4022,209,242,3910,
    	4061,3444,1899,1686,4930,2875,2019,3685,4090,4528,5088,3847,2499,4266,2519,
    	1793,4493,1630,4129,3018,3351,2777,1724,4704,1954,1769,3591,4285,2650,4870,
    	3774,4534,219,2659,4229,2866,1704,2027,4454,3408,2944,3448,2509,4071,4922,
    	5096,4667,1663,3870,3695,2748,1826,3790,4235,3388,4485,1854,3651,3496,1970,
    	4016,1906,3471,2478,1728,44,2857,3904,4885,3438,2009,4718,2548,3802,4221,
    	2767,1695,241,4448,3663,3841,2943,3412,1646,64,2776,3583,4659,4123,1944,
    	3371,4240,1776,2668,1718,72,4651,4047,2539,4279,2488,3711,4869,4102,3615,
    	3563,3909,1962,2837,1898,4026,4747,4877,3402,55,1890,4342,1816,4054,197,
    	4492,4440,3627,2629,3851,1848,1620,3587,2667,3465,2649,3827,2846,4673,3882,
    	3554,4498,4309,3796,2865,2018,2758,3657,3363,1834,4314,1666,63,2658,5087,
    	3577,71,2529,4002,4234,4484,1853,2538,3343,2639,4096,3903,1880,1760,4527,
    	3475,2933,4009,1788,4447,2856,2738,3431,4212,4921,33,3806,2017,3621,22,
    	2942,2728,3367,3667,5086,4650,3782,3834,3710,2619,4101,230,2836,3334,4453,
    	3443,4066,3876,1711,2864,1953,2008,1889,1658,3690,4711,4868,2747,2547,3357,
    	2932,4439,3491,4746,3395,1638,1905,2766,4431,1847,1723,1768,3631,1750,186,
    	3607,3881,1961,1841,4308,3407,2508,1782,4876,1685,4273,2007,4122,2941,1662,
    	4703,2546,2528,1825,4278,3789,3993,2757,3650,1629,1897,2638,4095,4021,3570,
    	43,1943,3437,1879,4060,4666,2537,1652,2608,3684,1787,4491,229,4089,2737,
    	3846,2765,4265,2518,3582,1904,2657,240,1717,4046,2666,3614,4065,3875,2477,
    	4228,3401,3689,3447,4658,2845,1815,4233,4483,208,3869,3490,2931,2498,4430,
    	4710,3902,3350,1896,2656,4015,3826,3470,1727,3773,1840,4446,1703,1781,1952,
    	3801,4272,4220,3362,3662,3840,4867,3411,2006,4526,3576,4001,2648,2545,2855,
    	1775,3387,2835,4020,4702,1619,1888,4059,1759,3683,54,4088,4438,2930,3430,
    	1694,3845,1951,4053,1846,4264,2517,4665,1645,3626,4866,2628,2746,3781,3586,
    	2756,2618,2487,3464,4307,62,3562,1710,1942,4227,1657,3795,2536,239,3356,
    	3656,1833,4649,3868,174,3394,2637,4094,4657,2834,21,1878,4014,3469,1749,
    	1887,185,196,3606,2736,61,3553,3800,1941,4008,4219,3661,3839,207,2535,
    	4211,3620,2727,3366,1774,4490,3992,2854,3833,3874,3342,4306,3569,2647,3442,
    	1651,53,2607,2527,4052,4429,32,3625,228,2844,2627,1722,1877,2655,1767,
    	4482,1839,3463,4701,1780,3333,4271,2735,3794,3406,2507,3655,1832,1661,4445,
    	2497,1824,2853,3788,1637,3349,3649,4058,2745,4648,42,3682,3436,4087,3772,
    	218,2755,1702,4007,4263,2516,60,1684,1940,4210,3619,3581,2726,2534,4437,
    	3386,1716,4045,3832,4656,1838,1628,4226,3613,195,3441,4270,3400,3867,1895,
    	1693,1814,1644,4013,2526,1721,1766,2843,2486,3825,2636,2754,4086,3561,4218,
    	59,2646,3838,2476,3405,4262,227,2506,3361,173,1773,217,3575,1823,4000,
    	3787,3648,4225,41,4481,52,3435,1758,4051,3866,3552,2645,2626,3429,3580,
    	2842,3462,1715,4044,3780,4428,3341,2617,3612,4647,3793,1618,4217,1709,3654,
    	2744,1831,3399,1656,206,3355,1813,1772,1886,31,3393,4436,3824,1748,51,
    	4006,3332,3605,4646,4050,4209,3618,2725,3360,2625,2833,3574,3999,3831,1885,
    	2515,1636,3991,2525,20,3568,2743,1757,2635,1830,1650,1876,2606,1683,3428,
    	184,1765,2734,3779,1627,2616,2524,4005,2505,1708,1655,4208,2634,1822,2724,
    	3354,3786,1875,3647,3830,2496,3392,40,3348,3434,194,1747,4427,3604,3771,
    	2475,1701,2644,50,1714,4043,1764,2832,3990,3611,3385,216,3567,3398,2504,
    	4426,1812,1649,2605,1821,3785,1692,3646,1829,1643,3823,39,4261,2514,2485,
    	1617,3560,2523,3573,3998,2831,183,4042,2495,1874,3610,2723,3347,1756,2733,
    	2513,3770,1811,3427,1700,3551,3778,4216,2615,3822,3384,19,1707,3340,1763,
    	172,3353,2633,3997,3391,1691,215,1642,30,1820,1746,2732,3603,1755,2484,
    	2624,3559,3331,38,3426,3989,3777,2614,49,3566,1635,1706,4041,1648,2604,
    	2623,2512,3550,3390,1682,1810,1745,4207,3602,205,3339,1626,3821,2494,3988,
    	3346,29,3565,3996,3769,4206,171,1699,2603,193,3330,2474,1754,3383,2503,
    	1634,48,3776,2613,1690,37,182,2493,1641,1681,3345,2483,2502,3558,3768,
    	1625,1698,1819,1616,1744,3601,3382,47,3987,3549,2622,1689,2722,2473,1640,
    	2602,3338,2482,3557,1809,18,28,1753,2492,3329,2501,3548,2721,1615,204,
    	3767,1697,1633,36,3337,3381,1680,1743,27,2612,1688,1624,170,3328,17,
    	1808,2481,3556,35,1632,2601,2472,1679,3986,3547,1623,192,203,3336,3766,
    	181,26,1614,2471,2491,3327,1742,1687,1631,2480,2611,1678,16,1613,180,
    	1622,191,3546,2490,2470,15,2600,25,3326,169,24,1612,2479,1677,1621,
    	1676,14,168,2469,2468,1611,23,1610,13,179,12,167,11
	};
	
	
}
class Data3{
	public static final int products[]= {
		48,72,80,108,112,120,162,168,176,180,200,208,252,264,270,
		272,280,300,304,312,368,378,392,396,405,408,420,440,450,456,
		464,468,496,500,520,552,567,588,592,594,612,616,630,656,660,
		675,680,684,696,700,702,728,744,750,760,780,828,882,888,891,
		918,920,924,945,952,968,980,984,990,1020,1026,1044,1050,1053,1064,
		1092,1100,1116,1125,1140,1144,1160,1170,1240,1242,1250,1288,1300,1323,1332,
		1352,1372,1377,1380,1386,1428,1452,1470,1476,1480,1485,1496,1530,1539,1540,
		1566,1575,1596,1624,1638,1640,1650,1672,1674,1700,1710,1716,1736,1740,1750,
		1755,1768,1820,1860,1863,1875,1900,1932,1950,1976,1998,2024,2028,2058,2070,
		2072,2079,2142,2156,2178,2205,2214,2220,2244,2295,2296,2300,2312,2349,2380,
		2392,2394,2420,2436,2450,2457,2460,2475,2508,2511,2548,2550,2552,2565,2574,
		2584,2604,2610,2625,2652,2660,2728,2750,2790,2850,2860,2888,2898,2900,2925,
		2964,2997,3016,3036,3042,3087,3100,3105,3108,3128,3213,3220,3224,3234,3250,
		3256,3267,3321,3330,3332,3366,3380,3388,3430,3444,3450,3465,3468,3496,3588,
		3591,3608,3630,3654,3675,3690,3700,3724,3740,3762,3822,3825,3828,3848,3850,
		3861,3876,3906,3915,3944,3978,4004,4060,4092,4095,4100,4125,4180,4185,4216,
		4232,4250,4264,4275,4332,4340,4347,4350,4375,4408,4420,4446,4508,4524,4550,
		4554,4563,4650,4662,4692,4712,4732,4750,4802,4836,4851,4875,4884,4940,4995,
		4998,5032,5049,5060,5070,5082,5145,5166,5175,5180,5202,5236,5244,5324,5336,
		5355,5382,5390,5412,5445,5481,5535,5550,5576,5586,5624,5643,5684,5704,5733,
		5740,5742,5750,5772,5775,5780,5814,5852,5859,5916,5950,5967,5980,5985,6050,
		6076,6125,6138,6150,6188,6232,6292,6324,6348,6370,6375,6380,6396,6435,6460,
		6498,6525,6612,6650,6669,6728,6762,6786,6808,6820,6825,6831,6875,6916,6975,
		6993,7038,7068,7084,7098,7125,7150,7192,7203,7220,7245,7250,7252,7254,7326,
		7436,7497,7540,7544,7546,7548,7605,7623,7688,7749,7750,7803,7820,7866,7986,
		8004,8036,8050,8060,8073,8085,8092,8118,8125,8140,8228,8325,8330,8364,8372,
		8379,8415,8436,8450,8470,8526,8556,8575,8584,8613,8625,8658,8670,8721,8740,
		8788,8874,8918,8925,8932,9009,9020,9044,9075,9114,9135,9176,9196,9207,9225,
		9250,9310,9348,9350,9405,9438,9486,9512,9522,9548,9555,9594,9620,9625,9724,
		9747,9765,9860,9918,9945,9975,10092,10108,10143,10150,10168,10179,10212,10250,10450,
		10540,10556,10557,10580,10602,10625,10647,10660,10725,10788,10830,10850,10868,10875,10878,
		10881,10948,10952,10989,11020,11050,11115,11132,11154,11270,11284,11316,11319,11322,11375,
		11385,11396,11492,11532,11625,11655,11662,11780,11781,11799,11830,11858,11875,11979,12005,
		12006,12054,12075,12136,12138,12177,12236,12342,12350,12495,12546,12580,12628,12650,12654,
		12675,12705,12716,12789,12834,12844,12876,12915,12950,12987,13005,13034,13156,13167,13182,
		13310,13311,13340,13377,13448,13455,13468,13475,13671,13764,13794,13804,13875,13923,13940,
		13965,14014,14022,14025,14036,14060,14157,14210,14212,14229,14260,14268,14283,14350,14355,
		14375,14391,14450,14535,14756,14812,14875,14877,14924,14950,15004,15028,15125,15138,15162,
		15190,15225,15252,15318,15345,15375,15428,15548,15561,15580,15675,15730,15778,15870,15884,
		15903,15925,15939,15950,16150,16182,16245,16275,16317,16428,16492,16562,16575,16588,16625,
		16698,16731,16796,16820,16905,16965,16974,16983,17020,17050,17204,17238,17298,17493,17595,
		17612,17732,17745,17787,17875,17908,17980,18009,18050,18081,18125,18130,18135,18204,18207,
		18315,18326,18513,18525,18590,18634,18676,18772,18819,18837,18850,18860,18865,18975,18981,
		19074,19220,19228,19251,19266,19314,19375,19425,19516,19550,19551,19604,19652,19665,19684,
		19773,19844,19894,19964,19965,20090,20097,20125,20150,20172,20230,20295,20332,20349,20350,
		20482,20570,20646,20691,20825,20956,21021,21033,21054,21125,21164,21175,21266,21315,21402,
		21460,21483,21525,21645,21658,21675,21692,21812,21850,21879,21964,21970,22022,22185,22218,
		22295,22425,22506,22542,22550,22707,22724,22743,22785,22878,22940,22977,22990,23125,23188,
		23275,23276,23322,23375,23452,23548,23595,23667,23715,23751,23780,23805,23826,23828,23925,
		23985,24050,24206,24225,24244,24273,24453,24548,24633,24642,24650,24794,24795,24843,25012,
		25025,25047,25172,25230,25270,25375,25382,25389,25420,25461,25575,25625,25636,25641,25857,
		25916,25947,26026,26125,26350,26404,26411,26450,26505,26588,26650,26862,26908,27075,27125,
		27195,27306,27380,27404,27436,27489,27508,27531,27550,27625,27676,27716,27830,27885,27951,
		28126,28158,28175,28275,28305,28322,28413,28611,28652,28730,28798,28830,28899,28971,29155,
		29282,29302,29325,29348,29406,29450,29478,29575,29601,29645,29716,29766,29841,30015,30044,
		30135,30225,30258,30303,30340,30345,30525,30628,30668,30723,30758,30855,30875,30932,30969,
		31059,31213,31262,31365,31372,31434,31450,31581,31625,31635,31654,31790,31899,31977,32085,
		32103,32110,32116,32186,32375,32487,32585,32708,32725,32775,32946,32955,33033,33201,33212,
		33275,33292,33327,33350,33418,33524,33579,33620,33759,33813,33825,34276,34317,34485,34606,
		34684,34713,34850,34914,34983,35035,35055,35090,35150,35322,35378,35525,35588,35650,35739,
		35836,35875,35972,36075,36125,36244,36309,36556,36575,36822,36946,36963,36975,37004,37030,
		37076,37107,37191,37323,37375,37444,37468,37510,37518,37570,37791,37845,37905,37975,38073,
		38295,38318,38332,38675,38709,38870,38950,38962,39039,39325,39445,39494,39525,39556,39627,
		39675,39710,39875,39882,39886,39897,39975,40052,40204,40222,40293,40362,40375,40455,40508,
		40817,40898,40959,41070,41154,41262,41325,41405,41492,41503,41574,41745,41876,42021,42050,
		42189,42237,42284,42435,42476,42483,42550,42625,42772,42826,43095,43197,43225,43245,43263,
		43732,43911,43923,43953,44109,44175,44198,44217,44252,44275,44289,44506,44649,44764,44770,
		44919,44950,44954,45125,45254,45325,45356,45387,45619,45747,45815,46137,46475,46585,46748,
		46893,46930,47068,47125,47138,47150,47151,47175,47212,47396,47481,47619,47685,47804,48050,
		48165,48279,48285,48314,48334,48484,48668,48807,48875,49010,49036,49049,49077,49126,49130,
		49419,49610,49735,49818,49972,50025,50127,50225,50286,50375,50430,50468,50575,50578,50692,
		50875,51129,51205,51425,51615,51646,51842,51909,52173,52234,52275,52316,52325,52371,52390,
		52514,52598,52635,52725,52767,52972,52983,53067,53165,53428,53475,53482,53505,53613,53650,
		53754,53958,53998,54145,54188,54418,54549,54625,54910,54925,55055,55223,55233,55419,55506,
		55545,55594,55796,55825,55924,56265,56277,56355,56375,56525,56637,57122,57188,57195,57350,
		57475,57477,57498,57681,57722,57868,57967,58190,58305,58311,58425,58443,58870,59204,59241,
		59409,59450,59565,59644,59675,59774,59823,59829,60125,60236,60306,60333,60515,60543,60775,
		61132,61226,61347,61364,61370,61605,61625,61642,61659,61731,61828,61893,61985,62271,62361,
		62530,62678,62814,63075,63175,63206,63426,63455,63550,63825,63916,64124,64141,64158,64239,
		64467,64676,65065,65219,65348,65366,65596,65598,65702,65875,65975,66033,66092,66125,66297,
		66470,66625,66748,66759,66861,67146,67155,67270,67425,67431,67599,67881,67925,68265,68306,
		68324,68425,68450,68590,68614,68770,68782,68875,68894,68913,69003,69290,69454,69575,69597,
		69629,69874,69938,70315,70395,70525,70587,70602,70642,70707,70725,70805,71094,71188,71225,
		71668,71687,71825,71995,72075,72261,72358,72471,72501,72964,73002,73036,73205,73255,73346,
		73515,73593,73625,73689,73695,73964,74415,74431,74698,74727,74907,74958,75429,75645,75803,
		75850,75867,76342,76475,76874,76895,77077,77121,77198,77372,77469,77763,77996,78039,78155,
		78166,78292,78351,78585,78625,78771,78884,78897,78925,79135,79475,80073,80142,80223,80275,
		80465,80475,80631,80852,80937,80997,81466,81548,81549,81627,82225,82251,82365,82418,82522,
		82654,82708,83030,83259,83375,83391,83398,83421,83486,83545,83810,84050,84175,84249,84303,
		84721,85514,85683,85782,85918,86025,86247,86275,86428,86515,86583,86756,86779,87125,87172,
		87285,87362,87412,87542,87725,87875,88102,88305,88412,88445,88806,88825,88837,89001,89125,
		89175,89590,89661,89930,90117,90354,90364,90459,91091,91143,91234,91839,92046,92055,92225,
		92365,92414,92463,92510,92575,93058,93092,93275,93357,93775,93795,93925,94017,94178,94221,
		94622,94809,95139,95325,95571,95795,95830,95874,96026,96237,96278,96425,96596,97006,97175,
		97375,97405,97526,97556,97682,98022,98049,98394,98397,98441,98494,98553,98716,98735,99127,
		99275,99567,99705,99715,100510,100555,100719,100793,100905,101062,102051,102245,102459,102487,102557,
		102675,102885,102921,103075,103155,103156,103173,103246,103341,103675,103935,104044,104181,104284,104690,
		104811,104907,104975,105125,105154,105183,105524,105710,105754,105903,105963,106227,106375,106641,106782,
		106930,107065,107525,107559,107653,107822,108086,108537,109089,109142,109174,109330,109388,109417,109503,
		109554,110019,110075,110331,110495,110789,110825,110946,111265,111476,111910,111925,112047,112375,112385,
		112406,112437,112651,113135,113553,113775,114057,114308,114513,115258,115292,115311,115797,116058,116242,
		116402,116522,116725,116932,116963,117249,117325,117334,117438,117670,117711,117845,117875,118490,119119,
		119164,119187,119306,120125,120175,120213,120785,120802,120835,121121,121670,121923,121975,122018,122199,
		122525,122815,122825,123025,123627,123783,123823,123981,124025,124468,124545,124558,124775,124930,125097,
		125229,125426,125541,125715,125829,125902,125948,126075,126445,127075,127426,127534,127738,127756,128018,
		128271,128673,128877,128986,129115,129311,129514,129605,130134,130203,130585,130975,131043,131118,131285,
		131313,131495,132153,132158,132275,132618,133052,133133,133209,133342,133570,133705,134113,134125,134162,
		134199,134385,134895,134995,135014,135531,135575,136045,136214,136325,136367,136851,137275,137547,137566,
		137924,138069,138229,138621,138765,138985,139113,139564,139587,139601,139638,140714,140777,141267,141933,
		142025,142228,142538,142766,142805,142970,143143,143375,143745,143811,144039,144279,144305,144417,144925,
		145475,145509,145521,146234,146289,146334,146523,146566,146575,147033,147175,147436,147591,147706,147741,
		147994,148010,148625,148666,148707,148925,149435,149702,149891,150183,150590,150765,150898,151294,151525,
		151593,152218,152438,153062,153065,153410,153425,153729,154105,154652,154693,154869,155771,156066,156325,
		156426,156674,156695,157035,157325,157339,157604,157731,158015,158389,158565,158631,158804,158875,159562,
		159790,160173,160225,160395,161161,161253,161414,161733,161975,162129,162578,163370,163415,163713,163761,
		163990,163995,164169,164255,164331,164738,164983,165025,165886,166175,166419,166634,167042,167214,167865,
		168175,168609,168674,169099,169169,169756,170126,170338,170765,171125,171275,171462,171475,171535,171925,
		171941,171955,172235,172546,172822,172887,172975,173225,173635,174087,174097,174363,174603,174685,174783,
		174845,174902,175491,175972,176001,176157,176505,176605,177023,177489,177735,177970,178126,178334,178746,
		178802,178959,179075,180154,180761,180895,181203,181447,181917,182505,182590,182666,182819,183027,183365,
		183425,183483,183799,184093,184382,184910,185725,186093,186238,186694,186702,186745,186837,186998,187187,
		187395,187775,188108,188139,188518,188853,188922,188993,189625,190333,190463,190855,191139,191301,191425,
		191607,191634,191675,192027,192185,192995,193325,193430,193479,194271,194463,194579,194996,195201,195415,
		195730,196075,196137,196677,197098,197846,198237,198927,199082,199927,200013,200158,200355,200725,201243,
		202027,202521,202612,203203,203319,203522,203665,204321,204425,205751,205942,206045,206305,206349,206635,
		206886,207214,207575,208075,208444,208495,208658,208715,209209,209457,209525,210125,210749,210826,211071,
		212602,213342,213785,213807,214149,214225,214291,214455,214774,214795,215747,215878,216775,216890,217217,
		217341,217558,217906,218405,218530,218855,219351,219373,219501,219849,220255,221030,221122,221221,221559,
		221991,222015,222111,222425,222999,223706,223975,224516,224553,224825,224939,225446,225885,225998,226347,
		226525,226941,228085,228206,228327,228475,228657,228718,228781,229586,229593,229957,230115,230318,231035,
		231275,231725,231978,232101,232562,232645,232730,232934,233206,233818,234025,234099,234175,234639,235011,
		235246,235445,235543,235586,236406,236555,237429,237614,238206,239071,239343,239575,239685,240065,240149,
		240526,240695,240737,240994,241129,242121,242515,243089,243815,243867,243890,244205,244559,244783,245055,
		245985,246123,246202,246235,247107,247225,247247,248788,248829,248897,249067,249158,249951,250325,250563,
		250821,251275,252586,252655,253011,253175,253253,254634,255189,255507,255626,256711,257193,258115,258819,
		258874,259233,259259,259325,259407,259666,260110,260642,260678,260710,261326,261443,261725,262353,262885,
		263097,263302,264275,264385,265475,265727,265837,266955,267189,267197,267325,267501,267674,268119,268203,
		269059,269555,270193,270215,270231,270802,272194,272855,272935,273325,273581,273885,273999,274022,274846,
		275684,276573,276575,277365,277574,278018,278179,278369,278690,279357,279775,280041,280053,280497,281015,
		282302,282777,283383,283475,284053,284258,284954,285131,285770,287287,287451,287638,287738,288145,288463,
		288827,289289,290145,290605,290966,291005,291305,291893,292175,292201,292494,293335,293595,293854,294151,
		294175,295075,295647,296225,296769,296989,297910,298265,298623,298775,299299,299367,300237,300713,302005,
		303025,303646,303862,303918,304175,304606,305045,305283,305762,305767,305942,306397,306475,307582,308074,
		308357,308913,309442,310329,310821,311170,311395,312325,312666,312987,313565,314019,314041,314171,314534,
		314755,314870,315425,315514,316239,316342,316825,317471,318478,318565,318734,318835,318903,319319,319345,
		319390,320013,320045,322161,322465,323449,323785,323817,324818,325335,325622,325703,325822,326337,326859,
		326975,327795,328757,329623,330395,331075,331177,331298,331545,331683,331731,333355,333925,335405,335559,
		335699,336091,336743,336774,336973,337502,337535,338169,338675,338997,339031,339521,340442,340535,341341,
		341446,341734,341887,342309,343077,343915,344379,344729,344810,345477,347282,347633,347967,348725,348843,
		349095,349401,349525,349809,350727,350987,351538,351785,352869,353379,353717,354609,355570,355946,356345,
		356421,356915,357309,357425,359414,359513,360778,360789,361361,361491,361675,362674,363562,364021,364154,
		364994,365585,365835,366415,367114,368039,369265,369303,369985,370025,370139,371665,371722,372775,373182,
		373737,374255,375193,375683,376475,377245,377377,378235,378301,378879,378917,380494,380545,381095,381938,
		381951,381997,382075,382109,382655,383439,383525,384307,384659,384826,385526,386425,386630,387686,388311,
		388531,389499,390165,390166,390963,391017,391065,391534,391685,391989,393421,394010,394953,395937,397010,
		397822,397969,398866,398905,399475,400078,400673,400775,401511,401698,401882,402866,403403,403535,404225,
		406203,406334,406445,406802,406847,407407,407827,408291,408425,409975,410669,410839,411033,411845,412114,
		412269,413075,413526,413678,414715,415454,416361,416585,417027,417074,417175,417571,417605,418035,419881,
		421685,422807,423243,423453,424390,424589,424762,424879,425258,425315,425546,425845,426374,426387,427025,
		427063,427431,428655,429598,429913,430606,431365,431457,431607,432055,435638,435953,436449,437255,438741,
		438991,440657,440781,440818,443989,444925,445315,445835,445991,446369,446865,447005,447083,447146,447811,
		447925,448063,450262,450385,451451,453299,453871,454138,454181,454597,455469,455793,455877,456025,456475,
		456665,456909,458643,458689,458913,458983,459173,460955,461373,462111,462275,462346,462553,462722,464163,
		465595,466697,466735,466755,467495,468999,469567,470327,471295,471801,472305,472549,473271,474513,474734,
		476749,477158,477717,478101,479085,480491,480766,481481,481574,482734,483575,484561,485537,486098,486266,
		487227,487475,487490,488433,488733,489325,490637,491878,492499,492745,493025,494615,496223,496947,497705,
		497798,498883,499681,500395,501787,502918,503234,505161,505325,506253,506530,507566,508079,508277,508805,
		508898,509675,510663,511819,512006,512169,512601,512746,512981,514786,514855,516925,516971,517215,517979,
		518035,519622,520331,520421,520923,521110,521594,521645,523957,527065,527307,528143,529529,531505,532763,
		533355,533533,533919,535717,536393,536558,536935,537251,539121,539695,540175,541167,541282,541717,542087,
		542225,542659,543286,543895,544011,544765,544825,545054,545343,546231,546325,547491,548359,550671,551614,
		552575,552805,555458,555611,555814,555841,557566,557583,558467,559265,559682,559773,561290,562438,563615,
		563914,564775,564949,564995,567853,568178,569023,570515,570741,571795,572242,572663,572907,573562,573965,
		574678,575795,576583,577239,578289,578347,579945,580601,581405,581529,581647,581825,582335,582958,583015,
		583219,584545,584647,585249,585599,587301,588115,588965,590359,591015,593021,593929,594035,594146,594473,
		595441,595515,596183,596733,598299,600117,600281,600457,600691,601315,602485,602547,602823,603725,603911,
		604299,604877,605098,607202,609501,609725,610203,612157,613118,614422,615043,615505,616975,618171,618233,
		620194,620289,620517,620806,620977,621970,622895,623162,623181,623441,624169,625611,625807,628694,630539,
		631465,633919,634114,634933,636585,637143,637887,638319,639065,639331,639561,640211,640871,644397,644725,
		645337,645909,647185,648907,649078,649165,650275,651605,651695,651775,651833,653315,653429,653457,654493,
		655402,656183,656903,657662,658255,659525,659813,661227,662966,663803,664411,665482,669185,670719,671099,
		675393,676286,677005,677846,680485,680846,681207,682486,683501,683675,684574,685055,685069,687115,687242,
		687401,689210,689843,692461,692714,693519,693842,693935,694083,695045,696725,696787,700553,700843,701437,
		702559,702658,704099,705686,705755,708883,709142,709423,709631,710645,712101,712327,712385,714425,715737,
		719095,719345,720575,720797,721149,722361,724101,724594,725249,726869,727415,729147,729399,729554,730303,
		730639,730825,731235,733381,734635,734638,735034,737426,737817,737891,742577,743002,743774,744107,744775,
		746697,748867,749177,751502,751709,754354,754377,754851,755573,756613,757393,758582,759115,759655,759795,
		761349,761453,761515,762671,763347,764405,764855,768009,768955,769119,770185,772179,773605,773927,774566,
		774706,775489,777925,779433,781665,782254,782391,782971,783959,785213,785519,785806,786335,787175,788785,
		789061,790855,790993,791282,792281,793117,796195,796835,798475,798721,800513,803551,804287,804837,806113,
		809042,809627,811923,812045,812383,813967,814055,814555,814929,815269,816221,817581,817663,818363,818662,
		823361,824182,824551,827421,828134,828245,828269,828971,829226,829939,830297,830414,831575,831649,832117,
		833187,833721,836349,836969,837199,838409,839523,839914,841841,841935,843479,843657,843755,845871,850586,
		851105,852267,853615,854335,858363,858458,859027,860343,861707,862017,862025,866723,866822,868205,870758,
		872053,872275,873422,874437,876826,877591,877933,878845,884051,884374,885391,886414,887777,888925,889778,
		889865,891219,893809,894179,894691,896506,898535,898909,900358,901945,906059,906685,907647,908831,908905,
		910385,910803,912247,912373,912485,914641,916487,917662,917785,918731,919677,921475,921557,921633,924482,
		926497,926782,927707,927979,929305,930291,931209,932955,933658,934743,935693,936859,943041,947546,947807,
		949003,950521,951142,951171,951235,952679,954845,955451,959077,960089,961961,962065,963815,964894,966329,
		966575,969215,971509,971618,973063,973617,975415,978835,979693,980837,983103,983411,985025,986493,988057,
		988418,989417,990437,990698,990847,992525,994449,994555,994903,997165,997339,997694,998223,998963,1000195,
		1004245,1004663,1004705,1005238,1006733,1007083,1007165,1012894,1013173,1014101,1014429,1015835,1016738,1016769,1017005,
		1018381,1021269,1023729,1024309,1024426,1026817,1026861,1028489,1030285,1030863,1032226,1033815,1034195,1036849,1037153,
		1038635,1039071,1040763,1042685,1049191,1053987,1056757,1057978,1058529,1058743,1059022,1060975,1061905,1062761,1063145,
		1063517,1063713,1063865,1065935,1066121,1067857,1070167,1070558,1070797,1072478,1073995,1076515,1076537,1078259,1083047,
		1083121,1084039,1085773,1085926,1086891,1088153,1089095,1094331,1094951,1095274,1096381,1099825,1100869,1101957,1102045,
		1102551,1103414,1104299,1105819,1106139,1106959,1107197,1114366,1114503,1114673,1115569,1115661,1117865,1119371,1121549,
		1121894,1123343,1125655,1127253,1131531,1132058,1132681,1133407,1135234,1135345,1136863,1137873,1139677,1140377,1146442,
		1147619,1155865,1156805,1157819,1159171,1159543,1161849,1162059,1162213,1169311,1171001,1172354,1173381,1175675,1178709,
		1181257,1182446,1183301,1186835,1186923,1187329,1191547,1192895,1195061,1196069,1196506,1196569,1198483,1199266,1201915,
		1203935,1206835,1208938,1209271,1210547,1211573,1213511,1213526,1213563,1213682,1215245,1215487,1215665,1216171,1218725,
		1225367,1227993,1229695,1230383,1234838,1236273,1239953,1242201,1242989,1243839,1244495,1245621,1245811,1255133,1255501,
		1257295,1257949,1257962,1258085,1259871,1262723,1263661,1266325,1266749,1267474,1268915,1269359,1272245,1272467,1274539,
		1275879,1277479,1279091,1280015,1281137,1281865,1281974,1282633,1284899,1285999,1286965,1287687,1292669,1293853,1294033,
		1295723,1299055,1300233,1301027,1302775,1303985,1306137,1306877,1310133,1310278,1314542,1315239,1316978,1322893,1325467,
		1326561,1329621,1331729,1334667,1336783,1338623,1339634,1340003,1341395,1344718,1344759,1346891,1349341,1349834,1350537,
		1351166,1353205,1354111,1354886,1356277,1356901,1358215,1362635,1365581,1368334,1370369,1370386,1372019,1376493,1379035,
		1381913,1386723,1388645,1389223,1389535,1390173,1392377,1393915,1396031,1399205,1400273,1400487,1403207,1403225,1405943,
		1406095,1406587,1409785,1410031,1412327,1414127,1414562,1416389,1420445,1421319,1422169,1423807,1426713,1428163,1430605,
		1431382,1432417,1433531,1433729,1433905,1436695,1437293,1442399,1442926,1446071,1447341,1447873,1448161,1448402,1454089,
		1457395,1457427,1459354,1459759,1465399,1466641,1468987,1469194,1472207,1482627,1483339,1485365,1486047,1486667,1488403,
		1489411,1492309,1496541,1497067,1497238,1503593,1507121,1507857,1508638,1511653,1512118,1512745,1514071,1515839,1516262,
		1518005,1519341,1519817,1524733,1525107,1526657,1529099,1531309,1532795,1533433,1536055,1536639,1542863,1544491,1548339,
		1550485,1552015,1552661,1554925,1557905,1563419,1565011,1566461,1567247,1571735,1575917,1582009,1582559,1583023,1585285,
		1586126,1586899,1586967,1588533,1589483,1600313,1602403,1604986,1605837,1608717,1612682,1616197,1616402,1617122,1618211,
		1619527,1622695,1628889,1629887,1635622,1638505,1639187,1641809,1642911,1644155,1655121,1657415,1657466,1661569,1663705,
		1670053,1671241,1671549,1675333,1681691,1682681,1682841,1685509,1687829,1689569,1690715,1691701,1692197,1694173,1694407,
		1694615,1698087,1698619,1701343,1701931,1702115,1702851,1706215,1709659,1711435,1711463,1718105,1719663,1721573,1722202,
		1723025,1727878,1729937,1731785,1734605,1735327,1739881,1742293,1750507,1751629,1753037,1756645,1758531,1760213,1761319,
		1764215,1769261,1771774,1772855,1773593,1773669,1776481,1778498,1781143,1786499,1790921,1791946,1792021,1794611,1794759,
		1798899,1801751,1804231,1804786,1806091,1807117,1811485,1812446,1813407,1818677,1820289,1820523,1822139,1823885,1825579,
		1826246,1834963,1836595,1837585,1843565,1847042,1847677,1849243,1852201,1852257,1852462,1856261,1857505,1859435,1869647,
		1870297,1872431,1877953,1878755,1879537,1885885,1886943,1891279,1894487,1896455,1901211,1901501,1907689,1908386,1910051,
		1916291,1920983,1922961,1924814,1929254,1930649,1933459,1936415,1936765,1939751,1944103,1945349,1951481,1952194,1955635,
		1956449,1957703,1958887,1964515,1965417,1968533,1971813,1973699,1975103,1975467,1976777,1978205,1979939,1980218,1982251,
		1984279,1987453,1988623,1994707,1999283,1999591,1999898,2002481,2002847,2007467,2009451,2011373,2017077,2019127,2019719,
		2022605,2024751,2026749,2032329,2040353,2044471,2046655,2048449,2050841,2052501,2055579,2056223,2060455,2062306,2066801,
		2070107,2070335,2071771,2073065,2076035,2079511,2092717,2099785,2100659,2111317,2114698,2116543,2117843,2120393,2121843,
		2125207,2126465,2132273,2132902,2137822,2141737,2145913,2146145,2146981,2147073,2150477,2153437,2155657,2164389,2167055,
		2167957,2170679,2172603,2172821,2176895,2181067,2183555,2188021,2189031,2192065,2193763,2200429,2203791,2204534,2207161,
		2209339,2210351,2210935,2212873,2215457,2215763,2216035,2219399,2221271,2224445,2234837,2237411,2238067,2241265,2242454,
		2245857,2250895,2257333,2262957,2266627,2268177,2271773,2274393,2275229,2284997,2285258,2289443,2293907,2294155,2301817,
		2302658,2304323,2311205,2313649,2316955,2320381,2329187,2330038,2334145,2336191,2338919,2340503,2343314,2345057,2357381,
		2359379,2362789,2363153,2363486,2367001,2368333,2368865,2372461,2377855,2379189,2382961,2386241,2388701,2396009,2397106,
		2399567,2405347,2407479,2412235,2416193,2419023,2422109,2424499,2424603,2425683,2428447,2429045,2442862,2444923,2445773,
		2453433,2459303,2461462,2466827,2469901,2471045,2473211,2476441,2476745,2481997,2482597,2486199,2494235,2497759,2501369,
		2501917,2505919,2513095,2519959,2532235,2536079,2541845,2542903,2544971,2551594,2553439,2561065,2571233,2572619,2580565,
		2580991,2581934,2582827,2583303,2585843,2589151,2591817,2592629,2598977,2600507,2603209,2611037,2612233,2614447,2618629,
		2618998,2624369,2630257,2631218,2636953,2640239,2641171,2644213,2644945,2647555,2648657,2655037,2657661,2667747,2673539,
		2674463,2676395,2678741,2681195,2681869,2687919,2688907,2700451,2705329,2707063,2707179,2709239,2710981,2711471,2714815,
		2718669,2732561,2733511,2737889,2738185,2739369,2750321,2758535,2760953,2764177,2766049,2767787,2769487,2770563,2771431,
		2778693,2785915,2791613,2792387,2798939,2804735,2816033,2820103,2827442,2830145,2831323,2831647,2838085,2857921,2861062,
		2862579,2865317,2866105,2868767,2884637,2886689,2887221,2893757,2893881,2898469,2902291,2904739,2906449,2915674,2922029,
		2926703,2928291,2930885,2937874,2939699,2951069,2951897,2956115,2970327,2977051,2986159,2988073,2991265,2997383,2997797,
		2998165,2999847,3004603,3005249,3007693,3022345,3022438,3025541,3027973,3033815,3033877,3034205,3047653,3055019,3056977,
		3066613,3068891,3078251,3082729,3085771,3087095,3090277,3093409,3093459,3095309,3101527,3102449,3114223,3120469,3124979,
		3130231,3137771,3140486,3144905,3147331,3151253,3154591,3159637,3160729,3168685,3170366,3172047,3192101,3197207,3199353,
		3204935,3206269,3206733,3211817,3230882,3234199,3235687,3243737,3246473,3255482,3267803,3268967,3271021,3275695,3276971,
		3286355,3292445,3295331,3299179,3306801,3307837,3308987,3316411,3328039,3328997,3332849,3339611,3346109,3349085,3361795,
		3363681,3372149,3374585,3377129,3377543,3377915,3379321,3381487,3387215,3390361,3400663,3411067,3414433,3415997,3420835,
		3424361,3425965,3427391,3427887,3445403,3453839,3453987,3457817,3459463,3467443,3479998,3487583,3487627,3491929,3494413,
		3495057,3502969,3514971,3516263,3518333,3531359,3536405,3537193,3542851,3545129,3545229,3558583,3569929,3578455,3585491,
		3595659,3604711,3607315,3607426,3610477,3612791,3614693,3617141,3621005,3624179,3628411,3637933,3646313,3648385,3651583,
		3655847,3660151,3662497,3664293,3665441,3672985,3683017,3692193,3693157,3702923,3706577,3719573,3728153,3735407,3743095,
		3744653,3746953,3748322,3753673,3765157,3771595,3779309,3779831,3780295,3789227,3790655,3800741,3809927,3816131,3817879,
		3827227,3827391,3833459,3856214,3860173,3861949,3864619,3872901,3881273,3900281,3915083,3926629,3928497,3929941,3933137,
		3946813,3946827,3962203,3965315,3973319,3985267,3993743,3997418,4012465,4012547,4024823,4031261,4031705,4035239,4039951,
		4040509,4041005,4042687,4042805,4050553,4055843,4081181,4086511,4089055,4090757,4093379,4103239,4121741,4131833,4133261,
		4138561,4143665,4148947,4153546,4170751,4172201,4180963,4187771,4197431,4219007,4221811,4231283,4241163,4247341,4247887,
		4260113,4260883,4273102,4274803,4277489,4291593,4302397,4305505,4309279,4314311,4319695,4321933,4325633,4352051,4358341,
		4373511,4375681,4392287,4395859,4402867,4405999,4406811,4416787,4425499,4429435,4433549,4436159,4446245,4449731,4458389,
		4459939,4467073,4479865,4486909,4502641,4509973,4511965,4531115,4533001,4533657,4554737,4560743,4565615,4567277,4574953,
		4585973,4586959,4600897,4602578,4609423,4617605,4617931,4619527,4621643,4631155,4632959,4672841,4678223,4688719,4706513,
		4709861,4710729,4721393,4721519,4724419,4729081,4739311,4742101,4755549,4757297,4767521,4770965,4775147,4777721,4780723,
		4789169,4793269,4796351,4803821,4812035,4821877,4822543,4823135,4829513,4834531,4846323,4864057,4871087,4875277,4880485,
		4883223,4884763,4890467,4893779,4903301,4930783,4936409,4940377,4950545,4950967,4951969,4955143,4999745,5009837,5034679,
		5035589,5047141,5050241,5069407,5084651,5097301,5100154,5107739,5135119,5142179,5143333,5155765,5161217,5178013,5211503,
		5219997,5222587,5231281,5240333,5258773,5271649,5276851,5280233,5286745,5292413,5296877,5306917,5316979,5321303,5323153,
		5332255,5343161,5343899,5344555,5357183,5382871,5389969,5397691,5411139,5436299,5448839,5459441,5487317,5511335,5517163,
		5528809,5538101,5551441,5570917,5579977,5590127,5592059,5606135,5617451,5621447,5622483,5634343,5635211,5644387,5651522,
		5656597,5657407,5659927,5677243,5690267,5699369,5713145,5724677,5748431,5756645,5761691,5768419,5783557,5784321,5787191,
		5801131,5818879,5824621,5825095,5827289,5837009,5841557,5852327,5858285,5888069,5891843,5896579,5897657,5898629,5908715,
		5920039,5964803,5972593,5975653,5992765,5996127,5998331,6009133,6024007,6024083,6027707,6047573,6068777,6107155,6129013,
		6153655,6159049,6166241,6170417,6182423,6201209,6224743,6226319,6229171,6230319,6243787,6244423,6247789,6268121,6271811,
		6298177,6305431,6315517,6316751,6322079,6343561,6378985,6387767,6391861,6409653,6412009,6424717,6439537,6447947,6454835,
		6464647,6468037,6483617,6485011,6503453,6528799,6534047,6547495,6578045,6580783,6583811,6585001,6591499,6595963,6608797,
		6649159,6658769,6674393,6675251,6679351,6704017,6709469,6725897,6736849,6752389,6791609,6832679,6876857,6883643,6903867,
		6918791,6930763,6958627,6971107,6979061,6982823,6999643,7005547,7039139,7048421,7050857,7058519,7065853,7068605,7119281,
		7132231,7139269,7152655,7166363,7172191,7206529,7218071,7229981,7243379,7289185,7292311,7296893,7344685,7358377,7359707,
		7367987,7379021,7395949,7401443,7424087,7431413,7434817,7451873,7453021,7464397,7465157,7482377,7517179,7525837,7534519,
		7537123,7556095,7563113,7620301,7624109,7650231,7653043,7685899,7715869,7777289,7780091,7795229,7800127,7829729,7848589,
		7851215,7858097,7867273,7872601,7877647,7887919,7888933,7903283,7925915,7936093,7947563,7966211,7979183,7998403,8026447,
		8054141,8059303,8077205,8080567,8084707,8115389,8138705,8155133,8155351,8176753,8201599,8234809,8238581,8258753,8272201,
		8297509,8316649,8329847,8332831,8339441,8389871,8401553,8420933,8448337,8452891,8477283,8480399,8516807,8544523,8550017,
		8553401,8560357,8609599,8615117,8642273,8675071,8699995,8707621,8717789,8723693,8740667,8773921,8782579,8804429,8806759,
		8827423,8869751,8890211,8894171,8907509,8909119,8930579,8992813,8995921,9001687,9018565,9035849,9036769,9099743,9116063,
		9166493,9194653,9209263,9230371,9303983,9309829,9370805,9379019,9389971,9411631,9414613,9472111,9478093,9485801,9503329,
		9523541,9536099,9549761,9613007,9622493,9640535,9649489,9659011,9732047,9744757,9781739,9806147,9828767,9855703,9872267,
		9896047,9926323,9965009,9968453,9993545,10013717,10044353,10050791,10060709,10083499,10158731,10170301,10188541,10193761,10204859,
		10232447,10275973,10282559,10309819,10314971,10316297,10354117,10383865,10405103,10432409,10482433,10496123,10506613,10511293,10553113,
		10578533,10586477,10610897,10631543,10652251,10657993,10682755,10692677,10737067,10754551,10773529,10784723,10891199,10896779,10938133,
		10991701,10999439,11096281,11137363,11173607,11194313,11231207,11233237,11308087,11342683,11366807,11386889,11393027,11394187,11430103,
		11473481,11473589,11484911,11506445,11516531,11528497,11529979,11560237,11630839,11647649,11648281,11692487,11730961,11731109,11758021,
		11780899,11870599,11950639,12005773,12007943,12023777,12041003,12124937,12166747,12178753,12179993,12264871,12311417,12333497,12404509,
		12447641,12488149,12511291,12540151,12568919,12595651,12625991,12664619,12689261,12713977,12726523,12750385,12774821,12815209,12823423,
		12836077,12853003,12871417,12888227,12901781,12999173,12999337,13018667,13055191,13119127,13184083,13306099,13404989,13435741,13438339,
		13482071,13496749,13538041,13590803,13598129,13642381,13707797,13739417,13745537,13759819,13791559,13863863,13895843,13902787,13955549,
		13957343,13990963,14033767,14088461,14128805,14200637,14223761,14329471,14332061,14365121,14404489,14466563,14471699,14537411,14575951,
		14638717,14686963,14742701,14854177,14955857,14967277,15060079,15068197,15117233,15145247,15231541,15247367,15320479,15340681,15355819,
		15362659,15405791,15464257,15523091,15538409,15550931,15581189,15699857,15735841,15745927,15759439,15878603,15881473,15999503,16036207,
		16109023,16158307,16221281,16267463,16360919,16398659,16414841,16460893,16585361,16593649,16623409,16656623,16782571,16831853,16895731,
		16976747,16999133,17023487,17102917,17145467,17218237,17272673,17349337,17389357,17437013,17529601,17546899,17596127,17598389,17769851,
		17850539,17905151,17974933,18129667,18171487,18240449,18285733,18327913,18378373,18457339,18545843,18588623,18596903,18738539,18809653,
		18812071,18951881,18999031,19060859,19096181,19139989,19424693,19498411,19572593,19591907,19645847,19780327,19805323,19840843,19870597,
		19918169,20089631,20262569,20309309,20375401,20413159,20452727,20607379,20615771,20755039,20764327,20843129,20922427,20943073,21000733,
		21001829,21160633,21209177,21240983,21303313,21688549,21709951,21875251,21925711,21946439,21985799,22135361,22186421,22261483,22365353,
		22450231,22453117,22619987,22772507,22844503,22998827,23207189,23272297,23383889,23437829,23448269,23502061,23716519,24033257,24240143,
		24319027,24364093,24528373,24584953,24783229,24877283,24880481,24971929,24996571,25054231,25065391,25314179,25352141,25690723,25788221,
		25983217,26169397,26280467,26480567,26694131,26782109,26795437,26860699,26948111,26998049,27180089,27462497,27566719,27671597,27698903,
		27775163,27909803,27974183,28050847,28092913,28306813,28713161,28998521,29343331,29579983,29692241,29834617,29903437,29916757,30118477,
		30259007,30663121,30693379,30927079,30998419,31083371,31860737,31965743,32515583,32777819,32902213,33059981,33136241,33151001,33388541,
		33530251,33785551,33978053,34170277,34270547,34758037,35305141,35421499,35609059,35691199,36115589,36321367,36459209,36634033,36734893,
		36998113,37155143,37438043,37864361,37975471,38152661,39121913,39458687,39549707,40019977,40594469,40783879,40997909,41485399,42277273,
		42599173,43105703,43351309,43724491,43825351,44346461,45192947,45537047,45970307,46847789,47204489,47765779,48037937,48451463,48677533,
		49140673,50078671,50459971,52307677,52929647,53689459,53939969,54350669,55915103,57962561,58098991,58651771,59771317,60226417,61959979,
		64379963,64992503,66233081,66737381,71339959,73952233,76840601,79052387,81947069,85147693,87598591,94352849,104553157};
}
class Data4{
	public static final short rankTable2[]= {		
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1608,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,7562,0,0,0,0,0,0,0,7561,0,0,0,7560,0,7559,1607,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,7558,0,0,0,0,0,0,0,7557,0,0,0,7556,0,7555,7554,0,0,0,0,0,0,0,0,7553,0,
		0,0,7552,0,7551,7550,0,0,0,0,7549,0,7548,7547,0,0,7546,7545,0,1606,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,7544,0,0,0,0,0,0,0,7543,0,0,0,7542,0,7541,7540,0,0,0,0,0,0,0,0,7539,0,0,0,7538,0,7537,7536,
		0,0,0,0,7535,0,7534,7533,0,0,7532,7531,0,7530,0,0,0,0,0,0,0,0,0,0,7529,0,0,0,7528,0,7527,7526,0,0,0,
		0,7525,0,7524,7523,0,0,7522,7521,0,7520,0,0,0,0,0,0,7519,0,7518,7517,0,0,7516,7515,0,7514,0,0,0,0,7513,7512,0,7511,
		0,0,0,1605,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7510,0,0,0,0,0,0,0,7509,
		0,0,0,7508,0,7507,7506,0,0,0,0,0,0,0,0,7505,0,0,0,7504,0,7503,7502,0,0,0,0,7501,0,7500,7499,0,0,7498,7497,
		0,7496,0,0,0,0,0,0,0,0,0,0,7495,0,0,0,7494,0,7493,7492,0,0,0,0,7491,0,7490,7489,0,0,7488,7487,0,7486,0,
		0,0,0,0,0,7485,0,7484,7483,0,0,7482,7481,0,7480,0,0,0,0,7479,7478,0,7477,0,0,0,7476,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,7475,0,0,0,7474,0,7473,7472,0,0,0,0,7471,0,7470,7469,0,0,7468,7467,0,7466,0,0,0,0,0,0,7465,
		0,7464,7463,0,0,7462,7461,0,7460,0,0,0,0,7459,7458,0,7457,0,0,0,7456,0,0,0,0,0,0,0,0,0,0,7455,0,7454,7453,
		0,0,7452,7451,0,7450,0,0,0,0,7449,7448,0,7447,0,0,0,7446,0,0,0,0,0,0,0,0,7445,7444,0,7443,0,0,0,7442,0,
		0,0,0,0,0,0,1604,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,7441,0,0,0,0,0,0,0,7440,0,0,0,7439,0,7438,7437,0,0,0,0,0,0,0,0,7436,0,0,0,7435,0,7434,7433,0,
		0,0,0,7432,0,7431,7430,0,0,7429,7428,0,7427,0,0,0,0,0,0,0,0,0,0,7426,0,0,0,7425,0,7424,7423,0,0,0,0,
		7422,0,7421,7420,0,0,7419,7418,0,7417,0,0,0,0,0,0,7416,0,7415,7414,0,0,7413,7412,0,7411,0,0,0,0,7410,7409,0,7408,0,
		0,0,7407,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7406,0,0,0,7405,0,7404,7403,0,0,0,0,7402,0,7401,7400,0,0,
		7399,7398,0,7397,0,0,0,0,0,0,7396,0,7395,7394,0,0,7393,7392,0,7391,0,0,0,0,7390,7389,0,7388,0,0,0,7387,0,0,0,
		0,0,0,0,0,0,0,7386,0,7385,7384,0,0,7383,7382,0,7381,0,0,0,0,7380,7379,0,7378,0,0,0,7377,0,0,0,0,0,0,
		0,0,7376,7375,0,7374,0,0,0,7373,0,0,0,0,0,0,0,7372,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,7371,0,0,0,7370,0,7369,7368,0,0,0,0,7367,0,7366,7365,0,0,7364,7363,0,7362,0,0,0,0,0,0,7361,0,
		7360,7359,0,0,7358,7357,0,7356,0,0,0,0,7355,7354,0,7353,0,0,0,7352,0,0,0,0,0,0,0,0,0,0,7351,0,7350,7349,0,
		0,7348,7347,0,7346,0,0,0,0,7345,7344,0,7343,0,0,0,7342,0,0,0,0,0,0,0,0,7341,7340,0,7339,0,0,0,7338,0,0,
		0,0,0,0,0,7337,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7336,0,7335,7334,0,0,7333,7332,0,7331,0,
		0,0,0,7330,7329,0,7328,0,0,0,7327,0,0,0,0,0,0,0,0,7326,7325,0,7324,0,0,0,7323,0,0,0,0,0,0,0,7322,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7321,7320,0,7319,0,0,0,7318,0,0,0,0,0,0,0,7317,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,1603,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7316,0,0,0,0,0,0,0,7315,0,0,
		0,7314,0,7313,7312,0,0,0,0,0,0,0,0,7311,0,0,0,7310,0,7309,7308,0,0,0,0,7307,0,7306,7305,0,0,7304,7303,0,7302,
		0,0,0,0,0,0,0,0,0,0,7301,0,0,0,7300,0,7299,7298,0,0,0,0,7297,0,7296,7295,0,0,7294,7293,0,7292,0,0,0,
		0,0,0,7291,0,7290,7289,0,0,7288,7287,0,7286,0,0,0,0,7285,7284,0,7283,0,0,0,7282,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,7281,0,0,0,7280,0,7279,7278,0,0,0,0,7277,0,7276,7275,0,0,7274,7273,0,7272,0,0,0,0,0,0,7271,0,7270,
		7269,0,0,7268,7267,0,7266,0,0,0,0,7265,7264,0,7263,0,0,0,7262,0,0,0,0,0,0,0,0,0,0,7261,0,7260,7259,0,0,
		7258,7257,0,7256,0,0,0,0,7255,7254,0,7253,0,0,0,7252,0,0,0,0,0,0,0,0,7251,7250,0,7249,0,0,0,7248,0,0,0,
		0,0,0,0,7247,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7246,0,0,0,7245,0,7244,7243,
		0,0,0,0,7242,0,7241,7240,0,0,7239,7238,0,7237,0,0,0,0,0,0,7236,0,7235,7234,0,0,7233,7232,0,7231,0,0,0,0,7230,
		7229,0,7228,0,0,0,7227,0,0,0,0,0,0,0,0,0,0,7226,0,7225,7224,0,0,7223,7222,0,7221,0,0,0,0,7220,7219,0,7218,
		0,0,0,7217,0,0,0,0,0,0,0,0,7216,7215,0,7214,0,0,0,7213,0,0,0,0,0,0,0,7212,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,7211,0,7210,7209,0,0,7208,7207,0,7206,0,0,0,0,7205,7204,0,7203,0,0,0,7202,0,0,
		0,0,0,0,0,0,7201,7200,0,7199,0,0,0,7198,0,0,0,0,0,0,0,7197,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,7196,7195,0,7194,0,0,0,7193,0,0,0,0,0,0,0,7192,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7191,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,7190,0,0,0,7189,0,7188,7187,0,0,0,0,7186,0,7185,7184,0,0,7183,7182,0,7181,0,0,0,0,0,0,7180,0,7179,7178,
		0,0,7177,7176,0,7175,0,0,0,0,7174,7173,0,7172,0,0,0,7171,0,0,0,0,0,0,0,0,0,0,7170,0,7169,7168,0,0,7167,
		7166,0,7165,0,0,0,0,7164,7163,0,7162,0,0,0,7161,0,0,0,0,0,0,0,0,7160,7159,0,7158,0,0,0,7157,0,0,0,0,
		0,0,0,7156,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7155,0,7154,7153,0,0,7152,7151,0,7150,0,0,0,
		0,7149,7148,0,7147,0,0,0,7146,0,0,0,0,0,0,0,0,7145,7144,0,7143,0,0,0,7142,0,0,0,0,0,0,0,7141,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,7140,7139,0,7138,0,0,0,7137,0,0,0,0,0,0,0,7136,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,7135,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,7134,0,7133,7132,0,0,7131,7130,0,7129,0,0,0,0,7128,7127,0,7126,0,0,0,7125,0,0,0,
		0,0,0,0,0,7124,7123,0,7122,0,0,0,7121,0,0,0,0,0,0,0,7120,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,7119,7118,0,7117,0,0,0,7116,0,0,0,0,0,0,0,7115,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7114,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7113,7112,0,7111,
		0,0,0,7110,0,0,0,0,0,0,0,7109,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7108,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1602,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7107,0,
		0,0,0,0,0,0,7106,0,0,0,7105,0,7104,7103,0,0,0,0,0,0,0,0,7102,0,0,0,7101,0,7100,7099,0,0,0,0,7098,
		0,7097,7096,0,0,7095,7094,0,7093,0,0,0,0,0,0,0,0,0,0,7092,0,0,0,7091,0,7090,7089,0,0,0,0,7088,0,7087,7086,
		0,0,7085,7084,0,7083,0,0,0,0,0,0,7082,0,7081,7080,0,0,7079,7078,0,7077,0,0,0,0,7076,7075,0,7074,0,0,0,7073,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,7072,0,0,0,7071,0,7070,7069,0,0,0,0,7068,0,7067,7066,0,0,7065,7064,0,7063,
		0,0,0,0,0,0,7062,0,7061,7060,0,0,7059,7058,0,7057,0,0,0,0,7056,7055,0,7054,0,0,0,7053,0,0,0,0,0,0,0,
		0,0,0,7052,0,7051,7050,0,0,7049,7048,0,7047,0,0,0,0,7046,7045,0,7044,0,0,0,7043,0,0,0,0,0,0,0,0,7042,7041,
		0,7040,0,0,0,7039,0,0,0,0,0,0,0,7038,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,7037,0,0,0,7036,0,7035,7034,0,0,0,0,7033,0,7032,7031,0,0,7030,7029,0,7028,0,0,0,0,0,0,7027,0,7026,7025,0,0,
		7024,7023,0,7022,0,0,0,0,7021,7020,0,7019,0,0,0,7018,0,0,0,0,0,0,0,0,0,0,7017,0,7016,7015,0,0,7014,7013,0,
		7012,0,0,0,0,7011,7010,0,7009,0,0,0,7008,0,0,0,0,0,0,0,0,7007,7006,0,7005,0,0,0,7004,0,0,0,0,0,0,
		0,7003,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7002,0,7001,7000,0,0,6999,6998,0,6997,0,0,0,0,6996,
		6995,0,6994,0,0,0,6993,0,0,0,0,0,0,0,0,6992,6991,0,6990,0,0,0,6989,0,0,0,0,0,0,0,6988,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,6987,6986,0,6985,0,0,0,6984,0,0,0,0,0,0,0,6983,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,6982,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,6981,0,0,0,6980,0,6979,6978,0,0,0,0,6977,0,6976,6975,0,0,6974,6973,0,6972,0,
		0,0,0,0,0,6971,0,6970,6969,0,0,6968,6967,0,6966,0,0,0,0,6965,6964,0,6963,0,0,0,6962,0,0,0,0,0,0,0,0,
		0,0,6961,0,6960,6959,0,0,6958,6957,0,6956,0,0,0,0,6955,6954,0,6953,0,0,0,6952,0,0,0,0,0,0,0,0,6951,6950,0,
		6949,0,0,0,6948,0,0,0,0,0,0,0,6947,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6946,0,6945,6944,
		0,0,6943,6942,0,6941,0,0,0,0,6940,6939,0,6938,0,0,0,6937,0,0,0,0,0,0,0,0,6936,6935,0,6934,0,0,0,6933,0,
		0,0,0,0,0,0,6932,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6931,6930,0,6929,0,0,0,6928,0,0,0,0,
		0,0,0,6927,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6926,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6925,0,6924,6923,0,0,6922,6921,0,6920,0,0,0,0,6919,6918,
		0,6917,0,0,0,6916,0,0,0,0,0,0,0,0,6915,6914,0,6913,0,0,0,6912,0,0,0,0,0,0,0,6911,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,6910,6909,0,6908,0,0,0,6907,0,0,0,0,0,0,0,6906,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,6905,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,6904,6903,0,6902,0,0,0,6901,0,0,0,0,0,0,0,6900,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,6899,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6898,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6897,
		0,0,0,6896,0,6895,6894,0,0,0,0,6893,0,6892,6891,0,0,6890,6889,0,6888,0,0,0,0,0,0,6887,0,6886,6885,0,0,6884,6883,
		0,6882,0,0,0,0,6881,6880,0,6879,0,0,0,6878,0,0,0,0,0,0,0,0,0,0,6877,0,6876,6875,0,0,6874,6873,0,6872,0,
		0,0,0,6871,6870,0,6869,0,0,0,6868,0,0,0,0,0,0,0,0,6867,6866,0,6865,0,0,0,6864,0,0,0,0,0,0,0,6863,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6862,0,6861,6860,0,0,6859,6858,0,6857,0,0,0,0,6856,6855,0,
		6854,0,0,0,6853,0,0,0,0,0,0,0,0,6852,6851,0,6850,0,0,0,6849,0,0,0,0,0,0,0,6848,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,6847,6846,0,6845,0,0,0,6844,0,0,0,0,0,0,0,6843,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,6842,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,6841,0,6840,6839,0,0,6838,6837,0,6836,0,0,0,0,6835,6834,0,6833,0,0,0,6832,0,0,0,0,0,0,0,
		0,6831,6830,0,6829,0,0,0,6828,0,0,0,0,0,0,0,6827,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6826,6825,
		0,6824,0,0,0,6823,0,0,0,0,0,0,0,6822,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6821,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6820,6819,0,6818,0,0,0,6817,
		0,0,0,0,0,0,0,6816,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6815,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6814,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6813,0,6812,6811,0,0,6810,6809,0,6808,0,0,0,0,6807,6806,0,6805,
		0,0,0,6804,0,0,0,0,0,0,0,0,6803,6802,0,6801,0,0,0,6800,0,0,0,0,0,0,0,6799,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,6798,6797,0,6796,0,0,0,6795,0,0,0,0,0,0,0,6794,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,6793,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,6792,6791,0,6790,0,0,0,6789,0,0,0,0,0,0,0,6788,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6787,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6786,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6785,6784,0,6783,0,0,0,6782,0,
		0,0,0,0,0,0,6781,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6780,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6779,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,1601,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1609,0,0,0,0,0,0,0,6778,0,0,0,6777,0,6776,6775,0,0,0,
		0,0,0,0,0,6774,0,0,0,6773,0,6772,6771,0,0,0,0,6770,0,6769,6768,0,0,6767,6766,0,6765,0,0,0,0,0,0,0,0,
		0,0,6764,0,0,0,6763,0,6762,6761,0,0,0,0,6760,0,6759,6758,0,0,6757,6756,0,6755,0,0,0,0,0,0,6754,0,6753,6752,0,
		0,6751,6750,0,6749,0,0,0,0,6748,6747,0,6746,0,0,0,6745,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6744,0,0,0,
		6743,0,6742,6741,0,0,0,0,6740,0,6739,6738,0,0,6737,6736,0,6735,0,0,0,0,0,0,6734,0,6733,6732,0,0,6731,6730,0,6729,0,
		0,0,0,6728,6727,0,6726,0,0,0,6725,0,0,0,0,0,0,0,0,0,0,6724,0,6723,6722,0,0,6721,6720,0,6719,0,0,0,0,
		6718,6717,0,6716,0,0,0,6715,0,0,0,0,0,0,0,0,6714,6713,0,6712,0,0,0,6711,0,0,0,0,0,0,0,6710,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6709,0,0,0,6708,0,6707,6706,0,0,0,0,6705,0,6704,6703,
		0,0,6702,6701,0,6700,0,0,0,0,0,0,6699,0,6698,6697,0,0,6696,6695,0,6694,0,0,0,0,6693,6692,0,6691,0,0,0,6690,0,
		0,0,0,0,0,0,0,0,0,6689,0,6688,6687,0,0,6686,6685,0,6684,0,0,0,0,6683,6682,0,6681,0,0,0,6680,0,0,0,0,
		0,0,0,0,6679,6678,0,6677,0,0,0,6676,0,0,0,0,0,0,0,6675,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,6674,0,6673,6672,0,0,6671,6670,0,6669,0,0,0,0,6668,6667,0,6666,0,0,0,6665,0,0,0,0,0,0,0,0,6664,6663,
		0,6662,0,0,0,6661,0,0,0,0,0,0,0,6660,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6659,6658,0,6657,0,
		0,0,6656,0,0,0,0,0,0,0,6655,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6654,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6653,0,0,0,6652,
		0,6651,6650,0,0,0,0,6649,0,6648,6647,0,0,6646,6645,0,6644,0,0,0,0,0,0,6643,0,6642,6641,0,0,6640,6639,0,6638,0,0,
		0,0,6637,6636,0,6635,0,0,0,6634,0,0,0,0,0,0,0,0,0,0,6633,0,6632,6631,0,0,6630,6629,0,6628,0,0,0,0,6627,
		6626,0,6625,0,0,0,6624,0,0,0,0,0,0,0,0,6623,6622,0,6621,0,0,0,6620,0,0,0,0,0,0,0,6619,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,6618,0,6617,6616,0,0,6615,6614,0,6613,0,0,0,0,6612,6611,0,6610,0,0,0,
		6609,0,0,0,0,0,0,0,0,6608,6607,0,6606,0,0,0,6605,0,0,0,0,0,0,0,6604,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,6603,6602,0,6601,0,0,0,6600,0,0,0,0,0,0,0,6599,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,6598,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,6597,0,6596,6595,0,0,6594,6593,0,6592,0,0,0,0,6591,6590,0,6589,0,0,0,6588,0,0,0,0,0,0,0,0,6587,6586,0,
		6585,0,0,0,6584,0,0,0,0,0,0,0,6583,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6582,6581,0,6580,0,0,
		0,6579,0,0,0,0,0,0,0,6578,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6577,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6576,6575,0,6574,0,0,0,6573,0,0,0,0,
		0,0,0,6572,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6571,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6570,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6569,0,0,0,6568,0,6567,6566,0,0,0,0,6565,0,6564,6563,0,0,
		6562,6561,0,6560,0,0,0,0,0,0,6559,0,6558,6557,0,0,6556,6555,0,6554,0,0,0,0,6553,6552,0,6551,0,0,0,6550,0,0,0,
		0,0,0,0,0,0,0,6549,0,6548,6547,0,0,6546,6545,0,6544,0,0,0,0,6543,6542,0,6541,0,0,0,6540,0,0,0,0,0,0,
		0,0,6539,6538,0,6537,0,0,0,6536,0,0,0,0,0,0,0,6535,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,6534,0,6533,6532,0,0,6531,6530,0,6529,0,0,0,0,6528,6527,0,6526,0,0,0,6525,0,0,0,0,0,0,0,0,6524,6523,0,6522,
		0,0,0,6521,0,0,0,0,0,0,0,6520,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6519,6518,0,6517,0,0,0,
		6516,0,0,0,0,0,0,0,6515,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6514,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6513,0,6512,6511,0,0,6510,6509,0,6508,0,
		0,0,0,6507,6506,0,6505,0,0,0,6504,0,0,0,0,0,0,0,0,6503,6502,0,6501,0,0,0,6500,0,0,0,0,0,0,0,6499,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6498,6497,0,6496,0,0,0,6495,0,0,0,0,0,0,0,6494,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,6493,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,6492,6491,0,6490,0,0,0,6489,0,0,0,0,0,0,0,6488,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,6487,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,6486,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		6485,0,6484,6483,0,0,6482,6481,0,6480,0,0,0,0,6479,6478,0,6477,0,0,0,6476,0,0,0,0,0,0,0,0,6475,6474,0,6473,0,
		0,0,6472,0,0,0,0,0,0,0,6471,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6470,6469,0,6468,0,0,0,6467,
		0,0,0,0,0,0,0,6466,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6465,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6464,6463,0,6462,0,0,0,6461,0,0,0,0,0,0,
		0,6460,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6459,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,6458,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,6457,6456,0,6455,0,0,0,6454,0,0,0,0,0,0,0,6453,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,6452,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,6451,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6450,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6449,0,0,0,6448,0,6447,6446,0,
		0,0,0,6445,0,6444,6443,0,0,6442,6441,0,6440,0,0,0,0,0,0,6439,0,6438,6437,0,0,6436,6435,0,6434,0,0,0,0,6433,6432,
		0,6431,0,0,0,6430,0,0,0,0,0,0,0,0,0,0,6429,0,6428,6427,0,0,6426,6425,0,6424,0,0,0,0,6423,6422,0,6421,0,
		0,0,6420,0,0,0,0,0,0,0,0,6419,6418,0,6417,0,0,0,6416,0,0,0,0,0,0,0,6415,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,6414,0,6413,6412,0,0,6411,6410,0,6409,0,0,0,0,6408,6407,0,6406,0,0,0,6405,0,0,0,
		0,0,0,0,0,6404,6403,0,6402,0,0,0,6401,0,0,0,0,0,0,0,6400,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,6399,6398,0,6397,0,0,0,6396,0,0,0,0,0,0,0,6395,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6394,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6393,0,
		6392,6391,0,0,6390,6389,0,6388,0,0,0,0,6387,6386,0,6385,0,0,0,6384,0,0,0,0,0,0,0,0,6383,6382,0,6381,0,0,0,
		6380,0,0,0,0,0,0,0,6379,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6378,6377,0,6376,0,0,0,6375,0,0,
		0,0,0,0,0,6374,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6373,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6372,6371,0,6370,0,0,0,6369,0,0,0,0,0,0,0,6368,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6367,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,6366,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,6365,0,6364,6363,0,0,6362,6361,0,6360,0,0,0,0,6359,6358,0,6357,0,0,0,6356,0,0,0,0,
		0,0,0,0,6355,6354,0,6353,0,0,0,6352,0,0,0,0,0,0,0,6351,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,6350,6349,0,6348,0,0,0,6347,0,0,0,0,0,0,0,6346,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6345,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6344,6343,0,6342,0,
		0,0,6341,0,0,0,0,0,0,0,6340,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6339,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6338,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6337,6336,0,6335,0,0,0,6334,0,0,0,0,0,0,0,6333,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,6332,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,6331,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,6330,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6329,0,6328,6327,
		0,0,6326,6325,0,6324,0,0,0,0,6323,6322,0,6321,0,0,0,6320,0,0,0,0,0,0,0,0,6319,6318,0,6317,0,0,0,6316,0,
		0,0,0,0,0,0,6315,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6314,6313,0,6312,0,0,0,6311,0,0,0,0,
		0,0,0,6310,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6309,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6308,6307,0,6306,0,0,0,6305,0,0,0,0,0,0,0,6304,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,6303,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,6302,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,6301,6300,0,6299,0,0,0,6298,0,0,0,0,0,0,0,6297,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,6296,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6295,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6294,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6293,6292,0,6291,0,0,0,6290,0,0,0,0,0,0,0,6289,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,6288,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,6287,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,6286,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1600};
}
















