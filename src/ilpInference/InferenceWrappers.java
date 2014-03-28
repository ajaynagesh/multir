package ilpInference;

import net.sf.javailp.Constraint;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryLpSolve;

public class InferenceWrappers {
	
	
	public int [] generateZUpdateILP(double[][] mentionScores, 
											  int numOfMentions, 
											  int [] goldPos,
											  int nilIndex){
//		System.out.println("Calling ILP inference for Pr (Z | Y,X)");
//		System.out.println("Num of mentions : " + numOfMentions);
//		System.out.println("Relation labels : " + goldPos);

		int [] zUpdate = new int[numOfMentions];
		
		SolverFactory factory = new SolverFactoryLpSolve();
		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

		Problem problem = new Problem();
		Linear objective = new Linear();
		Linear constraint;

		if(goldPos.length > numOfMentions){
			//////////////Objective --------------------------------------
			for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
				double[] score = mentionScores[mentionIdx];
				for(int label : goldPos){
					if(label == nilIndex)
						continue; 
					
					String var = "z"+mentionIdx+"_"+"y"+label;
					double coeff = score[label];
					objective.add(coeff, var);

					//System.out.print(score.getCount(label) + "  " + "z"+mentionIdx+"_"+"y"+label + " + ");
				}
			}
		
			problem.setObjective(objective, OptType.MAX);
			
			/////////// Constraints ------------------------------------------
			
			/// 1. \Sum_{i \in Y'} z_ji = 1 \forall j
			for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
				constraint = new Linear();
				for(int y : goldPos){
					if(y == nilIndex)
						continue;
					
					String var = "z"+mentionIdx+"_"+"y"+y;
					constraint.add(1, var);
						
					//System.out.print("z"+mentionIdx+"_"+"y"+y + " + ");				
				}			
				problem.add(constraint, "=", 1);
				//System.out.println(" 0 = "+ "1");
			}
			
			/// 2. \Sum_j z_ji <= 1 \forall i
			for(int y : goldPos){
				if(y == nilIndex)
					continue; 
				
				constraint = new Linear();
				for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
					String var = "z"+mentionIdx+"_"+"y"+y;
					constraint.add(1, var);
				}
				problem.add(constraint, "<=", 1);
			}
		}	
		else {
			//////////////Objective --------------------------------------
			for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
				double[] score = mentionScores[mentionIdx];
				for(int label : goldPos){
					if(label == nilIndex)
						continue;
					
					String var = "z"+mentionIdx+"_"+"y"+label;
					double coeff = score[label];
					objective.add(coeff, var);

					//System.out.print(score.getCount(label) + "  " + "z"+mentionIdx+"_"+"y"+label + " + ");
				}
				//add the nil label
				String var = "z"+mentionIdx+"_"+"y"+nilIndex;
				double coeff = score[nilIndex];
				objective.add(coeff, var);
			}

			problem.setObjective(objective, OptType.MAX);

			/// 1. equality constraints \Sum_{i \in Y'} z_ji = 1 \forall j
			for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
				constraint = new Linear();
				for(int y : goldPos){
					if(y == nilIndex)
						continue;
					
					String var = "z"+mentionIdx+"_"+"y"+y;
					constraint.add(1, var);
						
					//System.out.print("z"+mentionIdx+"_"+"y"+y + " + ");				
				}
				//add the nil label
				constraint.add(1, "z"+mentionIdx+"_"+"y"+nilIndex); //nil index added to constraint
				
				problem.add(constraint, "=", 1);
				//System.out.println(" 0 = "+ "1");
			}
			
			//System.out.println("\n-----------------");
			/// 2. inequality constraint ===>  1 <= \Sum_j z_ji \forall i \in Y'  {lhs=1, since we consider only Y' i.e goldPos}
			/////////// ------------------------------------------------------
			for(int y : goldPos){
				if(y == nilIndex)
					continue; 
				
				constraint = new Linear();
				for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
					String var = "z"+mentionIdx+"_"+"y"+y;
					constraint.add(1, var);
					//System.out.print("z"+mentionIdx+"_"+"y"+y + " + ");
				}
				problem.add(constraint, ">=", 1);
				//System.out.println(" 0 - " + "y"+y +" >= 0" );
			}
			/////////// ------------------------------------------------------
		}
				
		// Set the types of all variables to Binary
		for(Object var : problem.getVariables())
			problem.setVarType(var, Boolean.class);
		
