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
package discord4j.core.event.dispatch;

import discord4j.core.DiscordClient;
import discord4j.core.ServiceMediator;
import discord4j.core.StateHolder;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.channel.*;
import discord4j.core.object.data.PrivateChannelBean;
import discord4j.core.object.data.stored.CategoryBean;
import discord4j.core.object.data.stored.GuildBean;
import discord4j.core.object.data.stored.TextChannelBean;
import discord4j.core.object.data.stored.VoiceChannelBean;
import discord4j.core.object.entity.*;
import discord4j.core.util.ArrayUtil;
import discord4j.gateway.json.dispatch.ChannelCreate;
import discord4j.gateway.json.dispatch.ChannelDelete;
import discord4j.gateway.json.dispatch.ChannelPinsUpdate;
import discord4j.gateway.json.dispatch.ChannelUpdate;
import discord4j.gateway.json.response.GatewayChannelResponse;
import discord4j.store.api.primitive.LongObjStore;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

class ChannelDispatchHandlers {

    private final boolean readOnly;

    ChannelDispatchHandlers(boolean readOnly) {
        this.readOnly = readOnly;
    }

    private <T> Mono<T> emptyIfReadOnly(Mono<T> mono) {
        return readOnly ? Mono.empty() : mono;
    }

    Mono<? extends Event> channelCreate(DispatchContext<ChannelCreate> context) {
        Channel.Type type = Channel.Type.of(context.getDispatch().getChannel().getType());

        switch (type) {
            case GUILD_TEXT:
                return textChannelCreate(context);
            case DM:
                return privateChannelCreate(context);
            case GUILD_VOICE:
                return voiceChannelCreate(context);
            case GROUP_DM:
                throw new UnsupportedOperationException("Received channel_create for group on a bot account!");
            case GUILD_CATEGORY:
                return categoryCreateEvent(context);
            default:
                throw new AssertionError();
        }
    }

    private Mono<TextChannelCreateEvent> textChannelCreate(DispatchContext<ChannelCreate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        TextChannelBean bean = new TextChannelBean(channel, guildId);

        Mono<TextChannelCreateEvent> saveChannel =
                emptyIfReadOnly(serviceMediator.getStateHolder().getTextChannelStore().save(bean.getId(), bean))
                        .thenReturn(new TextChannelCreateEvent(client, new TextChannel(serviceMediator, bean)));

        return addChannelToGuild(serviceMediator.getStateHolder().getGuildStore(), channel, guildId)
                .then(saveChannel);
    }

    private Mono<PrivateChannelCreateEvent> privateChannelCreate(DispatchContext<ChannelCreate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();
        PrivateChannelBean bean = new PrivateChannelBean(context.getDispatch().getChannel());

        return Mono.just(new PrivateChannelCreateEvent(client, new PrivateChannel(serviceMediator, bean)));
    }

    private Mono<VoiceChannelCreateEvent> voiceChannelCreate(DispatchContext<ChannelCreate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        VoiceChannelBean bean = new VoiceChannelBean(channel, guildId);

        Mono<VoiceChannelCreateEvent> saveChannel =
                emptyIfReadOnly(serviceMediator.getStateHolder().getVoiceChannelStore().save(bean.getId(), bean))
                        .thenReturn(new VoiceChannelCreateEvent(client, new VoiceChannel(serviceMediator, bean)));

        return addChannelToGuild(serviceMediator.getStateHolder().getGuildStore(), channel, guildId)
                .then(saveChannel);
    }

    private Mono<CategoryCreateEvent> categoryCreateEvent(DispatchContext<ChannelCreate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        CategoryBean bean = new CategoryBean(channel, guildId);

        Mono<CategoryCreateEvent> saveChannel =
                emptyIfReadOnly(serviceMediator.getStateHolder().getCategoryStore().save(bean.getId(), bean))
                        .thenReturn(new CategoryCreateEvent(client, new Category(serviceMediator, bean)));

        return addChannelToGuild(serviceMediator.getStateHolder().getGuildStore(), channel, guildId)
                .then(saveChannel);
    }

