package org.apache.ibatis.mapping;

import org.apache.ibatis.session.Configuration;

import java.util.Collections;
import java.util.Map;

/**
 * 鉴别器
 * 根据查询返回的结果集不同而采用不同的ResultMap
 */
public class Discriminator {

    private ResultMapping resultMapping;

    private Map<String, String> discriminatorMap;

    Discriminator() {
    }

    public static class Builder {

        private Discriminator discriminator = new Discriminator();

        public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
            discriminator.resultMapping = resultMapping;
            discriminator.discriminatorMap = discriminatorMap;
        }

        public Discriminator build() {
            assert discriminator.resultMapping != null;
            assert discriminator.discriminatorMap != null;
            assert !discriminator.discriminatorMap.isEmpty();

            discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
            return discriminator;
        }
    }

    public ResultMapping getResultMapping() {
        return resultMapping;
    }

    public Map<String, String> getDiscriminatorMap() {
        return discriminatorMap;
    }

    public String getMapIdFor(String s) {
        return discriminatorMap.get(s);
    }

}