//		System.out.println("Num of variables : " + problem.getVariablesCount());
//		System.out.println("Num of Constraints : " + problem.getConstraintsCount());
//		System.out.println("Objective Function : ");
//		System.out.println(problem.getObjective());
//		System.out.println("Constraints : ");
//		for(Constraint c : problem.getConstraints())
//			System.out.println(c);
		
		// Solve the ILP problem by calling the ILP solver
		Solver solver = factory.get();
		Result result = solver.solve(problem);
		
		//System.out.println("Result : " + result);
		
		if(result == null){
			System.out.println("Num of variables : " + problem.getVariablesCount());
			System.out.println("Num of Constraints : " + problem.getConstraintsCount());
			System.out.println("Objective Function : ");
			System.out.println(problem.getObjective());
			System.out.println("Constraints : ");
			for(Constraint c : problem.getConstraints())
				System.out.println(c);
			
			System.out.println("Result is NULL ... Error ...");
			
			System.exit(0);

		}
		
		for(Object var : problem.getVariables()) {
			if(result.containsVar(var) && (result.get(var).intValue() == 1)){
				String [] split = var.toString().split("_");
				//System.out.println(split[0]);
				int mentionIdx = Integer.parseInt(split[0].toString().substring(1));
				//System.out.println(split[1]);
				int ylabel = Integer.parseInt(split[1].toString().substring(1));
				//if(ylabel != nilIndex)
				zUpdate[mentionIdx] = ylabel;
			}			
		}
	
		return zUpdate;
	}
	
	public YZPredicted generateYZPredictedILPnoisyOr(double[][] mentionScores,
			int numOfMentions, 
			int numYlabels, 
			int nilIndex){

		YZPredicted predictedVals = new YZPredicted(numOfMentions, numYlabels);

		int [] yPredicted = predictedVals.getYPredicted();
		int [] zPredicted = predictedVals.getZPredicted();

		//System.out.println("Calling ILP inference for Pr (Y,Z | X)");

		SolverFactory factory = new SolverFactoryLpSolve();
		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

		Problem problem = new Problem();

		Linear objective = new Linear();
		
		////////////// Objective --------------------------------------
		for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
			double[] scoreForMention = mentionScores[mentionIdx];
			for(int label = 0; label < numYlabels; label++){
				String var = "z"+mentionIdx+"_"+"y"+label;
				double coeff = scoreForMention[label];
				objective.add(coeff, var);

				//System.out.print(score.getCount(label) + "  " + "z"+mentionIdx+"_"+"y"+label + " + ");
			}
		}

		// noisy-or penalty in objective
		for(int y = 0; y < numYlabels; y++){
			String var = "e"+y;
			objective.add(-1, var);
		}
		
		problem.setObjective(objective, OptType.MAX);
		/////////// -----------------------------------------------------

		//System.out.println("\n-----------------");
		/////////// Constraints ------------------------------------------

		/// 1. equality constraints \Sum_i z_ji = 1 \forall j
		Linear constraint;
		for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
			constraint = new Linear();
			for(int y = 0; y < numYlabels; y++){
				String var = "z"+mentionIdx+"_"+"y"+y;
				constraint.add(1, var);

				//System.out.print("z"+mentionIdx+"_"+"y"+y + " + ");				
			}

			problem.add(constraint, "=", 1); // NOTE : To simulate Hoffmann 
			//System.out.println(" 0 = "+ "1");
		}

		//System.out.println("\n-----------------");

		/// 2. inequality constraint -- 1 ... z_ji <= y_i \forall j,i
		for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
			for(int y = 0; y < numYlabels; y++){
				constraint = new Linear();
				String var1 = "z"+mentionIdx+"_"+"y"+y;
				String var2 = "y"+y;
				constraint.add(1, var1);
				constraint.add(-1, var2);
				problem.add(constraint, "<=", 0);
				//System.out.println("z"+mentionIdx+"_"+"y"+y +" - " + "y"+y + " <= 0");
			}
		}

		//System.out.println("\n-----------------");
		/// 3. inequality constraint -- 2 ... y_i <= \Sum_j z_ji + e_i \forall i
		/////////// ------------------------------------------------------
		for(int y = 0; y < numYlabels; y++){
			constraint = new Linear();
			for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
				String var = "z"+mentionIdx+"_"+"y"+y;
				constraint.add(1, var);
				//System.out.print("z"+mentionIdx+"_"+"y"+y + " + ");
			}
			constraint.add(-1, "y"+y);
			constraint.add(1, "e"+y); // noise factor in constraint 3
			problem.add(constraint, ">=", 0);
			//System.out.println(" 0 - " + "y"+y +" >= 0" );
		}
		
		// Set the types of all variables to Binary
		for(Object var : problem.getVariables())
			problem.setVarType(var, Boolean.class);

		//System.out.println("Num of variables : " + problem.getVariablesCount());
		//System.out.println("Num of Constraints : " + problem.getConstraintsCount());
		//System.out.println("Objective Function : ");
		//System.out.println(problem.getObjective());
		//System.out.println("Constraints : ");
		//for(Constraint c : problem.getConstraints())
		//System.out.println(c);

		// Solve the ILP problem by calling the ILP solver
		Solver solver = factory.get();
		Result result = solver.solve(problem);

		//System.out.println("Result : " + result);

		if(result == null){
			//System.out.println("Num of variables : " + problem.getVariablesCount());
			//System.out.println("Num of Constraints : " + problem.getConstraintsCount());
			//System.out.println("Objective Function : ");
			//System.out.println(problem.getObjective());
			//System.out.println("Constraints : ");
			//for(Constraint c : problem.getConstraints())
			//System.out.println(c);

			System.out.println("Result is NULL ... Skipping this");

			return predictedVals;

		}

		for(Object var : problem.getVariables()) {
			if(result.containsVar(var) && (result.get(var).intValue() == 1)){
				if(var.toString().startsWith("y")) {
					//System.out.println(var + " = " + result.get(var) + " : Y-vars");
					int y = Integer.parseInt(var.toString().substring(1));
					if(y != nilIndex) {
						yPredicted[y] = result.get(var).intValue();
						// yPredicted.setCount(y, result.get(var).doubleValue());
					}
				}
				else if(var.toString().startsWith("z")) { 
					String [] split = var.toString().split("_");
					//System.out.println(split[0]);
					int mentionIdx = Integer.parseInt(split[0].toString().substring(1));
					//System.out.println(split[1]);
					int ylabel = Integer.parseInt(split[1].toString().substring(1));
					zPredicted[mentionIdx] = ylabel;

					//System.out.println(var + " = " + result.get(var) + " : Z-vars");

				}
			}
		}

		//System.out.println(yPredicted);
		//System.exit(0);

		return predictedVals;
	}
	
	public YZPredicted generateYZPredictedILP(double[][] mentionScores,
												  int numOfMentions, 
												  int numYlabels, 
												  int nilIndex){
		
		YZPredicted predictedVals = new YZPredicted(numOfMentions, numYlabels);
		
		int [] yPredicted = predictedVals.getYPredicted();
		int [] zPredicted = predictedVals.getZPredicted();
		
		//System.out.println("Calling ILP inference for Pr (Y,Z | X)");
		
		SolverFactory factory = new SolverFactoryLpSolve();
		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

		Problem problem = new Problem();
		
		Linear objective = new Linear();
		
		////////////// Objective --------------------------------------
		for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
			double[] scoreForMention = mentionScores[mentionIdx];
			for(int label = 0; label < numYlabels; label++){
				String var = "z"+mentionIdx+"_"+"y"+label;
				double coeff = scoreForMention[label];
				objective.add(coeff, var);
				
				//System.out.print(score.getCount(label) + "  " + "z"+mentionIdx+"_"+"y"+label + " + ");
			}
		}
		
		problem.setObjective(objective, OptType.MAX);
		/////////// -----------------------------------------------------
		 
		//System.out.println("\n-----------------");
		/////////// Constraints ------------------------------------------

		/// 1. equality constraints \Sum_i z_ji = 1 \forall j
		Linear constraint;
		for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
			constraint = new Linear();
			for(int y = 0; y < numYlabels; y++){
				String var = "z"+mentionIdx+"_"+"y"+y;
				constraint.add(1, var);
				
				//System.out.print("z"+mentionIdx+"_"+"y"+y + " + ");				
			}
			
			problem.add(constraint, "=", 1); // NOTE : To simulate Hoffmann 
			//System.out.println(" 0 = "+ "1");
		}
		
		//System.out.println("\n-----------------");
		
		/// 2. inequality constraint -- 1 ... z_ji <= y_i \forall j,i
		for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
			for(int y = 0; y < numYlabels; y++){
				constraint = new Linear();
				String var1 = "z"+mentionIdx+"_"+"y"+y;
				String var2 = "y"+y;
				constraint.add(1, var1);
				constraint.add(-1, var2);
				problem.add(constraint, "<=", 0);
				//System.out.println("z"+mentionIdx+"_"+"y"+y +" - " + "y"+y + " <= 0");
			}
		}
		
		//System.out.println("\n-----------------");
		/// 3. inequality constraint -- 2 ... y_i <= \Sum_j z_ji \forall i
		/////////// ------------------------------------------------------
		for(int y = 0; y < numYlabels; y++){
			constraint = new Linear();
			for(int mentionIdx = 0; mentionIdx < numOfMentions; mentionIdx ++){
				String var = "z"+mentionIdx+"_"+"y"+y;
				constraint.add(1, var);
				//System.out.print("z"+mentionIdx+"_"+"y"+y + " + ");
			}
			constraint.add(-1, "y"+y);
			problem.add(constraint, ">=", 0);
			//System.out.println(" 0 - " + "y"+y +" >= 0" );
		}
		
		// Set the types of all variables to Binary
		for(Object var : problem.getVariables())
			problem.setVarType(var, Boolean.class);
		
