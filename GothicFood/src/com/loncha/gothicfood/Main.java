package com.loncha.gothicfood;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	FoodRestrict fd;
	ControladorAlergias ca;
	
	FileConfiguration configFile;
	
	@Override
	public void onEnable() {
		fd = new FoodRestrict();
		ca = new ControladorAlergias(this);
		
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(this.fd, this);
		getServer().getPluginManager().registerEvents(this.ca, this);
		
		loadConfig();
		
		File dataFolder = new File("plugins/GothicFood/playerdata");
		File dataParent = new File(dataFolder.getParent());
		
		if (!dataParent.exists()) dataParent.mkdir();
		if (!dataFolder.exists()) dataFolder.mkdir();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) throws IOException, InvalidConfigurationException {
		Player p = e.getPlayer();
		
		fd.checkPlayerData(p, ca);
	}
	
	public FoodRestrict getFoodRestrict() {
		return fd;
	}
	
	public void loadConfig() {
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	//Método para establecer un .yml como archivo de configuración para acceder a el
	public FileConfiguration getCustomConfig() {
		return this.configFile;
	}

}
