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

package discord4j.core.event;

import discord4j.core.event.dispatch.DispatchHandlers;

/**
 * Default event mapping factory which uses {@link discord4j.core.event.dispatch.DispatchHandlers} as their strategy,
 * processing each inbound {@link discord4j.gateway.json.dispatch.Dispatch} event by writing to the current Store.
 */
public class DefaultEventMapperFactory implements EventMapperFactory {

    @Override
    public EventMapper getEventMapper() {
        return new DispatchHandlers(false);
    }
}
