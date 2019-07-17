package com.alida.fix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.quickfixj.QuickfixjComponent;
import org.apache.camel.component.quickfixj.QuickfixjEngine;
import org.apache.camel.component.quickfixj.converter.QuickfixjConverters;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.converter.StaticMethodTypeConverter;
import org.apache.camel.util.IOHelper;
import org.apache.commons.collections.MultiMap;

import quickfix.FixVersions;
import quickfix.Initiator;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.StringField;
import quickfix.field.BeginString;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.TransactTime;
import quickfix.fix50sp2.NewOrderSingle;

//import quickfix.fix50sp2.Message;
import quickfix.fixt11.Message;

import quickfix.*;

import com.sonicsw.xq.*;

/**
 * Esta clase se basa en el XQServiceEx de servicios Custom de Sonic, tiene por objetivo crear un contexto de Camel implementando 
 * el modulo FIX para versiones 4.2, 4.3, 4.4 y 5.0 con FIXT.1.1
 * El mensaje que se debe inyectar en este servicio debe ser texto plano con los tags separados por Pipe (|)  
 * @author (Mejoras y ajustes) Gilberto Romero - Alida Software
 * @version 1.3.0
 */
public class ArkaFix5 implements XQServiceEx {

	// This is the XQLog (the container's logging mechanism).
    private XQLog m_xqLog = null;

    //This is the the log prefix that helps identify this service during logging
    private String m_logPrefix = "FixInitiator";
    
    //These hold version information.
    private static int s_major = 1;
    private static int s_minor = 3;
    private static int s_buildNumber = 0;
    
    private File 				settingsFile;
    private File 				settingsFile2;
    private SessionSettings 	settings;
    private SessionID 			sessionID;
    private QuickfixjComponent 	component;
    private CamelContext 		camelContext;
	private String				qfjSettingsFile;
	private String				sonicjSettingsFile;
	private String 				senderID;
	private String 				targetID;

    /**
     * Constructor for a FixInitiator
     */
	public ArkaFix5 () {	
	}
	
	/**
	 * Método que inicializa el contexto de XQ y lee los archivos de parámetros.
	 */
    public void init(XQInitContext initialContext) throws XQServiceException {
    	
    	XQParameters params = initialContext.getParameters();
        m_xqLog = initialContext.getLog();
        setLogPrefix(params);
        m_xqLog.logInformation(" ********** [FixInitiator instance] init() **********");

        writeStartupMessage(params);
        writeParameters(params);
        //Obtiene el contenido de los archivos de parámetros para el Engine de FIX y el Broker de CX Messenger.
        qfjSettingsFile = params.getParameter("QuickFIX_Settings", 1);
        sonicjSettingsFile = params.getParameter("Sonic_Settings", 1); 
        try {
        	TreeMap<String, String> map = QuickfixjEngine.getProperties(qfjSettingsFile);
			//TreeMap<String, String> map = QuickfixjEngine.getProperties(qfjSettingsFile);
			senderID = map.get("SenderCompID");
			targetID = map.get("TargetCompID");
			m_xqLog.logInformation("SenderCompID=" + senderID + " TargetCompID=" + targetID);
		} catch (IOException e) {
			m_xqLog.logError("ERROR Unable to read file " + qfjSettingsFile + " --> " + e.getMessage());
			
		}
        m_xqLog.logInformation(" ********** [FixInitiator instance] Initialized. **********");
     }

