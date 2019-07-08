package com.ujjwal.src ;
import javacardx.apdu.ExtendedLength;

import javacard.framework.*;
import javacardx.framework.tlv.*;

public class Basic extends Applet implements AppletEvent,ExtendedLength
{
	File root 		= null;	
	File currentDF  = null;
	File currentEF  = null;

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Basic().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu)
	{
		//SelectApplet will select uniquely based on AID name
		if (selectingApplet())
		{
			//Resetting the Current file reference
			if(root != null)
				currentDF = root;
			return;
		}

		byte[] buf = apdu.getBuffer();
		switch (buf[ISO7816.OFFSET_INS])
		{
		case Constant.INS_CREATE_FILE:
			CreateFile(buf);
			break;
		case Constant.INS_SELECT_FILE:
			SelectFile(apdu,buf);
			break;
		case Constant.INS_UPDATE_BINARY:
			UpdateBinary(apdu,buf);
			break;
		case Constant.INS_READ_BINARY:
			ReadBinary(apdu,buf);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	private boolean isDedicateFile(byte lcsi,byte fdb,short fileID){
		
		if((byte)(lcsi&0x0F) != Constant.ACTIVATED)
			return false;
			
		if((byte)(fdb&0xFF) != Constant.DF_FDB)
			return false;
		
		if((short)(fileID&0xFF00) != 0xDF00){
			if((short)(fileID&0xFF00) != Constant.MF_FID && ((fileID&0xFF00) != 0xDF00))
				return false;
		} 
		
		return true;
	}
	
	private boolean isTransparent(byte lcsi,byte fdb,short fileID){
		
		if((byte)(lcsi&0x0F) != Constant.ACTIVATED)
			return false;
			
		if((byte)(fdb&0xFF) != Constant.TRANSPARENT_FDB)
			return false;
		
		if((short)(fileID&0xFF00) != (short)0xEF00 )
			return false;
		
		return true;
	}
	
	private void CreateDF(byte[] apduBuffer,short tag83_fileID,short tag80_dataLength,byte tag8A_LCSI,byte tag82_FDB){
		File tempFile = null;
		
		//Check if MF is already created
		if(root !=null & tag83_fileID == Constant.MF_FID){
			ISOException.throwIt(Constant.SW_FILE_ALREADY_EXIT);
		}
		
		//checking whether the FID exit or not
		if(root != null){
			if(File.getFile(currentDF,tag83_fileID) != null)
				ISOException.throwIt(Constant.SW_FILE_ALREADY_EXIT);
		}
		
		File newFile = new File(apduBuffer,ISO7816.OFFSET_CDATA,apduBuffer[ISO7816.OFFSET_LC],tag80_dataLength);
		newFile.FID  = tag83_fileID;
		newFile.LCSI = tag8A_LCSI;	
		newFile.FDB  = tag82_FDB;
		
		//If its the first file in heirarchy
		if(root == null){
			root 				= newFile;
			root.parent 		= null;
			root.sibling 		= null;
			root.child 			= null;
			root.FID 			= tag83_fileID;
			root.LCSI         	= tag8A_LCSI;
			root.FDB			= tag82_FDB;
			currentDF 			= root;
		}
		else{
			
			//Setting parent of the DF
			newFile.parent = currentDF;
			
			//Setting child of current file			
			//If a child is not present
			if(currentDF.child == null){
				currentDF.child = newFile;
			}				
			else{
				//Setting sibling
				tempFile = currentDF.child;
				while(tempFile.sibling !=null){
					tempFile = tempFile.sibling;
				}
				tempFile.sibling = newFile;
			}
			//Setting new file as current file
			currentDF = newFile;
			
		}
	}
	
	private void CreateEF(byte[] apduBuffer,short tag83_fileID,short tag80_dataLength,byte tag8A_LCSI,byte tag82_FDB){
		File tempFile = null;
			
		//Don't create Transparent file if MF don't exit
		if(root == null){
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}				
		
		//checking whether the FID exit or not
		if(File.getFile(currentDF,tag83_fileID) != null){
			ISOException.throwIt(Constant.SW_FILE_ALREADY_EXIT);
		}
		
		File newFile = new File(apduBuffer,ISO7816.OFFSET_CDATA,apduBuffer[ISO7816.OFFSET_LC],tag80_dataLength);
		newFile.FID  = tag83_fileID;
		newFile.LCSI = tag8A_LCSI;
		newFile.FDB	 = tag82_FDB;
		
		//Setting parent of the EF
		newFile.parent = currentDF;
		//Setting child of current file
		if(currentDF.child == null){
			currentDF.child = newFile;
		}			
		else{
			//Setting sibling
			tempFile = currentDF.child;
			while(tempFile.sibling !=null){
				tempFile = tempFile.sibling;
			}
			tempFile.sibling = newFile;
		}
	}
		
	private void CreateFile(byte[] apduBuffer){
		//Get Header Info
		final byte CLA 			= apduBuffer[ISO7816.OFFSET_CLA];
		final byte P1 			= apduBuffer[ISO7816.OFFSET_P1 ];
		final byte P2 			= apduBuffer[ISO7816.OFFSET_P2 ];
		final byte Lc 			= apduBuffer[ISO7816.OFFSET_LC];
		short tag80_dataLength 	= Constant.SHORT_ZERO;
		short tag83_fileID		= Constant.SHORT_ZERO;
		byte tag82_FDB			= Constant.BYTE_ZERO;
		byte tag8A_LCSI			= Constant.BYTE_ZERO;
		
		//Checking P1 and P2
		if(P1 != Constant.BYTE_ZERO && P2!= Constant.BYTE_ZERO){
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2 );			
		}
		
		//Check Data is BER encoded		
		boolean isBEREncoded = 	 BERTLV.verifyFormat(apduBuffer,ISO7816.OFFSET_CDATA,apduBuffer[ISO7816.OFFSET_CDATA]);
		// if(isBEREncoded != true){
			// ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		// }
		
		//Checking data is FCP formated 
		byte tag 			= apduBuffer[ISO7816.OFFSET_CDATA];
		short tagOffset 	= ISO7816.OFFSET_CDATA;	
		byte tagLen			= apduBuffer[tagOffset+1];
		
		if(tag != Constant.FCP_TEMP || tagLen >(apduBuffer[ISO7816.OFFSET_CDATA]-2)){
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		
		//Checking Mandatory tags
		short maxOffset = (short)(ISO7816.OFFSET_CDATA + apduBuffer[ISO7816.OFFSET_LC]);
		tagOffset       = ISO7816.OFFSET_CDATA+2;
		tag 			= apduBuffer[tagOffset];		
		tagLen			= apduBuffer[tagOffset+1];
		short tagValue  = Constant.SHORT_ZERO;
		while(tagOffset < maxOffset){
			switch(tag){
			case Constant.TAG_80:
				if(tagLen < (short)0x0002){
					ISOException.throwIt(ISO7816.SW_WRONG_DATA);
				}
				tagValue = Util.makeShort(apduBuffer[tagOffset+2],apduBuffer[tagOffset+3]);
				
				if(tagValue > Constant.MAX_BUFF_SIZE){
					ISOException.throwIt(ISO7816.SW_FILE_FULL);
				}
				
				//setting data length
				tag80_dataLength = tagValue;
				
				//Moving to next tag
				tagOffset 	= (short)(tagOffset+tagLen+2);
				tag 		= apduBuffer[tagOffset];
				tagLen 		= apduBuffer[tagOffset+1];				
				break;
			case Constant.TAG_82:
				tagValue = apduBuffer[tagOffset+2];
				if(tagLen > (short)0x01 ){
					ISOException.throwIt(ISO7816.SW_WRONG_DATA);
				}				
				
				tag82_FDB = (byte)tagValue;
				
				//Moving to next tag
				tagOffset 	= (short)(tagOffset+tagLen+2);
				tag 		= apduBuffer[tagOffset];
				tagLen 		= apduBuffer[tagOffset+1];				
				break;
			case Constant.TAG_83:
				tagValue = Util.makeShort(apduBuffer[tagOffset+2],apduBuffer[tagOffset+3]);				
				
				//setting data length
				tag83_fileID	= tagValue;
				
				//Moving to next tag
				tagOffset 		= (short)(tagOffset+tagLen+2);
				tag 			= apduBuffer[tagOffset];
				tagLen 			= apduBuffer[tagOffset+1];				
				break;
			case Constant.TAG_8A:
				tagValue = apduBuffer[tagOffset+2];
				if(tagLen > (short)0x01 || (byte)(tagValue&0xFF) != Constant.ACTIVATED){
					ISOException.throwIt(ISO7816.SW_WRONG_DATA);
				}
				
				tag8A_LCSI = (byte)tagValue;
				
				//Moving to next tag
				tagOffset 	= (short)(tagOffset+tagLen+2);
				tag 		= apduBuffer[tagOffset];
				tagLen 		= apduBuffer[tagOffset+1];				
				break;
			default:				ISOException.throwIt(ISO7816.SW_WRONG_DATA);
	
			
			}
		}
		
		//Check for DF/MF
		if(isDedicateFile(tag8A_LCSI,tag82_FDB,tag83_fileID)){
			CreateDF(apduBuffer,tag83_fileID,tag80_dataLength,tag8A_LCSI,tag82_FDB);			
		}
		//Check for EF
		else if(isTransparent(tag8A_LCSI,tag82_FDB,tag83_fileID)){
			CreateEF(apduBuffer,tag83_fileID,tag80_dataLength,tag8A_LCSI,tag82_FDB);						
		}
		//Throw error in other case
		else{
			ISOException.throwIt(ISO7816.SW_WRONG_DATA);
		}		
											
	}
	
	private void SelectParent(){		
		currentDF = currentDF.parent;
	}
	
	private void SelectChildEF(byte[] apduBuffer,short fileID){
		
		File tempFile = File.getFile(currentDF,fileID);
		
		if(tempFile == null){
			ISOException.throwIt(Constant.SW_FILE_ALREADY_EXIT);
		}
		
		if(tempFile.FDB != Constant.TRANSPARENT_FDB){
			ISOException.throwIt(ISO7816.SW_FILE_INVALID);
		}
		
		currentEF = tempFile;
	}
	
	private void SelectChildDF(byte[] apduBuffer,short fileID){
		
		File tempFile = File.getFile(currentDF,fileID);
		
		if(tempFile == null){
			ISOException.throwIt(Constant.SW_FILE_ALREADY_EXIT);
		}
		
		if(tempFile.FDB != Constant.DF_FDB){
			ISOException.throwIt(ISO7816.SW_FILE_INVALID);
		}
		
		currentDF = tempFile;
	}
	
	private void SelectByFID(byte[] apduBuffer,short fileID ){
		//Check whether file ID is 3F00(MF)
		if(fileID == Constant.MF_FID || apduBuffer[ISO7816.OFFSET_LC] == Constant.BYTE_ZERO ){
			//Check MF is created or not
			if(currentDF !=null){
				currentDF = root;
			}
			else{
				ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
			}
		}
		else{
			if(currentDF.FID == fileID){
				currentDF = currentDF;
			}
			File fileTemp = File.getFile(currentDF,fileID);
			
			if(fileTemp == null){
				ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
			}
			
			if(fileTemp.FDB == Constant.DF_FDB)
				currentDF = fileTemp;
			else
				currentEF = fileTemp;
		}
	}
		
	private void SelectFile(APDU apdu,byte[] apduBuffer){
		//Get Header Info
		final byte CLA 			= apduBuffer[ISO7816.OFFSET_CLA];
		final byte P1 			= apduBuffer[ISO7816.OFFSET_P1 ];
		final byte P2 			= apduBuffer[ISO7816.OFFSET_P2 ];
		final byte Lc 			= apduBuffer[ISO7816.OFFSET_LC];
		short fileID			= Constant.SHORT_ZERO;
		boolean FCPRequested  	= false; 
		
		//Check whether FCP is requested or not
		if(P2 == Constant.FCP_REQ){
			FCPRequested = true;
		}
		
		fileID = Lc<0x02? Constant.SHORT_ZERO:Util.makeShort(apduBuffer[ISO7816.OFFSET_CDATA],apduBuffer[ISO7816.OFFSET_CDATA+1]);
		
		switch(P1){
		case Constant.SELECT:
			if(Lc > 0x02){
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			}
			SelectByFID(apduBuffer,fileID);
			break;
		case Constant.SELECT_CHILD_DF:
			if(Lc > 0x02){
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			}
			SelectChildDF(apduBuffer,fileID);
			break;
		case Constant.SELECT_EF_UNDR_CURR_DF:
			if(Lc > 0x02){
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			}
			SelectChildEF(apduBuffer,fileID);
			break;
		case Constant.SELECT_PARENT:
			if(Lc != Constant.BYTE_ZERO){
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			}
			SelectParent();
			break;
		default:			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);			
		}
		
		//Prepare FCP response
		if(FCPRequested){
			Util.arrayCopy(currentDF.FCP,Constant.SHORT_ZERO,apduBuffer,Constant.SHORT_ZERO,currentDF.FCPLen);
			apdu.setOutgoingAndSend(Constant.SHORT_ZERO,currentDF.FCPLen);
		}
	}
	
	public void UpdateBinary(APDU apdu,byte[] apduBuffer){
		//Get Header Info
		final byte CLA 			= apduBuffer[ISO7816.OFFSET_CLA];
		final byte P1 			= apduBuffer[ISO7816.OFFSET_P1 ];
		final byte P2 			= apduBuffer[ISO7816.OFFSET_P2 ];
		final byte Lc 			= apduBuffer[ISO7816.OFFSET_LC];
		short bytesLeft			= Constant.SHORT_ZERO;
		short fileOffset		= Constant.SHORT_ZERO;
		
		//Checking actual data length
		if(Lc == (byte)0x00){	//Length is extended
			bytesLeft = Util.makeShort(apduBuffer[ISO7816.OFFSET_LC+1],apduBuffer[ISO7816.OFFSET_LC+1]);
		}
		else{					//Length is not extendede
			bytesLeft = Util.makeShort(Constant.BYTE_ZERO,apduBuffer[ISO7816.OFFSET_LC]);
		}
				
		File fileToBeUpdated 	= null;
		
		if(P1 == Constant.BYTE_ZERO && P2 == Constant.BYTE_ZERO ){
			fileToBeUpdated = currentEF;
		}
		
		short readCount = Constant.SHORT_ZERO;
		readCount = apdu.setIncomingAndReceive();
		byte[] buff = apdu.getBuffer();
		Util.arrayCopy(buff,(short)ISO7816.OFFSET_EXT_CDATA,fileToBeUpdated.buffer1,fileOffset,(short)readCount);
		bytesLeft -= readCount;		
		while ( bytesLeft > 0){		
			fileOffset += readCount;	
			readCount = apdu.receiveBytes ( ISO7816.OFFSET_EXT_CDATA ); // read more APDU data
			bytesLeft -= readCount;
			buff = apdu.getBuffer();
			//Writing data to the transparent file
			Util.arrayCopy(buff,(short)ISO7816.OFFSET_EXT_CDATA,fileToBeUpdated.buffer1,fileOffset,(short)readCount);
			
		}
		
	}
	
	public void ReadBinary(APDU apdu,byte[] apduBuffer){
		//Get Header Info
		final byte CLA 			= apduBuffer[ISO7816.OFFSET_CLA];
		final byte P1 			= apduBuffer[ISO7816.OFFSET_P1 ];
		final byte P2 			= apduBuffer[ISO7816.OFFSET_P2 ];
		short  dataLen			= Constant.SHORT_ZERO;
		short Le 				= apduBuffer[ISO7816.OFFSET_LC];
		
		//Checking Le Length, whether it is extended or not
		if(apduBuffer[ISO7816.OFFSET_LC] == Constant.BYTE_ZERO ){
				Le = Util.makeShort(apduBuffer[ISO7816.OFFSET_LC+1],apduBuffer[ISO7816.OFFSET_LC+2]);
		}
		
		File fileToBeRead 		= null;
		
		if(P1 == Constant.BYTE_ZERO && P2 == Constant.BYTE_ZERO ){
			fileToBeRead = currentEF;
			dataLen = fileToBeRead.dataLength;
		}
		
		//checking data length against maximum limit
		if(dataLen > (short)0x800){
			dataLen = (short)0x800;
		}
		
		apdu.setOutgoing();
		apdu.setOutgoingLength(dataLen);
		apdu.sendBytesLong(fileToBeRead.buffer1,Constant.SHORT_ZERO,dataLen);
		
		
		//Throwning warning incase Le does not match actual length
		if(Le > dataLen){
			ISOException.throwIt(Constant.SW_RECORD_REACH_BEFORE_Ne_BYTES);
		}
	}
	 
	public void uninstall(){
		root = null;
		currentDF  = null;
		currentEF  = null;
		JCSystem.requestObjectDeletion();
	}
	
}


