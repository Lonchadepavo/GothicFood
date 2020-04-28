package com.loncha.gothicfood;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;

public class FoodRestrict implements Listener {
	//Dos Hashmap, uno para controlar lo último que has comido y otro para controlar cuantas veces lo has comido
	HashMap<Player, String[]> datosComida = new HashMap<Player, String[]>(); //0 = nombre, 1 = ultima_comida, 2 = valor_ultima_comida,3 = comida_saturada, 4 = valor_reset, 5 = tiempo_desde_saturacion
	final int valorSaturacion = 20;
	FileConfiguration playerYaml;
	
	ControladorAlergias ca;
	
	public void checkPlayerData(Player p, ControladorAlergias ca) throws IOException, InvalidConfigurationException {
		this.ca = ca;
		String dataFolderName = p.getName()+"FoodData.yml";
		File originalDataFile = new File("plugins/GothicFood/playerdata/playerdatacopy.yml");
		File dataFoodFile = new File("plugins/GothicFood/playerdata/"+p.getName()+"FoodData.yml");		
		
		//Comprueba si el archivo de datos del player existe, si no existe lo crea y rellena el hashmap con datos default.
		if (!dataFoodFile.exists()) {
			Files.copy(originalDataFile.toPath(), dataFoodFile.toPath());
			
			//Crea un array temporal para guardar datos placeholder en el hashmap
			String[] tempDataArray = new String[7];
			for (int i = 0; i < tempDataArray.length; i++) {
				tempDataArray[i] = "0";
			}
			datosComida.put(p,tempDataArray);
			saveData(datosComida, p);
			
		} else {
			//Carga los datos existentes en el hashmap
			loadData(datosComida, p);
		}
		
		ca.setAlergias(this, p);
	}
	
	//Evento que detecta cuando un player come algo
	@EventHandler
	public void onPlayerEat(PlayerItemConsumeEvent e) throws InvalidConfigurationException {
		Player p = e.getPlayer();
		ItemStack lastEaten = e.getItem();
		
		//Si es un item de comida custom le pasa el nombre custom
		if (lastEaten.getItemMeta().getDisplayName() != null) {
			comprobarComida(lastEaten.getItemMeta().getDisplayName(), p);
			ca.checkAlergias(lastEaten.getItemMeta().getDisplayName(), p);
		} 
		
		//Si es un item de comida vanilla le pasa el type
		else {
			comprobarComida(lastEaten.getType().toString(), p);
			ca.checkAlergias(lastEaten.getType().toString(), p);

		}
		
	}
	
	public void comprobarComida(String comida, Player p) throws InvalidConfigurationException {
		String ultimaComida = datosComida.get(p)[1];
		String comidaSaturada = datosComida.get(p)[3];
		int valorResetSaturacion = Integer.parseInt(datosComida.get(p)[4]);
		int valorUltimaComida = Integer.parseInt(datosComida.get(p)[2]);
				
		//Si lo que acabas de comer es igual a lo último que comiste
		if (comida.equalsIgnoreCase(ultimaComida) && !comida.equalsIgnoreCase(comidaSaturada)) {
			//Se aumenta el número de veces que has comido esa comida y se guarda en el hashmap
			valorUltimaComida++;
			datosComida.put(p, new String[] {p.getName(), comida, String.valueOf(valorUltimaComida), datosComida.get(p)[3], datosComida.get(p)[4], datosComida.get(p)[5], datosComida.get(p)[6]});
			saveData(datosComida, p);
			
			switch (valorUltimaComida) {
				case (int) valorSaturacion/4:
					
					p.sendMessage("La comida te sienta bien");
					break;
					
				case (int) valorSaturacion/2:
					
					p.sendMessage("Empiezas a estar bastante cansado de esa comida...");
					break;
					
				default:
					if (valorUltimaComida >= valorSaturacion) {	
						p.sendMessage(ChatColor.RED + "Ya no puedes comer más comida de ese tipo, empiezas a vomitar...");
						p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 7));
						p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 7));
						
