package com.chenjy.test.revise;


/**
 * Poker类封装每张牌的属性
 */
public class Poker			
{	
	private String color;	//花色
	private String point;	//点数

	private String reg1="SHDC";
	private String reg2="23456789TJQKA";
	
	private int index;		//索引号
	private short number;		//点数的整数值
	
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
		//根据数组指定索引号
		index = reg2.indexOf(this.point) * 4 + reg1.indexOf(this.color);
		return index;
	}
}
