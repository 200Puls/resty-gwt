package org.fusesource.restygwt;


import junit.framework.Test;
import junit.framework.TestCase;

import org.fusesource.restygwt.client.BasicTestGwt;
import org.fusesource.restygwt.client.flaky.FlakyTestGwt;

import com.google.gwt.junit.tools.GWTTestSuite;


/**
 *
 * <p>
 * <ul>
 * <li>Add GWTTestCases here</li>
 * <li>See also <a href="http://mojo.codehaus.org/gwt-maven-plugin/user-guide/testing.html">maven docu</a></li>
 * </ul>
 *
 * IMPORTANT: Naming convention
 * <ul>
 * <li>Fast Junit Tests: end with "Test". Correct example: MyTest.java</li>
 * <li>GWT Tests: do NOT end with "Test". Do NOT start with GwtTest. Correct example: DateBeautifierTestGwt.java</li>
 * </ul>
 * </p>
 *
 * @author <a href="mailto:mail@raphaelbauer.com">rEyez</<a>
 */
public class GwtCompleteTestSuite extends TestCase {


	/**
	 * @return the suite of that module
	 */
    public static Test suite() {
        GWTTestSuite suite = new GWTTestSuite("all GwtTestCases" );

        suite.addTestSuite(BasicTestGwt.class);
        suite.addTestSuite(FlakyTestGwt.class);


        return suite;
    }
}
