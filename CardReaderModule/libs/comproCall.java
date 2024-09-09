/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2012</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

package lc;

public class comproCall
{
    static
    {
       System.loadLibrary("comPro");
    }

    /*  Common Functions */
	public native static int lc_init_ex(int portType, char[] fpath, int baud);
	public native static int lc_exit(int icdev);
	public native static int asc_hex(byte[] strasc_in,byte[] strhex_out,int hex_length);
	public native static int hex_asc(byte[] strhex_in,byte[] strasc_out,int hex_length);
	public native static int lc_getver(int icdev,char[] version);
	public native static int lc_GetDevSN(int icdev, byte[] pbuf, int[] rlen);
	public native static int lc_devReboot(int icdev);
	public native static int lc_getFlashUserMemSize(int icdev, int[] pSize);
	public native static int lc_srd_flash(int icdev,int offset,int length, byte[] rec_buffer);
	public native static int lc_swr_flash(int icdev,int offset,int length,byte[] buffer);
	public native static int lc_setAddress(int icdev, int addr);
	public native static int lc_getAddress(int icdev, int[] pAddr);
	public native static int lc_setBaudRate(int icdev,int baud);
	public native static int lc_SetDevPassword(int icdev,byte pwdAddr, byte pwdLen, byte[] pPwd);
	public native static int lc_crypt(int icdev,byte fEn_De, byte algorithm, int srcLen, byte[] pSrc_in, byte[] pIv, byte[] pDest_out, int[] pDestLen_out);
	public native static int lc_getAutoReturnedData(int icdev, byte[] rData, int[] rlen); 
	public native static int lc_set_identifyResponsePara(int icdev, byte[] pParaIn, byte paraLen);
	public native static int lc_get_identifyResponsePara(int icdev, byte[] pParaOut, byte[] pParaLen);
	public native static int lc_getAutoReturnedData(int icdev, byte[] pRData, byte[] pRLen);
	public native static int lc_set_AutoReportDataType(int icdev, byte type,short outputPara);
	public native static int lc_get_AutoReportDataType(int icdev, byte[] type_out, short[] pOutputPara);
	public native static int lc_setReadOnlyPara(int icdev,byte[] pPara,int paraLen);
	public native static int lc_getReadOnlyPara(int icdev, byte[] pPara_out, int[] paraLen_out);
	public native static int lc_setAppendData(int icdev, byte[] para_in, byte paraLen);
	public native static int lc_getAppendData(int icdev, byte[] pPara_out, byte[] pLen_out);
	public native static int lc_getReaderMode(int icdev, byte[] mode);
	//mode: 1->Read/Write, 2->Read only(Dec), 3->Read only(Hex), 4->Read only(with header)
	public native static int lc_setReaderMode(int icdev, byte mode);
	
	/* Simple Device Functions */
	public native static int lc_beep(int icdev,int _Msec);
	public native static int lc_led(int icdev, int iLED, int on_off);
	
