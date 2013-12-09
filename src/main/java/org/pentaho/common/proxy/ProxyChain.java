package org.pentaho.common.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProxyChain implements InvocationHandler {
  Class<?> proxiedInterface;
  Object proxiedObject;
  Object rootProxy;

  ConcurrentMap<Class<?>, ProxyChain> returnProxyMap = new ConcurrentHashMap<Class<?>, ProxyChain>();
  ConcurrentMap<Method, ProxyChain> returnNullProxyMap = new ConcurrentHashMap<Method, ProxyChain>();
  ConcurrentMap<MethodThrows, ProxyChain> methodThrowsProxyMap = new ConcurrentHashMap<MethodThrows, ProxyChain>();
  ConcurrentMap<Method, ProxyChain> calledMethodMap = new ConcurrentHashMap<Method, ProxyChain>();
  ConcurrentMap<Method, Object> methodDelegateMap = new ConcurrentHashMap<Method, Object>();

  public <T> ProxyChain( Class<T> intf ) {
    proxiedInterface = intf;
  }

  public Object use( Object obj ) {
    proxiedObject = obj;
    rootProxy = Proxy.newProxyInstance( obj.getClass().getClassLoader(), new Class[] { proxiedInterface }, this );

    return rootProxy;
  }

  public Class<?> getInterface() {
    return proxiedInterface;
  }

  public Object getObject() {
    return proxiedObject;
  }

  @Override
  public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {

    Object result = null;

    // First, check the method to see if we need to delegate
    ProxyChain pc = calledMethodMap.get( method );
    if ( pc == null ) {
      // Check for a specific delegate
      Object delegate = methodDelegateMap.get( method );
      if ( delegate == null ) {
        // Check for a general delegate
        delegate = methodDelegateMap.get( getAllMethodsPlaceholder() );
      }

      // If we have a delegate, invoke the method on it
      if ( delegate != null ) {
        Method delegateMethod = delegate.getClass().getMethod( method.getName(), method.getParameterTypes() );
        return delegateMethod.invoke( proxiedObject, args );
      }

      // Otherwise call the real method
      try {
        result = method.invoke( proxiedObject, args );
      } catch ( Throwable t ) {
        if(t instanceof InvocationTargetException) {
          Throwable cause = t.getCause();
          // Check throws delegates
          ProxyChain mt = methodThrowsProxyMap.get( new MethodThrows( method, cause.getClass() ) );
          if ( mt != null ) {
            return mt.invoke( proxy, method, args );
          }
        }
      }
    } else {
      
      // Delegate to the specified proxy chain
      pc.invoke( proxy, method, args );
    }

    return result;
  }

  public ProxyChain whenReturns( Class<?> intf ) {
    ProxyChain pc = new ProxyChain( intf );
    ProxyChain existing = returnProxyMap.putIfAbsent( intf, pc );
    System.out.println("Exists = "+(pc == existing));
    return (existing == null) ? pc : existing;
  }

  public ProxyChain whenReturnsNull( Method meth ) {
    returnNullProxyMap.putIfAbsent( meth, new ProxyChain( meth.getReturnType() ) );
    return returnNullProxyMap.get( meth );
  }

  public ProxyChain whenMethodCalled( Method meth ) {
    calledMethodMap.putIfAbsent( meth, new ProxyChain( meth.getReturnType() ) );
    return calledMethodMap.get( meth );
  }

  public ProxyChain whenMethodThrows( Method meth, Class<? extends Throwable> thrown ) {
    MethodThrows mt = new MethodThrows( meth, thrown );
    methodThrowsProxyMap.putIfAbsent( mt, new ProxyChain( meth.getReturnType() ) );
    return methodThrowsProxyMap.get( mt );
  }
  
  public ProxyChain whenMethodThrows( Class<? extends Throwable> thrown ) {
    return whenMethodThrows( getAllMethodsPlaceholder(), thrown );
  }

  protected class MethodThrows {
    Method method;
    Class<? extends Throwable> throwable;

    public MethodThrows( Method m, Class<? extends Throwable> thrown ) {
      method = m;
      throwable = thrown;
    }

    public Method getMethod() {
      return method;
    }

    public Class<? extends Throwable> getThrowable() {
      return throwable;
    }
  }

  public void delegateTo( Object m ) {
    try {
      // If this proxy chain is for a method
      if ( Method.class.isAssignableFrom( this.getObject().getClass() ) ) {
        methodDelegateMap.put( (Method) ( this.getObject() ), m );
      } else {
        methodDelegateMap.put( getAllMethodsPlaceholder(), m );
      }
    } catch ( Exception e ) {
      // TODO
      e.printStackTrace( System.err );
    }
  }

  // This is a placeholder method so we know that we need to delegate all invoked methods to the
  // specified delegate.
  @SuppressWarnings( "unused" )
  private static void allMethodsPlaceholder() {
  }
  
  private static Method getAllMethodsPlaceholder() {
    Method m = null;
    try {
      m =  ProxyChain.class.getDeclaredMethod( "allMethodsPlaceholder" );
    }
    catch( Exception e ) {
      // Something very very bad happened
      System.exit( 1 );
    }
    return m;
  }

}
