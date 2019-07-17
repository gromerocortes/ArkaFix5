package com.alida.fix;

import java.util.Iterator;

import org.apache.camel.component.quickfixj.QuickfixjEngine;
import org.apache.commons.collections.MultiMap;


//import quickfix.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.CharField;
import quickfix.DoubleField;
import quickfix.IntField;
import quickfix.StringField;
import quickfix.UtcTimeStampField;
//import quickfix.fix50sp2.Message;
import quickfix.fixt11.Message;

/**
 * Agrega las cabeceras y campos necesarios al mensaje FIX dependiendo del tipo de mensaje en proceso
 * @author (Mejoras y ajustes) Gilberto Romero
 *
 */
public class CustomFixMessage {

	MultiMap valoresfix = null;
	int NoPartyIDs = 0;
	Iterator partyID_it = null;
	Iterator partyIdsource_it = null;
	Iterator partyRole_it = null;
	
    private static final Logger LOG = LoggerFactory.getLogger(QuickfixjEngine.class);
    private String	senderID;
	private String	targetID; 
	private quickfix.DataDictionary dd;

	/**
	 * Asigna al mensaje FIX el SenderID y TargetID correspondiente
	 * @param senderIDParam
	 * @param targetIDParam
	 */
	public CustomFixMessage (String	senderIDParam,String	targetIDParam) {	
		this.senderID = senderIDParam;
		this.targetID = targetIDParam;
	}	