	/* RFID Functions */
    public native static int lc_setANT(int icdev, byte status);
    public native static int lc_selectANT(int icdev, int iANT);
    public native static int lc_rfReset(int icdev, int time);
    public native static int lc_getRF_TXConductance(int icdev, byte protocol,  byte[] pRegVal);
    public native static int lc_setRF_TXConductance(int icdev, byte protocol, byte regVal);
    // getMode: 0-Once every time each card,1-Can read many times one card 
    public native static int lc_getPBOC_PAN(int icdev, byte slot, byte getMode, char[] pPAN);
    public native static int lc_readSSC(int icdev, byte slot, char[] pSSC_Info);
	public native static int lc_readPBOC(int icdev, byte slot, char[] p_Info);
	public native static int lc_card(int icdev,byte _Mode, byte[] outSNBuf, byte[] outSNBufferSize, int[] pOutTag,byte[] pOutSak);
	public native static int lc_requestAndIdentifyTypeA(int icdev,byte _Mode,byte[] outSNBuf, byte[] outSNBufferSize, 
			byte[] pfTagTypeOut, byte[] pfCompliant14443_4_out);
	public native static int lc_authentication(int icdev,byte _Mode,byte blk, byte[] pKey);
	public native static int lc_read(int icdev,byte blk,byte[] _Data);
	public native static int lc_read_convenient(int icdev, byte blkAddr, byte keyMode, byte[] pKey, byte[] pData_out);
	public native static int lc_write(int icdev,byte blk,byte[] _Data);
	public native static int lc_write_convenient(int icdev, byte blkAddr, byte keyMode,byte[] pKey, byte[] pData_in);
	public native static int lc_halt(int icdev);
	public native static int lc_updateKey(int icdev, byte sector,byte[] pNewKeyA, byte[] pNewCtrlW, byte[] pNewKeyB);
	public native static int lc_initval(int icdev,byte _Adr,int _Value);
	public native static int lc_increment(int icdev,byte _Adr,int _Value);
	public native static int lc_decrement(int icdev,byte _Adr,int _Value);
	public native static int lc_readval(int icdev,byte _Adr,int[] pValue);
	public native static int lc_read_NTag(int icdev,byte addr,byte[] Data_out);
	public native static int lc_write_NTag(int icdev,byte addr,byte[] Data_in);
	public native static int lc_tag_authenPwd(int icdev, byte[] pKey_in);
	public native static int lc_tag_setPwd(
			int icdev, 
			byte tagType,
			byte[] pPwd_in,
			byte  protectType,
			byte protectStartAddr,
			byte maxErrorVerify,
			boolean	lockConfigAfterSet);
	public native static int lc_rawExchange(int icdev,byte slen,byte[] sbuff,byte[] rrlen,byte[] rbuff);
	//Utrlight-C
	public native static int lc_ultralt_C_authen(int icdev, byte[] key);
	public native static int lc_ultralt_C_setSafePage(int icdev, int ipage, boolean readSec);
	public native static int lc_ultralt_C_changePwd(int icdev, byte[] keyold,byte[] keynew);
	public native static int lc_ultralt_C_lockPage(int icdev, int flag);
    //FCPU Card
    public native static int lc_pro_reset(int IDevice,char[] rlen,byte[] rbuf);
    public native static int lc_pro_commandlink(int IDevice,short slen,byte[] sbuff,char[] rlen,byte[] rbuf,short tt,short fg);

    //NDEF Interface
    public native static int lc_NFC_FormatCard_WithURI(int icdev, int cardType, int URItype, char[] szURI, char[] szPwd, boolean fLock);
    public native static int lc_NFC_FormatCard_WithText(int icdev, int cardType,char[] szIANA, char[] szText,int textFormat,char[] szPwd, boolean fLock);
    public native static int lc_NFC_FormatCard_WithSmartPoster(int icdev, int cardType,char[] szIANA, char[] szText, int textFormat, int URItype, 
    											   char[] szURI,char[] szPwd,boolean fLock);
    public native static int lc_NFC_FormatCard_WithContact(int icdev, int cardType,
    											   char[] szName, 
    											   char[] szTelCell,
    											   char[] szTelWork,
    											   char[] szTelFax,
    											   char[] szTelHome,
    											   char[] szOrg,
    											   char[] szDepart,
    											   char[] szWorkAdr,
    											   char[] szEmail,
    											   char[] szURL,
    											   char[] szXQQ,
    											   char[] szWeibo,
    											   char[] szWeixin,
    											   int textFormat,
    											   char[] szPwd,
    											   boolean fLock);
    public native static int lc_NFC_ReadTag(int icdev, int tagType,int textFormat, char[] szPwd, int[] pPayloadType, char[] pContent_out);
    /*
    public static class Ndef_record{
    	byte 	tnf_type;
    	byte 	typeLength;
    	char[]	szType;
    	byte	idLength;
    	byte[]	bufIdentifier;
    	int		payLoadLen;
    	int		payloadMemorySize;
    	byte[]	bufPayLoad;
    	byte	nestRecNum;
    	Pointer pNestRecs;
    	Pointer pPreRec;
    	Pointer pNextRec;
    }
    public static class Ndef_message {
    	int arrNum;
    	int mmRecsNum;
    	Ndef_record[] pRec;
    }
    public native static int lc_NFC_FormatTag_Ex(int icdev, int tagType, Ndef_message[] pInRec,char[] szPwd, boolean fLock);
    public native static int lc_NFC_ReadTag_Ex(int icdev, int tagType, int textFormat, Ndef_message[] pOutRec);
    */

