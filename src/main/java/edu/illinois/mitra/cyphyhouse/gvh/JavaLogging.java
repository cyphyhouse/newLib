package edu.illinois.mitra.cyphyhouse.gvh;

import org.apache.log4j.Logger;

/**
 * Created by SC on 11/11/16.
 */
public class JavaLogging extends Logging{
    static Logger logger = Logger.getLogger("JLog");

    @Override
    public void e(String tag, String msg) {
        logger.error(tag+":"+msg);
    }

    @Override
    public void i(String tag, String msg) {
        logger.info(tag+":"+msg);
    }

    @Override
    public void d(String tag, String msg) {
        logger.debug(tag+":"+msg);
    }

    @Override
    public String getLog() {
        return null;
    }
}
