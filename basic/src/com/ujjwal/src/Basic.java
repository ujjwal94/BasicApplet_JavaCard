package com.ujjwal.src ;

import javacard.framework.*;
import javacardx.framework.tlv.*;

public class Basic extends Applet
{
		

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Basic().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			return;
		}

		byte[] buf 		= apdu.getBuffer();
		final byte INS 	= buf[ISO7816.OFFSET_INS];
		switch (INS)
		{
		case Constant.CREATE_FILE:
			CreateFile(buf);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	
	private void CreateFile(byte[] apduBuffer){
		//Get Header Info
		final byte CLA 	= apduBuffer[ISO7816.OFFSET_CLA];
		final byte P1 	= apduBuffer[ISO7816.OFFSET_P1 ];
		final byte P2 	= apduBuffer[ISO7816.OFFSET_P2 ];
		final byte Lc 	= apduBuffer[ISO7816.OFFSET_LC];
		
		//Checking P1 and P2
		if(P1 != Constant.BYTE_ZERO && P2!= Constant.BYTE_ZERO){
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2 );			
		}
		
		//Check whether FCP is BER-Encoded or not
		boolean isBEREncoded = BERTLV.verifyFormat(apduBuffer,ISO7816.OFFSET_CDATA,apduBuffer[ISO7816.OFFSET_LC]);
		if(isBEREncoded != true){
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		
		//Check Mandatory tags
		byte tag 		= apduBuffer[(short)(ISO7816.OFFSET_CDATA+2)];
		short tagOffset = (short)(ISO7816.OFFSET_CDATA+2);
		short taglen 	= apduBuffer[(short)(ISO7816.OFFSET_CDATA+3)];
		while(tagOffset < (short)(ISO7816.OFFSET_LC+Lc) )
			switch(tag){
			case Constant.TAG80:
				
				//Next Tag
				tag 		= apduBuffer[tagOffset+taglen+2];
				tagOffset 	= (short)(tagOffset+taglen+2);
				taglen		= apduBuffer[tagOffset+1];
				break;
			case Constant.TAG82:
				
				//Next Tag
				tag 		= apduBuffer[tagOffset+taglen+2];
				tagOffset 	= (short)(tagOffset+taglen+2);
				taglen		= apduBuffer[tagOffset+1];
				break;
			case Constant.TAG83:
				
				//Next Tag
				tag 		= apduBuffer[tagOffset+taglen+2];
				tagOffset 	= (short)(tagOffset+taglen+2);
				taglen		= apduBuffer[tagOffset+1];
				break;
			case Constant.TAG8A:
				
				//Next Tag
				tag 		= apduBuffer[tagOffset+taglen+2];
				tagOffset 	= (short)(tagOffset+taglen+2);
				taglen		= apduBuffer[tagOffset+1];
				break;	
			default:
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			}
	}
	
}


