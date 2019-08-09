package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

import java.util.Objects;

public interface DeferredImportSelector extends ImportSelector {

    @Nullable
    default Class<? extends Group> getImportGroup() {
        return null;
    }

    interface Group {

        void process(AnnotationMetadata metadata, DeferredImportSelector selector);

        Iterable<Entry> selectImports();

        class Entry {

            private final AnnotationMetadata metadata;

            private final String importClassName;

            public Entry(AnnotationMetadata metadata, String importClassName) {
                this.metadata = metadata;
                this.importClassName = importClassName;
            }

            public AnnotationMetadata getMetadata() {
                return this.metadata;
            }

            public String getImportClassName() {
                return this.importClassName;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                }
                if (other == null || getClass() != other.getClass()) {
                    return false;
                }
                Entry entry = (Entry) other;
                return (Objects.equals(this.metadata, entry.metadata) && Objects.equals(this.importClassName, entry.importClassName));
            }

            @Override
            public int hashCode() {
                return Objects.hash(this.metadata, this.importClassName);
            }

        }

    }

}
