package com.ujjwal.src ;

public class Constant  
{
	//Operation
	final static byte CREATE_FILE 		= (byte)0xE0;
	
	
	//
	final static byte BYTE_ZERO	 		= (byte)0x00;
	final static short SHORT_ZERO 		= (short)0x00;
	final static short MAX_BUFF_SIZE 	= (short)0x7FFF;
	
	//FCP
	final static byte TAG80				= (byte)0x80;	//Data Length
	final static byte TAG82				= (byte)0x82;   //File Descriptor byte
	final static byte TAG83				= (byte)0x83;   //File ID
	final static byte TAG8A				= (byte)0x8A;   //LCSI
}
