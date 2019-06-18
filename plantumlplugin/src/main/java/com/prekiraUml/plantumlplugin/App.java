package com.prekiraUml.plantumlplugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Takes dtomap json and turns into txt file for plant uml to visualize
 *
 */
public class App 
{
	public static String[] stereotypes = {"Audited", "interface", "enumeration"};
    public static String[] classNamesToIgnore = {"Dto", "String", "Boolean"};
    
    public static ArrayList<JSONObject> enumList = new ArrayList<JSONObject>();
	
	public static void main( String[] args ) throws IOException, InterruptedException
    {

        //write to txt file
        PrintWriter writer = new PrintWriter("javadtotest.txt", "UTF-8");
        JSONArray dtoMap = parseJson().getJSONArray("data");
        
        String umlText = "@startuml\n" + getStyleParams(dtoMap) + getClassInfo(dtoMap) + getEnumInfo() + "@enduml";
        writer.println(umlText);
        writer.close();
        print("complete");
        
        /*
         * can cause issues with graphviz dependency, 
         * use command line java -jar plantuml.jar javadtotest.txt instead
        //generating image
        File source = new File("javadtotest.txt");
        SourceFileReader reader = new SourceFileReader(source);
        java.util.List<GeneratedImage> list = reader.getGeneratedImages();
        // Generated files
        File png = list.get(0).getPngFile();
        */

    }
	
	/**
	 * goes through list of classes and obtains property and connection information
	 * @param dtoMap containing list of classes
	 * @return text for plantuml to visualize
	 */
	public static String getClassInfo(JSONArray dtoMap) {
		String umlText = "";
		for (int i = 0; i < dtoMap.length(); i++) {

            if (!(Arrays.asList(classNamesToIgnore).contains(dtoMap.getJSONObject(i).getString("className")))) {
                umlText += "class " + (dtoMap.getJSONObject(i).get("className")); 
                umlText += getStereotypeFromDto(dtoMap.getJSONObject(i));
                umlText += getPropertiesFromDto(dtoMap.getJSONObject(i));
                umlText += "\n";
            }
        }
		return umlText;
	}
	
	/**
	 * goes through list of enums found and obtains information
	 * @return text for plantuml to visualize
	 */
	public static String getEnumInfo() {
		String umlText = "";
		for (int i = 0; i < enumList.size(); i++) {
            umlText += "enum " + getClassNameFromString(enumList.get(i).getJSONObject("value").getString("name")) + " {\n";
            JSONArray enumValueList = enumList.get(i).getJSONObject("value").getJSONArray("values");
            for (int j = 0; j < enumValueList.length(); j++) {
            	umlText += "   " + enumValueList.getJSONObject(j).getString("name") + "\n";
            }
                
            umlText += "}\n";
        	
        }
		return umlText;
	}
	
	/**
	 * provides styling, number of pages for diagrams
	 * @param dtoList list of dtos
	 * @return styling text
	 */
	public static String getStyleParams(JSONArray dtoList) {
		//styling
        String umlText = "skinparam class {\n    BackgroundColor PaleGreen\n     ArrowColor SeaGreen\n   orderColor SpringGreen\n}\n";
        umlText += "skinparam enum {\n    BackgroundColor PaleBlue\n     ArrowColor Cyan\n   orderColor Blue\n}\n";

        //split into pages if large:
        //TODO: take out hardcode
        int dimension = 1;
        if (dtoList.length() > 8) {
        	dimension += dtoList.length()/2;
        }
        return umlText + "\npage "+ dimension + "x" + dimension + "\n";
	}
	
