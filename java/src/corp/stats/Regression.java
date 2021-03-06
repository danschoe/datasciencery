/****************************************************************************************
 * Class Regression
 *
 * - linear linear regression
 * - multiple linear regression,
 * - polynomial
 * - non-linear regression - Nelder and Mead Simplex method
 * - fitting data to a Gaussian distribution, Log-Normal distribution, Logistics distribution,
 *   Lorentzian distribution, Poisson distribution, Beta distribution, Gamma distribution,
 *   Erlang distribution, Gumbel distribution, Frechet distrubution, Weibull distribution,
 *   Exponential distribution, a Rayleigh distribution, Pareto distribution, 
 *   rectangular hyberbola, sigmoid threshold function, a x^n/(theta^n + x^n) sigmoid function,
 *   scaled Heaviside step function.
 *
 * The regressions may be performed on sets of x - y data or on a single set of data which is first binned 
 * into an histogram.
 *
 * The sum of squares function required by the non-linear regression procedure is supplied via an interface, 
 * RegressionFunction or RegressionFunction2 (more than one dependent variable).
 * 
 * Usage: Regression reg = new Regression(xdata, ydata, weight)
 *
 ***************************************************************************************/

package program.stats;

import java.util.*;
import javax.swing.JOptionPane;

import program.stats.MathFns;
import program.stats.StatFns;
import program.stats.ErrorProp;
import program.stats.Db;
import program.io.FileOutput;
import program.io.DisplayRegression;
import program.graphics.*;
import program.AGlobal;


public class Regression{

  protected int N_YData0=0;             //* number of y data points inputed (in a single array if multiple y arrays)
  protected int N_YData=0;              //* number of y data points (N_YData0 times the number of y arrays)
  protected int N_Xarrays=1;     	    //* number of x arrays
  protected int N_Yarrays=1;     	    //* number of y arrays
  protected int N_Params=0;       	    //* number of unknown parameters to be estimated
                                        //*  multiple linear: a + b*x1 +c*x2 + . . ., = N_Xarrays + 1
                                        //*  polynomial fitting: = polynomial degree + 1
                                        //*  generalised linear: = N_Xarrays
                                        //*  simplex = no. of parameters to be estimated
  protected int degreesOfFreedom=0;         //* degrees of freedom = N_YData - N_Params
  protected double[][]  xData=null;      	//* X  data values
  protected double[]    yData=null;      	//* Y  data values
  protected double[]    yHat=null;      	// calculated y values using the regrssion coefficients
  protected double[]    weight=null;     	// weighting factors
  protected double[]    residual=null;   	// residuals
  protected double[]    residualW=null;  	// weighted residuals
  protected boolean     weightOpt=false;    // weighting factor option
                                            // = true; weights supplied
                                            // = false; weigths set to unity in regression
                                            //          average error used in statistacal methods
                                            // if any weight[i] = zero,
                                            //                    weighOpt is set to false and
                                            //                    all weights set to unity
    protected Vector<Double>  best = new Vector<Double>();      // best estimates vector of the unknown parameters
    protected Vector<Double>  bestSd = new Vector<Double>(); 	// standard deviation estimates of the best estimates of the unknown parameters
	protected double[]pseudoSd = null;          // Pseudo-nonlinear sd
    protected double  chiSquare=-10.0D;         // chi  square (observed-calculated)^2/variance
    protected double  reducedChiSquare=-10.0D;  // reduced chi square
    protected double  sumOfSquares=-10.0D;      // Sum of the squares of the residuals
    protected double  lastSSnoConstraint=0.0D;  // Last sum of the squares of the residuals with no constraint penalty
	protected double[][] covar=null;            // Covariance matrix
	protected double[][] corrCoeff=null;        // Correlation coefficient matrix
	protected double[][] yxData=null;
	protected double[][] rMatrix=null;
	protected double[][] pMatrix=null;
    protected double sampleR = -10.0D;          //* Sample linear correlation coefficient
	                                            //* if N_Xarrays = 1 it is the linear or 'linear approximation' correlation coefficient
	                                            //* if N_Xarrays > 1 it is the multiple correlation coefficient
    protected double sampleR2 = -10.0D;         //* Sample coefficient of determination
	                                            //* sampleR2 = sampleR*sampleR
    protected double sampleR2Adj = -10.0D;
	
	protected double multipleF = -10.0D;        // Multiple correlation coefficient F ratio

	protected String[] paraName = null;   	    // names of parameters, eg, mean, sd; c[0], c[1], c[2] . . .
	protected int prec = 4;               	    // number of places to which double variables are truncated on output to text files
	protected int field = 13;             	    // field width on output to text files

    protected int lastMethod=-1;       //* code indicating the last regression procedure attempted
                                       //* = 0 Multiple linear regression, y = a + b.x1 +c.x2 . . .
                                       //* = 1 Polynomial fitting, y = a +b.x +c.x^2 . . .
                                       //* = 2 generalised multiple linear y = a.f1(x) + b.f2(x) . . .
                                                // = 3 Nelder and Mead simplex
                                            	// = 4 Fit to a Gaussian distribution (see also 38 below)
                                             	// = 5 Fit to a Lorentzian distribution
                                                // = 6 Fit to a Poisson distribution
                                            	// = 7 Fit to a Two Parameter Gumbel distribution (minimum order statistic)
                                            	// = 8 Fit to a Two Parameter Gumbel distribution (maximum order statistic)
                                            	// = 9 Fit to a One Parameter Gumbel distribution (minimum order statistic)
                                            	// = 10 Fit to One Parameter Gumbel distribution (maximum order statistic)
                                            	// = 11 Fit to a Standard Gumbel distribution (minimum order statistic)
                                           	    // = 12 Fit to a Standard Gumbel distribution (maximum order statistic)
                                                // = 13 Fit to a Three parameter Frechet distribution
                                                // = 14 Fit to a Two Parameter Frechet distribution
                                                // = 15 Fit to a Standard Frechet distribution
                                                // = 16 Fit to a Three parameter Weibull distribution
                                                // = 17 Fit to a Two Parameter Weibull distribution
                                                // = 18 Fit to a Standard Weibull distribution
                                                // = 19 Fit to a Two Parameter Exponential distribution
                                                // = 20 Fit to a One Parameter Exponential distribution
                                                // = 21 Fit to a Standard Parameter Exponential distribution
                                                // = 22 Fit to a Rayleigh distribution
                                                // = 23 Fit to a Two Parameter Pareto distribution
                                                // = 24 Fit to a One Parameter Pareto distribution
                                                // = 25 Fit to a Sigmoidal Threshold Function
                                                // = 26 Fit to a rectangular Hyperbola
                                                // = 27 Fit to a scaled Heaviside Step Function
                                                // = 28 Fit to a Hills/Sips Sigmoid
                                                // = 29 Fit to a Three Parameter Pareto distribution
                                                // = 30 Fit to a Logistic distribution
                                                // = 31 Fit to a Beta distribution - [0, 1] interval
                                                // = 32 Fit to a Beta distribution - [min, max] interval
                                                // = 33 Fit to a Three Parameter Gamma distribution
                                                // = 34 Fit to a Standard Gamma distribution
                                                // = 35 Fit to an Erlang distribution
                                                // = 36 Fit to a two parameter log-normal distribution
                                                // = 37 Fit to a three parameter log-normal distribution
                                                // = 38 Fit to a Gaussian distribution  [allows fixed p-arameters] (see also 4 above)
                                                // = 39 Fit to a EC50 dose response curve
                                                // = 40 Fit to a LogEC50 dose response curve
                                                // = 41 Fit to a EC50 dose response curve - bottom constrained
                                                // = 42 Fit to a LogEC50 dose response curve- bottom constrained

    protected double kayValue = 0.0D;           // rate parameter value in Erlang distribution (method 35)

    protected boolean frechetWeibull = true;    // Frechet Weibull switch - if true Frechet, if false Weibull
    protected boolean linNonLin = true;         // if true linear method, if false non-linear method
    protected boolean trueFreq = false;   	    // true if xData values are true frequencies, e.g. in a fit to Gaussian
                                        	    // false if not
                                        	    // if true chiSquarePoisson (see above) is also calculated
    protected String xLegend = "x axis values"; // x axis legend in X-Y plot
    protected String yLegend = "y axis values"; // y axis legend in X-Y plot
    protected String graphTitle = " ";          // user supplied graph title
    protected boolean legendCheck = false;      // = true if above legends overwritten by user supplied legends
    protected boolean supressPrint = false;     // = true if print results is to be supressed
    protected boolean supressYYplot= false;     // = true if plot of experimental versus calculated is to be supressed
    protected boolean supressErrorMessages= false;  // = true if some designated error messages are to be supressed

    // Non-linear members
    protected boolean nlrStatus=true; 	    // Status of non-linear regression on exiting regression method
                                		    // = true  -  convergence criterion was met
                                		    // = false -  convergence criterion not met - current estimates returned
    protected int scaleOpt=0;     		    //  if = 0; no scaling of initial estimates
                                		    //  if = 1; initial simplex estimates scaled to unity
                                		    //  if = 2; initial estimates scaled by user provided values in scale[]
                                		    //  (default = 0)
    protected double[] scale = null;  	    // values to scale initial estimate (see scaleOpt above)
    protected boolean zeroCheck = false; 	// true if any best estimate value is zero
                                       		// if true the scale factor replaces the best estimate in numerical differentiation
    protected boolean penalty = false; 	    // true if single parameter penalty function is included
    protected boolean sumPenalty = false; 	// true if multiple parameter penalty function is included
    protected int nConstraints = 0; 		// number of single parameter constraints
    protected int nSumConstraints = 0; 		// number of multiple parameter constraints
    protected Vector<Object> penalties = new Vector<Object>();      // 3 method index,
                                                                    // number of single parameter constraints,
                                                                    // then repeated for each constraint:
                                                                    //  penalty parameter index,
                                                                    //  below or above constraint flag,
                                                                    //  constraint boundary value
    protected Vector<Object> sumPenalties = new Vector<Object>();   // constraint method index,
                                                                    // number of multiple parameter constraints,
                                                                    // then repeated for each constraint:
                                                                    //  number of parameters in summation
                                                                    //  penalty parameter indices,
                                                                    //  summation signs
                                                                    //  below or above constraint flag,
                                                                    //  constraint boundary value
    protected int[] penaltyCheck = null;  	    // = -1 values below the single constraint boundary not allowed
                                        	    // = +1 values above the single constraint boundary not allowed
    protected int[] sumPenaltyCheck = null;  	// = -1 values below the multiple constraint boundary not allowed
                                        	    // = +1 values above the multiple constraint boundary not allowed
    protected double penaltyWeight = 1.0e30;    // weight for the penalty functions
    protected int[] penaltyParam = null;   	    // indices of paramaters subject to single parameter constraint
    protected int[][] sumPenaltyParam = null;   // indices of paramaters subject to multiple parameter constraint
    protected int[][] sumPlusOrMinus = null;    // sign before each parameter in multiple parameter summation
    protected int[] sumPenaltyNumber = null;    // number of paramaters in each multiple parameter constraint

    protected double[] constraints = null; 	    // single parameter constraint values
    protected double[] sumConstraints = null;   // multiple parameter constraint values
    protected int constraintMethod = 0;         // constraint method number
                                                // =0: cliff to the power two (only method at present)

    protected boolean scaleFlag = true;         //  if true ordinate scale factor, Ao, included as unknown in fitting to special functions
                                                //  if false Ao set to unity (default value) or user provided value (in yScaleFactor)
    protected double yScaleFactor = 1.0D;       //  y axis factor - set if scaleFlag (above) = false
    protected int nMax = 3000;    		        //  Nelder and Mead simplex maximum number of iterations
    protected int nIter = 0;      		        //  Nelder and Mead simplex number of iterations performed
    protected int konvge = 3;     		        //  Nelder and Mead simplex number of restarts allowed
    protected int kRestart = 0;       	        //  Nelder and Mead simplex number of restarts taken
    protected double fMin = -1.0D;    	        //  Nelder and Mead simplex minimum value
    protected double fTol = 1e-9;     	        //  Nelder and Mead simplex convergence tolerance
    protected double rCoeff = 1.0D;   	        //  Nelder and Mead simplex reflection coefficient
    protected double eCoeff = 2.0D;   	        //  Nelder and Mead simplex extension coefficient
    protected double cCoeff = 0.5D;   	        //  Nelder and Mead simplex contraction coefficient
    protected double[] startH = null; 	        //  Nelder and Mead simplex initial estimates
    protected double[] step = null;   	        //  Nelder and Mead simplex step values
    protected double dStep = 0.5D;    	        // Nelder and Mead simplex default step value
    protected double[][] grad = null; 	        // Non-linear regression gradients
	protected double delta = 1e-4;    	        // Fractional step in numerical differentiation
	protected boolean invertFlag=true; 	        // Hessian Matrix ('linear' non-linear statistics) check
	                                 	        // true matrix successfully inverted, false inversion failed
	protected boolean posVarFlag=true; 	        // Hessian Matrix ('linear' non-linear statistics) check
	                                 	        // true - all variances are positive; false - at least one is negative
    protected int minTest = 0;    		        // Nelder and Mead minimum test
                                		        //  = 0; tests simplex sd < fTol
                                		        //  = 1; tests reduced chi suare or sum of squares < mean of abs(y values)*fTol
    protected double simplexSd = 0.0D;    	    // simplex standard deviation
    protected boolean statFlag = true;    	    // if true - statistical method called
                                        	    // if false - no statistical analysis
    protected boolean plotOpt = true;           // if true - plot of calculated values is cubic spline interpolation between the calculated values
                                                // if false - calculated values linked by straight lines (accomodates Poiwsson distribution plots)
    protected boolean multipleY = false;        // = true if y variable consists of more than set of data each needing a different calculation in RegressionFunction
                                                // when set to true - the index of the y value is passed to the function in Regression function

    protected double[] values = null;           // values entered into gaussianFixed
    protected boolean[] fixed = null;           // true if above values[i] is fixed, false if it is not


  // HISTOGRAM CONSTRUCTION
  //  Tolerance used in including an upper point in last histogram bin when it is outside due to riunding erors
  protected static double histTol = 1.0001D;

  //* CONSTRUCTORS
  // Default constructor - primarily facilitating the subclass ImpedSpecRegression
  public Regression() {
  }

  //* Constructor with data with x as 2D array and weights provided
  public Regression(double[][] xData, double[] yData, double[] weight){

    int n=weight.length;
    this.N_YData0 = yData.length;
    this.weightOpt=true;
    for (int i=0; i<n; i++) {
        if (weight[i]==0.0D){
            this.weightOpt=false;
            System.out.println("a weight in Regression equals zero; all weights set to 1.0");
         }
     }
     setDefaultValues(xData, yData, weight);
  }

  //* Constructor with both x and y as 2D arrays and weights provided
  public Regression(double[][] xxData, double[][] yyData, double[][] wWeight){
    this.multipleY = true;
    int nY1 = yyData.length;
    this.N_Yarrays = nY1;
    int nY2 = yyData[0].length;
    this.N_YData0 = nY2;
    int nX1 = xxData.length;
    int nX2 = xxData[0].length;
    double[] yData = new double[nY1*nY2];
    double[] weight = new double[nY1*nY2];
    double[][] xData = new double[nY1*nY2][nX1];
    int ii=0;
    for (int i=0; i<nY1; i++){
        int nY = yyData[i].length;
        if(nY!=nY2) throw new IllegalArgumentException("multiple y arrays must be of the same length");
        int nX = xxData[i].length;
        if(nY!=nX) throw new IllegalArgumentException("multiple y arrays must be of the same length as the x array length");
        for(int j=0; j<nY2; j++){
            yData[ii] = yyData[i][j];
            xData[ii][i] = xxData[i][j];
            weight[ii] = wWeight[i][j];
            ii++;
        }
    }

    int n=weight.length;
    this.weightOpt=true;
    for (int i=0; i<n; i++){
        if (weight[i]==0.0D){
            this.weightOpt=false;
            System.out.println("a weight in Regression equals zero; all weights set to 1.0");
        }
    }
    setDefaultValues(xData, yData, weight);
  }

  //* Constructor with data with x as 1D array and weights provided
  public Regression(double[] xxData, double[] yData, double[] weight){
    this.N_YData0 = yData.length;
    int n = xxData.length;
    int m = weight.length;
    double[][] xData = new double[1][n];
    for (int i=0; i<n; i++){
        xData[0][i]=xxData[i];
    }

    this.weightOpt=true;
    for (int i=0; i<m; i++){
        if (weight[i]==0.0D) {
            this.weightOpt=false;
            System.out.println("a weight in Regression equals zero; all weights set to 1.0");
        }
    }
    setDefaultValues(xData, yData, weight);
  }

	// Constructor with data with x as 1D array and y as 2D array and weights provided
    public Regression(double[] xxData, double[][] yyData, double[][] wWeight){

        this.multipleY = true;
        int nY1 = yyData.length;
        this.N_Yarrays = nY1;
        int nY2= yyData[0].length;
        this.N_YData0 = nY2;
        double[] yData = new double[nY1*nY2];
        double[] weight = new double[nY1*nY2];
        int ii=0;
        for(int i=0; i<nY1; i++){
            int nY = yyData[i].length;
            if(nY!=nY2)throw new IllegalArgumentException("multiple y arrays must be of the same length");
            for(int j=0; j<nY2; j++){
                yData[ii] = yyData[i][j];
                weight[ii] = wWeight[i][j];
                ii++;
            }
        }
        int n = xxData.length;
        if(n!=nY2)throw new IllegalArgumentException("x and y data lengths must be the same");
        double[][] xData = new double[1][nY1*n];
        ii=0;
        for(int j=0; j<nY1; j++){
            for(int i=0; i<n; i++){
                xData[0][ii]=xxData[i];
                ii++;
            }
        }

        this.weightOpt=true;
        int m = weight.length;
        for(int i=0; i<m; i++){
            if(weight[i]==0.0D){
                this.weightOpt=false;
                System.out.println("a weight in Regression equals zero; all weights set to 1.0");
            }
        }
        setDefaultValues(xData, yData, weight);
	}

  /* Constructor with data with x as 2D array and no weights provided
   */
  public Regression(double[][] xData, double[] yData){
    this.N_YData0 = yData.length;
    
    int N_Y = yData.length;
    double[] weight = new double[N_Y];
    this.weightOpt=false;
    for(int i=0; i<N_Y; i++) weight[i]=1.0D;

    setDefaultValues(xData, yData, weight);
  }

    // Constructor with data with x and y as 2D arrays and no weights provided
    public Regression(double[][] xxData, double[][] yyData){
        this.multipleY = true;
        int nY1 = yyData.length;
        this.N_Yarrays = nY1;
        int nY2 = yyData[0].length;
        this.N_YData0 = nY2;
        int nX1 = xxData.length;
        int nX2 = xxData[0].length;
        double[] yData = new double[nY1*nY2];
        double[][] xData = new double[nY1*nY2][nX1];
        int ii=0;
        for(int i=0; i<nY1; i++){
            int nY = yyData[i].length;
            if(nY!=nY2)throw new IllegalArgumentException("multiple y arrays must be of the same length");
            int nX = xxData[i].length;
            if(nY!=nX)throw new IllegalArgumentException("multiple y arrays must be of the same length as the x array length");
            for(int j=0; j<nY2; j++){
                yData[ii] = yyData[i][j];
                xData[ii][i] = xxData[i][j];
                ii++;
            }
        }

        int n = yData.length;
        double[] weight = new double[n];

        this.weightOpt=false;
        for(int i=0; i<n; i++)weight[i]=1.0D;

        setDefaultValues(xData, yData, weight);
	}

  /* Constructor with data with x as 1D array and no weights provided
   */
  public Regression(double[] x_Data, double[] yData){
    this.N_YData0 = yData.length;
    int N_X = x_Data.length;
    double[][] xData = new double[1][N_X];
    double[] weight = new double[N_X];

    for(int i=0; i<N_X; i++) xData[0][i] = x_Data[i];

    this.weightOpt=false;
    for(int i=0; i<N_X; i++)weight[i]=1.0D;

    setDefaultValues(xData, yData, weight);
  }

	// Constructor with data with x as 1D array and y as a 2D array and no weights provided
    public Regression(double[] xxData, double[][] yyData){
        this.multipleY = true;
        int nY1 = yyData.length;
        this.N_Yarrays = nY1;
        int nY2= yyData[0].length;
        this.N_YData0 = nY2;
        double[] yData = new double[nY1*nY2];
        int ii=0;
        for(int i=0; i<nY1; i++){
            int nY = yyData[i].length;
            if(nY!=nY2)throw new IllegalArgumentException("multiple y arrays must be of the same length");
            for(int j=0; j<nY2; j++){
                yData[ii] = yyData[i][j];
                ii++;
            }
        }

        double[][] xData = new double[1][nY1*nY2];
        double[] weight = new double[nY1*nY2];

        ii=0;
        int n = xxData.length;
        for(int j=0; j<nY1; j++){
            for(int i=0; i<n; i++){
                xData[0][ii]=xxData[i];
                weight[ii]=1.0D;
                ii++;
            }
        }
        this.weightOpt=false;

        setDefaultValues(xData, yData, weight);
	}

	// Constructor with data as a single array that has to be binned
	// bin width and value of the low point of the first bin provided
    public Regression(double[] xxData, double binWidth, double binZero){
        double[][] data = Regression.histogramBins(xxData, binWidth, binZero);
        int n = data[0].length;
        this.N_YData0 = n;
        double[][] xData = new double[1][n];
        double[] yData = new double[n];
        double[] weight = new double[n];
        for(int i=0; i<n; i++){
            xData[0][i]=data[0][i];
            yData[i]=data[1][i];
        }
        boolean flag = setTrueFreqWeights(yData, weight);
        if(flag){
            this.trueFreq=true;
            this.weightOpt=true;
        }
        else{
            this.trueFreq=false;
            this.weightOpt=false;
        }
        setDefaultValues(xData, yData, weight);
	}

	// Constructor with data as a single array that has to be binned
	// bin width provided
    public Regression(double[] xxData, double binWidth){
        double[][] data = Regression.histogramBins(xxData, binWidth);
        int n = data[0].length;
        this.N_YData0 = n;
        double[][] xData = new double[1][n];
        double[] yData = new double[n];
        double[] weight = new double[n];
        for(int i=0; i<n; i++){
            xData[0][i]=data[0][i];
            yData[i]=data[1][i];
        }
        boolean flag = setTrueFreqWeights(yData, weight);
        if(flag){
            this.trueFreq=true;
            this.weightOpt=true;
        }
        else{
            this.trueFreq=false;
            this.weightOpt=false;
        }
        setDefaultValues(xData, yData, weight);
	}

	// Enter data methods
	   // Enter data with x as 2D array and weights provided
    public void enterData(double[][] xData, double[] yData, double[] weight){

        int n=weight.length;
        this.N_YData0 = yData.length;
        this.weightOpt=true;
        for(int i=0; i<n; i++){
            if(weight[i]==0.0D){
                this.weightOpt=false;
                System.out.println("a weight in Regression equals zero; all weights set to 1.0");
            }
        }
        setDefaultValues(xData, yData, weight);
	}

	// Enter data with x and y as 2D arrays and weights provided
    public void enterData(double[][] xxData, double[][] yyData, double[][] wWeight){
        this.multipleY = true;
        int nY1 = yyData.length;
        this.N_Yarrays = nY1;
        int nY2 = yyData[0].length;
        this.N_YData0 = nY2;
        int nX1 = xxData.length;
        int nX2 = xxData[0].length;
        double[] yData = new double[nY1*nY2];
        double[] weight = new double[nY1*nY2];
        double[][] xData = new double[nY1*nY2][nX1];
        int ii=0;
        for(int i=0; i<nY1; i++){
            int nY = yyData[i].length;
            if(nY!=nY2)throw new IllegalArgumentException("multiple y arrays must be of the same length");
            int nX = xxData[i].length;
            if(nY!=nX)throw new IllegalArgumentException("multiple y arrays must be of the same length as the x array length");
            for(int j=0; j<nY2; j++){
                yData[ii] = yyData[i][j];
                xData[ii][i] = xxData[i][j];
                weight[ii] = wWeight[i][j];
                ii++;
            }
        }

        int n=weight.length;
        this.weightOpt=true;
        for(int i=0; i<n; i++){
            if(weight[i]==0.0D){
                this.weightOpt=false;
                System.out.println("a weight in Regression equals zero; all weights set to 1.0");
            }
        }
        setDefaultValues(xData, yData, weight);
	}

	// Enter data with x as 1D array and weights provided
    public void enterData(double[] xxData, double[] yData, double[] weight){
        this.N_YData0 = yData.length;
        int n = xxData.length;
        int m = weight.length;
        double[][] xData = new double[1][n];
        for(int i=0; i<n; i++){
            xData[0][i]=xxData[i];
        }

        this.weightOpt=true;
        for(int i=0; i<m; i++){
            if(weight[i]==0.0D){
                this.weightOpt=false;
                System.out.println("a weight in Regression equals zero; all weights set to 1.0");
            }
        }
        setDefaultValues(xData, yData, weight);
	}

	// Enter data with x as 1D array and y as 2D array and weights provided
    public void enterData(double[] xxData, double[][] yyData, double[][] wWeight){

        this.multipleY = true;
        int nY1 = yyData.length;
        this.N_Yarrays = nY1;
        int nY2= yyData[0].length;
        this.N_YData0 = nY2;
        double[] yData = new double[nY1*nY2];
        double[] weight = new double[nY1*nY2];
        int ii=0;
        for(int i=0; i<nY1; i++){
            int nY = yyData[i].length;
            if(nY!=nY2)throw new IllegalArgumentException("multiple y arrays must be of the same length");
            for(int j=0; j<nY2; j++){
                yData[ii] = yyData[i][j];
                weight[ii] = wWeight[i][j];
                ii++;
            }
        }
        int n = xxData.length;
        if(n!=nY2)throw new IllegalArgumentException("x and y data lengths must be the same");
        double[][] xData = new double[1][nY1*n];
        ii=0;
        for(int j=0; j<nY1; j++){
            for(int i=0; i<n; i++){
                xData[0][ii]=xxData[i];
                ii++;
            }
        }

        this.weightOpt=true;
        int m = weight.length;
        for(int i=0; i<m; i++){
            if(weight[i]==0.0D){
                this.weightOpt=false;
                System.out.println("a weight in Regression equals zero; all weights set to 1.0");
            }
        }
        setDefaultValues(xData, yData, weight);
	}

    // Enter data with x as 2D array and no weights provided
    public void enterData(double[][] xData, double[] yData){
        this.N_YData0 = yData.length;
        int n = yData.length;
        double[] weight = new double[n];

        this.weightOpt=false;
        for(int i=0; i<n; i++)weight[i]=1.0D;

        setDefaultValues(xData, yData, weight);
	}

    // Enter data with x and y as 2D arrays and no weights provided
    public void enterData(double[][] xxData, double[][] yyData){
        this.multipleY = true;
        int nY1 = yyData.length;
        this.N_Yarrays = nY1;
        int nY2 = yyData[0].length;
        this.N_YData0 = nY2;
        int nX1 = xxData.length;
        int nX2 = xxData[0].length;
        double[] yData = new double[nY1*nY2];
        double[][] xData = new double[nY1*nY2][nX1];
        int ii=0;
        for(int i=0; i<nY1; i++){
            int nY = yyData[i].length;
            if(nY!=nY2)throw new IllegalArgumentException("multiple y arrays must be of the same length");
            int nX = xxData[i].length;
            if(nY!=nX)throw new IllegalArgumentException("multiple y arrays must be of the same length as the x array length");
            for(int j=0; j<nY2; j++){
                yData[ii] = yyData[i][j];
                xData[ii][i] = xxData[i][j];
                ii++;
            }
        }

        int n = yData.length;
        double[] weight = new double[n];

        this.weightOpt=false;
        for(int i=0; i<n; i++)weight[i]=1.0D;

        setDefaultValues(xData, yData, weight);
	}

    // Enter data with x as 1D array and no weights provided
    public void enterData(double[] xxData, double[] yData){
        this.N_YData0 = yData.length;
        int n = xxData.length;
        double[][] xData = new double[1][n];
        double[] weight = new double[n];

        for(int i=0; i<n; i++)xData[0][i]=xxData[i];

        this.weightOpt=false;
        for(int i=0; i<n; i++)weight[i]=1.0D;

        setDefaultValues(xData, yData, weight);
	}

	// Enter data with x as 1D array and y as a 2D array and no weights provided
    public void enterData(double[] xxData, double[][] yyData){
        this.multipleY = true;
        int nY1 = yyData.length;
        this.N_Yarrays = nY1;
        int nY2= yyData[0].length;
        this.N_YData0 = nY2;
        double[] yData = new double[nY1*nY2];
        int ii=0;
        for(int i=0; i<nY1; i++){
            int nY = yyData[i].length;
            if(nY!=nY2)throw new IllegalArgumentException("multiple y arrays must be of the same length");
            for(int j=0; j<nY2; j++){
                yData[ii] = yyData[i][j];
                ii++;
            }
        }

        double[][] xData = new double[1][nY1*nY2];
        double[] weight = new double[nY1*nY2];

        ii=0;
        int n = xxData.length;
        for(int j=0; j<nY1; j++){
            for(int i=0; i<n; i++){
                xData[0][ii]=xxData[i];
                weight[ii]=1.0D;
                ii++;
            }
        }
        this.weightOpt=false;

        setDefaultValues(xData, yData, weight);
	}

	// Enter data as a single array that has to be binned
	// bin width and value of the low point of the first bin provided
    public void enterData(double[] xxData, double binWidth, double binZero){
        double[][] data = Regression.histogramBins(xxData, binWidth, binZero);
        int n = data[0].length;
        this.N_YData0 = n;
        double[][] xData = new double[1][n];
        double[] yData = new double[n];
        double[] weight = new double[n];
        for(int i=0; i<n; i++){
            xData[0][i]=data[0][i];
            yData[i]=data[1][i];
        }
        boolean flag = setTrueFreqWeights(yData, weight);
        if(flag){
            this.trueFreq=true;
            this.weightOpt=true;
        }
        else{
            this.trueFreq=false;
            this.weightOpt=false;
        }
        setDefaultValues(xData, yData, weight);
	}

	// Enter data as a single array that has to be binned
	// bin width provided
    public void enterData(double[] xxData, double binWidth){
        double[][] data = Regression.histogramBins(xxData, binWidth);
        int n = data[0].length;
        this.N_YData0 = n;
        double[][] xData = new double[1][n];
        double[] yData = new double[n];
        double[] weight = new double[n];
        for(int i=0; i<n; i++){
            xData[0][i]=data[0][i];
            yData[i]=data[1][i];
        }
        boolean flag = setTrueFreqWeights(yData, weight);
        if(flag){
            this.trueFreq=true;
            this.weightOpt=true;
        }
        else{
            this.trueFreq=false;
            this.weightOpt=false;
        }
        setDefaultValues(xData, yData, weight);
	}


    protected static boolean setTrueFreqWeights(double[] yData, double[] weight){
        int N_YData=yData.length;
        boolean flag = true;
        boolean unityWeight=false;

        // Set all weights to square root of frequency of occurence
        for(int ii=0; ii<N_YData; ii++){
            weight[ii]=Math.sqrt(Math.abs(yData[ii]));
        }

        // Check for zero weights and take average of neighbours as weight if it is zero
        for(int ii=0; ii<N_YData; ii++){
            double last = 0.0D;
            double next = 0.0D;
            if(weight[ii]==0){
                // find previous non-zero value
                boolean testLast = true;
                int iLast = ii - 1;
                while(testLast){
                    if(iLast<0){
                        testLast = false;
                    }
                    else{
                        if(weight[iLast]==0.0D){
                            iLast--;
                        }
                        else{
                            last = weight[iLast];
                            testLast = false;
                        }
                    }
                }

                // find next non-zero value
                boolean testNext = true;
                int iNext = ii + 1;
                while(testNext){
                    if(iNext>=N_YData){
                        testNext = false;
                    }
                    else{
                        if(weight[iNext]==0.0D){
                            iNext++;
                        }
                        else{
                            next = weight[iNext];
                            testNext = false;
                        }
                    }
                }

                // Take average
                weight[ii]=(last + next)/2.0D;
            }
        }
        return flag;
    }

  /* Set data and default values
   */ 
  protected void setDefaultValues(double[][] xData, double[] yData, double[] weight){
    this.N_YData = yData.length;
    this.N_Xarrays = xData.length;
    this.N_Params = this.N_Xarrays;
    this.yData = new double[N_YData];
    this.yHat = new double[N_YData];
    this.weight = new double[N_YData];
    this.residual = new double[N_YData];
    this.residualW = new double[N_YData];
    this.xData = new double[N_Xarrays][N_YData];
    int n=weight.length;
    
    if (n!=this.N_YData) 
    	throw new IllegalArgumentException("The weight and the y data lengths do not agree");   
    this.yxData = new double[this.N_Xarrays+1][this.N_YData];
    this.rMatrix = new double[this.N_Xarrays+1][this.N_Xarrays+1];
    this.pMatrix = new double[this.N_Xarrays+1][this.N_Xarrays+1];
    
    for (int i=0; i<this.N_YData; i++) {
        this.yData[i]=yData[i];
        this.weight[i]=weight[i];
        this.yxData[0][i] = yData[i];
    }
    for (int j=0; j<this.N_Xarrays; j++) {
        n=xData[j].length;
        if(n!=this.N_YData) throw new IllegalArgumentException("An x and the y data length do not agree");
        for (int i=0; i<this.N_YData; i++){
            this.xData[j][i]=xData[j][i];
            this.yxData[j+1][i]=xData[j][i];
        }
    }
    
  }

	// Supress printing of results
	public void supressPrint(){
	    this.supressPrint = true;
	}

	// Supress plot of calculated versus experimental values
	public void supressYYplot(){
	    this.supressYYplot = true;
	}

    // Supress convergence and chiSquare error messages
	public void supressErrorMessages(){
	    this.supressErrorMessages = true;
	}


    // Reset the ordinate scale factor option
    // true - Ao is unkown to be found by regression procedure
    // false - Ao set to unity
    public void setYscaleOption(boolean flag){
        this.scaleFlag=flag;
        if(flag==false)this.yScaleFactor = 1.0D;
    }

    // Reset the ordinate scale factor option
    // true - Ao is unkown to be found by regression procedure
    // false - Ao set to unity
    // retained for backward compatibility
    public void setYscale(boolean flag){
        this.scaleFlag=flag;
        if(flag==false)this.yScaleFactor = 1.0D;
    }

    // Reset the ordinate scale factor option
    // true - Ao is unkown to be found by regression procedure
    // false - Ao set to given value
    public void setYscaleFactor(double scale){
        this.scaleFlag=false;
        this.yScaleFactor = scale;
    }

    // Get the ordinate scale factor option
    // true - Ao is unkown
    // false - Ao set to unity
    public boolean getYscaleOption(){
        return this.scaleFlag;
    }

    // Get the ordinate scale factor option
    // true - Ao is unkown
    // false - Ao set to unity
    // retained to ensure backward compatibility
    public boolean getYscale(){
        return this.scaleFlag;
    }

    // Reset the true frequency test, trueFreq
    // true if yData values are true frequencies, e.g. in a fit to Gaussian; false if not
    // if true chiSquarePoisson (see above) is also calculated
    public void setTrueFreq(boolean trFr){
        boolean trFrOld = this.trueFreq;
        this.trueFreq = trFr;
        if(trFr){
            boolean flag = setTrueFreqWeights(this.yData, this.weight);
            if(flag){
                this.trueFreq=true;
                this.weightOpt=true;
            }
            else{
                this.trueFreq=false;
                this.weightOpt=false;
            }
        }
        else{
            if(trFrOld){
                for(int i=0; i<this.weight.length; i++){
                    weight[i]=1.0D;
                }
                this.weightOpt=false;
            }
        }
    }

    // Get the true frequency test, trueFreq
    public boolean getTrueFreq(){
        return this.trueFreq;
    }

    // Reset the x axis legend
    public void setXlegend(String legend){
        this.xLegend = legend;
        this.legendCheck=true;
    }

    // Reset the y axis legend
    public void setYlegend(String legend){
        this.yLegend = legend;
        this.legendCheck=true;
    }

     // Set the title
    public void setTitle(String title){
        this.graphTitle = title;
    }

  /* Multiple linear regression with intercept (including univariate y = ax + b)
   * y = a + b.x1 + c.x2 + d.x3 + . . .
   */
  public void calcLinear(){
    this.linear();	        
  }  
  
  public void linear(){
    if (this.multipleY) 
    	throw new IllegalArgumentException("This method cannot handle multidim y arrays");
    this.lastMethod = 0;
    this.linNonLin = true;
    this.N_Params = this.N_Xarrays+1;  //* coeff's + y-intercept
    this.degreesOfFreedom = this.N_YData - this.N_Params;
	if (this.degreesOfFreedom<1)
	    throw new IllegalArgumentException("Degrees of freedom must be greater than 0");
    double[][] aa = new double[this.N_Params][this.N_YData];

    for (int j=0; j<N_YData; j++)
    	aa[0][j]=1.0D;
    for (int i=1; i<N_Params; i++) {
        for (int j=0; j<N_YData; j++) {
            aa[i][j]=this.xData[i-1][j];
        }
    }
    this.generalLinear(aa);
    this.generalLinearStats(aa);
  }