    private Mono<Void> addChannelToGuild(LongObjStore<GuildBean> guildStore, GatewayChannelResponse channel,
            long guildId) {
        return guildStore
                .find(guildId)
                .doOnNext(guild -> guild.setChannels(ArrayUtil.add(guild.getChannels(), channel.getId())))
                .flatMap(guild -> emptyIfReadOnly(guildStore.save(guild.getId(), guild)));
    }

    Mono<? extends Event> channelDelete(DispatchContext<ChannelDelete> context) {
        Channel.Type type = Channel.Type.of(context.getDispatch().getChannel().getType());

        switch (type) {
            case GUILD_TEXT:
                return textChannelDelete(context);
            case DM:
                return privateChannelDelete(context);
            case GUILD_VOICE:
                return voiceChannelDelete(context);
            case GROUP_DM:
                throw new UnsupportedOperationException("Received channel_delete for a group on a bot account!");
            case GUILD_CATEGORY:
                return categoryDeleteEvent(context);
            default:
                throw new AssertionError();
        }
    }

    private Mono<TextChannelDeleteEvent> textChannelDelete(DispatchContext<ChannelDelete> context) {
        StateHolder stateHolder = context.getServiceMediator().getStateHolder();
        DiscordClient client = context.getServiceMediator().getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        TextChannelBean bean = new TextChannelBean(channel, guildId);

        Mono<TextChannelDeleteEvent> deleteChannel = emptyIfReadOnly(stateHolder.getTextChannelStore()
                .delete(bean.getId()))
                .thenReturn(new TextChannelDeleteEvent(client, new TextChannel(context.getServiceMediator(), bean)));

        return removeChannelFromGuild(stateHolder.getGuildStore(), channel, guildId)
                .then(deleteChannel);
    }

    private Mono<PrivateChannelDeleteEvent> privateChannelDelete(DispatchContext<ChannelDelete> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = context.getServiceMediator().getClient();
        PrivateChannelBean bean = new PrivateChannelBean(context.getDispatch().getChannel());

        return Mono.just(new PrivateChannelDeleteEvent(client, new PrivateChannel(serviceMediator, bean)));

    }

    private Mono<VoiceChannelDeleteEvent> voiceChannelDelete(DispatchContext<ChannelDelete> context) {
        StateHolder stateHolder = context.getServiceMediator().getStateHolder();
        DiscordClient client = context.getServiceMediator().getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        VoiceChannelBean bean = new VoiceChannelBean(channel, guildId);

        Mono<VoiceChannelDeleteEvent> deleteChannel = emptyIfReadOnly(stateHolder.getVoiceChannelStore()
                .delete(bean.getId()))
                .thenReturn(new VoiceChannelDeleteEvent(client, new VoiceChannel(context.getServiceMediator(), bean)));

        return removeChannelFromGuild(stateHolder.getGuildStore(), channel, guildId)
                .then(deleteChannel);
    }

    private Mono<CategoryDeleteEvent> categoryDeleteEvent(DispatchContext<ChannelDelete> context) {
        StateHolder stateHolder = context.getServiceMediator().getStateHolder();
        DiscordClient client = context.getServiceMediator().getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        CategoryBean bean = new CategoryBean(channel, guildId);

        Mono<CategoryDeleteEvent> deleteChannel = emptyIfReadOnly(stateHolder.getCategoryStore()
                .delete(bean.getId()))
                .thenReturn(new CategoryDeleteEvent(client, new Category(context.getServiceMediator(), bean)));

        return removeChannelFromGuild(stateHolder.getGuildStore(), channel, guildId)
                .then(deleteChannel);
    }

    private Mono<Void> removeChannelFromGuild(LongObjStore<GuildBean> guildStore, GatewayChannelResponse channel,
            long guildId) {
        return guildStore
                .find(guildId)
                .doOnNext(guild -> guild.setChannels(ArrayUtil.remove(guild.getChannels(), channel.getId())))
                .flatMap(guild -> emptyIfReadOnly(guildStore.save(guild.getId(), guild)));
    }

