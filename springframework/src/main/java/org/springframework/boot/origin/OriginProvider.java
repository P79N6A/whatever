package org.springframework.boot.origin;

@FunctionalInterface
public interface OriginProvider {

    Origin getOrigin();

}