    //Desfire Interfaces
    public native static int lc_authen_desfire(int icdev, byte keyNo, byte[] key,byte[] sessionKey);
    public native static int lc_authen_desfire_aes(int icdev, byte keyNo, byte[] key,byte[] sessionKey);
    public native static int lc_getver_desfire(int icdev,byte[] rlen,byte[] version);
    public native static int lc_getAIDs_desfire(int icdev,byte[] rlen,byte[] AIDS);
    public native static int lc_selectApp_desfire(int icdev, byte[] AID);
    public native static int lc_getKeySetting_desfire(int icdev, byte[] rlen,byte[] setbuf);
    public native static int lc_getKeyver_desfire(int icdev,byte keyNo,byte[] keyVer);
    public native static int lc_createApp_desfire(int icdev,byte[] AID,byte KeySetting,byte NumOfKey);
    public native static int lc_delAID_desfire(int icdev,byte[] AID);
    public native static int lc_changeKeySetting_desfire(int icdev,byte newSet,byte[] sessionKey);
    public native static int lc_changeKey_desfire(int icdev,byte[] sessionKey,byte[] curKey,byte keyNo,byte[] newkey);
    public native static int lc_changeKey_desfire_aes(int icdev,byte[] sessionKey,byte[] curKey,byte keyNo,byte[] newkey,byte keyVersion);
    public native static int lc_getFileIDs_desfire(int icdev, byte[] rlen, byte[] fileIDs);
    public native static int lc_getFileProper(int icdev,byte fileNo,byte[] rlen,byte[] fileProper);
    public native static int lc_changeFileSetting(int icdev,byte fileNo,byte comSet,byte[] accessRight,byte[] sessionKey);
    public native static int lc_createDataFile_desfire(int icdev,byte fileNo,byte ComSet,byte[] AccessRight,byte[] FileSize);
    public native static int lc_createBackupDataFile_desfire(int icdev,byte fileNo,byte ComSet,byte[] AccessRight,byte[] FileSize);
    public native static int lc_createValueFile_desfire(int icdev,byte fileNo,byte ComSet,byte[] AccessRight,byte[] lowerLimit,byte[] upperLimit,byte[] value,byte creditEnabled);
    public native static int lc_createCsyRecord_desfire(int icdev,byte fileNo,byte comSet,byte[] AccessRight,byte[] RecordSize,byte[] MaxNum);
    public native static int lc_delFile_desfire(int icdev,byte fileNo);
    public native static int lc_write_desfire(int icdev,byte fileNo,int offset,int length,byte[] data,byte[] sessionKey);
    public native static int lc_read_desfire(int icdev,byte fileNo,int offset,int length,byte[] revData,byte[] sessionKey);
    public native static int lc_getvalue_desfire(int icdev,byte fileNo,int[] value,byte[] sessionKey);
    public native static int lc_credit_desfire(int icdev,byte fileNo,int value,byte[] sessionKey);
    public native static int lc_debit_desfire(int icdev,byte fileNo,int value,byte[] sessionKey);
    public native static int lc_writeRecord_desfire(int icdev,byte fileNo,int offset,int length,byte[] data,byte[] sessionKey);
    public native static int lc_readRecord_desfire(int icdev,byte fileNo,int offset,int length,byte[] revData,int[] SgRecordlen,int[] rlen,byte[] sessionKey);
    public native static int lc_clearRecord_desfire(int icdev,byte fileNo);
    public native static int lc_commitTransfer_desfire(int icdev);
    public native static int lc_abortTransfer_desfire(int icdev);
    public native static int lc_formatPICC_desfire(int icdev);
    
