package org.platkmframework.cgenerator.cli.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliConfiguration {
	
	private String token;
	private OnPromiseConfiguration onPromiseConfiguration;
	private List<Artifact> artifacts;
	private Map<String, String> globaConfigurations;
	private Map<Object, Object> mapData;
	private Map<String, String> globalProperties;
    
	public List<Artifact> getArtifacts() {
		return artifacts;
	}
	public void setArtifacts(List<Artifact> artifacts) {
		this.artifacts = artifacts;
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
	public Map<Object, Object> getMapData() {
		if(this.mapData == null) this.mapData = new HashMap<>();
		return mapData;
	}
	
	public void setMapData(Map<Object, Object> mapData) {
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
