package org.apache.dubbo.registry.status;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

import java.util.Collection;

@Activate
public class RegistryStatusChecker implements StatusChecker {

    @Override
    public Status check() {
        Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
        if (registries.isEmpty()) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        StringBuilder buf = new StringBuilder();
        for (Registry registry : registries) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(registry.getUrl().getAddress());
            if (!registry.isAvailable()) {
                level = Status.Level.ERROR;
                buf.append("(disconnected)");
            } else {
                buf.append("(connected)");
            }
        }
        return new Status(level, buf.toString());
    }

}
