/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DME2UnitTestUtil {
  /**
   * Sets a private, static and/or final variable
   * ONLY USE THIS WHEN NO OTHER OPTION EXISTS!
   *
   * @param field Field to modify (via MyClass.class.getDeclaredField("field"))
   * @param newValue New value
   * @throws Exception
   */
  public static void setFinalStatic(Field field, Object newValue) throws Exception {
    field.setAccessible(true);
    // remove final modifier from field
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newValue);
  }

  public static Object getPrivate(Field field, Object instance) throws Exception {
    field.setAccessible(true);
/*
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
*/
    // modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    return field.get(instance);
  }

  public static void executePrivateVoid( Method method, Object instance, Object ... args )
      throws InvocationTargetException, IllegalAccessException {
    method.setAccessible( true );
    method.invoke( instance, args );
  }
}
