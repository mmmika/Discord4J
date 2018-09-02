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
package discord4j.gateway.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import discord4j.common.jackson.PossibleModule;
import discord4j.common.jackson.UnknownPropertyHandler;
import discord4j.gateway.json.dispatch.PresenceUpdate;
import discord4j.gateway.json.dispatch.Ready;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PayloadDeserializationTest {

    private ObjectMapper mapper;

    @Before
    public void init() {
        mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .addHandler(new UnknownPropertyHandler(true))
                .registerModules(new PossibleModule(), new Jdk8Module());
    }

    @Test
    public void testIgnoringUnknownProperty() throws IOException {
        String input = "{\n" +
                "  \"t\": \"PRESENCE_UPDATE\",\n" +
                "  \"s\": 116597,\n" +
                "  \"op\": 0,\n" +
                "  \"d\": {\n" +
                "    \"user\": {\n" +
                "      \"id\": \"000000000000000000\"\n" +
                "    },\n" +
                "    \"status\": \"online\",\n" +
                "    \"roles\": [],\n" +
                "    \"nick\": null,\n" +
                "    \"guild_id\": \"000000000000000000\",\n" +
                "    \"game\": {\n" +
                "      \"type\": 2,\n" +
                "      \"timestamps\": {\n" +
                "        \"start\": 1533777632902,\n" +
                "        \"end\": 1533777837675\n" +
                "      },\n" +
                "      \"sync_id\": \"0000000000000000000000\",\n" +
                "      \"state\": \"0000000000000000\",\n" +
                "      \"session_id\": \"00000000000000000000000000000000\",\n" +
                "      \"party\": {\n" +
                "        \"id\": \"spotify:000000000000000000\"\n" +
                "      },\n" +
                "      \"name\": \"Spotify\",\n" +
                "      \"id\": \"0000000000000000\",\n" +
                "      \"flags\": 48,\n" +
                "      \"details\": \"00000000000000000\",\n" +
                "      \"created_at\": 1533777810345,\n" +
                "      \"assets\": {\n" +
                "        \"large_text\": \"00000000000000000\",\n" +
                "        \"large_image\": \"spotify:0000000000000000000000000000000000000000\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"activities\": [\n" +
                "      {\n" +
                "        \"type\": 2,\n" +
                "        \"timestamps\": {\n" +
                "          \"start\": 1533777632902,\n" +
                "          \"end\": 1533777837675\n" +
                "        },\n" +
                "        \"sync_id\": \"0000000000000000000000\",\n" +
                "        \"state\": \"0000000000000000\",\n" +
                "        \"session_id\": \"00000000000000000000000000000000\",\n" +
                "        \"party\": {\n" +
                "          \"id\": \"spotify:000000000000000000\"\n" +
                "        },\n" +
                "        \"name\": \"Spotify\",\n" +
                "        \"id\": \"0000000000000000\",\n" +
                "        \"flags\": 48,\n" +
                "        \"details\": \"00000000000000000\",\n" +
                "        \"created_at\": 1533777810345,\n" +
                "        \"assets\": {\n" +
                "          \"large_text\": \"00000000000000000\",\n" +
                "          \"large_image\": \"spotify:0000000000000000000000000000000000000000\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        GatewayPayload<PresenceUpdate> payload = mapper.readValue(input, GatewayPayload.class);
        assertNotNull(payload.getData());
    }

    @Test
    public void testDispatchReady() throws IOException {
        String input = "{\n" +
                "  \"t\": \"READY\",\n" +
                "  \"s\": 1,\n" +
                "  \"op\": 0,\n" +
                "  \"d\": {\n" +
                "    \"v\": 6,\n" +
                "    \"user_settings\": {},\n" +
                "    \"user\": {\n" +
                "      \"verified\": true,\n" +
                "      \"username\": \"Reacton\",\n" +
                "      \"mfa_enabled\": true,\n" +
                "      \"id\": \"344487830824943618\",\n" +
                "      \"email\": null,\n" +
                "      \"discriminator\": \"6221\",\n" +
                "      \"bot\": true,\n" +
                "      \"avatar\": \"bb1ac764222d6b3242d6a4f78214c9c9\"\n" +
                "    },\n" +
                "    \"session_id\": \"070e00aac4f437d4219501063559164c\",\n" +
                "    \"relationships\": [],\n" +
                "    \"private_channels\": [],\n" +
                "    \"presences\": [],\n" +
                "    \"guilds\": [\n" +
                "      {\n" +
                "        \"unavailable\": true,\n" +
                "        \"id\": \"135197118292819968\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"unavailable\": true,\n" +
                "        \"id\": \"346719828784185375\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"_trace\": [\n" +
                "      \"gateway-prd-main-qgwq\",\n" +
                "      \"discord-sessions-prd-1-12\"\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        GatewayPayload<Ready> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(0, payload.getOp().getRawOp());
        assertNotNull(payload.getData());
        assertEquals(6, payload.getData().getVersion());
    }

    @Test
    public void testHeartbeat() throws IOException {
        String input = "{\n" +
                "    \"op\": 1,\n" +
                "    \"d\": 251,\n" +
                "    \"s\": null,\n" +
                "    \"t\": null\n" +
                "}";
        GatewayPayload<Heartbeat> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(1, payload.getOp().getRawOp());
        assertNotNull(payload.getData());
        assertEquals(251, payload.getData().getSeq());
    }

    @Test
    public void testReconnect() throws IOException {
        String input = "{\n" +
                "    \"op\": 7,\n" +
                "    \"d\": null,\n" +
                "    \"s\": null,\n" +
                "    \"t\": null\n" +
                "}";
        GatewayPayload<?> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(7, payload.getOp().getRawOp());
        assertNull(payload.getData());
    }

    @Test
    public void testInvalidSession() throws IOException {
        String input = "{\n" +
                "    \"op\": 9,\n" +
                "    \"d\": false,\n" +
                "    \"s\": null,\n" +
                "    \"t\": null\n" +
                "}";
        GatewayPayload<InvalidSession> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(9, payload.getOp().getRawOp());
        assertNotNull(payload.getData());
        assertEquals(false, payload.getData().isResumable());
    }

    @Test
    public void testHello() throws IOException {
        String input = "{\n" +
                "    \"op\": 10,\n" +
                "    \"d\": {\n" +
                "        \"heartbeat_interval\": 45000,\n" +
                "        \"_trace\": [\"discord-gateway-prd-1-99\"]\n" +
                "    },\n" +
                "    \"s\": null,\n" +
                "    \"t\": null\n" +
                "}";
        GatewayPayload<Hello> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(10, payload.getOp().getRawOp());
        assertNotNull(payload.getData());
        assertEquals(45000, payload.getData().getHeartbeatInterval());
    }

    @Test
    public void testHeartbeatAck() throws IOException {
        String input = "{\n" +
                "    \"op\": 11,\n" +
                "    \"d\": null,\n" +
                "    \"s\": null,\n" +
                "    \"t\": null\n" +
                "}";
        GatewayPayload<?> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(11, payload.getOp().getRawOp());
        assertNull(payload.getData());
    }

    @Test
    public void testStatusUpdate() throws IOException {
        String input = "{\n" +
                "    \"op\": 3,\n" +
                "    \"d\":{\n" +
                "        \"since\": 91879201,\n" +
                "        \"game\": {\n" +
                "            \"name\": \"some_game\",\n" +
                "            \"type\": 0\n" +
                "        },\n" +
                "        \"status\": \"online\",\n" +
                "        \"afk\": false\n" +
                "    },\n" +
                "    \"s\":null,\n" +
                "    \"t\":null\n" +
                "}";
        GatewayPayload<StatusUpdate> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(3, payload.getOp().getRawOp());
        assertNotNull(payload.getData());
        assertTrue(payload.getData().toString().contains("since=91879201"));
        assertTrue(payload.getData().toString().contains("name=some_game"));
    }

    @Test
    public void testVoiceStateUpdate() throws IOException {
        String input = "{\n" +
                "    \"op\":4,\n" +
                "    \"d\":{\n" +
                "        \"guild_id\": \"41771983423143937\",\n" +
                "        \"channel_id\": \"127121515262115840\",\n" +
                "        \"self_mute\": false,\n" +
                "        \"self_deaf\": false\n" +
                "    },\n" +
                "    \"s\":null,\n" +
                "    \"t\":null\n" +
                "}";
        GatewayPayload<VoiceStateUpdate> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(4, payload.getOp().getRawOp());
        assertNotNull(payload.getData());
        assertTrue(payload.getData().toString().contains("guildId=41771983423143937"));
    }

    @Test
    public void testRequestGuildMembers() throws IOException {
        String input = "{\n" +
                "    \"op\":8,\n" +
                "    \"d\":{\n" +
                "        \"guild_id\": \"41771983444115456\",\n" +
                "        \"query\": \"\",\n" +
                "        \"limit\": 0\n" +
                "    },\n" +
                "    \"s\":null,\n" +
                "    \"t\":null\n" +
                "}";
        GatewayPayload<RequestGuildMembers> payload = mapper.readValue(input, GatewayPayload.class);

        assertEquals(8, payload.getOp().getRawOp());
        assertNotNull(payload.getData());
        assertTrue(payload.getData().toString().contains("guildId=41771983444115456"));
    }
}
