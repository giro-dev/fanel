package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.api.PeerRuntime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class WgDumpParser {
    private WgDumpParser() { }

    public static List<PeerRuntime> parse(String dump) {
        String[] lines = dump.split("\\R");
        List<PeerRuntime> peers = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            String[] columns = lines[i].split("\\t", -1);
            if (columns.length < 8) throw new IllegalArgumentException("Malformed wg dump peer line");
            long handshake = Long.parseLong(columns[4]);
            peers.add(new PeerRuntime(
                    columns[0],
                    "(none)".equals(columns[2]) ? null : columns[2],
                    "(none)".equals(columns[3]) || columns[3].isBlank() ? List.of() : List.of(columns[3].split(",")),
                    handshake == 0 ? null : Instant.ofEpochSecond(handshake),
                    Long.parseLong(columns[5]),
                    Long.parseLong(columns[6]),
                    "off".equals(columns[7]) ? null : Integer.valueOf(columns[7])));
        }
        return List.copyOf(peers);
    }
}
