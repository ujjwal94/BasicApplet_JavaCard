package com.ujjwal.src ;

import javacard.framework.*;

public class File  
{
	//Maximum buff size is 0x7FFF
	byte[] buffer1=null;
	//TO-DO: Implement file size more than 0x7FFF
	//byte[] buffer2=null;
	
	//File local attribute
	byte FDB;
	byte SFI;
	byte LCSI;
	short FID;
	short dataLength 	= Constant.SHORT_ZERO;	
	byte[] FCP 			= null;
	short FCPLen		= Constant.SHORT_ZERO;
	
	//File reference for heirarchy	
	File parent 		= null;	
	File sibling 		= null;
	File child 			= null;
	
	File(byte[] fcp,short fcpOffset,short fcpLen,short fileSize){
		
		FCP = new byte[fcpLen];
		FCPLen = fcpLen;
		Util.arrayCopy(fcp,fcpOffset,FCP,Constant.SHORT_ZERO,fcpLen);
		
		
		if(fileSize < 0x8000){
			buffer1 = new byte[fileSize];	
			dataLength = fileSize;
		}
		else{
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
	}
	
	public static File getFile(File currentFile,short fileID){
		if(currentFile.FID == fileID)
			return currentFile;
		
		File child = currentFile.child;
		
		while(child!=null){
			if(child.FID == fileID){
				break;
			}
			 
			child = child.sibling;
		}
		
		return child;
	}
		
	
}
