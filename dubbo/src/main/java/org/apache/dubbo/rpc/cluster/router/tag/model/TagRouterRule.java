package org.apache.dubbo.rpc.cluster.router.tag.model;

import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagRouterRule extends AbstractRouterRule {
    private List<Tag> tags;

    private Map<String, List<String>> addressToTagnames = new HashMap<>();

    private Map<String, List<String>> tagnameToAddresses = new HashMap<>();

    public void init() {
        if (!isValid()) {
            return;
        }
        tags.forEach(tag -> {
            tagnameToAddresses.put(tag.getName(), tag.getAddresses());
            tag.getAddresses().forEach(addr -> {
                List<String> tagNames = addressToTagnames.computeIfAbsent(addr, k -> new ArrayList<>());
                tagNames.add(tag.getName());
            });
        });
    }

    public List<String> getAddresses() {
        return tags.stream().flatMap(tag -> tag.getAddresses().stream()).collect(Collectors.toList());
    }

    public List<String> getTagNames() {
        return tags.stream().map(Tag::getName).collect(Collectors.toList());
    }

    public Map<String, List<String>> getAddressToTagnames() {
        return addressToTagnames;
    }

    public Map<String, List<String>> getTagnameToAddresses() {
        return tagnameToAddresses;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

}
