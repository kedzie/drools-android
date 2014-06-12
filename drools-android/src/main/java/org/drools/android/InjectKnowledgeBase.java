/*
 * Copyright (C) 2014 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

/*
 * Created by IntelliJ IDEA.
 * User: kedzie
 * Date: 12/23/14
 * Time: 6:00 PM
 */
package org.drools.android;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a variable member of a class (whether static or not) should be
 * injected with an Android resource.
 *
 * The value corresponds to the id of the resource.<br />
 *
 * You may specify the name of the resource instead of the id using {@link #names()},
 * which will use {@link android.content.res.Resources#getIdentifier(String, String, String)} to
 * resolve the resource by name.
 *
 * Usage example:<br />
 * {@code @InjectKnowledgeBase(R.raw.hello) protected KieBase helloKB;} <br/>
 * {@code @InjectKnowledgeBase(name="com.myapp:raw/hello") protected KieBase helloKB;}
 *
 * @author Marek Kedzierski
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@BindingAnnotation
public @interface InjectKnowledgeBase {
   int[] value() default { -1 };
   String[] names() default { "" };
}
