package org.platkmframework.cgenerator.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.platkmframework.annotation.HttpRequestMethod;
import org.platkmframework.cgenerator.cli.configuration.Artifact;
import org.platkmframework.cgenerator.cli.configuration.CliConfiguration;
import org.platkmframework.cgenerator.cli.configuration.GenConfigurationTemplate;
import org.platkmframework.cgenerator.cli.exception.CGeneratorException;
import org.platkmframework.cgenerator.cli.response.db.onpromise.CodeGenerationOnPromiseData;
import org.platkmframework.cgenerator.cli.response.db.onpromise.TemplateData;
import org.platkmframework.cgenerator.cli.response.db.onpromise.db.OnPromiseDatabaseData;
import org.platkmframework.cgenerator.core.data.ResultSourceData;
import org.platkmframework.content.json.JsonUtil;
import org.platkmframework.databasereader.core.DatabaseReader;
import org.platkmframework.databasereader.model.ImportedKey;
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
 * 
 */
public class MainCLI {

	private static Logger logger = LoggerFactory.getLogger(MainCLI.class);
	
	private static final String C_CURRENT_VERSION = "PLATKMFramework CGenerator version 1.2";
	  
	private static final String PLATKMFRAMEWORK_CLI_HOST = "";
	
	private static final String PLATKMFRAMEWORK_CLI_CODE_GEN_PATH = ""; 
	private static final String PLATKMFRAMEWORK_CLI_CODE_GEN_EXAMPLE_PATH = ""; 
	
	private static final String C_CGENERATOR_HOME = "CGENERATOR_HOME";
	
	public static void main(String[] args) throws Exception {
		MainCLI mainCLI = new MainCLI();
		mainCLI.runGenerationRequest(args);
	}
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public void runGenerationRequest(String[] args)throws Exception{
		
		validateArgs(args);

		JsonUtil.init();

		String generatorHomePath = System.getenv(C_CGENERATOR_HOME);
		if(StringUtils.isBlank(generatorHomePath)) {
			logger.error("Environment variable not found -> " + generatorHomePath);
			System.exit(-1);
		}
		
		File cliConfigurationRoot = new File(generatorHomePath.replace("//", File.separator).replace("\\", File.separator) + File.separator + "config" + File.separator + args[0]);
		File cliConfigurationFile = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "configuration");
		
		checkIsTemplateFile(cliConfigurationFile);
		
		FileInputStream fileInputStream = new FileInputStream(cliConfigurationFile);
		CliConfiguration cliConfiguration = new Yaml().loadAs(fileInputStream, CliConfiguration.class);
		
		CodeGenerationOnPromiseData codeGenerationOnPromiseData = new CodeGenerationOnPromiseData();
		for (int i = 0; i < args.length; i++) {
			if( "-prop".equals(args[i]) && ((i+1) < args.length) ) {
				codeGenerationOnPromiseData.setAddParameter(args[i+1]);
			}
		}
		