	/**
	 * Agrega el mensaje FIX las cabeceras y campos necesarios para una New Order Single, tipo de mensaje "D" 
	 * @param valoresfix
	 * @return mensajeD
	 */
	public  Message getNewOrderSingle(MultiMap valoresfix){
		int NoPartyIDs = 0;
		Iterator partyID_it = null;
		Iterator partyIdsource_it = null;
		Iterator partyRole_it = null;
		//+++++++++++++++++++++ Creacion del mensaje FIX D ++++++++
		Message mensajeD = new Message();
		// BeginString
		mensajeD.getHeader().setField(new StringField(8, "FIXT.1.1"));
		// SenderCompID
		mensajeD.getHeader().setField(new StringField(49, senderID));
		// TargetCompID, with enumeration
		mensajeD.getHeader().setField(new StringField(56, targetID));
		// MsgType
		mensajeD.getHeader().setField(new CharField(35, 'D'));
		// Sending time
		mensajeD.getHeader().setField(new UtcTimeStampField(52));
		//ClOrdID
		if(valoresfix.containsKey("11")){
			mensajeD.setField(new StringField(11, ((java.util.Collection)valoresfix.get("11")).iterator().next().toString()));
		}
		// NoPartyIDs Para determinar cuantos grupos se extraeran campo: "453"
		if(valoresfix.containsKey("453")){
			NoPartyIDs = Integer.valueOf( ((java.util.Collection)valoresfix.get("453")).iterator().next().toString()).intValue();
		}
		 // Component Block - <Parties>.
	    if(valoresfix.containsKey("448") && valoresfix.containsKey("447") && valoresfix.containsKey("452")){
	    	partyID_it = ((java.util.Collection)valoresfix.get("448")).iterator();
	    	partyIdsource_it = ((java.util.Collection)valoresfix.get("447")).iterator();
	    	partyRole_it =  ((java.util.Collection)valoresfix.get("452")).iterator();
	    	/* Creacion con grupo generico Paties */ 
	    	quickfix.Group grupoParties = new quickfix.Group(453,448);
	    	//Primer grupo
	    	grupoParties.setField(new StringField(448,partyID_it.next().toString()));
	    	grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
	    	grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
	    	mensajeD.addGroup(grupoParties); 
	    	//Segundo grupo
	    	grupoParties.setField(new StringField(448,partyID_it.next().toString()));
	    	grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
	    	grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
	    	mensajeD.addGroup(grupoParties); 	
	    }
	    
		// HandlInst
		if(valoresfix.containsKey("21")){
			mensajeD.setField(new CharField(21, ((java.util.Collection)valoresfix.get("21")).iterator().next().toString().charAt(0)));
		}
		// Acceso
		if(valoresfix.containsKey("20001")){
			mensajeD.setField(new StringField(20001,((java.util.Collection)valoresfix.get("20001")).iterator().next().toString()));
		}
	    // OrdType
	    if(valoresfix.containsKey("40")) {
	    	mensajeD.setField(new CharField(40, ((java.util.Collection)valoresfix.get("40")).iterator().next().toString().charAt(0)));
	    }
    	// Side
	    if(valoresfix.containsKey("54")) {
	    	mensajeD.setField(new CharField(54, ((java.util.Collection)valoresfix.get("54")).iterator().next().toString().charAt(0)));
	    }
    	// Account Type
	    if(valoresfix.containsKey("581")) {
	    	mensajeD.setField(new IntField(581, Integer.valueOf( ((java.util.Collection)valoresfix.get("581")).iterator().next().toString()).intValue()));
	    }
    	// CoveredOrUncovered
	    if(valoresfix.containsKey("203")) {
	    	mensajeD.setField(new IntField(203,Integer.valueOf( ((java.util.Collection)valoresfix.get("203")).iterator().next().toString()).intValue()));
	    }
    	// SecurityIDSource
	    if(valoresfix.containsKey("22")){
	    	mensajeD.setField(new StringField(22,((java.util.Collection)valoresfix.get("22")).iterator().next().toString()));
	    }
    	// SecurityID
	    if(valoresfix.containsKey("48")){
	    	mensajeD.setField(new StringField(48,((java.util.Collection)valoresfix.get("48")).iterator().next().toString()));
	    }
    	// OrderQty
	    if(valoresfix.containsKey("38")){
	    	mensajeD.setField(new DoubleField  (38,Double.valueOf( ((java.util.Collection)valoresfix.get("38")).iterator().next().toString()).doubleValue()));
	    }
    	// Price
	    if(valoresfix.containsKey("44")){
	    	mensajeD.setField(new DoubleField(44,Double.valueOf( ((java.util.Collection)valoresfix.get("44")).iterator().next().toString()).doubleValue()));
	    }
    	// ExpireDate
	    if(valoresfix.containsKey("432")){
	    	mensajeD.setField(new StringField  (432,((java.util.Collection)valoresfix.get("432")).iterator().next().toString()));
	    }
    	// ExpireTime
	    if(valoresfix.containsKey("126"))  {
	    	mensajeD.setField(new StringField  (126,((java.util.Collection)valoresfix.get("126")).iterator().next().toString()));
	    }
    	// StopPx
	    if(valoresfix.containsKey("99")) {
	    	mensajeD.setField(new DoubleField(99,Double.valueOf( ((java.util.Collection)valoresfix.get("99")).iterator().next().toString()).doubleValue()));
	    }
	    // TimeInForce
	    if(valoresfix.containsKey("59")) {
	    	mensajeD.setField(new CharField(59,((java.util.Collection)valoresfix.get("59")).iterator().next().toString().charAt(0)));
	    }
	    // TransactTime
	    if(valoresfix.containsKey("60")){
	    	mensajeD.setField(new StringField(60, ((java.util.Collection)valoresfix.get("60")).iterator().next().toString().trim()));
	    }
	    // MaxFloor
	    if(valoresfix.containsKey("111")){
	    	mensajeD.setField(new DoubleField(111,Double.valueOf( ((java.util.Collection)valoresfix.get("111")).iterator().next().toString().trim()).doubleValue()));
	    }
    	// MinQty
	    if(valoresfix.containsKey("110")){
	    	mensajeD.setField(new DoubleField(110,Double.valueOf( ((java.util.Collection)valoresfix.get("110")).iterator().next().toString()).doubleValue()));
	    }
    	// PegOffsetValue
	    if(valoresfix.containsKey("211")){
	    	mensajeD.setField(new StringField(211, ((java.util.Collection)valoresfix.get("211")).iterator().next().toString().trim()));
	    }
    	// PegOffsetType
	    if(valoresfix.containsKey("836")){
	    	mensajeD.setField(new DoubleField(836,Double.valueOf( ((java.util.Collection)valoresfix.get("836")).iterator().next().toString()).doubleValue()));
	    }
		// PegPriceType
	    if(valoresfix.containsKey("1094")){
	    	mensajeD.setField(new IntField(1094,Integer.valueOf( ((java.util.Collection)valoresfix.get("1094")).iterator().next().toString().trim()).intValue()));
	    }
	    // TriggerType
	    if(valoresfix.containsKey("1100")){
	    	mensajeD.setField(new CharField(1100, ((java.util.Collection)valoresfix.get("1100")).iterator().next().toString().charAt(0)));
	    }
	    // TriggerPrice
	    if(valoresfix.containsKey("1102")){
	    	mensajeD.setField(new DoubleField(1102, Double.valueOf( ((java.util.Collection)valoresfix.get("1102")).iterator().next().toString()).doubleValue()));
	    }
	    
	    // Modificación de Gil -- Se adiciono el 20170130 peticion por correo para comunicacion con Sungard
	    if(valoresfix.containsKey("1")){
	    	mensajeD.setField(new StringField(1, ((java.util.Collection)valoresfix.get("1")).iterator().next().toString().trim()));
	    }

	    // Modificación de Gil -- Se adiciono el 20180831 para version 5.0 SP2
	    if(valoresfix.containsKey("55")){
	    	mensajeD.setField(new StringField(55, ((java.util.Collection)valoresfix.get("55")).iterator().next().toString().trim()));
	    }
		
	    if(valoresfix.containsKey("386") && valoresfix.containsKey("625")){
	    	/* Creacion con grupo generico NoTradingSessions */ 
	    	quickfix.Group grupoNoTradingSessions = new quickfix.Group(386,625);
	       //Primer grupo
	    	grupoNoTradingSessions.setField(new StringField(625,((java.util.Collection)valoresfix.get("625")).iterator().next().toString()));	    		
	    	mensajeD.addGroup(grupoNoTradingSessions); 
	    } 
	    LOG.info(" ++++++++++++ Mensaje FIX D generado +++++++++++");
	    LOG.info(mensajeD.toString());
		return mensajeD;
	}
	
