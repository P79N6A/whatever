package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapMerger implements Merger<Map<?, ?>> {

    @Override
    public Map<?, ?> merge(Map<?, ?>... items) {
        if (ArrayUtils.isEmpty(items)) {
            return Collections.emptyMap();
        }
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Map<?, ?> item : items) {
            if (item != null) {
                result.putAll(item);
            }
        }
        return result;
    }

}