		if("-swagger".equals(args[1])){
			processSwaggerFile(args, cliConfigurationRoot, codeGenerationOnPromiseData);
		}else if("-collection".equals(args[1])){
			processCollectionFile(args, cliConfigurationRoot, codeGenerationOnPromiseData);
		}else if("-xml".equals(args[1])){
			processXMLFile(args, cliConfigurationRoot, codeGenerationOnPromiseData);
		}else if("-database".equals(args[1])){
			processDatabase(args, codeGenerationOnPromiseData, cliConfiguration, cliConfigurationRoot);
		}else if("-gen-json-database".equals(args[1])){
			//generar un json a partir del esquema de la base de datos
			processGenJsonDatabase(args, cliConfiguration, cliConfigurationRoot);
		}else if("-json-database".equals(args[1])){
			//generar codig a patir del json del esquema de base de datos
			processTableFromJsonDatabase(args, codeGenerationOnPromiseData, cliConfiguration, cliConfigurationRoot);
		}else if("-example".equals(args[1])){
			//generar codig a patir del json del esquema de base de datos
			processExample(args, codeGenerationOnPromiseData, cliConfiguration, cliConfigurationRoot);
		}else {
			logger.info("No se encontro el parametro para el tipod e generación, -swagger, -collection, -gen-json-database, -json-database -database, -xml");
			System.exit(-1);
		}
		
		
	}
	
	


	/**
	 * 
	 * @param args
	 * @param codeGenerationOnPromiseData
	 * @param cliConfiguration
	 * @throws CGeneratorException 
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private void processDatabase(String[] args,
			CodeGenerationOnPromiseData codeGenerationOnPromiseData, 
			CliConfiguration cliConfiguration, 
			File cliConfigurationRoot) throws CGeneratorException  {
		
		try {
		 
			Class.forName(cliConfiguration.getOnPromiseConfiguration().getDriver()); 
			Connection con = DriverManager.getConnection(cliConfiguration.getOnPromiseConfiguration().getUrl(), 
					cliConfiguration.getOnPromiseConfiguration().getUser(), 
					cliConfiguration.getOnPromiseConfiguration().getPassword());
			
			
			Table table;
			List<Table> list;
			String[] objects = args[2].split(",");
			
			DatabaseReader databaseReader = new DatabaseReader(con);
			if(objects.length ==1) {
				list = databaseReader.readTables(null, null, objects[0], new String[] {"TABLE", "VIEW"});
				if(list == null || list.isEmpty()) {
					logger.info("No se encontro una tabla con el nombre -> " + args[2]);
					System.exit(-1);
				}
				
				table = list.get(0);
				codeGenerationOnPromiseData.setTable(table);
				
				if(! table.getImportedKeys().isEmpty()) {
					for (ImportedKey importedKey : table.getImportedKeys()) {
						list = databaseReader.readTables(null, null,importedKey.getPkTableName(), new String[] {"TABLE", "VIEW"});
						codeGenerationOnPromiseData.getAdditionalObjects().put(list.get(0).getName(), list.get(0));
					}
				}
			}else {
				for (int i = 0; i < objects.length; i++) {
					list = databaseReader.readTables(null, null, objects[i], new String[] {"TABLE", "VIEW"});
					if(list == null || list.isEmpty()) {
						logger.info("No se encontro una tabla con el nombre -> " + objects[i]);
						System.exit(-1);
					}
					codeGenerationOnPromiseData.getAdditionalObjects().put(list.get(0).getName(), list.get(0));
				}
			}

			codeGenerationOnPromiseData.setSchemaType("-table");
			codeGenerationOnPromiseData.setMapData(cliConfiguration.getMapData());
			codeGenerationOnPromiseData.setDriver(cliConfiguration.getOnPromiseConfiguration().getDriver());
			
			runGeneration(args[0], cliConfiguration, codeGenerationOnPromiseData, cliConfigurationRoot, args[3].split(","), PLATKMFRAMEWORK_CLI_CODE_GEN_PATH);
			
		} catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			 throw new CGeneratorException(e.getMessage());
		} 		
			
	}

	private void processTableFromJsonDatabase(String[] args, CodeGenerationOnPromiseData codeGenerationOnPromiseData,
			CliConfiguration cliConfiguration, File cliConfigurationRoot) throws CGeneratorException {
		
		try {
			
			if(args.length < 4 ) {
				logger.info("cgenerator <config-folder> -gen-json-database <result file name>");
				throw new CGeneratorException("Deben ser 4 parámetros para este proceso");
			}
		 
			File schemaFile = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "jsonschema" + File.separator + args[2]);
			if(!schemaFile.exists() || !schemaFile.isFile()) 
				throw new CGeneratorException("El fichero no existe");
			
			FileInputStream fi = new FileInputStream(schemaFile);
			ObjectInputStream oi = new ObjectInputStream(fi); 
			OnPromiseDatabaseData onPromiseDatabaseData = (OnPromiseDatabaseData) oi.readObject();
			
			String[] objects = args[3].split(",");
			Table table;
			if(objects.length == 1) {
				table = onPromiseDatabaseData.getTables().stream().filter(t-> t.getName().equals(objects[0])).findFirst().orElse(null);
				if(table == null) { 
					throw new CGeneratorException("no se encontró el objeto con nombre -> " + objects[0]);
				}
				
				codeGenerationOnPromiseData.setTable(table);
				
				if(! table.getImportedKeys().isEmpty()){
					Table relatedTable;
					for (ImportedKey importedKey : table.getImportedKeys()) {
						relatedTable = onPromiseDatabaseData.getTables().stream().filter((t)-> t.getName().equals(importedKey.getPkTableName())).findFirst().orElse(null);
						codeGenerationOnPromiseData.getAdditionalObjects().put(relatedTable.getName(), relatedTable);
					}
				}
				
			}else{
				for (int i = 0; i < objects.length; i++) {
					int index = i;
					table = onPromiseDatabaseData.getTables().stream().filter(t-> t.getName().equals(objects[index])).findFirst().orElse(null);
					if(table == null) { 
						throw new CGeneratorException("no se encontró el objeto con nombre -> " + objects[index]);
					}
					codeGenerationOnPromiseData.getAdditionalObjects().put(table.getName(), table);
				}
			}
			
			codeGenerationOnPromiseData.setSchemaType("-table");
			codeGenerationOnPromiseData.setMapData(cliConfiguration.getMapData());
			codeGenerationOnPromiseData.setDriver(cliConfiguration.getOnPromiseConfiguration().getDriver());
			
			runGeneration(args[0], cliConfiguration, codeGenerationOnPromiseData, cliConfigurationRoot, args[4].split(","), PLATKMFRAMEWORK_CLI_CODE_GEN_PATH);
			
		} catch (IOException | ClassNotFoundException e) { 
			throw new CGeneratorException("No se encontró el fichero");
		} 
		
	}

	private void processGenJsonDatabase(String[] args,
			CliConfiguration cliConfiguration, File cliConfigurationRoot) throws CGeneratorException{
		
		try {
			
			if(args.length < 3 ) {
				logger.info("cgenerator <config-folder> -gen-json-database <result file name>");
				throw new CGeneratorException("Deben ser 3 parámetros para este proceso");
			}
		
			Class.forName(cliConfiguration.getOnPromiseConfiguration().getDriver()); 
			Connection con = DriverManager.getConnection(cliConfiguration.getOnPromiseConfiguration().getUrl(), 
					cliConfiguration.getOnPromiseConfiguration().getUser(), 
					cliConfiguration.getOnPromiseConfiguration().getPassword());
			
			DatabaseReader databaseReader = new DatabaseReader(con);
		
			List<Table> list = databaseReader.readTables(null, null, null, new String[] {"TABLE", "VIEW"});
			if(list == null || list.isEmpty()) {
				logger.info("No se encontraron estructuras de tablas o vistas.");
				System.exit(-1);
			}
		
			OnPromiseDatabaseData onPromiseDatabaseData = new OnPromiseDatabaseData();
			onPromiseDatabaseData.setName(args[2]);
			onPromiseDatabaseData.setTables(list);
		
			File folder = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "jsonschema");
			if(!folder.exists() || !folder.isDirectory()) {
				FileUtils.forceMkdir(folder);
			}
			File schemaFile = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "jsonschema" + File.separator + args[2] + ".jsonschema" );
			FileOutputStream f = new FileOutputStream(schemaFile);
			ObjectOutputStream o = new ObjectOutputStream(f);
			o.writeObject(onPromiseDatabaseData);
		
			logger.info("Esquema creado. Ruta:");
			logger.info(schemaFile.getAbsolutePath());
			System.exit(0);
		} catch ( IOException | ClassNotFoundException | SQLException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException e) {
			throw new CGeneratorException(e.getMessage());
		}  
	}
	
	private void processExample(String[] args, CodeGenerationOnPromiseData codeGenerationOnPromiseData,
			CliConfiguration cliConfiguration, File cliConfigurationRoot) throws CGeneratorException {

		try {
			 
			Class.forName(cliConfiguration.getOnPromiseConfiguration().getDriver()); 
			Connection con = DriverManager.getConnection(cliConfiguration.getOnPromiseConfiguration().getUrl(), 
					cliConfiguration.getOnPromiseConfiguration().getUser(), 
					cliConfiguration.getOnPromiseConfiguration().getPassword());
			
			Table table;
			List<Table> list;
			String object = args[2];
			
			DatabaseReader databaseReader = new DatabaseReader(con);
			list = databaseReader.readTables(null, null, object, new String[] {"TABLE", "VIEW"});
			if(list == null || list.isEmpty()) {
				logger.info("No se encontro una tabla con el nombre -> " + args[2]);
				System.exit(-1);
			}
			
			table = list.get(0);
			codeGenerationOnPromiseData.setTable(table); 

			codeGenerationOnPromiseData.setSchemaType("-table");
			codeGenerationOnPromiseData.setMapData(cliConfiguration.getMapData());
			codeGenerationOnPromiseData.setDriver(cliConfiguration.getOnPromiseConfiguration().getDriver());
			
			runGeneration(args[0], cliConfiguration, codeGenerationOnPromiseData, cliConfigurationRoot, args[3].split(","), PLATKMFRAMEWORK_CLI_CODE_GEN_EXAMPLE_PATH);
			
		} catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			 throw new CGeneratorException(e.getMessage());
		} 		
		
	}
 
	/**
	 * 
	 * @param cliConfiguration
	 * @param codeGenerationOnPromiseData
	 * @return
	 * @throws CGeneratorException 
	 * @throws HttpClientError
	 * @throws HttpClientAttemptError
	 */
	private ResponseInfo requestForGeneration(CliConfiguration cliConfiguration, CodeGenerationOnPromiseData codeGenerationOnPromiseData, String api) throws CGeneratorException{
	
		
		try{
			
			RestInfo restInfo = RestInfo.create().
		 
				header("Access-Control-Request-Headers", "Origin, Authorization, Content-Type, X-Auth-Token, C-Token-Generator").
				header("Access-Control-Request-Method", "POST,GET,PUT,DELETE").
				header("C-Token-Generator",  cliConfiguration.getToken());
		
			Map<String, String> mapQueryParam = new HashMap<>();
			ResponseInfo responseInfo = (ResponseInfo) HttpClientProcessor.instance().
					process(PLATKMFRAMEWORK_CLI_HOST + api, 
							mapQueryParam, 
							codeGenerationOnPromiseData,
							HttpRequestMethod.PUT, 
							false, 
							null,
							restInfo, 
							ResponseInfo.class);
			return responseInfo;
		
		} catch (HttpClientError | HttpClientAttemptError e) {
			throw new CGeneratorException(e.getMessage());
		}
	}

	/**
	 * 
	 * @param args
	 * @param cliConfigurationRoot
	 * @param codeGenerationOnPromiseData
	 */
	private void processXMLFile(String[] args, File cliConfigurationRoot,
			CodeGenerationOnPromiseData codeGenerationOnPromiseData) {
		throw new NotImplementedException("processXMLFile"); 
	}

	/**
	 * 
	 * @param args
	 * @param cliConfigurationRoot
	 * @param codeGenerationOnPromiseData
	 * @throws IOException
	 */
	private void processCollectionFile(String[] args, File cliConfigurationRoot,
			CodeGenerationOnPromiseData codeGenerationOnPromiseData) throws IOException {
		throw new NotImplementedException("processCollectionFile"); 
	}


	/**
	 * 
	 * @param args
	 * @param cliConfigurationRoot
	 * @param codeGenerationOnPromiseData
	 * @throws IOException
	 */
	private void processSwaggerFile(String[] args, File cliConfigurationRoot, CodeGenerationOnPromiseData codeGenerationOnPromiseData) throws IOException {
		throw new NotImplementedException("processSwaggerFile"); 
	}


	/**
	 * 
	 * @param configFolder
	 * @param fileName
	 * @param content
	 * @throws IOException 
	 */
	private void storeFileCopy(String configFolderName, String artifactName, String templateCode, String fileName, String content) throws IOException {
		File folder = new File(System.getProperty("user.home") +  
				 File.separator + configFolderName +
				 File.separator + artifactName +
				 File.separator + templateCode +
				 File.separator + fileName);
		
		FileUtils.write(folder, content, "UTF-8");
	}

	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	private void validateArgs(String[] args) throws IOException {
		
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
	}


	/**
	 * 
	 * @param templatesFile
	 */
	private void checkIsTemplateFile(File templatesFile) {
		
		if(!templatesFile.exists()) {
			logger.error("No se encontró un fichero con el camiono -> " + templatesFile.getAbsolutePath());
			System.exit(-1);
		}else if(!templatesFile.isFile()) {
			 logger.error("No es un fichero -> " + templatesFile.getAbsolutePath());
			 System.exit(-1);
		}else{
			if((((double)templatesFile.length()) / 1024) > 10){
				logger.error("El tamaño de la plantilla no debe exceder los 10 kb, tamaño actual -> " + templatesFile.length());
				System.exit(-1);
			}
		 }
	}


	/**
	 * 
	 * @param globaConfigurations
	 * @param keys
	 */
	private void processConfiguration(Map<String, String> globaConfigurations, List<String> keys) {
		if(globaConfigurations != null) {
			for (String key : keys) {
				globaConfigurations.replaceAll((k,v)->{ return (key == k)?v:v.toString().replace("${"+key+"}", globaConfigurations.get(key)); });
			}
		}
	}


	/**
	 * 
	 * @param outputpath
	 * @param globalProperties
	 * @return 	String[] artefactos = args[4].split(",");
	 */
	private String processOutput(String outputpath, Map<String, String> globalProperties) {
		if(outputpath == null) return "";
		if(globalProperties != null) {
			for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
				outputpath = outputpath.replace("${" + entry.getKey() + "}", entry.getValue().toString());
		    }
		}
		return outputpath;
	}
	
	private void runGeneration(String configFolderName, CliConfiguration cliConfiguration, CodeGenerationOnPromiseData codeGenerationOnPromiseData, 
			File cliConfigurationRoot, String[] artefactos, String api) throws CGeneratorException {
		
		List<GenConfigurationTemplate> genConfigurationTemplateList = procesTemplates(artefactos, cliConfigurationRoot, cliConfiguration, codeGenerationOnPromiseData);
		
		logger.info("Comenzando con el proceso, por favor espere..."  );
		
		ResponseInfo responseInfo = requestForGeneration(cliConfiguration, codeGenerationOnPromiseData, api);
		if( responseInfo.getStatus() != 200) {
			logger.info("No se pudo eralizar el proceso ->" + responseInfo.getReasonPhrase());
			System.exit(-1);
		}else {
			processResult(configFolderName, responseInfo, cliConfiguration, genConfigurationTemplateList);
		}
	}

	/**
	 * 
	 * @param args
	 * @param cliConfigurationRoot
	 * @param cliConfiguration
	 * @param codeGenerationOnPromiseData
	 * @return
	 * @throws CGeneratorException 
	 * @throws IOException
	 */
	private List<GenConfigurationTemplate>  procesTemplates(String[] artefactos, File cliConfigurationRoot, CliConfiguration cliConfiguration, CodeGenerationOnPromiseData codeGenerationOnPromiseData) throws CGeneratorException {
		
		try {
			
			File templatesFolder = new File(cliConfigurationRoot.getAbsolutePath() + File.separator + "templates");
			File templatesFile;
			TemplateData templateData;
			List<GenConfigurationTemplate> genConfigurationTemplateList = new ArrayList<>();
			for (int i = 0; i < artefactos.length; i++) {
				String artefacto  = artefactos[i];
				
				Artifact artifact = cliConfiguration.getArtifacts().stream().filter(c->c.getId().equals(artefacto)).findFirst().orElse(null);
				if(artifact == null) {
					logger.info("No se encontro el artefacto -> " + artefacto);
					System.exit(-1);
				}
				
				for (GenConfigurationTemplate genConfigurationTemplate : artifact.getTemplates()){
					genConfigurationTemplateList.add(genConfigurationTemplate);
					templateData = new TemplateData(artifact.getId(), genConfigurationTemplate.getCode(), genConfigurationTemplate.getData());
					templateData.setSuffix(genConfigurationTemplate.getSuffix());
					templateData.setPrefix(genConfigurationTemplate.getPrefix());
					templateData.setFileExtension(genConfigurationTemplate.getFileExtension());
					templateData.setFileExtension(genConfigurationTemplate.getFileExtension());
					if(StringUtils.isNotBlank(genConfigurationTemplate.getTemplatePath())){
						templatesFile = new File(templatesFolder.getAbsolutePath() + File.separator + genConfigurationTemplate.getTemplatePath());
						checkIsTemplateFile(templatesFile);
						byte[] fileContent = Files.readAllBytes(templatesFile.toPath());
						templateData.setContent(Base64.getEncoder().encodeToString(fileContent)); 
					}
					codeGenerationOnPromiseData.getTemplates().add(templateData);
				}
			}
			
			return genConfigurationTemplateList;
		
		} catch (IOException e) {
			throw new CGeneratorException(e.getMessage());
		}
		
	}

	/**
	 * 
	 * @param responseInfo
	 * @param cliConfiguration
	 * @param genConfigurationTemplateList
	 * @throws CGeneratorException 
	 * @throws JsonException 
	 * @throws IOException 
	 */
	private void processResult(String configFolder, ResponseInfo responseInfo, CliConfiguration cliConfiguration, List<GenConfigurationTemplate> genConfigurationTemplateList) throws CGeneratorException{
		
		try {
			List<ResultSourceData> result = JsonUtil.jsonToObjectTypeReference(responseInfo.getJson(), new TypeToken<ArrayList<ResultSourceData>>() {});
			if(!result.isEmpty()) {
				File file;
				GenConfigurationTemplate genConfigurationTemplate; 
				String fileURL;
				String fileName; 
				String oldContent;
				String outputPath;
				List<String> keys = cliConfiguration.getGlobaConfigurations().keySet().stream().collect(Collectors.toList());
				processConfiguration(cliConfiguration.getGlobaConfigurations(), keys);
				
				for (ResultSourceData resultSourceData : result) {
					
					genConfigurationTemplate = genConfigurationTemplateList.stream().filter(t-> t.getCode().equals(resultSourceData.getTemplate())).findFirst().orElse(null);
					
					if(genConfigurationTemplate == null){
						logger.info("No se pudo guardar la informacion. No se encontro la plantilla de informacion para " + resultSourceData.getTemplate());
						System.exit(-1);
					}
					fileName = "";
					if(StringUtils.isNotBlank(resultSourceData.getResultFileName())){
						fileName = resultSourceData.getResultFileName(); 
					}else if(StringUtils.isNotBlank(genConfigurationTemplate.getOutputFileName())) {
						fileName = genConfigurationTemplate.getOutputFileName(); 
					}
					
					if(StringUtils.isBlank(fileName)) {
						logger.error("No existe un nombre de fichero de salida para plantilla -> " + genConfigurationTemplate.getCode() + 
								". Considere incluir en el artefacto el resultFileName o en la plantilla usar la funcion setResultFileName o conformFileName");	
					}else{
						if(StringUtils.isNotBlank(resultSourceData.getResultFilePath())){
							outputPath = resultSourceData.getResultFilePath();
						}else {
							outputPath = cliConfiguration.getGlobaConfigurations().get(genConfigurationTemplate.getOutputpath());
						}
						
						fileURL = processOutput(outputPath, cliConfiguration.getGlobaConfigurations()) + File.separator + fileName;
						file = new File(fileURL);
						if(!file.exists() || "true".equalsIgnoreCase(genConfigurationTemplate.getRewritable())) {
							FileUtils.write(file, resultSourceData.getContent(), "UTF-8");
							logger.info(file.getAbsolutePath());
							
							if(file.exists()) {
								oldContent = FileUtils.readFileToString(file, "UTF-8");
								storeFileCopy(configFolder, resultSourceData.getArtifact(), genConfigurationTemplate.getCode(), file.getName(), oldContent);
							}
						}else{
							logger.info("Ya existe el fichero. Para sobrescribir agregue al artefacto el valor de rewritable=true, -> " + file.getAbsolutePath());
						}
					}
				}
			}else {
				logger.info("No hubo resultados de la generación");
			}
		}catch (JsonException | IOException e) {
			throw new CGeneratorException(e.getMessage());
		}
	}
	
	
}
