package org.platkmframework.cgenerator.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.platkmframework.annotation.HttpRequestMethod;
import org.platkmframework.cgenerator.cli.configuration.CliConfiguration;
import org.platkmframework.cgenerator.cli.configuration.Artifact;
import org.platkmframework.cgenerator.cli.configuration.GenConfigurationTemplate;
import org.platkmframework.cgenerator.cli.response.db.onpromise.CodeGenerationOnPromiseData;
import org.platkmframework.cgenerator.cli.response.db.onpromise.TemplateData;
import org.platkmframework.cgenerator.core.data.ResultSourceData;
import org.platkmframework.content.json.JsonUtil;
import org.platkmframework.databasereader.core.DatabaseReader;
import org.platkmframework.databasereader.model.ImportedKey;
import org.platkmframework.databasereader.model.Table;
import org.platkmframework.httpclient.RestInfo;
import org.platkmframework.httpclient.http.HttpClientProcessor;
import org.platkmframework.httpclient.response.ResponseInfo; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.reflect.TypeToken;


/**
 * ------------------------------
 *  Basic code generation syntax
 *  -----------------------------
 *  lidki-cli <config folder name> <config names separated by colon> <table name>
 *   
 * Examples
 * lidki-cli portal_proveedor all area
 * 
 * 
 */
public class MainCLI {

	private static Logger logger = LoggerFactory.getLogger(MainCLI.class);
	
	private static final String C_CURRENT_VERSION = "PLATKMFramework CGenerator version 1.0";
	 
	private static final String PLATKMFRAMEWORK_CLI_HOST = "";
	private static final String PLATKMFRAMEWORK_CLI_CODE_GEN_PATH = ""; 
	
	private static final String C_CGENERATOR_HOME = "CGENERATOR_HOME";
	
	public static void main(String[] args) throws Exception   {
		
		if( args == null) {
			logger.error("No parameters were sent");
			System.exit(-1);
		}
		
		if( args.length == 1 && ( "-v".equals(args[0]) || "-version".equals(args[0]) )) {
			logger.info(C_CURRENT_VERSION);
			System.exit(-1);
		}
		
		if( args.length == 1 && ( "-h".equals(args[0]) || "-help".equals(args[0]) )) {
			InputStream inputStream = MainCLI.class.getResourceAsStream("/help");
			logger.info(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
			System.exit(-1);
		}
			
		if(args.length < 2) {
			logger.error("Debe pasar al menos tres parametros, configuracion, artefactos y nombre del objeto");
			System.exit(-1);
		} 

		Yaml yaml = new Yaml();
		String generatorHomePath = System.getenv(C_CGENERATOR_HOME);
		if(StringUtils.isBlank(generatorHomePath))
			logger.error("Environment variable not found -> " + generatorHomePath);
		
		File cliConfigurationRoot = new File(generatorHomePath.replace("//", File.separator).replace("\\", File.separator) + File.separator + "config" + File.separator + args[0]);
		File cliConfigurationFile = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "configuration");
		FileInputStream fileInputStream = new FileInputStream(cliConfigurationFile);
		CliConfiguration cliConfiguration = yaml.loadAs(fileInputStream, CliConfiguration.class);
		
		Class.forName(cliConfiguration.getOnPromiseConfiguration().getDriver()); 
		Connection con = DriverManager.getConnection(cliConfiguration.getOnPromiseConfiguration().getUrl(), 
				cliConfiguration.getOnPromiseConfiguration().getUser(), 
				cliConfiguration.getOnPromiseConfiguration().getPassword());
		
		JsonUtil.init();
		
		DatabaseReader databaseReader = new DatabaseReader(con);
		
		List<Table> list = databaseReader.readTables(null, null, args[2], new String[] {"TABLE", "VIEW"});
		if(list == null || list.isEmpty()) {
			logger.info("No se encontro una tabla con el nombre -> " + args[2]);
			System.exit(-1);
		}
			
		Table table = list.get(0);
		CodeGenerationOnPromiseData codeGenerationOnPromiseData = new CodeGenerationOnPromiseData();
		codeGenerationOnPromiseData.setTable(table);
		codeGenerationOnPromiseData.setMapData(cliConfiguration.getMapData());
		 
		if(! table.getImportedKeys().isEmpty()) {
			for (ImportedKey importedKey : table.getImportedKeys()) {
				list = databaseReader.readTables(null, null,importedKey.getPkTableName(), new String[] {"TABLE", "VIEW"});
				codeGenerationOnPromiseData.getAdditionalObjects().put(list.get(0).getName(), list.get(0));
			}
		}
			
		String[] configurations = args[1].split(",");
		codeGenerationOnPromiseData.setDriver(cliConfiguration.getOnPromiseConfiguration().getDriver());
		File templatesFolder = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "templates");
		File templatesFile;
		TemplateData templateData;
		List<GenConfigurationTemplate> genConfigurationTemplateList = new ArrayList<>();
		for (int i = 0; i < configurations.length; i++) {
			String templatesConfig  = configurations[i];
			
			Artifact artifact = cliConfiguration.getArtifacts().stream().filter(c->c.getId().equals(templatesConfig)).findFirst().orElse(null);
			if(artifact == null) {
				logger.info("No se encontro una configuracion para el valor -> " + args[1]);
				System.exit(-1);
			}
			
			for (GenConfigurationTemplate genConfigurationTemplate : artifact.getTemplates()){
				genConfigurationTemplateList.add(genConfigurationTemplate);
				templateData = new TemplateData(genConfigurationTemplate.getCode(), genConfigurationTemplate.getData());
				templateData.setPostfix(genConfigurationTemplate.getPostfix());
				templateData.setPrefix(genConfigurationTemplate.getPrefix());
				if(StringUtils.isNotBlank(genConfigurationTemplate.getFilename())){
					templatesFile = new File(templatesFolder.getAbsolutePath() + File.separator + genConfigurationTemplate.getFilename());
					byte[] fileContent = Files.readAllBytes(templatesFile.toPath());
					templateData.setContent(Base64.getEncoder().encodeToString(fileContent)); 
				}
				codeGenerationOnPromiseData.getTemplates().add(templateData);
			}
		}
			
