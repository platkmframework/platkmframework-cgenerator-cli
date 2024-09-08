package org.platkmframework.cgenerator.cli.configuration;

import java.io.Serializable;
import java.util.List;

public class Artifact implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String id;
	private String description;
	private List<GenConfigurationTemplate> templates;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<GenConfigurationTemplate> getTemplates() {
		return templates;
	}
	public void setTemplates(List<GenConfigurationTemplate> templates) {
		this.templates = templates;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
}
