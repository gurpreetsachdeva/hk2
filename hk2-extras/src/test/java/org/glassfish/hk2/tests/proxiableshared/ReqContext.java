/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.hk2.tests.proxiableshared;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Context;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extras.operation.OperationHandle;
import org.glassfish.hk2.extras.operation.OperationManager;

/**
 * Request context to manage request scoped components.
 * A single threaded client is assumed for the sake of simplicity.
  *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Singleton
public class ReqContext implements Context<ReqScoped> {
    @Inject
    private OperationManager parentOperationManager;
    
    @Inject
    private ServiceLocator whoAmIFor;
    
    private OperationHandle<ReqScoped> currentRequest;

    private Map<ActiveDescriptor<?>, Object> context = null;

    /**
     * Make room for new request scoped data.
     */
    public void startRequest() {
        if (currentRequest != null) {
            currentRequest.closeOperation();
            currentRequest = null;
        }
        
        currentRequest = parentOperationManager.createAndStartOperation(ReqScopedImpl.REQ_SCOPED);
        currentRequest.setOperationData(whoAmIFor);
        
        context = new HashMap<ActiveDescriptor<?>, Object>();
    }

    /**
     * Forget all request data.
     */
    public void stopRequest() {
        this.context = null;
        if (currentRequest != null) {
            currentRequest.closeOperation();
            currentRequest = null;
        }
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ReqScoped.class;
    }

    @Override
    public <U> U findOrCreate(ActiveDescriptor<U> activeDescriptor, ServiceHandle<?> root) {

        ensureActiveRequest();

        Object result = context.get(activeDescriptor);
        if (result == null) {
            result = activeDescriptor.create(root);
            context.put(activeDescriptor, result);
        }
        return (U) result;
    }

    private void ensureActiveRequest() {
        if (context == null) {
            throw new IllegalStateException("Not inside an active request scope");
        }
    }

    @Override
    public boolean containsKey(ActiveDescriptor<?> descriptor) {
        ensureActiveRequest();
        return context.containsKey(descriptor);
    }

    @Override
    public void destroyOne(ActiveDescriptor<?> descriptor) {
        ensureActiveRequest();
        context.remove(descriptor);
    }

    @Override
    public boolean supportsNullCreation() {
        return false;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void shutdown() {
        context.clear();
    }
}