    //Mifare plus Interfaces
    public native static int lc_MFPlusL0_WritePerso(int icdev, byte[] key, short keyAddr);
    public native static int lc_MFPlusL0_CommitPerso(int icdev);
    public native static int lc_MFPlusL1_AuthenKeyL1(int icdev,byte[] key);
    public native static int lc_MFPlusL1_SwitchToL2(int icdev,byte[] key);
    public native static int lc_MFPlusL1_SwitchToL3(int icdev,byte[] key);
    public native static int lc_MFPlusL2_SwitchToL3(int icdev,byte[] key);
    public native static int lc_MFPlusL3_AuthenL3Key(int icdev,byte[] key, short keyAddr);
    public native static int lc_MFPlusL3_AuthenSectorKey(int icdev,char keyType,int sectorNo,byte[] key);
    public native static int lc_MFPlusL3_UpdateKey(int icdev, int keyAddr,byte[] newKey);
    public native static int lc_MFPlusL3_ReadWithEncrypt(int icdev, int blkAddr,byte blkNum,byte[] rdata,byte flag);
    public native static int lc_MFPlusL3_WriteWithEncrypt(int icdev, int blkAddr,byte blkNum,byte[] wdata,byte flag);
    public native static int lc_MFPlusL3_ReadWithPlain(int icdev, int blkAddr,byte blkNum, byte[] rdata);
    public native static int lc_MFPlusL3_WriteWithPlain(int icdev, int blkAddr,byte blkNum, byte[] wdata);
    public native static int lc_MFPlusL3_InitVal(int icdev, int blkAddr,long value);
    public native static int lc_MFPlusL3_ReadVal(int icdev, int blkAddr,long[] value);
    public native static int lc_MFPlusL3_Increment(int icdev, int blkAddr,long value);
    public native static int lc_MFPlusL3_Decrement(int icdev, int blkAddr,long value);
    
	//----------------------------- Typeb --------------
    public native static int lc_findTypeB(int icdev,byte fSelectCard, byte[] response_out, byte[] resLen_out);
    public native static int lc_typeB_command
    (
     int icdev,//Handle of device
     int slen,//The length of data to send
     byte[] sbuff,//Data to send
     byte[] rlen,//The length of data received
     byte[] rbuff//The data received
     );
    //----------------------------- ISO15693 --------------
    /* reqMode: The mode when finding card, 0x36-Single card mode, 0x16-Multiple card mode*/
    public native static int lc_find15693(
    	int icdev,
    	byte reqMode, 
    	byte[] response_out, 
    	byte[] resLen_out);
    public native static int lc_15693_command
    (
     int icdev,//Handle of device
     byte cmd, //The command to send
     byte[] pUID, //The id of card (8 bytes)
     int sParaLen,//The length of paramter 
     byte[] psPara,//Paramter 
     int[] rParaLen,//The length of paramter received
     byte[] rPara//The parater received
     );
    public native static int lc_15693_command_custom
    (
     int icdev,//Handle of device
     byte cmd, //The command to send
     byte MfgCode, //IC Mfg code
     byte[] pUID, //The id of card (8 bytes)
     int sParaLen,//The length of paramter 
     byte[] psPara,//Paramter 
     int[] rParaLen,//The length of paramter received
     byte[] rPara//The parater received
     );
    public native static int lc_15693_stay_quiet(
    	int icdev,
    	byte[] pUID);
    public native static int lc_15693_select_uid(
    	int icdev,
    	byte[] pUID);
    public native static int lc_15693_reset_to_ready(
    	int icdev,
    	byte[] pUID);
    public native static int lc_15693_readBlock(
    	int icdev,
    	byte[] pUID,
    	byte startBlock,
    	byte blockNum,
    	byte[] rlen,
    	byte[] rbuffer);
    public native static int lc_15693_writeBlock(
    	int icdev,
    	byte[] pUID,
    	byte startBlock,
    	byte blockNum, 
    	byte wlen,
    	byte[] wbuffer);
    public native static int lc_15693_lock_block(
    	int icdev,
    	byte[] pUID,
    	byte block);
    public native static int lc_15693_write_afi(
    	int icdev,
    	byte[] pUID,
    	byte AFI);
    public native static int lc_15693_lock_afi(
    	int icdev,
    	byte[] pUID);
    public native static int lc_15693_write_dsfid(
    	int icdev,
    	byte[] pUID,
    	byte DSFID);
    public native static int lc_15693_lock_dsfid(
    	int icdev,
    	byte[] pUID);
    public native static int lc_15693_get_systemInfo(
    	int icdev,
    	byte[] pUID,
    	byte[] rlen,
    	byte[] rbuffer);
    public native static int lc_15693_get_securityInfo(
    	int icdev,
    	byte[] pUID,
    	byte startBlock,
    	byte blockNum,
    	byte[] rlen,
    	byte[] rbuffer);
    /* pwdType:01h-Read, 02h-Write, 04h-Privacy,08h-Destroy SLI-L, 10h-EAS */
    public native static int lc_15693set_password(
    	int icdev,
    	byte[] pUID, 
    	byte pwdType, 
    	byte[] pPwd);
    /* pwdType:01h-Read, 02h-Write,04h-Privacy,08h-Destroy SLI-L, 10h-EAS , and privacy is default used*/
    public native static int lc_15693_write_password(
    	int icdev,
    	byte[] pUID, 
    	byte pwdType, 
    	byte[] pPwd);
    public native static int lc_15693_icode_64bitProtect(int icdev,byte[] pUID);
    /*protectStatus:00h-Public,01h-Read and write protect by read-pass, 10h-Write protect by wirte pass. 
    	11h-Read protect by the read pass, and Write protect by write pass*/
    public native static int lc_15693_protect_page(
    	int icdev,
    	byte[] pUID, 
    	byte iPage, 
    	byte protectStatus);
    
