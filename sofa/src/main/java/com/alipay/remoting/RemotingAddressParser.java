package com.alipay.remoting;

public interface RemotingAddressParser {

    Url parse(String url);

    String parseUniqueKey(String url);

    String parseProperty(String url, String propKey);

    void initUrlArgs(Url url);

    char COLON = ':';

    char EQUAL = '=';

    char AND = '&';

    char QUES = '?';

}
