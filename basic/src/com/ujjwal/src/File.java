package com.ujjwal.src ;

import javacard.framework.*;

public class File  
{
	//Maximum buff size is 0x7FFF
	byte[] buffer1=null;
	//TO-DO: Implement file size more than 0x7FFF
	//byte[] buffer2=null;
	File nextFile = null;	
	
	File(short size){
		if(size < 0x7FFF){
			buffer1 = new byte[size];			
		}
		else{
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
	}

}
