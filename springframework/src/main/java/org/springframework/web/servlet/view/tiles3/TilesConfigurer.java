package org.springframework.web.servlet.view.tiles3;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.TilesContainer;
import org.apache.tiles.TilesException;
import org.apache.tiles.definition.DefinitionsFactory;
import org.apache.tiles.definition.DefinitionsReader;
import org.apache.tiles.definition.dao.BaseLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.dao.CachingLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles.el.ELAttributeEvaluator;
import org.apache.tiles.el.ScopeELResolver;
import org.apache.tiles.el.TilesContextBeanELResolver;
import org.apache.tiles.el.TilesContextELResolver;
import org.apache.tiles.evaluator.AttributeEvaluator;
import org.apache.tiles.evaluator.AttributeEvaluatorFactory;
import org.apache.tiles.evaluator.BasicAttributeEvaluatorFactory;
import org.apache.tiles.evaluator.impl.DirectAttributeEvaluator;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer;
import org.apache.tiles.factory.AbstractTilesContainerFactory;
import org.apache.tiles.factory.BasicTilesContainerFactory;
import org.apache.tiles.impl.mgmt.CachingTilesContainer;
import org.apache.tiles.locale.LocaleResolver;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.ApplicationContextAware;
import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.startup.DefaultTilesInitializer;
import org.apache.tiles.startup.TilesInitializer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ServletContextAware;

