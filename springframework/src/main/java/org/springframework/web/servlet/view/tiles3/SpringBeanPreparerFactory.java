package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;
import org.springframework.web.context.WebApplicationContext;

public class SpringBeanPreparerFactory extends AbstractSpringPreparerFactory {

    @Override
    protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
        return context.getBean(name, ViewPreparer.class);
    }

}