	/**
	 * Get type, name, if enum, optional, cardinality, inter-class, embedded class connections
	 * @param dto current dto to get properties for
	 * @return text for plant uml to visualize 
	 */
	public static String getPropertiesFromDto(JSONObject dto) {
		String propertyOfClass = "{\n";
		String connectionsOfClass = "";
		JSONArray propertyList = dto.getJSONArray("properties");
		for (int i = 0; i < propertyList.length(); i++) {
			propertyOfClass += "	+";
			
			//type of field
			propertyOfClass += getClassNameFromString(propertyList.getJSONObject(i).getString("type"));
			
			//name of field
			propertyOfClass += " " + propertyList.getJSONObject(i).getString("name") + ": ";
			
			//if enum
			if (propertyList.getJSONObject(i).getJSONObject("dtoEnums").has("value")) {
				enumList.add(propertyList.getJSONObject(i).getJSONObject("dtoEnums"));
			}
			
			//if field has "Optional" or "not required" annotation
			//TODO: check if optional and not required mean same thing
			boolean isRequired = true;
			if ((propertyList.getJSONObject(i).getJSONObject("annotations").has("notRequired") && 
					propertyList.getJSONObject(i).getJSONObject("annotations").getString("notRequired").contentEquals("true")) ||
					(propertyList.getJSONObject(i).getJSONObject("annotations").has("Optional") && 
					propertyList.getJSONObject(i).getJSONObject("annotations").getString("Optional").contentEquals("true"))) {
				isRequired = false;
				propertyOfClass += "(Optional)";
			}
			
			propertyOfClass += "\n";
			
			//cardinality of class
			//doubled bc current DtoMaps have both upper and lowercase
			String cardinality = "1";
			if (propertyList.getJSONObject(i).getJSONObject("annotations").has("swaggerReference") || 
					propertyList.getJSONObject(i).getJSONObject("annotations").has("SwaggerReference")) {
				if (propertyList.getJSONObject(i).getJSONObject("annotations").has("isList") &&
						propertyList.getJSONObject(i).getJSONObject("annotations").getString("isList").contentEquals("true")) {
					cardinality = "0..*";
				} else {
					if (isRequired) {
						cardinality = "1";
					} else {
						cardinality = "0..1";
					}
				}
			}
			
			//connections between classes
			//doubled bc current DtoMaps have both upper and lowercase
			if (propertyList.getJSONObject(i).getJSONObject("annotations").has("swaggerReference")) {
				connectionsOfClass += "\n" + getClassNameFromString(propertyList.getJSONObject(i).getJSONObject("annotations").getString("swaggerReference") + 
					" -o " + dto.getString("className") + ": " + propertyList.getJSONObject(i).getString("name") + "	" + cardinality + "\n");
			}
			if (propertyList.getJSONObject(i).getJSONObject("annotations").has("SwaggerReference")) {
				connectionsOfClass += "\n" + getClassNameFromString(propertyList.getJSONObject(i).getJSONObject("annotations").getString("SwaggerReference") + 
					" -o " + dto.getString("className") + ": " + propertyList.getJSONObject(i).getString("name") + "	" + cardinality + "\n");
			}
			
			//find embedded class and connection between inner and outer classes
			if (isDto(propertyList.getJSONObject(i).getString("dtoClassName"))) {
				//inner class -> outer class : inner class
				connectionsOfClass += "\n "  + getClassNameFromString(propertyList.getJSONObject(i).getString("dtoClassName"))+ " -*" + 
						dto.getString("className") + " : + " + getClassNameFromString(propertyList.getJSONObject(i).getString("dtoClassName")) + " (inner class) ";
			}
		}
		return propertyOfClass + "}" + connectionsOfClass;
	}
	
	/**
	 * find if dtoobject is dto from type name
	 * @param typeName full type name
	 * @return if obj is dto
	 */
	public static boolean isDto(String typeName) {
		return typeName.substring(typeName.lastIndexOf(".") - "dto".length(), typeName.lastIndexOf(".")).toLowerCase().equals("dto");
	}
	
	/**
	 * extract class name from string of package directory
	 * @param name of path
	 * @return name of individual class
	 */
	public static String getClassNameFromString(String name) {
		return name.substring(name.lastIndexOf(".")+1, name.length()); 
	}
    
	/**
	 * stereotyping for dto by going through possible stereotypes and seeing if
	 * contained in dto annotations
	 * @param dto to stereotype
	 * @return formatted string for plant uml to signify stereotype
	 */
	public static String getStereotypeFromDto(JSONObject dto) {
		for (int i = 0; i < stereotypes.length; i++) {
			if (((JSONObject) dto.get("annotations")).has(stereotypes[i]) && ((JSONObject) dto.get("annotations")).getString(stereotypes[i]).equals("true")) {
				return " << " + stereotypes[i] + " >> ";
			}
		}
		return "";
	}
	
    /*easier printing syntax*/
    public static void print(Object x) {
    	System.out.println(x.toString());
    }
    
    /**
     * parse json from file 
     * @return parsed json object containing dto info
     */
    public static JSONObject parseJson() {
    	String projectJsonPath = "/target/generated-sources/generated-dto/DtoMap.json";

    	int ver = 1;
        String jsonPath = "DtoMap"+ver+".json";
        return new JSONObject("{ \"data\": " + Data.getFileContentsAsString(jsonPath) + "}");
    }
}
