package org.apache.ibatis.builder;

public interface InitializingObject {

    void initialize() throws Exception;

}