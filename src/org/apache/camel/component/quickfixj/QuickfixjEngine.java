/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.quickfixj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.camel.support.ServiceSupport;
import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Acceptor;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.field.ApplVerID;
import quickfix.fixt11.MessageFactory;
//import quickfix.fix50sp2.MessageFactory;
//import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldConvertError;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Initiator;
import quickfix.JdbcLogFactory;
import quickfix.JdbcSetting;
import quickfix.JdbcStoreFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.Message;
//import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RejectLogon;
import quickfix.SLF4JLogFactory;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SleepycatStoreFactory;
import quickfix.SocketAcceptor;
import quickfix.SocketInitiator;
import quickfix.ThreadedSocketAcceptor;
import quickfix.ThreadedSocketInitiator;
import quickfix.UnsupportedMessageType;


public class QuickfixjEngine extends ServiceSupport {
    public static final String DEFAULT_START_TIME = "10:00:00";
    public static final String DEFAULT_END_TIME = "00:00:00";
    public static final long DEFAULT_HEARTBTINT = 30;
    public static final String SETTING_THREAD_MODEL = "ThreadModel";
    public static final String SETTING_USE_JMX = "UseJmx";

    private static final Logger LOG = LoggerFactory.getLogger(QuickfixjEngine.class);

    private Acceptor acceptor;
    private Initiator initiator;
    private JmxExporter jmxExporter;
    private MessageStoreFactory messageStoreFactory;
    private LogFactory sessionLogFactory;
    private MessageFactory messageFactory;
    private final MessageCorrelator messageCorrelator = new MessageCorrelator();
    private List<QuickfixjEventListener> eventListeners = new CopyOnWriteArrayList<QuickfixjEventListener>();
    private final String uri;
    private ObjectName acceptorObjectName;
    private ObjectName initiatorObjectName;
    private final SessionSettings settings;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private boolean lazy;

    /********************************************/
    //Variables de conexion a Sonic
    private static javax.jms.Connection connect = null;
    private static javax.jms.Session pubSession = null;
    private static javax.jms.MessageProducer publisher = null; 
    private static String brokerList = "";
    //private static String topicoS = "";
    private static String queueName = "";
    private static String username = "";
    private static String password = "";
 
    
    public enum ThreadModel {
        ThreadPerConnector, ThreadPerSession;
    }

    @Deprecated
    public QuickfixjEngine(String uri, String settingsResourceName, boolean forcedShutdown)
        throws ConfigError, FieldConvertError, IOException, JMException {

        this(uri, settingsResourceName, forcedShutdown, null, null, null);
    }

    public QuickfixjEngine(String uri, String settingsResourceName) throws ConfigError, FieldConvertError, IOException, JMException {
        this(uri, settingsResourceName, null, null, null);
    }

    @Deprecated
    public QuickfixjEngine(String uri, String settingsResourceName, boolean forcedShutdown,
            MessageStoreFactory messageStoreFactoryOverride, LogFactory sessionLogFactoryOverride,
            MessageFactory messageFactoryOverride) throws ConfigError, FieldConvertError, IOException, JMException {
        this(uri, loadSettings(settingsResourceName, null, null), forcedShutdown, messageStoreFactoryOverride,
                sessionLogFactoryOverride, messageFactoryOverride);
    }

    public QuickfixjEngine(String uri, String settingsResourceName, MessageStoreFactory messageStoreFactoryOverride, LogFactory sessionLogFactoryOverride,
                           MessageFactory messageFactoryOverride) throws ConfigError, FieldConvertError, IOException, JMException {
        this(uri, loadSettings(settingsResourceName, null, null), messageStoreFactoryOverride, sessionLogFactoryOverride, messageFactoryOverride);
    }

    @Deprecated
    public QuickfixjEngine(String uri, SessionSettings settings, boolean forcedShutdown,
            MessageStoreFactory messageStoreFactoryOverride, LogFactory sessionLogFactoryOverride,
            MessageFactory messageFactoryOverride) throws ConfigError, FieldConvertError, IOException, JMException {
        this(uri, settings, messageStoreFactoryOverride, sessionLogFactoryOverride, messageFactoryOverride);
    }

