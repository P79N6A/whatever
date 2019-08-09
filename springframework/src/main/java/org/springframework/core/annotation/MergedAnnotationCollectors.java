package org.springframework.core.annotation;

import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

public abstract class MergedAnnotationCollectors {

    private static final Characteristics[] NO_CHARACTERISTICS = {};

    private static final Characteristics[] IDENTITY_FINISH_CHARACTERISTICS = {Characteristics.IDENTITY_FINISH};

    private MergedAnnotationCollectors() {
    }

    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
        return Collector.of(ArrayList<A>::new, (list, annotation) -> list.add(annotation.synthesize()), MergedAnnotationCollectors::addAll, LinkedHashSet::new);
    }

    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Annotation[]> toAnnotationArray() {
        return toAnnotationArray(Annotation[]::new);
    }

    public static <R extends Annotation, A extends R> Collector<MergedAnnotation<A>, ?, R[]> toAnnotationArray(IntFunction<R[]> generator) {
        return Collector.of(ArrayList::new, (list, annotation) -> list.add(annotation.synthesize()), MergedAnnotationCollectors::addAll, list -> list.toArray(generator.apply(list.size())));
    }

    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(Adapt... adaptations) {
        return toMultiValueMap(Function.identity(), adaptations);
    }

    public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(Function<MultiValueMap<String, Object>, MultiValueMap<String, Object>> finisher, Adapt... adaptations) {
        Characteristics[] characteristics = (isSameInstance(finisher, Function.identity()) ? IDENTITY_FINISH_CHARACTERISTICS : NO_CHARACTERISTICS);
        return Collector.of(LinkedMultiValueMap::new, (map, annotation) -> annotation.asMap(adaptations).forEach(map::add), MergedAnnotationCollectors::merge, finisher, characteristics);
    }

    private static boolean isSameInstance(Object instance, Object candidate) {
        return instance == candidate;
    }

    private static <E, L extends List<E>> L addAll(L list, L additions) {
        list.addAll(additions);
        return list;
    }

    private static <K, V> MultiValueMap<K, V> merge(MultiValueMap<K, V> map, MultiValueMap<K, V> additions) {
        map.addAll(additions);
        return map;
    }

}
