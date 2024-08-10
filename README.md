# CGenerator-cli



## Instalación

1- Descargar OpenJDK versión 20 o superior

Sitio web: https://jdk.java.net

-	Agregue la variable de entorno ```JAVA_HOME```, ejemplo
  	```JAVA_HOME=c:\java_20```
-	 agregue en la variable de entorno ```Path``` el camino donde colocó el OpenJDK, ejemplo ```Path= .....;c:\java_20\bin```
-	 Compruebe que la versión esté instalada. Abra la ventana de comando y ejecute ```java -version```.



2- Descargar CLI cgenerator

Sitio web: https://github.com/platkmframework/platkmframework-cgenerator-cli
fichero cgenerator.zip

-	Agregar variable de entorno ```CGENERATOR_HOME```

  
  	 ```CGENERATOR_HOME=d:\instalaciones\platkmframework\cgenerator```
-	 agregue en la variable de entorno ```Path``` el camino donde colocó el cgenerator,

 
 	```Path= .....;d:\instalaciones\platkmframework\cgenerator\bin```  
-	 compruebe que CGenerator esté bien instalado, abra la ventana de comando y ejecue

 
 	```cgenerator -version```.



## Sintaxis


Generación de código basado en estructuras de base de datos.

- 	**config-folder** : carpeta de configuración del proyecto
-	**artifact** s: artefactos a procesar
-	**object** : nombre de la tabla de base de datos.
-	**-prop** : propiedades adicionales.

```
cgenerator <config-folder> <artifacts1, artifacts2…n> <object> -prop key1=value1,value2;key2=value21,value22;...n
```

Ejemplos

versión
```
cgenerator -v
cgenerator -version
```

ayuda
```
cgenerator -h
cgenerator -help
```

Ejemplo de generación de artefactos para el frontend y backend de una aplicación, basado en el objeto product
```
cgenerator project1 art-front,art-back product
```


## Fichero de configuración
```
token: <granted token>
onPromiseConfiguration:
	driver: <database drive>
url: <database url>
user: <database user>
password: <database password>
excludedFields:<comma separator columns to exclude from process.
artifacts:
 - id: "<artifact identifier>"
  		 templates:
     - code: "<unique code"
       outputpath: "<generation output path >"
       data:
       filename: "<template file>"
       postfix: "<result filename postfix>"
       prefix: "<result filename prefix>"
       rewritable: "<true/false>"
       resultFileName: "<the real file name, this attribute is optional>"
globaConfigurations:
<key>:<value>
mapData:
<key>:<value>
```



 
## Hash
----
6FBF471D2DA45BAC5A33A79EAEF223189F9B61A4F3E67F4672F2184ADFDAC2F6