import javax.el.*;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspFactory;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TilesConfigurer implements ServletContextAware, InitializingBean, DisposableBean {

    private static final boolean tilesElPresent = ClassUtils.isPresent("org.apache.tiles.el.ELAttributeEvaluator", TilesConfigurer.class.getClassLoader());

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private TilesInitializer tilesInitializer;

    @Nullable
    private String[] definitions;

    private boolean checkRefresh = false;

    private boolean validateDefinitions = true;

    @Nullable
    private Class<? extends DefinitionsFactory> definitionsFactoryClass;

    @Nullable
    private Class<? extends PreparerFactory> preparerFactoryClass;

    private boolean useMutableTilesContainer = false;

    @Nullable
    private ServletContext servletContext;

    public void setTilesInitializer(TilesInitializer tilesInitializer) {
        this.tilesInitializer = tilesInitializer;
    }

    public void setCompleteAutoload(boolean completeAutoload) {
        if (completeAutoload) {
            try {
                this.tilesInitializer = new SpringCompleteAutoloadTilesInitializer();
            } catch (Throwable ex) {
                throw new IllegalStateException("Tiles-Extras 3.0 not available", ex);
            }
        } else {
            this.tilesInitializer = null;
        }
    }

    public void setDefinitions(String... definitions) {
        this.definitions = definitions;
    }

    public void setCheckRefresh(boolean checkRefresh) {
        this.checkRefresh = checkRefresh;
    }

    public void setValidateDefinitions(boolean validateDefinitions) {
        this.validateDefinitions = validateDefinitions;
    }

    public void setDefinitionsFactoryClass(Class<? extends DefinitionsFactory> definitionsFactoryClass) {
        this.definitionsFactoryClass = definitionsFactoryClass;
    }

    public void setPreparerFactoryClass(Class<? extends PreparerFactory> preparerFactoryClass) {
        this.preparerFactoryClass = preparerFactoryClass;
    }

    public void setUseMutableTilesContainer(boolean useMutableTilesContainer) {
        this.useMutableTilesContainer = useMutableTilesContainer;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void afterPropertiesSet() throws TilesException {
        Assert.state(this.servletContext != null, "No ServletContext available");
        ApplicationContext preliminaryContext = new SpringWildcardServletTilesApplicationContext(this.servletContext);
        if (this.tilesInitializer == null) {
            this.tilesInitializer = new SpringTilesInitializer();
        }
        this.tilesInitializer.initialize(preliminaryContext);
    }

    @Override
    public void destroy() throws TilesException {
        if (this.tilesInitializer != null) {
            this.tilesInitializer.destroy();
        }
    }

    private class SpringTilesInitializer extends DefaultTilesInitializer {

        @Override
        protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
            return new SpringTilesContainerFactory();
        }

    }

    private class SpringTilesContainerFactory extends BasicTilesContainerFactory {

        @Override
        protected TilesContainer createDecoratedContainer(TilesContainer originalContainer, ApplicationContext context) {
            return (useMutableTilesContainer ? new CachingTilesContainer(originalContainer) : originalContainer);
        }

        @Override
        protected List<ApplicationResource> getSources(ApplicationContext applicationContext) {
            if (definitions != null) {
                List<ApplicationResource> result = new LinkedList<>();
                for (String definition : definitions) {
                    Collection<ApplicationResource> resources = applicationContext.getResources(definition);
                    if (resources != null) {
                        result.addAll(resources);
                    }
                }
                return result;
            } else {
                return super.getSources(applicationContext);
            }
        }

        @Override
        protected BaseLocaleUrlDefinitionDAO instantiateLocaleDefinitionDao(ApplicationContext applicationContext, LocaleResolver resolver) {
            BaseLocaleUrlDefinitionDAO dao = super.instantiateLocaleDefinitionDao(applicationContext, resolver);
            if (checkRefresh && dao instanceof CachingLocaleUrlDefinitionDAO) {
                ((CachingLocaleUrlDefinitionDAO) dao).setCheckRefresh(true);
            }
            return dao;
        }

        @Override
        protected DefinitionsReader createDefinitionsReader(ApplicationContext context) {
            DigesterDefinitionsReader reader = (DigesterDefinitionsReader) super.createDefinitionsReader(context);
            reader.setValidating(validateDefinitions);
            return reader;
        }

        @Override
        protected DefinitionsFactory createDefinitionsFactory(ApplicationContext applicationContext, LocaleResolver resolver) {
            if (definitionsFactoryClass != null) {
                DefinitionsFactory factory = BeanUtils.instantiateClass(definitionsFactoryClass);
                if (factory instanceof ApplicationContextAware) {
                    ((ApplicationContextAware) factory).setApplicationContext(applicationContext);
                }
                BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(factory);
                if (bw.isWritableProperty("localeResolver")) {
                    bw.setPropertyValue("localeResolver", resolver);
                }
                if (bw.isWritableProperty("definitionDAO")) {
                    bw.setPropertyValue("definitionDAO", createLocaleDefinitionDao(applicationContext, resolver));
                }
                return factory;
            } else {
                return super.createDefinitionsFactory(applicationContext, resolver);
            }
        }

        @Override
        protected PreparerFactory createPreparerFactory(ApplicationContext context) {
            if (preparerFactoryClass != null) {
                return BeanUtils.instantiateClass(preparerFactoryClass);
            } else {
                return super.createPreparerFactory(context);
            }
        }

        @Override
        protected LocaleResolver createLocaleResolver(ApplicationContext context) {
            return new SpringLocaleResolver();
        }

        @Override
        protected AttributeEvaluatorFactory createAttributeEvaluatorFactory(ApplicationContext context, LocaleResolver resolver) {
            AttributeEvaluator evaluator;
            if (tilesElPresent && JspFactory.getDefaultFactory() != null) {
                evaluator = new TilesElActivator().createEvaluator();
            } else {
                evaluator = new DirectAttributeEvaluator();
            }
            return new BasicAttributeEvaluatorFactory(evaluator);
        }

    }

    private static class SpringCompleteAutoloadTilesInitializer extends CompleteAutoloadTilesInitializer {

        @Override
        protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
            return new SpringCompleteAutoloadTilesContainerFactory();
        }

    }

    private static class SpringCompleteAutoloadTilesContainerFactory extends CompleteAutoloadTilesContainerFactory {

        @Override
        protected LocaleResolver createLocaleResolver(ApplicationContext applicationContext) {
            return new SpringLocaleResolver();
        }

    }

    private class TilesElActivator {

        public AttributeEvaluator createEvaluator() {
            ELAttributeEvaluator evaluator = new ELAttributeEvaluator();
            evaluator.setExpressionFactory(JspFactory.getDefaultFactory().getJspApplicationContext(servletContext).getExpressionFactory());
            evaluator.setResolver(new CompositeELResolverImpl());
            return evaluator;
        }

    }

    private static class CompositeELResolverImpl extends CompositeELResolver {

        public CompositeELResolverImpl() {
            add(new ScopeELResolver());
            add(new TilesContextELResolver(new TilesContextBeanELResolver()));
            add(new TilesContextBeanELResolver());
            add(new ArrayELResolver(false));
            add(new ListELResolver(false));
            add(new MapELResolver(false));
            add(new ResourceBundleELResolver());
            add(new BeanELResolver(false));
        }

    }

}
