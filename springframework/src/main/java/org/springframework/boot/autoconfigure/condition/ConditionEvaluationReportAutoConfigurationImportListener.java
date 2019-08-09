package org.springframework.boot.autoconfigure.condition;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportEvent;
import org.springframework.boot.autoconfigure.AutoConfigurationImportListener;

class ConditionEvaluationReportAutoConfigurationImportListener implements AutoConfigurationImportListener, BeanFactoryAware {

    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
        if (this.beanFactory != null) {
            ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
            report.recordEvaluationCandidates(event.getCandidateConfigurations());
            report.recordExclusions(event.getExclusions());
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory) ? (ConfigurableListableBeanFactory) beanFactory : null;
    }

}
