package com.ujjwal.src ;

public class Constant  
{
	//Operation
	final static byte INS_CREATE_FILE 					= (byte)0xE0;
	final static byte INS_SELECT_FILE 					= (byte)0xA4;
	final static byte INS_UPDATE_BINARY 				= (byte)0xD6;
	final static byte INS_READ_BINARY					= (byte)0xB0;
	
	
	//sw
	final static short SW_FILE_ALREADY_EXIT 			= (short)0x6A89;
	final static short SW_RECORD_REACH_BEFORE_Ne_BYTES 	= (short)0x6282;
	
	//
	final static byte BYTE_ZERO	 						= (byte)0x00;
	final static short SHORT_ZERO 						= (short)0x00;
	final static short MAX_BUFF_SIZE 					= (short)0x7FFF;
	final static short MF_FID							= (short)0x3F00;
	
	//FCP
	final static byte FCP_TEMP							= (byte)0x62;
	final static byte TAG_80 							= (byte)0x80;
	final static byte TAG_82 							= (byte)0x82;
	final static byte TAG_83 							= (byte)0x83;
	final static byte TAG_8A 							= (byte)0x8A;
	
	//FDB
	final static byte DF_FDB   							= (byte)0x38;
	final static byte TRANSPARENT_FDB   				= (byte)0x01;
	
	//LCSI
	final static byte CREATION_STATE   					= (byte)0x01;
	final static byte ACTIVATED		   					= (byte)0x05;
	
	//Select Parameter
	final static byte SELECT							= (byte)0x00;
	final static byte SELECT_CHILD_DF					= (byte)0x01;
	final static byte SELECT_EF_UNDR_CURR_DF			= (byte)0x02;
	final static byte SELECT_PARENT						= (byte)0x03;
	final static byte FCP_REQ							= (byte)0x0C;
	
}