	/**
	 * Crea un mensaje de reenvio de petición, tipo de mensaje "2"
	 * @param valoresfix
	 * @return mensaje2
	 */
	public Message getResendRequest(MultiMap valoresfix){
		//+++++++++++++++++++++ Craecion del mensaje FIX 2 ++++++++		 
		Message mensaje2 = new Message();
		// BeginString
		mensaje2.getHeader().setField(new StringField(8, "FIXT.1.1"));
		// SenderCompID
		mensaje2.getHeader().setField(new StringField(49, senderID));
		// TargetCompID, with enumeration
		mensaje2.getHeader().setField(new StringField(56, targetID));
		// MsgType
		mensaje2.getHeader().setField(new CharField(35, '2'));
		//Sending time
		mensaje2.getHeader().setField(new UtcTimeStampField(52));
		// BeginSeqNo
		if(valoresfix.containsKey("7")){
			mensaje2.setField(new IntField(7, Integer.valueOf(((java.util.Collection)valoresfix.get("7")).iterator().next().toString()).intValue()));
		}
		// EndSeqNo
		if(valoresfix.containsKey("16")){
			mensaje2.setField(new IntField(16, Integer.valueOf(((java.util.Collection)valoresfix.get("16")).iterator().next().toString()).intValue()));
		}
		return mensaje2;
	}
	
	/**
	 * Crea el mensaje de reenvio 57, mensaje tipo "2"
	 * @param valoresfix
	 * @return mensaje2
	 */
	public Message getResendRequest57(MultiMap valoresfix){
		//+++++++++++++++++++++ Craecion del mensaje FIX 2 ++++++++		 
		Message mensaje2 = new Message();
		// BeginString
		mensaje2.getHeader().setField(new StringField(8, "FIXT.1.1"));
		// SenderCompID
		mensaje2.getHeader().setField(new StringField(49, senderID));
		// TargetCompID, with enumeration
		mensaje2.getHeader().setField(new StringField(56, targetID));
		// MsgType
		mensaje2.getHeader().setField(new CharField(35, '2'));
	    // MsgType     header del regreso 8 para el tag 57
		mensaje2.getHeader().setField(new StringField(57, "8"));
		// Sending time
		mensaje2.getHeader().setField(new UtcTimeStampField(52));
		// BeginSeqNo
		if(valoresfix.containsKey("7")){
			mensaje2.setField(new IntField(7, Integer.valueOf(((java.util.Collection)valoresfix.get("7")).iterator().next().toString()).intValue()));
		}
		// EndSeqNo
		if(valoresfix.containsKey("16")){
			mensaje2.setField(new IntField(16, Integer.valueOf(((java.util.Collection)valoresfix.get("16")).iterator().next().toString()).intValue()));
		}
		return mensaje2;
	}
	