    public QuickfixjEngine(String uri, SessionSettings settings, MessageStoreFactory messageStoreFactoryOverride, LogFactory sessionLogFactoryOverride,
                           MessageFactory messageFactoryOverride) throws ConfigError, FieldConvertError, IOException, JMException {
        this(uri, settings, messageStoreFactoryOverride, sessionLogFactoryOverride, messageFactoryOverride, false);
    }

    public QuickfixjEngine(String uri, SessionSettings settings, MessageStoreFactory messageStoreFactoryOverride, LogFactory sessionLogFactoryOverride,
            MessageFactory messageFactoryOverride, boolean lazy) throws ConfigError, FieldConvertError, IOException, JMException {
        addEventListener(messageCorrelator);

        this.uri = uri;
        this.lazy = lazy;
        this.settings = settings;

        // overrides
        if (messageFactoryOverride != null) {
            messageFactory = messageFactoryOverride;
        }
        if (sessionLogFactoryOverride != null) {
            sessionLogFactory = sessionLogFactoryOverride;
        }
        if (messageStoreFactoryOverride != null) {
            messageStoreFactory = messageStoreFactoryOverride;
        }

        if (!lazy) {
            initializeEngine();
        }
    }

    void initializeEngine() throws ConfigError,
            FieldConvertError, JMException, IOException {
    	

    	/*******************************************/
    	//gil
    	//File file = new File("/opt/aurea/sonic/Workbench10.0/workspace/Arka1/src/org/apache/camel/component/quickfixj/examples/inprocess.cfg");
		//FileInputStream inputStream = null;
		//inputStream = new FileInputStream(file);
		
		//SessionSettings settings = new SessionSettings(inputStream);
		//if (inputStream != null) inputStream.close();
    	/*******************************************/
		
    	
        if (messageFactory == null) {
            messageFactory = new MessageFactory();
        }
        
        if (sessionLogFactory == null) {
            sessionLogFactory = inferLogFactory(settings);
        }
        if (messageStoreFactory == null) {
            messageStoreFactory = inferMessageStoreFactory(settings);
        }

        // Set default session schedule if not specified in configuration
        if (!settings.isSetting(Session.SETTING_START_TIME)) {
            settings.setString(Session.SETTING_START_TIME, DEFAULT_START_TIME);
        }
        if (!settings.isSetting(Session.SETTING_END_TIME)) {
            settings.setString(Session.SETTING_END_TIME, DEFAULT_END_TIME);
        }
        // Default heartbeat interval
        if (!settings.isSetting(Session.SETTING_HEARTBTINT)) {
            settings.setLong(Session.SETTING_HEARTBTINT, DEFAULT_HEARTBTINT);
        }

        // Allow specification of the QFJ threading model
        ThreadModel threadModel = ThreadModel.ThreadPerConnector;
        if (settings.isSetting(SETTING_THREAD_MODEL)) {
            threadModel = ThreadModel.valueOf(settings.getString(SETTING_THREAD_MODEL));
        }

        if (settings.isSetting(SETTING_USE_JMX) && settings.getBool(SETTING_USE_JMX)) {
            LOG.info("Enabling JMX for QuickFIX/J");
            jmxExporter = new JmxExporter();
        } else {
            jmxExporter = null;
        }

        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            if (isConnectorRole(settings, SessionFactory.ACCEPTOR_CONNECTION_TYPE)) {
                acceptor = createAcceptor(new Dispatcher(), settings, messageStoreFactory, 
                    sessionLogFactory, messageFactory, threadModel);
            } else {
                acceptor = null;
            }

            if (isConnectorRole(settings, SessionFactory.INITIATOR_CONNECTION_TYPE)) {
                initiator = createInitiator(new Dispatcher(), settings, messageStoreFactory, 
                    sessionLogFactory, messageFactory, threadModel);
            } else {
                initiator = null;
            }

            if (acceptor == null && initiator == null) {
                throw new ConfigError("No connector role");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
        initialized.set(true);
    }

    static SessionSettings loadSettings(String settingsResourceName, String qfjSettingsFile, String sonicSettingsFile) throws ConfigError, IOException {
    	
    	LOG.info("Loading settings from " + qfjSettingsFile);
    	
    	//File file = new File("/opt/aurea/sonic/Workbench10.0/workspace/Arka1/src/com/alidasoftware/fix/inprocess.cfg");
    	//"C:\\Aurea\\SonicVPM\\Workbench10.0\\workspace\\ArkaFix\\src\\com\\alida\\fix\\inprocess.cfg
    	File file = new File(qfjSettingsFile);

		FileInputStream inputStream1 = null;
		inputStream1 = new FileInputStream(file);
    	
        //InputStream inputStream = ObjectHelper.loadResourceAsStream(settingsResourceName);
        if (inputStream1 == null) {
            throw new IllegalArgumentException("ERROR loading:  " + qfjSettingsFile);
        }

        /**************************************************/
        //De aqui obtiene la configuracio del broker
        try {
        	// C:\\Aurea\\SonicVPM\\Workbench10.0\\workspace\\ArkaFix\\src\\com\\alida\\fix\\broker.properties
            TreeMap<String, String> map = getProperties(sonicSettingsFile);
            //System.out.println(map);
         }
         catch (IOException e) {
             // error using the file
        	 throw new IllegalArgumentException("ERROR loading properties file:  " + sonicSettingsFile);
         }
        /**************************************************/
        //Aqui va la conexion al broker
        //javax.jms.ConnectionFactory factory;
        progress.message.jclient.ConnectionFactory factory;
        try
        {
        	LOG.info("Establishing connection to " + brokerList + " username:" + username + "/password:" + password);
            factory = new progress.message.jclient.ConnectionFactory();
            factory.setConnectionURLs(brokerList);
            factory.setFaultTolerant(true);
            //factory.setFaultTolerantReconnectTimeout(30);
            connect = factory.createConnection (username, password);
            pubSession = connect.createSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
        }
        catch (javax.jms.JMSException e)
        {
        	LOG.error("ERROR Unable to connect to " + brokerList);
            e.printStackTrace();
            //System.exit(1);
        }

        // Create Publisher to queue
        try
        {
             javax.jms.Queue queue = pubSession.createQueue(queueName);
        	//javax.jms.Topic topico = pubSession.createTopic (topicoS);
            //javax.jms.MessageConsumer subscriber = subSession.createConsumer(topic);
            //subscriber.setMessageListener(this);
            //publisher = pubSession.createProducer(topico);
             publisher = pubSession.createProducer(queue);
             LOG.info("Creating queue " + queueName);
            // Now that setup is complete, start the Connection
            connect.start();
        }
        catch (javax.jms.JMSException jmse)
        {
            jmse.printStackTrace();
        }
        
        /**************************************************/
        
        return new SessionSettings(inputStream1);
    }

    public static TreeMap<String, String> getProperties(String infile) throws IOException {
        final int lhs = 0;
        final int rhs = 1;
        TreeMap<String, String> map = new TreeMap<String, String>();
        BufferedReader  bfr = new BufferedReader(new FileReader(new File(infile)));

        String line;
        while ((line = bfr.readLine()) != null) {
            //Reading properties from file: broker.properties
        	if (line.startsWith("brokerList.url")) {
                String[] pair = line.trim().split("=");
                map.put(pair[lhs].trim(), pair[rhs].trim());
                brokerList = pair[rhs].trim();
            }
            if (line.startsWith("broker.usuario")) {
                String[] pair = line.trim().split("=");
                map.put(pair[lhs].trim(), pair[rhs].trim());
                username = pair[rhs].trim();
            }
            if (line.startsWith("broker.password")) {
                String[] pair = line.trim().split("=");
                map.put(pair[lhs].trim(), pair[rhs].trim());
                password = pair[rhs].trim();
            }
            if (line.startsWith("broker.queue")) {
                String[] pair = line.trim().split("=");
                map.put(pair[lhs].trim(), pair[rhs].trim());
                queueName = pair[rhs].trim();
            }
          //Reading properties from file: inprocess.cfg
            if (line.startsWith("SenderCompID")) {
                String[] pair = line.trim().split("=");
                map.put(pair[lhs].trim(), pair[rhs].trim()); 
            }
            if (line.startsWith("TargetCompID")) {
                String[] pair = line.trim().split("=");
                map.put(pair[lhs].trim(), pair[rhs].trim()); 
            }
        }

        bfr.close();

        return(map);
    }    
    @Override
    protected void doStart() throws Exception {
        if (acceptor != null) {
            acceptor.start();
            if (jmxExporter != null) {
                acceptorObjectName = jmxExporter.register(acceptor);
            }
        }
        if (initiator != null) {
            initiator.start();
            if (jmxExporter != null) {
                initiatorObjectName = jmxExporter.register(initiator);
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (acceptor != null) {
            acceptor.stop();

            if (jmxExporter != null && acceptorObjectName != null) {
                jmxExporter.getMBeanServer().unregisterMBean(acceptorObjectName);
            }
        }
        if (initiator != null) {
            initiator.stop();

            if (jmxExporter != null && initiatorObjectName != null) {
                jmxExporter.getMBeanServer().unregisterMBean(initiatorObjectName);
            }
        }
    }

    @Override
    protected void doShutdown() throws Exception {
        // also clear event listeners
        eventListeners.clear();
    }

    private Initiator createInitiator(Application application, SessionSettings settings,
            MessageStoreFactory messageStoreFactory, LogFactory sessionLogFactory, 
            MessageFactory messageFactory, ThreadModel threadModel) throws ConfigError {
        
        Initiator initiator;
        if (threadModel == ThreadModel.ThreadPerSession) {
            initiator = new ThreadedSocketInitiator(application, messageStoreFactory, settings, sessionLogFactory, messageFactory);
        } else if (threadModel == ThreadModel.ThreadPerConnector) {
            initiator = new SocketInitiator(application, messageStoreFactory, settings, sessionLogFactory, messageFactory);
        } else {
            throw new ConfigError("Unknown thread mode: " + threadModel);
        }
        return initiator;
    }

    private Acceptor createAcceptor(Application application, SessionSettings settings,
            MessageStoreFactory messageStoreFactory, LogFactory sessionLogFactory, 
            MessageFactory messageFactory, ThreadModel threadModel) throws ConfigError {

        Acceptor acceptor;
        if (threadModel == ThreadModel.ThreadPerSession) {
            acceptor = new ThreadedSocketAcceptor(application, messageStoreFactory, settings, sessionLogFactory, messageFactory);
        } else if (threadModel == ThreadModel.ThreadPerConnector) {
            acceptor = new SocketAcceptor(application, messageStoreFactory, settings, sessionLogFactory, messageFactory);
        } else {
            throw new ConfigError("Unknown thread mode: " + threadModel);
        }
        return acceptor;
    }

    private MessageStoreFactory inferMessageStoreFactory(SessionSettings settings) throws ConfigError {
        Set<MessageStoreFactory> impliedMessageStoreFactories = new HashSet<MessageStoreFactory>();
        isJdbcStore(settings, impliedMessageStoreFactories);
        isFileStore(settings, impliedMessageStoreFactories);
        isSleepycatStore(settings, impliedMessageStoreFactories);
        if (impliedMessageStoreFactories.size() > 1) {
            throw new ConfigError("Ambiguous message store implied in configuration.");
        }
        MessageStoreFactory messageStoreFactory;
        if (impliedMessageStoreFactories.size() == 1) {
            messageStoreFactory = impliedMessageStoreFactories.iterator().next();
        } else {
            messageStoreFactory = new MemoryStoreFactory();
        }
        LOG.info("Inferring message store factory: {}", messageStoreFactory.getClass().getName());
        return messageStoreFactory;
    }

    private void isSleepycatStore(SessionSettings settings, Set<MessageStoreFactory> impliedMessageStoreFactories) {
        if (settings.isSetting(SleepycatStoreFactory.SETTING_SLEEPYCAT_DATABASE_DIR)) {
            impliedMessageStoreFactories.add(new SleepycatStoreFactory(settings));
        }
    }

    private void isFileStore(SessionSettings settings, Set<MessageStoreFactory> impliedMessageStoreFactories) {
        if (settings.isSetting(FileStoreFactory.SETTING_FILE_STORE_PATH)) {
            impliedMessageStoreFactories.add(new FileStoreFactory(settings));
        }
    }

    private void isJdbcStore(SessionSettings settings, Set<MessageStoreFactory> impliedMessageStoreFactories) {
        if (settings.isSetting(JdbcSetting.SETTING_JDBC_DRIVER) || settings.isSetting(JdbcSetting.SETTING_JDBC_DS_NAME)) {
            impliedMessageStoreFactories.add(new JdbcStoreFactory(settings));
        }
    }

    private LogFactory inferLogFactory(SessionSettings settings) throws ConfigError {
        Set<LogFactory> impliedLogFactories = new HashSet<LogFactory>();
        isFileLog(settings, impliedLogFactories);
        isScreenLog(settings, impliedLogFactories);
        isSL4JLog(settings, impliedLogFactories);
        isJdbcLog(settings, impliedLogFactories);
        if (impliedLogFactories.size() > 1) {
            throw new ConfigError("Ambiguous log factory implied in configuration");
        }
        LogFactory sessionLogFactory;
        if (impliedLogFactories.size() == 1) {
            sessionLogFactory = impliedLogFactories.iterator().next();
        } else {
            // Default
            sessionLogFactory = new ScreenLogFactory(settings);
        }
        LOG.info("Inferring log factory: {}", sessionLogFactory.getClass().getName());
        return sessionLogFactory;
    }

    private void isScreenLog(SessionSettings settings, Set<LogFactory> impliedLogFactories) {
        if (settings.isSetting(ScreenLogFactory.SETTING_LOG_EVENTS)
                || settings.isSetting(ScreenLogFactory.SETTING_LOG_INCOMING)
                || settings.isSetting(ScreenLogFactory.SETTING_LOG_OUTGOING)) {
            impliedLogFactories.add(new ScreenLogFactory(settings));
        }
    }

    private void isFileLog(SessionSettings settings, Set<LogFactory> impliedLogFactories) {
        if (settings.isSetting(FileLogFactory.SETTING_FILE_LOG_PATH)) {
            impliedLogFactories.add(new FileLogFactory(settings));
        }
    }

    private void isJdbcLog(SessionSettings settings, Set<LogFactory> impliedLogFactories) {
        if ((settings.isSetting(JdbcSetting.SETTING_JDBC_DRIVER) || settings.isSetting(JdbcSetting.SETTING_JDBC_DS_NAME))
                && settings.isSetting(JdbcSetting.SETTING_LOG_EVENT_TABLE)) {
            impliedLogFactories.add(new JdbcLogFactory(settings));
        }
    }

    private void isSL4JLog(SessionSettings settings, Set<LogFactory> impliedLogFactories) {
        for (Object key : settings.getDefaultProperties().keySet()) {
            if (key.toString().startsWith("SLF4J")) {
                impliedLogFactories.add(new SLF4JLogFactory(settings));
                return;
            }
        }
    }

    private boolean isConnectorRole(SessionSettings settings, String connectorRole) throws ConfigError {
        boolean hasRole = false;
        Iterator<SessionID> sessionIdItr = settings.sectionIterator();
        while (sessionIdItr.hasNext()) {
            try {
                if (connectorRole.equals(settings.getString(sessionIdItr.next(),
                        SessionFactory.SETTING_CONNECTION_TYPE))) {
                    hasRole = true;
                    break;
                }
            } catch (FieldConvertError e) {
                throw new ConfigError(e);
            }
        }
        return hasRole;
    }
    
    public void addEventListener(QuickfixjEventListener listener) {
        eventListeners.add(listener);
    }
    
    public void removeEventListener(QuickfixjEventListener listener) {
        eventListeners.remove(listener);
    }

    private class Dispatcher implements Application {
        @Override
        public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
            try {
            	//gil
                LOG.info("--------------------------------------------- From Admin----- Notificación del mensaje de administración que se recibe desde el destino. " + message.toString());
                //dispatch(QuickfixjEventCategory.AdminMessageReceived, sessionID, message);
                //LOG.info("---------------------------------------------Despues de Dispatch");
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                rethrowIfType(e, FieldNotFound.class);
                rethrowIfType(e, IncorrectDataFormat.class);
                rethrowIfType(e, IncorrectTagValue.class);
                rethrowIfType(e, RejectLogon.class);               
                throw new DispatcherException(e);
            }
        }
        
        @Override
        public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
            try {
                LOG.info("---------------------------------------------FromApp--- Notificación de la aplicación de recibido por el del destino. "  + message.toString());
            	//gil
                javax.jms.TextMessage msg = pubSession.createTextMessage();
                msg.setText( message.toString() );
                publisher.send( msg );
                LOG.info("Message sent ........................................." + message.toString());
                dispatch(QuickfixjEventCategory.AppMessageReceived, sessionID, message);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                rethrowIfType(e, FieldNotFound.class);
                rethrowIfType(e, IncorrectDataFormat.class);
                rethrowIfType(e, IncorrectTagValue.class);
                rethrowIfType(e, UnsupportedMessageType.class);
                throw new DispatcherException(e);
            }
        }

        @Override
        public void onCreate(SessionID sessionID) {
            try {
                //LOG.info("---------------------------------------------Antes de Dispatch On Create");
                dispatch(QuickfixjEventCategory.SessionCreated, sessionID, null);
            } catch (Exception e) {
                throw new DispatcherException(e);
            }
        }

        @Override
        public void onLogon(SessionID sessionID) {
            try {
                //LOG.info("---------------------------------------------Antes de Dispatch On Logon "  + sessionID.toString());
                dispatch(QuickfixjEventCategory.SessionLogon, sessionID, null); 
            } catch (Exception e) {
                throw new DispatcherException(e);
            }
        }

        @Override
        public void onLogout(SessionID sessionID) {
            try {
                //LOG.info("---------------------------------------------Antes de Dispatch On Logout " + sessionID.toString());
                dispatch(QuickfixjEventCategory.SessionLogoff, sessionID, null);
            } catch (Exception e) {
                throw new DispatcherException(e);
            }
        }

        @Override
        public void toAdmin(Message message, SessionID sessionID) {
            try {
                LOG.info("---------------------------------------------ToAdmin--- Notificación de envío de mensaje de administrador a destino." + message.toString());
                //Session.lookupSession(sessionID).setTargetDefaultApplicationVersionID(new ApplVerID("9"));
                dispatch(QuickfixjEventCategory.AdminMessageSent, sessionID, message);
            } catch (Exception e) {
                throw new DispatcherException(e);
            }
        }

        @Override
        public void toApp(Message message, SessionID sessionID) throws DoNotSend {
            try {
            	/*************************************************/
            	//Aqui envia el mensaje al broker
                /*javax.jms.TextMessage msg = pubSession.createTextMessage();
                msg.setText( message.toString() );
                publisher.send( msg );*/
            	
            	/*************************************************/
                LOG.info("---------------------------------------------ToApp----Notificación del mensaje de la aplicación que se envía al destino. " +  message.toString());
                dispatch(QuickfixjEventCategory.AppMessageSent, sessionID, message);
            } catch (Exception e) {
                throw new DispatcherException(e);
            }
        }

        private <T extends Exception> void rethrowIfType(Exception e, Class<T> exceptionClass) throws T {
            if (e.getClass() == exceptionClass) {
                throw exceptionClass.cast(e);
            }
        }

        private void dispatch(QuickfixjEventCategory quickfixjEventCategory, SessionID sessionID, Message message) throws Exception {
            //LOG.info("FIX event dispatched: {} {}", quickfixjEventCategory, message != null ? message : "");
            for (QuickfixjEventListener listener : eventListeners) {
                // Exceptions propagate back to the FIX engine so sequence numbers can be adjusted
                listener.onEvent(quickfixjEventCategory, sessionID, message);
            }
        }

        private class DispatcherException extends RuntimeException {

            private static final long serialVersionUID = 1L;

            public DispatcherException(Throwable cause) {
                super(cause);
            }
        }
    }

    public String getUri() {
        return uri;
    }

    public MessageCorrelator getMessageCorrelator() {
        return messageCorrelator;
    }

    public boolean isInitialized() {
        return this.initialized.get();
    }

    public boolean isLazy() {
        return this.lazy;
    }

    // For Testing
    Initiator getInitiator() {
        return initiator;
    }

    // For Testing
    Acceptor getAcceptor() {
        return acceptor;
    }

    // For Testing
    public MessageStoreFactory getMessageStoreFactory() {
        return messageStoreFactory;
    }

    // For Testing
    public LogFactory getLogFactory() {
        return sessionLogFactory;
    }

    // For Testing
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }
}
