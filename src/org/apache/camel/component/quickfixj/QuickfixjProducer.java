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

import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
//gil
import org.apache.commons.collections.MultiMap;

import com.alida.fix.UtileriasFix;

import quickfix.CharField;
import quickfix.IntField;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.StringField;

public class QuickfixjProducer extends DefaultProducer {
    public static final String CORRELATION_TIMEOUT_KEY = "CorrelationTimeout";
    public static final String CORRELATION_CRITERIA_KEY = "CorrelationCriteria";
    //gil
	private MultiMap valoresfix;
    
    public QuickfixjProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public QuickfixjEndpoint getEndpoint() {
        return (QuickfixjEndpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            getEndpoint().ensureInitialized();
            sendMessage(exchange, exchange.getIn());
        } catch (Exception e) {
            exchange.setException(e);
        }
    }

    void sendMessage(Exchange exchange, org.apache.camel.Message camelMessage) throws Exception {
    	
        Message message = camelMessage.getBody(Message.class);

        SessionID messageSessionID = getEndpoint().getSessionID();
        if (messageSessionID == null) {
            messageSessionID = MessageUtils.getSessionID(message);
        }

        Session session = getSession(messageSessionID);
        if (session == null) {
            throw new IllegalStateException("Unknown session: " + messageSessionID);
        }

        Callable<Message> callable = null;

        if (exchange.getPattern().isOutCapable()) {
            MessageCorrelator messageCorrelator = getEndpoint().getEngine().getMessageCorrelator();
            callable = messageCorrelator.getReply(getEndpoint().getSessionID(), exchange);
        }

        //gil
		int NoPartyIDs = 0;
		Iterator partyID_it = null;
		Iterator partyIdsource_it = null;
		Iterator partyRole_it = null;
        try{
        	valoresfix = UtileriasFix.getValores(exchange.getIn().getBody().toString());
        }
        catch( Exception e) {
        	log.info("ERROR reading FIX values - " + e.getMessage());
        }
        if(valoresfix.containsKey("35"))
		{
    		if(((java.util.Collection)valoresfix.get("35")).iterator().next().toString().equals("D")){
    			if(valoresfix.containsKey("453")){
        			NoPartyIDs = Integer.valueOf( ((java.util.Collection)valoresfix.get("453")).iterator().next().toString()).intValue();
        		}
    		    if(valoresfix.containsKey("448") && valoresfix.containsKey("447") && valoresfix.containsKey("452")){
    		    	partyID_it 			= ((java.util.Collection)valoresfix.get("448")).iterator();
    		    	partyIdsource_it 	= ((java.util.Collection)valoresfix.get("447")).iterator();
    		    	partyRole_it 		= ((java.util.Collection)valoresfix.get("452")).iterator();

    		    	quickfix.Group grupoParties = new quickfix.Group(453,448);
    		    	   		    
    		    	//message.removeGroup(grupoParties);
    		    	message.removeField(448);
    		    	message.removeField(447);
    		    	message.removeField(452);
    		    	
    		    	//Primer grupo
    		    	grupoParties.setField(new StringField(448,partyID_it.next().toString()));
    		    	grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
    		    	grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
    		    	message.addGroup(grupoParties);
    		    	
    		    	//Segundo grupo
    		    	grupoParties.setField(new StringField(448,partyID_it.next().toString()));
    		    	grupoParties.setField(new CharField(447,partyIdsource_it.next().toString().charAt(0)));				    	 
    		    	grupoParties.setField(new IntField(452,Integer.valueOf(partyRole_it.next().toString()).intValue()));
    		    	message.addGroup(grupoParties); 
    		    }
		    }
		}

        //log.info("Sending FIX message (message) : {}", message);
        //log.info("Versus              (exchange): {}", exchange.getIn().getBody().toString());
        
        if (!session.send(message)) {
        	log.info("Connection with Acceptor was lost");
            //throw new CannotSendException("Cannot send FIX message: " + message.toString());
        }

        if (callable != null) {
            Message reply = callable.call();
            exchange.getOut().getHeaders().putAll(camelMessage.getHeaders());
            exchange.getOut().setBody(reply);
        }
    }

    Session getSession(SessionID messageSessionID) {
        return Session.lookupSession(messageSessionID);
    }
}