	/**
	 * Crea un mensaje con la lista de seguridad de una petición mensaje tipo "x"
	 * @param valoresfix
	 * @return mensajex
	 */
	public Message getSecurityListRequest(MultiMap valoresfix){
		//+++++++++++++++++++++ Craeciï¿½n del mensaje FIX x ++++++++		 
		Message mensajex = new Message();
		// BeginString
		mensajex.getHeader().setField(new StringField(8, "FIXT.1.1"));
		// SenderCompID
		mensajex.getHeader().setField(new StringField(49, senderID));
		// TargetCompID, with enumeration
		mensajex.getHeader().setField(new StringField(56, targetID));
		// MsgType
		mensajex.getHeader().setField(new CharField(35, 'x'));
		// Sending time
		mensajex.getHeader().setField(new UtcTimeStampField(52));
		// SecurityReqID
		if(valoresfix.containsKey("320")){
			mensajex.setField(new StringField(320, ((java.util.Collection)valoresfix.get("320")).iterator().next().toString()));
		}
        // SecurityListRequestType		 
		if(valoresfix.containsKey("559")){
			mensajex.setField(new IntField(559,Integer.valueOf(((java.util.Collection)valoresfix.get("559")).iterator().next().toString()).intValue() ));
		}
		return mensajex;
	}
	
	/**
	 * Crea el mensaje de cancelacion de petición mensaje tipo "F"
	 * @param valoresfix
	 * @return mensajeF
	 */
	public Message getOrderCancelRequest ( MultiMap valoresfix){
		//+++++++++++++++++++++ Craecion del mensaje FIX F ++++++++		 
		Message mensajeF = new Message();
		// BeginString
		mensajeF.getHeader().setField(new StringField(8, "FIXT.1.1"));
		// SenderCompID
		mensajeF.getHeader().setField(new StringField(49, senderID));
		// TargetCompID, with enumeration
		mensajeF.getHeader().setField(new StringField(56, targetID));
		// MsgType
		mensajeF.getHeader().setField(new CharField(35, 'F'));
		// Sending time
		mensajeF.getHeader().setField(new UtcTimeStampField(52));
		// HandlInst
		if(valoresfix.containsKey("21")){
			mensajeF.setField(new CharField(21, ((java.util.Collection)valoresfix.get("21")).iterator().next().toString().charAt(0)));
		}
        // ClOrdID
		if(valoresfix.containsKey("11")){
			mensajeF.setField(new StringField(11, ((java.util.Collection)valoresfix.get("11")).iterator().next().toString()));
		}
		// OrderID
		if(valoresfix.containsKey("37")){
			mensajeF.setField(new StringField(37, ((java.util.Collection)valoresfix.get("37")).iterator().next().toString()));
		}
		// Side
	    if(valoresfix.containsKey("54")) {
	    	mensajeF.setField(new CharField(54, ((java.util.Collection)valoresfix.get("54")).iterator().next().toString().charAt(0)));
	    }
	    // SecurityIDSource
	    if(valoresfix.containsKey("22")){
	    	mensajeF.setField(new StringField(22,((java.util.Collection)valoresfix.get("22")).iterator().next().toString()));
	    }
	    // SecurityID
	    if(valoresfix.containsKey("48")){
	    	mensajeF.setField(new StringField(48,((java.util.Collection)valoresfix.get("48")).iterator().next().toString()));
	    }
	    // OrderQty
	    if(valoresfix.containsKey("38")){
	    	mensajeF.setField(new DoubleField  (38,Double.valueOf( ((java.util.Collection)valoresfix.get("38")).iterator().next().toString()).doubleValue()));
	    }
	    // Price
	    if(valoresfix.containsKey("44")){
	    	mensajeF.setField(new DoubleField(44,Double.valueOf( ((java.util.Collection)valoresfix.get("44")).iterator().next().toString()).doubleValue()));
	    }
	    // TransactTime
	    if(valoresfix.containsKey("60")){
	    	mensajeF.setField(new StringField(60, ((java.util.Collection)valoresfix.get("60")).iterator().next().toString().trim()));
	    }
	    // OrigClOrdID
	    if(valoresfix.containsKey("41"))  {
	    	mensajeF.setField(new StringField  (41,((java.util.Collection)valoresfix.get("41")).iterator().next().toString()));
	    }
	    if(valoresfix.containsKey("769") && valoresfix.containsKey("771")){
	    	/* Creacion con grupo generico TrdRegTimestamp */ 
	    	//TrdRegTimestamp  768
	    	quickfix.Group grupoNoTrdRegTimestamps = new quickfix.Group(768,769);
	    	// Primer grupo
	    	grupoNoTrdRegTimestamps.setField(new StringField  (769,((java.util.Collection)valoresfix.get("769")).iterator().next().toString()));
	    	grupoNoTrdRegTimestamps.setField(new StringField  (771,((java.util.Collection)valoresfix.get("771")).iterator().next().toString()));				    	 
	    	mensajeF.addGroup(grupoNoTrdRegTimestamps); 
	    }
		return mensajeF;
	}

