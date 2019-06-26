package org.apache.ibatis.scripting.xmltags;

import ognl.DefaultClassResolver;
import org.apache.ibatis.io.Resources;

public class OgnlClassResolver extends DefaultClassResolver {

    @Override
    protected Class toClassForName(String className) throws ClassNotFoundException {
        return Resources.classForName(className);
    }

}