	//----------------------------- TypeF --------------
    public native static int lc_findTypeF(int icdev,short sysCode, byte[] IDm_out, byte[] PMm_out);
    public native static int lc_typeF_command
    (
     int icdev,//Handle of device
     int slen,//The length of data to send
     byte[] sbuff,//Data to send
     byte[] rlen,//The length of data received
     byte[] rbuff//The data received
     );
    //----------------------------- ICC --------------
    public native static int  lc_iccGetCardState(int icdev, short slot, byte[] pSt);
	public native static int  lc_iccSelCardType
		(
				int icdev,//Handle of device
				short slot,
				int cardType
		);
	public native static int  lc_icc_SlotPower(int ICDev, short slot,int fIntent);
    public native static int  lc_iccGetATR(int icdev,short slot,byte[] response_out, byte[] resLen_out);
    public native static int  lc_icc_APDU
    (
     int ICDev,//Handle of device
     short slot,
     int slen,//The length of data to send
     byte[] sbuff,//Data to send
     int[] rlen,//The length of data received
     byte[] rbuff//The data received
     );
	public native static int  lc_iccSetBaud(int icdev, short slot, int baud);
	public native static int  lc_iccPPS(int icdev, short slot, byte[] pPara);
	public native static int  lc_icc_ReadMem
		(
				int icdev,//Handle of device
				short slot,
				int  address,
				int  rLen,
				byte[] rbuff//The data received
		);
	public native static int  lc_icc_WriteMem
		(
				int icdev,//Handle of device
				short slot,
				int  address,
				int  wLen,
				byte[] wbuff
		);
	public native static int  lc_icc_VerifyUserPass
		(
				int icdev,//Handle of device
				short slot,
				int  passLen,
				byte[] pPassIn
		);
	public native static int  lc_icc_ReadErrorCounter(
		int icdev,
		short slot,
		byte[] pCnt);

	public native static int  lc_icc_UpdateUserPass
		(
				int icdev,//Handle of device
				short slot,
				int  passLen,
				byte[] pPassIn
		);
    /*  NB Functions */
    public native static int lc_setNB_serverIP(int icdev, byte[] pIP_in);
    public native static int lc_getNB_serverIP(int icdev, byte[] pIP_out);
    public native static int lc_setNB_serverPort(int icdev, int port);
    public native static int lc_getNB_serverPort(int icdev, int[] pPort);
    public native static int lc_setNB_URL(int icdev, byte[] pURL_in);
    public native static int lc_getNB_URL(int icdev, byte[] pURL_out);
    
    /* UHF */
    public native static int lc_uhf_inventory(int icdev,short scanTime,byte[] response_out, byte[] resLen_out);
    public native static int lc_uhf_readTag(int icdev,byte bTagBank,int addrWord,int lenWord,byte[] pPwd, int[] rlen,byte[] rbuff);
    public native static int lc_uhf_writeTag(int icdev,byte bTagBank,int addrWord,int lenWord,byte[] pPwd, byte[] wbuff);
    public native static int lc_uhf_lockTag(int icdev, byte bTagBank,byte bLockType,byte[] pPwd);
    public native static int lc_uhf_killTag(int icdev,byte[] pPwd);
    


}