	/**
	 * Crea el mensaje de petición de cancelar/reemplazar orden, Mensaje tipo "G"
	 * @param valoresfix
	 * @return mensajeG
	 */
	public Message getOrderCancelReplaceRequest( MultiMap valoresfix){
		int NoPartyIDs = 0;
		Iterator partyID_it = null;
		Iterator partyIdsource_it = null;
		Iterator partyRole_it = null;
		LOG.info(" ++++++++++++ Craecion del mensaje FIX G +++++++++++");
		Message mensajeG = new Message();
		// BeginString
		mensajeG.getHeader().setField(new StringField(8, "FIXT.1.1"));
		// SenderCompID
		mensajeG.getHeader().setField(new StringField(49, senderID));
		// TargetCompID, with enumeration
		mensajeG.getHeader().setField(new StringField(56, targetID));
		// MsgType
		mensajeG.getHeader().setField(new CharField(35, 'G'));
		// Sending time
		mensajeG.getHeader().setField(new UtcTimeStampField(52));
		// Acceso
		if(valoresfix.containsKey("20001")){
			mensajeG.setField(new StringField(20001, ((java.util.Collection)valoresfix.get("20001")).iterator().next().toString()));
		}
		// ClOrdID
		if(valoresfix.containsKey("11")){
			mensajeG.setField(new StringField(11, ((java.util.Collection)valoresfix.get("11")).iterator().next().toString()));
		}
		// HandlInst
		if(valoresfix.containsKey("21")){
			mensajeG.setField(new CharField(21, ((java.util.Collection)valoresfix.get("21")).iterator().next().toString().charAt(0)));
		}
		// OrderID
		if(valoresfix.containsKey("37")){
			mensajeG.setField(new StringField(37, ((java.util.Collection)valoresfix.get("37")).iterator().next().toString()));
		}
		// Side
	    if(valoresfix.containsKey("54")) {
	    	mensajeG.setField(new CharField(54, ((java.util.Collection)valoresfix.get("54")).iterator().next().toString().charAt(0)));
	    }
	    // SecurityIDSource
	    if(valoresfix.containsKey("22")){
	    	mensajeG.setField(new StringField(22,((java.util.Collection)valoresfix.get("22")).iterator().next().toString()));
	    }
	    // SecurityID
	    if(valoresfix.containsKey("48")){
	    	mensajeG.setField(new StringField(48,((java.util.Collection)valoresfix.get("48")).iterator().next().toString()));
	    }
	    // OrderQty
	    if(valoresfix.containsKey("38")){
	    	mensajeG.setField(new DoubleField  (38,Double.valueOf( ((java.util.Collection)valoresfix.get("38")).iterator().next().toString()).doubleValue()));
	    }
	    // Price
	    if(valoresfix.containsKey("44")){
	    	mensajeG.setField(new DoubleField(44,Double.valueOf( ((java.util.Collection)valoresfix.get("44")).iterator().next().toString()).doubleValue()));
	    }
	    // ExpireDate
	    if(valoresfix.containsKey("432")){
	    	mensajeG.setField(new StringField  (432,((java.util.Collection)valoresfix.get("432")).iterator().next().toString()));
	    }
	    // ExpireTime
	    if(valoresfix.containsKey("126"))  {
	    	mensajeG.setField(new StringField  (126,((java.util.Collection)valoresfix.get("126")).iterator().next().toString()));
	    }
	    // StopPx
	    if(valoresfix.containsKey("99")) {
	    	mensajeG.setField(new DoubleField(99,Double.valueOf( ((java.util.Collection)valoresfix.get("99")).iterator().next().toString()).doubleValue()));
	    }
	    // MaxFloor
	    if(valoresfix.containsKey("111")){
	    	mensajeG.setField(new DoubleField(111,Double.valueOf( ((java.util.Collection)valoresfix.get("111")).iterator().next().toString().trim()).doubleValue()));
	    }
	    // OrdType
	    if(valoresfix.containsKey("40")) {
	    	mensajeG.setField(new CharField(40, ((java.util.Collection)valoresfix.get("40")).iterator().next().toString().charAt(0)));
	    }
	    // OrigClOrdID
	    if(valoresfix.containsKey("41"))  {
	    	mensajeG.setField(new StringField  (41,((java.util.Collection)valoresfix.get("41")).iterator().next().toString()));
	    }
	    // TimeInForce
	    if(valoresfix.containsKey("59")) {
	    	mensajeG.setField(new CharField(59,((java.util.Collection)valoresfix.get("59")).iterator().next().toString().charAt(0)));
	    }
	    if(valoresfix.containsKey("448") && valoresfix.containsKey("447") && valoresfix.containsKey("452")){
	    	partyID_it = ((java.util.Collection)valoresfix.get("448")).iterator();
	    	partyIdsource_it = ((java.util.Collection)valoresfix.get("447")).iterator();
	    	partyRole_it =  ((java.util.Collection)valoresfix.get("452")).iterator();
	    	/* Creación con grupo generico Paties */ 
	    	// NoPartyIDs 453 = 2
	    	quickfix.Group grupoParties = new quickfix.Group(453,448);
	    	// Primer grupo
	    	grupoParties.setField(new StringField(448,partyID_it.next().toString()));
	    	grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
	    	grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
	    	mensajeG.addGroup(grupoParties); 
		    
	    	// Segundo grupo
	    	grupoParties.setField(new StringField(448,partyID_it.next().toString()));
	    	grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
	    	grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
	    	mensajeG.addGroup(grupoParties); 	
	    }
	    // AccountType
	    if(valoresfix.containsKey("581")){
	    	mensajeG.setField(new IntField  (581,Double.valueOf( ((java.util.Collection)valoresfix.get("581")).iterator().next().toString()).intValue()));
	    }
	    // CoveredOrUncovered
	    if(valoresfix.containsKey("203")) {
	    	mensajeG.setField(new IntField(203,Integer.valueOf( ((java.util.Collection)valoresfix.get("203")).iterator().next().toString()).intValue()));
	    }
	    // MinQty
	    if(valoresfix.containsKey("110")){
	    	mensajeG.setField(new DoubleField(110,Double.valueOf( ((java.util.Collection)valoresfix.get("110")).iterator().next().toString()).doubleValue()));
	    }
	    // TransactTime
	    if(valoresfix.containsKey("60")){
	    	mensajeG.setField(new StringField(60, ((java.util.Collection)valoresfix.get("60")).iterator().next().toString().trim()));
	    }
	    // PegPriceType
	    if(valoresfix.containsKey("1094")) {
	    	mensajeG.setField(new IntField(1094,Integer.valueOf( ((java.util.Collection)valoresfix.get("1094")).iterator().next().toString()).intValue()));
	    }
		// PegOffsetValue
	    if(valoresfix.containsKey("211")){
	    	mensajeG.setField(new StringField(211, ((java.util.Collection)valoresfix.get("211")).iterator().next().toString().trim()));
	    }
	  	// PegOffsetType
	    if(valoresfix.containsKey("836")){
	    	mensajeG.setField(new DoubleField(836,Double.valueOf( ((java.util.Collection)valoresfix.get("836")).iterator().next().toString()).doubleValue()));
	    }
	 	// TriggerType
	    if(valoresfix.containsKey("1100")){
	    	mensajeG.setField(new CharField(1100, ((java.util.Collection)valoresfix.get("1100")).iterator().next().toString().charAt(0)));
	    }
	    // TriggerPrice
	    if(valoresfix.containsKey("1102")){
	    	mensajeG.setField(new DoubleField(1102, Double.valueOf( ((java.util.Collection)valoresfix.get("1102")).iterator().next().toString()).doubleValue()));
	    }
	    // Gil adicione el 20170130 a peticion por correo para comunicacion con Sungard
	    if(valoresfix.containsKey("1")){
	    	mensajeG.setField(new StringField(1, ((java.util.Collection)valoresfix.get("1")).iterator().next().toString().trim()));
	    }
	    if(valoresfix.containsKey("386") && valoresfix.containsKey("625")){
		    /* Creacion con grupo generico NoTradingSessions */ 
	    	//group name="NoTradingSessions" 386 = 1
	    	quickfix.Group grupoNoTradingSessions = new quickfix.Group(386,625);
	    	// Primer grupo
	    	grupoNoTradingSessions.setField(new StringField(625,((java.util.Collection)valoresfix.get("625")).iterator().next().toString()));	    		
	    	mensajeG.addGroup(grupoNoTradingSessions); 
	    } 
	    /* Fin de construccion mensaje FIX 
	     * 
	     */
	    LOG.info(" ++++++++++++ Mensaje FIX G generado +++++++++++");
	    LOG.info(mensajeG.toString());
		return mensajeG;
	}
	
