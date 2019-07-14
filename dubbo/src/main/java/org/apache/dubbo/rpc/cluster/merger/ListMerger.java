package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListMerger implements Merger<List<?>> {

    @Override
    public List<Object> merge(List<?>... items) {
        if (ArrayUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<Object> result = new ArrayList<Object>();
        for (List<?> item : items) {
            if (item != null) {
                result.addAll(item);
            }
        }
        return result;
    }

}
