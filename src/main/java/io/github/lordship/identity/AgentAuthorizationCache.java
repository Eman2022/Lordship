package io.github.lordship.identity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

@Component
public class AgentAuthorizationCache {

    // backup eviction policy: clear cache after n number of seconds
    private static final Duration TTL = Duration.ofSeconds(60);

    private static final int MAX_ENTRIES = 5000;

    private final Cache<UUID, AgentAuthorization> cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(MAX_ENTRIES)
            .recordStats()
            .build();

    public AgentAuthorization get(UUID agentId, Function<UUID, AgentAuthorization> loader) {
        return cache.get(agentId, loader);
    }

    public void invalidate(UUID agentId) {
        cache.invalidate(agentId);
        againAfterTransaction(() -> cache.invalidate(agentId));
    }

    public void invalidateAll() {
        cache.invalidateAll();
        againAfterTransaction(cache::invalidateAll);
    }

    public CacheStats stats() {
        return cache.stats();
    }

    // deletes the cached item again
    private static void againAfterTransaction(Runnable eviction) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                eviction.run();
            }
        });
    }


}