//		System.out.println("Num of variables : " + problem.getVariablesCount());
//		System.out.println("Num of Constraints : " + problem.getConstraintsCount());
//		System.out.println("Objective Function : ");
//		System.out.println(problem.getObjective());
//		System.out.println("Constraints : ");
//		for(Constraint c : problem.getConstraints())
//			System.out.println(c);
		
		// Solve the ILP problem by calling the ILP solver
		Solver solver = factory.get();
		Result result = solver.solve(problem);
		
		//System.out.println("Result : " + result);
		
		if(result == null){
//			System.out.println("Num of variables : " + problem.getVariablesCount());
//			System.out.println("Num of Constraints : " + problem.getConstraintsCount());
//			System.out.println("Objective Function : ");
//			System.out.println(problem.getObjective());
//			System.out.println("Constraints : ");
//			for(Constraint c : problem.getConstraints())
//				System.out.println(c);
			
			System.out.println("Result is NULL ... Skipping this");
			
			return predictedVals;

		}
		
		for(Object var : problem.getVariables()) {
			if(result.containsVar(var) && (result.get(var).intValue() == 1)){
				if(var.toString().startsWith("y")) {
					//System.out.println(var + " = " + result.get(var) + " : Y-vars");
					int y = Integer.parseInt(var.toString().substring(1));
					if(y != nilIndex) {
						yPredicted[y] = result.get(var).intValue();
						// yPredicted.setCount(y, result.get(var).doubleValue());
					}
				}
				else if(var.toString().startsWith("z")) { 
					String [] split = var.toString().split("_");
					//System.out.println(split[0]);
					int mentionIdx = Integer.parseInt(split[0].toString().substring(1));
					//System.out.println(split[1]);
					int ylabel = Integer.parseInt(split[1].toString().substring(1));
					zPredicted[mentionIdx] = ylabel;
					
					//System.out.println(var + " = " + result.get(var) + " : Z-vars");
					
				}
			}
		}
		
		//System.out.println(yPredicted);
		//System.exit(0);
		
		return predictedVals;
	}

}
