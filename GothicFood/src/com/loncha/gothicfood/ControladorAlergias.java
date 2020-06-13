package com.loncha.gothicfood;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ControladorAlergias implements Listener {
	//Referencias
	Main plugin;
	FileConfiguration playerYaml;
	FoodRestrict fd;
	
	//Máximo de alergias por jugador
	int intMaxAlergias;
	
	//Probabilidades
	Integer[] probabilidadAlergias;
	Integer[] probabilidadNivelAlergias;
	
	//Tipos de alergias y mensajes de cada una
	String[] arrNiveles = new String[] {"LEVE", "GRAVE", "MORTAL"};
	String[] mensajesAlergias = new String[] {ChatColor.RED+"Lo que acabas de comer te ha sentado un poco mal...", ChatColor.RED+"Te entran fuertes mareos y dolores después de comer eso", ChatColor.DARK_RED + "Casi no lo cuentas, nunca más vuelvas a probar esa comida"};
	
	static String[] listaComidas;
	
	//Constructor que coge el main como parámetro
	ControladorAlergias(Main p) {
		plugin = p;
		cargarDatosAlergias();
	}
	
	//Método encargado de establecer las alergias a un jugador
	public void setAlergias(FoodRestrict fd, Player p) throws FileNotFoundException, IOException, InvalidConfigurationException {
		this.fd = fd;
		String[] arrAlergias;
		String[] arrNivelAlergias;
		ArrayList<String> listaAlergias = new ArrayList<String>(Arrays.asList(listaComidas));
		
		//Se establece de manera aleatoria cuantas alergias tendrá el player.
		//La probabilidad de tener alergias y cuantas se pueden tener es algo que se puede configurar en en el config.yml
		int tempResultado = (int)(Math.random() * 100) + 1;
		int intNAlergias = 0;
		
		int tempMenor = 1;
		for (int i = 0; i < probabilidadAlergias.length; i++) {
			if (tempResultado > tempMenor && tempResultado <= (probabilidadAlergias[i]+tempMenor)) {
				intNAlergias = i;
			}
			tempMenor += probabilidadAlergias[i];
		}
		
		arrAlergias = new String[intNAlergias];
		arrNivelAlergias = new String[intNAlergias];
		
		//Establece que alergias tendrá el jugador
		for (int i = 0; i < intNAlergias; i++) {
			int alergiaSeleccionada = (int)(Math.random() * listaAlergias.size());
			arrAlergias[i] = listaAlergias.get(alergiaSeleccionada);
			listaAlergias.remove(alergiaSeleccionada);
			
			tempResultado = (int)(Math.random() * 100) + 1;

			if (tempResultado < probabilidadNivelAlergias[0]) {
				arrNivelAlergias[i] = "LEVE";
				
			} else if (tempResultado >= probabilidadNivelAlergias[0] && tempResultado < probabilidadNivelAlergias[1]+probabilidadNivelAlergias[0]) {
				arrNivelAlergias[i] = "GRAVE";
				
			} else if (tempResultado >= probabilidadNivelAlergias[1]+probabilidadNivelAlergias[0] && tempResultado < probabilidadNivelAlergias[2]+probabilidadNivelAlergias[1]+probabilidadNivelAlergias[0]) {
				arrNivelAlergias[i] = "MORTAL";
			}

		}
		
		if (!p.hasPlayedBefore()) {
			File dataFoodFile = new File("plugins/GothicFood/playerdata/"+p.getName()+"FoodData.yml");
			
			playerYaml = new YamlConfiguration();
			playerYaml.load(dataFoodFile);
			
			getCustomConfig().set("player.alergias", arrAlergias);
			getCustomConfig().set("player.nivel_alergias", arrNivelAlergias);
			getCustomConfig().save(dataFoodFile);
			
			String alergiasEnString = Arrays.toString(arrAlergias), nivelAlergiasEnString = Arrays.toString(arrNivelAlergias);
			alergiasEnString = alergiasEnString.substring(1, alergiasEnString.length()-1).replace(",", "");
			nivelAlergiasEnString = nivelAlergiasEnString.substring(1, nivelAlergiasEnString.length()-1).replace(",", "");
	
			fd.datosComida.put(p, new String[] {fd.datosComida.get(p)[0], fd.datosComida.get(p)[1], fd.datosComida.get(p)[2], fd.datosComida.get(p)[3], fd.datosComida.get(p)[4], alergiasEnString, nivelAlergiasEnString});	
		}
	}
	
	//Método encargado de comprobar las alergias de un jugador (para ver si tienes que ponerle efectos o no)
	public void checkAlergias(String comida, Player p) {
		String[] tempAlergias, tempNivelAlergias;
		tempAlergias = fd.datosComida.get(p)[5].split(" ");
		tempNivelAlergias = fd.datosComida.get(p)[6].split(" ");
		
		List<String> arrEfectosAlergias;
		
		for (int i = 0; i < tempAlergias.length; i++) {
			if (comida.equals(tempAlergias[i])) {
				if (plugin.getConfig().contains("nivel_alergias."+comida)) {
					arrEfectosAlergias = plugin.getConfig().getStringList("nivel_alergias."+comida+"."+tempNivelAlergias[i]);
					if (tempNivelAlergias[i].equals("LEVE")) {
						p.sendMessage(mensajesAlergias[0]);
					} else if (tempNivelAlergias[i].equals("GRAVE")) {
						p.sendMessage(mensajesAlergias[1]);
					} else {
						p.sendMessage(mensajesAlergias[2]);
					}
					
					for (int k = 0 ; k < arrEfectosAlergias.size(); k++) {
						String[] currentEffectLine = arrEfectosAlergias.get(k).split(" ");
						String currentEffect = currentEffectLine[0], currentEffectTime = currentEffectLine[1], currentEffectLevel = currentEffectLine[2];
						
						p.addPotionEffect(new PotionEffect(PotionEffectType.getByName(currentEffect), Integer.parseInt(currentEffectTime), Integer.parseInt(currentEffectLevel)));
						
					}
					
				} else {
					arrEfectosAlergias = plugin.getConfig().getStringList("nivel_alergias.default."+tempNivelAlergias[i]);
					if (tempNivelAlergias[i].equals("LEVE")) {
						p.sendMessage(mensajesAlergias[0]);
					} else if (tempNivelAlergias[i].equals("GRAVE")) {
						p.sendMessage(mensajesAlergias[1]);
					} else {
						p.sendMessage(mensajesAlergias[2]);
					}
					
					for (int k = 0 ; k < arrEfectosAlergias.size(); k++) {
						String[] currentEffectLine = arrEfectosAlergias.get(k).split(" ");
						String currentEffect = currentEffectLine[0], currentEffectTime = currentEffectLine[1], currentEffectLevel = currentEffectLine[2];
						
						p.addPotionEffect(new PotionEffect(PotionEffectType.getByName(currentEffect), Integer.parseInt(currentEffectTime), Integer.parseInt(currentEffectLevel)));
						
					}
					
				}
			}
		}
	}
	
	//Método para establecer un .yml como archivo de configuración para acceder a el
	public FileConfiguration getCustomConfig() {
		return this.playerYaml;
	}
	
	//Cargar datos de las alergias desde el archivo de configuración.
	public void cargarDatosAlergias() {
		//Cargamos la lista de comidas a las que puedes ser alérgico
		List<String> tempComidas = plugin.getConfig().getStringList("comidas");
		ControladorAlergias.listaComidas = tempComidas.toArray(new String[tempComidas.size()]);
		
		//Se guarda el total de alergias aleatorias por jugador
		intMaxAlergias = plugin.getConfig().getInt("max_alergias");
		
		//Se guarda el porcentaje para cada alergia
		List<Integer> tempProbabilidades = plugin.getConfig().getIntegerList("probabilidad_alergias");
		probabilidadAlergias = tempProbabilidades.toArray(new Integer[tempProbabilidades.size()]);
		
		//Se guarda el porcentaje para cada nivel de alergias
		tempProbabilidades = plugin.getConfig().getIntegerList("probabilidad_nivel_alergias");
		probabilidadNivelAlergias = tempProbabilidades.toArray(new Integer[tempProbabilidades.size()]);
	}
 	
}
