package org.springframework.http.codec.support;

import org.springframework.http.codec.ServerCodecConfigurer;

public class DefaultServerCodecConfigurer extends BaseCodecConfigurer implements ServerCodecConfigurer {

    public DefaultServerCodecConfigurer() {
        super(new ServerDefaultCodecsImpl());
    }

    @Override
    public ServerDefaultCodecs defaultCodecs() {
        return (ServerDefaultCodecs) super.defaultCodecs();
    }

}
