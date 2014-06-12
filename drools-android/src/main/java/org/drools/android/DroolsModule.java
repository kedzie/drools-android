/*
 * Copyright (C) 2014 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package org.drools.android;

import android.app.Application;
import android.content.Context;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Roboguice module
 * @author kedzie
 */
public class DroolsModule extends AbstractModule {
   private static final Logger logger = LoggerFactory.getLogger("DroolsModule");

   private Application application;

   public DroolsModule(Context ctx) {
      this.application = (Application)ctx;
   }

   @Override
   protected void configure() {
      final org.drools.android.KnowledgeBaseListener kbListener = new org.drools.android.KnowledgeBaseListener(application);
      bind(org.drools.android.KnowledgeBaseListener.class).toInstance(kbListener);
      bindListener(Matchers.any(), kbListener);
   }
}
