package com.chenjy.test.revise;


/**
 * Poker���װÿ���Ƶ�����
 */
public class Poker			
{	
	private String color;	//��ɫ
	private String point;	//����

	private String reg1="SHDC";
	private String reg2="23456789TJQKA";
	
	private int index;		//������
	private short number;		//����������ֵ
	
	public Poker(String i)
	{
		//this.color="S";
		this.point=i;
		this.index=0;
	}
	
	public  Poker(String color, String point){
		setColor(color);
		setPoint(point);
	}
	
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
		//��������ָ��������
		index = reg2.indexOf(this.point) * 4 + reg1.indexOf(this.color);
		return index;
	}
}
