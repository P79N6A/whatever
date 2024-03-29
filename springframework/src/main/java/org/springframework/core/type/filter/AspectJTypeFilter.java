package org.springframework.core.type.filter;

import org.aspectj.bridge.IMessageHandler;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.World;
import org.aspectj.weaver.bcel.BcelWorld;
import org.aspectj.weaver.patterns.*;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;

import java.io.IOException;

public class AspectJTypeFilter implements TypeFilter {

    private final World world;

    private final TypePattern typePattern;

    public AspectJTypeFilter(String typePatternExpression, @Nullable ClassLoader classLoader) {
        this.world = new BcelWorld(classLoader, IMessageHandler.THROW, null);
        this.world.setBehaveInJava5Way(true);
        PatternParser patternParser = new PatternParser(typePatternExpression);
        TypePattern typePattern = patternParser.parseTypePattern();
        typePattern.resolve(this.world);
        IScope scope = new SimpleScope(this.world, new FormalBinding[0]);
        this.typePattern = typePattern.resolveBindings(scope, Bindings.NONE, false, false);
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        String className = metadataReader.getClassMetadata().getClassName();
        ResolvedType resolvedType = this.world.resolve(className);
        return this.typePattern.matchesStatically(resolvedType);
    }

}
