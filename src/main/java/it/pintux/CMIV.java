package it.pintux;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;

import java.util.Optional;

@Plugin(
        id = "cmiv",
        name = "CMIV",
        version = "1.1",
        authors = {"pintux"}
)
public class CMIV {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    private final MinecraftChannelIdentifier outgoing = MinecraftChannelIdentifier.from("cmib:fromproxy");
    private final MinecraftChannelIdentifier identifier = MinecraftChannelIdentifier.from("cmib:fromserver");

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        logger.info("Initializing plugin CMI Velocity");
        server.getEventManager().register(this, PluginMessageEvent.class, event -> {
            if (event.getIdentifier().equals(identifier)) {
                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                String subChannel = in.readUTF();

                if (subChannel.equalsIgnoreCase("CMIServerListRequest")) {
                    for (RegisteredServer registeredServer : this.server.getAllServers()) {
                        if (registeredServer.getPlayersConnected().isEmpty()) {
                            continue;
                        }
                        ServerInfo server = registeredServer.getServerInfo();
                        String info = server.getName() + ";:" + server.getAddress().getAddress().getHostAddress() + ";:" + server.getAddress().getPort() + ";:" + "CorrySamu";
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("ServerListFeedback");
                        out.writeUTF(server.getName());
                        out.writeUTF(info);
                        Player player = registeredServer.getPlayersConnected().iterator().next();
                        if (player.getCurrentServer().isPresent() && player.getCurrentServer().get().getServerInfo() != null)
                            player.getCurrentServer().get().sendPluginMessage(outgoing, out.toByteArray());
                    }
                } else if (subChannel.equalsIgnoreCase("CMIPlayerListRequest")) {
                    for (RegisteredServer registeredServer : this.server.getAllServers()) {
                        if (registeredServer.getPlayersConnected().isEmpty()) {
                            continue;
                        }
                        ServerInfo server = registeredServer.getServerInfo();
                        StringBuilder players = new StringBuilder();
                        for (Player oneP : registeredServer.getPlayersConnected()) {
                            if (!players.toString().isEmpty())
                                players.append(";;");
                            players.append(oneP.getUsername()).append("::").append(oneP.getUniqueId().toString());
                        }
                        String info = registeredServer.getServerInfo().getName() + ";:" + players.toString();
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("PlayerListFeedback");
                        out.writeUTF(server.getName());
                        out.writeUTF(info);
                        Player player = registeredServer.getPlayersConnected().iterator().next();
                        if (player.getCurrentServer().isPresent() && player.getCurrentServer().get().getServerInfo() != null)
                            player.getCurrentServer().get().sendPluginMessage(outgoing, out.toByteArray());
                    }
                }
            }
        });
    }

    @Subscribe
    public void onConnect(ServerConnectedEvent event) {
        if (event.getPreviousServer().isPresent()) {
            sendServerSwitchEvent(event.getPlayer(), event.getPreviousServer().get().getServerInfo(), event.getServer().getServerInfo());
        } else {
            sendServerEvent(event.getPlayer(), event.getServer().getServerInfo().getName(), "ServerConnectEvent");
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        String serverName = "unknown";
        if (event.getPlayer() == null)
            return;
        Optional<ServerConnection> serverConnection = event.getPlayer().getCurrentServer();

        if (serverConnection.isPresent()) {
            ServerInfo info = serverConnection.get().getServerInfo();
            if (info != null && info.getName() != null)
                serverName = info.getName();
        }
        sendServerEvent(event.getPlayer(), serverName, "PlayerDisconnectEvent");
    }


    public void sendServerEvent(Player player, String serverName, String eventType) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(eventType);
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(player.getUsername());
        out.writeUTF(serverName);
        for (RegisteredServer registeredServer : this.server.getAllServers()) {
            registeredServer.sendPluginMessage(outgoing, out.toByteArray());
        }
    }

    public void sendServerSwitchEvent(Player player, ServerInfo serverFrom, ServerInfo serverTo) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CMIServerSwitchEvent");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(player.getUsername());
        out.writeUTF((serverFrom == null) ? "" : serverFrom.getName());
        out.writeUTF((serverTo == null) ? "" : serverTo.getName());
        for (RegisteredServer registeredServer : this.server.getAllServers()) {
            registeredServer.sendPluginMessage(outgoing, out.toByteArray());
        }
    }
}
