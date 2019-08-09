package org.springframework.aop.aspectj;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.bridge.IMessageHandler;

public class AspectJWeaverMessageHandler implements IMessageHandler {

    private static final String AJ_ID = "[AspectJ] ";

    private static final Log logger = LogFactory.getLog("AspectJ Weaver");

    @Override
    public boolean handleMessage(IMessage message) throws AbortException {
        Kind messageKind = message.getKind();
        if (messageKind == IMessage.DEBUG) {
            if (logger.isDebugEnabled()) {
                logger.debug(makeMessageFor(message));
                return true;
            }
        } else if (messageKind == IMessage.INFO || messageKind == IMessage.WEAVEINFO) {
            if (logger.isInfoEnabled()) {
                logger.info(makeMessageFor(message));
                return true;
            }
        } else if (messageKind == IMessage.WARNING) {
            if (logger.isWarnEnabled()) {
                logger.warn(makeMessageFor(message));
                return true;
            }
        } else if (messageKind == IMessage.ERROR) {
            if (logger.isErrorEnabled()) {
                logger.error(makeMessageFor(message));
                return true;
            }
        } else if (messageKind == IMessage.ABORT) {
            if (logger.isFatalEnabled()) {
                logger.fatal(makeMessageFor(message));
                return true;
            }
        }
        return false;
    }

    private String makeMessageFor(IMessage aMessage) {
        return AJ_ID + aMessage.getMessage();
    }

    @Override
    public boolean isIgnoring(Kind messageKind) {
        return false;
    }

    @Override
    public void dontIgnore(Kind messageKind) {
    }

    @Override
    public void ignore(Kind kind) {
    }

}
