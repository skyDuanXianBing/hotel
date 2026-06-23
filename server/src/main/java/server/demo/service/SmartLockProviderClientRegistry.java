package server.demo.service;

import org.springframework.stereotype.Component;
import server.demo.enums.SmartLockProvider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SmartLockProviderClientRegistry {
    private final Map<SmartLockProvider, SmartLockProviderClient> clients;

    public SmartLockProviderClientRegistry(List<SmartLockProviderClient> providerClients) {
        this.clients = new EnumMap<>(SmartLockProvider.class);
        for (SmartLockProviderClient client : providerClients) {
            clients.put(client.getProvider(), client);
        }
    }

    public SmartLockProviderClient getClient(SmartLockProvider provider) {
        SmartLockProviderClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("不支持的门锁服务商: " + provider);
        }
        return client;
    }
}