	/**
	 * Creacion de mensaje new order single con tag 57
	 * @param valoresfix
	 * @return mensajeD
	 */
	public Message getNewOrderSingle57(MultiMap valoresfix){
		  int NoPartyIDs = 0;
		  Iterator partyID_it = null;
		  Iterator partyIdsource_it = null;
		  Iterator partyRole_it = null;
		  //+++++++++++++++++++++ Craecion del mensaje FIX D ++++++++
		  Message mensajeD = new Message();
		  mensajeD.
		  getHeader().setField(new StringField(8, "FIXT.1.1"));
		  // SenderCompID
		  mensajeD.getHeader().setField(new StringField(49, senderID));
		  // TargetCompID, with enumeration
		  mensajeD.getHeader().setField(new StringField(56, targetID));
		  // MsgType
		  mensajeD.getHeader().setField(new CharField(35, 'D'));
		  // Sending time
		  mensajeD.getHeader().setField(new UtcTimeStampField(52));
		  // TargetSubID
		  mensajeD.getHeader().setField(new StringField(57, "MXMP"));
		  // ClOrdID
		  if(valoresfix.containsKey("11")){
			  mensajeD.setField(new StringField(11, ((java.util.Collection)valoresfix.get("11")).iterator().next().toString()));
		  }
		  // NoPartyIDs Para determinar cuantos grupos se extraeran campo: "453"
		  if(valoresfix.containsKey("453")){
			  NoPartyIDs = Integer.valueOf( ((java.util.Collection)valoresfix.get("453")).iterator().next().toString()).intValue();
		  }
		  // Component Block - <Parties>.
		  if(valoresfix.containsKey("448") && valoresfix.containsKey("447") && valoresfix.containsKey("452")){
			  partyID_it = ((java.util.Collection)valoresfix.get("448")).iterator();
			  partyIdsource_it = ((java.util.Collection)valoresfix.get("447")).iterator();
			  partyRole_it =  ((java.util.Collection)valoresfix.get("452")).iterator();
			  /* Creacion con grupo generico Parties */ 
			  // NoPartyIDs 453 = 2
			  quickfix.Group grupoParties = new quickfix.Group(453,448);
			  // Primer grupo
			  grupoParties.setField(new StringField(448,partyID_it.next().toString()));
			  grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
			  grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
	    		
			  mensajeD.addGroup(grupoParties); 
	    
			  // Segundo grupo
			  grupoParties.setField(new StringField(448,partyID_it.next().toString()));
			  grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
			  grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
			  mensajeD.addGroup(grupoParties); 	
	    }
		// HandlInst
		if(valoresfix.containsKey("21")){
			mensajeD.setField(new CharField(21, ((java.util.Collection)valoresfix.get("21")).iterator().next().toString().charAt(0)));
		}
		// Acceso
		if(valoresfix.containsKey("20001")){
			mensajeD.setField(new StringField(20001,((java.util.Collection)valoresfix.get("20001")).iterator().next().toString()));
		}
	    // OrdType
	    if(valoresfix.containsKey("40")) {
	    	mensajeD.setField(new CharField(40, ((java.util.Collection)valoresfix.get("40")).iterator().next().toString().charAt(0)));
	    }
	    // Side
	    if(valoresfix.containsKey("54")) {
	    	mensajeD.setField(new CharField(54, ((java.util.Collection)valoresfix.get("54")).iterator().next().toString().charAt(0)));
	    }
	    // Account Type
	    if(valoresfix.containsKey("581")) {
	    	mensajeD.setField(new IntField(581, Integer.valueOf( ((java.util.Collection)valoresfix.get("581")).iterator().next().toString()).intValue()));
	    }
	    // CoveredOrUncovered
	    if(valoresfix.containsKey("203")) {
	    	mensajeD.setField(new IntField(203,Integer.valueOf( ((java.util.Collection)valoresfix.get("203")).iterator().next().toString()).intValue()));
	    }
	    // SecurityIDSource
	    if(valoresfix.containsKey("22")){
	    	mensajeD.setField(new StringField(22,((java.util.Collection)valoresfix.get("22")).iterator().next().toString()));
	    }
	    // SecurityID
	    if(valoresfix.containsKey("48")){
	    	mensajeD.setField(new StringField(48,((java.util.Collection)valoresfix.get("48")).iterator().next().toString()));
	    }
	    // OrderQty
	    if(valoresfix.containsKey("38")){
	    	mensajeD.setField(new DoubleField  (38,Double.valueOf( ((java.util.Collection)valoresfix.get("38")).iterator().next().toString()).doubleValue()));
	    }
	    // Price
	    if(valoresfix.containsKey("44")){
	    	mensajeD.setField(new DoubleField(44,Double.valueOf( ((java.util.Collection)valoresfix.get("44")).iterator().next().toString()).doubleValue()));
	    }
	    //ExpireDate
	    if(valoresfix.containsKey("432")){
	    	mensajeD.setField(new StringField  (432,((java.util.Collection)valoresfix.get("432")).iterator().next().toString()));
	    }
	    // ExpireTime
	    if(valoresfix.containsKey("126"))  {
	    	mensajeD.setField(new StringField  (126,((java.util.Collection)valoresfix.get("126")).iterator().next().toString()));
	    }
	    // StopPx
	    if(valoresfix.containsKey("99")) {
	    	mensajeD.setField(new DoubleField(99,Double.valueOf( ((java.util.Collection)valoresfix.get("99")).iterator().next().toString()).doubleValue()));
	    }
	    // TimeInForce
	    if(valoresfix.containsKey("59")) {
	    	mensajeD.setField(new CharField(59,((java.util.Collection)valoresfix.get("59")).iterator().next().toString().charAt(0)));
	    }
	    // TransactTime
	    if(valoresfix.containsKey("60")){
	    	mensajeD.setField(new StringField(60, ((java.util.Collection)valoresfix.get("60")).iterator().next().toString().trim()));
	    }
	    // MaxFloor
	    if(valoresfix.containsKey("111")){
	    	mensajeD.setField(new DoubleField(111,Double.valueOf( ((java.util.Collection)valoresfix.get("111")).iterator().next().toString().trim()).doubleValue()));
	    }
	   	// MinQty
	    if(valoresfix.containsKey("110")){
	    	mensajeD.setField(new DoubleField(110,Double.valueOf( ((java.util.Collection)valoresfix.get("110")).iterator().next().toString()).doubleValue()));
	    }
	    // PegOffsetValue
	    if(valoresfix.containsKey("211")){
	    	mensajeD.setField(new DoubleField(211,Double.valueOf( ((java.util.Collection)valoresfix.get("211")).iterator().next().toString()).doubleValue()));
	    }
	    // PegOffsetType
	    if(valoresfix.containsKey("836")){
	    	mensajeD.setField(new DoubleField(836,Double.valueOf( ((java.util.Collection)valoresfix.get("836")).iterator().next().toString()).doubleValue()));
	    }
		// PegPriceType
	    if(valoresfix.containsKey("1094")){
	    	mensajeD.setField(new IntField(1094,Integer.valueOf( ((java.util.Collection)valoresfix.get("1094")).iterator().next().toString().trim()).intValue()));
	    }
	    // TriggerType
	    if(valoresfix.containsKey("1100")){
	    	mensajeD.setField(new CharField(1100, ((java.util.Collection)valoresfix.get("1100")).iterator().next().toString().charAt(0)));
	    }
	    // TriggerPrice
	    if(valoresfix.containsKey("1102")){
	    	mensajeD.setField(new DoubleField(1102, Double.valueOf( ((java.util.Collection)valoresfix.get("1102")).iterator().next().toString()).doubleValue()));
	    }
	    // Gil adicione el 20170130 peticion por correo para comunicacion con Sungard
	    if(valoresfix.containsKey("1")){
	    	mensajeD.setField(new StringField(1, ((java.util.Collection)valoresfix.get("1")).iterator().next().toString().trim()));
	    }
	    if(valoresfix.containsKey("386") && valoresfix.containsKey("625")){
	    	/* Creacion con grupo generico NoTradingSessions */ 
	    	//group name="NoTradingSessions" 386 = 1
	    	quickfix.Group grupoNoTradingSessions = new quickfix.Group(386,625);
	    	//Primer grupo
	    	grupoNoTradingSessions.setField(new StringField(625,((java.util.Collection)valoresfix.get("625")).iterator().next().toString()));	    		
	    	mensajeD.addGroup(grupoNoTradingSessions); 
	    } 
	    /* Fin de construccion mensaje FIX 
	     * 
	     */
		return mensajeD;
	}
}
