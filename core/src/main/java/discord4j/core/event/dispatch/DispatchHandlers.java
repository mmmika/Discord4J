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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J. If not, see <http://www.gnu.org/licenses/>.
 */
package discord4j.core.event.dispatch;

import discord4j.core.DiscordClient;
import discord4j.core.ServiceMediator;
import discord4j.core.event.EventMapper;
import discord4j.core.event.domain.*;
import discord4j.core.event.domain.channel.TypingStartEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.data.stored.PresenceBean;
import discord4j.core.object.data.stored.UserBean;
import discord4j.core.object.data.stored.VoiceStateBean;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Presence;
import discord4j.gateway.json.dispatch.*;
import discord4j.gateway.retry.GatewayStateChange;
import discord4j.store.api.util.LongLongTuple2;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for {@link discord4j.gateway.json.dispatch.Dispatch} to {@link discord4j.core.event.domain.Event}
 * mapping operations, performing read and write operations on the configured Store on each inbound Dispatch event.
 */
public class DispatchHandlers implements EventMapper {

    private final Map<Class<?>, DispatchHandler<?, ?>> handlerMap = new HashMap<>();

    private final boolean readOnly;

    public DispatchHandlers(boolean readOnly) {
        this.readOnly = readOnly;
        ChannelDispatchHandlers channel = new ChannelDispatchHandlers(readOnly);
        GuildDispatchHandlers guild = new GuildDispatchHandlers(readOnly);
        MessageDispatchHandlers message = new MessageDispatchHandlers(readOnly);
        LifecycleDispatchHandlers lifecycle = new LifecycleDispatchHandlers(readOnly);

        addHandler(ChannelCreate.class, channel::channelCreate);
        addHandler(ChannelDelete.class, channel::channelDelete);
        addHandler(ChannelPinsUpdate.class, channel::channelPinsUpdate);
        addHandler(ChannelUpdate.class, channel::channelUpdate);
        addHandler(GuildBanAdd.class, guild::guildBanAdd);
        addHandler(GuildBanRemove.class, guild::guildBanRemove);
        addHandler(GuildCreate.class, guild::guildCreate);
        addHandler(GuildDelete.class, guild::guildDelete);
        addHandler(GuildEmojisUpdate.class, guild::guildEmojisUpdate);
        addHandler(GuildIntegrationsUpdate.class, guild::guildIntegrationsUpdate);
        addHandler(GuildMemberAdd.class, guild::guildMemberAdd);
        addHandler(GuildMemberRemove.class, guild::guildMemberRemove);
        addHandler(GuildMembersChunk.class, guild::guildMembersChunk);
        addHandler(GuildMemberUpdate.class, guild::guildMemberUpdate);
        addHandler(GuildRoleCreate.class, guild::guildRoleCreate);
        addHandler(GuildRoleDelete.class, guild::guildRoleDelete);
        addHandler(GuildRoleUpdate.class, guild::guildRoleUpdate);
        addHandler(GuildUpdate.class, guild::guildUpdate);
        addHandler(MessageCreate.class, message::messageCreate);
        addHandler(MessageDelete.class, message::messageDelete);
        addHandler(MessageDeleteBulk.class, message::messageDeleteBulk);
        addHandler(MessageReactionAdd.class, message::messageReactionAdd);
        addHandler(MessageReactionRemove.class, message::messageReactionRemove);
        addHandler(MessageReactionRemoveAll.class, message::messageReactionRemoveAll);
        addHandler(MessageUpdate.class, message::messageUpdate);
        addHandler(PresenceUpdate.class, this::presenceUpdate);
        addHandler(Ready.class, lifecycle::ready);
        addHandler(Resumed.class, lifecycle::resumed);
        addHandler(TypingStart.class, this::typingStart);
        addHandler(UserUpdate.class, this::userUpdate);
        addHandler(VoiceServerUpdate.class, this::voiceServerUpdate);
        addHandler(VoiceStateUpdateDispatch.class, this::voiceStateUpdateDispatch);
        addHandler(WebhooksUpdate.class, this::webhooksUpdate);

        addHandler(GatewayStateChange.class, lifecycle::gatewayStateChanged);
    }

