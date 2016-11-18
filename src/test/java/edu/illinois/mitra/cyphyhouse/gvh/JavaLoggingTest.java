package edu.illinois.mitra.cyphyhouse.gvh;

import org.junit.Test;
import sun.rmi.runtime.Log;

import static org.junit.Assert.*;

/**
 * Created by SC on 11/18/16.
 */
public class JavaLoggingTest {
    Logging log = new JavaLogging();
    String TAG = "JavaLoggingTest";
    @Test
    public void e() throws Exception {
        log.e(TAG, "sample error");
    }

    @Test
    public void i() throws Exception {
        log.i(TAG, "sample info");
    }

    @Test
    public void d() throws Exception {
        log.d(TAG, "sample debug");
    }

}