    // Multiple linear regression with intercept (including y = ax + b)
    // plus plot and output file
    // y = a + b.x1 + c.x2 + d.x3 + . . .
    // legends provided
    public void linearPlot(String xLegend, String yLegend){
        this.xLegend = xLegend;
        this.yLegend = yLegend;
        this.legendCheck = true;
        
        this.linear();
        if(!this.supressPrint) this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY();
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }
    
    // Multiple linear regression with intercept (including y = ax + b)
    // plus plot and output file
    // y = a + b.x1 + c.x2 + d.x3 + . . .
    // no legends provided
    public void linearPlot(){
      this.linear();
      if(!this.supressPrint) this.print();
      int flag = 0;
        
      this.displayStats();
    
      if(this.xData.length<2)flag = this.plotXY();
      if(flag!=-2 && !this.supressYYplot)this.plotYY();  
    }

  
  public void plotLinear(){
    int flag = 0;
 
    if (this.xData.length<2)
    	flag = this.plotXY();
    //if (flag!=-2 && !this.supressYYplot)
    //	this.plotYY();
  } 
    
    //* Polynomial fitting
    //* y = a + b.x + c.x^2 + d.x^3 + . . .
    public void polynomial(int deg){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
        if(this.N_Xarrays>1)throw new IllegalArgumentException("This class will only perform a polynomial regression on a single x array");
        if(deg<1)throw new IllegalArgumentException("Polynomial degree must be greater than zero");
        this.lastMethod = 1;
        this.linNonLin = true;
        this.N_Params =  deg+1;
        this.degreesOfFreedom = this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");
        double[][] aa = new double[this.N_Params][this.N_YData];

        for(int j=0; j<N_YData; j++)aa[0][j]=1.0D;
        for(int j=0; j<N_YData; j++)aa[1][j]=this.xData[0][j];

        for(int i=2; i<N_Params; i++){
            for(int j=0; j<N_YData; j++){
                aa[i][j]=Math.pow(this.xData[0][j],i);
            }
        }
        this.generalLinear(aa);
        this.generalLinearStats(aa);
    }

