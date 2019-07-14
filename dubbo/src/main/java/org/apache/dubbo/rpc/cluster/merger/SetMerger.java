package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SetMerger implements Merger<Set<?>> {

    @Override
    public Set<Object> merge(Set<?>... items) {
        if (ArrayUtils.isEmpty(items)) {
            return Collections.emptySet();
        }
        Set<Object> result = new HashSet<Object>();
        for (Set<?> item : items) {
            if (item != null) {
                result.addAll(item);
            }
        }
        return result;
    }

}
