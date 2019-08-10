package com.alipay.remoting.rpc;

import com.alipay.remoting.ResponseStatus;
import com.alipay.remoting.exception.*;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.exception.*;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.alipay.remoting.util.StringUtils;
import org.slf4j.Logger;

public class RpcResponseResolver {
    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    public static Object resolveResponseObject(ResponseCommand responseCommand, String addr) throws RemotingException {
        // 处理异常
        preProcess(responseCommand, addr);
        if (responseCommand.getResponseStatus() == ResponseStatus.SUCCESS) {
            // 反序列化
            return toResponseObject(responseCommand);
        } else {
            String msg = String.format("Rpc invocation exception: %s, the address is %s, id=%s", responseCommand.getResponseStatus(), addr, responseCommand.getId());
            logger.warn(msg);
            if (responseCommand.getCause() != null) {
                throw new InvokeException(msg, responseCommand.getCause());
            } else {
                throw new InvokeException(msg + ", please check the server log for more.");
            }
        }

    }

    private static void preProcess(ResponseCommand responseCommand, String addr) throws RemotingException {
        RemotingException e = null;
        String msg = null;
        if (responseCommand == null) {
            msg = String.format("Rpc invocation timeout[responseCommand null]! the address is %s", addr);
            e = new InvokeTimeoutException(msg);
        } else {
            switch (responseCommand.getResponseStatus()) {
                case TIMEOUT:
                    msg = String.format("Rpc invocation timeout[responseCommand TIMEOUT]! the address is %s", addr);
                    e = new InvokeTimeoutException(msg);
                    break;
                case CLIENT_SEND_ERROR:
                    msg = String.format("Rpc invocation send failed! the address is %s", addr);
                    e = new InvokeSendFailedException(msg, responseCommand.getCause());
                    break;
                case CONNECTION_CLOSED:
                    msg = String.format("Connection closed! the address is %s", addr);
                    e = new ConnectionClosedException(msg);
                    break;
                case SERVER_THREADPOOL_BUSY:
                    msg = String.format("Server thread pool busy! the address is %s, id=%s", addr, responseCommand.getId());
                    e = new InvokeServerBusyException(msg);
                    break;
                case CODEC_EXCEPTION:
                    msg = String.format("Codec exception! the address is %s, id=%s", addr, responseCommand.getId());
                    e = new CodecException(msg);
                    break;
                case SERVER_SERIAL_EXCEPTION:
                    msg = String.format("Server serialize response exception! the address is %s, id=%s, serverSide=true", addr, responseCommand.getId());
                    e = new SerializationException(detailErrMsg(msg, responseCommand), toThrowable(responseCommand), true);
                    break;
                case SERVER_DESERIAL_EXCEPTION:
                    msg = String.format("Server deserialize request exception! the address is %s, id=%s, serverSide=true", addr, responseCommand.getId());
                    e = new DeserializationException(detailErrMsg(msg, responseCommand), toThrowable(responseCommand), true);
                    break;
                case SERVER_EXCEPTION:
                    msg = String.format("Server exception! Please check the server log, the address is %s, id=%s", addr, responseCommand.getId());
                    e = new InvokeServerException(detailErrMsg(msg, responseCommand), toThrowable(responseCommand));
                    break;
                default:
                    break;
            }
        }
        if (StringUtils.isNotBlank(msg)) {
            logger.warn(msg);
        }
        if (null != e) {
            throw e;
        }
    }

    private static Object toResponseObject(ResponseCommand responseCommand) throws CodecException {
        RpcResponseCommand response = (RpcResponseCommand) responseCommand;
        response.deserialize();
        return response.getResponseObject();
    }

    private static Throwable toThrowable(ResponseCommand responseCommand) throws CodecException {
        RpcResponseCommand resp = (RpcResponseCommand) responseCommand;
        resp.deserialize();
        Object ex = resp.getResponseObject();
        if (ex != null && ex instanceof Throwable) {
            return (Throwable) ex;
        }
        return null;
    }

    private static String detailErrMsg(String clientErrMsg, ResponseCommand responseCommand) {
        RpcResponseCommand resp = (RpcResponseCommand) responseCommand;
        if (StringUtils.isNotBlank(resp.getErrorMsg())) {
            return String.format("%s, ServerErrorMsg:%s", clientErrMsg, resp.getErrorMsg());
        } else {
            return String.format("%s, ServerErrorMsg:null", clientErrMsg);
        }
    }

}
