package org.platkmframework.cgenerator.cli.configuration;

import java.util.Map;

public class GenConfigurationTemplate {
	
	private String code;
	private String outputpath;
	private Map<String, String> data;
	private String filename;
	private String prefix;
	private String postfix;
	private String rewritable;
	private String resultFileName;
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getOutputpath() {
		return outputpath;
	}
	public void setOutputpath(String outputpath) {
		this.outputpath = outputpath;
	} 
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getPostfix() {
		return postfix;
	}
	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}
	public String getRewritable() {
		return rewritable;
	}
	public void setRewritable(String rewritable) {
		this.rewritable = rewritable;
	}
	public Map<String, String> getData() {
		return data;
	}
	public void setData(Map<String, String> data) {
		this.data = data;
	}
	public String getResultFileName() {
		return resultFileName;
	}
	public void setResultFileName(String resultFileName) {
		this.resultFileName = resultFileName;
	}

}
