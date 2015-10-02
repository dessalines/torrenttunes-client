package com.torrenttunes.client.tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;



public enum Strings {
	
	EN(DataSources.STRINGS_EN_LOCATION()), 
	ES(DataSources.STRINGS_ES_LOCATION());
	

	
	/**
	 * This is of the format: {strings: { home: Home, ...}}<br>
	 * And used like {{strings.home}} in mustache templating
	 */
	public Map<String, Map<String, String>> map; 
	
	private Strings(String jsonLocation) {
		
		map = new HashMap<String, Map<String, String>>();
		
		String json = Tools.readFile(jsonLocation);
		
		JsonNode node = Tools.jsonToNode(json);
		
		Map<String, String> innerMap = new HashMap<String, String>();
		
		// Iterate over all the string fields
		JsonNode s = node.get("strings");
		
		Iterator<Entry<String, JsonNode>> sIt = s.getFields();
		while (sIt.hasNext()) {
			Entry<String, JsonNode> e = sIt.next();
			
			innerMap.put(e.getKey(), e.getValue().asText());
		}
		
		map.put("strings", innerMap);
		
	}
	

}
