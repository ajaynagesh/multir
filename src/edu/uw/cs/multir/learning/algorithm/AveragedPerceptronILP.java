package edu.uw.cs.multir.learning.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import edu.uw.cs.multir.learning.data.Dataset;
import edu.uw.cs.multir.learning.data.MILDocument;
import edu.uw.cs.multir.util.DenseVector;
import edu.uw.cs.multir.util.SparseBinaryVector;

public class AveragedPerceptronILP {

	public int maxIterations = 50;
	public boolean computeAvgParameters = true;
	public boolean updateOnTrueY = true;
	public double delta = 1;

	private Scorer scorer;
	private Model model;
	private Random random;

	public AveragedPerceptronILP(Model model, Random random) {
		scorer = new Scorer();
		this.model = model;
		this.random = random;
	}

	// the following two are actually not storing weights:
	// the first is storing the iteration in which the average weights were
	// last updated, and the other is storing the next update value
	private Parameters avgParamsLastUpdatesIter;
	private Parameters avgParamsLastUpdates;

	private Parameters avgParameters;
	private Parameters iterParameters;

	public Parameters train(Dataset trainingData, String type) {

		if (computeAvgParameters) {
			avgParameters = new Parameters();
			avgParameters.model = model;
			avgParameters.init();

			avgParamsLastUpdatesIter = new Parameters();
			avgParamsLastUpdates = new Parameters();
			avgParamsLastUpdatesIter.model = avgParamsLastUpdates.model = model;
			avgParamsLastUpdatesIter.init();
			avgParamsLastUpdates.init();
		}

		iterParameters = new Parameters();
		iterParameters.model = model;
		iterParameters.init();

		for (int i = 0; i < maxIterations; i++)
			trainingIteration(i, trainingData, type);

		if (computeAvgParameters) finalizeRel();
		
		return (computeAvgParameters) ? avgParameters : iterParameters;
	}

	int avgIteration = 0;

	public void trainingIteration(int iteration, Dataset trainingData, String trainType) {
		System.out.println("iteration " + iteration);

		MILDocument doc = new MILDocument();

		trainingData.shuffle(random);

		trainingData.reset();

		int numOfEps = 0;
		long itS  = System.currentTimeMillis();
		long epS = System.currentTimeMillis();
		
		while (trainingData.next(doc)) {
			
			Parse predictedParse = FullInferenceILP.inferILP(doc, scorer,
						iterParameters, trainType, "sum"); // Last param "scoring type" does not matter in train. Hence arbitrarily choosing sum
//			Parse predictedParse = FullInferenceILP.infer(doc, scorer, iterParameters);

			if (updateOnTrueY || !YsAgree(predictedParse.Y, doc.Y)) {
				// if this is the first avgIteration, then we need to initialize
				// the lastUpdate vector
				if (computeAvgParameters && avgIteration == 0)
					avgParamsLastUpdates.sum(iterParameters, 1.0f);

				Parse trueParse = ConditionalInferenceILP.inferILP(doc, scorer,
					iterParameters);
//				Parse trueParse = ConditionalInferenceILP.infer(doc, scorer, iterParameters);
				
//				if(trueParse.score < trueParseDash.score){
//					System.out.println("Suboptimal solution!!!!!!");
//				}
				
//				areSame(trueParse, trueParseDash);
//				if(!areSame(trueParse, trueParseDash)){
//					trueParse = ConditionalInferenceILP.inferILP(doc, scorer,
//							iterParameters);
//					trueParseDash = ConditionalInferenceILP.infer(doc, scorer, iterParameters);
//				}
				
				update(predictedParse, trueParse);
			}

			if (computeAvgParameters) avgIteration++;
			
			numOfEps++;
			if(numOfEps % 1000 == 0){
				long epE = System.currentTimeMillis();
				double epTime = (epE - epS) / 1000.0;
				System.out.println("Iter " + iteration  + " : " + "Processed " 
							+ numOfEps +" /  " +  trainingData.numDocs() + " in " + epTime + " s");
				epS = epE;
			}
		}
		
		long itE  = System.currentTimeMillis();
		double time = (itE - itS) / 1000.0;
		System.err.println("Time for iteration " + iteration + ": " + time + " s");
	}

