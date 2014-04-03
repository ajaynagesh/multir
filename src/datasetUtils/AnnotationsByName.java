package datasetUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class AnnotationsByName {

	public static void main(String args[]) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(new File("annotations/sentential.txt")));
		
		HashMap<String, entityInfo> entityMap = readDataset.createMap("annotations/filtered-freebase-simple-topic-dump-3cols.tsv");
		
		String line="";
		
		while((line = br.readLine()) != null){
			String fields[] = line.split("\\t");
			
			String e1Name = entityMap.get(fields[0]).toString();
			String e2Name = entityMap.get(fields[1]).toString();
			
			System.out.println(e1Name + "\t" + e2Name + "\t" + 
			fields[2] + "\t" + fields[3] + "\t" + fields[4] + "\t" + fields[5]
					+"\t" + fields[6] + "\t" + fields[7] + "\t" + fields[8]);
			
		}
		
	}
}
