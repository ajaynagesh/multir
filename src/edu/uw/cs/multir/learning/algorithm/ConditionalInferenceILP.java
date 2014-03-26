package edu.uw.cs.multir.learning.algorithm;

import ilpInference.InferenceWrappers;

import java.util.Arrays;
import java.util.Comparator;

import edu.uw.cs.multir.learning.data.MILDocument;

public class ConditionalInferenceILP {

	public static Parse inferILP(MILDocument doc,
			Scorer parseScorer, Parameters params) {
		int numMentions = doc.numMentions;
		
		Parse parse = new Parse();
		parse.doc = doc;
		parseScorer.setParameters(params);
		
		// construct the 2D array scores s_ji --> for a mention {z_j = i}
		double[][] mentionScores = new double[doc.numMentions][params.model.numRelations];
		
		for(int m = 0; m < numMentions; m++){ // go over every mention
			for(int l = 0; l < params.model.numRelations; l ++){ // go over all the possible relation labels
				// mention 'm' taking label 'l' has  Score = scores[m][l] 
				mentionScores[m][l] = parseScorer.scoreMentionRelation(doc, m, l);
			}
		}
		
		int nilIndex = 0;
		InferenceWrappers ilpInfHandle = new InferenceWrappers();
		int z[] = ilpInfHandle.generateZUpdateILP(mentionScores, numMentions, doc.Y, nilIndex);
		
		// we can now write the results
		parse.Y = doc.Y;
		parse.Z = z;
		parse.score = 0;
		for (int i=0; i < numMentions; i++) {
			parse.score += mentionScores[i][z[i]];
		}
		
		return parse;
	}

	static class Edge {
		int m;
		int y;
		double score;		
		Edge(int m, int y, double score) {
			this.m = m;
			this.y = y;
			this.score = score;
		}
	}
}
