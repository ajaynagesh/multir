package edu.uw.cs.multir.learning.algorithm;

import ilpInference.InferenceWrappers;
import ilpInference.YZPredicted;
import edu.uw.cs.multir.learning.data.MILDocument;

public class FullInferenceILP {
	
	public static Parse inferILP(MILDocument entityPair,
			Scorer parseScorer, Parameters params, String trainType, String scoring) {
		Parse parse = new Parse();
		parse.doc = entityPair; // doc == entityPair
		parse.Z = new int[entityPair.numMentions];
		
		parseScorer.setParameters(params);
		
		// construct the 2D array scores s_ji --> for a mention {z_j = i}
		double[][] mentionScores = new double[entityPair.numMentions][params.model.numRelations];
		
		for (int m = 0; m < entityPair.numMentions; m++) { // go over every mention
			for (int l = 0; l < params.model.numRelations; l++){ // go over all the possible relation labels
				// mention 'm' taking label 'l' has  Score = scores[m][l] 
				mentionScores[m][l] = parseScorer.scoreMentionRelation(entityPair, m, l);
			}
		}
		
		int nilIndex = 0;
		InferenceWrappers ilpInfHandle = new InferenceWrappers();
		YZPredicted yz_pred_ilp = null; 
		
		if(trainType.equals("ilp"))
			yz_pred_ilp = ilpInfHandle.generateYZPredictedILP(mentionScores, entityPair.numMentions, 
																		params.model.numRelations, nilIndex);
		else if(trainType.equals("noisyor"))
			yz_pred_ilp = ilpInfHandle.generateYZPredictedILPnoisyOr(mentionScores, entityPair.numMentions, 
					params.model.numRelations, nilIndex);
		
		boolean[] binaryYs = new boolean[params.model.numRelations];
		int numYs = 0;
		double[] scores = new double[params.model.numRelations];
		for (int i=0; i < scores.length; i++) scores[i] = Double.NEGATIVE_INFINITY;		
		for (int m = 0; m < entityPair.numMentions; m++) {
			int label_m = yz_pred_ilp.getZPredicted()[m];
			parse.Z[m] = label_m;
			
			if (label_m > 0 && !binaryYs[label_m]) {
				binaryYs[label_m] = true;
				numYs++;
			}
			
			// Max scoring
			if(scoring == "max") {
				if (mentionScores[m][label_m] > scores[parse.Z[m]])
					scores[parse.Z[m]] = mentionScores[m][label_m];
			}
			
			// sum scoring
			if(scoring == "sum") {
				if (scores[parse.Z[m]] == Double.NEGATIVE_INFINITY)
					scores[parse.Z[m]] = mentionScores[m][label_m];
				else
					scores[parse.Z[m]] += mentionScores[m][label_m];
			}
		}

		parse.Y = new int[numYs];
		int pos = 0;
		for (int i=1; i < binaryYs.length; i++)
			if (binaryYs[i]) {
				parse.Y[pos++] = i;
				if (pos == numYs) break;
			}
		
		parse.scores = scores;
		
		/*
		 * Cross check the y labels set here and y labels returned from ilp
		 */
//		for(int i = 0; i < params.model.numRelations; i++){
//			if(!((binaryYs[i] == true && yz_pred_ilp.getYPredicted()[i] == 1)
//					|| (binaryYs[i] == false && yz_pred_ilp.getYPredicted()[i] == 0))){
//				System.out.println("The labels do not match ... how come ? ");
//			}
//		}
		//System.out.println("Labels match .. OK");
		/***
		 * Keeping the following piece of code as is
		 * TODO: Need to look at this later for the total score of a parse
		 */
		// It's important to ignore the _NO_RELATION_ type here, so
		// need to start at 1!
		// final value is avg of maxes
		int sumNum = 0;
		double sumSum = 0;
		for (int i=1; i < scores.length; i++)
			if (scores[i] > Double.NEGATIVE_INFINITY) { 
				sumNum++; sumSum += scores[i]; 
			}
		if (sumNum ==0) parse.score = Double.NEGATIVE_INFINITY;
		else parse.score = sumSum / sumNum;
		
		return parse;		
	}
}
