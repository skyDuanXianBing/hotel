package server.demo.service;

import org.springframework.stereotype.Component;
import server.demo.enums.SmartLockProvider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import server.demo.i18n.ApiMessages;
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.3c20c7dc39b6") + provider);
        }
        return client;
    }
}