		for (int i = 0; i < args.length; i++) {
			if( "-prop".equals(args[i]) && ((i+1) < args.length) ) {
				codeGenerationOnPromiseData.setAddParameter(args[i+1]);
			}
		}
			
		RestInfo restInfo = RestInfo.create().
				//url("http://localhost:8098/app/heaou/evayreah/generate/template").
				header("Access-Control-Request-Headers", "Origin, Authorization, Content-Type, X-Auth-Token, C-Token-Generator").
				header("Access-Control-Request-Method", "POST,GET,PUT,DELETE").
				header("C-Token-Generator",  cliConfiguration.getToken());
		
		Map<String, String> mapQueryParam = new HashMap<>();
		ResponseInfo responseInfo = (ResponseInfo) HttpClientProcessor.instance().
				process(PLATKMFRAMEWORK_CLI_HOST + PLATKMFRAMEWORK_CLI_CODE_GEN_PATH, 
						mapQueryParam, 
						codeGenerationOnPromiseData,
						HttpRequestMethod.PUT, 
						false, 
						null,
						restInfo, 
						ResponseInfo.class);
		if( responseInfo.getStatus() != 200) {
			logger.info("No se pudo eralizar el proceso ->" + responseInfo.getReasonPhrase());
			System.exit(-1);
		}else {
			List<ResultSourceData> result = JsonUtil.jsonToObjectTypeReference(responseInfo.getJson(), new TypeToken<ArrayList<ResultSourceData>>() {});
			if(!result.isEmpty()) {
				File file;
				GenConfigurationTemplate genConfigurationTemplate; 
				String fileURL;
				String fileName; 
				
				List<String> keys = cliConfiguration.getGlobaConfigurations().keySet().stream().collect(Collectors.toList());
				processConfiguration(cliConfiguration.getGlobaConfigurations(), keys);
				
				for (ResultSourceData resultSourceData : result) {
					
					genConfigurationTemplate = genConfigurationTemplateList.stream().filter(t-> t.getCode().equals(resultSourceData.getTemplate())).findFirst().orElse(null);
					
					if(genConfigurationTemplate == null){
						logger.info("No se pudo guardar la informacion. No se encontro la plantilla de informacion para " + resultSourceData.getFilename());
						System.exit(-1);
					}
					
					if(StringUtils.isNotBlank(genConfigurationTemplate.getResultFileName())) {
						fileName = genConfigurationTemplate.getResultFileName();
					}else if(resultSourceData.getOutPut().containsKey("customFileName")) {
						fileName = resultSourceData.getOutPut().get("customFileName").toString();
					}else {
						fileName = resultSourceData.getFilename();
					}
					fileURL = processOutput(cliConfiguration.getGlobaConfigurations().get(genConfigurationTemplate.getOutputpath()), cliConfiguration.getGlobaConfigurations()) + File.separator + fileName.replace("//", File.separator).replace("\\", File.separator);
					fileURL = fileURL.replace("${tablename}", table.getName().toLowerCase());
					file = new File(fileURL);
					if(!file.exists() || "true".equalsIgnoreCase(genConfigurationTemplate.getRewritable())) {
						FileUtils.write(file, resultSourceData.getContent(), "UTF-8");
						logger.info(file.getAbsolutePath());
					}else{
						logger.info("Ya existe el fichero -> " + file.getAbsolutePath());
					}
				}
			}
		} 
	}


	private static void processConfiguration(Map<String, String> globaConfigurations, List<String> keys) {
		if(globaConfigurations != null) {
			for (String key : keys) {
				globaConfigurations.replaceAll((k,v)->{ return (key == k)?v:v.toString().replace("${"+key+"}", globaConfigurations.get(key)); });
			}
		}
	}


	private static String processOutput(String outputpath, Map<String, String> globalProperties) {
		if(outputpath == null) return "";
		if(globalProperties != null) {
			for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
				outputpath = outputpath.replace("${" + entry.getKey() + "}", entry.getValue().toString());
		    }
		}
		return outputpath;
	}

}
