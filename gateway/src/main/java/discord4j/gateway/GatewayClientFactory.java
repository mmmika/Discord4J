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

package discord4j.gateway;

import discord4j.gateway.payload.PayloadReader;
import discord4j.gateway.payload.PayloadWriter;
import discord4j.gateway.retry.RetryOptions;

/**
 * An abstract factory to create a {@link discord4j.gateway.GatewayClient}, allowing clients to send and
 * receive Discord Gateway events from this node.
 */
public interface GatewayClientFactory {

    /**
     * Create a Gateway client, supporting real-time events from Discord.
     *
     * @param payloadReader a deserialization strategy to read inbound payloads
     * @param payloadWriter a serialization strategy to write outbound payloads
     * @param retryOptions a configurable retrying policy
     * @param token an authorization token to log into Discord Gateway
     * @param identifyOptions a configurable set of shard and presence parameters
     * @return a client allowing communication with Discord Gateway
     */
    GatewayClient getGatewayClient(PayloadReader payloadReader, PayloadWriter payloadWriter,
            RetryOptions retryOptions, String token, IdentifyOptions identifyOptions);
}