    //* Polynomial fitting plus plot and output file
    //* y = a + b.x + c.x^2 + d.x^3 + . . .
    public void polynomialPlot(int deg, String xLegend, String yLegend){
        this.xLegend = xLegend;
        this.yLegend = yLegend;
        this.legendCheck = true;
        this.polynomial(deg);
        if(!this.supressPrint)this.print();
        int flag = this.plotXY();
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

    //* Polynomial fitting plus plot and output file, no legends
    //* y = a + b.x + c.x^2 + d.x^3 + . . .
    public void polynomialPlot(int deg){
        this.polynomial(deg);
        if(!this.supressPrint)this.print();
        int flag = this.plotXY();
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

    
    // Generalised linear regression
    // y = a.f1(x) + b.f2(x) + c.f3(x) + . . .
    public void linearGeneral(){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
        this.lastMethod = 2;

        this.linNonLin = true;
        this.N_Params = this.N_Xarrays;
        this.degreesOfFreedom = this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");
        this.generalLinear(this.xData);
        this.generalLinearStats(this.xData);
    }

    // Generalised linear regression plus plot and output file
    // y = a.f1(x) + b.f2(x) + c.f3(x) + . . .
    // legends provided
    public void linearGeneralPlot(String xLegend, String yLegend){
        this.xLegend = xLegend;
        this.yLegend = yLegend;
        this.legendCheck = true;
        this.linearGeneral();
        if(!this.supressPrint)this.print();
        if(!this.supressYYplot)this.plotYY();
    }

    // Generalised linear regression plus plot and output file
    // y = a.f1(x) + b.f2(x) + c.f3(x) + . . .
    // No legends provided
    public void linearGeneralPlot(){
        this.linearGeneral();
        if(!this.supressPrint)this.print();
        if(!this.supressYYplot)this.plotYY();
    }

  //* Generalised linear regression (called by linear(), linearGeneral() and polynomial())
  protected void generalLinear(double[][] xd){
        if(this.N_YData<=this.N_Params)throw new IllegalArgumentException("Number of unknown parameters is greater than or equal to the number of data points");
	    double sum=0.0D; 
	    //double sde, yHattemp=0.0D;
        double[][] a = new double[this.N_Params][this.N_Params];
        //double[][] h = new double[this.N_Params][this.N_Params];

        double[]b = new double[this.N_Params];
        double[]coeff = new double[this.N_Params];

		for (int i=0; i<N_Params; ++i){
			sum=0.0D ;
			for (int j=0; j<N_YData; ++j){
				sum += this.yData[j]*xd[i][j]/MathFns.square(this.weight[j]);
			}
			b[i]=sum;
		}
		for (int i=0; i<N_Params; ++i){
			for (int j=0; j<N_Params; ++j){
				sum=0.0;
				for (int k=0; k<N_YData; ++k){
					sum += xd[i][k]*xd[j][k]/MathFns.square(this.weight[k]);
				}
				a[j][i]=sum;
			}
		}
		Matrix aa = new Matrix(a);
		if(this.supressErrorMessages)aa.supressErrorMessage();
		coeff = aa.solveLinearSet(b);

		if(!this.best.isEmpty()){
		    int m=this.best.size();
		    for(int i=m-1; i>=0; i--){
		        this.best.removeElementAt(i);
		    }
		}
	    for(int i=0; i<this.N_Params; i++){
		    this.best.addElement(new Double(coeff[i]));
        }
  }

  //* Generalised linear regression statistics (called by linear(), linearGeneral() and polynomial())
  protected void generalLinearStats(double[][] xd){
    double sde, sum=0.0D, yHattemp=0.0D;
    //double[][] a = new double[this.N_Params][this.N_Params];
    double[][] h = new double[this.N_Params][this.N_Params];
    double[][] stat = new double[this.N_Params][this.N_Params];
    //double[][] cov = new double[this.N_Params][this.N_Params];
    this.covar = new double[this.N_Params][this.N_Params];
    this.corrCoeff = new double[this.N_Params][this.N_Params];
    double[]coeffSd = new double[this.N_Params];
    double[]coeff = new double[this.N_Params];

    for (int i=0; i<this.N_Params; i++){
        coeff[i] = ((Double) best.elementAt(i)).doubleValue();
    }

	if(this.weightOpt) this.chiSquare=0.0D;
	this.sumOfSquares=0.0D;
	for (int i=0; i< N_YData; ++i){
		yHattemp=0.0;
		for (int j=0; j<N_Params; ++j){
		    yHattemp += coeff[j]*xd[j][i];
		}
		this.yHat[i] = yHattemp;
		yHattemp -= this.yData[i];
		this.residual[i]=yHattemp;
		this.residualW[i]=yHattemp/weight[i];
		if(weightOpt) this.chiSquare += MathFns.square(yHattemp/this.weight[i]);
		this.sumOfSquares += MathFns.square(yHattemp);
	}
	if(this.weightOpt || this.trueFreq) this.reducedChiSquare = this.chiSquare/(this.degreesOfFreedom);
	double varY = this.sumOfSquares/(this.degreesOfFreedom);
	double sdY = Math.sqrt(varY);

    if (this.sumOfSquares==0.0D){
             for (int i=0; i<this.N_Params;i++){
                 coeffSd[i]=0.0D;
		         for (int j=0; j<this.N_Params;j++){
		             this.covar[i][j]=0.0D;
		             if (i==j) {
		                 this.corrCoeff[i][j]=1.0D;
		             }
		             else {
		                 this.corrCoeff[i][j]=0.0D;
		             }
		         }
		     }
    } else {
	    for (int i=0; i<this.N_Params; ++i){
	        for (int j=0; j<this.N_Params; ++j){
	            sum=0.0;
	    		for (int k=0; k<this.N_YData; ++k){
	    		    if (weightOpt){
	    		        sde = weight[k];
	    	        } else{
	    		        sde = sdY;
	                }
                    sum += xd[i][k]*xd[j][k]/MathFns.square(sde);
                }
	            h[j][i]=sum;
	    	}
	    }
        Matrix hh = new Matrix(h);
		if(this.supressErrorMessages) hh.supressErrorMessage();
		hh = hh.inverse();
		stat = hh.getArrayCopy();
		for (int j=0; j<N_Params; ++j){
		    coeffSd[j] = Math.sqrt(stat[j][j]);
		}

	    for (int i=0; i<this.N_Params;i++){
		    for (int j=0; j<this.N_Params;j++){
		        this.covar[i][j]=stat[i][j];
		    }
		}

		for (int i=0; i<this.N_Params;i++){
		    for (int j=0; j<this.N_Params;j++){
		        if (i==j){
		            this.corrCoeff[i][j] = 1.0D;
		        } else {
		            this.corrCoeff[i][j]=covar[i][j]/(coeffSd[i]*coeffSd[j]);
                }
            }
	    }
	}

    if (!this.bestSd.isEmpty()){
        int m=this.bestSd.size();
	    for (int i=m-1; i>=0; i--){
	        this.bestSd.removeElementAt(i);
	    }
    }
    for (int i=0; i<this.N_Params; i++){
        this.bestSd.addElement(new Double(coeffSd[i]));
    }

    if (this.N_Xarrays==1 && N_Yarrays==1){
        this.sampleR = StatFns.corrCoeff(this.xData[0], this.yData, this.weight);
        this.sampleR2 = this.sampleR*this.sampleR;
    } else {
        this.multCorrelCoeff(this.yData, this.yHat, this.weight);
    }
  }


    // Nelder and Mead Simplex Simplex Non-linear Regression
    protected void nelderMead(Vector vec, double[] start, double[] step, double fTol, int nMax){
        boolean testContract=false; // test whether a simplex contraction has been performed
        int np = start.length;  // number of unknown parameters;
        this.nlrStatus = true;
        this.N_Params = np;
        int nnp = np+1; // Number of simplex apices
        this.lastSSnoConstraint=0.0D;

        if(this.scaleOpt<2)this.scale = new double[np];
        if(scaleOpt==2 && scale.length!=start.length)throw new IllegalArgumentException("scale array and initial estimate array are of different lengths");
        if(step.length!=start.length)throw new IllegalArgumentException("step array length " + step.length + " and initial estimate array length " + start.length + " are of different");

        // check for zero step sizes
        for(int i=0; i<np; i++)if(step[i]==0.0D)throw new IllegalArgumentException("step " + i+ " size is zero");

	    // set up arrays
	    this.startH = new double[np];
	    this.step = new double[np];
	    double[]pmin = new double[np];   //Nelder and Mead Pmin

	    double[][] pp = new double[nnp][nnp];   //Nelder and Mead P
	    double[] yy = new double[nnp];          //Nelder and Mead y
	    double[] pbar = new double[nnp];        //Nelder and Mead P with bar superscript
	    double[] pstar = new double[nnp];       //Nelder and Mead P*
	    double[] p2star = new double[nnp];      //Nelder and Mead P**

        // mean of abs values of yData (for testing for minimum)
        double yabsmean=0.0D;
        for(int i=0; i<this.N_YData; i++)yabsmean += Math.abs(yData[i]);
        yabsmean /= this.N_YData;
        // degrees of freedom
        double degfree = (double)(this.degreesOfFreedom);

        // Set any single parameter constraint parameters
        if(this.penalty){
            Integer itemp = (Integer)this.penalties.elementAt(1);
            this.nConstraints = itemp.intValue();
            this.penaltyParam = new int[this.nConstraints];
            this.penaltyCheck = new int[this.nConstraints];
            this.constraints = new double[this.nConstraints];
            Double dtemp = null;
            int j=2;
            for(int i=0;i<this.nConstraints;i++){
                itemp = (Integer)this.penalties.elementAt(j);
                this.penaltyParam[i] = itemp.intValue();
                j++;
                itemp = (Integer)this.penalties.elementAt(j);
                this.penaltyCheck[i] = itemp.intValue();
                j++;
                dtemp = (Double)this.penalties.elementAt(j);
                this.constraints[i] = dtemp.doubleValue();
                j++;
            }
        }

        // Set any multiple parameter constraint parameters
        if(this.sumPenalty){
            Integer itemp = (Integer)this.sumPenalties.elementAt(1);
            this.nSumConstraints = itemp.intValue();
            this.sumPenaltyParam = new int[this.nSumConstraints][];
            this.sumPlusOrMinus = new int[this.nSumConstraints][];
            this.sumPenaltyCheck = new int[this.nSumConstraints];
            this.sumPenaltyNumber = new int[this.nSumConstraints];
            this.sumConstraints = new double[this.nSumConstraints];
            int[] itempArray = null;
            Double dtemp = null;
            int j=2;
            for(int i=0;i<this.nSumConstraints;i++){
                itemp = (Integer)this.sumPenalties.elementAt(j);
                this.sumPenaltyNumber[i] = itemp.intValue();
                j++;
                itempArray = (int[])this.sumPenalties.elementAt(j);
                this.sumPenaltyParam[i] = itempArray;
                j++;
                itempArray = (int[])this.sumPenalties.elementAt(j);
                this.sumPlusOrMinus[i] = itempArray;
                j++;
                itemp = (Integer)this.sumPenalties.elementAt(j);
                this.sumPenaltyCheck[i] = itemp.intValue();
                j++;
                dtemp = (Double)this.sumPenalties.elementAt(j);
                this.sumConstraints[i] = dtemp.doubleValue();
                j++;
            }
        }

        // Store unscaled start values
        for(int i=0; i<np; i++)this.startH[i]=start[i];

        // scale initial estimates and step sizes
        if(this.scaleOpt>0){
            boolean testzero=false;
            for(int i=0; i<np; i++)if(start[i]==0.0D)testzero=true;
            if(testzero){
                System.out.println("Neler and Mead Simplex: a start value of zero precludes scaling");
                System.out.println("Regression performed without scaling");
                this.scaleOpt=0;
            }
        }
        switch(this.scaleOpt){
            case 0: for(int i=0; i<np; i++)scale[i]=1.0D;
                    break;
            case 1: for(int i=0; i<np; i++){
                        scale[i]=1.0/start[i];
                        step[i]=step[i]/start[i];
                        start[i]=1.0D;
                    }
                    break;
            case 2: for(int i=0; i<np; i++){
                        step[i]*=scale[i];
                        start[i]*= scale[i];
                    }
                    break;
        }

        // set class member values
        this.fTol=fTol;
        this.nMax=nMax;
        this.nIter=0;
        for(int i=0; i<np; i++){
            this.step[i]=step[i];
            this.scale[i]=scale[i];
        }

	    // initial simplex
	    double sho=0.0D;
	    for (int i=0; i<np; ++i){
 	        sho=start[i];
	 	    pstar[i]=sho;
		    p2star[i]=sho;
		    pmin[i]=sho;
	    }

	    int jcount=this.konvge;  // count of number of restarts still available

	    for (int i=0; i<np; ++i){
	        pp[i][nnp-1]=start[i];
	    }
	    yy[nnp-1]=this.sumSquares(vec, start);
	    for (int j=0; j<np; ++j){
		    start[j]=start[j]+step[j];

		    for (int i=0; i<np; ++i)pp[i][j]=start[i];
		    yy[j]=this.sumSquares(vec, start);
		    start[j]=start[j]-step[j];
	    }

	    // loop over allowed iterations
        double  ynewlo=0.0D;    // current value lowest y
	    double 	ystar = 0.0D;   // Nelder and Mead y*
	    double  y2star = 0.0D;  // Nelder and Mead y**
	    double  ylo = 0.0D;     // Nelder and Mead y(low)
	    double  fMin;   // function value at minimum
	    // variables used in calculating the variance of the simplex at a putative minimum
	    double 	curMin = 00D, sumnm = 0.0D, summnm = 0.0D, zn = 0.0D;
	    int ilo=0;  // index of low apex
	    int ihi=0;  // index of high apex
	    int ln=0;   // counter for a check on low and high apices
	    boolean test = true;    // test becomes false on reaching minimum

	    while(test){
	        // Determine h
	        ylo=yy[0];
	        ynewlo=ylo;
    	    ilo=0;
	        ihi=0;
	        for (int i=1; i<nnp; ++i){
		        if (yy[i]<ylo){
			        ylo=yy[i];
			        ilo=i;
		        }
		        if (yy[i]>ynewlo){
			        ynewlo=yy[i];
			        ihi=i;
		        }
	        }
	        // Calculate pbar
	        for (int i=0; i<np; ++i){
		        zn=0.0D;
		        for (int j=0; j<nnp; ++j){
			        zn += pp[i][j];
		        }
		        zn -= pp[i][ihi];
		        pbar[i] = zn/np;
	        }

	        // Calculate p=(1+alpha).pbar-alpha.ph {Reflection}
	        for (int i=0; i<np; ++i)pstar[i]=(1.0 + this.rCoeff)*pbar[i]-this.rCoeff*pp[i][ihi];

	        // Calculate y*
	        ystar=this.sumSquares(vec, pstar);

	        ++this.nIter;

	        // check for y*<yi
	        if(ystar < ylo){
                // Form p**=(1+gamma).p*-gamma.pbar {Extension}
	            for (int i=0; i<np; ++i)p2star[i]=pstar[i]*(1.0D + this.eCoeff)-this.eCoeff*pbar[i];
	            // Calculate y**
	            y2star=this.sumSquares(vec, p2star);
	            ++this.nIter;
                if(y2star < ylo){
                    // Replace ph by p**
		            for (int i=0; i<np; ++i)pp[i][ihi] = p2star[i];
	                yy[ihi] = y2star;
	            }
	            else{
	                //Replace ph by p*
	                for (int i=0; i<np; ++i)pp[i][ihi]=pstar[i];
	                yy[ihi]=ystar;
	            }
	        }
	        else{
	            // Check y*>yi, i!=h
		        ln=0;
	            for (int i=0; i<nnp; ++i)if (i!=ihi && ystar > yy[i]) ++ln;
	            if (ln==np ){
	                // y*>= all yi; Check if y*>yh
                    if(ystar<=yy[ihi]){
                        // Replace ph by p*
	                    for (int i=0; i<np; ++i)pp[i][ihi]=pstar[i];
	                    yy[ihi]=ystar;
	                }
	                // Calculate p** =beta.ph+(1-beta)pbar  {Contraction}
	                for (int i=0; i<np; ++i)p2star[i]=this.cCoeff*pp[i][ihi] + (1.0 - this.cCoeff)*pbar[i];
	                // Calculate y**
	                y2star=this.sumSquares(vec, p2star);
	                ++this.nIter;
	                // Check if y**>yh
	                if(y2star>yy[ihi]){
	                    //Replace all pi by (pi+pl)/2

	                    for (int j=0; j<nnp; ++j){
		                    for (int i=0; i<np; ++i){
			                    pp[i][j]=0.5*(pp[i][j] + pp[i][ilo]);
			                    pmin[i]=pp[i][j];
		                    }
		                    yy[j]=this.sumSquares(vec, pmin);
	                    }
	                    this.nIter += nnp;
	                }
	                else{
	                    // Replace ph by p**
		                for (int i=0; i<np; ++i)pp[i][ihi] = p2star[i];
	                    yy[ihi] = y2star;
	                }
	            }
	            else{
	                // replace ph by p*
	                for (int i=0; i<np; ++i)pp[i][ihi]=pstar[i];
	                yy[ihi]=ystar;
	            }
	        }

            // test for convergence
            // calculte sd of simplex and minimum point
            sumnm=0.0;
	        ynewlo=yy[0];
	        ilo=0;
	        for (int i=0; i<nnp; ++i){
	            sumnm += yy[i];
	            if(ynewlo>yy[i]){
	                ynewlo=yy[i];
	                ilo=i;
	            }
	        }
	        sumnm /= (double)(nnp);
	        summnm=0.0;
	        for (int i=0; i<nnp; ++i){
		        zn=yy[i]-sumnm;
	            summnm += zn*zn;
	        }
	        curMin=Math.sqrt(summnm/np);

	        // test simplex sd
	        switch(this.minTest){
	            case 0:
                    if(curMin<fTol)test=false;
                    break;
	            case 1:
                    if(Math.sqrt(ynewlo/degfree)<yabsmean*fTol)test=false;
                    break;
		    }
            this.sumOfSquares=ynewlo;
	        if(!test){
	            // store best estimates
	            for (int i=0; i<np; ++i)pmin[i]=pp[i][ilo];
	            yy[nnp-1]=ynewlo;
	            // store simplex sd
	            this.simplexSd = curMin;
	            // test for restart
	            --jcount;
	            if(jcount>0){
	                test=true;
	   	            for (int j=0; j<np; ++j){
		                pmin[j]=pmin[j]+step[j];
		                for (int i=0; i<np; ++i)pp[i][j]=pmin[i];
		                yy[j]=this.sumSquares(vec, pmin);
		                pmin[j]=pmin[j]-step[j];
	                 }
	            }
	        }

	        if(test && this.nIter>this.nMax){
	            if(!this.supressErrorMessages){
	                System.out.println("Maximum iteration number reached, in Regression.simplex(...)");
	                System.out.println("without the convergence criterion being satisfied");
	                System.out.println("Current parameter estimates and sum of squares values returned");
	            }
	            this.nlrStatus = false;
	            // store current estimates
	            for (int i=0; i<np; ++i)pmin[i]=pp[i][ilo];
	            yy[nnp-1]=ynewlo;
                test=false;
            }

        }

	    if(!this.best.isEmpty()){
    		int m=this.best.size();
		    for(int i=m-1; i>=0; i--){
		        this.best.removeElementAt(i);
		        this.bestSd.removeElementAt(i);
		    }
	    }
	    for (int i=0; i<np; ++i){
            pmin[i] = pp[i][ilo];
            this.best.addElement(new Double(pmin[i]/this.scale[i]));
            this.scale[i]=1.0D; // unscale for statistical methods
        }
    	this.fMin=ynewlo;
    	this.kRestart=this.konvge-jcount;

        if(statFlag){
            pseudoLinearStats(vec);
        }
        else{
            for (int i=0; i<np; ++i){
                this.bestSd.addElement(new Double(Double.NaN));
            }
        }
	}

    // Nelder and Mead Simplex Simplex Non-linear Regression
    public void simplex(RegressionFunction g, double[] start, double[] step, double fTol, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fTol, nMax);
    }


    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
    public void simplexPlot(RegressionFunction g, double[] start, double[] step, double fTol, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fTol, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  maximum iterations
    public void simplex(RegressionFunction g, double[] start, double[] step, double fTol){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int nMaxx = this.nMax;
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fTol, nMaxx);
    }

    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
	// Default  maximum iterations
    public void simplexPlot(RegressionFunction g, double[] start, double[] step, double fTol){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex(g, start, step, fTol);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  tolerance
    public void simplex(RegressionFunction g, double[] start, double[] step, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        double fToll = this.fTol;
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fToll, nMax);
    }

    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
	// Default  tolerance
    public void simplexPlot(RegressionFunction g, double[] start, double[] step, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex(g, start, step, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  tolerance
	// Default  maximum iterations
    public void simplex(RegressionFunction g, double[] start, double[] step){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        double fToll = this.fTol;
        int nMaxx = this.nMax;
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fToll, nMaxx);
    }

    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
	// Default  tolerance
	// Default  maximum iterations
    public void simplexPlot(RegressionFunction g, double[] start, double[] step){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex(g, start, step);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default step option - all step[i] = dStep
    public void simplex(RegressionFunction g, double[] start, double fTol, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fTol, nMax);
    }

    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
	// Default step option - all step[i] = dStep
    public void simplexPlot(RegressionFunction g, double[] start, double fTol, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex(g, start, fTol, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplex(RegressionFunction g, double[] start, double fTol){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        int nMaxx = this.nMax;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fTol, nMaxx);
    }

    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
	// Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplexPlot(RegressionFunction g, double[] start, double fTol){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex(g, start, fTol);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
    // Default  tolerance
	// Default step option - all step[i] = dStep
    public void simplex(RegressionFunction g, double[] start, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        double fToll = this.fTol;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fToll, nMax);
    }

    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
    // Default  tolerance
	// Default step option - all step[i] = dStep
    public void simplexPlot(RegressionFunction g, double[] start, int nMax){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex(g, start, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
    // Default  tolerance
    // Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplex(RegressionFunction g, double[] start){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplex2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        int nMaxx = this.nMax;
        double fToll = this.fTol;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fToll, nMaxx);
    }

    // Nelder and Mead Simplex Simplex Non-linear Regression
    // plus plot and output file
    // Default  tolerance
    // Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplexPlot(RegressionFunction g, double[] start){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays\nsimplexPlot2 should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex(g, start);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }



    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    public void simplex2(RegressionFunction2 g, double[] start, double[] step, double fTol, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fTol, nMax);
    }


    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
    public void simplexPlot2(RegressionFunction2 g, double[] start, double[] step, double fTol, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fTol, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  maximum iterations
    public void simplex2(RegressionFunction2 g, double[] start, double[] step, double fTol){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int nMaxx = this.nMax;
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fTol, nMaxx);
    }

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
	// Default  maximum iterations
    public void simplexPlot2(RegressionFunction2 g, double[] start, double[] step, double fTol){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex2(g, start, step, fTol);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  tolerance
    public void simplex2(RegressionFunction2 g, double[] start, double[] step, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        double fToll = this.fTol;
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fToll, nMax);
    }

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
	// Default  tolerance
    public void simplexPlot2(RegressionFunction2 g, double[] start, double[] step, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex2(g, start, step, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  tolerance
	// Default  maximum iterations
    public void simplex2(RegressionFunction2 g, double[] start, double[] step){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        double fToll = this.fTol;
        int nMaxx = this.nMax;
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, step, fToll, nMaxx);
    }

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
	// Default  tolerance
	// Default  maximum iterations
    public void simplexPlot2(RegressionFunction2 g, double[] start, double[] step){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex2(g, start, step);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default step option - all step[i] = dStep
    public void simplex2(RegressionFunction2 g, double[] start, double fTol, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fTol, nMax);
    }

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
	// Default step option - all step[i] = dStep
    public void simplexPlot2(RegressionFunction2 g, double[] start, double fTol, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex2(g, start, fTol, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
	// Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplex2(RegressionFunction2 g, double[] start, double fTol){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        int nMaxx = this.nMax;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fTol, nMaxx);
    }

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
	// Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplexPlot2(RegressionFunction2 g, double[] start, double fTol){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex2(g, start, fTol);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
    // Default  tolerance
	// Default step option - all step[i] = dStep
    public void simplex2(RegressionFunction2 g, double[] start, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        double fToll = this.fTol;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fToll, nMax);
    }

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
    // Default  tolerance
	// Default step option - all step[i] = dStep
    public void simplexPlot2(RegressionFunction2 g, double[] start, int nMax){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex2(g, start, nMax);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

	// Nelder and Mead simplex
    // Default  tolerance
    // Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplex2(RegressionFunction2 g, double[] start){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int n=start.length;
        int nMaxx = this.nMax;
        double fToll = this.fTol;
        double[] stepp = new double[n];
        for(int i=0; i<n;i++)stepp[i]=this.dStep*start[i];
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.degreesOfFreedom = this.N_YData - start.length;
        this.nelderMead(vec, start, stepp, fToll, nMaxx);
    }

    // Nelder and Mead Simplex Simplex2 Non-linear Regression
    // plus plot and output file
    // Default  tolerance
    // Default  maximum iterations
	// Default step option - all step[i] = dStep
    public void simplexPlot2(RegressionFunction2 g, double[] start){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.lastMethod=3;
        this.linNonLin = false;
        this.zeroCheck = false;
        this.simplex2(g, start);
        if(!this.supressPrint)this.print();
        int flag = 0;
        if(this.xData.length<2)flag = this.plotXY2(g);
        if(flag!=-2 && !this.supressYYplot)this.plotYY();
    }

    // Calculate the sum of squares of the residuals for non-linear regression
	protected double sumSquares(Vector vec, double[] x){
	    RegressionFunction g1 = null;
	    RegressionFunction2 g2 = null;
	    if(this.multipleY){
            g2 = (RegressionFunction2)vec.elementAt(0);
        }
        else{
            g1 = (RegressionFunction)vec.elementAt(0);
        }

	    double ss = -3.0D;
	    double[] param = new double[this.N_Params];
	    double[] xd = new double[this.N_Xarrays];
	    // rescale
	    for(int i=0; i<this.N_Params; i++)param[i]=x[i]/scale[i];

        // single parameter penalty functions
        double tempFunctVal = this.lastSSnoConstraint;
        boolean test=true;
        if(this.penalty){
            int k=0;
            for(int i=0; i<this.nConstraints; i++){
                k = this.penaltyParam[i];
                if(this.penaltyCheck[i]==-1){
                    if(param[k]<constraints[i]){
                        ss = tempFunctVal + this.penaltyWeight*MathFns.square(param[k]-constraints[i]);
                        test=false;
                     }
                }
                if(this.penaltyCheck[i]==1){
                    if(param[k]>constraints[i]){
                        ss = tempFunctVal + this.penaltyWeight*MathFns.square(param[k]-constraints[i]);
	                    test=false;
                    }
                }
            }
        }

        // multiple parameter penalty functions
        if(this.sumPenalty){
            int kk = 0;
            int pSign = 0;
            double sumPenaltySum = 0.0D;
            for(int i=0; i<this.nSumConstraints; i++){
                for(int j=0; j<this.sumPenaltyNumber[i]; j++){
                    kk = this.sumPenaltyParam[i][j];
                    pSign = this.sumPlusOrMinus[i][j];
                    sumPenaltySum += param[kk]*pSign;
                }
                if(this.sumPenaltyCheck[i]==-1){
                    if(sumPenaltySum<sumConstraints[i]){
                        ss = tempFunctVal + this.penaltyWeight*MathFns.square(sumPenaltySum-sumConstraints[i]);
                        test=false;
                     }
                }
                if(this.sumPenaltyCheck[i]==1){
                    if(sumPenaltySum>sumConstraints[i]){
                        ss = tempFunctVal + this.penaltyWeight*MathFns.square(sumPenaltySum-sumConstraints[i]);
	                    test=false;
                    }
                }
            }
        }

        if(test){
            ss = 0.0D;
            for(int i=0; i<this.N_YData; i++){
                for(int j=0; j<N_Xarrays; j++)xd[j]=this.xData[j][i];
                if(!this.multipleY){
                    ss += MathFns.square((this.yData[i] - g1.function(param, xd))/this.weight[i]);
                }
                else{
                    ss += MathFns.square((this.yData[i] - g2.function(param, xd, i))/this.weight[i]);
                }

            }
            this.lastSSnoConstraint = ss;

        }


	    return ss;

	}

	// add a single parameter constraint boundary for the non-linear regression
	public void addConstraint(int paramIndex, int conDir, double constraint){
	    this.penalty=true;

        // First element reserved for method number if other methods than 'cliff' are added later
		if(this.penalties.isEmpty())this.penalties.addElement(new Integer(this.constraintMethod));

		// add constraint
	    if(penalties.size()==1){
		    this.penalties.addElement(new Integer(1));
		}
		else{
		    int nPC = ((Integer)this.penalties.elementAt(1)).intValue();
            nPC++;
            this.penalties.setElementAt(new Integer(nPC), 1);
		}
		this.penalties.addElement(new Integer(paramIndex));
 	    this.penalties.addElement(new Integer(conDir));
 	    this.penalties.addElement(new Double(constraint));
 	}

    // add a multiple parameter constraint boundary for the non-linear regression
	public void addConstraint(int[] paramIndices, int[] plusOrMinus, int conDir, double constraint){
	    int nCon = paramIndices.length;
	    int nPorM = plusOrMinus.length;
	    if(nCon!=nPorM)throw new IllegalArgumentException("num of parameters, " + nCon + ", does not equal number of parameter signs, " + nPorM);
	    this.sumPenalty=true;

        // First element reserved for method number if other methods than 'cliff' are added later
		if(this.sumPenalties.isEmpty())this.sumPenalties.addElement(new Integer(this.constraintMethod));

    	// add constraint
		if(sumPenalties.size()==1){
		    this.sumPenalties.addElement(new Integer(1));
		}
		else{
		    int nPC = ((Integer)this.sumPenalties.elementAt(1)).intValue();
            nPC++;
            this.sumPenalties.setElementAt(new Integer(nPC), 1);
		}
		this.sumPenalties.addElement(new Integer(nCon));
		this.sumPenalties.addElement(paramIndices);
		this.sumPenalties.addElement(plusOrMinus);
 	    this.sumPenalties.addElement(new Integer(conDir));
 	    this.sumPenalties.addElement(new Double(constraint));
 	}

	// remove all constraint boundaries for the non-linear regression
	public void removeConstraints(){

	    // check if single parameter constraints already set
	    if(!this.penalties.isEmpty()){
		    int m=this.penalties.size();

		    // remove single parameter constraints
    		for(int i=m-1; i>=0; i--){
		        this.penalties.removeElementAt(i);
		    }
		}
		this.penalty = false;
		this.nConstraints = 0;

	    // check if mutiple parameter constraints already set
	    if(!this.sumPenalties.isEmpty()){
		    int m=this.sumPenalties.size();

		    // remove multiple parameter constraints
    		for(int i=m-1; i>=0; i--){
		        this.sumPenalties.removeElementAt(i);
		    }
		}
		this.sumPenalty = false;
		this.nSumConstraints = 0;
	}

	//  linear statistics applied to a non-linear regression
    protected int pseudoLinearStats(Vector vec){
	    double	f1 = 0.0D, f2 = 0.0D, f3 = 0.0D, f4 = 0.0D; // intermdiate values in numerical differentiation
	    int	flag = 0;       // returned as 0 if method fully successful;
	                        // negative if partially successful or unsuccessful: check posVarFlag and invertFlag
	                        //  -1  posVarFlag or invertFlag is false;
	                        //  -2  posVarFlag and invertFlag are false
	    int np = this.N_Params;

	    double[] f = new double[np];
    	double[] pmin = new double[np];
    	double[] coeffSd = new double[np];
    	double[] xd = new double[this.N_Xarrays];
	    double[][]stat = new double[np][np];
	    pseudoSd = new double[np];

	    Double temp = null;

	    this.grad = new double[np][2];
	    this.covar = new double[np][np];
        this.corrCoeff = new double[np][np];

        // get best estimates
	    for (int i=0;i<np; ++i){
	        temp = (Double)this.best.elementAt(i);
	        pmin[i]=temp.doubleValue();
	    }

        // gradient both sides of the minimum
        double hold0 = 1.0D;
        double hold1 = 1.0D;
	    for (int i=0;i<np; ++i){
		    for (int k=0;k<np; ++k){
			    f[k]=pmin[k];
		    }
		    hold0=pmin[i];
            if(hold0==0.0D){
                hold0=this.step[i];
                this.zeroCheck=true;
            }
		    f[i]=hold0*(1.0D - this.delta);
	        this.lastSSnoConstraint=this.sumOfSquares;
		    f1=sumSquares(vec, f);
		    f[i]=hold0*(1.0 + this.delta);
	        this.lastSSnoConstraint=this.sumOfSquares;
		    f2=sumSquares(vec, f);
		    this.grad[i][0]=(this.fMin-f1)/Math.abs(this.delta*hold0);
		    this.grad[i][1]=(f2-this.fMin)/Math.abs(this.delta*hold0);
	    }

        // second patial derivatives at the minimum
	    this.lastSSnoConstraint=this.sumOfSquares;
	    for (int i=0;i<np; ++i){
		    for (int j=0;j<np; ++j){
			    for (int k=0;k<np; ++k){
				    f[k]=pmin[k];
			    }
			    hold0=f[i];
                if(hold0==0.0D){
                    hold0=this.step[i];
                    this.zeroCheck=true;
                }
			    f[i]=hold0*(1.0 + this.delta/2.0D);
			    hold0=f[j];
                if(hold0==0.0D){
                    hold0=this.step[j];
                    this.zeroCheck=true;
                }
			    f[j]=hold0*(1.0 + this.delta/2.0D);
        	    this.lastSSnoConstraint=this.sumOfSquares;
			    f1=sumSquares(vec, f);
			    f[i]=pmin[i];
			    f[j]=pmin[j];
			    hold0=f[i];
                if(hold0==0.0D){
                    hold0=this.step[i];
                    this.zeroCheck=true;
                }
 			    f[i]=hold0*(1.0 - this.delta/2.0D);
			    hold0=f[j];
                if(hold0==0.0D){
                    hold0=this.step[j];
                    this.zeroCheck=true;
                }
 		        f[j]=hold0*(1.0 + this.delta/2.0D);
	            this.lastSSnoConstraint=this.sumOfSquares;
			    f2=sumSquares(vec, f);
			    f[i]=pmin[i];
			    f[j]=pmin[j];
			    hold0=f[i];
                if(hold0==0.0D){
                    hold0=this.step[i];
                    this.zeroCheck=true;
                }
    		    f[i]=hold0*(1.0 + this.delta/2.0D);
    		    hold0=f[j];
                if(hold0==0.0D){
                    hold0=this.step[j];
                    this.zeroCheck=true;
                }
			    f[j]=hold0*(1.0 - this.delta/2.0D);
	            this.lastSSnoConstraint=this.sumOfSquares;
			    f3=sumSquares(vec, f);
			    f[i]=pmin[i];
			    f[j]=pmin[j];
			    hold0=f[i];
                if(hold0==0.0D){
                    hold0=this.step[i];
                    this.zeroCheck=true;
                }
			    f[i]=hold0*(1.0 - this.delta/2.0D);
			    hold0=f[j];
                if(hold0==0.0D){
                    hold0=this.step[j];
                    this.zeroCheck=true;
                }
			    f[j]=hold0*(1.0 - this.delta/2.0D);
	            this.lastSSnoConstraint=this.sumOfSquares;
			    f4=sumSquares(vec, f);
			    stat[i][j]=(f1-f2-f3+f4)/(this.delta*this.delta);
		    }
	    }

        double ss=0.0D;
        double sc=0.0D;
	    for(int i=0; i<this.N_YData; i++){
            for(int j=0; j<N_Xarrays; j++)xd[j]=this.xData[j][i];
            if(this.multipleY){
	            this.yHat[i] = ((RegressionFunction2)vec.elementAt(0)).function(pmin, xd, i);
	        }
	        else{
	            this.yHat[i] = ((RegressionFunction)vec.elementAt(0)).function(pmin, xd);
	        }
	        this.residual[i] = this.yHat[i]-this.yData[i];
	        ss += MathFns.square(this.residual[i]);
	        this.residualW[i] = this.residual[i]/this.weight[i];
	        sc += MathFns.square(this.residualW[i]);
	    }
	    this.sumOfSquares = ss;
	    double varY = ss/(this.N_YData-np);
	    double sdY = Math.sqrt(varY);
	    if(this.weightOpt || this.trueFreq){
	        this.chiSquare=sc;
	        this.reducedChiSquare=sc/(this.N_YData-np);
	    }

        // calculate reduced sum of squares
        double red=1.0D;
        if(!this.weightOpt && !this.trueFreq)red=this.sumOfSquares/(this.N_YData-np);

        // calculate pseudo errors  -  reduced sum of squares over second partial derivative
        for(int i=0; i<np; i++){
            pseudoSd[i] = (2.0D*this.delta*red*Math.abs(pmin[i]))/(grad[i][1]-grad[i][0]);
            if(pseudoSd[i]>=0.0D){
                pseudoSd[i] = Math.sqrt(pseudoSd[i]);
            }
            else{
                pseudoSd[i] = Double.NaN;
            }
        }

        // calculate covariance matrix
	    if(np==1){
	        hold0=pmin[0];
            if(hold0==0.0D)hold0=this.step[0];
	        stat[0][0]=1.0D/stat[0][0];
		    this.covar[0][0] = stat[0][0]*red*hold0*hold0;
		    if(covar[0][0]>=0.0D){
			    coeffSd[0]=Math.sqrt(this.covar[0][0]);
			    corrCoeff[0][0]=1.0D;
			}
	        else{
			    coeffSd[0]=Double.NaN;
			    corrCoeff[0][0]=Double.NaN;
			    this.posVarFlag=false;
			}
		}
		else{
            Matrix cov = new Matrix(stat);
		    if(this.supressErrorMessages)cov.supressErrorMessage();
            cov = cov.inverse();
            this.invertFlag = cov.getMatrixCheck();
            if(this.invertFlag==false)flag--;
            stat = cov.getArrayCopy();

	        this.posVarFlag=true;
	        if (this.invertFlag){
		        for (int i=0; i<np; ++i){
		            hold0=pmin[i];
                    if(hold0==0.0D)hold0=this.step[i];
			        for (int j=i; j<np;++j){
			            hold1=pmin[j];
                        if(hold1==0.0D)hold1=this.step[j];
				        this.covar[i][j] = 2.0D*stat[i][j]*red*hold0*hold1;
				        this.covar[j][i] = this.covar[i][j];
			        }
			        if(covar[i][i]>=0.0D){
			            coeffSd[i]=Math.sqrt(this.covar[i][i]);
			        }
			        else{
			            coeffSd[i]=Double.NaN;
			            this.posVarFlag=false;
			        }
		        }

		        for (int i=0; i<np; ++i){
			        for (int j=0; j<np; ++j){
			            if((coeffSd[i]!= Double.NaN) && (coeffSd[j]!= Double.NaN)){
			                this.corrCoeff[i][j] = this.covar[i][j]/(coeffSd[i]*coeffSd[j]);
			            }
			            else{
			                this.corrCoeff[i][j]= Double.NaN;
			            }
			        }
		        }
 	        }
 	        else{
		        for (int i=0; i<np; ++i){
			        for (int j=0; j<np;++j){
			            this.covar[i][j] = Double.NaN;
			            this.corrCoeff[i][j] = Double.NaN;
			        }
			        coeffSd[i]=Double.NaN;
			        this.posVarFlag=false;
		        }
		    }
		}
	    if(this.posVarFlag==false)flag--;

	    for(int i=0; i<this.N_Params; i++){
		    this.bestSd.addElement(new Double(coeffSd[i]));
	      }

        this.multCorrelCoeff(this.yData, this.yHat, this.weight);

        return flag;

	}

  //* Print the results of the regression
  // File name provided
  // prec = truncation precision
	public void print(String filename, int prec){
	    this.prec = prec;
	    this.print(filename);
	}

	// Print the results of the regression
	// No file name provided
	// prec = truncation precision
	public void print(int prec){
	    this.prec = prec;
		String filename="RegressionOutput.txt";
        this.print(filename);
	}

  //* Print the results of the regression
  //* No file name provided
  public void print(){
    String filename="RegressOutput.txt";
	this.print(filename);
  }
	  
  //* Print the results of the regression
  //* File name provided
  //* default value for truncation precision
  public void print(String filename){
    if (filename.indexOf('.')==-1) filename = filename+".txt";
	FileOutput fout = new FileOutput(filename, 'n');
	fout.dateAndTimeln(filename);
	fout.println(this.graphTitle);
	paraName = new String[this.N_Params];
	if (lastMethod==38) paraName = new String[3];
    if (weightOpt){
        fout.println("Weighted Least Squares Minimisation");
    } else {
        fout.println("Unweighted Least Squares Minimisation");
    }
    switch(this.lastMethod){
	  case 0: fout.println("Linear Regression with intercept");
              fout.println("yhat = c[0] + c[1]*x1 + c[2]*x2 +c[3]*x3 + . . .");
              for(int i=0;i<this.N_Params;i++) this.paraName[i]="c["+i+"]";
              this.linearPrint(fout);
	          break;
	  case 1: fout.println("Polynomial (with degree = " + (N_Params-1) + ") Fitting Linear Regression");
	                fout.println("yhat = c[0] + c[1]*x + c[2]*x^2 + c[3]*x^3 + . . .");
	                for(int i=0;i<this.N_Params;i++)this.paraName[i]="c["+i+"]";
                    this.linearPrint(fout);
	                break;
	        case 2: fout.println("Generalised linear regression");
	                fout.println("y = c[0]*f1(x) + c[1]*f2(x) + c[2]*f3(x) + . . .");
	                for(int i=0;i<this.N_Params;i++)this.paraName[i]="c["+i+"]";
                    this.linearPrint(fout);
	                break;
	        case 3: fout.println("Nelder and Mead Simplex Non-linear Regression");
	                fout.println("y = f(x1, x2, x3 . . ., c[0], c[1], c[2] . . .");
	                fout.println("y is non-linear with respect to the c[i]");
	                for(int i=0;i<this.N_Params;i++)this.paraName[i]="c["+i+"]";
                    this.nonLinearPrint(fout);
	                break;
	        case 4: fout.println("Fitting to a Normal (Gaussian) distribution");
	                fout.println("y = (yscale/(sd.sqrt(2.pi)).exp(0.5.square((x-mean)/sd))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mean";
	                paraName[1]="sd";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 5: fout.println("Fitting to a Lorentzian distribution");
	                fout.println("y = (yscale/pi).(gamma/2)/((x-mean)^2+(gamma/2)^2)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mean";
	                paraName[1]="gamma";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
	                break;
            case 6: fout.println("Fitting to a Poisson distribution");
	                fout.println("y = yscale.mu^k.exp(-mu)/mu!");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mean";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 7: fout.println("Fitting to a Two Parameter Minimum Order Statistic Gumbel [Type 1 Extreme Value] Distribution");
	                fout.println("y = (yscale/sigma)*exp((x - mu)/sigma))*exp(-exp((x-mu)/sigma))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="sigma";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 8: fout.println("Fitting to a Two Parameter Maximum Order Statistic Gumbel [Type 1 Extreme Value] Distribution");
	                fout.println("y = (yscale/sigma)*exp(-(x - mu)/sigma))*exp(-exp(-(x-mu)/sigma))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="sigma";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 9: fout.println("Fitting to a One Parameter Minimum Order Statistic Gumbel [Type 1 Extreme Value] Distribution");
	                fout.println("y = (yscale)*exp(x/sigma))*exp(-exp(x/sigma))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
		            paraName[0]="sigma";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 10: fout.println("Fitting to a One Parameter Maximum Order Statistic Gumbel [Type 1 Extreme Value] Distribution");
	                fout.println("y = (yscale)*exp(-x/sigma))*exp(-exp(-x/sigma))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
		            paraName[0]="sigma";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 11: fout.println("Fitting to a Standard Minimum Order Statistic Gumbel [Type 1 Extreme Value] Distribution");
	                fout.println("y = (yscale)*exp(x))*exp(-exp(x))");
	                fout.println("Linear regression used to fit y = yscale*z where z = exp(x))*exp(-exp(x)))");
	                if(this.scaleFlag)paraName[0]="y scale";
                    this.linearPrint(fout);
                    break;
            case 12: fout.println("Fitting to a Standard Maximum Order Statistic Gumbel [Type 1 Extreme Value] Distribution");
	                fout.println("y = (yscale)*exp(-x))*exp(-exp(-x))");
	                fout.println("Linear regression used to fit y = yscale*z where z = exp(-x))*exp(-exp(-x)))");
	                if(this.scaleFlag)paraName[0]="y scale";
                    this.linearPrint(fout);
                    break;
	        case 13: fout.println("Fitting to a Three Parameter Frechet [Type 2 Extreme Value] Distribution");
	                fout.println("y = yscale.(gamma/sigma)*((x - mu)/sigma)^(-gamma-1)*exp(-((x-mu)/sigma)^-gamma");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="sigma";
	                paraName[2]="gamma";
	                if(this.scaleFlag)paraName[3]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 14: fout.println("Fitting to a Two parameter Frechet [Type2  Extreme Value] Distribution");
	                fout.println("y = yscale.(gamma/sigma)*(x/sigma)^(-gamma-1)*exp(-(x/sigma)^-gamma");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="sigma";
	                paraName[1]="gamma";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
  	        case 15: fout.println("Fitting to a Standard Frechet [Type 2 Extreme Value] Distribution");
	                fout.println("y = yscale.gamma*(x)^(-gamma-1)*exp(-(x)^-gamma");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="gamma";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
	        case 16: fout.println("Fitting to a Three parameter Weibull [Type 3 Extreme Value] Distribution");
	                fout.println("y = yscale.(gamma/sigma)*((x - mu)/sigma)^(gamma-1)*exp(-((x-mu)/sigma)^gamma");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="sigma";
	                paraName[2]="gamma";
	                if(this.scaleFlag)paraName[3]="y scale";
                    this.nonLinearPrint(fout);
                    break;
  	        case 17: fout.println("Fitting to a Two parameter Weibull [Type 3 Extreme Value] Distribution");
	                fout.println("y = yscale.(gamma/sigma)*(x/sigma)^(gamma-1)*exp(-(x/sigma)^gamma");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="sigma";
	                paraName[1]="gamma";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
  	        case 18: fout.println("Fitting to a Standard Weibull [Type 3 Extreme Value] Distribution");
	                fout.println("y = yscale.gamma*(x)^(gamma-1)*exp(-(x)^gamma");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="gamma";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
		    case 19: fout.println("Fitting to a Two parameter Exponential Distribution");
	                fout.println("y = (yscale/sigma)*exp(-(x-mu)/sigma)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="sigma";
		            if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
  	        case 20: fout.println("Fitting to a One parameter Exponential Distribution");
	                fout.println("y = (yscale/sigma)*exp(-x/sigma)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="sigma";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
  	        case 21: fout.println("Fitting to a Standard Exponential Distribution");
	                fout.println("y = yscale*exp(-x)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                if(this.scaleFlag)paraName[0]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 22: fout.println("Fitting to a Rayleigh Distribution");
	                fout.println("y = (yscale/sigma)*(x/sigma)*exp(-0.5*(x/sigma)^2)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="sigma";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 23: fout.println("Fitting to a Two Parameter Pareto Distribution");
	                fout.println("y = yscale*(alpha*beta^alpha)/(x^(alpha+1))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="alpha";
	                paraName[1]="beta";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
             case 24: fout.println("Fitting to a One Parameter Pareto Distribution");
	                fout.println("y = yscale*(alpha)/(x^(alpha+1))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="alpha";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
             case 25: fout.println("Fitting to a Sigmoidal Threshold Function");
	                fout.println("y = yscale/(1 + exp(-slopeTerm(x - theta)))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="slope term";
	                paraName[1]="theta";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
             case 26: fout.println("Fitting to a Rectangular Hyperbola");
	                fout.println("y = yscale.x/(theta + x)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="theta";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 27: fout.println("Fitting to a Scaled Heaviside Step Function");
	                fout.println("y = yscale.H(x - theta)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="theta";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 28: fout.println("Fitting to a Hill/Sips Sigmoid");
	                fout.println("y = yscale.x^n/(theta^n + x^n)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="theta";
	                paraName[1]="n";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 29: fout.println("Fitting to a Three Parameter Pareto Distribution");
	                fout.println("y = yscale*(alpha*beta^alpha)/((x-theta)^(alpha+1))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="alpha";
	                paraName[1]="beta";
	                paraName[2]="theta";
	                if(this.scaleFlag)paraName[3]="y scale";
                    this.nonLinearPrint(fout);
                    break;
	        case 30: fout.println("Fitting to a Logistic distribution");
	                fout.println("y = yscale*exp(-(x-mu)/beta)/(beta*(1 + exp(-(x-mu)/beta))^2");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="beta";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 31: fout.println("Fitting to a Beta distribution - [0, 1] interval");
	                fout.println("y = yscale*x^(alpha-1)*(1-x)^(beta-1)/B(alpha, beta)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="alpha";
	                paraName[1]="beta";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 32: fout.println("Fitting to a Beta distribution - [min, max] interval");
	                fout.println("y = yscale*(x-min)^(alpha-1)*(max-x)^(beta-1)/(B(alpha, beta)*(max-min)^(alpha+beta-1)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="alpha";
	                paraName[1]="beta";
	                paraName[2]="min";
	                paraName[3]="max";
	                if(this.scaleFlag)paraName[4]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 33: fout.println("Fitting to a Three Parameter Gamma distribution");
	                fout.println("y = yscale*((x-mu)/beta)^(gamma-1)*exp(-(x-mu)/beta)/(beta*Gamma(gamma))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="beta";
	                paraName[2]="gamma";
	                if(this.scaleFlag)paraName[3]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 34: fout.println("Fitting to a Standard Gamma distribution");
	                fout.println("y = yscale*x^(gamma-1)*exp(-x)/Gamma(gamma)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="gamma";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 35: fout.println("Fitting to an Erang distribution");
	                fout.println("y = yscale*lambda^k*x^(k-1)*exp(-x*lambda)/(k-1)!");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="lambda";
	                if(this.scaleFlag)paraName[1]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 36: fout.println("Fitting to a two parameter log-normal distribution");
	                fout.println("y = (yscale/(x.sigma.sqrt(2.sigma)).exp(0.5.square((log(x)-muu)/sigma))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mu";
	                paraName[1]="sigma";
	                if(this.scaleFlag)paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 37: fout.println("Fitting to a three parameter log-normal distribution");
	                fout.println("y = (yscale/((x-alpha).beta.sqrt(2.beta)).exp(0.5.square((log(x-alpha)/gamma)/beta))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="alpha";
	                paraName[1]="beta";
	                paraName[2]="gamma";
	                if(this.scaleFlag)paraName[3]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 38: fout.println("Fitting to a Normal (Gaussian) distribution with fixed parameters");
	                fout.println("y = (yscale/(sd.sqrt(2.pi)).exp(0.5.square((x-mean)/sd))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="mean";
	                paraName[1]="sd";
	                paraName[2]="y scale";
                    this.nonLinearPrint(fout);
                    break;
            case 39: fout.println("Fitting to a EC50 dose response curve");
	                fout.println("y = bottom + (top - bottom)/(1 + (x/EC50)^HillSlope)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="bottom";
	                paraName[1]="top";
	                paraName[2]="EC50";
	                paraName[3]="Hill Slope";
                    this.nonLinearPrint(fout);
                    break;
            case 40: fout.println("Fitting to a LogEC50 dose response curve");
	                fout.println("y = bottom + (top - bottom)/(1 + 10^((logEC50 - x).HillSlope))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="bottom";
	                paraName[1]="top";
	                paraName[2]="LogEC50";
	                paraName[3]="Hill Slope";
                    this.nonLinearPrint(fout);
                    break;
            case 41: fout.println("Fitting to a EC50 dose response curve - bottom constrained to be zero or positive");
	                fout.println("y = bottom + (top - bottom)/(1 + (x/EC50)^HillSlope)");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="bottom";
	                paraName[1]="top";
	                paraName[2]="EC50";
	                paraName[3]="Hill Slope";
                    this.nonLinearPrint(fout);
                    break;
            case 42: fout.println("Fitting to a LogEC50 dose response curve - bottom constrained to be zero or positive");
	                fout.println("y = bottom + (top - bottom)/(1 + 10^((logEC50 - x).HillSlope))");
	                fout.println("Nelder and Mead Simplex used to fit the data)");
	                paraName[0]="bottom";
	                paraName[1]="top";
	                paraName[2]="LogEC50";
	                paraName[3]="Hill Slope";
                    this.nonLinearPrint(fout);
                    break;
                    
      default: throw new IllegalArgumentException("Method number (this.lastMethod) not found");
    }  //* end switch

	fout.close();
	
  }  //* method print(String filename)

  
  public void displayStats(){  	
    
	//* Just header here   
	DisplayRegression disp = new DisplayRegression();
	disp.dateAndTimeln();
	disp.println(this.graphTitle);
	paraName = new String[this.N_Params];
	if (lastMethod==38) paraName = new String[3];
    if (weightOpt){
        disp.println("Weighted Least Squares Minimisation");
    } else {
    	disp.println("Unweighted Least Squares Minimisation");
    }
    switch(this.lastMethod){
	  case 0: disp.println("Linear Regression with intercept");
	          disp.println("yhat = c[0] + c[1]*x1 + c[2]*x2 +c[3]*x3 + . . .");
	          disp.println();
              for(int i=0;i<this.N_Params;i++) this.paraName[i]="c["+i+"]";
              this.linearDisplay(disp);  //* this is the stats
	          break;
	  case 1: disp.println("Polynomial (with degree = " + (N_Params-1) + ") Fitting Linear Regression");
	          disp.println("yhat = c[0] + c[1]*x + c[2]*x^2 + c[3]*x^3 + . . .");
	          disp.println();
	          for(int i=0;i<this.N_Params;i++) this.paraName[i]="c["+i+"]";
              this.linearDisplay(disp);  //* this is the stats
	          break;
    }
  }
  
    
  protected void linearPrint(FileOutput fout){

    Double temp = null;
	double[] coeff = new double[this.N_Params];
	double[] coeffSd = new double[this.N_Params];

	for(int i=0; i<this.N_Params; i++){
    	    temp = (Double) this.best.elementAt(i);
    	    coeff[i] = temp.doubleValue();
    	    temp = (Double) this.bestSd.elementAt(i);
    	    coeffSd[i] = temp.doubleValue();
	}

	    if(this.legendCheck){
            fout.println();
            fout.println("x1 = " + this.xLegend);
            fout.println("y  = " + this.yLegend);
 	    }

        fout.println();
        fout.printtab(" ", this.field);
        fout.printtab("Best", this.field);
        fout.printtab("Standard", this.field);
        fout.println("Coefficient of");

        fout.printtab(" ", this.field);
        fout.printtab("Estimate");
        fout.printtab("Deviation", this.field);
        fout.println("variation (%)");

        for(int i=0; i<this.N_Params; i++){
            fout.printtab(this.paraName[i], this.field);
            fout.printtab(MathFns.truncate(coeff[i],this.prec), this.field);
            fout.printtab(MathFns.truncate(coeffSd[i],this.prec), this.field);
            fout.println(MathFns.truncate(coeffSd[i]*100.0D/coeff[i],this.prec));
        }
        fout.println();

        int ii=0;
        if(this.lastMethod<2)ii=1;
        for(int i=0; i<this.N_Xarrays; i++){
            fout.printtab("x"+String.valueOf(i+ii), this.field);
        }
        fout.printtab("y", this.field);
        fout.printtab("yHat", this.field);
        fout.printtab("weight", this.field);
        fout.printtab("residual", this.field);
        fout.println("residual");

        for(int i=0; i<this.N_Xarrays; i++){
            fout.printtab(" ", this.field);
        }
        fout.printtab(" ", this.field);
        fout.printtab(" ", this.field);
        fout.printtab(" ", this.field);
        fout.printtab("(unweighted)", this.field);
        fout.println("(weighted)");


        for(int i=0; i<this.N_YData; i++){
            for(int j=0; j<this.N_Xarrays; j++){
                fout.printtab(MathFns.truncate(this.xData[j][i],this.prec), this.field);
            }
            fout.printtab(MathFns.truncate(this.yData[i],this.prec), this.field);
            fout.printtab(MathFns.truncate(this.yHat[i],this.prec), this.field);
            fout.printtab(MathFns.truncate(this.weight[i],this.prec), this.field);
            fout.printtab(MathFns.truncate(this.residual[i],this.prec), this.field);
            fout.println(MathFns.truncate(this.residualW[i],this.prec));
         }
        fout.println();
        fout.println("Sum of squares " + MathFns.truncate(this.sumOfSquares, this.prec));
        
		if(this.trueFreq){
		    fout.printtab("Chi Square (Poissonian bins)");
		    fout.println(MathFns.truncate(this.chiSquare,this.prec));
            fout.printtab("Reduced Chi Square (Poissonian bins)");
		    fout.println(MathFns.truncate(this.reducedChiSquare,this.prec));
            fout.printtab("Chi Square (Poissonian bins) Probability");
		    fout.println(MathFns.truncate((1.0D-StatFns.chiSquareProb(this.chiSquare, this.N_YData-this.N_Xarrays)),this.prec));
		}
		else{
		    if(weightOpt){
	            fout.printtab("Chi Square");
		        fout.println(MathFns.truncate(this.chiSquare,this.prec));
                fout.printtab("Reduced Chi Square");
		        fout.println(MathFns.truncate(this.reducedChiSquare,this.prec));
                fout.printtab("Chi Square Probability");
		        fout.println(MathFns.truncate(this.getchiSquareProb(),this.prec));
		    }
		}
	    fout.println(" ");
	    fout.println("Correlation: x - y data");
	    if(this.N_Xarrays>1){
	        fout.printtab("Multiple Correlation Coefficient");
	        fout.println(MathFns.truncate(this.sampleR,this.prec));
	        if(this.sampleR2<=1.0D){
		        fout.printtab("Multiple Correlation Coefficient F-test ratio");
		        fout.println(MathFns.truncate(this.multipleF,this.prec));
		        fout.printtab("Multiple Correlation Coefficient F-test probability");
		        fout.println(MathFns.truncate(StatFns.fCompCDF(this.multipleF, this.N_Xarrays-1, this.N_YData-this.N_Xarrays),this.prec));
		    }
		}
	    else{
		    fout.printtab("Linear Correlation Coefficient");
	        fout.println(MathFns.truncate(this.sampleR,this.prec));
	        if(this.sampleR2<=1.0D){
		        fout.printtab("Linear Correlation Coefficient Probability");
		        fout.println(MathFns.truncate(StatFns.corrCoeffProb(this.sampleR, this.N_YData-this.N_Params),this.prec));
            }
        }

    	fout.println(" ");
	    fout.println("Correlation: y - yhat");
        fout.printtab("Linear Correlation Coefficient");
        double ccyy = StatFns.corrCoeff(this.yData, this.yHat);

	    fout.println(MathFns.truncate(ccyy, this.prec));
		fout.printtab("Linear Correlation Coefficient Probability");
		fout.println(MathFns.truncate(StatFns.corrCoeffProb(ccyy, this.N_YData-1),this.prec));


        fout.println(" ");
        fout.printtab("Degrees of freedom:");
		fout.println(this.N_YData - this.N_Params);
        fout.printtab("Number of data points:");
		fout.println(this.N_YData);
        fout.printtab("Number of estimated paramaters:");
		fout.println(this.N_Params);

        fout.println();
        if(this.chiSquare!=0.0D){
            fout.println("Correlation coefficients");
            fout.printtab(" ", this.field);
            for(int i=0; i<this.N_Params;i++){
                fout.printtab(paraName[i], this.field);
            }
            fout.println();

        for (int j=0; j<this.N_Params;j++){
            fout.printtab(paraName[j], this.field);
            for (int i=0; i<this.N_Params;i++) {
                fout.printtab(MathFns.truncate(this.corrCoeff[i][j], this.prec), this.field);
            }
            fout.println();
        }
    }

    fout.println();
    fout.println("End of file");

    fout.close();
  }

 
  protected void linearDisplay(DisplayRegression disp){

    Double temp = null;
	double[] coeff = new double[this.N_Params];
	double[] coeffSd = new double[this.N_Params];

	for (int i=0; i<this.N_Params; i++){
	    temp = (Double) this.best.elementAt(i);
	    coeff[i] = temp.doubleValue();
	    temp = (Double) this.bestSd.elementAt(i);
	    coeffSd[i] = temp.doubleValue();
    }

	if (this.legendCheck){
	    disp.print("\n");
	    disp.print("x1 = " + this.xLegend);
	    disp.print("y  = " + this.yLegend);
	}

    //* Observations	
	int ii=0;
	if (this.lastMethod<2) ii=1;
	for (int i=0; i<this.N_Xarrays; i++){
		disp.printtab("x"+String.valueOf(i+ii), this.field);
	}
	disp.printtab("y", this.field);
	disp.printtab("yHat", this.field);
	disp.printtab("weight", this.field);
	disp.printtab("residual", this.field);
	disp.println("residual");

	for (int i=0; i<this.N_Xarrays; i++){
		disp.printtab(" ", this.field);
	 }
	disp.printtab(" ", this.field);
	disp.printtab(" ", this.field);
	disp.printtab(" ", this.field);
	disp.printtab("(unweighted)", this.field);
	disp.println("(weighted)");

	for (int i=0; i<this.N_YData; i++){
	     for(int j=0; j<this.N_Xarrays; j++){
	    	 disp.printtab(MathFns.truncate(this.xData[j][i],this.prec), this.field);
	      }
	     disp.printtab(MathFns.truncate(this.yData[i],this.prec), this.field);
	     disp.printtab(MathFns.truncate(this.yHat[i],this.prec), this.field);
	     disp.printtab(MathFns.truncate(this.weight[i],this.prec), this.field);
	     disp.printtab(MathFns.truncate(this.residual[i],this.prec), this.field);
	     disp.println(MathFns.truncate(this.residualW[i],this.prec));
    }
	disp.println();
	
	//* Coefficients:
	disp.printtab(" ", this.field);
	disp.printtab("Best", this.field);
	disp.printtab("Standard", this.field);
	disp.println("Coefficient of");

	disp.printtab("Coefficients", this.field);
	disp.printtab("Estimate");
	disp.printtab("Deviation", this.field);
	disp.println("Variation (%)");
	disp.println("--------------------------------------------------------------");
	
	for (int i=0; i<this.N_Params; i++){
		disp.printtab(this.paraName[i], this.field);
		disp.printtab(MathFns.truncate(coeff[i],this.prec), this.field);
		disp.printtab(MathFns.truncate(coeffSd[i],this.prec), this.field);
		disp.println(MathFns.truncate(coeffSd[i]*100.0D/coeff[i],this.prec));
	 }
	disp.println();
	        
	if (this.trueFreq){
		disp.printtab("Chi Square (Poissonian bins)");
		disp.println(MathFns.truncate(this.chiSquare,this.prec));
	    disp.printtab("Reduced Chi Square (Poissonian bins)");
	    disp.println(MathFns.truncate(this.reducedChiSquare,this.prec));
	    disp.printtab("Chi Square (Poissonian bins) Probability");
	    disp.println(MathFns.truncate((1.0D-StatFns.chiSquareProb(this.chiSquare, this.N_YData-this.N_Xarrays)),this.prec));
	    disp.println();
	} else {
	    if (weightOpt){
	    	disp.printtab("Chi Square");
	    	disp.println(MathFns.truncate(this.chiSquare,this.prec));
	    	disp.printtab("Reduced Chi Square");
	    	disp.println(MathFns.truncate(this.reducedChiSquare,this.prec));
	    	disp.printtab("Chi Square Probability");
	    	disp.println(MathFns.truncate(this.getchiSquareProb(),this.prec));
	    	disp.println();
	    }
	}
    if (this.N_Xarrays>1){    //* Multivariate
        disp.println("Anova Analysis using F-Test for Multivariate Regression");
        disp.println("-------------------------------------------------------");
        disp.println("Sum of squares: " + MathFns.truncate(this.sumOfSquares, this.prec));    	
    	disp.printtab("R:");  // * ie Multiple Correlation Coefficient
    	disp.println(MathFns.truncate(this.sampleR,this.prec));
		disp.printtab("R^2:");
		disp.println(MathFns.truncate(this.sampleR2,this.prec));    	
		disp.printtab("R-Adjusted:");
		double r2Adj = 1 - (1-this.sampleR2)*(((double)this.N_YData-1.0)/((double)this.N_YData-this.N_Params));
        disp.println(MathFns.truncate(r2Adj,this.prec));
		if (this.sampleR2<=1.0D){
			disp.printtab("F-statistic:");  //* ie Multiple Correlation Coefficient F-test ratio
			/*
		     * self.F = (self.R2/self.df_r) / ((1-self.R2)/self.df_e)  # model F-statistic
		     */ 
		    double fdF = (this.sampleR2/this.N_Xarrays) / ((1-this.sampleR2)/(this.N_YData-this.N_Xarrays-1));
		    disp.println(MathFns.truncate(fdF, this.prec));
			//disp.println(MathFns.truncate(this.multipleF,this.prec));
		    
			disp.printtab("P-Value:"); //* ie Multiple Correlation Coefficient F-test probability
			double fdPval = StatFns.fCompCDF(fdF, this.N_Xarrays, this.N_YData-this.N_Xarrays-1);
			disp.println(MathFns.truncate(fdPval,this.prec));
	        /* P-Value - 2-tailed student F-distribution
	         * H0: sigma1 = sigma2
	         * F0 = s1^2/s2^2
	         * F0 (for H0) = b1/s_b1
	         */ 
		}	

/*
disp.printtab("10.22:");
double fdPvalue_2Side_Ftest = StatFns.fTestProb(10.22, 2, 7);
disp.println(MathFns.truncate(fdPvalue_2Side_Ftest, this.prec));
*/

	} else {    //* Univariate
	    disp.println("T-Test for Univariate Regression");
	    disp.println("--------------------------------");
	    disp.println("Sum of squares: " + MathFns.truncate(this.sumOfSquares, this.prec));
		disp.printtab("Correlation Coefficient:");
		disp.println(MathFns.truncate(this.sampleR,this.prec));
		disp.printtab("R^2 (Coef of Determination):");
		disp.println(MathFns.truncate(this.sampleR2,this.prec));
		disp.printtab("R-Adjusted:");
		/*
		 * self.R2adj = 1-(1-self.R2)*((self.nobs-1)/(self.nobs-self.ncoef))   # adjusted R-square
		 */
		double r2Adj = 1 - (1-this.sampleR2)*(((double)this.N_YData-1.0)/((double)this.N_YData-this.N_Params));
        disp.println(MathFns.truncate(r2Adj,this.prec));
        /*
        if (this.sampleR2<=1.0D){
			disp.printtab("Correlation Coefficient Probability:");
			disp.println(MathFns.truncate(StatFns.corrCoeffProb(this.sampleR, this.N_YData-this.N_Params),this.prec));
	    }
	    */
	    /* P-Value - 2-tailed student T-distribution
	     * T0 (for H0) = b1/s_b1
	     * 
	     * self.F = (self.R2/self.df_r) / ((1-self.R2)/self.df_e)  # model F-statistic
	     */ 
	    disp.printtab("F-statistic:");
	    double fdF = (this.sampleR2/1) / ((1-this.sampleR2)/(this.N_YData-2));
	    disp.println(MathFns.truncate(fdF, this.prec));
	    
	    double fdT0 = coeff[1] / coeffSd[1]; 
	    double fdPvalue = 2*(1 - StatFns.studentTcdf(Math.abs(fdT0), this.N_YData-this.N_Params-1));	    
	    disp.printtab("T-statistic:");
	    disp.println(MathFns.truncate(fdT0, this.prec));
	    disp.printtab("P-Value:");
	    disp.println(MathFns.truncate(fdPvalue, this.prec));
	    	    
	    /*
		disp.printtab("F-statistic:");  //* ie Multiple Correlation Coefficient F-test ratio
		disp.println(MathFns.truncate(this.multipleF,this.prec));
		disp.printtab("P-Value (F-statistic):"); //* ie Multiple Correlation Coefficient F-test probability
		fdPvalue = 1 - StatFns.fTestProb(this.multipleF, this.N_Xarrays-1, this.N_YData-this.N_Xarrays);
		disp.println(MathFns.truncate(fdPvalue,this.prec));
        */
        /* 
	    disp.printtab(" -2.5927e-02:");
	    double r = -.025927;
	    double Ttemp = r * Math.sqrt((3072-2) / (1 - r*r)); 
	    double ppTemp = 2*StatFns.studentTcdf(Ttemp, 3072-2);
	    disp.println(MathFns.truncate(ppTemp, this.prec));
	    */
	}

    //* y vs yhat
    disp.println(" ");
    disp.println("Correlation y, yhat");
	disp.printtab("Correlation Coefficient:");
	double ccyy = StatFns.corrCoeff(this.yData, this.yHat);

    disp.println(MathFns.truncate(ccyy, this.prec));
    disp.printtab("Correlation Coefficient Probability:");
    disp.println(MathFns.truncate(StatFns.corrCoeffProb(ccyy, this.N_YData-1),this.prec));

    disp.println(" ");
	disp.printtab("Degrees of freedom:");
	disp.println(this.N_YData - this.N_Params);
	disp.printtab("Number of data points:");
	disp.println((int) this.N_YData);
	disp.printtab("Number of estimated paramaters:");
	disp.println((int) this.N_Params);
	disp.println();
	
	//* Correlation coefficients
	if (2 == 1) {
	if (this.chiSquare!=0.0D){
		disp.println("Correlation coefficients of coeff's");
		disp.printtab(" ", this.field);
	    for (int i=0; i<this.N_Params; i++){
	    	 disp.printtab(paraName[i], this.field);
	    }
	    disp.println();
	    for (int j=0; j<this.N_Params;j++){
	        disp.printtab(paraName[j], this.field);
	        for (int i=0; i<this.N_Params;i++) {
	        	disp.printtab(MathFns.truncate(this.corrCoeff[i][j], this.prec), this.field);
	         }
	        disp.println();
	    }
	    disp.println();
    }
	}
	
	//* Correlation Coefficient Matrix & p-Value matrix
	rMatrix = StatFns.corrCoefMatrix(this.yxData);
    pMatrix = StatFns.pValueMatrix(rMatrix, this.yData.length);
	disp.println("Correlation coefficients of Y, X's");
	disp.printtab(" ", this.field);
    for (int i=0; i<this.N_Xarrays+1; i++){
        if (i==0) {	
    	    disp.printtab("Y", this.field);
        } else {    
    	    disp.printtab("X" + i, this.field);
        }    
    }
    disp.println();
    for (int i=0; i<this.N_Xarrays+1; i++){
        if (i==0) {	
    	    disp.printtab("Y", this.field);
        } else {    
    	    disp.printtab("X" + i, this.field);
        }   
        for (int j=0; j<this.N_Params;j++) {
        	disp.printtab(MathFns.truncate(rMatrix[i][j], this.prec), this.field);
        }
        disp.println();
    }

    disp.println();
	disp.println("p-Values of correlation coefficients of Y, X's");
	disp.printtab(" ", this.field);
    for (int i=0; i<this.N_Xarrays+1; i++){
        if (i==0) {	
    	    disp.printtab("Y", this.field);
        } else {    
    	    disp.printtab("X" + i, this.field);
        }    
    }
    disp.println();
    for (int i=0; i<this.N_Xarrays+1; i++){
        if (i==0) {	
    	    disp.printtab("Y", this.field);
        } else {    
    	    disp.printtab("X" + i, this.field);
        }   
        for (int j=0; j<this.N_Params;j++) {
        	disp.printtab(MathFns.truncate(pMatrix[i][j], this.prec), this.field);
        }
        disp.println();
    }
    disp.println();
    
  }  //* end method displayRegression
  
  
	// protected method - print non-linear regression output
	protected void nonLinearPrint(FileOutput fout){

	    Double temp = null;
	    double[] coeff = new double[this.N_Params];
	    double[] coeffSd = new double[this.N_Params];

	    for(int i=0; i<this.N_Params; i++){
    	    temp = (Double) this.best.elementAt(i);
    	    coeff[i] = temp.doubleValue();
    	    temp = (Double) this.bestSd.elementAt(i);
    	    coeffSd[i] = temp.doubleValue();
	    }

        if(this.legendCheck){
            fout.println();
            fout.println("x1 = " + this.xLegend);
            fout.println("y  = " + this.yLegend);
 	    }

        fout.println();
        if(!this.nlrStatus){
            fout.println("Convergence criterion was not satisfied");
            fout.println("The following results are, or a derived from, the current estimates on exiting the regression method");
            fout.println();
        }

        fout.println("Estimated parameters");
        fout.println("The statistics are obtained assuming that the model behaves as a linear model about the minimum.");
        fout.println("The Hessian matrix is calculated as the numerically derived second derivatives of chi square with respect to all pairs of parameters.");
        if(this.zeroCheck)fout.println("The best estimate/s equal to zero were replaced by the step size in the numerical differentiation!!!");
        fout.println("Consequentlty treat the statistics with great caution");
        if(!this.posVarFlag){
            fout.println("Covariance matrix contains at least one negative diagonal element");
            fout.println(" - all variances are dubious");
            fout.println(" - may not be at a minimum");
        }
        if(!this.invertFlag){
            fout.println("Hessian matrix is singular");
            fout.println(" - variances cannot be calculated");
            fout.println(" - may not be at a minimum");
        }

        fout.println(" ");
        if(!this.scaleFlag){
            fout.println("The ordinate scaling factor [yscale, Ao] has been set equal to " + this.yScaleFactor);
            fout.println(" ");
        }
        if(lastMethod==35){
            fout.println("The integer rate parameter, k, was varied in unit steps to obtain a minimum sum of squares");
            fout.println("This value of k was " + this.kayValue);
            fout.println(" ");
        }

        fout.printtab(" ", this.field);
        fout.printtab("Best", this.field);
        if(this.invertFlag){
            fout.printtab("Estimate", this.field);
            fout.printtab("Coefficient", this.field);
        }
        fout.printtab("Pre-min", this.field);
        fout.printtab("Post-min", this.field);
        fout.printtab("Initial", this.field);
        fout.println("Fractional");

        fout.printtab(" ", this.field);
        fout.printtab("estimate", this.field);
        if(this.invertFlag){
            fout.printtab("of the sd", this.field);
            fout.printtab("of", this.field);
        }
        fout.printtab("gradient", this.field);
        fout.printtab("gradient", this.field);
        fout.printtab("estimate", this.field);
        fout.println("step");
        if(this.invertFlag){
            fout.printtab(" ", this.field);
            fout.printtab(" ", this.field);
            fout.printtab(" ", this.field);
            fout.println("variation (%)");
        }

        if(this.lastMethod==38){
            int nT = 3;
            int ii = 0;
            for(int i=0; i<nT; i++){
                fout.printtab(this.paraName[i], this.field);
                if(this.fixed[i]){
                    fout.printtab(this.values[i]);
                    fout.println(" fixed parameter");
                }
                else{
                    fout.printtab(MathFns.truncate(coeff[ii],this.prec), this.field);
                    if(invertFlag){
                        fout.printtab(MathFns.truncate(coeffSd[ii],this.prec), this.field);
                        fout.printtab(MathFns.truncate(coeffSd[ii]*100.0D/coeff[ii],this.prec), this.field);
                    }
                    fout.printtab(MathFns.truncate(this.grad[ii][0],this.prec), this.field);
                    fout.printtab(MathFns.truncate(this.grad[ii][1],this.prec), this.field);
                    fout.printtab(MathFns.truncate(this.startH[ii],this.prec), this.field);
                    fout.println(MathFns.truncate(this.step[ii],this.prec));
                    ii++;
                }
            }
        }
        else{
            for(int i=0; i<this.N_Params; i++){
                fout.printtab(this.paraName[i], this.field);
                fout.printtab(MathFns.truncate(coeff[i],this.prec), this.field);
                if(invertFlag){
                    fout.printtab(MathFns.truncate(coeffSd[i],this.prec), this.field);
                    fout.printtab(MathFns.truncate(coeffSd[i]*100.0D/coeff[i],this.prec), this.field);
                }
                fout.printtab(MathFns.truncate(this.grad[i][0],this.prec), this.field);
                fout.printtab(MathFns.truncate(this.grad[i][1],this.prec), this.field);
                fout.printtab(MathFns.truncate(this.startH[i],this.prec), this.field);
                fout.println(MathFns.truncate(this.step[i],this.prec));
            }
        }
        fout.println();

        ErrorProp ePeak = null;
        ErrorProp eYscale = null;
        if(this.scaleFlag){
            switch(this.lastMethod){
            case 4: ErrorProp eSigma = new ErrorProp(coeff[1], coeffSd[1]);
                    eYscale = new ErrorProp(coeff[2]/Math.sqrt(2.0D*Math.PI), coeffSd[2]/Math.sqrt(2.0D*Math.PI));
                    ePeak = eYscale.over(eSigma);
                    fout.printsp("Calculated estimate of the peak value = ");
                    fout.println(ErrorProp.truncate(ePeak, prec));
                    break;
            case 5: ErrorProp eGamma = new ErrorProp(coeff[1], coeffSd[1]);
                    eYscale = new ErrorProp(2.0D*coeff[2]/Math.PI, 2.0D*coeffSd[2]/Math.PI);
                    ePeak = eYscale.over(eGamma);
                    fout.printsp("Calculated estimate of the peak value = ");
                    fout.println(ErrorProp.truncate(ePeak, prec));
                    break;

            }
        }
        if(this.lastMethod==25){
            fout.printsp("Calculated estimate of the maximum gradient = ");
            if(this.scaleFlag){
                fout.println(MathFns.truncate(coeff[0]*coeff[2]/4.0D, prec));
            }
            else{
                fout.println(MathFns.truncate(coeff[0]*this.yScaleFactor/4.0D, prec));
            }

        }
        if(this.lastMethod==28){
            fout.printsp("Calculated estimate of the maximum gradient = ");
            if(this.scaleFlag){
                fout.println(MathFns.truncate(coeff[1]*coeff[2]/(4.0D*coeff[0]), prec));
            }
            else{
                fout.println(MathFns.truncate(coeff[1]*this.yScaleFactor/(4.0D*coeff[0]), prec));
            }
            fout.printsp("Calculated estimate of the Ka, i.e. theta raised to the power n = ");
            fout.println(MathFns.truncate(Math.pow(coeff[0], coeff[1]), prec));
        }
        fout.println();

        int kk=0;
        for(int j=0; j<N_Yarrays; j++){
            if(this.multipleY)fout.println("Y array " + j);

            for(int i=0; i<this.N_Xarrays; i++){
                fout.printtab("x"+String.valueOf(i), this.field);
            }

            fout.printtab("y", this.field);
            fout.printtab("yHat", this.field);
            fout.printtab("weight", this.field);
            fout.printtab("residual", this.field);
            fout.println("residual");

            for(int i=0; i<this.N_Xarrays; i++){
                fout.printtab(" ", this.field);
            }
            fout.printtab(" ", this.field);
            fout.printtab(" ", this.field);
            fout.printtab(" ", this.field);
            fout.printtab("(unweighted)", this.field);
            fout.println("(weighted)");
            for(int i=0; i<this.N_YData0; i++){
                for(int jj=0; jj<this.N_Xarrays; jj++){
                    fout.printtab(MathFns.truncate(this.xData[jj][kk],this.prec), this.field);
                }
                fout.printtab(MathFns.truncate(this.yData[kk],this.prec), this.field);
                fout.printtab(MathFns.truncate(this.yHat[kk],this.prec), this.field);
                fout.printtab(MathFns.truncate(this.weight[kk],this.prec), this.field);
                fout.printtab(MathFns.truncate(this.residual[kk],this.prec), this.field);
                fout.println(MathFns.truncate(this.residualW[kk],this.prec));
                kk++;
            }
            fout.println();
        }

	    fout.printtab("Sum of squares of the unweighted residuals");
		fout.println(MathFns.truncate(this.sumOfSquares,this.prec));
	    if(this.trueFreq){
		    fout.printtab("Chi Square (Poissonian bins)");
		    fout.println(MathFns.truncate(this.chiSquare,this.prec));
            fout.printtab("Reduced Chi Square (Poissonian bins)");
		    fout.println(MathFns.truncate(this.reducedChiSquare,this.prec));
            fout.printtab("Chi Square (Poissonian bins) Probability");
		    fout.println(MathFns.truncate(1.0D-StatFns.chiSquareProb(this.reducedChiSquare,this.degreesOfFreedom),this.prec));
		}
		else{
		    if(weightOpt){
	            fout.printtab("Chi Square");
		        fout.println(MathFns.truncate(this.chiSquare,this.prec));
                fout.printtab("Reduced Chi Square");
		        fout.println(MathFns.truncate(this.reducedChiSquare,this.prec));
                fout.printtab("Chi Square Probability");
		        fout.println(MathFns.truncate(this.getchiSquareProb(),this.prec));
		    }
		}

        fout.println(" ");
	    fout.println("Correlation: x - y data");
	    if(this.N_Xarrays>1){
	        fout.printtab("Multiple Correlation Coefficient");
	        fout.println(MathFns.truncate(this.sampleR, this.prec));
            if(this.sampleR2<=1.0D){
		        fout.printtab("Multiple Correlation Coefficient F-test ratio");
		        fout.println(MathFns.truncate(this.multipleF, this.prec));
		        fout.printtab("Multiple Correlation Coefficient F-test probability");
		        fout.println(StatFns.fCompCDF(this.multipleF, this.N_Xarrays-1, this.N_YData-this.N_Xarrays));
		    }
		}
		else{
		    fout.printtab("Linear Correlation Coefficient");
	        fout.println(MathFns.truncate(this.sampleR, this.prec));
            if(this.sampleR2<=1.0D){
		        fout.printtab("Linear Correlation Coefficient Probability");
		        fout.println(MathFns.truncate(StatFns.corrCoeffProb(this.sampleR, this.N_YData-this.N_Params),this.prec));
            }
        }

    	fout.println(" ");
	    fout.println("Correlation: y(experimental) - y(calculated)");
        fout.printtab("Linear Correlation Coefficient");
        double ccyy = StatFns.corrCoeff(this.yData, this.yHat);
	    fout.println(MathFns.truncate(ccyy, this.prec));
		fout.printtab("Linear Correlation Coefficient Probability");
		fout.println(MathFns.truncate(StatFns.corrCoeffProb(ccyy, this.N_YData-1),this.prec));

    	fout.println(" ");
        fout.printtab("Degrees of freedom");
		fout.println(this.degreesOfFreedom);
        fout.printtab("Number of data points");
		fout.println(this.N_YData);
        fout.printtab("Number of estimated paramaters");
		fout.println(this.N_Params);

        fout.println();

        if(this.posVarFlag && this.invertFlag && this.chiSquare!=0.0D){
            fout.println("Parameter - parameter correlation coefficients");
            fout.printtab(" ", this.field);
            for(int i=0; i<this.N_Params;i++){
                fout.printtab(paraName[i], this.field);
            }
            fout.println();

            for(int j=0; j<this.N_Params;j++){
                fout.printtab(paraName[j], this.field);
                for(int i=0; i<this.N_Params;i++){
                    fout.printtab(MathFns.truncate(this.corrCoeff[i][j], this.prec), this.field);
                }
                fout.println();
            }
            fout.println();
        }

        fout.println();
        fout.printtab("Number of iterations taken");
        fout.println(this.nIter);
        fout.printtab("Maximum number of iterations allowed");
        fout.println(this.nMax);
        fout.printtab("Number of restarts taken");
        fout.println(this.kRestart);
        fout.printtab("Maximum number of restarts allowed");
        fout.println(this.konvge);
        fout.printtab("Standard deviation of the simplex at the minimum");
        fout.println(MathFns.truncate(this.simplexSd, this.prec));
        fout.printtab("Convergence tolerance");
        fout.println(this.fTol);
        switch(minTest){
            case 0: fout.println("simplex sd < the tolerance times the mean of the absolute values of the y values");
                    break;
            case 1: fout.println("simplex sd < the tolerance");
                    break;
            case 2: fout.println("simplex sd < the tolerance times the square root(sum of squares/degrees of freedom");
                    break;
        }
        fout.println("Step used in numerical differentiation to obtain Hessian matrix");
        fout.println("d(parameter) = parameter*"+this.delta);

        fout.println();
        fout.println("End of file");
		fout.close();
	}

	// plot calculated y against experimental y
	// title provided
    public void plotYY(String title){
        this.graphTitle = title;
        int ncurves = 2;
        int npoints = this.N_YData0;
        double[][] data = PlotGraph.data(ncurves, npoints);

        int kk = 0;
        for(int jj=0; jj<this.N_Yarrays; jj++){

            // fill first curve with experimental versus best fit values
            for(int i=0; i<N_YData0; i++){
                data[0][i]=this.yData[kk];
                data[1][i]=this.yHat[kk];
                kk++;
            }

            // Create a title
            String title0 = this.setGandPtitle(this.graphTitle);
            if(this.multipleY)title0 = title0 + "y array " + jj;
            String title1 = "Calculated versus experimental y values";

            // Calculate best fit straight line between experimental and best fit values
            Regression yyRegr = new Regression(this.yData, this.yHat, this.weight);
            yyRegr.linear();
            double[] coef = yyRegr.getCoeff();
            data[2][0]=MathFns.minimum(this.yData);
            data[3][0]=coef[0]+coef[1]*data[2][0];
            data[2][1]=MathFns.maximum(this.yData);
            data[3][1]=coef[0]+coef[1]*data[2][1];

            PlotGraph pg = new PlotGraph(data);

            pg.setGraphTitle(title0);
            pg.setGraphTitle2(title1);
            pg.setXaxisLegend("Experimental y value");
            pg.setYaxisLegend("Calculated y value");
            int[] popt = {1, 0};
            pg.setPoint(popt);
            int[] lopt = {0, 3};
            pg.setLine(lopt);

            pg.plot();
        }
    }

    //Creates a title
    protected String setGandPtitle(String title){
        String title1 = "";
        switch(this.lastMethod){
	        case 0: title1 = "Linear regression (with intercept): "+title;
	                break;
	        case 1: title1 = "Linear(polynomial with degree = " + (N_Params-1) + ") regression: "+title;
	                break;
	        case 2: title1 = "General linear regression: "+title;
	                break;
	        case 3: title1 = "Non-linear (simplex) regression: "+title;
	                break;
	        case 4: title1 = "Fit to a Gaussian distribution: "+title;
	                break;
	        case 5: title1 = "Fit to a Lorentzian distribution: "+title;
	                break;
	        case 6:title1 = "Fit to a Poisson distribution: "+title;
	                break;
		    case 7: title1 = "Fit to a Two Parameter Minimum Order Statistic Gumbel distribution: "+title;
	                break;
            case 8: title1 = "Fit to a two Parameter Maximum Order Statistic Gumbel distribution: "+title;
	                break;
	        case 9: title1 = "Fit to a One Parameter Minimum Order Statistic Gumbel distribution: "+title;
	                break;
	        case 10: title1 = "Fit to a One Parameter Maximum Order Statistic Gumbel distribution: "+title;
	                break;
            case 11: title1 = "Fit to a Standard Minimum Order Statistic Gumbel distribution: "+title;
	                break;
            case 12: title1 = "Fit to a Standard Maximum Order Statistic Gumbel distribution: "+title;
	                break;
	        case 13:title1 = "Fit to a Three Parameter Frechet distribution: "+title;
	                break;
	        case 14:title1 = "Fit to a Two Parameter Frechet distribution: "+title;
	                break;
	        case 15:title1 = "Fit to a Standard Frechet distribution: "+title;
	                break;
	        case 16:title1 = "Fit to a Three Parameter Weibull distribution: "+title;
	                break;
	        case 17:title1 = "Fit to a Two Parameter Weibull distribution: "+title;
	                break;
	        case 18:title1 = "Fit to a Standard Weibull distribution: "+title;
	                break;
	        case 19:title1 = "Fit to a Two Parameter Exponential distribution: "+title;
	                break;
	        case 20:title1 = "Fit to a One Parameter Exponential distribution: "+title;
	                break;
	        case 21:title1 = "Fit to a Standard exponential distribution: "+title;
	                break;
	        case 22:title1 = "Fit to a Rayleigh distribution: "+title;
	                break;
	        case 23:title1 = "Fit to a Two Parameter Pareto distribution: "+title;
	                break;
	        case 24:title1 = "Fit to a One Parameter Pareto distribution: "+title;
	                break;
	        case 25:title1 = "Fit to a Sigmoid Threshold Function: "+title;
	                break;
	        case 26:title1 = "Fit to a Rectangular Hyperbola: "+title;
	                break;
	        case 27:title1 = "Fit to a Scaled Heaviside Step Function: "+title;
	                break;
	        case 28:title1 = "Fit to a Hill/Sips Sigmoid: "+title;
	                break;
	        case 29:title1 = "Fit to a Three Parameter Pareto distribution: "+title;
	                break;
	        case 30:title1 = "Fit to a Logistic distribution: "+title;
                    break;
            case 31:title1 = "Fit to a Beta distribution - interval [0, 1]: "+title;
                    break;
            case 32:title1 = "Fit to a Beta distribution - interval [min, max]: "+title;
                    break;
            case 33:title1 = "Fit to a Three Parameter Gamma distribution]: "+title;
                    break;
            case 34:title1 = "Fit to a Standard Gamma distribution]: "+title;
                    break;
            case 35:title1 = "Fit to an Erlang distribution]: "+title;
                    break;
            case 36:title1 = "Fit to an two parameter log-normal distribution]: "+title;
                    break;
            case 37:title1 = "Fit to an three parameter log-normal distribution]: "+title;
                    break;
            case 38: title1 = "Fit to a Gaussian distribution with fixed parameters: "+title;
	                break;
	        case 39: title1 = "Fit to a EC50 dose response curve: "+title;
	                break;
	        case 40: title1 = "Fit to a LogEC50 dose response curve: "+title;
	                break;
	        case 41: title1 = "Fit to a EC50 dose response curve - bottom constrained [>= 0]: "+title;
	                break;
	        case 42: title1 = "Fit to a LogEC50 dose response curve - bottom constrained [>= 0]: "+title;
	                break;
            default: title1 = " "+title;
	    }
	    return title1;
    }

	// plot calculated y against experimental y
	// no title provided
    public void plotYY(){
        plotYY(this.graphTitle);
    }

    // plot experimental x against experimental y and against calculated y
    // linear regression data
	// title provided
    protected int plotXY(String title){
        this.graphTitle = title;
        int flag=0;
        if(!this.linNonLin && this.N_Params>0){
            System.out.println("You attempted to use Regression.plotXY() for a non-linear regression without providing the function reference (pointer) in the plotXY argument list");
            System.out.println("No plot attempted");
            flag=-1;
            return flag;
        }
        flag = this.plotXYlinear(title);
        return flag;
    }

    // plot experimental x against experimental y and against calculated y
    // Linear regression data
	// no title provided
    public int plotXY(){
        int flag = plotXY(this.graphTitle);
        return flag;
    }

    // plot experimental x against experimental y and against calculated y
    // non-linear regression data
	// title provided
	// matching simplex
    protected int plotXY(RegressionFunction g, String title){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y array\nplotXY2 should have been called");
        this.graphTitle = title;
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int flag = this.plotXYnonlinear(vec, title);
        return flag;
    }

    // plot experimental x against experimental y and against calculated y
    // non-linear regression data
	// title provided
	// matching simplex2
    protected int plotXY2(RegressionFunction2 g, String title){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nsimplex should have been called");
        this.graphTitle = title;
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int flag = this.plotXYnonlinear(vec, title);
        return flag;
    }

    // plot experimental x against experimental y and against calculated y
    // non-linear regression data
	// no title provided
	// matches simplex
    protected int plotXY(RegressionFunction g){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y array\nplotXY2 should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int flag = this.plotXYnonlinear(vec, this.graphTitle);
        return flag;
    }

    // plot experimental x against experimental y and against calculated y
    // non-linear regression data
	// no title provided
	// matches simplex2
    protected int plotXY2(RegressionFunction2 g){
        if(!this.multipleY)throw new IllegalArgumentException("This method cannot handle singly dimensioned y array\nplotXY should have been called");
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(g);
        int flag = this.plotXYnonlinear(vec, this.graphTitle);
        return flag;
    }

    // Add legends option
    public void addLegends(){
        int ans = JOptionPane.showConfirmDialog(null, "Do you wish to add your own legends to the x and y axes", "Axis Legends", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(ans==0){
            this.xLegend = JOptionPane.showInputDialog("Type the legend for the abscissae (x-axis) [first data set]" );
            this.yLegend = JOptionPane.showInputDialog("Type the legend for the ordinates (y-axis) [second data set]" );
            this.legendCheck = true;
        }
    }

  //* protected method for plotting experimental x against experimental y and against calculated y
  //* Linear regression
  //* title provided
  protected int plotXYlinear(String title){
    this.graphTitle = title;
    int flag=0;  //Returned as 0 if plot data can be plotted, -1 if not, -2 if tried multiple regression plot
        if(this.N_Xarrays>1){
            System.out.println("You attempted to use Regression.plotXY() for a multiple regression");
            System.out.println("No plot attempted");
            flag=-2;
            return flag;
        }

	    double[] coeff = new double[this.N_Params];
	    Double temp = null;

        for(int i=0; i<this.N_Params; i++){
    	    temp = (Double) this.best.elementAt(i);
    	    coeff[i] = temp.doubleValue();
        }

        int ncurves = 2;
        int npoints = 200;
        if(npoints<this.N_YData0)npoints=this.N_YData0;
        if(this.lastMethod==11 || this.lastMethod==12 || this.lastMethod==21)npoints=this.N_YData0;
        double[][] data = PlotGraph.data(ncurves, npoints);
        double xmin =MathFns.minimum(xData[0]);
        double xmax =MathFns.maximum(xData[0]);
        double inc = (xmax - xmin)/(double)(npoints - 1);
        String title1 = " ";
        String title2 = " ";

        for(int i=0; i<N_YData0; i++){
            data[0][i] = this.xData[0][i];
            data[1][i] = this.yData[i];
        }

        data[2][0]=xmin;
        for(int i=1; i<npoints; i++)data[2][i] = data[2][i-1] + inc;
        if(this.N_Params==0){
            switch(this.lastMethod){
	        case 11: title1 = "No regression: Minimum Order Statistic Standard Gumbel (y = exp(x)exp(-exp(x))): "+this.graphTitle;
                    title2 = " points - experimental values;   line - theoretical curve;   no parameters to be estimated";
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";
	                for(int i=0; i<npoints; i++)data[3][i] = this.yHat[i];
	                break;
	        case 12: title1 = "No regression:  Maximum Order Statistic Standard Gumbel (y = exp(-x)exp(-exp(-x))): "+this.graphTitle;
                    title2 = " points - experimental values;   line - theoretical curve;   no parameters to be estimated";
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";
	                for(int i=0; i<npoints; i++)data[3][i] = this.yHat[i];
	                break;
	        case 21: title1 = "No regression:  Standard Exponential (y = exp(-x)): "+this.graphTitle;
                    title2 = " points - experimental values;   line - theoretical curve;   no parameters to be estimated";
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";
	                for(int i=0; i<npoints; i++)data[3][i] = this.yHat[i];
	                break;
	        }

        }
        else{
	        switch(this.lastMethod){
	        case 0: title1 = "Linear Regression (Y = a + b*X) "+ this.graphTitle;
                    title2 = "Independent Vs Response Variable;  Line - best fit curve";
	                if(weightOpt) title2 = title2 + ";   error bars - weighting factors";
	                for(int i=0; i<npoints; i++)data[3][i] = coeff[0] + coeff[1]*data[2][i];
	                break;
	        case 1: title1 = "Linear (polynomial with degree = " + (N_Params-1) + ") regression: "+this.graphTitle;
	                title2 = " points - experimental values;   line - best fit curve";
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";
	                for(int i=0; i<npoints; i++){
                        double sum=coeff[0];
                        for(int j=1; j<this.N_Params; j++)sum+=coeff[j]*Math.pow(data[2][i],j);
                        data[3][i] = sum;
                    }
	                break;
	        case 2: title1 = "Linear regression  (y = a.x): "+this.graphTitle;
                    title2 = " points - experimental values;   line - best fit curve";
	                if(this.N_Xarrays==1){
	                    if(weightOpt)title2 = title2 +";   error bars - weighting factors";
	                    for(int i=0; i<npoints; i++)data[3][i] = coeff[0]*data[2][i];
	                }
	                else{
	                    System.out.println("Regression.plotXY(linear): lastMethod, "+lastMethod+",cannot be plotted in two dimensions");
	                    System.out.println("No plot attempted");
	                    flag=-1;
	                }
	                break;
	        case 11: title1 = "Linear regression: Minimum Order Statistic Standard Gumbel (y = a.z where z = exp(x)exp(-exp(x))): "+this.graphTitle;
                    title2 = " points - experimental values;   line - best fit curve";
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";
	                for(int i=0; i<npoints; i++)data[3][i] = coeff[0]*Math.exp(data[2][i])*Math.exp(-Math.exp(data[2][i]));
	                break;
	        case 12: title1 = "Linear regression:  Maximum Order Statistic Standard Gumbel (y = a.z where z=exp(-x)exp(-exp(-x))): "+this.graphTitle;
                    title2 = " points - experimental values;   line - best fit curve";
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";
	                for(int i=0; i<npoints; i++)data[3][i] = coeff[0]*Math.exp(-data[2][i])*Math.exp(-Math.exp(-data[2][i]));
	                break;
	        default: System.out.println("Regression.plotXY(linear): lastMethod, "+lastMethod+", either not recognised or cannot be plotted in two dimensions");
	                System.out.println("No plot attempted");
	                flag=-1;
	                return flag;
	        }
	    }

        PlotGraph pg = new PlotGraph(data);

        pg.setGraphTitle(title1);
        pg.setGraphTitle2(title2);
        pg.setXaxisLegend(this.xLegend);
        pg.setYaxisLegend(this.yLegend);
        int[] popt = {1,0};
        pg.setPoint(popt);
        int[] lopt = {0,3};
        pg.setLine(lopt);
        if(weightOpt)pg.setErrorBars(0,this.weight);
    pg.plot();

    return flag;
  }

    // protected method for plotting experimental x against experimental y and against calculated y
	// Non-linear regression
	// title provided
    public int plotXYnonlinear(Vector vec, String title){
        this.graphTitle = title;
        RegressionFunction g1 = null;
	    RegressionFunction2 g2 = null;
	    if(this.multipleY){
            g2 = (RegressionFunction2)vec.elementAt(0);
        }
        else{
            g1 = (RegressionFunction)vec.elementAt(0);
        }

        int flag=0;  //Returned as 0 if plot data can be plotted, -1 if not
	    double[] coeff = new double[this.N_Params];
	    Double temp = null;

        if(this.lastMethod<3){
	        System.out.println("Regression.plotXY(non-linear): lastMethod, "+lastMethod+", either not recognised or cannot be plotted in two dimensions");
	        System.out.println("No plot attempted");
	        flag=-1;
	        return flag;
	    }


	    for(int i=0; i<this.N_Params; i++){
    	    temp = (Double) this.best.elementAt(i);
    	    coeff[i] = temp.doubleValue();
        }

	    if(this.N_Xarrays>1){
	        System.out.println("Multiple Linear Regression with more than one independent variable cannot be plotted in two dimensions");
            System.out.println("plotYY() called instead of plotXY()");
            this.plotYY(title);
            flag=-2;
        }
	    else{
	        if(this.multipleY){
	            int ncurves = 2;
                int npoints = 200;
                if(npoints<this.N_YData0)npoints=this.N_YData0;
                String title1, title2;
                int kk=0;
                double[] wWeight = new double[this.N_YData0];
                for(int jj=0; jj<this.N_Yarrays; jj++){
                    double[][] data = PlotGraph.data(ncurves, npoints);
                    for(int i=0; i<this.N_YData0; i++){
                        data[0][i] = this.xData[0][kk];
                        data[1][i] = this.yData[kk];
                        wWeight[i] = this.weight[kk];
                        kk++;
                    }
                    double xmin =MathFns.minimum(xData[0]);
                    double xmax =MathFns.maximum(xData[0]);
                    double inc = (xmax - xmin)/(double)(npoints - 1);
                    data[2][0]=xmin;
                    for(int i=1; i<npoints; i++)data[2][i] = data[2][i-1] + inc;
                    double[] xd = new double[this.N_Xarrays];
                    for(int i=0; i<npoints; i++){
                        xd[0] = data[2][i];
                        data[3][i] = g2.function(coeff, xd, jj*this.N_YData0);
                    }

                    // Create a title
 	                title1 = this.setGandPtitle(title);
    	            title2 = " points - experimental values;   line - best fit curve;  y data array " + jj;
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";

                    PlotGraph pg = new PlotGraph(data);

                    pg.setGraphTitle(title1);
                    pg.setGraphTitle2(title2);
                    pg.setXaxisLegend(this.xLegend);
                    pg.setYaxisLegend(this.yLegend);
                    int[] popt = {1,0};
                    pg.setPoint(popt);
                    int[] lopt = {0,3};
                    pg.setLine(lopt);
                    if(weightOpt)pg.setErrorBars(0,wWeight);

                    pg.plot();
                }
	        }
	        else{
                int ncurves = 2;
                int npoints = 200;
                if(npoints<this.N_YData0)npoints=this.N_YData0;
                if(this.lastMethod==6)npoints=this.N_YData0;
                String title1, title2;
                double[][] data = PlotGraph.data(ncurves, npoints);
                for(int i=0; i<this.N_YData0; i++){
                    data[0][i] = this.xData[0][i];
                    data[1][i] = this.yData[i];
                }
                if(this.lastMethod==6){
                    double[] xd = new double[this.N_Xarrays];
                    for(int i=0; i<npoints; i++){
                        data[2][i]=data[0][i];
                        xd[0] = data[2][i];
                        data[3][i] = g1.function(coeff, xd);
                    }
                }
                else{
                    double xmin =MathFns.minimum(xData[0]);
                    double xmax =MathFns.maximum(xData[0]);
                    double inc = (xmax - xmin)/(double)(npoints - 1);
                    data[2][0]=xmin;
                    for(int i=1; i<npoints; i++)data[2][i] = data[2][i-1] + inc;
                    double[] xd = new double[this.N_Xarrays];
                    for(int i=0; i<npoints; i++){
                        xd[0] = data[2][i];
                        data[3][i] = g1.function(coeff, xd);
                    }
                }

                // Create a title
 	            title1 = this.setGandPtitle(title);
    	        title2 = " points - experimental values;   line - best fit curve";
	            if(weightOpt)title2 = title2 +";   error bars - weighting factors";

                PlotGraph pg = new PlotGraph(data);

                pg.setGraphTitle(title1);
                pg.setGraphTitle2(title2);
                pg.setXaxisLegend(this.xLegend);
                pg.setYaxisLegend(this.yLegend);
                int[] popt = {1,0};
                pg.setPoint(popt);
                int[] lopt = {0,3};
                pg.setLine(lopt);

                if(weightOpt)pg.setErrorBars(0,this.weight);

                pg.plot();
	        }
	    }
        return flag;
	}

    // protected method for plotting experimental x against experimental y and against calculated y
	// Non-linear regression
	// all parameters fixed
    public int plotXYfixed(Vector vec, String title){
        this.graphTitle = title;
        RegressionFunction g1 = null;
	    RegressionFunction2 g2 = null;
	    if(this.multipleY){
            g2 = (RegressionFunction2)vec.elementAt(0);
        }
        else{
            g1 = (RegressionFunction)vec.elementAt(0);
        }

        int flag=0;  //Returned as 0 if plot data can be plotted, -1 if not

        if(this.lastMethod<3){
	        System.out.println("Regression.plotXY(non-linear): lastMethod, "+lastMethod+", either not recognised or cannot be plotted in two dimensions");
	        System.out.println("No plot attempted");
	        flag=-1;
	        return flag;
	    }


	    if(this.N_Xarrays>1){
	        System.out.println("Multiple Linear Regression with more than one independent variable cannot be plotted in two dimensions");
            System.out.println("plotYY() called instead of plotXY()");
            this.plotYY(title);
            flag=-2;
        }
	    else{
	        if(this.multipleY){
	            int ncurves = 2;
                int npoints = 200;
                if(npoints<this.N_YData0)npoints=this.N_YData0;
                String title1, title2;
                int kk=0;
                double[] wWeight = new double[this.N_YData0];
                for(int jj=0; jj<this.N_Yarrays; jj++){
                    double[][] data = PlotGraph.data(ncurves, npoints);
                    for(int i=0; i<this.N_YData0; i++){
                        data[0][i] = this.xData[0][kk];
                        data[1][i] = this.yData[kk];
                        wWeight[i] = this.weight[kk];
                        kk++;
                    }
                    double xmin =MathFns.minimum(xData[0]);
                    double xmax =MathFns.maximum(xData[0]);
                    double inc = (xmax - xmin)/(double)(npoints - 1);
                    data[2][0]=xmin;
                    for(int i=1; i<npoints; i++)data[2][i] = data[2][i-1] + inc;
                    double[] xd = new double[this.N_Xarrays];
                    for(int i=0; i<npoints; i++){
                        xd[0] = data[2][i];
                        data[3][i] = g2.function(this.values, xd, jj*this.N_YData0);
                    }

                    // Create a title
 	                title1 = this.setGandPtitle(title);
    	            title2 = " points - experimental values;   line - best fit curve;  y data array " + jj;
	                if(weightOpt)title2 = title2 +";   error bars - weighting factors";

                    PlotGraph pg = new PlotGraph(data);

                    pg.setGraphTitle(title1);
                    pg.setGraphTitle2(title2);
                    pg.setXaxisLegend(this.xLegend);
                    pg.setYaxisLegend(this.yLegend);
                    int[] popt = {1,0};
                    pg.setPoint(popt);
                    int[] lopt = {0,3};
                    pg.setLine(lopt);
                    if(weightOpt)pg.setErrorBars(0,wWeight);

                    pg.plot();
                }
	        }
	        else{
                int ncurves = 2;
                int npoints = 200;
                if(npoints<this.N_YData0)npoints=this.N_YData0;
                if(this.lastMethod==6)npoints=this.N_YData0;
                String title1, title2;
                double[][] data = PlotGraph.data(ncurves, npoints);
                for(int i=0; i<this.N_YData0; i++){
                    data[0][i] = this.xData[0][i];
                    data[1][i] = this.yData[i];
                }
                if(this.lastMethod==6){
                    double[] xd = new double[this.N_Xarrays];
                    for(int i=0; i<npoints; i++){
                        data[2][i]=data[0][i];
                        xd[0] = data[2][i];
                        data[3][i] = g1.function(this.values, xd);
                    }
                }
                else{
                    double xmin =MathFns.minimum(xData[0]);
                    double xmax =MathFns.maximum(xData[0]);
                    double inc = (xmax - xmin)/(double)(npoints - 1);
                    data[2][0]=xmin;
                    for(int i=1; i<npoints; i++)data[2][i] = data[2][i-1] + inc;
                    double[] xd = new double[this.N_Xarrays];
                    for(int i=0; i<npoints; i++){
                        xd[0] = data[2][i];
                        data[3][i] = g1.function(this.values, xd);
                    }
                }

                // Create a title
 	            title1 = this.setGandPtitle(title);
    	        title2 = " points - experimental values;   line - best fit curve";
	            if(weightOpt)title2 = title2 +";   error bars - weighting factors";

                PlotGraph pg = new PlotGraph(data);

                pg.setGraphTitle(title1);
                pg.setGraphTitle2(title2);
                pg.setXaxisLegend(this.xLegend);
                pg.setYaxisLegend(this.yLegend);
                int[] popt = {1,0};
                pg.setPoint(popt);
                int[] lopt = {0,3};
                pg.setLine(lopt);

                if(weightOpt)pg.setErrorBars(0,this.weight);

                pg.plot();
	        }
	    }
        return flag;
	}


    // Get the non-linear regression status
    // true if convergence was achieved
    // false if convergence not achieved before maximum number of iterations
    //  current values then returned
    public boolean getNlrStatus(){
        return this.nlrStatus;
    }

    // Reset scaling factors (scaleOpt 0 and 1, see below for scaleOpt 2)
    public void setScale(int n){
        if(n<0 || n>1)throw new IllegalArgumentException("The argument must be 0 (no scaling) 1(initial estimates all scaled to unity) or the array of scaling factors");
        this.scaleOpt=n;
    }

    // Reset scaling factors (scaleOpt 2, see above for scaleOpt 0 and 1)
    public void setScale(double[] sc){
        this.scale=sc;
        this.scaleOpt=2;
    }

    // Get scaling factors
    public double[] getScale(){
        return this.scale;
    }

	// Reset the non-linear regression convergence test option
	public void setMinTest(int n){
	    if(n<0 || n>1)throw new IllegalArgumentException("minTest must be 0 or 1");
	    this.minTest=n;
	}

    // Get the non-linear regression convergence test option
	public int getMinTest(){
	    return this.minTest;
	}

	// Get the simplex sd at the minimum
	public double getSimplexSd(){
	    return this.simplexSd;
	}

	// Get the best estimates of the unknown parameters
	public double[] getCoeff(){
	    double[] coeff = new double[this.N_Params];
	    Double temp = null;
	    for(int i=0; i<this.N_Params; i++){
    	    temp = (Double)this.best.elementAt(i);
    	    coeff[i] = temp.doubleValue();
 	    }
	    return coeff;
	}

	// Get the estimates of the standard deviations of the best estimates of the unknown parameters
	public double[] getCoeffSd(){
	    double[] coeffSd = new double[this.N_Params];
	    Double temp = null;
	    for(int i=0; i<this.N_Params; i++){
    	    temp = (Double) this.bestSd.elementAt(i);
    	    coeffSd[i] = temp.doubleValue();
 	    }
	    return coeffSd;
	}

	// Get the cofficients of variations of the best estimates of the unknown parameters
	public double[] getCoeffVar(){
	    double[] coeffVar = new double[this.N_Params];
	    double coeff = 0.0D;
	    double coeffSd = 0.0D;

	    Double temp = null;
	    for(int i=0; i<this.N_Params; i++){
    	    temp = (Double) this.best.elementAt(i);
    	    coeff = temp.doubleValue();
    	    temp = (Double) this.bestSd.elementAt(i);
    	    coeffSd = temp.doubleValue();
    	    coeffVar[i]=coeffSd*100.0D/coeff;
 	    }
	    return coeffVar;
	}

	// Get the pseudo-estimates of the standard deviations of the best estimates of the unknown parameters
	public double[] getPseudoSd(){

	    return (double[])pseudoSd.clone();
	}


	// Get the inputted x values
	public double[][] getXdata(){
	    return (double[][])xData.clone();
	}

    // Get the inputted y values
	public double[] getYdata(){
	    return (double[])yData.clone();
	}

	// Get the calculated y values
	public double[] getyHat(){
	    double[] temp = new double[this.N_YData];
	    for(int i=0; i<this.N_YData; i++)temp[i]=this.yHat[i];
	    return temp;
	}

	// Get the unweighted residuals, y(experimental) - y(calculated)
	public double[] getResiduals(){
	    double[] temp = new double[this.N_YData];
	    for(int i=0; i<this.N_YData; i++)temp[i]=this.yData[i]-this.yHat[i];
	    return temp;
	}

	// Get the weighted residuals, (y(experimental) - y(calculated))/weight
	public double[] getWeightedResiduals(){

	    double[] temp = new double[this.N_YData];
	    for(int i=0; i<this.N_YData; i++)temp[i]=(this.yData[i]-this.yHat[i])/weight[i];
	    return temp;
	}

	// Get the unweighted sum of squares of the residuals
	public double getSumOfSquares(){
	    return this.sumOfSquares;
	}

	// Get the chi square estimate
	public double getChiSquare(){
	    double ret=0.0D;
	    if(weightOpt){
		    ret = this.chiSquare;
	    }
	    else{
	        System.out.println("Chi Square cannot be calculated as data are neither true frequencies nor weighted");
	        System.out.println("A value of -1 is returned as Chi Square");
	        ret = -1.0D;
	    }
	    return ret;
	}

	// Get the reduced chi square estimate
	public double getReducedChiSquare(){
	    double ret=0.0D;
	    if(weightOpt){
	        ret = this.reducedChiSquare;
		}
	    else{
	        System.out.println("A Reduced Chi Square cannot be calculated as data are neither true frequencies nor weighted");
	        System.out.println("A value of -1 is returned as Reduced Chi Square");
	        ret = -1.0D;
	    }
	    return ret;
	}

	// Get the chi square probablity
	public double getchiSquareProb(){
	    double ret=0.0D;
	    if(weightOpt){
	        ret = 1.0D-StatFns.chiSquareProb(this.chiSquare, this.N_YData-this.N_Xarrays);
	    }
		else{
	        System.out.println("A Chi Square probablity cannot be calculated as data are neither true frequencies nor weighted");
	        System.out.println("A value of -1 is returned as Reduced Chi Square");
	        ret = -1.0D;
	    }
	    return ret;
	}

	// Get the covariance matrix
	public double[][] getCovMatrix(){
	    return this.covar;
	}

	// Get the correlation coefficient matrix
	public double[][] getCorrCoeffMatrix(){
	    return this.corrCoeff;
	}

	// Get the number of iterations in nonlinear regression
	public int getNiter(){
	    return this.nIter;
	}


	// Set the maximum number of iterations allowed in nonlinear regression
	public void setNmax(int nmax){
	    this.nMax = nmax;
	}

	// Get the maximum number of iterations allowed in nonlinear regression
	public int getNmax(){
	    return this.nMax;
	}

	// Get the number of restarts in nonlinear regression
	public int getNrestarts(){
	    return this.kRestart;
	}

    // Set the maximum number of restarts allowed in nonlinear regression
	public void setNrestartsMax(int nrs){
	    this.konvge = nrs;
	}

	// Get the maximum number of restarts allowed in nonlinear regression
	public int getNrestartsMax(){
	    return this.konvge;
	}

	// Get the degrees of freedom
	public double getDegFree(){
	    return (this.degreesOfFreedom);
	}

	// Reset the Nelder and Mead reflection coefficient [alpha]
	public void setNMreflect(double refl){
	    this.rCoeff = refl;
	}

	// Get the Nelder and Mead reflection coefficient [alpha]
	public double getNMreflect(){
	    return this.rCoeff;
	}

    // Reset the Nelder and Mead extension coefficient [beta]
	public void setNMextend(double ext){
	    this.eCoeff = ext;
	}
	// Get the Nelder and Mead extension coefficient [beta]
	public double getNMextend(){
	    return this.eCoeff;
	}

	// Reset the Nelder and Mead contraction coefficient [gamma]
	public void setNMcontract(double con){
	    this.cCoeff = con;
	}

	// Get the Nelder and Mead contraction coefficient [gamma]
	public double getNMcontract(){
	    return cCoeff;
	}

	// Set the non-linear regression tolerance
	public void setTolerance(double tol){
	    this.fTol = tol;
	}


	// Get the non-linear regression tolerance
	public double getTolerance(){
	    return this.fTol;
	}

	// Get the non-linear regression pre and post minimum gradients
	public double[][] getGrad(){
	    return this.grad;
	}

	// Set the non-linear regression fractional step size used in numerical differencing
	public void setDelta(double delta){
	    this.delta = delta;
	}

	// Get the non-linear regression fractional step size used in numerical differencing
	public double getDelta(){
	    return this.delta;
	}

	// Get the non-linear regression statistics Hessian matrix inversion status flag
	public boolean getInversionCheck(){
	    return this.invertFlag;
	}

	// Get the non-linear regression statistics Hessian matrix inverse diagonal status flag
	public boolean getPosVarCheck(){
	    return this.posVarFlag;
	}

    // Test of an additional terms  {extra sum of squares]
    // return F-ratio, probability, order check and values provided in order used
    public static Vector<Object> testOfAdditionalTerms(double chiSquareR, int nParametersR, double chiSquareF, int nParametersF, int nPoints){
        int degFreedomR = nPoints - nParametersR;
        int degFreedomF = nPoints - nParametersF;

        // Check that model 2 has the lowest degrees of freedom
        boolean reversed = false;
        if(degFreedomR<degFreedomF){
            reversed = true;
            double holdD = chiSquareR;
            chiSquareR = chiSquareF;
            chiSquareF = holdD;
            int holdI = nParametersR;
            nParametersR = nParametersF;
            nParametersF = holdI;
            degFreedomR = nPoints - nParametersR;
            degFreedomF = nPoints - nParametersF;
            System.out.println("package flanagan.analysis; class Regression; method testAdditionalTerms");
            System.out.println("the order of the chi-squares has been reversed to give a second chi- square with the lowest degrees of freedom");
        }
        int degFreedomD = degFreedomR - degFreedomF;

        // F ratio
        double numer = (chiSquareR - chiSquareF)/degFreedomD;
        double denom = chiSquareF/degFreedomF;
        double fRatio = numer/denom;

        // Probability
        double fProb = 1.0D;
        if(chiSquareR>chiSquareF){
            fProb = StatFns.fCompCDF(fRatio, degFreedomD, degFreedomF);
        }

        // Return vector
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(new Double(fRatio));
        vec.addElement(new Double(fProb));
        vec.addElement(new Boolean(reversed));
        vec.addElement(new Double(chiSquareR));
        vec.addElement(new Integer(nParametersR));
        vec.addElement(new Double(chiSquareF));
        vec.addElement(new Integer(nParametersF));
        vec.addElement(new Integer(nPoints));

        return vec;
    }

    // Test of an additional terms  {extra sum of squares]
    // return F-ratio only
    public double testOfAdditionalTermsFratio(double chiSquareR, int nParametersR, double chiSquareF, int nParametersF, int nPoints){
        int degFreedomR = nPoints - nParametersR;
        int degFreedomF = nPoints - nParametersF;

        // Check that model 2 has the lowest degrees of freedom
        boolean reversed = false;
        if(degFreedomR<degFreedomF){
            reversed = true;
            double holdD = chiSquareR;
            chiSquareR = chiSquareF;
            chiSquareF = holdD;
            int holdI = nParametersR;
            nParametersR = nParametersF;
            nParametersF = holdI;
            degFreedomR = nPoints - nParametersR;
            degFreedomF = nPoints - nParametersF;
            System.out.println("package flanagan.analysis; class Regression; method testAdditionalTermsFratio");
            System.out.println("the order of the chi-squares has been reversed to give a second chi- square with the lowest degrees of freedom");
        }
        int degFreedomD = degFreedomR - degFreedomF;

        // F ratio
        double numer = (chiSquareR - chiSquareF)/degFreedomD;
        double denom = chiSquareF/degFreedomF;
        double fRatio = numer/denom;

        return fRatio;
    }

    // Test of an additional terms  {extra sum of squares]
    // return F-distribution probablity only
    public double testOfAdditionalTermsFprobabilty(double chiSquareR, int nParametersR, double chiSquareF, int nParametersF, int nPoints){
        int degFreedomR = nPoints - nParametersR;
        int degFreedomF = nPoints - nParametersF;

        // Check that model 2 has the lowest degrees of freedom
        boolean reversed = false;
        if(degFreedomR<degFreedomF){
            reversed = true;
            double holdD = chiSquareR;
            chiSquareR = chiSquareF;
            chiSquareF = holdD;
            int holdI = nParametersR;
            nParametersR = nParametersF;
            nParametersF = holdI;
            degFreedomR = nPoints - nParametersR;
            degFreedomF = nPoints - nParametersF;
            System.out.println("package flanagan.analysis; class Regression; method testAdditionalTermsFprobability");
            System.out.println("the order of the chi-squares has been reversed to give a second chi- square with the lowest degrees of freedom");
        }
        int degFreedomD = degFreedomR - degFreedomF;

        // F ratio
        double numer = (chiSquareR - chiSquareF)/degFreedomD;
        double denom = chiSquareF/degFreedomF;
        double fRatio = numer/denom;

        // Probability
        double fProb = 1.0D;
        if(chiSquareR>chiSquareF){
            fProb = StatFns.fCompCDF(fRatio, degFreedomD, degFreedomF);
        }

        return fProb;
    }



    // FIT TO SPECIAL FUNCTIONS
	// Fit to a Poisson distribution
	public void poisson(){
	    this.fitPoisson(0);
	}

	// Fit to a Poisson distribution
	public void poissonPlot(){
	    this.fitPoisson(1);
	}

	protected void fitPoisson(int plotFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=6;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=2;
	    if(!this.scaleFlag)this.N_Params=2;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // Check all abscissae are integers
	    for(int i=0; i<this.N_YData; i++){
	        if(xData[0][i]-Math.floor(xData[0][i])!=0.0D)throw new IllegalArgumentException("all abscissae must be, mathematically, integer values");
	    }

	    // Calculate  x value at peak y (estimate of the distribution mode)
	    Vector<Object> ret1 = Regression.dataSign(yData);
	 	Double tempd = null;
	 	Integer tempi = null;
	    tempi = (Integer)ret1.elementAt(5);
	 	int peaki = tempi.intValue();
	    double mean = xData[0][peaki];

	    // Calculate peak value
	    tempd = (Double)ret1.elementAt(4);
	    double peak = tempd.doubleValue();

	    // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        start[0] = mean;
        if(this.scaleFlag){
            start[1] = peak/(Math.exp(mean*Math.log(mean)-StatFns.logFactorial(mean))*Math.exp(-mean));
        }
        step[0] = 0.1D*start[0];
        if(step[0]==0.0D){
            Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	    Double tempdd = null;
	        tempdd = (Double)ret0.elementAt(2);
	 	    double xmax = tempdd.doubleValue();
	 	    if(xmax==0.0D){
	 	        tempdd = (Double)ret0.elementAt(0);
	 	        xmax = tempdd.doubleValue();
	 	    }
	        step[0]=xmax*0.1D;
	    }
        if(this.scaleFlag)step[1] = 0.1D*start[1];

	    // Nelder and Mead Simplex Regression
        PoissonFunction f = new PoissonFunction();
        this.addConstraint(1,-1,0.0D);
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();
            // Plot results
            this.plotOpt=false;
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }
	}


    // FIT TO A NORMAL (GAUSSIAN) DISTRIBUTION

	// Fit to a Gaussian
	public void gaussian(){
	    this.fitGaussian(0);
	}

	public void normal(){
	    this.fitGaussian(0);
	}

	// Fit to a Gaussian
	public void gaussianPlot(){
	    this.fitGaussian(1);
	}

    // Fit to a Gaussian
	public void normalPlot(){
	    this.fitGaussian(1);
	}

    // Fit data to a Gaussian (normal) probability function
	protected void fitGaussian(int plotFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=4;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=3;
	    if(!this.scaleFlag)this.N_Params=2;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitGaussian(): This implementation of the Gaussian distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // Calculate  x value at peak y (estimate of the Gaussian mean)
	    Vector<Object> ret1 = Regression.dataSign(yData);
	 	Integer tempi = null;
	    tempi = (Integer)ret1.elementAt(5);
	 	int peaki = tempi.intValue();
	    double mean = xData[0][peaki];

	    // Calculate an estimate of the sd
	    double sd = Math.sqrt(2.0D)*halfWidth(xData[0], yData);

	    // Calculate estimate of y scale
	    tempd = (Double)ret1.elementAt(4);
	    double ym = tempd.doubleValue();
	    ym=ym*sd*Math.sqrt(2.0D*Math.PI);

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        start[0] = mean;
        start[1] = sd;
        if(this.scaleFlag){
            start[2] = ym;
        }
        step[0] = 0.1D*sd;
        step[1] = 0.1D*start[1];
        if(step[1]==0.0D){
            Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	    Double tempdd = null;
	        tempdd = (Double)ret0.elementAt(2);
	 	    double xmax = tempdd.doubleValue();
	 	    if(xmax==0.0D){
	 	        tempdd = (Double)ret0.elementAt(0);
	 	        xmax = tempdd.doubleValue();
	 	    }
	        step[0]=xmax*0.1D;
	    }
        if(this.scaleFlag)step[2] = 0.1D*start[1];

	    // Nelder and Mead Simplex Regression
        GaussianFunction f = new GaussianFunction();
        this.addConstraint(1,-1, 0.0D);
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }

	}

	// Fit data to a Gaussian (normal) probability function
    // with option toi fix some of the parameters
    // parameter order - mean, sd, scale factor
	public void gaussian(double[] initialEstimates, boolean[] fixed){
	    this.fitGaussianFixed(initialEstimates, fixed, 0);
	}

	// Fit to a Gaussian
	// with option to fix some of the parameters
    // parameter order - mean, sd, scale factor
	public void normal(double[] initialEstimates, boolean[] fixed){
	    this.fitGaussianFixed(initialEstimates, fixed, 0);
	}

	// Fit to a Gaussian
	// with option to fix some of the parameters
    // parameter order - mean, sd, scale factor
	public void gaussianPlot(double[] initialEstimates, boolean[] fixed){
	    this.fitGaussianFixed(initialEstimates, fixed, 1);
	}

    // Fit to a Gaussian
    // with option to fix some of the parameters
    // parameter order - mean, sd, scale factor
	public void normalPlot(double[] initialEstimates, boolean[] fixed){
	    this.fitGaussianFixed(initialEstimates, fixed, 1);
	}


	// Fit data to a Gaussian (normal) probability function
    // with option to fix some of the parameters
    // parameter order - mean, sd, scale factor
	protected void fitGaussianFixed(double[] initialEstimates, boolean[] fixed, int plotFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=38;
	    this.values = initialEstimates;
	    this.fixed = fixed;
	    this.scaleFlag=true;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=3;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitGaussian(): This implementation of the Gaussian distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

        // Create instance of GaussianFunctionFixed
        GaussianFunctionFixed f = new GaussianFunctionFixed();
        f.fixed = fixed;
        f.param = initialEstimates;

        // Determine unknowns
        int nT = this.N_Params;
        for(int i=0; i<this.N_Params; i++)if(fixed[i])nT--;
        if(nT==0){
            if(plotFlag==0){
                throw new IllegalArgumentException("At least one parameter must be available for variation by the Regression procedure or GauasianPlot should have been called and not Gaussian");
            }
            else{
                plotFlag = 3;
            }
        }

        double[] start = new double[nT];
        double[] step = new double[nT];
        boolean[] constraint = new boolean[nT];

        // Fill arrays needed by the Simplex
        double xMin = MathFns.minimum(xData[0]);
        double xMax = MathFns.maximum(xData[0]);
        double yMax = MathFns.maximum(yData);
        if(initialEstimates[2]==0.0D){
            if(fixed[2]){
                throw new IllegalArgumentException("Scale factor has been fixed at zero");
            }
            else{
              initialEstimates[2] = yMax;
            }
        }
        int ii = 0;
        for(int i=0; i<this.N_Params; i++){
            if(!fixed[i]){
                start[ii] = initialEstimates[i];
                step[ii] = start[ii]*0.1D;
                if(step[ii]==0.0D)step[ii] = (xMax - xMin)*0.1D;
                constraint[ii] = false;
                if(i==1)constraint[ii] = true;
                ii++;
            }
        }
        this.N_Params = nT;

	    // Nelder and Mead Simplex Regression
	    for(int i=0; i<this.N_Params; i++){
            if(constraint[i])this.addConstraint(i,-1, 0.0D);
        }
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        if(plotFlag!=3)this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(plotFlag==3){
            // Plot results
            int flag = this.plotXYfixed(vec2, "Gaussian distribution - all parameters fixed");
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }

	}

    // FIT TO LOG-NORMAL DISTRIBUTIONS (TWO AND THREE PARAMETERS)

    // TWO PARAMETER LOG-NORMAL DISTRIBUTION
    // Fit to a two parameter log-normal distribution
	public void logNormal(){
	    this.fitLogNormalTwoPar(0);
	}

	public void logNormalTwoPar(){
	    this.fitLogNormalTwoPar(0);
	}

    // Fit to a two parameter log-normal distribution and plot result
	public void logNormalPlot(){
	    this.fitLogNormalTwoPar(1);
	}

    public void logNormalTwoParPlot(){
	    this.fitLogNormalTwoPar(1);
	}

    // Fit data to a two parameterlog-normal probability function
	protected void fitLogNormalTwoPar(int plotFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=36;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=3;
	    if(!this.scaleFlag)this.N_Params=2;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitLogNormalTwoPar(): This implementation of the two parameter log-nprmal distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // Calculate  x value at peak y
	    Vector<Object> ret1 = Regression.dataSign(yData);
	 	Integer tempi = null;
	    tempi = (Integer)ret1.elementAt(5);
	 	int peaki = tempi.intValue();
	    double mean = xData[0][peaki];

	    // Calculate an estimate of the mu
	    double mu = 0.0D;
	    for(int i=0; i<this.N_YData; i++)mu += Math.log(xData[0][i]);
	    mu /= this.N_YData;

	    // Calculate estimate of sigma
	    double sigma = 0.0D;
	    for(int i=0; i<this.N_YData; i++)sigma += MathFns.square(Math.log(xData[0][i]) - mu);
	    sigma = Math.sqrt(sigma/this.N_YData);

	    // Calculate estimate of y scale
	    tempd = (Double)ret1.elementAt(4);
	    double ym = tempd.doubleValue();
	    ym=ym*Math.exp(mu - sigma*sigma/2);

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        start[0] = mu;
        start[1] = sigma;
        if(this.scaleFlag){
            start[2] = ym;
        }
        step[0] = 0.1D*start[0];
        step[1] = 0.1D*start[1];
        if(step[0]==0.0D){
            Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	    Double tempdd = null;
	        tempdd = (Double)ret0.elementAt(2);
	 	    double xmax = tempdd.doubleValue();
	 	    if(xmax==0.0D){
	 	        tempdd = (Double)ret0.elementAt(0);
	 	        xmax = tempdd.doubleValue();
	 	    }
	        step[0]=xmax*0.1D;
	    }
	    if(step[0]==0.0D){
	        Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	    Double tempdd = null;
	        tempdd = (Double)ret0.elementAt(2);
	 	    double xmax = tempdd.doubleValue();
	 	    if(xmax==0.0D){
	 	        tempdd = (Double)ret0.elementAt(0);
	 	        xmax = tempdd.doubleValue();
	 	    }
	        step[1]=xmax*0.1D;
	    }
        if(this.scaleFlag)step[2] = 0.1D*start[2];

	    // Nelder and Mead Simplex Regression
        LogNormalTwoParFunction f = new LogNormalTwoParFunction();
        this.addConstraint(1,-1,0.0D);
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }
	}


    // THREE PARAMETER LOG-NORMAL DISTRIBUTION
    // Fit to a three parameter log-normal distribution
	public void logNormalThreePar(){
	    this.fitLogNormalTwoPar(0);
	}

    // Fit to a three parameter log-normal distribution and plot result
    public void logNormalThreeParPlot(){
	    this.fitLogNormalThreePar(1);
	}

    // Fit data to a three parameter log-normal probability function
	protected void fitLogNormalThreePar(int plotFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=37;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=4;
	    if(!this.scaleFlag)this.N_Params=3;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitLogNormalThreePar(): This implementation of the three parameter log-normal distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // Calculate  x value at peak y
	    Vector<Object> ret1 = Regression.dataSign(yData);
	 	Integer tempi = null;
	    tempi = (Integer)ret1.elementAt(5);
	 	int peaki = tempi.intValue();
	    double mean = xData[0][peaki];

	    // Calculate an estimate of the gamma
	    double gamma = 0.0D;
	    for(int i=0; i<this.N_YData; i++)gamma += xData[0][i];
	    gamma /= this.N_YData;

	    // Calculate estimate of beta
	    double beta = 0.0D;
	    for(int i=0; i<this.N_YData; i++)beta += MathFns.square(Math.log(xData[0][i]) - Math.log(gamma));
	    beta = Math.sqrt(beta/this.N_YData);

	    // Calculate estimate of alpha
	    Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	Double tempdd = null;
	 	tempdd = (Double)ret0.elementAt(0);
	 	double xmin = tempdd.doubleValue();
	    tempdd = (Double)ret0.elementAt(2);
	 	double xmax = tempdd.doubleValue();
	    double alpha = xmin - (xmax - xmin)/100.0D;;
	    if(xmin==0.0D)alpha -= (xmax - xmin)/100.0D;


	    // Calculate estimate of y scale
	    tempd = (Double)ret1.elementAt(4);
	    double ym = tempd.doubleValue();
	    ym=ym*(gamma+alpha)*Math.exp(- beta*beta/2);

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        start[0] = alpha;
        start[1] = beta;
        start[2] = gamma;
        if(this.scaleFlag){
            start[3] = ym;
        }
        step[0] = 0.1D*start[0];
        step[1] = 0.1D*start[1];
        step[2] = 0.1D*start[2];
        for(int i=0; i<3; i++){
            if(step[i]==0.0D)step[i]=xmax*0.1D;
        }
        if(this.scaleFlag)step[3] = 0.1D*start[3];

	    // Nelder and Mead Simplex Regression
        LogNormalThreeParFunction f = new LogNormalThreeParFunction();
        this.addConstraint(0,+1,xmin);
        this.addConstraint(1,-1,0.0D);
        this.addConstraint(2,-1,0.0D);

        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }
	}


    // FIT TO A LORENTZIAN DISTRIBUTION

    // Fit data to a lorentzian
	public void lorentzian(){
	    this.fitLorentzian(0);
	}

	public void lorentzianPlot(){
	    this.fitLorentzian(1);
	}

	protected void fitLorentzian(int allTest){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=5;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=3;
	    if(!this.scaleFlag)this.N_Params=2;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

        // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitLorentzian(): This implementation of the Lorentzian distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // Calculate  x value at peak y (estimate of the distribution mode)
	    Vector ret1 = Regression.dataSign(yData);
	 	Integer tempi = null;
	    tempi = (Integer)ret1.elementAt(5);
	 	int peaki = tempi.intValue();
	    double mean = xData[0][peaki];

	    // Calculate an estimate of the half-height width
	    double sd = halfWidth(xData[0], yData);

	    // Calculate estimate of y scale
	    tempd = (Double)ret1.elementAt(4);
	    double ym = tempd.doubleValue();
	    ym=ym*sd*Math.PI/2.0D;

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        start[0] = mean;
        start[1] = sd*0.9D;
        if(this.scaleFlag){
            start[2] = ym;
         }
        step[0] = 0.2D*sd;
        if(step[0]==0.0D){
            Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	    Double tempdd = null;
	        tempdd = (Double)ret0.elementAt(2);
	 	    double xmax = tempdd.doubleValue();
	 	    if(xmax==0.0D){
	 	        tempdd = (Double)ret0.elementAt(0);
	 	        xmax = tempdd.doubleValue();
	 	    }
	        step[0]=xmax*0.1D;
	    }
        step[1] = 0.2D*start[1];
        if(this.scaleFlag)step[2] = 0.2D*start[2];

	    // Nelder and Mead Simplex Regression
        LorentzianFunction f = new LorentzianFunction();
        this.addConstraint(1,-1,0.0D);
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(allTest==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }

	}


	// Static method allowing fitting of a data array to one or several of the above distributions
	public static void fitOneOrSeveralDistributions(double[] array){

        int numberOfPoints = array.length;          // number of points
        double maxValue = MathFns.maximum(array);     // maximum value of distribution
        double minValue = MathFns.minimum(array);     // minimum value of distribution
        double span = maxValue - minValue;          // span of distribution

        // Calculation of number of bins and bin width
        int numberOfBins = (int)Math.ceil(Math.sqrt(numberOfPoints));
        double binWidth = span/numberOfBins;
        double averagePointsPerBin = (double)numberOfPoints/(double)numberOfBins;

        // Option for altering bin width
        String comment = "Maximum value:  " + maxValue + "\n";
        comment += "Minimum value:  " + minValue + "\n";
        comment += "Suggested bin width:  " + binWidth + "\n";
        comment += "Giving an average points per bin:  " + averagePointsPerBin + "\n";
        comment += "If you wish to change the bin width enter the new value below \n";
        comment += "and click on OK\n";
        comment += "If you do NOT wish to change the bin width simply click on OK";
        binWidth = Db.readDouble(comment, binWidth);

        // Create output file
        comment = "Input the name of the output text file\n";
        comment += "[Do not forget the extension, e.g.   .txt]";
        String outputTitle = Db.readLine(comment, "fitOneOrSeveralDistributionsOutput.txt");
        FileOutput fout = new FileOutput(outputTitle, 'n');
        fout.println("Fitting a set of data to one or more distributions");
        fout.println("Class Regression/StatUtils: method fitAllDistributions");
        fout.dateAndTimeln();
        fout.println();
        fout.printtab("Number of points: ");
        fout.println(numberOfPoints);
        fout.printtab("Minimum value: ");
        fout.println(minValue);
        fout.printtab("Maximum value: ");
        fout.println(maxValue);
        fout.printtab("Number of bins: ");
        fout.println(numberOfBins);
        fout.printtab("Bin width: ");
        fout.println(binWidth);
        fout.printtab("Average number of points per bin: ");
        fout.println(averagePointsPerBin);
        fout.println();

        // Choose distributions and perform regression
        String[] comments = {"Gaussian Distribution", "Two parameter Log-normal Distribution", "Three parameter Log-normal Distribution", "Logistic Distribution", "Lorentzian Distribution",  "Type 1 Extreme Distribution - Gumbel minimum order statistic", "Type 1 Extreme Distribution - Gumbel maximum order statistic", "Type 2 Extreme Distribution - Frechet", "Type 3 Extreme Distribution - Weibull", "Type 3 Extreme Distribution - Exponential Distribution", "Type 3 Extreme Distribution - Rayleigh Distribution", "Pareto Distribution", "Beta Distribution", "Gamma Distribution", "Erlang Distribution", "exit"};
        String[] boxTitles = {" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", "exit"};
        String headerComment = "Choose next distribution to be fitted by clicking on box number";
        int defaultBox = 1;
        boolean testDistType = true;
        Regression reg = null;
        double[] coeff = null;
        while(testDistType){
                int opt =  Db.optionBox(headerComment, comments, boxTitles, defaultBox);
                switch(opt){
                    case 1: // Gaussian
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.gaussianPlot();
                            coeff = reg.getCoeff();
                            fout.println("NORMAL (GAUSSIAN) DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Mean [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Standard deviation [sigma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[2]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 2: // Two parameter Log-normal
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.logNormalTwoParPlot();
                            coeff = reg.getCoeff();
                            fout.println("LOG-NORMAL DISTRIBUTION (two parameter statistic)");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Shape parameter [sigma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[2]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 3: // Three parameter Log-normal
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.logNormalThreeParPlot();
                            coeff = reg.getCoeff();
                            fout.println("LOG-NORMAL DISTRIBUTION (three parameter statistic)");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [alpha] ");
                            fout.println(coeff[0]);
                            fout.printtab("Shape parameter [beta] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scale parameter [gamma] ");
                            fout.println(coeff[2]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[3]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 4: // Logistic
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.logisticPlot();
                            coeff = reg.getCoeff();
                            fout.println("LOGISTIC DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [beta] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[2]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 5: // Lorentzian
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.lorentzianPlot();
                            coeff = reg.getCoeff();
                            fout.println("LORENTZIAN DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Mean [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Half-height parameter [Gamma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[2]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 6: // Gumbel [minimum]
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.gumbelMinPlot();
                            coeff = reg.getCoeff();
                            fout.println("TYPE 1 (GUMBEL) EXTREME DISTRIBUTION [MINIMUM ORDER STATISTIC]");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [sigma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[2]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 7: // Gumbel [maximum]
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.gumbelMaxPlot();
                            coeff = reg.getCoeff();
                            fout.println("TYPE 1 (GUMBEL) EXTREME DISTRIBUTION [MAXIMUM ORDER STATISTIC]");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [sigma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[2]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 8: // Frechet
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.frechetPlot();
                            coeff = reg.getCoeff();
                            fout.println("TYPE 2 (FRECHET) EXTREME DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [sigma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Shape parameter [gamma] ");
                            fout.println(coeff[2]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[3]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 9: // Weibull
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.weibullPlot();
                            coeff = reg.getCoeff();
                            fout.println("TYPE 3 (WEIBULL) EXTREME DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [sigma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Shape parameter [gamma] ");
                            fout.println(coeff[2]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[3]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 10: // Exponential
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.exponentialPlot();
                            coeff = reg.getCoeff();
                            fout.println("EXPONENTIAL DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [sigma] ");
                            fout.println(coeff[1]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[2]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 11: // Rayleigh
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.rayleighPlot();
                            coeff = reg.getCoeff();
                            fout.println("RAYLEIGH DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Scale parameter [beta] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[1]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 12: // Pareto
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.paretoThreeParPlot();
                            coeff = reg.getCoeff();
                            fout.println("PARETO DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Shape parameter [alpha] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [beta] ");
                            fout.println(coeff[1]);
                            fout.printtab("Threshold parameter [theta] ");
                            fout.println(coeff[2]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[3]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 13: // Beta
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.betaMinMaxPlot();
                            coeff = reg.getCoeff();
                            fout.println("BETA DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Shape parameter [alpha] ");
                            fout.println(coeff[0]);
                            fout.printtab("Shape parameter [beta] ");
                            fout.println(coeff[1]);
                            fout.printtab("minimum limit [min] ");
                            fout.println(coeff[2]);
                            fout.printtab("maximum limit [max] ");
                            fout.println(coeff[3]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[4]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 14: // Gamma
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.gammaPlot();
                            coeff = reg.getCoeff();
                            fout.println("GAMMA DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Location parameter [mu] ");
                            fout.println(coeff[0]);
                            fout.printtab("Scale parameter [beta] ");
                            fout.println(coeff[1]);
                            fout.printtab("Shape parameter [gamma] ");
                            fout.println(coeff[2]);
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[3]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 15: // Erlang
                            reg = new Regression(array, binWidth);
                            reg.supressPrint();
                            reg.erlangPlot();
                            coeff = reg.getCoeff();
                            fout.println("ERLANG DISTRIBUTION");
                            fout.println("Best Estimates:");
                            fout.printtab("Shape parameter [lambda] ");
                            fout.println(coeff[0]);
                            fout.printtab("Rate parameter [k] ");
                            fout.println(reg.getKayValue());
                            fout.printtab("Scaling factor [Ao] ");
                            fout.println(coeff[1]);
                            Regression.regressionDetails(fout, reg);
                            break;
                    case 16: // exit
                    default: fout.close();
                             testDistType = false;
                }
            }
    }

    // Output method for fitOneOrSeveralDistributions
    protected static void regressionDetails(FileOutput fout, Regression reg){
        fout.println();
        fout.println("Regression details:");
        fout.printtab("Chi squared: ");
        fout.println(reg.getChiSquare());
        fout.printtab("Reduced chi squared: ");
        fout.println(reg.getReducedChiSquare());
        fout.printtab("Sum of squares: ");
        fout.println(reg.getSumOfSquares());
        fout.printtab("Degrees of freedom: ");
        fout.println(reg.getDegFree());
        fout.printtab("Number of iterations: ");
        fout.println(reg.getNiter());
        fout.printtab("maximum number of iterations allowed: ");
        fout.println(reg.getNmax());
        fout.println();
        fout.println();
    }


    // Calculate the multiple correlation coefficient
    protected void multCorrelCoeff(double[] yy, double[] yyHat, double[] ww){

        // sum of reciprocal weights squared
        double sumRecipW = 0.0D;
        for(int i=0; i<this.N_YData; i++){
            sumRecipW += 1.0D/MathFns.square(ww[i]);
        }

        // weighted mean of yy
        double my = 0.0D;
        for(int j=0; j<this.N_YData; j++){
            my += yy[j]/MathFns.square(ww[j]);
        }
        my /= sumRecipW;


        // weighted mean of residuals
        double mr = 0.0D;
        double[] residuals = new double[this.N_YData];
        for(int j=0; j<this.N_YData; j++){
            residuals[j] = yy[j] - yyHat[j];
            mr += residuals[j]/MathFns.square(ww[j]);
        }
        mr /= sumRecipW;

        // calculate yy weighted sum of squares
        double s2yy = 0.0D;
        for(int k=0; k<this.N_YData; k++){
            s2yy += MathFns.square((yy[k]-my)/ww[k]);
        }

        // calculate residual weighted sum of squares
        double s2r = 0.0D;
        for(int k=0; k<this.N_YData; k++){
            s2r += MathFns.square((residuals[k]-mr)/ww[k]);
        }

        // calculate multiple coefficient of determination
        this.sampleR2 = 1.0D - s2r/s2yy;
        // calculate multiple correlation coefficient
        this.sampleR = Math.sqrt(this.sampleR2);

        if(this.N_Xarrays>1){
            this.multipleF = this.sampleR2*(this.N_YData-this.N_Xarrays)/((1.0D-this.sampleR2)*(this.N_Xarrays-1));
        }
    }

    // Get the Sample Correlation Coefficient
    public double getSampleR(){
        return this.sampleR;
    }

    // Get the Coefficient of Determination
    public double getSampleR2(){
        return this.sampleR2;
    }

    // Get the Multiple Correlation Coefficient F ratio
    public double getMultipleF(){
        if(this.N_Xarrays==1){
            System.out.println("Regression.getMultipleF - The rgression is not amultple regession: -10 returnrd");
        }
        return this.multipleF;
    }

    // check data arrays for sign, max, min and peak
 	protected static Vector<Object> dataSign(double[] data){

        Vector<Object> ret = new Vector<Object>();
        int n = data.length;

        //
	    double peak=0.0D;       // peak: larger of maximum and any abs(negative minimum)
	    int peaki=-1;           // index of above
	    double shift=0.0D;      // shift to make all positive if a mixture of positive and negative
	    double max=data[0];     // maximum
	    int maxi=0;             // index of above
	    double min=data[0];     // minimum
	    int mini=0;             // index of above
	    int signCheckPos=0;     // number of negative values
	    int signCheckNeg=0;     // number of positive values
	    int signCheckZero=0;    // number of zero values
	    int signFlag=-1;        // 0 all positive; 1 all negative; 2 positive and negative
	    double mean = 0.0D;     // mean value

	    for(int i=0; i<n; i++){
	        mean =+ data[i];
	        if(data[i]>max){
	            max=data[i];
	            maxi=i;
	        }
	        if(data[i]<min){
	            min=data[i];
	            mini=i;
	        }
	        if(data[i]==0.0D)signCheckZero++;
	        if(data[i]>0.0D)signCheckPos++;
	        if(data[i]<0.0D)signCheckNeg++;
	    }
	    mean /= (double)n;

	    if((signCheckZero+signCheckPos)==n){
	        peak=max;
	        peaki=maxi;
	        signFlag=0;
	    }
	    else{
	        if((signCheckZero+signCheckNeg)==n){
	            peak=min;
	            peaki=mini;
	            signFlag=1;
	        }
	        else{
	            peak=max;
	            peaki=maxi;
	            if(-min>max){
	                peak=min;
	                peak=mini;
	            }
	            signFlag=2;
	            shift=-min;
	        }
	    }

	    // transfer results to the Vector
	    ret.addElement(new Double(min));
	    ret.addElement(new Integer(mini));
	    ret.addElement(new Double(max));
	    ret.addElement(new Integer(maxi));
	    ret.addElement(new Double(peak));
	    ret.addElement(new Integer(peaki));
	    ret.addElement(new Integer(signFlag));
	    ret.addElement(new Double(shift));
	    ret.addElement(new Double(mean));

	    return ret;
	}

    public void frechet(){
	    this.fitFrechet(0, 0);
	}

	public void frechetPlot(){
	    this.fitFrechet(1, 0);
	}

	public void frechetTwoPar(){
	    this.fitFrechet(0, 1);
	}

	public void frechetTwoParPlot(){
	    this.fitFrechet(1, 1);
	}

	public void frechetStandard(){
	    this.fitFrechet(0, 2);
	}

	public void frechetStandardPlot(){
	    this.fitFrechet(1, 2);
	}

    protected void fitFrechet(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    switch(typeFlag){
    	    case 0: this.lastMethod=13;
	                this.N_Params=4;
	                break;
	        case 1: this.lastMethod=14;
	                this.N_Params=3;
	                break;
	        case 2: this.lastMethod=15;
	                this.N_Params=2;
	                break;
        }
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;
        this.frechetWeibull=true;
        this.fitFrechetWeibull(allTest, typeFlag);
    }

    // method for fitting data to either a Frechet or a Weibull distribution
    protected void fitFrechetWeibull(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

        // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // check y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    Integer tempi = null;
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();

	 	// check for infinity
	 	if(this.infinityCheck(yPeak, peaki)){
 	        retY = Regression.dataSign(yData);
	        tempd = (Double)retY.elementAt(4);
	        yPeak = tempd.doubleValue();
	        tempi = null;
	        tempi = (Integer)retY.elementAt(5);
	 	    peaki = tempi.intValue();
	 	}

 	    // check sign of y data
 	    String ss = "Weibull";
	    if(this.frechetWeibull)ss = "Frechet";
 	    boolean ySignFlag = false;
	    if(yPeak<0.0D){
	        this.reverseYsign(ss);
	        retY = Regression.dataSign(this.yData);
	        yPeak = -yPeak;
	        ySignFlag = true;
	    }

        // check y values for all very small values
        boolean magCheck=false;
        double magScale = this.checkYallSmall(yPeak, ss);
        if(magScale!=1.0D){
            magCheck=true;
            yPeak=1.0D;
        }

	    // minimum value of x
	    Vector<Object> retX = Regression.dataSign(this.xData[0]);
        tempd = (Double)retX.elementAt(0);
	    double xMin = tempd.doubleValue();

	    // maximum value of x
        tempd = (Double)retX.elementAt(2);
	    double xMax = tempd.doubleValue();

        // Calculate  x value at peak y (estimate of the 'distribution mode')
		double distribMode = xData[0][peaki];

	    // Calculate an estimate of the half-height width
	    double sd = Math.log(2.0D)*halfWidth(xData[0], yData);

	    // Save x-y-w data
	    double[] xx = new double[this.N_YData];
	    double[] yy = new double[this.N_YData];
	    double[] ww = new double[this.N_YData];

	    for(int i=0; i<this.N_YData; i++){
	        xx[i]=this.xData[0][i];
	        yy[i]=this.yData[i];
	        ww[i]=this.weight[i];
	    }

	    // Calculate the cumulative probability and return ordinate scaling factor estimate
	    double[] cumX = new double[this.N_YData];
	    double[] cumY = new double[this.N_YData];
	    double[] cumW = new double[this.N_YData];
	    ErrorProp[] cumYe = ErrorProp.oneDarray(this.N_YData);
        double yScale = this.calculateCumulativeValues(cumX, cumY, cumW, cumYe, peaki, yPeak, distribMode, ss);

	    //Calculate loglog v log transforms
	    if(this.frechetWeibull){
	        for(int i=0; i<this.N_YData; i++){
	            cumYe[i] = ErrorProp.over(1.0D, cumYe[i]);
	            cumYe[i] = ErrorProp.log(cumYe[i]);
	            cumYe[i] = ErrorProp.log(cumYe[i]);
	            cumY[i] = cumYe[i].getValue();
	            cumW[i] = cumYe[i].getError();
	        }
	    }
	    else{
	        for(int i=0; i<this.N_YData; i++){
	            cumYe[i] = ErrorProp.minus(1.0D,cumYe[i]);
	            cumYe[i] = ErrorProp.over(1.0D, cumYe[i]);
	            cumYe[i] = ErrorProp.log(cumYe[i]);
	            cumYe[i] = ErrorProp.log(cumYe[i]);
	            cumY[i] = cumYe[i].getValue();
	            cumW[i] = cumYe[i].getError();
	        }
        }

        // Fill data arrays with transformed data
        for(int i =0; i<this.N_YData; i++){
	                xData[0][i] = cumX[i];
	                yData[i] = cumY[i];
	                weight[i] = cumW[i];
	    }
	    boolean weightOptHold = this.weightOpt;
	    this.weightOpt=true;

		// Nelder and Mead Simplex Regression for semi-linearised Frechet or Weibull
		// disable statistical analysis
		this.statFlag=false;

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        for(int i=0; i<this.N_Params; i++){
            start[i]=1.0D;
            step[i]=0.2D;
        }
        switch(typeFlag){
    	    case 0:
                    start[0] = xMin*0.9D;         //mu
                    start[1] = sd;                //sigma
                    start[2] = 4.0D;              //gamma
                    step[0] = 0.2D*start[0];
                    if(step[0]==0.0D){
                        Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	                Double tempdd = null;
	                    tempdd = (Double)ret0.elementAt(2);
	 	                double xmax = tempdd.doubleValue();
	 	                if(xmax==0.0D){
	 	                    tempdd = (Double)ret0.elementAt(0);
	 	                    xmax = tempdd.doubleValue();
	 	                }
	                    step[0]=xmax*0.1D;
	                }
                    step[1] = 0.2D*start[1];
                    step[2] = 0.5D*start[2];
                    this.addConstraint(0,+1,xMin);
                    this.addConstraint(1,-1,0.0D);
                    this.addConstraint(2,-1,0.0D);
                    break;
    	    case 1: start[0] = sd;                //sigma
                    start[1] = 4.0D;              //gamma
                    step[0] = 0.2D*start[0];
                    step[1] = 0.5D*start[1];
                    this.addConstraint(0,-1,0.0D);
                    this.addConstraint(1,-1,0.0D);
                    break;
    	    case 2: start[0] = 4.0D;              //gamma
                    step[0] = 0.5D*start[0];
                    this.addConstraint(0,-1,0.0D);
                    break;
        }

        // Create instance of loglog function and perform regression
        if(this.frechetWeibull){
            FrechetFunctionTwo f = new FrechetFunctionTwo();
            f.typeFlag = typeFlag;
            Vector<Object> vec2 = new Vector<Object>();
            vec2.addElement(f);
            this.nelderMead(vec2, start, step, this.fTol, this.nMax);
        }
        else{
            WeibullFunctionTwo f = new WeibullFunctionTwo();
            f.typeFlag = typeFlag;
            Vector<Object> vec2 = new Vector<Object>();
            vec2.addElement(f);
            this.nelderMead(vec2, start, step, this.fTol, this.nMax);
        }

	    // Get best estimates of loglog regression
	    double[] ests = new double[this.N_Params];
	    for (int i=0;i<this.N_Params; ++i){
	        tempd = (Double)this.best.elementAt(i);
	        ests[i]=tempd.doubleValue();
	    }

	    // Nelder and Mead Simplex Regression for Frechet or Weibull
	    // using best estimates from loglog regression as initial estimates

		// enable statistical analysis
		this.statFlag=true;

	    // restore data reversing the loglog transform but maintaining any sign reversals
	    this.weightOpt=weightOptHold;
	    for(int i =0; i<this.N_YData; i++){
	        xData[0][i] = xx[i];
	        yData[i] = yy[i];
	        weight[i] = ww[i];
	    }

        // Fill arrays needed by the Simplex
        switch(typeFlag){
            case 0: start[0] = ests[0];         //mu
                    start[1] = ests[1];         //sigma
                    start[2] = ests[2];         //gamma
                    if(this.scaleFlag){
                        start[3] = 1.0/yScale;      //y axis scaling factor
                     }
                    step[0] = 0.1D*start[0];
                    if(step[0]==0.0D){
                        Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	                Double tempdd = null;
	                    tempdd = (Double)ret0.elementAt(2);
	 	                double xmax = tempdd.doubleValue();
	 	                if(xmax==0.0D){
	 	                    tempdd = (Double)ret0.elementAt(0);
	 	                    xmax = tempdd.doubleValue();
	 	                }
	                    step[0]=xmax*0.1D;
	                }
                    step[1] = 0.1D*start[1];
                    step[2] = 0.1D*start[2];
                    if(this.scaleFlag){
                        step[3] = 0.1D*start[3];
                    }
                   break;
            case 1: start[0] = ests[0];         //sigma
                    start[1] = ests[1];         //gamma
                    if(this.scaleFlag){
                        start[2] = 1.0/yScale;      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    step[1] = 0.1D*start[1];
                    if(this.scaleFlag)step[2] = 0.1D*start[2];
                    break;
            case 2: start[0] = ests[0];         //gamma
                    if(this.scaleFlag){
                        start[1] = 1.0/yScale;      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    if(this.scaleFlag)step[1] = 0.1D*start[1];
                    break;
        }

        // Create instance of Frechet function and perform regression
        if(this.frechetWeibull){
            FrechetFunctionOne ff = new FrechetFunctionOne();
            ff.typeFlag = typeFlag;
            ff.scaleOption = this.scaleFlag;
            ff.scaleFactor = this.yScaleFactor;
            Vector<Object> vec2 = new Vector<Object>();
            vec2.addElement(ff);
            this.nelderMead(vec2, start, step, this.fTol, this.nMax);
            if(allTest==1){
                // Print results
                if(!this.supressPrint)this.print();
                // Plot results
                int flag = this.plotXY(ff);
                if(flag!=-2 && !this.supressYYplot)this.plotYY();
            }
        }
        else{
            WeibullFunctionOne ff = new WeibullFunctionOne();
            ff.typeFlag = typeFlag;
            ff.scaleOption = this.scaleFlag;
            ff.scaleFactor = this.yScaleFactor;
            Vector<Object> vec2 = new Vector<Object>();
            vec2.addElement(ff);
            this.nelderMead(vec2, start, step, this.fTol, this.nMax);
            if(allTest==1){
                // Print results
                if(!this.supressPrint)this.print();
                // Plot results
                int flag = this.plotXY(ff);
                if(flag!=-2 && !this.supressYYplot)this.plotYY();
            }
        }

        // restore data
        this.weightOpt = weightOptHold;
	    if(magCheck){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i] = yy[i]/magScale;
	            if(this.weightOpt)this.weight[i] = ww[i]/magScale;
	        }
	    }
	    if(ySignFlag){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i]=-this.yData[i];
	        }
	    }
	}

	// Check for y value = infinity
	public boolean infinityCheck(double yPeak, int peaki){
	    boolean flag=false;
	 	if(yPeak == 1.0D/0.0D || yPeak == -1.0D/0.0D){
	 	    int ii = peaki+1;
	 	    if(peaki==this.N_YData-1)ii = peaki-1;
	 	    this.xData[0][peaki]=this.xData[0][ii];
	 	    this.yData[peaki]=this.yData[ii];
 	        this.weight[peaki]=this.weight[ii];
 	        System.out.println("An infinty has been removed at point "+peaki);
	 	    flag = true;
 	    }
 	    return flag;
    }

    // reverse sign of y values if negative
    public void reverseYsign(String ss){
	        System.out.println("This implementation of the " + ss + " distributions takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                this.yData[i] = -this.yData[i];
	        }
	}

    // check y values for all y are very small value
    public double checkYallSmall(double yPeak, String ss){
	    double magScale = 1.0D;
	    double recipYpeak = MathFns.truncate(1.0/yPeak, 4);
        if(yPeak<1e-4){
            System.out.println(ss + " fitting: The ordinate axis (y axis) has been rescaled by "+recipYpeak+" to reduce rounding errors");
            for(int i=0; i<this.N_YData; i++){
                this.yData[i]*=recipYpeak;
                if(this.weightOpt)this.weight[i]*=recipYpeak;
            }
            magScale=recipYpeak;
        }
        return magScale;
    }

    // Calculate cumulative values
    public double calculateCumulativeValues(double[] cumX, double[] cumY, double[] cumW, ErrorProp[] cumYe, int peaki, double yPeak, double distribMode, String ss){
        cumX[0]= this.xData[0][0];
	    for(int i=1; i<this.N_YData; i++){
            cumX[i] = this.xData[0][i];
	    }

	    ErrorProp[] yE = ErrorProp.oneDarray(this.N_YData);
	    for(int i=0; i<this.N_YData; i++){
            yE[i].reset(this.yData[i], this.weight[i]);
	    }

	    // check on shape of data for first step of cumulative calculation
	    if(peaki!=0){
	        if(peaki==this.N_YData-1){
	            System.out.println("The data does not cover a wide enough range of x values to fit to a " + ss + " distribution with any accuracy");
	            System.out.println("The regression will be attempted but you should treat any result with great caution");
	        }
	        if(this.yData[0]<this.yData[1]*0.5D && this.yData[0]>distribMode*0.02D){
	            ErrorProp x0 = new ErrorProp(0.0D, 0.0D);
	            x0 = yE[0].times(this.xData[0][1]-this.xData[0][0]);
	            x0 = x0.over(yE[1].minus(yE[0]));
	            x0 = ErrorProp.minus(this.xData[0][0],x0);
	            if(this.yData[0]>=0.9D*yPeak)x0=(x0.plus(this.xData[0][0])).over(2.0D);
		        if(x0.getValue()<0.0D)x0.reset(0.0D, 0.0D);
	            cumYe[0] = yE[0].over(2.0D);
	            cumYe[0] = cumYe[0].times(ErrorProp.minus(this.xData[0][0], x0));
	        }
	        else{
	            cumYe[0].reset(0.0D, this.weight[0]);
	        }
	    }
	    else{
	        cumYe[0].reset(0.0D, this.weight[0]);

	    }

	    // cumulative calculation for rest of the points (trapezium approximation)
	    for(int i=1; i<this.N_YData; i++){
	        cumYe[i] = yE[i].plus(yE[i-1]);
            cumYe[i] = cumYe[i].over(2.0D);
	        cumYe[i] = cumYe[i].times(this.xData[0][i]-this.xData[0][i-1]);
	        cumYe[i] = cumYe[i].plus(cumYe[i-1]);
		    }
	    // check on shape of data for final step of cumulative calculation
	    ErrorProp cumYtotal = cumYe[this.N_YData-1].copy();
	    if(peaki==this.N_YData-1){
	        cumYtotal = cumYtotal.times(2.0D);
	    }
	    else{
	        if(this.yData[this.N_YData-1]<yData[this.N_YData-2]*0.5D && yData[this.N_YData-1]>distribMode*0.02D){
	            ErrorProp xn = new ErrorProp();
	            xn = yE[this.N_YData-1].times(this.xData[0][this.N_YData-2]-this.xData[0][this.N_YData-1]);
	            xn = xn.over(yE[this.N_YData-2].minus(yE[this.N_YData-1]));
	            xn = ErrorProp.minus(this.xData[0][this.N_YData-1], xn);
	            if(this.yData[0]>=0.9D*yPeak)xn=(xn.plus(this.xData[0][this.N_YData-1])).over(2.0D);
	            cumYtotal =  cumYtotal.plus(ErrorProp.times(0.5D,(yE[this.N_YData-1].times(xn.minus(this.xData[0][this.N_YData-1])))));
	        }
	    }
	    // estimate y scaling factor
	    double yScale = 1.0D/cumYtotal.getValue();
	    for(int i=0; i<this.N_YData; i++){
	        cumYe[i]=cumYe[i].over(cumYtotal);
	    }

	    // check for zero and negative  values
	    int jj = 0;
	    boolean test = true;
	    for(int i=0; i<this.N_YData; i++){
	        if(cumYe[i].getValue()<=0.0D){
	            if(i<=jj){
	                test=true;
	                jj = i;
	                while(test){
	                    jj++;
	                    if(jj>=this.N_YData)throw new ArithmeticException("all zero cumulative data!!");
	                    if(cumYe[jj].getValue()>0.0D){
	                        cumYe[i]=cumYe[jj].copy();
	                        cumX[i]=cumX[jj];
	                        test=false;
	                    }
	                }
	            }
	            else{
	                if(i==this.N_YData-1){
	                    cumYe[i]=cumYe[i-1].copy();
	                    cumX[i]=cumX[i-1];
	                }
	                else{
	                    cumYe[i]=cumYe[i-1].plus(cumYe[i+1]);
	                    cumYe[i]=cumYe[i].over(2.0D);
	                    cumX[i]=(cumX[i-1]+cumX[i+1])/2.0D;
	                }
	            }
	        }
	    }

	    // check for unity value
		jj = this.N_YData-1;
	    for(int i=this.N_YData-1; i>=0; i--){
	        if(cumYe[i].getValue()>=1.0D){
	            if(i>=jj){
	                test=true;
	                jj = this.N_YData-1;
	                while(test){
	                    jj--;
	                    if(jj<0)throw new ArithmeticException("all unity cumulative data!!");
	                    if(cumYe[jj].getValue()<1.0D){
	                        cumYe[i]=cumYe[jj].copy();
	                        cumX[i]=cumX[jj];
	                        test=false;
	                    }
	                }
	            }
	            else{
	                if(i==0){
	                    cumYe[i]=cumYe[i+1].copy();
	                    cumX[i]=cumX[i+1];
	                }
	                else{
	                    cumYe[i]=cumYe[i-1].plus(cumYe[i+1]);
	                    cumYe[i]=cumYe[i].over(2.0D);
	                    cumX[i]=(cumX[i-1]+cumX[i+1])/2.0D;
	                }
	            }
	        }
	    }
	    return yScale;
	}

    public void weibull(){
	    this.fitWeibull(0, 0);
	}

	public void weibullPlot(){
	    this.fitWeibull(1, 0);
	}

	public void weibullTwoPar(){
	    this.fitWeibull(0, 1);
	}

	public void weibullTwoParPlot(){
	    this.fitWeibull(1, 1);
	}

	public void weibullStandard(){
	    this.fitWeibull(0, 2);
	}

	public void weibullStandardPlot(){
	    this.fitWeibull(1, 2);
	}

    protected void fitWeibull(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    switch(typeFlag){
    	    case 0: this.lastMethod=16;
	                this.N_Params=4;
	                break;
	        case 1: this.lastMethod=17;
	                this.N_Params=3;
	                break;
	        case 2: this.lastMethod=18;
	                this.N_Params=2;
	                break;
        }
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;
        this.frechetWeibull=false;
        this.fitFrechetWeibull(allTest, typeFlag);
    }

	public void gumbelMin(){
	    this.fitGumbel(0, 0);
	}

	public void gumbelMinPlot(){
	    this.fitGumbel(1, 0);
	}

	public void gumbelMax(){
	    this.fitGumbel(0, 1);
	}
	public void gumbelMaxPlot(){
	    this.fitGumbel(1, 1);
	}

	public void gumbelMinOnePar(){
	    this.fitGumbel(0, 2);
	}

	public void gumbelMinOneParPlot(){
	    this.fitGumbel(1, 2);
	}

    public void gumbelMaxOnePar(){
	    this.fitGumbel(0, 3);
	}

	public void gumbelMaxOneParPlot(){
	    this.fitGumbel(1, 3);
	}

	public void gumbelMinStandard(){
	    this.fitGumbel(0, 4);
	}

	public void gumbelMinStandardPlot(){
	    this.fitGumbel(1, 4);
	}

	public void gumbelMaxStandard(){
	    this.fitGumbel(0, 5);
	}

	public void gumbelMaxStandardPlot(){
	    this.fitGumbel(1, 5);
	}

    // No parameters set for estimation
    // Correlation coefficient and plot
    protected void noParameters(String ss){
        System.out.println(ss+" Regression");
        System.out.println("No parameters set for estimation");
        System.out.println("Theoretical curve obtained");
	    String filename1="RegressOutput.txt";
	    String filename2="RegressOutputN.txt";
	    FileOutput fout = new FileOutput(filename1, 'n');
	    System.out.println("Results printed to the file "+filename2);
	    fout.dateAndTimeln(filename1);
        fout.println("No parameters set for estimation");
        switch(this.lastMethod){
            case 11:     fout.println("Minimal Standard Gumbel p(x) = exp(x)exp(-exp(x))");
                        for(int i=0; i<this.N_YData; i++)this.yHat[i]=Math.exp(this.xData[0][i])*Math.exp(-Math.exp(this.xData[0][i]));
                        break;
            case 12:    fout.println("Maximal Standard Gumbel p(x) = exp(-x)exp(-exp(-x))");
                        for(int i=0; i<this.N_YData; i++)this.yHat[i]=Math.exp(-this.xData[0][i])*Math.exp(-Math.exp(-this.xData[0][i]));
                        break;
            case 21:    fout.println("Standard Exponential p(x) = exp(-x)");
                        for(int i=0; i<this.N_YData; i++)this.yHat[i]=Math.exp(-this.xData[0][i]);
                        break;
        }
        this.sumOfSquares = 0.0D;
        this.chiSquare = 0.0D;
        double temp = 0.0D;
         for(int i=0; i<this.N_YData; i++){
            temp = MathFns.square(this.yData[i]-this.yHat[i]);
            this.sumOfSquares += temp;
            this.chiSquare += temp/MathFns.square(this.weight[i]);
        }
        double corrCoeff = StatFns.corrCoeff(this.yData, this.yHat);
        fout.printtab("Correlation Coefficient");
        fout.println(MathFns.truncate(corrCoeff, this.prec));
        fout.printtab("Correlation Coefficient Probability");
        fout.println(MathFns.truncate(1.0D-StatFns.corrCoeffProb(corrCoeff, this.degreesOfFreedom-1), this.prec));

        fout.printtab("Sum of Squares");
        fout.println(MathFns.truncate(this.sumOfSquares, this.prec));
        if(this.weightOpt || this.trueFreq){
            fout.printtab("Chi Square");
            fout.println(MathFns.truncate(this.chiSquare, this.prec));
            fout.printtab("chi square probability");
            fout.println(MathFns.truncate(StatFns.chiSquareProb(this.chiSquare, this.degreesOfFreedom-1), this.prec));
        }
        fout.println(" ");

        fout.printtab("x", this.field);
        fout.printtab("p(x) [expl]", this.field);
        fout.printtab("p(x) [calc]", this.field);
        fout.println("residual");

        for(int i=0; i<this.N_YData; i++){
            fout.printtab(MathFns.truncate(this.xData[0][i], this.prec), this.field);
            fout.printtab(MathFns.truncate(this.yData[i], this.prec), this.field);
            fout.printtab(MathFns.truncate(this.yHat[i], this.prec), this.field);
            fout.println(MathFns.truncate(this.yData[i]-this.yHat[i], this.prec));
       }
       fout.close();
       this.plotXY();
       if(!this.supressYYplot)this.plotYY();
    }

	protected void fitGumbel(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    switch(typeFlag){
    	    case 0: this.lastMethod=7;
	                this.N_Params=3;
	                break;
	        case 1: this.lastMethod=8;
	                this.N_Params=3;
	                break;
	        case 2: this.lastMethod=9;
	                this.N_Params=2;
	                break;
	        case 3: this.lastMethod=10;
	                this.N_Params=2;
	                break;
	        case 4: this.lastMethod=11;
	                this.N_Params=1;
	                break;
            case 5: this.lastMethod=12;
	                this.N_Params=1;
	                break;
	    }
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;
	    this.zeroCheck = false;
		this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");
	    if(this.N_Params==0){
	        this.noParameters("Gumbel");
	    }
	    else{


	        // order data into ascending order of the abscissae
            Regression.sort(this.xData[0], this.yData, this.weight);

	        // check sign of y data
	        Double tempd=null;
	        Vector<Object> retY = Regression.dataSign(yData);
	        tempd = (Double)retY.elementAt(4);
	        double yPeak = tempd.doubleValue();
	        boolean yFlag = false;

	    if(yPeak<0.0D){
	        System.out.println("Regression.fitGumbel(): This implementation of the Gumbel distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // check  x data
	    Vector<Object> retX = Regression.dataSign(xData[0]);
	 	Integer tempi = null;

        // Calculate  x value at peak y (estimate of the 'distribution mode')
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();
	    double distribMode = xData[0][peaki];

	    // Calculate an estimate of the half-height width
	    double sd = halfWidth(xData[0], yData);

	    // Nelder and Mead Simplex Regression for Gumbel
        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        switch(typeFlag){
            case 0:
            case 1:
                    start[0] = distribMode;                     //mu
                    start[1] = sd*Math.sqrt(6.0D)/Math.PI;      //sigma
                    if(this.scaleFlag){
                        start[2] = yPeak*start[1]*Math.exp(1);      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    if(step[0]==0.0D){
                        Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	                Double tempdd = null;
	                    tempdd = (Double)ret0.elementAt(2);
	 	                double xmax = tempdd.doubleValue();
	 	                if(xmax==0.0D){
	 	                    tempdd = (Double)ret0.elementAt(0);
	 	                    xmax = tempdd.doubleValue();
	 	                }
	                    step[0]=xmax*0.1D;
	                }
                    step[1] = 0.1D*start[1];
                    if(this.scaleFlag)step[2] = 0.1D*start[2];

	                // Add constraints
                    this.addConstraint(1,-1,0.0D);
                    break;
            case 2:
            case 3:
                    start[0] = sd*Math.sqrt(6.0D)/Math.PI;      //sigma
                    if(this.scaleFlag){
                        start[1] = yPeak*start[0]*Math.exp(1);      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    if(this.scaleFlag)step[1] = 0.1D*start[1];
	                // Add constraints
                    this.addConstraint(0,-1,0.0D);
                    break;
            case 4:
            case 5:
                    if(this.scaleFlag){
                        start[0] = yPeak*Math.exp(1);               //y axis scaling factor
                        step[0] = 0.1D*start[0];
                    }
                    break;
        }

        // Create instance of Gumbel function
        GumbelFunction ff = new GumbelFunction();

        // Set minimum type / maximum type option
        ff.typeFlag = typeFlag;

        // Set ordinate scaling option
        ff.scaleOption = this.scaleFlag;
        ff.scaleFactor = this.yScaleFactor;

        if(typeFlag<4){

            // Perform simplex regression
            Vector<Object> vec2 = new Vector<Object>();
            vec2.addElement(ff);
            this.nelderMead(vec2, start, step, this.fTol, this.nMax);

            if(allTest==1){
                // Print results
                if(!this.supressPrint)this.print();

                // Plot results
                int flag = this.plotXY(ff);
                if(flag!=-2 && !this.supressYYplot)this.plotYY();
            }
        }
        else{
            // calculate exp exp term
            double[][] xxx = new double[1][this.N_YData];
            double aa=1.0D;
            if(typeFlag==5)aa=-1.0D;
            for(int i=0; i<this.N_YData; i++){
                xxx[0][i]=Math.exp(aa*this.xData[0][i])*Math.exp(-Math.exp(aa*this.xData[0][i]));
            }

            // perform linear regression
            this.linNonLin = true;
            this.generalLinear(xxx);

            if(!this.supressPrint)this.print();
            if(!this.supressYYplot)this.plotYY();
            this.plotXY();

            this.linNonLin = false;

        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }
        }
	}

	// sort elements x, y and w arrays of doubles into ascending order of the x array
    // using selection sort method
    protected static void sort(double[] x, double[] y, double[] w){
            int index = 0;
            int lastIndex = -1;
            int n = x.length;
            double holdx = 0.0D;
            double holdy = 0.0D;
            double holdw = 0.0D;

            while(lastIndex < n-1){
                index = lastIndex+1;
                for(int i=lastIndex+2; i<n; i++){
                    if(x[i]<x[index]){
                        index=i;
                    }
                }
                lastIndex++;
                holdx=x[index];
                x[index]=x[lastIndex];
                x[lastIndex]=holdx;
                holdy=y[index];
                y[index]=y[lastIndex];
                y[lastIndex]=holdy;
                holdw=w[index];
                w[index]=w[lastIndex];
                w[lastIndex]=holdw;
            }
        }

        // returns estimate of half-height width
        protected static double halfWidth(double[] xData, double[] yData){
            double ymax = yData[0];
            int imax = 0;
            int n = xData.length;

            for(int i=1; i<n; i++){
                if(yData[i]>ymax){
                    ymax=yData[i];
                    imax=i;
                }
            }
            ymax /= 2.0D;

            double halflow=-1.0D;
            double temp = -1.0D;
            int ihl=-1;
            if(imax>0){
                ihl=imax-1;
                halflow=Math.abs(ymax-yData[ihl]);
                for(int i=imax-2; i>=0; i--){
                    temp=Math.abs(ymax-yData[i]);
                    if(temp<halflow){
                        halflow=temp;
                        ihl=i;
                    }
                }
                halflow=Math.abs(xData[ihl]-xData[imax]);
            }

            double halfhigh=-1.0D;
            temp = -1.0D;
            int ihh=-1;
            if(imax<n-1){
                ihh=imax+1;
                halfhigh=Math.abs(ymax-yData[ihh]);
                for(int i=imax+2; i<n; i++){
                    temp=Math.abs(ymax-yData[i]);
                    if(temp<halfhigh){
                        halfhigh=temp;
                        ihh=i;
                    }
                }
                halfhigh=Math.abs(xData[ihh]-xData[imax]);
            }

            double halfw = 0.0D;
            int nd = 0;
            if(ihl!=-1){
                halfw += halflow;
                nd++;
            }
            if(ihh!=-1){
                halfw += halfhigh;
                nd++;
            }
            halfw /= nd;

            return halfw;
    }

    public void exponential(){
	    this.fitExponential(0, 0);
	}

	public void exponentialPlot(){
	    this.fitExponential(1, 0);
	}

	public void exponentialOnePar(){
	    this.fitExponential(0, 1);
	}

	public void exponentialOneParPlot(){
	    this.fitExponential(1, 1);
	}

	public void exponentialStandard(){
	    this.fitExponential(0, 2);
	}

	public void exponentialStandardPlot(){
	    this.fitExponential(1, 2);
	}

    protected void fitExponential(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    switch(typeFlag){
    	    case 0: this.lastMethod=19;
	                this.N_Params=3;
	                break;
	        case 1: this.lastMethod=20;
	                this.N_Params=2;
	                break;
	        case 2: this.lastMethod=21;
	                this.N_Params=1;
	                break;
        }
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;
   	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");
	    if(this.N_Params==0){
	        this.noParameters("Exponential");
	    }
	    else{

	    // Save x-y-w data
	    double[] xx = new double[this.N_YData];
	    double[] yy = new double[this.N_YData];
	    double[] ww = new double[this.N_YData];

	    for(int i=0; i<this.N_YData; i++){
	        xx[i]=this.xData[0][i];
	        yy[i]=this.yData[i];
	        ww[i]=this.weight[i];
	    }

        // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // check y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    Integer tempi = null;
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();

 	    // check sign of y data
 	    String ss = "Exponential";
 	    boolean ySignFlag = false;
	    if(yPeak<0.0D){
	        this.reverseYsign(ss);
	        retY = Regression.dataSign(this.yData);
	        yPeak = -yPeak;
	        ySignFlag = true;
	    }

        // check y values for all very small values
        boolean magCheck=false;
        double magScale = this.checkYallSmall(yPeak, ss);
        if(magScale!=1.0D){
            magCheck=true;
            yPeak=1.0D;
        }

	    // minimum value of x
	    Vector<Object> retX = Regression.dataSign(this.xData[0]);
        tempd = (Double)retX.elementAt(0);
	    double xMin = tempd.doubleValue();

        // estimate of sigma
        double yE = yPeak/Math.exp(1.0D);
        if(this.yData[0]<yPeak)yE = (yPeak+yData[0])/(2.0D*Math.exp(1.0D));
        double yDiff = Math.abs(yData[0]-yE);
        double yTest = 0.0D;
        int iE = 0;
        for(int i=1; i<this.N_YData; i++){
            yTest=Math.abs(this.yData[i]-yE);
            if(yTest<yDiff){
                yDiff=yTest;
                iE=i;
            }
        }
        double sigma = this.xData[0][iE]-this.xData[0][0];

	    // Nelder and Mead Simplex Regression
	    double[] start = new double[this.N_Params];
	    double[] step = new double[this.N_Params];

        // Fill arrays needed by the Simplex
        switch(typeFlag){
            case 0: start[0] = xMin*0.9;    //mu
                    start[1] = sigma;       //sigma
                    if(this.scaleFlag){
                        start[2] = yPeak*sigma; //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    if(step[0]==0.0D){
                        Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	                Double tempdd = null;
	                    tempdd = (Double)ret0.elementAt(2);
	 	                double xmax = tempdd.doubleValue();
	 	                if(xmax==0.0D){
	 	                    tempdd = (Double)ret0.elementAt(0);
	 	                    xmax = tempdd.doubleValue();
	 	                }
	                    step[0]=xmax*0.1D;
	                }
                    step[1] = 0.1D*start[1];
                    if(this.scaleFlag)step[2] = 0.1D*start[2];
                    break;
            case 1: start[0] = sigma;       //sigma
                    if(this.scaleFlag){
                        start[1] = yPeak*sigma; //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    if(this.scaleFlag)step[1] = 0.1D*start[1];
                    break;
            case 2: if(this.scaleFlag){
                        start[0] = yPeak;       //y axis scaling factor
                        step[0] = 0.1D*start[0];
                    }
                    break;
        }

        // Create instance of Exponential function and perform regression
        ExponentialFunction ff = new ExponentialFunction();
        ff.typeFlag = typeFlag;
        ff.scaleOption = this.scaleFlag;
        ff.scaleFactor = this.yScaleFactor;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(ff);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(allTest==1){
            // Print results
            if(!this.supressPrint)this.print();
            // Plot results
            int flag = this.plotXY(ff);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        // restore data
	    if(magCheck){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i] = yy[i]/magScale;
	            if(this.weightOpt)this.weight[i] = ww[i]/magScale;
	        }
	    }
	    if(ySignFlag){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i]=-this.yData[i];
	        }
	    }
	    }
	}

    // check for zero and negative  values
    public void checkZeroNeg(double [] xx, double[] yy, double[] ww){
	    int jj = 0;
	    boolean test = true;
	    for(int i=0; i<this.N_YData; i++){
	        if(yy[i]<=0.0D){
	            if(i<=jj){
	                test=true;
	                jj = i;
	                while(test){
	                    jj++;
	                    if(jj>=this.N_YData)throw new ArithmeticException("all zero cumulative data!!");
	                    if(yy[jj]>0.0D){
	                        yy[i]=yy[jj];
	                        xx[i]=xx[jj];
	                        ww[i]=ww[jj];
	                        test=false;
	                    }
	                }
	            }
	            else{
	                if(i==this.N_YData-1){
	                    yy[i]=yy[i-1];
	                    xx[i]=xx[i-1];
	                    ww[i]=ww[i-1];
	                }
	                else{
	                    yy[i]=(yy[i-1] + yy[i+1])/2.0D;
	                    xx[i]=(xx[i-1] + xx[i+1])/2.0D;
	                    ww[i]=(ww[i-1] + ww[i+1])/2.0D;
	                }
	            }
	        }
	    }
	}

	public void rayleigh(){
	    this.fitRayleigh(0, 0);
	}

	public void rayleighPlot(){
	    this.fitRayleigh(1, 0);
	}

    protected void fitRayleigh(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
    	this.lastMethod=22;
	    this.N_Params=2;
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;
   	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");


        // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // check y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    Integer tempi = null;
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();

 	    // check sign of y data
 	    String ss = "Rayleigh";
 	    boolean ySignFlag = false;
	    if(yPeak<0.0D){
	        this.reverseYsign(ss);
	        retY = Regression.dataSign(this.yData);
	        yPeak = -yPeak;
	        ySignFlag = true;
	    }

        // check y values for all very small values
        boolean magCheck=false;
        double magScale = this.checkYallSmall(yPeak, ss);
        if(magScale!=1.0D){
            magCheck=true;
            yPeak=1.0D;
        }

	    // Save x-y-w data
	    double[] xx = new double[this.N_YData];
	    double[] yy = new double[this.N_YData];
	    double[] ww = new double[this.N_YData];

	    for(int i=0; i<this.N_YData; i++){
	        xx[i]=this.xData[0][i];
	        yy[i]=this.yData[i];
	        ww[i]=this.weight[i];
	    }

	    // minimum value of x
	    Vector<Object> retX = Regression.dataSign(this.xData[0]);
        tempd = (Double)retX.elementAt(0);
	    double xMin = tempd.doubleValue();

	    // maximum value of x
        tempd = (Double)retX.elementAt(2);
	    double xMax = tempd.doubleValue();

        // Calculate  x value at peak y (estimate of the 'distribution mode')
		double distribMode = xData[0][peaki];

	    // Calculate an estimate of the half-height width
	    double sd = Math.log(2.0D)*halfWidth(xData[0], yData);

	    // Calculate the cumulative probability and return ordinate scaling factor estimate
	    double[] cumX = new double[this.N_YData];
	    double[] cumY = new double[this.N_YData];
	    double[] cumW = new double[this.N_YData];
	    ErrorProp[] cumYe = ErrorProp.oneDarray(this.N_YData);
        double yScale = this.calculateCumulativeValues(cumX, cumY, cumW, cumYe, peaki, yPeak, distribMode, ss);

	    //Calculate log  transform
	    for(int i=0; i<this.N_YData; i++){
	        cumYe[i] = ErrorProp.minus(1.0D,cumYe[i]);
	        cumYe[i] = ErrorProp.over(1.0D, cumYe[i]);
	        cumYe[i] = ErrorProp.log(cumYe[i]);
	        cumY[i] = cumYe[i].getValue();
	        cumW[i] = cumYe[i].getError();
        }

        // Fill data arrays with transformed data
        for(int i =0; i<this.N_YData; i++){
	        xData[0][i] = cumX[i];
	        yData[i] = cumY[i];
	        weight[i] = cumW[i];
	    }
	    boolean weightOptHold = this.weightOpt;
	    this.weightOpt=true;

		// Nelder and Mead Simplex Regression for semi-linearised Rayleigh
		// disable statistical analysis
		this.statFlag=false;

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        for(int i=0; i<this.N_Params; i++){
            start[i]=1.0D;
            step[i]=0.2D;
        }
        start[0] = sd;                //sigma
        step[0] = 0.2D;
        this.addConstraint(0,-1,0.0D);

        // Create instance of log function and perform regression
        RayleighFunctionTwo f = new RayleighFunctionTwo();
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

	    // Get best estimates of log regression
	    double[] ests = new double[this.N_Params];
	    for (int i=0;i<this.N_Params; ++i){
	        tempd = (Double)this.best.elementAt(i);
	        ests[i]=tempd.doubleValue();
	    }

		// enable statistical analysis
		this.statFlag=true;

	    // restore data reversing the loglog transform but maintaining any sign reversals
	    this.weightOpt=weightOptHold;
	    for(int i =0; i<this.N_YData; i++){
	        xData[0][i] = xx[i];
	        yData[i] = yy[i];
	        weight[i] = ww[i];
	    }

        // Fill arrays needed by the Simplex
        start[0] = ests[0];         //sigma
        if(this.scaleFlag){
            start[1] = 1.0/yScale;      //y axis scaling factor
        }
        step[0] = 0.1D*start[0];
        if(this.scaleFlag)step[1] = 0.1D*start[1];


        // Create instance of Rayleigh function and perform regression
        RayleighFunctionOne ff = new RayleighFunctionOne();
        ff.scaleOption = this.scaleFlag;
        ff.scaleFactor = this.yScaleFactor;
        Vector<Object> vec3 = new Vector<Object>();
        vec3.addElement(ff);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(allTest==1){
            // Print results
            if(!this.supressPrint)this.print();
            // Plot results
            int flag = this.plotXY(ff);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        // restore data
	    if(magCheck){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i] = yy[i]/magScale;
	            if(this.weightOpt)this.weight[i] = ww[i]/magScale;
	        }
	    }
	    if(ySignFlag){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i]=-this.yData[i];
	        }
	    }
	}

	// Three Parameter Pareto
    public void paretoThreePar(){
	    this.fitPareto(0, 3);
	}

	public void paretoThreeParPlot(){
	    this.fitPareto(1, 3);
	}

    // Two Parameter Pareto
	public void paretoTwoPar(){
	    this.fitPareto(0, 2);
	}
	// Deprecated
	public void pareto(){
	    this.fitPareto(0, 2);
	}

	public void paretoTwoParPlot(){
	    this.fitPareto(1, 2);
	}
	// Deprecated
    public void paretoPlot(){
	    this.fitPareto(1, 2);
	}

    // One Parameter Pareto
	public void paretoOnePar(){
	    this.fitPareto(0, 1);
	}

	public void paretoOneParPlot(){
	    this.fitPareto(1, 1);
	}

    // method for fitting data to a Pareto distribution
    protected void fitPareto(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    switch(typeFlag){
	        case 3: this.lastMethod=29;
	                this.N_Params=4;
	                break;
    	    case 2: this.lastMethod=23;
	                this.N_Params=3;
	                break;
	        case 1: this.lastMethod=24;
	                this.N_Params=2;
	                break;
	    }

	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");
 	    String ss = "Pareto";

        // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // check y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    Integer tempi = null;
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();

	 	// check for infinity
	 	if(this.infinityCheck(yPeak, peaki)){
 	        retY = Regression.dataSign(yData);
	        tempd = (Double)retY.elementAt(4);
	        yPeak = tempd.doubleValue();
	        tempi = null;
	        tempi = (Integer)retY.elementAt(5);
	 	    peaki = tempi.intValue();
	 	}

 	    // check sign of y data
 	    boolean ySignFlag = false;
	    if(yPeak<0.0D){
	        this.reverseYsign(ss);
	        retY = Regression.dataSign(this.yData);
	        yPeak = -yPeak;
	        ySignFlag = true;
	    }

        // check y values for all very small values
        boolean magCheck=false;
        double magScale = this.checkYallSmall(yPeak, ss);
        if(magScale!=1.0D){
            magCheck=true;
            yPeak=1.0D;
        }

	    // minimum value of x
	    Vector<Object> retX = Regression.dataSign(this.xData[0]);
        tempd = (Double)retX.elementAt(0);
	    double xMin = tempd.doubleValue();

	    // maximum value of x
        tempd = (Double)retX.elementAt(2);
	    double xMax = tempd.doubleValue();

        // Calculate  x value at peak y (estimate of the 'distribution mode')
		double distribMode = xData[0][peaki];

	    // Calculate an estimate of the half-height width
	    double sd = Math.log(2.0D)*halfWidth(xData[0], yData);

	    // Save x-y-w data
	    double[] xx = new double[this.N_YData];
	    double[] yy = new double[this.N_YData];
	    double[] ww = new double[this.N_YData];

	    for(int i=0; i<this.N_YData; i++){
	        xx[i]=this.xData[0][i];
	        yy[i]=this.yData[i];
	        ww[i]=this.weight[i];
	    }

	    // Calculate the cumulative probability and return ordinate scaling factor estimate
	    double[] cumX = new double[this.N_YData];
	    double[] cumY = new double[this.N_YData];
	    double[] cumW = new double[this.N_YData];
	    ErrorProp[] cumYe = ErrorProp.oneDarray(this.N_YData);
        double yScale = this.calculateCumulativeValues(cumX, cumY, cumW, cumYe, peaki, yPeak, distribMode, ss);

	    //Calculate l - cumlative probability
	    for(int i=0; i<this.N_YData; i++){
	        cumYe[i] = ErrorProp.minus(1.0D,cumYe[i]);
	        cumY[i] = cumYe[i].getValue();
	        cumW[i] = cumYe[i].getError();
        }

        // Fill data arrays with transformed data
        for(int i =0; i<this.N_YData; i++){
	                xData[0][i] = cumX[i];
	                yData[i] = cumY[i];
	                weight[i] = cumW[i];
	    }
	    boolean weightOptHold = this.weightOpt;
	    this.weightOpt=true;

		// Nelder and Mead Simplex Regression for Pareto estimated cdf
		// disable statistical analysis
		this.statFlag=false;

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        for(int i=0; i<this.N_Params; i++){
            start[i]=1.0D;
            step[i]=0.2D;
        }
        switch(typeFlag){
            case 3: start[0] = 2;           //alpha
                    start[1] = xMin*0.9D;   //beta
                    if(xMin<0){             //theta
                        start[2] = -xMin*1.1D;
                    }
                    else{
                        start[2] = xMin*0.01;
                    }
                    if(start[1]<0.0D)start[1]=0.0D;
                    step[0] = 0.2D*start[0];
                    step[1] = 0.2D*start[1];
                    if(step[1]==0.0D){
                        double xmax = xMax;
	 	                if(xmax==0.0D){
	 	                    xmax = xMin;
	 	                }
	                    step[1]=xmax*0.1D;
	                }
	                this.addConstraint(0,-1,0.0D);
	                this.addConstraint(1,-1,0.0D);
                    this.addConstraint(1,+1,xMin);
                    break;
    	    case 2: if(xMin<0)System.out.println("Method: FitParetoTwoPar/FitParetoTwoParPlot\nNegative data values present\nFitParetoThreePar/FitParetoThreeParPlot would have been more appropriate");
    	            start[0] = 2;           //alpha
                    start[1] = xMin*0.9D;   //beta
                    if(start[1]<0.0D)start[1]=0.0D;
                    step[0] = 0.2D*start[0];
                    step[1] = 0.2D*start[1];
                    if(step[1]==0.0D){
                        double xmax = xMax;
	 	                if(xmax==0.0D){
	 	                    xmax = xMin;
	 	                }
	                    step[1]=xmax*0.1D;
	                }
	                this.addConstraint(0,-1,0.0D);
	                this.addConstraint(1,-1,0.0D);
                    break;
    	    case 1: if(xMin<0)System.out.println("Method: FitParetoOnePar/FitParetoOneParPlot\nNegative data values present\nFitParetoThreePar/FitParetoThreeParPlot would have been more appropriate");
    	            start[0] = 2;                //alpha
                    step[0] = 0.2D*start[0];
                    this.addConstraint(0,-1,0.0D);
                    this.addConstraint(1,-1,0.0D);
                    break;
        }

        // Create instance of cdf function and perform regression
        ParetoFunctionTwo f = new ParetoFunctionTwo();
        f.typeFlag = typeFlag;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

	    // Get best estimates of cdf regression
	    double[] ests = new double[this.N_Params];
	    for (int i=0;i<this.N_Params; ++i){
	        tempd = (Double)this.best.elementAt(i);
	        ests[i]=tempd.doubleValue();
	    }

	    // Nelder and Mead Simplex Regression for Pareto
	    // using best estimates from cdf regression as initial estimates

		// enable statistical analysis
		this.statFlag=true;

	    // restore data reversing the cdf transform but maintaining any sign reversals
	    this.weightOpt=weightOptHold;
	    for(int i =0; i<this.N_YData; i++){
	        xData[0][i] = xx[i];
	        yData[i] = yy[i];
	        weight[i] = ww[i];
	    }

        // Fill arrays needed by the Simplex
        switch(typeFlag){
            case 3: start[0] = ests[0];                         //alpha
                    if(start[0]<=0.0D){
                        if(start[0]==0.0D){
                            start[0]=1.0D;
                        }
                        else{
                            start[0] = Math.min(1.0D,-start[0]);
                        }
                    }
                    start[1] = ests[1];                         //beta
                    if(start[1]<=0.0D){
                        if(start[1]==0.0D){
                            start[1]=1.0D;
                        }
                        else{
                            start[1] = Math.min(1.0D,-start[1]);
                        }
                    }
                    start[2] = ests[2];
                    if(this.scaleFlag){
                        start[3] = 1.0/yScale;    //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    step[1] = 0.1D*start[1];
                    if(step[1]==0.0D){
                        double xmax = xMax;
	 	                if(xmax==0.0D){
	 	                    xmax = xMin;
	 	                }
	                    step[1]=xmax*0.1D;
	                }
                    if(this.scaleFlag)step[2] = 0.1D*start[2];
                    break;
            case 2: start[0] = ests[0];                         //alpha
                    if(start[0]<=0.0D){
                        if(start[0]==0.0D){
                            start[0]=1.0D;
                        }
                        else{
                            start[0] = Math.min(1.0D,-start[0]);
                        }
                    }
                    start[1] = ests[1];                         //beta
                    if(start[1]<=0.0D){
                        if(start[1]==0.0D){
                            start[1]=1.0D;
                        }
                        else{
                            start[1] = Math.min(1.0D,-start[1]);
                        }
                    }
                    if(this.scaleFlag){
                        start[2] = 1.0/yScale;    //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    step[1] = 0.1D*start[1];
                    if(step[1]==0.0D){
                        double xmax = xMax;
	 	                if(xmax==0.0D){
	 	                    xmax = xMin;
	 	                }
	                    step[1]=xmax*0.1D;
	                }
                    if(this.scaleFlag)step[2] = 0.1D*start[2];
                    break;
            case 1: start[0] = ests[0];                         //alpha
                    if(start[0]<=0.0D){
                        if(start[0]==0.0D){
                            start[0]=1.0D;
                        }
                        else{
                            start[0] = Math.min(1.0D,-start[0]);
                        }
                    }
                    if(this.scaleFlag){
                        start[1] = 1.0/yScale;    //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    if(this.scaleFlag)step[1] = 0.1D*start[1];
                    break;
         }

        // Create instance of Pareto function and perform regression
        ParetoFunctionOne ff = new ParetoFunctionOne();
        ff.typeFlag = typeFlag;
        ff.scaleOption = this.scaleFlag;
        ff.scaleFactor = this.yScaleFactor;
        Vector<Object> vec3 = new Vector<Object>();
        vec3.addElement(ff);
        this.nelderMead(vec3, start, step, this.fTol, this.nMax);

        if(allTest==1){
            // Print results
            if(!this.supressPrint)this.print();
            // Plot results
            int flag = this.plotXY(ff);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        // restore data
        this.weightOpt = weightOptHold;
	    if(magCheck){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i] = yy[i]/magScale;
	            if(this.weightOpt)this.weight[i] = ww[i]/magScale;
	        }
	    }
	    if(ySignFlag){
	        for(int i =0; i<this.N_YData; i++){
	            this.yData[i]=-this.yData[i];
	        }
	    }
	}


	// method for fitting data to a sigmoid threshold function
    public void sigmoidThreshold(){
        fitSigmoidThreshold(0);
    }

    // method for fitting data to a sigmoid threshold function with plot and print out
    public void sigmoidThresholdPlot(){
        fitSigmoidThreshold(1);
    }


    // method for fitting data to a sigmoid threshold function
    protected void fitSigmoidThreshold(int plotFlag){

        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=25;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=3;
	    if(!this.scaleFlag)this.N_Params=2;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // Estimate  of theta
	    double yymin = MathFns.minimum(this.yData);
	    double yymax = MathFns.maximum(this.yData);
	    int dirFlag = 1;
	    if(yymin<0)dirFlag=-1;
	    double yyymid = (yymax - yymin)/2.0D;
	    double yyxmidl = xData[0][0];
	    int ii = 1;
	    int nLen = this.yData.length;
	    boolean test = true;
	    while(test){
	        if(this.yData[ii]>=dirFlag*yyymid){
	            yyxmidl = xData[0][ii];
	            test = false;
	        }
	        else{
	            ii++;
	            if(ii>=nLen){
	                yyxmidl = StatFns.mean(this.xData[0]);
	                ii=nLen-1;
                    test = false;
                }
	        }
	    }
	    double yyxmidh = xData[0][nLen-1];
	    int jj = nLen-1;
	    test = true;
	    while(test){
	        if(this.yData[jj]<=dirFlag*yyymid){
	            yyxmidh = xData[0][jj];
	            test = false;
	        }
	        else{
	            jj--;
	            if(jj<0){
	                yyxmidh = StatFns.mean(this.xData[0]);
	                jj=1;
                    test = false;
                }
	        }
	    }
	    int thetaPos = (ii+jj)/2;
        double theta0 = xData[0][thetaPos];

	    // estimate of slope
	    double thetaSlope1 = 2.0D*(yData[nLen-1] - theta0)/(xData[0][nLen-1] - xData[0][thetaPos]);
	    double thetaSlope2 = 2.0D*theta0/(xData[0][thetaPos] - xData[0][nLen-1]);
	    double thetaSlope = Math.max(thetaSlope1, thetaSlope2);

        // initial estimates
        double[] start = new double[N_Params];
        start[0] = 4.0D*thetaSlope;
        if(dirFlag==1){
            start[0] /= yymax;
        }
        else{
            start[0] /= yymin;
        }
        start[1] = theta0;
        if(this.scaleFlag){
            if(dirFlag==1){
                start[2] = yymax;
            }
            else{
                start[2] = yymin;
            }
        }

        // initial step sizes
        double[] step = new double[N_Params];
        for(int i=0; i<N_Params; i++)step[i] = 0.1*start[i];
        if(step[0]==0.0D)step[0] = 0.1*(xData[0][nLen-1] - xData[0][0])/(yData[nLen-1] - yData[0]);
        if(step[1]==0.0D)step[1] = (xData[0][nLen-1] - xData[0][0])/20.0D;
        if(this.scaleFlag)if(step[2]==0.0D)step[2] = 0.1*(yData[nLen-1] - yData[0]);

        // Nelder and Mead Simplex Regression
        SigmoidThresholdFunction f = new SigmoidThresholdFunction();
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(f);
        this.nelderMead(vec, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }
    }
    // method for fitting data to a Hill/Sips Sigmoid
    public void sigmoidHillSips(){
        fitsigmoidHillSips(0);
    }

    // method for fitting data to a Hill/Sips Sigmoid with plot and print out
    public void sigmoidHillSipsPlot(){
        fitsigmoidHillSips(1);
    }

    // method for fitting data to a Hill/Sips Sigmoid
    protected void fitsigmoidHillSips(int plotFlag){

        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=28;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=3;
	    if(!this.scaleFlag)this.N_Params=2;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // Estimate  of theta
	    double yymin = MathFns.minimum(this.yData);
	    double yymax = MathFns.maximum(this.yData);
	    int dirFlag = 1;
	    if(yymin<0)dirFlag=-1;
	    double yyymid = (yymax - yymin)/2.0D;
	    double yyxmidl = xData[0][0];
	    int ii = 1;
	    int nLen = this.yData.length;
	    boolean test = true;
	    while(test){
	        if(this.yData[ii]>=dirFlag*yyymid){
	            yyxmidl = xData[0][ii];
	            test = false;
	        }
	        else{
	            ii++;
	            if(ii>=nLen){
	                yyxmidl = StatFns.mean(this.xData[0]);
	                ii=nLen-1;
                    test = false;
                }
	        }
	    }
	    double yyxmidh = xData[0][nLen-1];
	    int jj = nLen-1;
	    test = true;
	    while(test){
	        if(this.yData[jj]<=dirFlag*yyymid){
	            yyxmidh = xData[0][jj];
	            test = false;
	        }
	        else{
	            jj--;
	            if(jj<0){
	                yyxmidh = StatFns.mean(this.xData[0]);
	                jj=1;
                    test = false;
                }
	        }
	    }
	    int thetaPos = (ii+jj)/2;
	    double theta0 = xData[0][thetaPos];

        // initial estimates
        double[] start = new double[N_Params];
        start[0] = theta0;
        start[1] = 1;
        if(this.scaleFlag){
            if(dirFlag==1){
                start[2] = yymax;
            }
            else{
                start[2] = yymin;
            }
        }

        // initial step sizes
        double[] step = new double[N_Params];
        for(int i=0; i<N_Params; i++)step[i] = 0.1*start[i];
        if(step[0]==0.0D)step[0] = (xData[0][nLen-1] - xData[0][0])/20.0D;
        if(this.scaleFlag)if(step[2]==0.0D)step[2] = 0.1*(yData[nLen-1] - yData[0]);

        // Nelder and Mead Simplex Regression
        SigmoidHillSipsFunction f = new SigmoidHillSipsFunction();
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(f);
        this.nelderMead(vec, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }
    }

    // method for fitting data to a EC50 dose response curve
    public void ec50(){
        fitEC50(0);
    }

    // method for fitting data to a EC50 dose response curve with plot and print out
    public void ec50Plot(){
        fitEC50(1);
    }

    // method for fitting data to a EC50 dose response curve
    // bottom constrained to zero or positive values
    public void ec50constrained(){
        fitEC50(2);
    }

    // method for fitting data to a EC50 dose response curve with plot and print out
    // bottom constrained to zero or positive values
    public void ec50constrainedPlot(){
        fitEC50(3);
    }

    // method for fitting data to a logEC50 dose response curve
    protected void fitEC50(int cpFlag){

        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
        int plotFlag = 0;
        boolean constrained = false;
	    switch(cpFlag){
	        case 0: this.lastMethod= 39;
	                plotFlag = 0;
	                break;
	        case 1: this.lastMethod= 39;
	                plotFlag = 1;
	                break;
	        case 2: this.lastMethod= 41;
	                plotFlag = 0;
	                constrained = true;
	                break;
	        case 3: this.lastMethod= 41;
	                plotFlag = 1;
	                constrained = true;
	                break;
	    }

	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=4;
	    this.scaleFlag = false;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // Estimate of bottom and top
	    double bottom = MathFns.minimum(this.yData);
	    double top = MathFns.maximum(this.yData);

	    // Estimate of EC50
	    int dirFlag = 1;
	    double yyymid = (top - bottom)/2.0D;
	    double yyxmidl = xData[0][0];
	    int ii = 1;
	    int nLen = this.yData.length;
	    boolean test = true;
	    while(test){
	        if(this.yData[ii]>=dirFlag*yyymid){
	            yyxmidl = xData[0][ii];
	            test = false;
	        }
	        else{
	            ii++;
	            if(ii>=nLen){
	                yyxmidl = StatFns.mean(this.xData[0]);
	                ii=nLen-1;
                    test = false;
                }
	        }
	    }
	    double yyxmidh = xData[0][nLen-1];
	    int jj = nLen-1;
	    test = true;
	    while(test){
	        if(this.yData[jj]<=dirFlag*yyymid){
	            yyxmidh = xData[0][jj];
	            test = false;
	        }
	        else{
	            jj--;
	            if(jj<0){
	                yyxmidh = StatFns.mean(this.xData[0]);
	                jj=1;
                    test = false;
                }
	        }
	    }
	    int thetaPos = (ii+jj)/2;
        double EC50 = xData[0][thetaPos];

	    // estimate of slope
	    double thetaSlope1 = 2.0D*(yData[nLen-1] - EC50)/(xData[0][nLen-1] - xData[0][thetaPos]);
	    double thetaSlope2 = 2.0D*EC50/(xData[0][thetaPos] - xData[0][nLen-1]);
	    double hillSlope = Math.max(thetaSlope1, thetaSlope2);

        // initial estimates
        double[] start = new double[N_Params];
        start[0] = bottom;
        start[1] = top;
        start[2] = EC50;
        start[3] = -hillSlope;

        // initial step sizes
        double[] step = new double[N_Params];
        for(int i=0; i<N_Params; i++)step[i] = 0.1*start[i];
        if(step[0]==0.0D)step[0] = 0.1*(yData[nLen-1] - yData[0]);
        if(step[1]==0.0D)step[1] = 0.1*(yData[nLen-1] - yData[0]) + yData[nLen-1];
        if(step[2]==0.0D)step[2] = 0.05*(xData[0][nLen-1] - xData[0][0]);
        if(step[3]==0.0D)step[3] = 0.1*(xData[0][nLen-1] - xData[0][0])/(yData[nLen-1] - yData[0]);

        // Constrained option
        if(constrained)this.addConstraint(0, -1, 0.0D);

        // Nelder and Mead Simplex Regression
        EC50Function f = new EC50Function();
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(f);
        this.nelderMead(vec, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }
    }

    // method for fitting data to a logEC50 dose response curve
    public void logEC50(){
        fitLogEC50(0);
    }

    // method for fitting data to a logEC50 dose response curve with plot and print out
    public void logEC50Plot(){
        fitLogEC50(1);
    }

    // method for fitting data to a logEC50 dose response curve
    // bottom constrained to zero or positive values
    public void logEC50constrained(){
        fitLogEC50(2);
    }

    // method for fitting data to a logEC50 dose response curve with plot and print out
    // bottom constrained to zero or positive values
    public void logEC50constrainedPlot(){
        fitLogEC50(3);
    }

    // method for fitting data to a logEC50 dose response curve
    protected void fitLogEC50(int cpFlag){

        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    int plotFlag = 0;
        boolean constrained = false;
	    switch(cpFlag){
	        case 0: this.lastMethod= 40;
	                plotFlag = 0;
	                break;
	        case 1: this.lastMethod= 40;
	                plotFlag = 1;
	                break;
	        case 2: this.lastMethod= 42;
	                plotFlag = 0;
	                constrained = true;
	                break;
	        case 3: this.lastMethod= 42;
	                plotFlag = 1;
	                constrained = true;
	                break;
	    }
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=4;
	    this.scaleFlag = false;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // Estimate of bottom and top
	    double bottom = MathFns.minimum(this.yData);
	    double top = MathFns.maximum(this.yData);

	    // Estimate of LogEC50
	    int dirFlag = 1;
	    double yyymid = (top - bottom)/2.0D;
	    double yyxmidl = xData[0][0];
	    int ii = 1;
	    int nLen = this.yData.length;
	    boolean test = true;
	    while(test){
	        if(this.yData[ii]>=dirFlag*yyymid){
	            yyxmidl = xData[0][ii];
	            test = false;
	        }
	        else{
	            ii++;
	            if(ii>=nLen){
	                yyxmidl = StatFns.mean(this.xData[0]);
	                ii=nLen-1;
                    test = false;
                }
	        }
	    }
	    double yyxmidh = xData[0][nLen-1];
	    int jj = nLen-1;
	    test = true;
	    while(test){
	        if(this.yData[jj]<=dirFlag*yyymid){
	            yyxmidh = xData[0][jj];
	            test = false;
	        }
	        else{
	            jj--;
	            if(jj<0){
	                yyxmidh = StatFns.mean(this.xData[0]);
	                jj=1;
                    test = false;
                }
	        }
	    }
	    int thetaPos = (ii+jj)/2;
        double logEC50 = xData[0][thetaPos];

	    // estimate of slope
	    double thetaSlope1 = 2.0D*(yData[nLen-1] - logEC50)/(xData[0][nLen-1] - xData[0][thetaPos]);
	    double thetaSlope2 = 2.0D*logEC50/(xData[0][thetaPos] - xData[0][nLen-1]);
	    double hillSlope = Math.max(thetaSlope1, thetaSlope2);

        // initial estimates
        double[] start = new double[N_Params];
        start[0] = bottom;
        start[1] = top;
        start[2] = logEC50;
        start[3] = hillSlope;

        // initial step sizes
        double[] step = new double[N_Params];
        for(int i=0; i<N_Params; i++)step[i] = 0.1*start[i];
        if(step[0]==0.0D)step[0] = 0.1*(yData[nLen-1] - yData[0]);
        if(step[1]==0.0D)step[1] = 0.1*(yData[nLen-1] - yData[0]) + yData[nLen-1];
        if(step[2]==0.0D)step[2] = 0.05*(xData[0][nLen-1] - xData[0][0]);
        if(step[3]==0.0D)step[3] = 0.1*(xData[0][nLen-1] - xData[0][0])/(yData[nLen-1] - yData[0]);

        // Constrained option
        if(constrained)this.addConstraint(0, -1, 0.0D);

        // Nelder and Mead Simplex Regression
        LogEC50Function f = new LogEC50Function();
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(f);
        this.nelderMead(vec, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }
    }

	// method for fitting data to a rectangular hyberbola
    public void rectangularHyperbola(){
        fitRectangularHyperbola(0);
    }

    // method for fitting data to a rectangular hyberbola with plot and print out
    public void rectangularHyperbolaPlot(){
        fitRectangularHyperbola(1);
    }

    // method for fitting data to a rectangular hyperbola
    protected void fitRectangularHyperbola(int plotFlag){

        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=26;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=2;
	    if(!this.scaleFlag)this.N_Params=1;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // Estimate  of theta
	    double yymin = MathFns.minimum(this.yData);
	    double yymax = MathFns.maximum(this.yData);
	    int dirFlag = 1;
	    if(yymin<0)dirFlag=-1;
	    double yyymid = (yymax - yymin)/2.0D;
	    double yyxmidl = xData[0][0];
	    int ii = 1;
	    int nLen = this.yData.length;
	    boolean test = true;
	    while(test){
	        if(this.yData[ii]>=dirFlag*yyymid){
	            yyxmidl = xData[0][ii];
	            test = false;
	        }
	        else{
	            ii++;
	            if(ii>=nLen){
	                yyxmidl = StatFns.mean(this.xData[0]);
	                ii=nLen-1;
                    test = false;
                }
	        }
	    }
	    double yyxmidh = xData[0][nLen-1];
	    int jj = nLen-1;
	    test = true;
	    while(test){
	        if(this.yData[jj]<=dirFlag*yyymid){
	            yyxmidh = xData[0][jj];
	            test = false;
	        }
	        else{
	            jj--;
	            if(jj<0){
	                yyxmidh = StatFns.mean(this.xData[0]);
	                jj=1;
                    test = false;
                }
	        }
	    }
	    int thetaPos = (ii+jj)/2;
	    double theta0 = xData[0][thetaPos];

        // initial estimates
        double[] start = new double[N_Params];
        start[0] = theta0;
        if(this.scaleFlag){
            if(dirFlag==1){
                start[1] = yymax;
            }
            else{
                start[1] = yymin;
            }
        }

        // initial step sizes
        double[] step = new double[N_Params];
        for(int i=0; i<N_Params; i++)step[i] = 0.1*start[i];
        if(step[0]==0.0D)step[0] = (xData[0][nLen-1] - xData[0][0])/20.0D;
        if(this.scaleFlag)if(step[1]==0.0D)step[1] = 0.1*(yData[nLen-1] - yData[0]);

        // Nelder and Mead Simplex Regression
        RectangularHyperbolaFunction f = new RectangularHyperbolaFunction();
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(f);
        this.nelderMead(vec, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }
    }

// method for fitting data to a scaled Heaviside Step Function
    public void stepFunction(){
        fitStepFunction(0);
    }

    // method for fitting data to a scaled Heaviside Step Function with plot and print out
    public void stepFunctionPlot(){
        fitStepFunction(1);
    }

    // method for fitting data to a scaled Heaviside Step Function
    protected void fitStepFunction(int plotFlag){

        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=27;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=2;
	    if(!this.scaleFlag)this.N_Params=1;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

	    // Estimate  of theta
	    double yymin = MathFns.minimum(this.yData);
	    double yymax = MathFns.maximum(this.yData);
	    int dirFlag = 1;
	    if(yymin<0)dirFlag=-1;
	    double yyymid = (yymax - yymin)/2.0D;
	    double yyxmidl = xData[0][0];
	    int ii = 1;
	    int nLen = this.yData.length;
	    boolean test = true;
	    while(test){
	        if(this.yData[ii]>=dirFlag*yyymid){
	            yyxmidl = xData[0][ii];
	            test = false;
	        }
	        else{
	            ii++;
	            if(ii>=nLen){
	                yyxmidl = StatFns.mean(this.xData[0]);
	                ii=nLen-1;
                    test = false;
                }
	        }
	    }
	    double yyxmidh = xData[0][nLen-1];
	    int jj = nLen-1;
	    test = true;
	    while(test){
	        if(this.yData[jj]<=dirFlag*yyymid){
	            yyxmidh = xData[0][jj];
	            test = false;
	        }
	        else{
	            jj--;
	            if(jj<0){
	                yyxmidh = StatFns.mean(this.xData[0]);
	                jj=1;
                    test = false;
                }
	        }
	    }
	    int thetaPos = (ii+jj)/2;
	    double theta0 = xData[0][thetaPos];

        // initial estimates
        double[] start = new double[N_Params];
        start[0] = theta0;
        if(this.scaleFlag){
            if(dirFlag==1){
                start[1] = yymax;
            }
            else{
                start[1] = yymin;
            }
        }

        // initial step sizes
        double[] step = new double[N_Params];
        for(int i=0; i<N_Params; i++)step[i] = 0.1*start[i];
        if(step[0]==0.0D)step[0] = (xData[0][nLen-1] - xData[0][0])/20.0D;
        if(this.scaleFlag)if(step[1]==0.0D)step[1] = 0.1*(yData[nLen-1] - yData[0]);

        // Nelder and Mead Simplex Regression
        StepFunctionFunction f = new StepFunctionFunction();
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec = new Vector<Object>();
        vec.addElement(f);
        this.nelderMead(vec, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }
    }


    	// Fit to a Logistic
	public void logistic(){
	    this.fitLogistic(0);
	}

	// Fit to a Logistic
	public void logisticPlot(){

	    this.fitLogistic(1);
	}

    // Fit data to a Logistic probability function
	protected void fitLogistic(int plotFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=30;
	    this.linNonLin = false;
	    this.zeroCheck = false;
	    this.N_Params=3;
	    if(!this.scaleFlag)this.N_Params=2;
	    this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitLogistic(): This implementation of the Logistic distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	                yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // Calculate  x value at peak y (estimate of the Logistic mean)
	    Vector<Object> ret1 = Regression.dataSign(yData);
	 	Integer tempi = null;
	    tempi = (Integer)ret1.elementAt(5);
	 	int peaki = tempi.intValue();
	    double mu = xData[0][peaki];

	    // Calculate an estimate of the beta
	    double beta = Math.sqrt(6.0D)*halfWidth(xData[0], yData)/Math.PI;

	    // Calculate estimate of y scale
	    tempd = (Double)ret1.elementAt(4);
	    double ym = tempd.doubleValue();
	    ym=ym*beta*Math.sqrt(2.0D*Math.PI);

        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        start[0] = mu;
        start[1] = beta;
        if(this.scaleFlag){
            start[2] = ym;
        }
        step[0] = 0.1D*beta;
        step[1] = 0.1D*start[1];
        if(step[1]==0.0D){
            Vector<Object> ret0 = Regression.dataSign(xData[0]);
	 	    Double tempdd = null;
	        tempdd = (Double)ret0.elementAt(2);
	 	    double xmax = tempdd.doubleValue();
	 	    if(xmax==0.0D){
	 	        tempdd = (Double)ret0.elementAt(0);
	 	        xmax = tempdd.doubleValue();
	 	    }
	        step[0]=xmax*0.1D;
	    }
        if(this.scaleFlag)step[2] = 0.1D*start[2];

	    // Nelder and Mead Simplex Regression
        LogisticFunction f = new LogisticFunction();
        this.addConstraint(1,-1,0.0D);
        f.scaleOption = this.scaleFlag;
        f.scaleFactor = this.yScaleFactor;
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(f);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(plotFlag==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(f);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }

	}

    public void beta(){
	    this.fitBeta(0, 0);
	}

	public void betaPlot(){
	    this.fitBeta(1, 0);
	}

	public void betaMinMax(){
	    this.fitBeta(0, 1);
	}

	public void betaMinMaxPlot(){
	    this.fitBeta(1, 1);
	}

    protected void fitBeta(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    switch(typeFlag){
    	    case 0: this.lastMethod=31;
	                this.N_Params=3;
	                break;
	        case 1: this.lastMethod=32;
	                this.N_Params=5;
	                break;
        }
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;

	    this.zeroCheck = false;
		this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitBeta(): This implementation of the Beta distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	            yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // check  x data
	    Vector<Object> retX = Regression.dataSign(xData[0]);
	 	Integer tempi = null;

        // Calculate  x value at peak y (estimate of the 'distribution mode')
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();
	    double distribMode = xData[0][peaki];

	    // minimum value
	    tempd = (Double)retX.elementAt(0);
	    double minX = tempd.doubleValue();
	    // maximum value
	    tempd = (Double)retX.elementAt(2);
	    double maxX = tempd.doubleValue();
	    // mean value
	    tempd = (Double)retX.elementAt(8);
	    double meanX = tempd.doubleValue();


	    // test that data is within range
	    if(typeFlag==0){
	        if(minX<0.0D){
	            System.out.println("Regression: beta: data points must be greater than or equal to 0");
	            System.out.println("method betaMinMax used in place of method beta");
	            typeFlag = 1;
	            this.lastMethod=32;
	            this.N_Params=5;
	        }
	        if(maxX>1.0D){
	            System.out.println("Regression: beta: data points must be less than or equal to 1");
	            System.out.println("method betaMinMax used in place of method beta");
	            typeFlag = 1;
	            this.lastMethod=32;
	            this.N_Params=5;
	        }
        }

	    // Calculate an estimate of the alpha, beta and scale factor
	    double dMode = distribMode;
	    double dMean = meanX;
	    if(typeFlag==1){
	        dMode = (distribMode - minX*0.9D)/(maxX*1.2D - minX*0.9D);
	        dMean = (meanX - minX*0.9D)/(maxX*1.2D - minX*0.9D);
        }
	    double alphaGuess = 2.0D*dMode*dMean/(dMode - dMean);
	    if(alphaGuess<1.3)alphaGuess = 1.6D;
	    double betaGuess = alphaGuess*(1.0D - dMean)/dMean;
	    if(betaGuess<=1.3)betaGuess = 1.6D;
	    double scaleGuess = 0.0D;
	    if(typeFlag==0){
	        scaleGuess = yPeak/StatFns.betaPDF(alphaGuess, betaGuess, distribMode);
	    }
	    else{
	        scaleGuess = yPeak/StatFns.betaPDF(minX, maxX, alphaGuess, betaGuess, distribMode);
        }
        if(scaleGuess<0)scaleGuess=1;


	    // Nelder and Mead Simplex Regression for Gumbel
        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        switch(typeFlag){
            case 0: start[0] = alphaGuess;          //alpha
                    start[1] = betaGuess;           //beta
                    if(this.scaleFlag){
                        start[2] = scaleGuess;      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    step[1] = 0.1D*start[1];
                    if(this.scaleFlag)step[2] = 0.1D*start[2];

	                // Add constraints
                    this.addConstraint(0,-1,1.0D);
                    this.addConstraint(1,-1,1.0D);
                    break;
            case 1: start[0] = alphaGuess;          //alpha
                    start[1] = betaGuess;           //beta
                    start[2] = 0.9D*minX;           // min
                    start[3] = 1.1D*maxX;           // max
                    if(this.scaleFlag){
                        start[4] = scaleGuess;      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    step[1] = 0.1D*start[1];
                    step[2] = 0.1D*start[2];
                    step[3] = 0.1D*start[3];
                    if(this.scaleFlag)step[4] = 0.1D*start[4];

	                // Add constraints
                    this.addConstraint(0,-1,1.0D);
                    this.addConstraint(1,-1,1.0D);
                    this.addConstraint(2,+1,minX);
                    this.addConstraint(3,-1,maxX);
                    break;

        }

        // Create instance of Beta function
        BetaFunction ff = new BetaFunction();

        // Set minimum maximum type option
        ff.typeFlag = typeFlag;

        // Set ordinate scaling option
        ff.scaleOption = this.scaleFlag;
        ff.scaleFactor = this.yScaleFactor;

        // Perform simplex regression
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(ff);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(allTest==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(ff);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }
	}

    public void gamma(){
	    this.fitGamma(0, 0);
	}

	public void gammaPlot(){
	    this.fitGamma(1, 0);
	}

	public void gammaStandard(){
	    this.fitGamma(0, 1);
	}

	public void gammaStandardPlot(){
	    this.fitGamma(1, 1);
	}

    protected void fitGamma(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    switch(typeFlag){
    	    case 0: this.lastMethod=33;
	                this.N_Params=4;
	                break;
	        case 1: this.lastMethod=34;
	                this.N_Params=2;
	                break;
        }
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;

	    this.zeroCheck = false;
		this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitGamma(): This implementation of the Gamma distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	            yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // check  x data
	    Vector<Object> retX = Regression.dataSign(xData[0]);
	 	Integer tempi = null;

        // Calculate  x value at peak y (estimate of the 'distribution mode')
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();
	    double distribMode = xData[0][peaki];

	    // minimum value
	    tempd = (Double)retX.elementAt(0);
	    double minX = tempd.doubleValue();
	    // maximum value
	    tempd = (Double)retX.elementAt(2);
	    double maxX = tempd.doubleValue();
	    // mean value
	    tempd = (Double)retX.elementAt(8);
	    double meanX = tempd.doubleValue();


	    // test that data is within range
	    if(typeFlag==1){
	        if(minX<0.0D){
	            System.out.println("Regression: gammaStandard: data points must be greater than or equal to 0");
	            System.out.println("method gamma used in place of method gammaStandard");
	            typeFlag = 0;
	            this.lastMethod=33;
	            this.N_Params=2;
	        }
        }

	    // Calculate an estimate of the mu, beta, gamma and scale factor
	    double muGuess = 0.8D*minX;
	    if(muGuess==0.0D)muGuess = -0.1D;
	    double betaGuess = meanX - distribMode;
	    if(betaGuess<=0.0D)betaGuess = 1.0D;
	    double gammaGuess = (meanX + muGuess)/betaGuess;
	    if(typeFlag==1)gammaGuess = meanX;
	    if(gammaGuess<=0.0D)gammaGuess = 1.0D;
	    double scaleGuess = 0.0D;
	    if(typeFlag==0){
	        scaleGuess = yPeak/StatFns.gammaPDF(muGuess, betaGuess, gammaGuess, distribMode);
	    }
	    else{
	        scaleGuess = yPeak/StatFns.gammaPDF(gammaGuess, distribMode);
        }
        if(scaleGuess<0)scaleGuess=1;


	    // Nelder and Mead Simplex Regression for Gamma
        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        switch(typeFlag){
            case 1: start[0] = gammaGuess;          //gamma
                    if(this.scaleFlag){
                        start[1] = scaleGuess;      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    if(this.scaleFlag)step[1] = 0.1D*start[1];

	                // Add constraints
                    this.addConstraint(0,-1,0.0D);
                    break;
            case 0: start[0] = muGuess;             // mu
                    start[1] = betaGuess;           // beta
                    start[2] = gammaGuess;          // gamma
                    if(this.scaleFlag){
                        start[3] = scaleGuess;      //y axis scaling factor
                    }
                    step[0] = 0.1D*start[0];
                    step[1] = 0.1D*start[1];
                    step[2] = 0.1D*start[2];
                    if(this.scaleFlag)step[3] = 0.1D*start[3];

	                // Add constraints
                    this.addConstraint(1,-1,0.0D);
                    this.addConstraint(2,-1,0.0D);
                    break;
        }

        // Create instance of Gamma function
        GammaFunction ff = new GammaFunction();

        // Set type option
        ff.typeFlag = typeFlag;

        // Set ordinate scaling option
        ff.scaleOption = this.scaleFlag;
        ff.scaleFactor = this.yScaleFactor;

        // Perform simplex regression
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(ff);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        if(allTest==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(ff);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }
	}

    // Fit to an Erlang Distribution
    public void erlang(){
	    this.fitErlang(0, 0);
	}

	public void erlangPlot(){
	    this.fitErlang(1, 0);
	}

    protected void fitErlang(int allTest, int typeFlag){
        if(this.multipleY)throw new IllegalArgumentException("This method cannot handle multiply dimensioned y arrays");
	    this.lastMethod=35;
	    int nTerms0 = 2;    // number of erlang terms
	    int nTerms1 = 4;    // number of gamma terms - initial estimates procedure
	    this.N_Params = nTerms1;
	    if(!this.scaleFlag)this.N_Params=this.N_Params-1;

	    this.zeroCheck = false;
		this.degreesOfFreedom=this.N_YData - this.N_Params;
	    if(this.degreesOfFreedom<1)throw new IllegalArgumentException("Degrees of freedom must be greater than 0");

	    // order data into ascending order of the abscissae
        Regression.sort(this.xData[0], this.yData, this.weight);

        // check sign of y data
	    Double tempd=null;
	    Vector<Object> retY = Regression.dataSign(yData);
	    tempd = (Double)retY.elementAt(4);
	    double yPeak = tempd.doubleValue();
	    boolean yFlag = false;
	    if(yPeak<0.0D){
	        System.out.println("Regression.fitGamma(): This implementation of the Gamma distribution takes only positive y values\n(noise taking low values below zero are allowed)");
	        System.out.println("All y values have been multiplied by -1 before fitting");
	        for(int i =0; i<this.N_YData; i++){
	            yData[i] = -yData[i];
	        }
	        retY = Regression.dataSign(yData);
	        yFlag=true;
	    }

	    // check  x data
	    Vector<Object> retX = Regression.dataSign(xData[0]);
	 	Integer tempi = null;

        // Calculate  x value at peak y (estimate of the 'distribution mode')
	    tempi = (Integer)retY.elementAt(5);
	 	int peaki = tempi.intValue();
	    double distribMode = xData[0][peaki];

	    // minimum value
	    tempd = (Double)retX.elementAt(0);
	    double minX = tempd.doubleValue();
	    // maximum value
	    tempd = (Double)retX.elementAt(2);
	    double maxX = tempd.doubleValue();
	    // mean value
	    tempd = (Double)retX.elementAt(8);
	    double meanX = tempd.doubleValue();


	    // test that data is within range
        if(minX<0.0D)throw new IllegalArgumentException("data points must be greater than or equal to 0");

        // FIT TO GAMMA DISTRIBUTION TO OBTAIN INITIAL ESTIMATES
	    // Calculate an estimate of the mu, beta, gamma and scale factor
	    double muGuess = 0.8D*minX;
	    if(muGuess==0.0D)muGuess = -0.1D;
	    double betaGuess = meanX - distribMode;
	    if(betaGuess<=0.0D)betaGuess = 1.0D;
	    double gammaGuess = (meanX + muGuess)/betaGuess;
	    if(typeFlag==1)gammaGuess = meanX;
	    if(gammaGuess<=0.0D)gammaGuess = 1.0D;
	    double scaleGuess = 0.0D;
        scaleGuess = yPeak/StatFns.gammaPDF(muGuess, betaGuess, gammaGuess, distribMode);
        if(scaleGuess<0)scaleGuess=1;


	    // Nelder and Mead Simplex Regression for Gamma
        // Fill arrays needed by the Simplex
        double[] start = new double[this.N_Params];
        double[] step = new double[this.N_Params];
        start[0] = muGuess;             // mu
        start[1] = betaGuess;           // beta
        start[2] = gammaGuess;          // gamma
        if(this.scaleFlag)start[3] = scaleGuess;      //y axis scaling factor

        step[0] = 0.1D*start[0];
        step[1] = 0.1D*start[1];
        step[2] = 0.1D*start[2];
        if(this.scaleFlag)step[3] = 0.1D*start[3];

	    // Add constraints
        this.addConstraint(1,-1,0.0D);
        this.addConstraint(2,-1,0.0D);

        // Create instance of Gamma function
        GammaFunction ff = new GammaFunction();

        // Set type option
        ff.typeFlag = typeFlag;

        // Set ordinate scaling option
        ff.scaleOption = this.scaleFlag;
        ff.scaleFactor = this.yScaleFactor;

        // Perform simplex regression
        Vector<Object> vec2 = new Vector<Object>();
        vec2.addElement(ff);
        this.nelderMead(vec2, start, step, this.fTol, this.nMax);

        // FIT TO ERLANG DISTRIBUTION USING GAMMA BEST ESTIMATES AS INITIAL ESTIMATES
        // AND VARYING RATE PARAMETER BY UNIT STEPS
        this.removeConstraints();

        // Initial estimates
        double[] bestGammaEst = this.getCoeff();

        // Swap from Gamma dimensions to Erlang dimensions
        this.N_Params = nTerms0;
        start = new double[this.N_Params];
        step = new double[this.N_Params];

        // initial estimates
        start[0] = 1.0D/bestGammaEst[1];                    // lambda
        if(this.scaleFlag)start[1] = bestGammaEst[3];       //y axis scaling factor

        step[0] = 0.1D*start[0];
        if(this.scaleFlag)step[1] = 0.1D*start[1];

	    // Add constraints
        this.addConstraint(0,-1,0.0D);

        // fix initial integer rate parameter
        double kay0 = Math.round(bestGammaEst[2]);
        double kay = kay0;

        // Create instance of Erlang function
        ErlangFunction ef = new ErlangFunction();

        // Set ordinate scaling option
        ef.scaleOption = this.scaleFlag;
        ef.scaleFactor = this.yScaleFactor;
        ef.kay = kay;

        // Fit stepping up
        boolean testKay = true;
        double ssMin = Double.NaN;
        double upSS = Double.NaN;
        double upKay = Double.NaN;
        double kayFinal = Double.NaN;
        int iStart = 1;

        while(testKay){

            // Perform simplex regression
            Vector<Object> vec3 = new Vector<Object>();
            vec3.addElement(ef);
            this.nelderMead(vec3, start, step, this.fTol, this.nMax);
            double sumOfSquares = this.getSumOfSquares();
            if(iStart==1){
                iStart = 2;
                ssMin = sumOfSquares;
                kay = kay + 1;
                start[0] = 1.0D/bestGammaEst[1];                    // lambda
                if(this.scaleFlag)start[1] = bestGammaEst[3];       //y axis scaling factor
                step[0] = 0.1D*start[0];
                if(this.scaleFlag)step[1] = 0.1D*start[1];
                this.addConstraint(0,-1,0.0D);
                ef.kay = kay;
            }
            else{
                if(sumOfSquares<ssMin){
                    ssMin = sumOfSquares;
                    kay = kay + 1;
                    start[0] = 1.0D/bestGammaEst[1];                    // lambda
                    if(this.scaleFlag)start[1] = bestGammaEst[3];       //y axis scaling factor
                    step[0] = 0.1D*start[0];
                    if(this.scaleFlag)step[1] = 0.1D*start[1];
                    this.addConstraint(0,-1,0.0D);
                    ef.kay = kay;
                }
                else{
                    upSS = ssMin;
                    upKay = kay - 1;
                    testKay = false;
                }
            }
        }

        if(kay0==1){
            kayFinal = upKay;
        }
        else{

            // Fit stepping down
            iStart = 1;
            testKay = true;
            ssMin = Double.NaN;
            double downSS = Double.NaN;
            double downKay = Double.NaN;
            // initial estimates
            start[0] = 1.0D/bestGammaEst[1];                    // lambda
            if(this.scaleFlag)start[1] = bestGammaEst[3];       //y axis scaling factor
            step[0] = 0.1D*start[0];
            if(this.scaleFlag)step[1] = 0.1D*start[1];
	        // Add constraints
            this.addConstraint(0,-1,0.0D);
            kay = kay0;
            ef.kay = kay;

            while(testKay){

                // Perform simplex regression
                Vector<Object> vec3 = new Vector<Object>();
                vec3.addElement(ef);
                this.nelderMead(vec3, start, step, this.fTol, this.nMax);
                double sumOfSquares = this.getSumOfSquares();
                if(iStart==1){
                    iStart = 2;
                    ssMin = sumOfSquares;
                    kay = kay - 1;
                    if(Math.rint(kay)<1L){
                        downSS = ssMin;
                        downKay = kay + 1;
                        testKay = false;
                    }
                    else{
                        start[0] = 1.0D/bestGammaEst[1];                    // lambda
                        if(this.scaleFlag)start[1] = bestGammaEst[3];       //y axis scaling factor
                        step[0] = 0.1D*start[0];
                        if(this.scaleFlag)step[1] = 0.1D*start[1];
                        this.addConstraint(0,-1,0.0D);
                        ef.kay = kay;
                    }
                }
                else{
                    if(sumOfSquares<ssMin){
                        ssMin = sumOfSquares;
                        kay = kay - 1;
                        if(Math.rint(kay)<1L){
                            downSS = ssMin;
                            downKay = kay + 1;
                            testKay = false;
                        }
                        else{
                            start[0] = 1.0D/bestGammaEst[1];                    // lambda
                            if(this.scaleFlag)start[1] = bestGammaEst[3];       //y axis scaling factor
                            step[0] = 0.1D*start[0];
                            if(this.scaleFlag)step[1] = 0.1D*start[1];
                            this.addConstraint(0,-1,0.0D);
                            ef.kay = kay;
                        }
                    }
                    else{
                        downSS = ssMin;
                        downKay = kay + 1;
                        testKay = false;
                    }
                }
            }
            if(downSS<upSS){
                kayFinal = downKay;
            }
            else{
                kayFinal = upKay;
            }
        }

        // Final fit
        // initial estimates
        start[0] = 1.0D/bestGammaEst[1];                    // lambda
        if(this.scaleFlag)start[1] = bestGammaEst[3];       //y axis scaling factor

        step[0] = 0.1D*start[0];
        if(this.scaleFlag)step[1] = 0.1D*start[1];

	    // Add constraints
        this.addConstraint(0,-1,0.0D);

        // Set function variables
        ef.scaleOption = this.scaleFlag;
        ef.scaleFactor = this.yScaleFactor;
        ef.kay = Math.round(kayFinal);
        this.kayValue = Math.round(kayFinal);

        // Perform final regression
        Vector<Object> vec3 = new Vector<Object>();
        vec3.addElement(ef);
        this.nelderMead(vec3, start, step, this.fTol, this.nMax);


        if(allTest==1){
            // Print results
            if(!this.supressPrint)this.print();

            // Plot results
            int flag = this.plotXY(ef);
            if(flag!=-2 && !this.supressYYplot)this.plotYY();
        }

        if(yFlag){
            // restore data
            for(int i=0; i<this.N_YData-1; i++){
                this.yData[i]=-this.yData[i];
            }
        }
	}

	// return Erlang rate parameter (k) value
	public double getKayValue(){
	    return this.kayValue;
	}


    // HISTOGRAM METHODS
        // Distribute data into bins to obtain histogram
        // zero bin position and upper limit provided
        public static double[][] histogramBins(double[] data, double binWidth, double binZero, double binUpper){
            int n = 0;              // new array length
            int m = data.length;    // old array length;
            for(int i=0; i<m; i++)if(data[i]<=binUpper)n++;
            if(n!=m){
                double[] newData = new double[n];
                int j = 0;
                for(int i=0; i<m; i++){
                    if(data[i]<=binUpper){
                        newData[j] = data[i];
                        j++;
                    }
                }
                System.out.println((m-n)+" data points, above histogram upper limit, excluded in StatFns.histogramBins");
                return histogramBins(newData, binWidth, binZero);
            }
            else{
                 return histogramBins(data, binWidth, binZero);

            }
        }

        // Distribute data into bins to obtain histogram
        // zero bin position provided
        public static double[][] histogramBins(double[] data, double binWidth, double binZero){
            double dmax = MathFns.maximum(data);
            int nBins = (int) Math.ceil((dmax - binZero)/binWidth);
            if(binZero+nBins*binWidth>dmax)nBins++;
            int nPoints = data.length;
            int[] dataCheck = new int[nPoints];
            for(int i=0; i<nPoints; i++)dataCheck[i]=0;
            double[]binWall = new double[nBins+1];
            binWall[0]=binZero;
            for(int i=1; i<=nBins; i++){
                binWall[i] = binWall[i-1] + binWidth;
            }
            double[][] binFreq = new double[2][nBins];
            for(int i=0; i<nBins; i++){
                binFreq[0][i]= (binWall[i]+binWall[i+1])/2.0D;
                binFreq[1][i]= 0.0D;
            }
            boolean test = true;

            for(int i=0; i<nPoints; i++){
                test=true;
                int j=0;
                while(test){
                    if(j==nBins-1){
                        if(data[i]>=binWall[j] && data[i]<=binWall[j+1]*(1.0D + Regression.histTol)){
                            binFreq[1][j]+= 1.0D;
                            dataCheck[i]=1;
                            test=false;
                        }
                    }
                    else{
                        if(data[i]>=binWall[j] && data[i]<binWall[j+1]){
                            binFreq[1][j]+= 1.0D;
                            dataCheck[i]=1;
                            test=false;
                        }
                    }
                    if(test){
                        if(j==nBins-1){
                            test=false;
                        }
                        else{
                            j++;
                        }
                    }
                }
            }
            int nMissed=0;
            for(int i=0; i<nPoints; i++)if(dataCheck[i]==0){
                nMissed++;
                System.out.println("p " + i + " " + data[i] + " " + binWall[0] + " " + binWall[nBins]);
            }
            if(nMissed>0)System.out.println(nMissed+" data points, outside histogram limits, excluded in StatFns.histogramBins");
            return binFreq;
        }

        // Distribute data into bins to obtain histogram
        // zero bin position calculated
        public static double[][] histogramBins(double[] data, double binWidth){

            double dmin = MathFns.minimum(data);
            double dmax = MathFns.maximum(data);
            double span = dmax - dmin;
            double binZero = dmin;
            int nBins = (int) Math.ceil(span/binWidth);
            double histoSpan = ((double)nBins)*binWidth;
            double rem = histoSpan - span;
            if(rem>=0){
                binZero -= rem/2.0D;
            }
            else{
                if(Math.abs(rem)/span>Regression.histTol){
                    // readjust binWidth
                    boolean testBw = true;
                    double incr = Regression.histTol/nBins;
                    int iTest = 0;
                    while(testBw){
                       binWidth += incr;
                       histoSpan = ((double)nBins)*binWidth;
                        rem = histoSpan - span;
                        if(rem<0){
                            iTest++;
                            if(iTest>1000){
                                testBw = false;
                                System.out.println("histogram method could not encompass all data within histogram\nContact Michael thomas Flanagan");
                            }
                        }
                        else{
                            testBw = false;
                        }
                    }
                }
            }

            return Regression.histogramBins(data, binWidth, binZero);
        }


}

//  CLASSES TO EVALUATE THE SPECIAL FUNCTIONS


// Class to evaluate the Gausian (normal) function y = (yscale/sd.sqrt(2.pi)).exp(-0.5[(x - xmean)/sd]^2).
class GaussianFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double y = (yScale/(p[1]*Math.sqrt(2.0D*Math.PI)))*Math.exp(-0.5D*MathFns.square((x[0]-p[0])/p[1]));
        return y;
    }
}

// Class to evaluate the Gausian (normal) function y = (yscale/sd.sqrt(2.pi)).exp(-0.5[(x - xmean)/sd]^2).
// Some parameters may be fixed
class GaussianFunctionFixed implements RegressionFunction{

    public double[] param = new double[3];
    public boolean[] fixed = new boolean[3];

    public double function(double[] p, double[] x){

        int ii = 0;
        for(int i=0; i<3; i++){
            if(!fixed[i]){
                param[i] = p[ii];
                ii++;
            }
        }

        double y = (param[2]/(param[1]*Math.sqrt(2.0D*Math.PI)))*Math.exp(-0.5D*MathFns.square((x[0]-param[0])/param[1]));
        return y;
    }
}

// Class to evaluate the two parameter log-normal function y = (yscale/x.sigma.sqrt(2.pi)).exp(-0.5[(log(x) - mu)/sd]^2).
class LogNormalTwoParFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double y = (yScale/(x[0]*p[1]*Math.sqrt(2.0D*Math.PI)))*Math.exp(-0.5D*MathFns.square((Math.log(x[0])-p[0])/p[1]));
        return y;
    }
}

// Class to evaluate the three parameter log-normal function y = (yscale/(x-alpha).beta.sqrt(2.pi)).exp(-0.5[(log((x-alpha)/gamma)/sd]^2).
class LogNormalThreeParFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double y = (yScale/((x[0]-p[0])*p[1]*Math.sqrt(2.0D*Math.PI)))*Math.exp(-0.5D*MathFns.square(Math.log((x[0]-p[0])/p[2])/p[1]));
        return y;
    }
}


// Class to evaluate the Lorentzian function
// y = (yscale/pi).(gamma/2)/((x - mu)^2+(gamma/2)^2).
class LorentzianFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;

    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double y = (yScale/Math.PI)*(p[1]/2.0D)/(MathFns.square(x[0]-p[0])+MathFns.square(p[1]/2.0D));
        return y;
    }
}

// Class to evaluate the Poisson function
// y = yscale.(mu^k).exp(-mu)/k!.
class PoissonFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;

    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[1];
        double y = yScale*Math.pow(p[0],x[0])*Math.exp(-p[0])/StatFns.factorial(x[0]);
        return y;
    }
}

// Class to evaluate the Gumbel function
class GumbelFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public int typeFlag = 0; // set to 0 -> Minimum Mode Gumbel
                            // reset to 1 -> Maximum Mode Gumbel
                            // reset to 2 -> one parameter Minimum Mode Gumbel
                            // reset to 3 -> one parameter Maximum Mode Gumbel
                            // reset to 4 -> standard Minimum Mode Gumbel
                            // reset to 5 -> standard Maximum Mode Gumbel

    public double function(double[] p, double[] x){
        double y=0.0D;
        double arg=0.0D;
        double yScale = scaleFactor;

        switch(this.typeFlag){
            case 0:
            // y = yscale*(1/gamma)*exp((x-mu)/gamma)*exp(-exp((x-mu)/gamma))
                arg = (x[0]-p[0])/p[1];
                if(scaleOption)yScale = p[2];
                y = (yScale/p[1])*Math.exp(arg)*Math.exp(-(Math.exp(arg)));
                break;
            case 1:
            // y = yscale*(1/gamma)*exp((mu-x)/gamma)*exp(-exp((mu-x)/gamma))
                arg = (p[0]-x[0])/p[1];
                if(scaleOption)yScale = p[2];
                y = (yScale/p[1])*Math.exp(arg)*Math.exp(-(Math.exp(arg)));
                break;
             case 2:
            // y = yscale*(1/gamma)*exp((x)/gamma)*exp(-exp((x)/gamma))
                arg = x[0]/p[0];
                if(scaleOption)yScale = p[1];
                y = (yScale/p[0])*Math.exp(arg)*Math.exp(-(Math.exp(arg)));
                break;
            case 3:
            // y = yscale*(1/gamma)*exp((-x)/gamma)*exp(-exp((-x)/gamma))
                arg = -x[0]/p[0];
                if(scaleOption)yScale = p[1];
                y = (yScale/p[0])*Math.exp(arg)*Math.exp(-(Math.exp(arg)));
                break;
            case 4:
            // y = yscale*exp(x)*exp(-exp(x))
                if(scaleOption)yScale = p[0];
                y = yScale*Math.exp(x[0])*Math.exp(-(Math.exp(x[0])));
                break;
            case 5:
            // y = yscale*exp(-x)*exp(-exp(-x))
                if(scaleOption)yScale = p[0];
                y = yScale*Math.exp(-x[0])*Math.exp(-(Math.exp(-x[0])));
                break;
        }
        return y;
    }
}

// Class to evaluate the Frechet function
// y = yscale.(gamma/sigma)*((x - mu)/sigma)^(-gamma-1)*exp(-((x-mu)/sigma)^-gamma
class FrechetFunctionOne implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public int typeFlag = 0; // set to 0 -> Three Parameter Frechet
                            // reset to 1 -> Two Parameter Frechet
                            // reset to 2 -> Standard Frechet

    public double function(double[] p, double[] x){
        double y = 0.0D;
        boolean test = false;
        double yScale = scaleFactor;

        switch(typeFlag){
            case 0: if(x[0]>=p[0]){
                        double arg = (x[0] - p[0])/p[1];
                        if(scaleOption)yScale = p[3];
                        y = yScale*(p[2]/p[1])*Math.pow(arg,-p[2]-1.0D)*Math.exp(-Math.pow(arg,-p[2]));
                    }
                    break;
            case 1: if(x[0]>=0.0D){
                        double arg = x[0]/p[0];
                        if(scaleOption)yScale = p[2];
                        y = yScale*(p[1]/p[0])*Math.pow(arg,-p[1]-1.0D)*Math.exp(-Math.pow(arg,-p[1]));
                    }
                    break;
            case 2: if(x[0]>=0.0D){
                        double arg = x[0];
                        if(scaleOption)yScale = p[1];
                        y = yScale*p[0]*Math.pow(arg,-p[0]-1.0D)*Math.exp(-Math.pow(arg,-p[0]));
                    }
                    break;
        }
        return y;
    }
}

// Class to evaluate the semi-linearised Frechet function
// log(log(1/(1-Cumulative y) = gamma*log((x-mu)/sigma)
class FrechetFunctionTwo implements RegressionFunction{

    public int typeFlag = 0; // set to 0 -> Three Parameter Frechet
                            // reset to 1 -> Two Parameter Frechet
                            // reset to 2 -> Standard Frechet

    public double function(double[] p, double[] x){
        double y=0.0D;
        switch(typeFlag){
            case 0: y = -p[2]*Math.log(Math.abs(x[0]-p[0])/p[1]);
                    break;
            case 1: y = -p[1]*Math.log(Math.abs(x[0])/p[0]);
                    break;
            case 2: y = -p[0]*Math.log(Math.abs(x[0]));
                    break;
        }

        return y;
    }
}

// Class to evaluate the Weibull function
// y = yscale.(gamma/sigma)*((x - mu)/sigma)^(gamma-1)*exp(-((x-mu)/sigma)^gamma
class WeibullFunctionOne implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public int typeFlag = 0; // set to 0 -> Three Parameter Weibull
                            // reset to 1 -> Two Parameter Weibull
                            // reset to 2 -> Standard Weibull

    public double function(double[] p, double[] x){
        double y = 0.0D;
        boolean test = false;
        double yScale = scaleFactor;

        switch(typeFlag){
            case 0: if(x[0]>=p[0]){
                        double arg = (x[0] - p[0])/p[1];
                        if(scaleOption)yScale = p[3];
                        y = yScale*(p[2]/p[1])*Math.pow(arg,p[2]-1.0D)*Math.exp(-Math.pow(arg,p[2]));
                    }
                    break;
            case 1: if(x[0]>=0.0D){
                        double arg = x[0]/p[0];
                        if(scaleOption)yScale = p[2];
                        y = yScale*(p[1]/p[0])*Math.pow(arg,p[1]-1.0D)*Math.exp(-Math.pow(arg,p[1]));
                    }
                    break;
            case 2: if(x[0]>=0.0D){
                        double arg = x[0];
                        if(scaleOption)yScale = p[1];
                        y = yScale*p[0]*Math.pow(arg,p[0]-1.0D)*Math.exp(-Math.pow(arg,p[0]));
                    }
                    break;
        }
        return y;
    }
}

// Class to evaluate the semi-linearised Weibull function
// log(log(1/(1-Cumulative y) = gamma*log((x-mu)/sigma)
class WeibullFunctionTwo implements RegressionFunction{

    public int typeFlag = 0; // set to 0 -> Three Parameter Weibull
                            // reset to 1 -> Two Parameter Weibull
                            // reset to 2 -> Standard Weibull

    public double function(double[] p, double[] x){
        double y=0.0D;
        switch(typeFlag){
            case 0: y = p[2]*Math.log(Math.abs(x[0]-p[0])/p[1]);
                    break;
            case 1: y = p[1]*Math.log(Math.abs(x[0])/p[0]);
                    break;
            case 2: y = p[0]*Math.log(Math.abs(x[0]));
            break;
        }

        return y;
    }
}

// Class to evaluate the Rayleigh function
// y = (yscale/sigma)*(x/sigma)*exp(-0.5((x-mu)/sigma)^2
class RayleighFunctionOne implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;

    public double function(double[] p, double[] x){
        double y = 0.0D;
        boolean test = false;
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[1];
        if(x[0]>=0.0D){
            double arg = x[0]/p[0];
            y = (yScale/p[0])*arg*Math.exp(-0.5D*Math.pow(arg,2));
        }
        return y;
    }
}


// Class to evaluate the semi-linearised Rayleigh function
// log(1/(1-Cumulative y) = 0.5*(x/sigma)^2
class RayleighFunctionTwo implements RegressionFunction{

    public double function(double[] p, double[] x){
        double y = 0.5D*Math.pow(x[0]/p[0],2);
        return y;
    }
}

class ExponentialFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public int typeFlag = 0; // set to 0 -> Two Parameter Exponential
                            // reset to 1 -> One Parameter Exponential
                            // reset to 2 -> Standard Exponential

    public double function(double[] p, double[] x){
        double y = 0.0D;
        boolean test = false;
        double yScale = scaleFactor;

        switch(typeFlag){
            case 0: if(x[0]>=p[0]){
                        if(scaleOption)yScale = p[2];
                        double arg = (x[0] - p[0])/p[1];
                        y = (yScale/p[1])*Math.exp(-arg);
                    }
                    break;
            case 1: if(x[0]>=0.0D){
                        double arg = x[0]/p[0];
                        if(scaleOption)yScale = p[1];
                        y = (yScale/p[0])*Math.exp(-arg);
                    }
                    break;
            case 2: if(x[0]>=0.0D){
                        double arg = x[0];
                        if(scaleOption)yScale = p[0];
                        y = yScale*Math.exp(-arg);
                    }
                    break;
        }
        return y;
    }
}

// class to evaluate a Pareto scaled pdf
class ParetoFunctionOne implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public int typeFlag = 0;    // set to 3 -> Three Parameter Pareto
                                // set to 2 -> Two Parameter Pareto
                                // set to 1 -> One Parameter Pareto

    public double function(double[] p, double[] x){
        double y = 0.0D;
        boolean test = false;
        double yScale = scaleFactor;

        switch(typeFlag){
            case 3: if(x[0]>=p[1]+p[2]){
                        if(scaleOption)yScale = p[3];
                        y = yScale*p[0]*Math.pow(p[1],p[0])/Math.pow((x[0]-p[2]),p[0]+1.0D);
                    }
                    break;
            case 2: if(x[0]>=p[1]){
                        if(scaleOption)yScale = p[2];
                        y = yScale*p[0]*Math.pow(p[1],p[0])/Math.pow(x[0],p[0]+1.0D);
                    }
                    break;
            case 1: if(x[0]>=1.0D){
                        double arg = x[0]/p[0];
                        if(scaleOption)yScale = p[1];
                        y = yScale*p[0]/Math.pow(x[0],p[0]+1.0D);
                    }
                    break;
        }
        return y;
    }
}

// class to evaluate a Pareto cdf
class ParetoFunctionTwo implements RegressionFunction{

    public int typeFlag = 0;    // set to 3 -> Three Parameter Pareto
                                // set to 2 -> Two Parameter Pareto
                                // set to 1 -> One Parameter Pareto

    public double function(double[] p, double[] x){
        double y = 0.0D;
        switch(typeFlag){
            case 3: if(x[0]>=p[1]+p[2]){
                        y = 1.0D - Math.pow(p[1]/(x[0]-p[2]),p[0]);
                    }
                    break;
            case 2: if(x[0]>=p[1]){
                        y = 1.0D - Math.pow(p[1]/x[0],p[0]);
                    }
                    break;
            case 1: if(x[0]>=1.0D){
                        y = 1.0D - Math.pow(1.0D/x[0],p[0]);
                    }
                    break;
         }
        return y;
    }
}

// class to evaluate a Sigmoidal threshold function
class SigmoidThresholdFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;

    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double y = yScale/(1.0D + Math.exp(-p[0]*(x[0] - p[1])));
        return y;
     }
}

// class to evaluate a Rectangular Hyberbola
class RectangularHyperbolaFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;

    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double y = yScale*x[0]/(p[0] + x[0]);
        return y;
     }

}

// class to evaluate a scaled Heaviside Step Function
class StepFunctionFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;

    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[1];
        double y = 0.0D;
        if(x[0]>p[0])y = yScale;
        return y;
     }
}

// class to evaluate a Hill or Sips sigmoidal function
class SigmoidHillSipsFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;

    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double xterm = Math.pow(x[0],p[1]);
        double y = yScale*xterm/(Math.pow(p[0], p[1]) + xterm);
        return y;
     }
}

// Class to evaluate the Logistic probability function y = yscale*exp(-(x-mu)/beta)/(beta*(1 + exp(-(x-mu)/beta))^2.
class LogisticFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public double function(double[] p, double[] x){
        double yScale = scaleFactor;
        if(scaleOption)yScale = p[2];
        double y = yScale*MathFns.square(MathFns.sech((x[0] - p[0])/(2.0D*p[1])))/(4.0D*p[1]);
        return y;
    }
}

// class to evaluate a Beta scaled pdf
class BetaFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public int typeFlag = 0;    // set to 0 -> Beta Distibution - [0, 1] interval
                                // set to 1 -> Beta Distibution - [min, max] interval

    public double function(double[] p, double[] x){
        double y = 0.0D;
        boolean test = false;
        double yScale = scaleFactor;

        switch(typeFlag){
            case 0: if(scaleOption)yScale = p[2];
                    y = yScale*Math.pow(x[0],p[0]-1.0D)*Math.pow(1.0D-x[0],p[1]-1.0D)/StatFns.betaFunction(p[0],p[1]);
                    break;
            case 1: if(scaleOption)yScale = p[4];
                    y = yScale*Math.pow(x[0]-p[2],p[0]-1.0D)*Math.pow(p[3]-x[0],p[1]-1.0D)/StatFns.betaFunction(p[0],p[1]);
                    y = y/Math.pow(p[3]-p[2],p[0]+p[1]-1.0D);
                    break;
        }
        return y;
    }
}

// class to evaluate a Gamma scaled pdf
class GammaFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public int typeFlag = 0;    // set to 0 -> Three parameter Gamma Distribution
                                // set to 1 -> Standard Gamma Distribution

    public double function(double[] p, double[] x){
        double y = 0.0D;
        boolean test = false;
        double yScale = scaleFactor;

        switch(typeFlag){
            case 0: if(scaleOption)yScale = p[3];
                    double xTerm = (x[0] - p[0])/p[1];
                    y = yScale*Math.pow(xTerm,p[2]-1.0D)*Math.exp(-xTerm)/(p[1]*StatFns.gammaFunction(p[2]));
                    break;
            case 1: if(scaleOption)yScale = p[1];
                    y = yScale*Math.pow(x[0],p[0]-1.0D)*Math.exp(-x[0])/StatFns.gammaFunction(p[0]);
                    break;
        }
        return y;
    }
}

// class to evaluate a Erlang scaled pdf
// rate parameter is fixed
class ErlangFunction implements RegressionFunction{
    public boolean scaleOption = true;
    public double scaleFactor = 1.0D;
    public double kay = 1.0D;   // rate parameter

    public double function(double[] p, double[] x){
        boolean test = false;
        double yScale = scaleFactor;

        if(scaleOption)yScale = p[1];

        double y = kay*Math.log(p[0]) + (kay - 1)*Math.log(x[0]) - x[0]*p[0] - MathFns.logFactorial(kay - 1);
        y = yScale*Math.exp(y);

        return y;
    }
}

// class to evaluate a EC50 function
class EC50Function implements RegressionFunction{

    public double function(double[] p, double[] x){
        double y = p[0] + (p[1] - p[0])/(1.0D + Math.pow(x[0]/p[2], p[3]));
        return y;
     }
}

// class to evaluate a LogEC50 function
class LogEC50Function implements RegressionFunction{

    public double function(double[] p, double[] x){
        double y = p[0] + (p[1] - p[0])/(1.0D + Math.pow(10.0D, (p[2] - x[0])*p[3]));
        return y;
     }
}