	boolean areSame(Parse p1, Parse p2){

		boolean print = false;
		
		HashMap<Integer, Integer> p1Hash = new HashMap<Integer, Integer>();
		for(int z : p1.Z){
			if(!p1Hash.containsKey(z))
				p1Hash.put(z, 1);
			else {
				int cnt = p1Hash.get(z);
				cnt++;
				p1Hash.put(z, cnt);
			}
		}
		
		HashMap<Integer, Integer> p2Hash = new HashMap<Integer, Integer>();
		for(int z : p2.Z){
			if(!p2Hash.containsKey(z))
				p2Hash.put(z, 1);
			else {
				int cnt = p2Hash.get(z);
				cnt++;
				p2Hash.put(z, cnt);
			}
		}

		HashSet<Integer> keys = new HashSet<Integer>();
		keys.addAll(p1Hash.keySet());
		keys.addAll(p2Hash.keySet());
		for(int z : keys){

			if(! (p1Hash.containsKey(z) && p2Hash.containsKey(z) && p1Hash.get(z) == p2Hash.get(z))){
				print = true;
			}
		}
			
		if(print == true) {
			
			System.out.println(p1Hash.get(13) + " &" + p2Hash.get(13));
			System.out.print("Z[ilp] :");
			if(p1Hash.get(13) != p2Hash.get(13))
				System.out.println("why ??");
			for(int z : p1Hash.keySet()){
				System.out.print(z + ":" + p1Hash.get(z) + " ");
			}
			System.out.println();
			System.out.print("Z[orig] :");
			for(int z : p2Hash.keySet()){
				System.out.print(z + ":" + p2Hash.get(z) + " ");
			}
			System.out.println();
			System.out.println("-----");
		}

		//		if(p1.Z.length == 3){
//			
//			for(int i = 0; i < p1.Z.length; i++ ){
//				if(p1.Z[i] != p2.Z[i]){
//					print (p1.Y, p2.Y, "Y");
//					print (p1.Z, p2.Z, "Z");
//					return false;
//				}
//			}
//		}
		return true;
	}
	
	void print(int[] a, int [] b, String name){
		System.out.println(name+"-ilp");
		for(int i = 0; i < a.length; i ++)
			System.out.print(a[i] + " ");
		System.out.println();
		System.out.println(name+"-orig");
		for(int i = 0; i < b.length; i ++)
			System.out.print(b[i] + " ");
		System.out.println();
		System.out.println("---");
	}
	
	private boolean YsAgree(int[] y1, int[] y2) {
		if (y1.length != y2.length)
			return false;
		for (int i = 0; i < y1.length; i++)
			if (y1[i] != y2[i])
				return false;
		return true;
	}

	// a bit dangerous, since scorer.setDocument is called only inside inference
	public void update(Parse pred, Parse tru) {
		int numMentions = tru.Z.length;

		// iterate over mentions
		for (int m = 0; m < numMentions; m++) {
			int truRel = tru.Z[m];
			int predRel = pred.Z[m];

			if (truRel != predRel) {
				SparseBinaryVector v1a = scorer.getMentionRelationFeatures(
						tru.doc, m, truRel);
				updateRel(truRel, v1a, delta, computeAvgParameters);

				SparseBinaryVector v2a = scorer.getMentionRelationFeatures(
						tru.doc, m, predRel);
				updateRel(predRel, v2a, -delta, computeAvgParameters);
			}
		}
	}

	private void updateRel(int toState, SparseBinaryVector features,
			double delta, boolean useIterAverage) {
		iterParameters.relParameters[toState].addSparse(features, delta);

		if (useIterAverage) {
			DenseVector lastUpdatesIter = (DenseVector) avgParamsLastUpdatesIter.relParameters[toState];
			DenseVector lastUpdates = (DenseVector) avgParamsLastUpdates.relParameters[toState];
			DenseVector avg = (DenseVector) avgParameters.relParameters[toState];
			DenseVector iter = (DenseVector) iterParameters.relParameters[toState];
			for (int j = 0; j < features.num; j++) {
				int id = features.ids[j];
				if (lastUpdates.vals[id] != 0)
					avg.vals[id] += (avgIteration - lastUpdatesIter.vals[id])
							* lastUpdates.vals[id];

				lastUpdatesIter.vals[id] = avgIteration;
				lastUpdates.vals[id] = iter.vals[id];
			}
		}
	}

	private void finalizeRel() {
		for (int s = 0; s < model.numRelations; s++) {
			DenseVector lastUpdatesIter = (DenseVector) avgParamsLastUpdatesIter.relParameters[s];
			DenseVector lastUpdates = (DenseVector) avgParamsLastUpdates.relParameters[s];
			DenseVector avg = (DenseVector) avgParameters.relParameters[s];
			for (int id = 0; id < avg.vals.length; id++) {
				if (lastUpdates.vals[id] != 0) {
					avg.vals[id] += (avgIteration - lastUpdatesIter.vals[id])
							* lastUpdates.vals[id];
					lastUpdatesIter.vals[id] = avgIteration;
				}
			}
		}
	}
}
