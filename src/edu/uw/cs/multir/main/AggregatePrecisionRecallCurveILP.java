package edu.uw.cs.multir.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import datasetUtils.entityInfo;
import datasetUtils.readDataset;
import edu.uw.cs.multir.learning.algorithm.FullInference;
import edu.uw.cs.multir.learning.algorithm.FullInferenceILP;
import edu.uw.cs.multir.learning.algorithm.Model;
import edu.uw.cs.multir.learning.algorithm.Parameters;
import edu.uw.cs.multir.learning.algorithm.Parse;
import edu.uw.cs.multir.learning.algorithm.Scorer;
import edu.uw.cs.multir.learning.data.Dataset;
import edu.uw.cs.multir.learning.data.MILDocument;
import edu.uw.cs.multir.learning.data.MemoryDataset;
import edu.uw.cs.multir.main.AggregatePrecisionRecallCurve.Prediction;
import edu.uw.cs.multir.preprocess.Mappings;

public class AggregatePrecisionRecallCurveILP {
	
	public static void run(String dir, String trainType, String scoring) 
		throws IOException {
		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");

		Parameters params = new Parameters();
		params.model = model;
		params.deserialize(dir + File.separatorChar + "params");
		
		Dataset test = new MemoryDataset(dir + File.separatorChar + "test");

		eval(test, params, System.out, trainType, scoring);
	}
	
	public static void eval(Dataset test, Parameters params,
			PrintStream ps, String trainType, String scoring) throws IOException {
		System.out.println("eval");
		Scorer scorer = new Scorer();
		
		// this could also be a file
		List<Prediction> predictions = new ArrayList<Prediction>();
		MILDocument doc = new MILDocument();
		int numRelationInst = 0;
		test.reset();
		int numOfEps = 0;
		long epS = System.currentTimeMillis();
		
		HashMap<String, entityInfo> entityMap = readDataset.createMap("annotations/filtered-freebase-simple-topic-dump-3cols.tsv");
		
		while (test.next(doc)) {
			//numRelationInst += doc.Y.length;
			Parse parse = FullInferenceILP.inferILP(doc, scorer, params, trainType, scoring);
			int[] Yt = doc.Y;
			int[] Yp = parse.Y;
			
			// NA is empty array
			if (Yt.length == 0 && Yp.length == 0) continue;
				// true negative, we ignore that

			boolean[] binaryYt = new boolean[100];
			boolean[] binaryYp = new boolean[100];
			for (int i=0; i < Yt.length; i++)
				binaryYt[Yt[i]] = true;
			for (int i=0; i < Yp.length; i++)
				binaryYp[Yp[i]] = true;
			
			for (int i=1; i < binaryYt.length; i++) {				
				if (binaryYt[i] || binaryYp[i]) {
					predictions.add
						(new Prediction(i, binaryYt[i], binaryYp[i], parse.scores[i], doc, parse, entityMap.get(doc.arg1).toString(), entityMap.get(doc.arg2).toString()));
				}
			}
			
			for (int i=1; i < binaryYt.length; i++)
				if (binaryYt[i]) numRelationInst++;
			
			numOfEps++;
			if(numOfEps % 1000 == 0){
				long epE = System.currentTimeMillis();
				double epTime = (epE - epS) / 1000.0;
				System.err.println("Processed " 
							+ numOfEps +" /  " +  test.numDocs() + " in " + epTime + " s");
				epS = epE;
			}
		}
		
		Collections.sort(predictions, new Comparator<Prediction>() {
			public int compare(Prediction p1, Prediction p2) {
				if (p1.score > p2.score) return -1;
				else return +1;
			} });
		
		// need mapping from relIDs to rels
		String mappingFile = "output" + File.separatorChar + "mapping";
		Mappings mapping = new Mappings();
		mapping.read(mappingFile);
		Map<Integer,String> relID2rel = new HashMap<Integer,String>();
		for (Map.Entry<String,Integer> e : mapping.getRel2RelID().entrySet())
			relID2rel.put(e.getValue(), e.getKey());
		
		PrecisionRecallTester prt = new PrecisionRecallTester();
		prt.reset();
		double prevRec = -1, prevPre = -1;
		for (int i=0; i < predictions.size(); i++) {
			Prediction p = predictions.get(i);
			prt.handle(p.rel, p.predRel, p.trueRel, p.score);
			prt.numRelations = numRelationInst;
			double recall = prt.recall();
			double precision = prt.precision();
			if (recall != prevRec || precision != prevPre) {
				//ps.println(recall + "\t" + precision);
				ps.println(recall + "\t" + precision + "\t" + p.e1 + "\t" + p.e2 + "\t" +  relID2rel.get(p.rel) + "\t" + p.predRel + "\t" + p.trueRel + "\t" + p.score);
				prevRec = recall;
				prevPre = precision;
			}
		}
	}
	
	static class Prediction {
		int rel;
		boolean trueRel;
		boolean predRel;
		double score;
		MILDocument doc;
		Parse parse;
		
		String e1;
		String e2;
		
		Prediction(int rel, boolean trueRel, boolean predRel, double score, 
				MILDocument doc, Parse parse) {
			this.rel = rel;
			this.trueRel = trueRel;
			this.predRel = predRel;
			this.score = score;
			this.doc = doc;
			this.parse = parse;
		}
		
		Prediction(int rel, boolean trueRel, boolean predRel, double score, 
				MILDocument doc, Parse parse, String e1, String e2) {
			this.rel = rel;
			this.trueRel = trueRel;
			this.predRel = predRel;
			this.score = score;
			this.doc = doc;
			this.parse = parse;
			this.e1 = e1;
			this.e2 = e2;
		}
	}

	
	static class PrecisionRecallTester {
		public double numCorrect, numPredictions, numRelations;
		
		public void handle(String[] tokens, boolean[] predictedLabels, 
				boolean[] trueLabels, double score) {
			boolean[] p = predictedLabels;
			boolean[] t = trueLabels;
			
			for (int i=1; i < p.length; i++) {
				if (p[i] && !t[i]) numPredictions++;
				else if (!p[i] && t[i]) numRelations++;
				else if (p[i] && t[i]) {
					numCorrect++;
					numPredictions++;
					numRelations++;
				}
			}
		}
		
		public void handle(int rel, boolean p, boolean t, double score) {
			if (p && !t) numPredictions++;
			else if (!p && t) numRelations++;
			else if (p && t) {
				numCorrect++;
				numPredictions++;
				numRelations++;
			}
		}
		
		public void reset() { numCorrect = numPredictions = numRelations = 0; }
		public double precision() {
			if (numPredictions == 0) return 1;
			return numCorrect / numPredictions; 
		}
		public double recall() { 
			return numCorrect / numRelations; 
		}
	}
}
