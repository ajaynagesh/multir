package datasetUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class SetentialExtraction {

	public static void  main(String args[]) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(new File("/home/ajay/Desktop/ReidelData-readable/tmp/test_eps.txt")));
		BufferedReader br2 = new BufferedReader(new FileReader(new File("/home/ajay/Desktop/ReidelData-readable/tmp/test_sen.txt")));
		
		HashMap<String, Integer> epFreq  = new HashMap<String, Integer>();
		HashMap<String, Integer> relFreq = new HashMap<String, Integer>();
		
		String line = "";
		int totalLines = 0;
		while((line = br2.readLine()) != null){
			int idx = line.indexOf(" ");
			int freq = Integer.parseInt(line.substring(0, idx));
			String ep = line.substring(idx+1);
			//System.out.println("F:" + freq + "\t" + ep);
			epFreq.put(ep, freq);
			totalLines += freq;
		}
		
		int totalLinesMatching = 0;
		while((line = br.readLine()) != null){
			String eps = line.split("\\t")[0] + "\t" + line.split("\\t")[1];
			String rels = line.split("\\t")[2];
			int freq = epFreq.get(eps);
			//System.out.println("F:" + freq + "\t" + line);
			for(String rel : rels.split(",")){
				if(!relFreq.containsKey(rel)){
					relFreq.put(rel, freq);
				}
				else{
					int cnt = relFreq.get(rel);
					cnt += freq;
					relFreq.put(rel, cnt);
				}
			}
			
			totalLinesMatching += freq;
		}
		System.out.println("Total lines Matching: " + totalLinesMatching + " / " + totalLines);
		int totalRelCnt = 0 ;
		for(String rel : relFreq.keySet()){
			int relcnt = relFreq.get(rel);
			System.out.println(rel + " heuristically tagged to " + relcnt + " sentences");
			totalRelCnt += relcnt;
		}
		System.out.println("Total Rel Cnt : " + totalRelCnt);
		
		for(String rel : relFreq.keySet()){
			System.out.println(rel + "\t" + relFreq.get(rel));
		}
		
	}
}
