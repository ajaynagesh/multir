package datasetUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import cc.factorie.protobuf.DocumentProtos.Relation;

public class readDataset {

	public static void read(String input, HashMap<String, entityInfo> entityMap) throws FileNotFoundException, IOException{
		InputStream is = new GZIPInputStream(
	    		new BufferedInputStream
	    		(new FileInputStream(input)));
		
	    Relation r = null;

	    while ((r = Relation.parseDelimitedFrom(is))!=null) {
	    	//System.out.println(r);
	    	String sguid = r.getSourceGuid();
	    	String dguid = r.getDestGuid();
	    	
	    	entityInfo e_src = entityMap.get(sguid);
	    	entityInfo e_dst = entityMap.get(dguid);
	    	//String[] rels = r.getRelType().split(",");
	    	System.out.println(e_src + "\t" + e_dst + "\t" + r.getRelType());
//	    	for(int i = 0; i < r.getMentionCount(); i++){
//	    		System.out.println(e_src.name + "\t" + e_dst.name + "\t" + r.getMention(i).getSentence());
//	    		//System.out.println(sguid + "\t" + dguid + "\t" + i);
//	    	}
	    	
	    }
	}

	public static HashMap<String, entityInfo> createMap(String mappingFile) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(new File(mappingFile)));
		
		HashMap<String, entityInfo> entityMap = new HashMap<String, entityInfo>();
		
		String line="";
		while((line=br.readLine())!=null){
			String [] fields = line.split("\\t");
			String guid = fields[0];
			String name = fields[1];
			String [] types = fields[2].split(",");
			
			entityInfo e = new  entityInfo(guid, name, types);
			entityMap.put(guid, e);
		}
		
		return entityMap;
	}
	public static void main(String args[]) throws FileNotFoundException, IOException{
		String input = args[0];
		String mapping = args[1];
		HashMap<String, entityInfo> entityMap = createMap(mapping);
		read(input, entityMap);
	}
}