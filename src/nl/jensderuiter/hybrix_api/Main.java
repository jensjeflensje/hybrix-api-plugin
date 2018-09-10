package nl.jensderuiter.hybrix_api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

public class Main extends JavaPlugin {

	HttpsServer server;

	private static final Logger log = Logger.getLogger("Minecraft");
	private static Economy econ = null;
	private static Permission perms = null;
	private static Chat chat = null;
	private File customConfigFile;
	private FileConfiguration customConfig;
	public static Main plugin;

	public void onEnable() {
		if (!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		try {
			createCustomConfig();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		try {
			SSLContext sslContext = SSLContextUtils.createSSLContext(
                    new FileInputStream(new File(getDataFolder(), "keystore.jks")), "changeit",
                    new FileInputStream(new File(getDataFolder(), "keystore.jks")), "changeit");
			server = HttpsServer.create(new InetSocketAddress(getCustomConfig().getInt("port")), 0);
			server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    params.setProtocols(new String[]{"TLSv1.1", "TLSv1.2"});
                    params.setNeedClientAuth(false);
                }
            });
			server.createContext("/playerinfo", new GetPlayerInfo());
			server.createContext("/verifyaccount", new VerifyAccount());
			server.createContext("/transfermoney", new TransferMoney());
			server.createContext("/changepassword", new ChangePassword());
			server.setExecutor(null);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		plugin = this;
	}

	public void onDisable() {
		server.stop(1);
	}

	static class GetPlayerInfo implements HttpHandler {
		@SuppressWarnings("deprecation")
		@Override
		public void handle(HttpExchange t) throws IOException {
			try {
				String paramsRaw = t.getRequestURI().getQuery();
				String[] code = paramsRaw.split("&")[0].split("=");
				String codeString = code[1];
				if (codeString.equalsIgnoreCase(plugin.getCustomConfig().getString("api-code"))) {
					String[] userParams = paramsRaw.split("&")[1].split("=");
					String response = econ.format(econ.getBalance(Bukkit.getServer().getOfflinePlayer(userParams[1])));
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
				} else {
					String response = "False";
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				String response = "False";
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		}
	}

	static class VerifyAccount implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			try {
				String paramsRaw = t.getRequestURI().getQuery();
				String[] code = paramsRaw.split("&")[0].split("=");
				String codeString = code[1];
				if (codeString.equalsIgnoreCase(plugin.getCustomConfig().getString("api-code"))) {
					String[] userParams = paramsRaw.split("&")[1].split("=");
					String[] codeParams = paramsRaw.split("&")[2].split("=");
					Bukkit.getServer().getConsoleSender().sendMessage("Er is om /verifyaccount gevraagd!");
					String response = "True";
					Player player = Bukkit.getServer().getPlayer(userParams[1]);
					TextComponent message = new TextComponent("Je hebt je account proberen te verifieren, ga naar deze link om te verifieren: http:// " + plugin.getCustomConfig().getString("website") + "/verify?username=" + player.getName() + "&code=" + codeParams[1]);
					message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://" + plugin.getCustomConfig().getString("website") + "/verify?username=" + player.getName() + "&code=" + codeParams[1]));
					player.spigot().sendMessage(message);
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
				} else {
					String response = "False";
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				String response = "False";
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		}
	}
	
	static class ChangePassword implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			try {
				String paramsRaw = t.getRequestURI().getQuery();
				String[] code = paramsRaw.split("&")[0].split("=");
				String codeString = code[1];
				if (codeString.equalsIgnoreCase(plugin.getCustomConfig().getString("api-code"))) {
					String[] userParams = paramsRaw.split("&")[1].split("=");
					String[] codeParams = paramsRaw.split("&")[2].split("=");
					Bukkit.getServer().getConsoleSender().sendMessage("Er is om /changepassword gevraagd!");
					String response = "True";
					Player player = Bukkit.getServer().getPlayer(userParams[1]);
					TextComponent message = new TextComponent("Je hebt wachtwoord proberen te veranderen, ga naar deze link om je wachtwoord te veranderen: http://" + plugin.getCustomConfig().getString("website") + "/changepasswordchoose?username=" + player.getName() + "&code=" + codeParams[1]);
					message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://" + plugin.getCustomConfig().getString("website") + "/changepasswordchoose?username=" + player.getName() + "&code=" + codeParams[1]));
					player.spigot().sendMessage(message);
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
				} else {
					String response = "False";
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				String response = "False";
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		}
	}

	static class TransferMoney implements HttpHandler {
		@SuppressWarnings("deprecation")
		@Override
		public void handle(HttpExchange t) throws IOException {
			try {
				Bukkit.getServer().getConsoleSender().sendMessage("Er is om /transfermoney gevraagd!");
				String paramsRaw = t.getRequestURI().getQuery();
				String[] code = paramsRaw.split("&")[0].split("=");
				String codeString = code[1];
				if (codeString.equalsIgnoreCase(plugin.getCustomConfig().getString("api-code"))) {
					String[] userParams = paramsRaw.split("&")[1].split("=");
					String[] user2Params = paramsRaw.split("&")[2].split("=");
					String[] moneyParams = paramsRaw.split("&")[3].split("=");
					OfflinePlayer[] offlinePlayers = Bukkit.getServer().getOfflinePlayers();
					Boolean player1Found = null;
					Boolean player2Found = null;
					for (OfflinePlayer p : offlinePlayers) {
						if (p.getName().trim().contains(userParams[1])) {
							player1Found = true;
						}
						if (p.getName().trim().contains(user2Params[1])) {
							player2Found = true;
						}
					}
					if (player1Found == true && player2Found == true) {
						EconomyResponse moneyResponse2 = econ.withdrawPlayer(userParams[1], Double.parseDouble(moneyParams[1]));
						EconomyResponse moneyResponse = econ.depositPlayer(user2Params[1], Double.parseDouble(moneyParams[1]));
						if (!moneyResponse.transactionSuccess()) {
							String response = "False";
							t.sendResponseHeaders(200, response.length());
							OutputStream os = t.getResponseBody();
							os.write(response.getBytes());
							os.close();
							return;
						}
						if (!moneyResponse2.transactionSuccess()) {
							String response = "False";
							t.sendResponseHeaders(200, response.length());
							OutputStream os = t.getResponseBody();
							os.write(response.getBytes());
							os.close();
							return;
						}
						String response = "True";
						t.sendResponseHeaders(200, response.length());
						OutputStream os = t.getResponseBody();
						os.write(response.getBytes());
						os.close();
					}
					
				} else {
					String response = "False";
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				String response = "False";
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		}
	}
	
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public static Economy getEconomy() {
		return econ;
	}

	public static Permission getPermissions() {
		return perms;
	}

	public static Chat getChat() {
		return chat;
	}

	public FileConfiguration getCustomConfig() {
		return customConfig;
	}

	private void createCustomConfig() throws InvalidConfigurationException {
		customConfigFile = new File(getDataFolder(), "config.yml");
		if (!customConfigFile.exists()) {
			customConfigFile.getParentFile().mkdirs();
			saveResource("config.yml", false);
		}

		customConfig = new YamlConfiguration();
		try {
			customConfig.load(customConfigFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}