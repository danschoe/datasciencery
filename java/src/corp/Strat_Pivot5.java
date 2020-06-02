package program;


class Strat_Pivot5 extends Strat_Pivot{
	
  static final String CMD="pivot";
  
  
  Strat_Pivot5(final String cmdExpression) {
	super(cmdExpression);  
  }
  
	
  @Override	
  public void calcPts() throws Exception{
    int cntNP=0;
    double fdPivot = calcdExpr[Strategy.begTstDateIndex+maxExprDysBk];
    
	for (int i=maxExprDysBk + 1 + Strategy.begTstDateIndex; i<=Strategy.endTstDateIndex; i++) {	
      if (calcdExpr[i]-calcdExpr[i-1] >= cmdfdMove) {
    	  if (cntNP < 0) cntNP = 0;   //* reset 
    	  cntNP += (int)((calcdExpr[i]-calcdExpr[i-1])/cmdfdMove);
          fdPivot = calcdExpr[i];  
      } else if (calcdExpr[i]-fdPivot <= -cmdfdMove) {
    	  if (cntNP > 0) cntNP = 0;   //* reset
    	  cntNP += (int)((calcdExpr[i]-calcdExpr[i-1])/cmdfdMove);
          fdPivot = calcdExpr[i];            
      }    
	  calcdExprFn[i] = cntNP;
	  System.out.println(cntNP + "  ");
	}  //* i loop
  }
    
  @Override  
  public void calcZ() throws Exception{  	
	//* calculates z-score move using current price but OLD pivot mu and std!	
	double mu_i;
	double fdStd_i;
	double fdZscore_i;
	int cntNP=0;
	maxExprDysBk += cmdStdPeriod - 1;
	
	for (int i=maxExprDysBk + 1 + Strategy.begTstDateIndex; i<=Strategy.endTstDateIndex; i++) {			
	  	  
	  //* Calc the stdev of "C-C1" 
      //* var_s = 1/(N-1)*Sum(x_i - mu)^2
	  //*       = ( N*Sum(x^2) - (Sum_x)^2 ) / N(N-1)
	  mu_i=0.0;
	  fdStd_i=0.0;
	  fdZscore_i=0.0;
	  for(int k=0; k<cmdStdPeriod; k++)
		mu_i += calcdExpr[i-k];
	  mu_i /= cmdStdPeriod;
	  for(int k=0; k<cmdStdPeriod; k++)
	    fdStd_i += Math.pow(calcdExpr[i-k] - mu_i,2);
	  fdStd_i /= (cmdStdPeriod-1);  //* unbiased
      fdStd_i = Math.sqrt(fdStd_i);
      
      fdZscore_i = (calcdExpr[i] - calcdExpr[i-1])/fdStd_i;  
      
      //if (calcdExpr[i] >= cmdfdMove*fdPivotStd+fdPivotMu) {
      if (fdZscore_i >= cmdfdMove) {
    	  if (cntNP < 0) cntNP = 0;
          cntNP++;
     
          
      } else if (fdZscore_i < -cmdfdMove) {    
    	  if (cntNP > 0) cntNP = 0;
    	  cntNP--;
      }    

      calcdExprFn[i] = cntNP;
 //System.out.println(cntNP + "  " + calcdExpr[i] + "  " + fdZscore_i);      
    }   //* i loop
  }
  
}
