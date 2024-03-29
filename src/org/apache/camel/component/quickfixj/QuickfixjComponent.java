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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.StartupListener;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.UriEndpointComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.LogFactory;
//import quickfix.MessageFactory;
//import quickfix.fix50sp2.MessageFactory;
import quickfix.fixt11.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;

public class QuickfixjComponent extends UriEndpointComponent implements StartupListener {
    private static final Logger LOG = LoggerFactory.getLogger(QuickfixjComponent.class);
    private static final String PARAMETER_LAZY_CREATE_ENGINE = "lazyCreateEngine";

    private final Object engineInstancesLock = new Object();
    private final Map<String, QuickfixjEngine> engines = new HashMap<String, QuickfixjEngine>();
    private final Map<String, QuickfixjEngine> provisionalEngines = new HashMap<String, QuickfixjEngine>();
    private final Map<String, QuickfixjEndpoint> endpoints = new HashMap<String, QuickfixjEndpoint>();

    private MessageStoreFactory messageStoreFactory;
    private LogFactory logFactory;
    private MessageFactory messageFactory;
    private Map<String, QuickfixjConfiguration> configurations = new HashMap<String, QuickfixjConfiguration>();
    private boolean lazyCreateEngines;

    
    public QuickfixjComponent() {
        super(QuickfixjEndpoint.class);
    }

    
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        // Look up the engine instance based on the settings file ("remaining")
        QuickfixjEngine engine;
        synchronized (engineInstancesLock) {
            QuickfixjEndpoint endpoint = endpoints.get(uri);
            if (endpoint == null) {
                engine = engines.get(remaining);
                if (engine == null) {
                    engine = provisionalEngines.get(remaining);
                }
                if (engine == null) {
                    QuickfixjConfiguration configuration = configurations.get(remaining);
                    SessionSettings settings = null;
                    if (configuration != null) {
                        settings = configuration.createSessionSettings();
                    } else {
                        settings = QuickfixjEngine.loadSettings(remaining, null, null);
                    }
                    Boolean lazyCreateEngineForEndpoint = super.getAndRemoveParameter(parameters, PARAMETER_LAZY_CREATE_ENGINE, Boolean.TYPE);
                    if (lazyCreateEngineForEndpoint == null) {
                        lazyCreateEngineForEndpoint = isLazyCreateEngines();
                    }
                    engine = new QuickfixjEngine(uri, settings, messageStoreFactory, logFactory, messageFactory,
                            lazyCreateEngineForEndpoint);

                    // only start engine if CamelContext is already started, otherwise the engines gets started
                    // automatic later when CamelContext has been started using the StartupListener
                    if (getCamelContext().getStatus().isStarted()) {
                        startQuickfixjEngine(engine);
                        engines.put(remaining, engine);
                    } else {
                        // engines to be started later
                        provisionalEngines.put(remaining, engine);
                    }
                }

                endpoint = new QuickfixjEndpoint(engine, uri, this);
                endpoint.setConfigurationName(remaining);
                engine.addEventListener(endpoint);
                endpoints.put(uri, endpoint);
            }

            return endpoint;
        }
    }
	
    
    public QuickfixjEndpoint createEndpoint1(String uri, String remaining, Map<String, Object> parameters, String qfjSettingsFile, String sonicjSettingsFile) throws Exception {
        // Look up the engine instance based on the settings file ("remaining")
        QuickfixjEngine engine;
        synchronized (engineInstancesLock) {
            QuickfixjEndpoint endpoint = endpoints.get(uri);
            if (endpoint == null) {
                engine = engines.get(remaining);
                if (engine == null) {
                    engine = provisionalEngines.get(remaining);
                }
                if (engine == null) {
                    QuickfixjConfiguration configuration = configurations.get(remaining);
                    SessionSettings settings = null;
                    if (configuration != null) {
                        settings = configuration.createSessionSettings();
                    } else {
                        settings = QuickfixjEngine.loadSettings(remaining, qfjSettingsFile, sonicjSettingsFile);
                    }
                    Boolean lazyCreateEngineForEndpoint = super.getAndRemoveParameter(parameters, PARAMETER_LAZY_CREATE_ENGINE, Boolean.TYPE);
                    if (lazyCreateEngineForEndpoint == null) {
                        lazyCreateEngineForEndpoint = isLazyCreateEngines();
                    }
                    engine = new QuickfixjEngine(uri, settings, messageStoreFactory, logFactory, messageFactory,
                            lazyCreateEngineForEndpoint);

                    // only start engine if CamelContext is already started, otherwise the engines gets started
                    // automatic later when CamelContext has been started using the StartupListener
                    if (getCamelContext().getStatus().isStarted()) {
                        startQuickfixjEngine(engine);
                        engines.put(remaining, engine);
                    } else {
                        // engines to be started later
                        provisionalEngines.put(remaining, engine);
                    }
                }

                endpoint = new QuickfixjEndpoint(engine, uri, this);
                endpoint.setConfigurationName(remaining);
                engine.addEventListener(endpoint);
                endpoints.put(uri, endpoint);
            }

            return endpoint;
        }
    }
    
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // we defer starting quickfix engines till the onCamelContextStarted callback
    }

    @Override
    protected void doStop() throws Exception {
        // stop engines when stopping component
        synchronized (engineInstancesLock) {
            for (QuickfixjEngine engine : engines.values()) {
                engine.stop();
            }
        }
        super.doStop();
    }

    @Override
    protected void doShutdown() throws Exception {
        // cleanup when shutting down
        engines.clear();
        provisionalEngines.clear();
        endpoints.clear();
        super.doShutdown();
    }

    private void startQuickfixjEngine(QuickfixjEngine engine) throws Exception {
        if (!engine.isLazy()) {
            LOG.info("Starting QuickFIX/J engine: {}", engine.getUri());
            engine.start();
        } else {
            LOG.info("QuickFIX/J engine: {} will start lazily", engine.getUri());
        }
    }

    // Test Support
    public Map<String, QuickfixjEngine> getEngines() {
        return Collections.unmodifiableMap(engines);
    }

    // Test Support
    Map<String, QuickfixjEngine> getProvisionalEngines() {
        return Collections.unmodifiableMap(provisionalEngines);
    }

    public void setMessageFactory(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    public void setLogFactory(LogFactory logFactory) {
        this.logFactory = logFactory;
    }

    public void setMessageStoreFactory(MessageStoreFactory messageStoreFactory) {
        this.messageStoreFactory = messageStoreFactory;
    }

    /**
     * @deprecated Don't use as setting the {@code forcedShutdown} property had/has no effect.
     */
    @Deprecated
    public void setForcedShutdown(boolean forcedShutdown) {
    }

    public Map<String, QuickfixjConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, QuickfixjConfiguration> configurations) {
        this.configurations = configurations;
    }

    public boolean isLazyCreateEngines() {
        return this.lazyCreateEngines;
    }

    /**
     * If set to <code>true</code>, the engines will be created and started when needed (when first message
     * is send)
     *
     * @param lazyCreateEngines
     */
    public void setLazyCreateEngines(boolean lazyCreateEngines) {
        this.lazyCreateEngines = lazyCreateEngines;
    }

    @Override
    public void onCamelContextStarted(CamelContext camelContext, boolean alreadyStarted) throws Exception {
        // only start quickfix engines when CamelContext have finished starting
        synchronized (engineInstancesLock) {
            for (QuickfixjEngine engine : engines.values()) {
                startQuickfixjEngine(engine);
            }
            for (Map.Entry<String, QuickfixjEngine> entry : provisionalEngines.entrySet()) {
                startQuickfixjEngine(entry.getValue());
                engines.put(entry.getKey(), entry.getValue());
            }
            provisionalEngines.clear();
        }
    }
}
