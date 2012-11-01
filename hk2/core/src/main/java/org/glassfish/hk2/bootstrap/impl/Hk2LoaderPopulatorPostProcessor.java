/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.hk2.bootstrap.impl;

import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.bootstrap.PopulatorPostProcessor;
import org.glassfish.hk2.utilities.DescriptorImpl;

/**
 * A Simple PopulatorPostProcessor that uses the given classloader to override default
 * HK2Loader behavior 
 * 
 * @author mtaube
 *
 */
public class Hk2LoaderPopulatorPostProcessor implements PopulatorPostProcessor {

	private final ClassLoader classLoader;
	
	/**
	 * Creates the post-processor to use the given classloader
	 * 
	 * @param classLoader The classloader to use, may not be null
	 */
	public Hk2LoaderPopulatorPostProcessor(ClassLoader classLoader) {
		if (classLoader != null) {
           this.classLoader = classLoader;
		} else {
			this.classLoader = getClass().getClassLoader();
		}
    }

	public Hk2LoaderPopulatorPostProcessor() {
		this(null);
	}
	
	/**
	 * Uses the given classloader to load the class from the descriptor
	 */
	@Override
	public DescriptorImpl process(ServiceLocator serviceLocator, DescriptorImpl descriptorImpl) {
		descriptorImpl.setLoader(new HK2Loader() {
			
			@Override
			public Class<?> loadClass(String className) throws MultiException {
				try {
					return classLoader.loadClass(className);
				} catch (ClassNotFoundException e) {
					throw new MultiException(e);
				}
		    }
		});
		
		return descriptorImpl;
	}	

}
