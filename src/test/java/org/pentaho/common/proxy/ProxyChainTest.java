package org.pentaho.common.proxy;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProxyChainTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testCtor1() throws Throwable {
    ProxyChain pc = new ProxyChain( Cloneable.class ) {

      @Override
      public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {

        return "testCtor1";
      }
    };

    assertEquals( pc.getInterface(), Cloneable.class );
    assertNull( pc.getObject() );
    assertEquals( pc.invoke( null, null, null ), "testCtor1" );
  }

  @Test
  public void testUse1() {
    ProxyChain pc = new ProxyChain( Comparable.class );

    @SuppressWarnings( "unchecked" )
    Comparable<String> s = (Comparable<String>) pc.use( "Hello world" );
    assertTrue( "Proxy doesn't delegate to normal object", s.equals( "Hello world" ) );
  }

  @Test
  public void testWhenReturns1() {
    ProxyChain pc1 = new ProxyChain( Comparable.class );
    ProxyChain pc2 = pc1.whenReturns( Comparable.class );
    assertFalse( pc1.equals(pc2) || pc1 == pc2 );
  }

  @Test
  public void testDriver() {
    ProxyChain pc = new ProxyChain(Driver.class);
    assertNotNull(pc);
    ProxyChain c = pc.whenReturns(Connection.class);
    assertNotNull(c);
    ProxyChain s = c.whenMethodThrows(SQLException.class);
    assertNotNull(s);
    
  }
}
