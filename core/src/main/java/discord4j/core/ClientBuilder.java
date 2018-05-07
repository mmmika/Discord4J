/*
 * This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */
package discord4j.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import discord4j.common.jackson.PossibleModule;
import discord4j.common.json.payload.dispatch.Dispatch;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.dispatch.DispatchContext;
import discord4j.core.event.dispatch.DispatchHandlers;
import discord4j.core.event.domain.Event;
import discord4j.core.object.presence.Presence;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.IdentifyOptions;
import discord4j.gateway.payload.JacksonPayloadReader;
import discord4j.gateway.payload.JacksonPayloadWriter;
import discord4j.gateway.retry.RetryOptions;
import discord4j.rest.RestClient;
import discord4j.rest.http.*;
import discord4j.rest.http.client.SimpleHttpClient;
import discord4j.rest.request.Router;
import discord4j.rest.route.Routes;
import discord4j.store.service.StoreService;
import discord4j.store.service.StoreServiceLoader;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;

public final class ClientBuilder {

    private String token;
    private Integer shardIndex;
    private Integer shardCount;
    private StoreService storeService;
    private FluxProcessor<Event, Event> eventProcessor;
    private Scheduler eventScheduler;
    private Presence initialPresence;
    private IdentifyOptions identifyOptions;

    public ClientBuilder(final String token) {
        this.token = Objects.requireNonNull(token);
        storeService = new StoreServiceLoader().getStoreService();
        eventProcessor = EmitterProcessor.create(false);
        eventScheduler = Schedulers.elastic();
        initialPresence = null;
        identifyOptions = null;
    }

    public String getToken() {
        return token;
    }

    public ClientBuilder setToken(final String token) {
        this.token = Objects.requireNonNull(token);
        return this;
    }

    public Integer getShardIndex() {
        return shardIndex;
    }

    public ClientBuilder setShardIndex(final Integer shardIndex) {
        this.shardIndex = shardIndex;
        return this;
    }

    public Integer getShardCount() {
        return shardCount;
    }

    public ClientBuilder setShardCount(final Integer shardCount) {
        this.shardCount = shardCount;
        return this;
    }

    public StoreService getStoreService() {
        return storeService;
    }

    public ClientBuilder setStoreService(final StoreService storeService) {
        this.storeService = Objects.requireNonNull(storeService);
        return this;
    }

    public FluxProcessor<Event, Event> getEventProcessor() {
        return eventProcessor;
    }

    public ClientBuilder setEventProcessor(final FluxProcessor<Event, Event> eventProcessor) {
        this.eventProcessor = Objects.requireNonNull(eventProcessor);
        return this;
    }

    public Scheduler getEventScheduler() {
        return eventScheduler;
    }

    public ClientBuilder setEventScheduler(final Scheduler eventScheduler) {
        this.eventScheduler = Objects.requireNonNull(eventScheduler);
        return this;
    }

    public Presence getInitialPresence() {
        return initialPresence;
    }

    public ClientBuilder setInitialPresence(@Nullable Presence initialPresence) {
        this.initialPresence = initialPresence;
        return this;
    }

    public IdentifyOptions getIdentifyOptions() {
        return identifyOptions;
    }

    public ClientBuilder setIdentifyOptions(IdentifyOptions identifyOptions) {
        this.identifyOptions = identifyOptions;
        return this;
    }

    public DiscordClient build() {
        final ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .registerModules(new PossibleModule(), new Jdk8Module());

        final SimpleHttpClient httpClient = SimpleHttpClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bot " + token)
                .defaultHeader("User-Agent", "Discord4J")
                .readerStrategy(new JacksonReaderStrategy<>(mapper))
                .readerStrategy(new EmptyReaderStrategy())
                .writerStrategy(new MultipartWriterStrategy(mapper))
                .writerStrategy(new JacksonWriterStrategy(mapper))
                .writerStrategy(new EmptyWriterStrategy())
                .baseUrl(Routes.BASE_URL)
                .build();

        // TODO custom retry parameters
        if (identifyOptions == null) {
            identifyOptions = new IdentifyOptions();
        }
        if (shardIndex != null) {
            identifyOptions.setShardIndex(shardIndex);
        }
        if (shardCount != null) {
            identifyOptions.setShardCount(shardCount);
        }
        if (identifyOptions.getShardIndex() == null ^ identifyOptions.getShardCount() == null) {
            throw new IllegalArgumentException("Must set shardIndex and shardCount together");
        }
        if (initialPresence != null) {
            identifyOptions.setInitialStatus(initialPresence.asStatusUpdate());
        }

        final GatewayClient gatewayClient = new GatewayClient(
                new JacksonPayloadReader(mapper), new JacksonPayloadWriter(mapper),
                new RetryOptions(Duration.ofSeconds(5), Duration.ofSeconds(120)), token, identifyOptions);

        final StoreHolder storeHolder = new StoreHolder(storeService);
        final RestClient restClient = new RestClient(new Router(httpClient));
        final ClientConfig config = new ClientConfig(token, shardIndex, shardCount);
        final EventDispatcher eventDispatcher = new EventDispatcher(eventProcessor, eventScheduler);

        final ServiceMediator serviceMediator = new ServiceMediator(gatewayClient, restClient, storeHolder,
                eventDispatcher, config);

        serviceMediator.getGatewayClient().dispatch()
                .map(dispatch -> DispatchContext.of(dispatch, serviceMediator))
                .flatMap(DispatchHandlers::<Dispatch, Event>handle)
                .subscribeWith(eventProcessor);

        return serviceMediator.getClient();
    }
}
