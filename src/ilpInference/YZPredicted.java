package ilpInference;

/**
 * TODO: Need to adapt to the new MultiR code-base
 * @author ajay
 *
 */
public class YZPredicted {
	  int [] yPredicted;
	  int [] zPredicted;
	  //double[] yPredictedScores;
	  
	  public YZPredicted(int numMentions, int numRelations){
		  yPredicted = new int[numRelations]; 
		  zPredicted = new int [numMentions];
		  //yPredictedScores = new double[numRelations];
	  }
	  public int[] getYPredicted(){
		  return yPredicted;
	  }
	  public int [] getZPredicted(){
		  return zPredicted;
	  }
//	  public double[] getYPredictedScores(){
//		  return yPredictedScores;
//	  }
}
