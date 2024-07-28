package org.platkmframework.cgenerator.cli.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliConfiguration {
	
	private String token;
	private OnPromiseConfiguration onPromiseConfiguration;
	private List<GenConfiguration> genConfigurations;
	private Map<String, String> globaConfigurations;
	private Map<String, String> mapData;
	private Map<String, String> globalProperties;
    
	public List<GenConfiguration> getGenConfigurations() {
		return genConfigurations;
	}
	public void setGenConfigurations(List<GenConfiguration> genConfigurations) {
		this.genConfigurations = genConfigurations;
	}

	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public OnPromiseConfiguration getOnPromiseConfiguration() {
		return onPromiseConfiguration;
	}
	public void setOnPromiseConfiguration(OnPromiseConfiguration onPromiseConfiguration) {
		this.onPromiseConfiguration = onPromiseConfiguration;
	}
	public Map<String, String> getGlobaConfigurations() {
		return globaConfigurations;
	}
	
	public void setGlobaConfigurations(Map<String, String> globaConfigurations) {
		this.globaConfigurations = globaConfigurations;
	}
	public Map<String, String> getMapData() {
		if(this.mapData == null) this.mapData = new HashMap<>();
		return mapData;
	}
	
	public void setMapData(Map<String, String> mapData) {
		this.mapData = mapData;
	}
	
	public Map<String, String> getGlobalProperties() {
		return globalProperties;
	}
	
	public void setGlobalProperties(Map<String, String> globalProperties) {
		if(this.globalProperties == null)
			this.globalProperties = new HashMap<>();
		this.globalProperties = globalProperties;
	}

}