    Mono<PinsUpdateEvent> channelPinsUpdate(DispatchContext<ChannelPinsUpdate> context) {
        long channelId = context.getDispatch().getChannelId();
        Instant timestamp = context.getDispatch().getLastPinTimestamp() == null ? null :
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(context.getDispatch().getLastPinTimestamp(),
                        Instant::from);

        return Mono.just(new PinsUpdateEvent(context.getServiceMediator().getClient(), channelId, timestamp));
    }

    Mono<? extends Event> channelUpdate(DispatchContext<ChannelUpdate> context) {
        Channel.Type type = Channel.Type.of(context.getDispatch().getChannel().getType());

        switch (type) {
            case GUILD_TEXT:
                return textChannelUpdate(context);
            case DM:
                throw new UnsupportedOperationException("Received channel_update for a DM on a bot account!");
            case GUILD_VOICE:
                return voiceChannelUpdate(context);
            case GROUP_DM:
                throw new UnsupportedOperationException("Received channel_update for a group on a bot account!");
            case GUILD_CATEGORY:
                return categoryUpdateEvent(context);
            default:
                throw new AssertionError();
        }
    }

    private Mono<TextChannelUpdateEvent> textChannelUpdate(DispatchContext<ChannelUpdate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        TextChannelBean bean = new TextChannelBean(channel, guildId);
        TextChannel current = new TextChannel(context.getServiceMediator(), bean);

        Mono<Void> saveNew = emptyIfReadOnly(serviceMediator.getStateHolder().getTextChannelStore()
                .save(bean.getId(), bean));

        return serviceMediator.getStateHolder().getTextChannelStore()
                .find(bean.getId())
                .flatMap(saveNew::thenReturn)
                .map(old -> new TextChannelUpdateEvent(client, current, new TextChannel(serviceMediator, old)))
                .switchIfEmpty(saveNew.thenReturn(new TextChannelUpdateEvent(client, current, null)));
    }

    private Mono<VoiceChannelUpdateEvent> voiceChannelUpdate(DispatchContext<ChannelUpdate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        VoiceChannelBean bean = new VoiceChannelBean(channel, guildId);
        VoiceChannel current = new VoiceChannel(context.getServiceMediator(), bean);

        Mono<Void> saveNew = emptyIfReadOnly(serviceMediator.getStateHolder().getVoiceChannelStore()
                .save(bean.getId(), bean));

        return serviceMediator.getStateHolder().getVoiceChannelStore()
                .find(bean.getId())
                .flatMap(saveNew::thenReturn)
                .map(old -> new VoiceChannelUpdateEvent(client, current, new VoiceChannel(serviceMediator, old)))
                .switchIfEmpty(saveNew.thenReturn(new VoiceChannelUpdateEvent(client, current, null)));
    }

    private Mono<CategoryUpdateEvent> categoryUpdateEvent(DispatchContext<ChannelUpdate> context) {
        ServiceMediator serviceMediator = context.getServiceMediator();
        DiscordClient client = serviceMediator.getClient();
        GatewayChannelResponse channel = context.getDispatch().getChannel();
        long guildId = context.getDispatch().getGuildId();
        CategoryBean bean = new CategoryBean(channel, guildId);
        Category current = new Category(context.getServiceMediator(), bean);

        Mono<Void> saveNew = emptyIfReadOnly(serviceMediator.getStateHolder().getCategoryStore()
                .save(bean.getId(), bean));

        return serviceMediator.getStateHolder().getCategoryStore()
                .find(bean.getId())
                .flatMap(saveNew::thenReturn)
                .map(old -> new CategoryUpdateEvent(client, current, new Category(serviceMediator, old)))
                .switchIfEmpty(saveNew.thenReturn(new CategoryUpdateEvent(client, current, null)));
    }

}