    private <D extends Dispatch, E extends Event> void addHandler(Class<D> dispatchType,
            DispatchHandler<D, E> dispatchHandler) {
        handlerMap.put(dispatchType, dispatchHandler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Dispatch, E extends Event> Mono<E> handle(DispatchContext<D> context) {
        DispatchHandler<D, E> entry = (DispatchHandler<D, E>) handlerMap.get(context.getDispatch().getClass());
        if (entry == null) {
            return Mono.empty();
        }
        return entry.handle(context);
    }

    private <T> Mono<T> emptyIfReadOnly(Mono<T> mono) {
        return readOnly ? Mono.empty() : mono;
    }

    private Mono<PresenceUpdateEvent> presenceUpdate(DispatchContext<PresenceUpdate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();

        long guildId = context.getDispatch().getGuildId();
        long userId = context.getDispatch().getUser().getId();
        LongLongTuple2 key = LongLongTuple2.of(guildId, userId);
        PresenceBean bean = new PresenceBean(context.getDispatch());
        Presence current = new Presence(bean);

        Mono<Void> saveNew = emptyIfReadOnly(serviceMediator.getStateHolder().getPresenceStore().save(key, bean));

        return serviceMediator.getStateHolder().getPresenceStore()
                .find(key)
                .flatMap(saveNew::thenReturn)
                .map(old -> new PresenceUpdateEvent(client, guildId, userId, current, new Presence(old)))
                .switchIfEmpty(saveNew.thenReturn(new PresenceUpdateEvent(client, guildId, userId, current, null)));
    }

    private Mono<TypingStartEvent> typingStart(DispatchContext<TypingStart> context) {
        DiscordClient client = context.getServiceMediator().getClient();
        long channelId = context.getDispatch().getChannelId();
        long userId = context.getDispatch().getUserId();
        Instant startTime = Instant.ofEpochMilli(context.getDispatch().getTimestamp());

        return Mono.just(new TypingStartEvent(client, channelId, userId, startTime));
    }

    private Mono<UserUpdateEvent> userUpdate(DispatchContext<UserUpdate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();

        UserBean bean = new UserBean(context.getDispatch().getUser());
        User current = new User(serviceMediator, bean);

        Mono<Void> saveNew = emptyIfReadOnly(serviceMediator.getStateHolder().getUserStore().save(bean.getId(), bean));

        return serviceMediator.getStateHolder().getUserStore()
                .find(context.getDispatch().getUser().getId())
                .flatMap(saveNew::thenReturn)
                .map(old -> new UserUpdateEvent(client, current, new User(serviceMediator, old)))
                .switchIfEmpty(saveNew.thenReturn(new UserUpdateEvent(client, current, null)));
    }

    private Mono<Event> voiceServerUpdate(DispatchContext<VoiceServerUpdate> context) {
        DiscordClient client = context.getServiceMediator().getClient();
        String token = context.getDispatch().getToken();
        long guildId = context.getDispatch().getGuildId();
        String endpoint = context.getDispatch().getEndpoint();

        return Mono.just(new VoiceServerUpdateEvent(client, token, guildId, endpoint));
    }

    private Mono<VoiceStateUpdateEvent> voiceStateUpdateDispatch(DispatchContext<VoiceStateUpdateDispatch>
            context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();

        long guildId = context.getDispatch().getVoiceState().getGuildId();
        long userId = context.getDispatch().getVoiceState().getUserId();

        LongLongTuple2 key = LongLongTuple2.of(guildId, userId);
        VoiceStateBean bean = new VoiceStateBean(context.getDispatch().getVoiceState());
        VoiceState current = new VoiceState(serviceMediator, bean);

        Mono<Void> saveNew = emptyIfReadOnly(serviceMediator.getStateHolder().getVoiceStateStore().save(key, bean));

        return serviceMediator.getStateHolder().getVoiceStateStore()
                .find(key)
                .flatMap(saveNew::thenReturn)
                .map(old -> new VoiceStateUpdateEvent(client, current, new VoiceState(serviceMediator, old)))
                .switchIfEmpty(saveNew.thenReturn(new VoiceStateUpdateEvent(client, current, null)));
    }

    private Mono<Event> webhooksUpdate(DispatchContext<WebhooksUpdate> context) {
        long guildId = context.getDispatch().getGuildId();
        long channelId = context.getDispatch().getChannelId();

        return Mono.just(new WebhooksUpdateEvent(context.getServiceMediator().getClient(), guildId, channelId));
    }
}
