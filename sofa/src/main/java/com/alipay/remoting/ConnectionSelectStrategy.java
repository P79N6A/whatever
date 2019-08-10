package com.alipay.remoting;

import java.util.List;

public interface ConnectionSelectStrategy {

    Connection select(List<Connection> connections);

}