    /**
     * Método que inicia el servicio y pone en funcionamiento su Listener
     */
    public void service(XQServiceContext ctx) throws XQServiceException {
    	m_xqLog.logInformation(" +++++++++++++  Message Received ++++++++++++++++");
    	String msgPart="";
		CustomFixMessage messageConstructor = new CustomFixMessage(senderID, targetID);
		// Obtiene el mensaje.
		XQEnvelope env = ctx.getNextIncoming();
		if (env != null) {
			XQMessage msg = env.getMessage();
			
			MultiMap valoresfix = null;
			try{
	        	msgPart = msg.getPart(0).getContent().toString();
	        	m_xqLog.logInformation(msgPart);
	        	// Obtiene los tags FIX que contiene el mensaje
				valoresfix = UtileriasFix.getValores(msgPart);
	        }
	        catch( Exception e) {
	        	m_xqLog.logError("ERROR reading FIX values. Message received: " + msgPart + "--> "+ e.getMessage());
	        }
			// Inicia la construcción del mensaje GIX con base en el contendio del mensaje
			if(valoresfix.containsKey("35"))
			    {
					m_xqLog.logInformation("Contains Header 35");
					Message mensajeFIX = null;
				if(valoresfix.containsKey("57"))
			    	{
					m_xqLog.logDebug( "Contains header 57");
					// Creacion de Mensaje D
					if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("D")){
						m_xqLog.logDebug("Message type D 57");
						mensajeFIX = messageConstructor.getNewOrderSingle57(valoresfix);
						m_xqLog.logDebug( " Sending message D 57... \n" +  mensajeFIX.toString() );
			        }
					// Creacion de mensaje 8
					else if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("8")){
						m_xqLog.logDebug("Message type 8 57");
						mensajeFIX = messageConstructor.getResendRequest57(valoresfix);
						m_xqLog.logDebug( " Sending message 8... \n" +  mensajeFIX.toString() );
					}      
			    }
			    else
			    {
			    	m_xqLog.logDebug( "Not contains Header 57");
			    	// Creacion de Mensaje D
			    	if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("D")){ 
			    		m_xqLog.logInformation("Message type D");
			    		m_xqLog.logDebug("Message type D");
			    		mensajeFIX = messageConstructor.getNewOrderSingle(valoresfix);
			    		m_xqLog.logInformation( " Sending message D 35... \n" +  mensajeFIX.toString() );
			        }
			    	// Creacion de mensaje G
			    	else if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("G")){
			    		m_xqLog.logInformation("Message type G");
			    		m_xqLog.logDebug("Message type G");
			    		mensajeFIX = messageConstructor.getOrderCancelReplaceRequest(valoresfix);
			    		m_xqLog.logDebug( " Sending message G... \n" +  mensajeFIX.toString() );
			    	}
			    	// Creacion de mensaje F
			    	else if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("F")){
			    		m_xqLog.logDebug("Message type F");
			    		mensajeFIX = messageConstructor.getOrderCancelRequest(valoresfix);
			    		m_xqLog.logDebug( " Sending message F... \n" +  mensajeFIX.toString() );
			    	}         
			    	// Creacion de mensaje x
			    	else if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("x")){
			    		m_xqLog.logDebug("Message type x");
			    		mensajeFIX = messageConstructor.getSecurityListRequest(valoresfix);
			    		m_xqLog.logDebug( " Sending message x... \n" +  mensajeFIX.toString() );
			    	} 
			    	// Creacion de mensaje 2
			    	else if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("2")){
			    		m_xqLog.logDebug("Message type 2");
			    		mensajeFIX = messageConstructor.getResendRequest(valoresfix);
			    		m_xqLog.logDebug( " Sending message 2... \n" +  mensajeFIX.toString() );
			    	}
			    	// Creacion de mensaje 8
			    	else if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("8")){
			    		m_xqLog.logDebug("Message type 8");
			    		mensajeFIX = messageConstructor.getResendRequest(valoresfix);
			    		m_xqLog.logDebug( " Sending message 8... \n" +  mensajeFIX.toString() );
			    	}
			    }
	        
				try {
					sendMessageToTarget(mensajeFIX);
				} catch (Exception e) {
					m_xqLog.logError("ERROR Unable to execute sendMessageToTarget() for message: " + mensajeFIX  + " ---> " + e.getMessage());
				}
				try {
					int iPartCnt = msg.getPartCount();
					for (int i = 0; i < iPartCnt; i++) {
						XQPart prt = msg.getPart(i);
						Object content = prt.getContent();
						//m_xqLog.logInformation("Content at Part [" + i + "]:\n" + content);
					}
				} catch (XQMessageException me) {
					throw new XQServiceException("Exception accessing XQMessage: " + me.getMessage(), me);
				}

				// Envía el mensaje al outbox, sin embargo, en este caso no es necesaria la salida del mensaje por este medio.
				Iterator<XQAddress> addressList = env.getAddresses();
				if (addressList.hasNext()) {
					// Add the message to the Outbox
					//ctx.addOutgoing(env);
				}
			}
		}
			 
	}
    
    /**
     * Envía el mensaje FIX al TargetID
     * @param message
     * @throws Exception
     */
	public void sendMessageToTarget(quickfix.Message message) throws Exception {
	    try {
	    	StringField msgVersion = message.getHeader().getField(new BeginString());
    		String sBegin = sessionID.getBeginString();
    		if (sBegin.equals(msgVersion.getValue())) {
    			boolean sendOk = Session.sendToTarget(message, sessionID);
	    		if (sendOk) {
	    			m_xqLog.logInformation("Message sent  to session " + sessionID.toString());
	    		}
	    		else {
	    			m_xqLog.logInformation("Message will be sent when the session is available. Session: " + sessionID.toString());
	    		}
	    		return;
	    	}
	    	
	    }
	    catch (Exception e) {
	    	m_xqLog.logError("ERROR " + e.getMessage() + " Message: " + message.toString() );
	    }
	}
	
    /**
     * Destruye el servicio.
     *
     * <p> Implementa el metodo XQService.
     */
    public void destroy() {
    	  m_xqLog.logInformation(" ********** [FixInitiator instance] destroy() **********");
    }
    
    /**
     * Cargado por el contenedor en al iniciar.
     *
     * <p> Implementa el metodo XQService.
     */
	public void start() {
		  m_xqLog.logInformation(" ********** [FixInitiator instance] start() **********");
	    try {
			sessionID = new SessionID(FixVersions.BEGINSTRING_FIXT11, senderID, targetID);
			m_xqLog.logInformation(" ********** [FixInitiator instance] Started... ********** " + sessionID.toString() );
		} catch (Exception e) {
			e.printStackTrace();
		}
	    // Crea el contexto de Camel
        camelContext = new DefaultCamelContext();
        // Agrega el componente Quickfix al contexto de Camel
        component = new org.apache.camel.component.quickfixj.QuickfixjComponent();
        component.setCamelContext(camelContext);
        m_xqLog.logInformation(" *********************** Camel Context Set. ***********************+*****");
        camelContext.addComponent("quickfix", component);
        try {
			camelContext.start();
	        m_xqLog.logInformation(" *********************** Camel Context Started rocj. ***********************+*****");
		} catch (Exception e2) {
			e2.printStackTrace();
		}

        Method converterMethod = null;
		try {
			converterMethod = QuickfixjConverters.class.getMethod("toSessionID", new Class<?>[] {String.class});
	        camelContext.getTypeConverterRegistry().addTypeConverter(SessionID.class, String.class,  new StaticMethodTypeConverter(converterMethod, false));
	        m_xqLog.logInformation(" *********************** Type Registry. ****************************");
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
		Endpoint e1;
		try {
			Map<String, Object> parameters = new HashMap<String, Object>();
			e1 = component.createEndpoint1("quickfix:/examples/arka50Linux.cfg", "examples/arka50Linux.cfg", parameters, qfjSettingsFile, sonicjSettingsFile);
		} catch (Exception e) {
			m_xqLog.logError("ERROR invoking method createEndpoint1(). " + e.getMessage());
			e.printStackTrace();
		}
		m_xqLog.logInformation(m_logPrefix + "Started.");
	}
	
	/**
	 * Obtiene la Uri del EndPoint
	 * @param configFilename
	 * @param sid
	 * @return uri 
	 */
    private String getEndpointUri(final String configFilename, SessionID sid) {
        String uri = "quickfix:" + configFilename;
        if (sid != null) {
            uri += "?sessionID=" + sid;
        }
        return uri;
	}

    /**
     * Escribe la configuración en el servicio
     * @throws IOException
     */
	private void writeSettings() throws IOException {
        writeSettings(settings, true);
    }

	/**
	 * Obtiene los parámetros del servicio a partir del archivo de confirguración 
	 * @param settings
	 * @param firstSettingsFile
	 * @throws IOException
	 */
    private void writeSettings(SessionSettings settings, boolean firstSettingsFile) throws IOException {
        FileOutputStream settingsOut = new FileOutputStream(firstSettingsFile ? settingsFile : settingsFile2);
        try {
            settings.toStream(settingsOut);
        } finally {
            IOHelper.close(settingsOut);
        }
    }

    /**
     * Obtiene el nomreb de sesión y los identificadores de Sender y Target
     * @param sessionSettings
     * @param sessionID
     */
	private void setSessionID(SessionSettings sessionSettings, SessionID sessionID) {
		// TODO Auto-generated method stub
        sessionSettings.setString(sessionID, SessionSettings.BEGINSTRING, sessionID.getBeginString());
        sessionSettings.setString(sessionID, SessionSettings.SENDERCOMPID, sessionID.getSenderCompID());
        sessionSettings.setString(sessionID, SessionSettings.TARGETCOMPID, sessionID.getTargetCompID());
	}

	/**
	 * Se ejecuta al terminar el servicio
	 */
	public void stop() {
		m_xqLog.logInformation(" ********** [FixInitiator instance] stop() **********");
		
		//NewOrderSingle order = new NewOrderSingle(new ClOrdID("MISYS1002"),
		//		new Side(Side.BUY), new TransactTime(new Date()), new OrdType(OrdType.LIMIT));
		//order.set(new OrderQty(45));
		//order.set(new Price(240.90));
		//m_xqLog.logInformation("Sending Order to Server");
		//try {
		//	Session.sendToTarget(order, sessionID);
		//} catch (SessionNotFound e) {
		//	e.printStackTrace();
		//}
		
		m_xqLog.logInformation(" ********** [FixInitiator instance] Stopped. **********");
	}
	
	/**
     * Establece el prefijo de XQLog para esta instancia del servicio
     */
	protected void setLogPrefix(XQParameters params) {
		String serviceName = params.getParameter(XQConstants.PARAM_SERVICE_NAME, XQConstants.PARAM_STRING);
		m_logPrefix = "[ " +  serviceName + " ]";
	}
	
    /**
     * Provee acceso a la versión del servicio implementado.
     *
     */
	protected String getVersion(){
		return s_major + "." + s_minor + ". build " + s_buildNumber;  
	}
	
    /**
     * Escribe un mensaje de inicio del servicio en el log.
     */
    protected void writeStartupMessage(XQParameters params) {
        final StringBuffer buffer = new StringBuffer();
        String serviceTypeName = params.getParameter(XQConstants.SERVICE_PARAM_SERVICE_TYPE, XQConstants.PARAM_STRING);
        buffer.append("\n\n");
        buffer.append("\t\t " + serviceTypeName + "\n ");
        buffer.append("\t\t Version ");
        buffer.append(" " +getVersion());
        buffer.append("\n");
        m_xqLog.logInformation(buffer.toString());
	}
    
    /**
     * Escribe los parámetros al Log.
     */
    protected void writeParameters(XQParameters params) {
        final Map<String,XQParameterInfo> map = params.getAllInfo();
        final Iterator<XQParameterInfo> iter = map.values().iterator();

        while (iter.hasNext()) {
            final XQParameterInfo info = (XQParameterInfo) iter.next();

            if (info.getType() == XQConstants.PARAM_XML) {
                m_xqLog.logInformation( m_logPrefix + "Parameter Name =  " + info.getName());
            } else if (info.getType() == XQConstants.PARAM_STRING) {
            	m_xqLog.logInformation( m_logPrefix + "Parameter Name = " + info.getName());
            }

            if (info.getRef() != null) {
            	m_xqLog.logInformation(m_logPrefix +"Parameter Reference = " + info.getRef());
            	m_xqLog.logInformation(m_logPrefix +"----Parameter Value Start--------");
            	m_xqLog.logInformation("\n" +  info.getValue() +"\n");
            	m_xqLog.logInformation(m_logPrefix +"----Parameter Value End--------");
            }else{
            	m_xqLog.logInformation(m_logPrefix +"Parameter Value = " + info.getValue());            	
            }
        }
    }
}