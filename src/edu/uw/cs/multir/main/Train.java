package edu.uw.cs.multir.main;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import edu.uw.cs.multir.learning.algorithm.AveragedPerceptron;
import edu.uw.cs.multir.learning.algorithm.AveragedPerceptronILP;
import edu.uw.cs.multir.learning.algorithm.Model;
import edu.uw.cs.multir.learning.algorithm.Parameters;
import edu.uw.cs.multir.learning.data.Dataset;
import edu.uw.cs.multir.learning.data.MemoryDataset;

public class Train {

	public static void train(String dir, String type) throws IOException {
		
		Random random = new Random(1);
		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");
		
		Dataset train = new MemoryDataset(dir + File.separatorChar + "train");

		System.out.println("starting training");
		
		long start = System.currentTimeMillis();
		
		Parameters params = null;
		if(type.equals("baseline")){
			AveragedPerceptron ct = new AveragedPerceptron(model, random);
			params = ct.train(train);
		}
		else if(type.equals("ilp") || type.equals("noisyor")){
			AveragedPerceptronILP ct = new AveragedPerceptronILP(model, random);
			params = ct.train(train, type);
		}
		long end = System.currentTimeMillis();
		System.out.println("training time " + (end-start)/1000.0 + " seconds");

		params.serialize(dir + File.separatorChar + "params");
	}
}