						datosComida.put(p, new String[] {p.getName(), comida, String.valueOf(valorUltimaComida), comida, String.valueOf(0), datosComida.get(p)[5], datosComida.get(p)[6]});				
					}
					break;
			}
			
		} else if (!comida.equalsIgnoreCase(ultimaComida) && !comida.equalsIgnoreCase(comidaSaturada)) {
			//Si lo que acabas de comer no es igual a lo último que comiste, se guarda la nueva comida y se establece el contador a 1	
			datosComida.put(p, new String[] {p.getName(), comida, String.valueOf(1), datosComida.get(p)[3], String.valueOf(valorResetSaturacion), datosComida.get(p)[5], datosComida.get(p)[6]});
					
		} else if (comida.equalsIgnoreCase(comidaSaturada)) {
			valorUltimaComida++;
			if (valorUltimaComida >= valorSaturacion+10) {
				String[] arrAlergiasTemp = datosComida.get(p)[5].split(" ");
				String[] arrNivelesAlergiasTemp = datosComida.get(p)[6].split(" ");
				
				ArrayList<String> tempAlergias = new ArrayList<String>(Arrays.asList(arrAlergiasTemp));
				ArrayList<String> tempNivelesAlergias = new ArrayList<String>(Arrays.asList(arrNivelesAlergiasTemp));
				
				if (!tempAlergias.contains(comida)) {
					tempAlergias.add(comida);
					tempNivelesAlergias.add("LEVE");
					
					String[] tempArrAlergias = tempAlergias.toArray(new String[tempAlergias.size()]);
					String[] tempArrNivelesAlergias = tempNivelesAlergias.toArray(new String[tempNivelesAlergias.size()]);
					
					String alergiasEnString = Arrays.toString(tempArrAlergias);
					String nivelesAlergiasEnString = Arrays.toString(tempArrNivelesAlergias);
					
					alergiasEnString = alergiasEnString.substring(1, alergiasEnString.length()-1).replace(",", "");
					nivelesAlergiasEnString = nivelesAlergiasEnString.substring(1, nivelesAlergiasEnString.length()-1).replace(",", "");

					datosComida.put(p, new String[] {p.getName(), comida, String.valueOf(valorUltimaComida), comida, String.valueOf(0), alergiasEnString, nivelesAlergiasEnString});
					p.sendMessage(ChatColor.RED + "Has contraído una nueva alergia");
				}
			} else if (valorUltimaComida < valorSaturacion+1) { 
				p.sendMessage(ChatColor.RED + "Ya no puedes comer más comida de ese tipo, empiezas a vomitar...");
				p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 7));
				p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 7));
				
				datosComida.put(p, new String[] {p.getName(), comida, String.valueOf(valorUltimaComida), comida, String.valueOf(0), datosComida.get(p)[5], datosComida.get(p)[6]});
				valorResetSaturacion = Integer.parseInt(datosComida.get(p)[4]);
			}
		}

		if (!comidaSaturada.equals(" ") && !comidaSaturada.contentEquals("0")) {
			valorResetSaturacion++;
		}
		
		if (valorResetSaturacion >= valorSaturacion) {
			valorResetSaturacion = 0;
			datosComida.put(p, new String[] {p.getName(), datosComida.get(p)[1], datosComida.get(p)[2], " ", String.valueOf(valorResetSaturacion), datosComida.get(p)[5], datosComida.get(p)[6]});
			
		} else {
			datosComida.put(p, new String[] {p.getName(), datosComida.get(p)[1], datosComida.get(p)[2], datosComida.get(p)[3], String.valueOf(valorResetSaturacion), datosComida.get(p)[5], datosComida.get(p)[6]});
		}
		
		saveData(datosComida, p);
	}
	
	//Método para guardar los datos en el .yml correspondiente
	public boolean saveData(HashMap<Player, String[]> hm, Player p) throws InvalidConfigurationException {
		
		try {
			String[] tempDataArray = hm.get(p); //Pasa los datos a un array temporal
			String[] tempAlergiasArray = tempDataArray[5].split(" ");
			String[] tempNivelAlergiasArray = tempDataArray[6].split(" ");
			
			//Crea un writer y coge la ruta del archivo para guardarlo después
			File dataFoodFile = new File("plugins/GothicFood/playerdata/"+p.getName()+"FoodData.yml");
			
			playerYaml = new YamlConfiguration();
			playerYaml.load(dataFoodFile);
			
			Yaml dataYaml = new Yaml();
			FileWriter writer = new FileWriter(dataFoodFile);
			getCustomConfig().set("player.nombre", tempDataArray[0]);
			getCustomConfig().set("player.ultima_comida", tempDataArray[1]);
			getCustomConfig().set("player.valor_ultima_comida", tempDataArray[2]);
			getCustomConfig().set("player.comida_saturada", tempDataArray[3]);
			getCustomConfig().set("player.valor_reset", tempDataArray[4]);
			getCustomConfig().set("player.alergias", tempAlergiasArray);
			getCustomConfig().set("player.nivel_alergias", tempNivelAlergiasArray);
			getCustomConfig().save(dataFoodFile);
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	//Método para cargar los datos de un .yml en el hashmap correspondiente
	public boolean loadData(HashMap<Player, String[]> hm, Player p) {
		try {
			String[] tempDataArray = new String[7]; //Crea un array temporal
			
			//Crea un writer y coge la ruta del archivo para guardarlo después
			File dataFoodFile = new File("plugins/GothicFood/playerdata/"+p.getName()+"FoodData.yml");
			
			playerYaml = new YamlConfiguration();
			playerYaml.load(dataFoodFile);
			
			List<String> tempAlergias;
			List<String> tempNivelAlergias;
			
			//Recorre el yaml y guarda en un array temporal toda la información
			tempDataArray[0] = getCustomConfig().getString("player.nombre");
			tempDataArray[1] = getCustomConfig().getString("player.ultima_comida");
			tempDataArray[2] = getCustomConfig().getString("player.valor_ultima_comida");
			tempDataArray[3] = getCustomConfig().getString("player.comida_saturada");
			tempDataArray[4] = getCustomConfig().getString("player.valor_reset");
			tempAlergias = getCustomConfig().getStringList("player.alergias");
			tempNivelAlergias = getCustomConfig().getStringList("player.nivel_alergias");
			
			String[] tempAlergiasArray = tempAlergias.toArray(new String[tempAlergias.size()]);
			String[] tempNivelAlergiasArray = tempNivelAlergias.toArray(new String[tempNivelAlergias.size()]);
			
			String str1, str2;
			
			str1 = Arrays.toString(tempAlergiasArray);
			str2 = Arrays.toString(tempNivelAlergiasArray);
			
			tempDataArray[5] = str1.substring(1, str1.length()-1).replace(",", "");
			tempDataArray[6] = str2.substring(1, str2.length()-1).replace(",", "");
			
			//Guarda el array en el hashmap correspondiente
			hm.put(p,tempDataArray);
			p.sendMessage("Datos cargados con éxito");
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			p.sendMessage("no se han cargado los datos");
			return false;
		}
	}
	
	public HashMap getDatosComida() {
		return datosComida;
	}
	
	public void setDatosComida(HashMap <Player,String[]> hm) {
		datosComida = hm;
	}
	
	//Método para establecer un .yml como archivo de configuración para acceder a el
	public FileConfiguration getCustomConfig() {
		return this.playerYaml;
	}
}
