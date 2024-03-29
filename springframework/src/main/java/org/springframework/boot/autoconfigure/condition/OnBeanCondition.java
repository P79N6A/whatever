package org.springframework.boot.autoconfigure.condition;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.BeanTypeRegistry.TypeExtractor;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 继承SpringBootCondition，继承matches方法
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {

    public static final String FACTORY_BEAN_OBJECT_TYPE = BeanTypeRegistry.FACTORY_BEAN_OBJECT_TYPE;

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        // 在注册Bean的时候注解生效
        return ConfigurationPhase.REGISTER_BEAN;
    }

    @Override
    protected final ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
        for (int i = 0; i < outcomes.length; i++) {
            String autoConfigurationClass = autoConfigurationClasses[i];
            if (autoConfigurationClass != null) {
                Set<String> onBeanTypes = autoConfigurationMetadata.getSet(autoConfigurationClass, "ConditionalOnBean");
                outcomes[i] = getOutcome(onBeanTypes, ConditionalOnBean.class);
                if (outcomes[i] == null) {
                    Set<String> onSingleCandidateTypes = autoConfigurationMetadata.getSet(autoConfigurationClass, "ConditionalOnSingleCandidate");
                    outcomes[i] = getOutcome(onSingleCandidateTypes, ConditionalOnSingleCandidate.class);
                }
            }
        }
        return outcomes;
    }

    private ConditionOutcome getOutcome(Set<String> requiredBeanTypes, Class<? extends Annotation> annotation) {
        List<String> missing = filter(requiredBeanTypes, ClassNameFilter.MISSING, getBeanClassLoader());
        if (!missing.isEmpty()) {
            ConditionMessage message = ConditionMessage.forCondition(annotation).didNotFind("required type", "required types").items(Style.QUOTE, missing);
            return ConditionOutcome.noMatch(message);
        }
        return null;
    }

    /**
     * 获得判断结果
     */
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // ConditionMessage
        ConditionMessage matchMessage = ConditionMessage.empty();
        if (metadata.isAnnotated(ConditionalOnBean.class.getName())) {
            BeanSearchSpec spec = new BeanSearchSpec(context, metadata, ConditionalOnBean.class);
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (!matchResult.isAllMatched()) {
                String reason = createOnBeanNoMatchReason(matchResult);
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnBean.class, spec).because(reason));
            }
            matchMessage = matchMessage.andCondition(ConditionalOnBean.class, spec).found("bean", "beans").items(Style.QUOTE, matchResult.getNamesOfAllMatches());
        }
        if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
            BeanSearchSpec spec = new SingleCandidateBeanSearchSpec(context, metadata, ConditionalOnSingleCandidate.class);
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (!matchResult.isAllMatched()) {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnSingleCandidate.class, spec).didNotFind("any beans").atAll());
            } else if (!hasSingleAutowireCandidate(context.getBeanFactory(), matchResult.getNamesOfAllMatches(), spec.getStrategy() == SearchStrategy.ALL)) {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnSingleCandidate.class, spec).didNotFind("a primary bean from beans").items(Style.QUOTE, matchResult.getNamesOfAllMatches()));
            }
            matchMessage = matchMessage.andCondition(ConditionalOnSingleCandidate.class, spec).found("a primary bean from beans").items(Style.QUOTE, matchResult.getNamesOfAllMatches());
        }
        // 当前标注的注解是不是@ConditionalOnMissingBean
        if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
            // 包含@ConditionalOnMissingBean注解的属性
            BeanSearchSpec spec = new BeanSearchSpec(context, metadata, ConditionalOnMissingBean.class);
            // 搜索实现
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (matchResult.isAnyMatched()) {
                String reason = createOnMissingBeanNoMatchReason(matchResult);
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnMissingBean.class, spec).because(reason));
            }
            matchMessage = matchMessage.andCondition(ConditionalOnMissingBean.class, spec).didNotFind("any beans").atAll();
        }
        return ConditionOutcome.match(matchMessage);
    }

    protected final MatchResult getMatchingBeans(ConditionContext context, BeanSearchSpec beans) {
        // 当前BeanFactory
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        // 搜索策略是ANCESTORS
        if (beans.getStrategy() == SearchStrategy.ANCESTORS) {
            BeanFactory parent = beanFactory.getParentBeanFactory();
            Assert.isInstanceOf(ConfigurableListableBeanFactory.class, parent, "Unable to use SearchStrategy.PARENTS");
            // 父工厂
            beanFactory = (ConfigurableListableBeanFactory) parent;
        }
        MatchResult matchResult = new MatchResult();
        // 搜索策略是CURRENT
        boolean considerHierarchy = beans.getStrategy() != SearchStrategy.CURRENT;
        TypeExtractor typeExtractor = beans.getTypeExtractor(context.getClassLoader());
        List<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(beans.getIgnoredTypes(), typeExtractor, beanFactory, context, considerHierarchy);
        // 要查找的Bean类型
        for (String type : beans.getTypes()) {
            Collection<String> typeMatches = getBeanNamesForType(beanFactory, type, typeExtractor, context.getClassLoader(), considerHierarchy);
            typeMatches.removeAll(beansIgnoredByType);
            if (typeMatches.isEmpty()) {
                matchResult.recordUnmatchedType(type);
            } else {
                matchResult.recordMatchedType(type, typeMatches);
            }
        }
        for (String annotation : beans.getAnnotations()) {
            List<String> annotationMatches = Arrays.asList(getBeanNamesForAnnotation(beanFactory, annotation, context.getClassLoader(), considerHierarchy));
            annotationMatches.removeAll(beansIgnoredByType);
            if (annotationMatches.isEmpty()) {
                matchResult.recordUnmatchedAnnotation(annotation);
            } else {
                matchResult.recordMatchedAnnotation(annotation, annotationMatches);
            }
        }
        for (String beanName : beans.getNames()) {
            if (!beansIgnoredByType.contains(beanName) && containsBean(beanFactory, beanName, considerHierarchy)) {
                matchResult.recordMatchedName(beanName);
            } else {
                matchResult.recordUnmatchedName(beanName);
            }
        }
        return matchResult;
    }

    private String[] getBeanNamesForAnnotation(ConfigurableListableBeanFactory beanFactory, String type, ClassLoader classLoader, boolean considerHierarchy) throws LinkageError {
        Set<String> names = new HashSet<>();
        try {
            @SuppressWarnings("unchecked") Class<? extends Annotation> annotationType = (Class<? extends Annotation>) ClassUtils.forName(type, classLoader);
            collectBeanNamesForAnnotation(names, beanFactory, annotationType, considerHierarchy);
        } catch (ClassNotFoundException ex) {
            // Continue
        }
        return StringUtils.toStringArray(names);
    }

    private void collectBeanNamesForAnnotation(Set<String> names, ListableBeanFactory beanFactory, Class<? extends Annotation> annotationType, boolean considerHierarchy) {
        BeanTypeRegistry registry = BeanTypeRegistry.get(beanFactory);
        names.addAll(registry.getNamesForAnnotation(annotationType));
        if (considerHierarchy) {
            BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
            if (parent instanceof ListableBeanFactory) {
                collectBeanNamesForAnnotation(names, (ListableBeanFactory) parent, annotationType, considerHierarchy);
            }
        }
    }

    private List<String> getNamesOfBeansIgnoredByType(List<String> ignoredTypes, TypeExtractor typeExtractor, ListableBeanFactory beanFactory, ConditionContext context, boolean considerHierarchy) {
        List<String> beanNames = new ArrayList<>();
        for (String ignoredType : ignoredTypes) {
            beanNames.addAll(getBeanNamesForType(beanFactory, ignoredType, typeExtractor, context.getClassLoader(), considerHierarchy));
        }
        return beanNames;
    }

    private boolean containsBean(ConfigurableListableBeanFactory beanFactory, String beanName, boolean considerHierarchy) {
        if (considerHierarchy) {
            return beanFactory.containsBean(beanName);
        }
        return beanFactory.containsLocalBean(beanName);
    }

    private Collection<String> getBeanNamesForType(ListableBeanFactory beanFactory, String type, TypeExtractor typeExtractor, ClassLoader classLoader, boolean considerHierarchy) throws LinkageError {
        try {
            return getBeanNamesForType(beanFactory, considerHierarchy, ClassUtils.forName(type, classLoader), typeExtractor);
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            return Collections.emptySet();
        }
    }

    private Collection<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type, TypeExtractor typeExtractor) {
        Set<String> result = new LinkedHashSet<>();
        collectBeanNamesForType(result, beanFactory, type, typeExtractor, considerHierarchy);
        return result;
    }

    private void collectBeanNamesForType(Set<String> result, ListableBeanFactory beanFactory, Class<?> type, TypeExtractor typeExtractor, boolean considerHierarchy) {
        BeanTypeRegistry registry = BeanTypeRegistry.get(beanFactory);
        result.addAll(registry.getNamesForType(type, typeExtractor));
        if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory) {
            BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
            if (parent instanceof ListableBeanFactory) {
                collectBeanNamesForType(result, (ListableBeanFactory) parent, type, typeExtractor, considerHierarchy);
            }
        }
    }

    private String createOnBeanNoMatchReason(MatchResult matchResult) {
        StringBuilder reason = new StringBuilder();
        appendMessageForNoMatches(reason, matchResult.getUnmatchedAnnotations(), "annotated with");
        appendMessageForNoMatches(reason, matchResult.getUnmatchedTypes(), "of type");
        appendMessageForNoMatches(reason, matchResult.getUnmatchedNames(), "named");
        return reason.toString();
    }

    private void appendMessageForNoMatches(StringBuilder reason, Collection<String> unmatched, String description) {
        if (!unmatched.isEmpty()) {
            if (reason.length() > 0) {
                reason.append(" and ");
            }
            reason.append("did not find any beans ");
            reason.append(description);
            reason.append(" ");
            reason.append(StringUtils.collectionToDelimitedString(unmatched, ", "));
        }
    }

    private String createOnMissingBeanNoMatchReason(MatchResult matchResult) {
        StringBuilder reason = new StringBuilder();
        appendMessageForMatches(reason, matchResult.getMatchedAnnotations(), "annotated with");
        appendMessageForMatches(reason, matchResult.getMatchedTypes(), "of type");
        if (!matchResult.getMatchedNames().isEmpty()) {
            if (reason.length() > 0) {
                reason.append(" and ");
            }
            reason.append("found beans named ");
            reason.append(StringUtils.collectionToDelimitedString(matchResult.getMatchedNames(), ", "));
        }
        return reason.toString();
    }

    private void appendMessageForMatches(StringBuilder reason, Map<String, Collection<String>> matches, String description) {
        if (!matches.isEmpty()) {
            matches.forEach((key, value) -> {
                if (reason.length() > 0) {
                    reason.append(" and ");
                }
                reason.append("found beans ");
                reason.append(description);
                reason.append(" '");
                reason.append(key);
                reason.append("' ");
                reason.append(StringUtils.collectionToDelimitedString(value, ", "));
            });
        }
    }

    private boolean hasSingleAutowireCandidate(ConfigurableListableBeanFactory beanFactory, Set<String> beanNames, boolean considerHierarchy) {
        return (beanNames.size() == 1 || getPrimaryBeans(beanFactory, beanNames, considerHierarchy).size() == 1);
    }

    private List<String> getPrimaryBeans(ConfigurableListableBeanFactory beanFactory, Set<String> beanNames, boolean considerHierarchy) {
        List<String> primaryBeans = new ArrayList<>();
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = findBeanDefinition(beanFactory, beanName, considerHierarchy);
            if (beanDefinition != null && beanDefinition.isPrimary()) {
                primaryBeans.add(beanName);
            }
        }
        return primaryBeans;
    }

    private BeanDefinition findBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName, boolean considerHierarchy) {
        if (beanFactory.containsBeanDefinition(beanName)) {
            return beanFactory.getBeanDefinition(beanName);
        }
        if (considerHierarchy && beanFactory.getParentBeanFactory() instanceof ConfigurableListableBeanFactory) {
            return findBeanDefinition(((ConfigurableListableBeanFactory) beanFactory.getParentBeanFactory()), beanName, considerHierarchy);
        }
        return null;
    }

    protected static class BeanSearchSpec {

        private final Class<?> annotationType;

        private final List<String> names = new ArrayList<>();

        private final List<String> types = new ArrayList<>();

        private final List<String> annotations = new ArrayList<>();

        private final List<String> ignoredTypes = new ArrayList<>();

        private final List<String> parameterizedContainers = new ArrayList<>();

        private final SearchStrategy strategy;

        public BeanSearchSpec(ConditionContext context, AnnotatedTypeMetadata metadata, Class<?> annotationType) {
            this(context, metadata, annotationType, null);
        }

        public BeanSearchSpec(ConditionContext context, AnnotatedTypeMetadata metadata, Class<?> annotationType, Class<?> genericContainer) {
            this.annotationType = annotationType;
            MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(annotationType.getName(), true);
            // 将attributes这个map中的数据放到对应的list成员变量中
            collect(attributes, "name", this.names);
            collect(attributes, "value", this.types);
            collect(attributes, "type", this.types);
            collect(attributes, "annotation", this.annotations);
            collect(attributes, "ignored", this.ignoredTypes);
            collect(attributes, "ignoredType", this.ignoredTypes);
            collect(attributes, "parameterizedContainer", this.parameterizedContainers);
            this.strategy = (SearchStrategy) attributes.getFirst("search");
            BeanTypeDeductionException deductionException = null;
            try {
                if (this.types.isEmpty() && this.names.isEmpty()) {
                    addDeducedBeanType(context, metadata, this.types);
                }
            } catch (BeanTypeDeductionException ex) {
                deductionException = ex;
            }
            validate(deductionException);
        }

        /**
         * 验证
         */
        protected void validate(BeanTypeDeductionException ex) {
            if (!hasAtLeastOne(this.types, this.names, this.annotations)) {
                String message = getAnnotationName() + " did not specify a bean using type, name or annotation";
                if (ex == null) {
                    throw new IllegalStateException(message);
                }
                throw new IllegalStateException(message + " and the attempt to deduce" + " the bean's type failed", ex);
            }
        }

        private boolean hasAtLeastOne(List<?>... lists) {
            return Arrays.stream(lists).anyMatch((list) -> !list.isEmpty());
        }

        protected final String getAnnotationName() {
            return "@" + ClassUtils.getShortName(this.annotationType);
        }

        protected void collect(MultiValueMap<String, Object> attributes, String key, List<String> destination) {
            List<?> values = attributes.get(key);
            if (values != null) {
                for (Object value : values) {
                    if (value instanceof String[]) {
                        Collections.addAll(destination, (String[]) value);
                    } else {
                        destination.add((String) value);
                    }
                }
            }
        }

        private void addDeducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata, final List<String> beanTypes) {
            if (metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName())) {
                addDeducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata, beanTypes);
            }
        }

        private void addDeducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata, final List<String> beanTypes) {
            try {
                Class<?> returnType = getReturnType(context, metadata);
                beanTypes.add(returnType.getName());
            } catch (Throwable ex) {
                throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
            }
        }

        private Class<?> getReturnType(ConditionContext context, MethodMetadata metadata) throws ClassNotFoundException, LinkageError {
            // We should be safe to load at this point since we are in the
            // REGISTER_BEAN phase
            ClassLoader classLoader = context.getClassLoader();
            Class<?> returnType = ClassUtils.forName(metadata.getReturnTypeName(), classLoader);
            if (isParameterizedContainer(returnType, classLoader)) {
                returnType = getReturnTypeGeneric(metadata, classLoader);
            }
            return returnType;
        }

        private Class<?> getReturnTypeGeneric(MethodMetadata metadata, ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
            Class<?> declaringClass = ClassUtils.forName(metadata.getDeclaringClassName(), classLoader);
            Method beanMethod = findBeanMethod(declaringClass, metadata.getMethodName());
            return ResolvableType.forMethodReturnType(beanMethod).resolveGeneric();
        }

        private Method findBeanMethod(Class<?> declaringClass, String methodName) {
            Method method = ReflectionUtils.findMethod(declaringClass, methodName);
            if (isBeanMethod(method)) {
                return method;
            }
            return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(declaringClass)).filter((candidate) -> candidate.getName().equals(methodName)).filter(this::isBeanMethod).findFirst().orElseThrow(() -> new IllegalStateException("Unable to find bean method " + methodName));
        }

        private boolean isBeanMethod(Method method) {
            return method != null && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.EXHAUSTIVE).isPresent(Bean.class);
        }

        public TypeExtractor getTypeExtractor(ClassLoader classLoader) {
            if (this.parameterizedContainers.isEmpty()) {
                return ResolvableType::resolve;
            }
            return (type) -> {
                Class<?> resolved = type.resolve();
                if (isParameterizedContainer(resolved, classLoader)) {
                    return type.getGeneric().resolve();
                }
                return resolved;
            };
        }

        private boolean isParameterizedContainer(Class<?> type, ClassLoader classLoader) {
            for (String candidate : this.parameterizedContainers) {
                try {
                    if (ClassUtils.forName(candidate, classLoader).isAssignableFrom(type)) {
                        return true;
                    }
                } catch (Exception ex) {
                }
            }
            return false;
        }

        public SearchStrategy getStrategy() {
            return (this.strategy != null) ? this.strategy : SearchStrategy.ALL;
        }

        public List<String> getNames() {
            return this.names;
        }

        public List<String> getTypes() {
            return this.types;
        }

        public List<String> getAnnotations() {
            return this.annotations;
        }

        public List<String> getIgnoredTypes() {
            return this.ignoredTypes;
        }

        @Override
        public String toString() {
            StringBuilder string = new StringBuilder();
            string.append("(");
            if (!this.names.isEmpty()) {
                string.append("names: ");
                string.append(StringUtils.collectionToCommaDelimitedString(this.names));
                if (!this.types.isEmpty()) {
                    string.append("; ");
                }
            }
            if (!this.types.isEmpty()) {
                string.append("types: ");
                string.append(StringUtils.collectionToCommaDelimitedString(this.types));
            }
            string.append("; SearchStrategy: ");
            string.append(this.strategy.toString().toLowerCase(Locale.ENGLISH));
            string.append(")");
            return string.toString();
        }

    }

    private static class SingleCandidateBeanSearchSpec extends BeanSearchSpec {

        SingleCandidateBeanSearchSpec(ConditionContext context, AnnotatedTypeMetadata metadata, Class<?> annotationType) {
            super(context, metadata, annotationType);
        }

        @Override
        protected void collect(MultiValueMap<String, Object> attributes, String key, List<String> destination) {
            super.collect(attributes, key, destination);
            destination.removeAll(Arrays.asList("", Object.class.getName()));
        }

        @Override
        protected void validate(BeanTypeDeductionException ex) {
            Assert.isTrue(getTypes().size() == 1, () -> getAnnotationName() + " annotations must specify only one type (got " + getTypes() + ")");
        }

    }

    protected static final class MatchResult {

        private final Map<String, Collection<String>> matchedAnnotations = new HashMap<>();

        private final List<String> matchedNames = new ArrayList<>();

        private final Map<String, Collection<String>> matchedTypes = new HashMap<>();

        private final List<String> unmatchedAnnotations = new ArrayList<>();

        private final List<String> unmatchedNames = new ArrayList<>();

        private final List<String> unmatchedTypes = new ArrayList<>();

        private final Set<String> namesOfAllMatches = new HashSet<>();

        private void recordMatchedName(String name) {
            this.matchedNames.add(name);
            this.namesOfAllMatches.add(name);
        }

        private void recordUnmatchedName(String name) {
            this.unmatchedNames.add(name);
        }

        private void recordMatchedAnnotation(String annotation, Collection<String> matchingNames) {
            this.matchedAnnotations.put(annotation, matchingNames);
            this.namesOfAllMatches.addAll(matchingNames);
        }

        private void recordUnmatchedAnnotation(String annotation) {
            this.unmatchedAnnotations.add(annotation);
        }

        private void recordMatchedType(String type, Collection<String> matchingNames) {
            this.matchedTypes.put(type, matchingNames);
            this.namesOfAllMatches.addAll(matchingNames);
        }

        private void recordUnmatchedType(String type) {
            this.unmatchedTypes.add(type);
        }

        public boolean isAllMatched() {
            return this.unmatchedAnnotations.isEmpty() && this.unmatchedNames.isEmpty() && this.unmatchedTypes.isEmpty();
        }

        public boolean isAnyMatched() {
            return (!this.matchedAnnotations.isEmpty()) || (!this.matchedNames.isEmpty()) || (!this.matchedTypes.isEmpty());
        }

        public Map<String, Collection<String>> getMatchedAnnotations() {
            return this.matchedAnnotations;
        }

        public List<String> getMatchedNames() {
            return this.matchedNames;
        }

        public Map<String, Collection<String>> getMatchedTypes() {
            return this.matchedTypes;
        }

        public List<String> getUnmatchedAnnotations() {
            return this.unmatchedAnnotations;
        }

        public List<String> getUnmatchedNames() {
            return this.unmatchedNames;
        }

        public List<String> getUnmatchedTypes() {
            return this.unmatchedTypes;
        }

        public Set<String> getNamesOfAllMatches() {
            return this.namesOfAllMatches;
        }

    }

    static final class BeanTypeDeductionException extends RuntimeException {

        private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
            super("Failed to deduce bean type for " + className + "." + beanMethodName, cause);
        }

    }

}
