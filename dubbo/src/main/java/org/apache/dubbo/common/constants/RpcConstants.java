package org.apache.dubbo.common.constants;

public interface RpcConstants {

    String HESSIAN2_REQUEST_KEY = "hessian2.request";

    boolean DEFAULT_HESSIAN2_REQUEST = false;

    String HESSIAN_OVERLOAD_METHOD_KEY = "hessian.overload.method";

    boolean DEFAULT_HESSIAN_OVERLOAD_METHOD = false;

    String DEFAULT_HTTP_CLIENT = "jdk";

    String DEFAULT_HTTP_SERVER = "servlet";

    String DEFAULT_HTTP_SERIALIZATION = "json";

    String SHARE_CONNECTIONS_KEY = "shareconnections";

    String DEFAULT_SHARE_CONNECTIONS = "1";

    String INPUT_KEY = "input";

    String OUTPUT_KEY = "output";

    String DECODE_IN_IO_THREAD_KEY = "decode.in.io";

    boolean DEFAULT_DECODE_IN_IO_THREAD = true;

    String CALLBACK_SERVICE_KEY = "callback.service.instid";

    String CALLBACK_INSTANCES_LIMIT_KEY = "callbacks";

    int DEFAULT_CALLBACK_INSTANCES = 1;

    String CALLBACK_SERVICE_PROXY_KEY = "callback.service.proxy";

    String IS_CALLBACK_SERVICE = "is_callback_service";

    String CHANNEL_CALLBACK_KEY = "channel.callback.invokers.key";

    String LAZY_CONNECT_INITIAL_STATE_KEY = "connect.lazy.initial.state";

    boolean DEFAULT_LAZY_CONNECT_INITIAL_STATE = true;

    String OPTIMIZER_KEY = "optimizer";

    String DUBBO_VERSION_KEY = "dubbo";

    String LOCAL_KEY = "local";

    String STUB_KEY = "stub";

    String MOCK_KEY = "mock";

    String DEPRECATED_KEY = "deprecated";

    String $INVOKE = "$invoke";

    String $ECHO = "$echo";

    String RETURN_PREFIX = "return ";

    String THROW_PREFIX = "throw";

    String FAIL_PREFIX = "fail:";

    String FORCE_PREFIX = "force:";

    String MERGER_KEY = "merger";

    String IS_SERVER_KEY = "isserver";

    String FORCE_USE_TAG = "dubbo.force.tag";

    String GENERIC_SERIALIZATION_NATIVE_JAVA = "nativejava";

    String GENERIC_SERIALIZATION_DEFAULT = "true";

    String GENERIC_SERIALIZATION_BEAN = "bean";

    String GENERIC_SERIALIZATION_PROTOBUF = "protobuf-json";

    String TPS_LIMIT_RATE_KEY = "tps";

    String TPS_LIMIT_INTERVAL_KEY = "tps.interval";

    long DEFAULT_TPS_LIMIT_INTERVAL = 60 * 1000;

    String AUTO_ATTACH_INVOCATIONID_KEY = "invocationid.autoattach";

    String STUB_EVENT_KEY = "dubbo.stub.event";

    boolean DEFAULT_STUB_EVENT = false;

    String STUB_EVENT_METHODS_KEY = "dubbo.stub.event.methods";

    String PROXY_KEY = "proxy";

    String EXECUTES_KEY = "executes";

    String REFERENCE_FILTER_KEY = "reference.filter";

    String INVOKER_LISTENER_KEY = "invoker.listener";

    String SERVICE_FILTER_KEY = "service.filter";

    String EXPORTER_LISTENER_KEY = "exporter.listener";

    String ACCESS_LOG_KEY = "accesslog";

    String ACTIVES_KEY = "actives";

    String CONNECTIONS_KEY = "connections";

    String ID_KEY = "id";

    String ASYNC_KEY = "async";

    String FUTURE_GENERATED_KEY = "future_generated";

    String FUTURE_RETURNTYPE_KEY = "future_returntype";

    String RETURN_KEY = "return";

    String TOKEN_KEY = "token";

    String INTERFACES = "interfaces";

    String GENERIC_KEY = "generic";

    String LOCAL_PROTOCOL = "injvm";

    String KEEP_ALIVE_KEY = "keepalive";

    boolean DEFAULT_KEEP_ALIVE = true;

    String EXTENSION_KEY = "extension";

}
