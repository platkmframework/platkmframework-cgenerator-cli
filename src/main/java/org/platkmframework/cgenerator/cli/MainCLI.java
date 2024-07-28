package org.platkmframework.cgenerator.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.platkmframework.annotation.HttpRequestMethod;
import org.platkmframework.cgenerator.cli.configuration.CliConfiguration;
import org.platkmframework.cgenerator.cli.configuration.GenConfiguration;
import org.platkmframework.cgenerator.cli.configuration.GenConfigurationTemplate;
import org.platkmframework.cgenerator.cli.response.db.onpromise.CodeGenerationOnPromiseData;
import org.platkmframework.cgenerator.cli.response.db.onpromise.TemplateData;
import org.platkmframework.cgenerator.core.data.ResultSourceData;
import org.platkmframework.content.json.JsonUtil;
import org.platkmframework.databasereader.core.DatabaseReader;
import org.platkmframework.databasereader.model.Table;
import org.platkmframework.httpclient.RestInfo;
import org.platkmframework.httpclient.error.HttpClientAttemptError;
import org.platkmframework.httpclient.error.HttpClientError;
import org.platkmframework.httpclient.http.HttpClientProcessor;
import org.platkmframework.httpclient.response.ResponseInfo;
import org.platkmframework.util.JsonException;
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

 */
public class MainCLI {

	private static Logger logger = LoggerFactory.getLogger(MainCLI.class);
	 

	private static final String PLATKMFRAMEWORK_CLI_HOST = "";
	private static final String PLATKMFRAMEWORK_CLI_CODE_GEN_PATH = "";
	
	
	public static void main(String[] args) throws Exception   {
		MainCLI mainCLI = new MainCLI();
		mainCLI.generate(args);
	}

	public void generate(String[] args) throws IOException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, HttpClientError, HttpClientAttemptError, JsonException {
		
		if( args == null || args.length < 2) {
			logger.error("Debe pasar al menos tres parámetros, nombre del fichero de configuración, identificador de generación y nombre de la tabla");
			System.exit(-1);
		} 
		
		Yaml yaml = new Yaml();
		File cliConfigurationRoot = new File(new File("").getAbsolutePath() + File.separator + "config" + File.separator + args[0]);
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
			logger.info("No se encontró una tabla con el nombre -> " + args[2]);
			System.exit(-1);
		}
			
		CodeGenerationOnPromiseData codeGenerationOnPromiseData = new CodeGenerationOnPromiseData();
		codeGenerationOnPromiseData.setTable(list.get(0));
		codeGenerationOnPromiseData.setMapData(cliConfiguration.getMapData());
		
		String[] configurations = args[1].split(",");
		codeGenerationOnPromiseData.setDriver(cliConfiguration.getOnPromiseConfiguration().getDriver());
		File templatesFolder = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "templates");
		File templatesFile;
		String templateContent;
		TemplateData templateData;
		List<GenConfigurationTemplate> genConfigurationTemplateList = new ArrayList<>();
		for (int i = 0; i < configurations.length; i++) {
			String templatesConfig  = configurations[i];
			
			GenConfiguration genConfiguration = cliConfiguration.getGenConfigurations().stream().filter(c->c.getId().equals(templatesConfig)).findFirst().orElse(null);
			if(genConfiguration == null) {
				logger.info("No se encontró una configuración para el valor -> " + args[1]);
				System.exit(-1);
			}
			
			for (GenConfigurationTemplate genConfigurationTemplate : genConfiguration.getTemplates()){
				genConfigurationTemplateList.add(genConfigurationTemplate);
				templateData = new TemplateData(genConfigurationTemplate.getCode(), genConfigurationTemplate.getData());
				templateData.setPostfix(genConfigurationTemplate.getPostfix());
				templateData.setPrefix(genConfigurationTemplate.getPrefix());
				if(StringUtils.isNotBlank(genConfigurationTemplate.getFilename())){
					templatesFile = new File(templatesFolder.getAbsolutePath() + File.separator + genConfigurationTemplate.getFilename());
					templateContent = FileUtils.readFileToString(templatesFile, "UTF-8");
					templateData.setContent(templateContent);
				}
				codeGenerationOnPromiseData.getTemplates().add(templateData);
			}
		}
		//codeGenerationOnPromiseData.setTemplates(genConfigurationTemplateList.stream().map(t -> new TemplateData(t.getCode(), cliConfiguration.getGlobaConfigurations().get(t.getPackagename()).toString() )).collect(Collectors.toList()));
		
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
				
				logger.info("-----------------------GENERATION RESULT--------------------------");
				for (ResultSourceData resultSourceData : result) {
					
					genConfigurationTemplate = genConfigurationTemplateList.stream().filter(t-> t.getCode().equals(resultSourceData.getTemplate())).findFirst().orElse(null);
					
					if(genConfigurationTemplate == null){
						logger.error("No se pudo guardar la información. No se encontró la plantilla de información para " + resultSourceData.getFilename());
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
					fileURL = fileURL.replace("${tablename}", list.get(0).getName().toLowerCase());
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

	private void processConfiguration(Map<String, String> globaConfigurations, List<String> keys) {
		if(globaConfigurations != null) {
			for (String key : keys) {
				globaConfigurations.replaceAll((k,v)->{ return (key == k)?v:v.toString().replace("${"+key+"}", globaConfigurations.get(key)); });
			}
		}
	}


	private String processOutput(String outputpath, Map<String, String> globalProperties) {
		if(outputpath == null) return "";
		String out = outputpath;
		if(globalProperties != null) {
			for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
				out = out.replace("${" + entry.getKey() + "}", entry.getValue().toString());
		    }
		}
		return out;
	}

}
