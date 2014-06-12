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
import android.content.res.Resources;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.drools.android.InjectKnowledgeBase;
import org.drools.core.util.DroolsStreamUtils;
import org.kie.api.KieBase;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.definition.KnowledgePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.inject.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marek Kedzierski
 */
public class KnowledgeBaseListener implements TypeListener {
   private static final Logger logger = LoggerFactory.getLogger("KnowledgeBaseListener");

   protected Application application;

   public KnowledgeBaseListener(Application application) {
      this.application = application;
   }

   public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {

      for( Class<?> c = typeLiteral.getRawType(); c!=Object.class; c = c.getSuperclass() )
         for (Field field : c.getDeclaredFields())
            if ( field.isAnnotationPresent(InjectKnowledgeBase.class) && !Modifier.isStatic(field.getModifiers()) )
               typeEncounter.register(new KnowledgeBaseMembersInjector<I>(field, application, field.getAnnotation(InjectKnowledgeBase.class)));

   }

   protected static class KnowledgeBaseMembersInjector<T> implements MembersInjector<T> {

      protected Field field;
      protected Application application;
      protected InjectKnowledgeBase annotation;

      public KnowledgeBaseMembersInjector(Field field, Application application, InjectKnowledgeBase annotation) {
         this.field = field;
         this.application = application;
         this.annotation = annotation;
      }

      public void injectMembers(T instance) {

         Object value = null;

         try {

            final Resources resources = application.getResources();
            final int []ids = getId(resources,annotation);

            final Class<?> t = field.getType();

            List<KnowledgePackage> pkgs = new ArrayList<KnowledgePackage>();
            for(int id : ids) {
               pkgs.add((KnowledgePackage) DroolsStreamUtils.streamIn(resources.openRawResource(id)));
            }
            KnowledgeBase knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
            knowledgeBase.addKnowledgePackages(pkgs);

            if (KnowledgeBase.class.isAssignableFrom(t)) {
               value = knowledgeBase;
            } else if (KieBase.class.isAssignableFrom(t)) {
               value = knowledgeBase;
            }

            if (value == null && Nullable.notNullable(field) ) {
               throw new NullPointerException(String.format("Can't inject null value into %s.%s when field is not @Nullable", field.getDeclaringClass(), field
                     .getName()));
            }

            field.setAccessible(true);
            field.set(instance, value);
            logger.info("Injected Knowledge Base: " + value);
         } catch (IllegalArgumentException f) {
            throw new IllegalArgumentException(String.format("Can't assign %s value %s to %s field %s", value != null ? value.getClass() : "(null)", value,
                  field.getType(), field.getName()));
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }

      protected int[] getId(Resources resources, InjectKnowledgeBase annotation) {
         int []id = annotation.value();
         if(id[0]>=0) return id;
         id = new int[annotation.names().length];
         for(int i=0; i<annotation.names().length; i++) {
            id[i] = resources.getIdentifier(annotation.names()[i],null,application.getPackageName());
         }
         return id;
      }
   }
}
