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

import java.util.Properties;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

//import quickfix.fix50sp2.MessageFactory;
import quickfix.fixt11.MessageFactory;
import quickfix.FixVersions;
import quickfix.Message;
//import quickfix.MessageFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.field.MsgType;
import quickfix.fix50sp2.NewOrderSingle;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

public class QuickfixjSpring extends CamelSpringTestSupport {

    public void setUp() throws Exception {
        //if (isJava16()) {
            // cannot test on java 1.6
        //    return;
        //}
        super.setUp();
    }

    @Test
    public void configureInSpring() throws Exception {
        //if (isJava16()) {
            // cannot test on java 1.6
        //    return;
        //}

        SessionID sessionID = new SessionID("FIX.4.4:ARKA1->EXECUTOR");
        QuickfixjConfiguration configuration = context.getRegistry().lookupByNameAndType("quickfixjConfiguration", QuickfixjConfiguration.class);

        SessionSettings springSessionSettings = configuration.createSessionSettings();
        Properties sessionProperties = springSessionSettings.getSessionProperties(sessionID, true);

        String a = sessionProperties.get("ConnectionType").toString();
        //= CoreMatchers.is("initiator")
        String b = sessionProperties.get("SocketConnectProtocol").toString();
        //CoreMatchers.is("VM_PIPE"));

        QuickfixjComponent component = context.getComponent("quickfix", QuickfixjComponent.class);

        

        boolean c = component.isLazyCreateEngines();
        //is(false));
        QuickfixjEngine engine = component.getEngines().values().iterator().next();
        boolean d = engine.isInitialized();
        //is(true));

        QuickfixjComponent lazyComponent = context.getComponent("lazyQuickfix", QuickfixjComponent.class);
        boolean e = lazyComponent.isLazyCreateEngines();
        //is(true));
        
        QuickfixjEngine lazyEngine = lazyComponent.getEngines().values().iterator().next();
        boolean f = lazyEngine.isInitialized();
        //is(false));
        
        MessageFactory g = (MessageFactory) engine.getMessageFactory();
        //is(instanceOf(CustomMessageFactory.class)));
        
    }

    /**
     * Customer message factory and message class for test purposes
     */
    public static class CustomMessageFactory extends MessageFactory {
        @Override
        public Message create(String beginString, String msgType) {
        	
            if (beginString.equals(FixVersions.BEGINSTRING_FIXT11) && msgType.equals(MsgType.ORDER_SINGLE)) {
                return new CustomNewOrderSingle();
            }
            return super.create(beginString, msgType);
        }
    }

    public static class CustomNewOrderSingle extends NewOrderSingle {
        private static final long serialVersionUID = 1L;
    }

	@Override
	protected AbstractApplicationContext createApplicationContext() {
		// TODO Auto-generated method stub
		return null;
	}
}
