package weka.filters.timeseries;
/* Performs a FFT of the data set. NOTE:
      * copyright: Anthony Bagnall

 * 1. truncates or pads with the mean the each series (i.e. each Instance) so length is power of 2 by flag pad (default true)
 * 2. By default, stores the complex terms in order, so att 1 is real coeff of Fourier term 1, attribute 2 the imag etc
 * 3. Only stores the first half of the courier terms
 * 
 * Note that the series does store the first fourier term (series mean) and the imaginary part will always be zero
*/
import weka.core.*;
import weka.filters.SimpleBatchFilter;


public class FFT extends SimpleBatchFilter {
	/**
	 * 
	 */
         public enum AlgorithmType {DFT,FFTRadix2,FFTMixed}
         AlgorithmType algo=AlgorithmType.FFTRadix2;
	private static final long serialVersionUID = 1L;
	private boolean pad=true;
    private static final double TWOPI = (Math.PI * 2);
	public void padSeries(boolean b){pad=b;}
	
	@Override
	protected Instances determineOutputFormat(Instances inputFormat)
			throws Exception {
//Check all attributes are real valued, otherwise throw exception
		for(int i=0;i<inputFormat.numAttributes();i++)
			if(inputFormat.classIndex()!=i)
				if(!inputFormat.attribute(i).isNumeric())
					throw new Exception("Non numeric attribute not allowed in FFT");
		
		int length=findPowerOfTwoLength(inputFormat);
		
		//Set up instances size and format. 
		
		FastVector atts=new FastVector();
		String name;
		for(int i=0;i<length;i++){
			if(i%2==0)
				name="FFT_"+(i/2)+"_Real";
			else
				name="FFT_"+(i/2)+"_Imag";
			atts.addElement(new Attribute(name));
		}
		if(inputFormat.classIndex()>=0){	//Classification set, set class 
			//Get the class values as a fast vector			
			Attribute target =inputFormat.attribute(inputFormat.classIndex());

			FastVector vals=new FastVector(target.numValues());
			for(int i=0;i<target.numValues();i++)
				vals.addElement(target.value(i));
			atts.addElement(new Attribute(inputFormat.attribute(inputFormat.classIndex()).name(),vals));
		}	
		Instances result = new Instances("FFT_"+inputFormat.relationName(),atts,inputFormat.numInstances());
		if(inputFormat.classIndex()>=0)
			result.setClassIndex(result.numAttributes()-1);
		return result;
	}
	protected int findLength(Instances inputFormat){
            if(algo==AlgorithmType.FFTRadix2)
                return findPowerOfTwoLength(inputFormat);
            throw new RuntimeException("Aglorithm Type+ "+algo+" has not been implemented for FFT Class");
        }
	
	protected int findPowerOfTwoLength(Instances inputFormat){
		int oldLength=0;
		int length=0;
		if(inputFormat.classIndex()>=0)	//Classification set, dont transform the target class!
			oldLength=inputFormat.numAttributes()-1;
		else
			oldLength=inputFormat.numAttributes();
		//Check if a power of 2, if not either pad or truncate
		if(!MathsPower2.isPow2(oldLength)){
			length=(int)MathsPower2.roundPow2((float)oldLength);
			if(pad){
				if(length<oldLength)
					length*=2;
			}else{
				if(length>oldLength)
					length/=2;
			}
		}
		else
				length=oldLength;
		return length;
	}
	
	@Override
	public String globalInfo() {

		return null;
	}
	
	@Override
	public Instances process(Instances instances) throws Exception {
		Instances output=determineOutputFormat(instances);

//For each data, first extract the relevant data
		int length=output.numAttributes();
		if(instances.classIndex()>=0)
			length--;
			
		for(int i=0;i<instances.numInstances();i++){
			
//1. Get series stored in a complex array
			Complex[] c=new Complex[length];
			int count=0;
			double seriesTotal=0;
			for(int j=0;j<instances.numAttributes()&&count<length;j++){
				if(instances.classIndex()!=j){
					c[count]=new Complex(instances.instance(i).value(j),0.0);
					seriesTotal+=instances.instance(i).value(j);
					count++;
				}
			}
			while(count<length){
				c[count]=new Complex(seriesTotal,0);
				count++;
			}
//			System.out.println("Length="+length);
//2. Find FFT of series.	
//			c=FFT_Alternate.fft(c);
			fft(c,length);
//Extract out the terms and set the attributes
			Instance inst=new Instance(length+1);
			
			for(int j=0;j<length;j++){
				if(j%2==0)//Real
					inst.setValue(j, c[j/2].getReal());
				else	//Imag
					inst.setValue(j, c[j/2].getImag());
			}
	//Set class value.
			//Set class value.
			if(instances.classIndex()>=0)
				inst.setValue(output.classIndex(), instances.instance(i).classValue());

			output.add(inst);
		}
		return output;
	}
	
	
	
	
	
	
	  /**
	Perform an in-place mixed-radix inverse Fast Fourier Transform
	on the first <code>n</code> elements of the given set of
	<code>Complex</code> numbers. If <code>n</code> is not a power
	of two then the inverse FFT is performed on the first N
	numbers where N is largest power of two less than
	<code>n</code>
    */
    public void fft(Complex[] complex, int n) {
    	fft(1, complex, n);
        }

    /**
    Sort a set of <code>Complex</code> numbers into a bit-reversed
    order - only sort the first <code>n</code> elements. This
    method performs the sort in-place
 */
	 public static void bitReverse(Complex[] complex, int n) {
		int     halfN = n / 2;
		int     i, j, m;
		Complex temp;
		for (i = j = 0; i < n; ++i) {
		    if (j >= i) {
			temp = complex[j];
			complex[j] = complex[i];
			complex[i] = temp;
		    }
		    m = halfN;
		    while (m >= 1 && j >= m) {
			j -= m;
			m /= 2;
		    }
		    j += m;
		}
		temp = null;
	 }
     /**
 	Perform an in-place mixed-radix inverse Fast Fourier Transform
 	on the first <code>n</code> elements of the given set of
 	<code>Complex</code> numbers. If <code>n</code> is not a power
 	of two then the inverse FFT is performed on the first N
 	numbers where N is largest power of two less than
 	<code>n</code>
     */
        public void inverseFFT(Complex[] complex, int n) {
    	fft(-1, complex, n);
        }

        // Perform an in-place mixed-radix FFT (if sign is 1) or inverse
        // FFT (if sign is -1) on the first n elements of the given set of
        // Complex numbers. Round n to the nearest power of two.
        //
        // This method performs the FFT in-place on the given set.

        private void fft(int sign, Complex[] complex, int n) {
    	// n is number of data elements upon which FFT will be
    	// performed. Round number of data elements to nearest power
    	// of 2
    	n = (int)MathsPower2.roundPow2(n);
    	// Sort the first n elements into bit-reversed order
    	bitReverse(complex, n);
    	if (n == 2) {
    	    // Perform a radix-2 FFT
    	    radix2FFT(sign, complex, n, 0);
    	} else if (((float)Math.log(n) % (float)Math.log(4)) == 0) {
    	    // Perform a radix-4 FFT
    	    radix4FFT(sign, complex, n, 0);
    	} else {
    	    // n is a multiple or two or four [8, 32, 128, ...]
    	    // Perform a mixed-radix FFT
    	    int halfN = n / 2;
    	    // Do a radix-4 transform on elements 0..halfN - 1 which
    	    // contains even-indexed elements from the original
    	    // unsorted set of numbers by definition of the bit
    	    // reversal operation
    	    radix4FFT(sign, complex, halfN, 0);
    	    // Do a radix-4 transform on elements halfN - 1 .. n - 1
    	    // which contains odd-indexed elements from the original
    	    // unsorted set of numbers by definition of the bit
    	    // reversal operation
    	    radix4FFT(sign, complex, halfN, halfN);
    	    // Pair off even and odd elements and do final radix-2
    	    // transforms, multiplying by twiddle factors as required
    	    // Loop variables used to point to pairs of even and odd
    	    // elements
    	    int       g, h;
    	    // Array of two complex numbers for performing radix-2
    	    // FFTs on pairs of elements
    	    Complex[] radix2x2 = new Complex[2];
    	    // Twiddle factor
    	    Complex   twiddle = new Complex();
    	    // Values defining twiddle factor
    	    double     delta = -sign * TWOPI / n;
    	    double     w = 0;
    	    for (g = 0, h = halfN; g < halfN; g++, h++) {
    		// Twiddle factors...
    		twiddle.setRealImag((float)Math.cos(w),
    				    (float)Math.sin(w));
    		complex[h].multiply(twiddle);
    		radix2x2[0] = complex[g];
    		radix2x2[1] = complex[h];
    		// Perform the radix-2 FFT
    		radix2FFT(sign, radix2x2, 2, 0);
    		complex[g] = radix2x2[0];
    		complex[h] = radix2x2[1];
    		w += delta;
    	    }
    	    radix2x2 = null;
    	    twiddle = null;
    	}
    	if (sign == -1) {
    	    // Divide all values by n
    	    for (int g = 0; g < n; g++) {
    		complex[g].divide(n);
    	    }
    	}
        }

        // Perform an in-place radix-4 FFT (if sign is 1) or inverse
        // FFT (if sign is -1). FFT is performed in the n elements
        // starting at index lower
        //
        // Assumes that n is a power of 2 and that lower + n is less than
        // or equal to the number of complex numbers given
        //
        // This method performs the FFT in-place on the given set.

        private static void radix4FFT(int sign, Complex[] complex, int n,
    				  int lower) {
    	// Index of last element in array which will take part in the
    	// FFT
    	int     upper = n + lower;
    	// Variables used to hold the indicies of the elements forming
    	// the four inputs to a butterfly
    	int     i, j, k, l;
    	// Variables holding the results of the four main operations
    	// performed when processing a butterfly
    	Complex ijAdd = new Complex();
    	Complex klAdd = new Complex();
    	Complex ijSub = new Complex();
    	Complex klSub = new Complex();
    	// Twiddle factor
    	Complex twiddle = new Complex();
    	// Values defining twiddle factor
    	double   delta, w, w2, w3;
    	double   deltaLower = -sign * TWOPI;
    	// intraGap is number of array elements between the
    	// two inputs to a butterfly (equivalent to the number of
    	// butterflies in a cluster)
    	int     intraGap;
    	// interGap is the number of array elements between the first
    	// input of the ith butterfly in two adjacent clusters
    	int     interGap;
    	for (intraGap = 1, interGap = 4 * intraGap;
    	     intraGap < n;
    	     intraGap = interGap, interGap = 4 * intraGap) {
    	    delta = deltaLower / (float)interGap;
    	    // For each butterfly in a cluster
    	    w = w2 = w3 = 0;
    	    for (int but = 0; but < intraGap; ++but) {
    		// Process the intraGap-th butterfly in each cluster
    		// i is the top input to a butterfly and j the second,
    		// k third and l fourth
    		for (i = (but + lower), j = i + intraGap,
    			 k = j + intraGap, l = k + intraGap;
    		     i < upper;
    		     i += interGap, j += interGap,
    			 k += interGap, l += interGap) {
    		    // Calculate and apply twiddle factors
    		    // cos(0) = 1 and sin(0) = 0
    		    twiddle.setRealImag(1, 0);
    		    complex[i].multiply(twiddle);
    		    twiddle.setRealImag((float)Math.cos(w2),
    					(float)Math.sin(w2));
    		    complex[j].multiply(twiddle);
    		    twiddle.setRealImag((float)Math.cos(w),
    					(float)Math.sin(w));
    		    complex[k].multiply(twiddle);
    		    twiddle.setRealImag((float)Math.cos(w3),
    					(float)Math.sin(w3));
    		    complex[l].multiply(twiddle);
    		    // Compute the butterfly
    		    Complex.add(complex[i], complex[j], ijAdd);
    		    Complex.subtract(complex[i], complex[j], ijSub);
    		    Complex.add(complex[k], complex[l], klAdd);
    		    Complex.subtract(complex[k], complex[l], klSub);
    		    // Assign values
    		    Complex.add(ijAdd, klAdd, complex[i]);
    		    klSub.multiply(sign);
    		    complex[j].setRealImag(ijSub.getReal() +
    					   klSub.getImag(),
    					   ijSub.getImag() -
    					   klSub.getReal());
    		    Complex.subtract(ijAdd, klAdd, complex[k]);
    		    complex[l].setRealImag(ijSub.getReal() -
    					   klSub.getImag(),
    					   ijSub.getImag() +
    					   klSub.getReal());
    		}
    		w += delta;
    		w2 = w + w;
    		w3 = w2 + w;
    	    }
    	    intraGap = interGap;
    	}
    	ijAdd = klAdd = ijSub = klSub = twiddle = null;
        }

        // Perform an in-place radix-2 FFT (if sign is 1) or inverse
        // FFT (if sign is -1). FFT is performed in the n elements
        // starting at index lower
        //
        // Assumes that n is a power of 2 and that lower + n is less than
        // or equal to the number of complex numbers given...
        //
        // This method performs the FFT in-place on the given set.

        private static void radix2FFT(int sign, Complex[] complex, int n,
    				  int lower) {
    	// Index of last element in array which will take part in the
    	// FFT
    	int     upper = n + lower;
    	// Variables used to hold the indicies of the elements forming
    	// the two inputs to a butterfly
    	int     i, j;
    	// intraGap is number of array elements between the
    	// two inputs to a butterfly (equivalent to the number of
    	// butterflies in a cluster)
    	int     intraGap;
    	// interGap is the number of array elements between the first
    	// input of the ith butterfly in two adjacent clusters
    	int     interGap;
    	// The twiddle factor
    	Complex twiddle = new Complex();
    	// Values defining twiddle factor
    	float   deltaLower = -(float)(sign * Math.PI);
    	float   w, delta;
    	// Variable used to hold result of multiplying butterfly input
    	// by a twiddle factor
    	Complex twiddledInput = new Complex();
    	for (intraGap = 1, interGap = intraGap + intraGap;
    	     intraGap < n; intraGap = interGap,
    		 interGap = intraGap + intraGap) {
    	    delta = deltaLower / (float)intraGap;
    	    // For each butterfly in a cluster
    	    w = 0;
    	    for (int butterfly = 0; butterfly < intraGap; ++butterfly)
    		{
    		    // Calculate the twiddle factor
    		    twiddle.setRealImag((float)Math.cos(w),
    					(float)Math.sin(w));
    		    // i is the top input to a butterfly and j the
    		    // bottom
    		    for (i = (butterfly + lower), j = i + intraGap;
    			 i < upper; i += interGap, j += interGap) {
    			// Calculate the butterfly-th butterfly in
    			// each cluster
    			// Apply the twiddle factor
    			Complex.multiply(complex[j], twiddle,
    					 twiddledInput);
    			// Subtraction part of butterfly
    			Complex.subtract(complex[i], twiddledInput,
    					 complex[j]);
    			// Addition part of butterfly
    			complex[i].add(twiddledInput);
    		    }
    		    w += delta;
    		}
    	    intraGap = interGap;
    	}
    	twiddle = twiddledInput = null;
        }
	        
	        
	        
	public String getRevision() {
		return null;
	}
	public static class MathsPower2 {
	    
	    /** Return 2 to the power of <code>power</code> */
	    
	    public static int pow2(int power) { 
		return (1 << power); 
	    }
	    
	    /** Is <code>value</code> a power of 2? */
	    
	    public static boolean isPow2(int value) {
		return (value == (int)roundPow2(value));
	    }
	    
	    /** Round <code>value</code> to nearest power of 2 */
	    
	    public static float roundPow2(float value) {
		float power = (float)(Math.log(value) / Math.log(2));
		int intPower = Math.round(power);
		return (float)(pow2(intPower));
	    }
	    
	    /** 
		Return the log to base 2 of <code>value</code> rounded to the 
		nearest integer 
	    */
	    
	    public static int integerLog2(float value) {
		int intValue;
		if (value < 2) {
		    intValue = 0;
		} else if (value < 4) {
		    intValue = 1;
		} else if (value < 8) {
		    intValue = 2;
		} else if (value < 16) {
		    intValue = 3;
		} else if (value < 32) {
		    intValue = 4;
		} else if (value < 64) {
		    intValue = 5;
		} else if (value < 128) {
		    intValue = 6;
		} else if (value < 256) {
		    intValue = 7;
		} else if (value < 512) {
		    intValue = 8;
		} else if (value < 1024) {
		    intValue = 9;
		} else if (value < 2048) {
		    intValue = 10;
		} else if (value < 4098) {
		    intValue = 11;
		} else if (value < 8192) {
		    intValue = 12;
		} else {
		    intValue = Math.round(roundPow2(value));
		}
		return intValue;
	    }
	}

	/** Remove all attributes unless the target class
	 *  I'm not sure if the indexing changes 
	 * @param n
	 */
	public void truncate(Instances d, int n){
		int att=n;
		if(att<d.numAttributes()-1){//Remove the first two terms first
			d.deleteAttributeAt(0);
			d.deleteAttributeAt(0);
		}
		while(att<d.numAttributes()){
			if(att==d.classIndex())
				att++;
			else
				d.deleteAttributeAt(att);
		}
	}

        
	public static void computeDft(double[] inreal, double[] inimag, double[] outreal, double[] outimag) {
		int n = inreal.length;
		for (int k = 0; k < n; k++) {  // For each output element
			double sumreal = 0;
			double sumimag = 0;
			for (int t = 0; t < n; t++) {  // For each input element
				sumreal +=  inreal[t]*Math.cos(2*Math.PI * t * k / n) + inimag[t]*Math.sin(2*Math.PI * t * k / n);
				sumimag += -inreal[t]*Math.sin(2*Math.PI * t * k / n) + inimag[t]*Math.cos(2*Math.PI * t * k / n);
			}
			outreal[k] = sumreal;
			outimag[k] = sumimag;
		}
	}
	        
        
	/** Author Mike Jackson - University of Edinburgh - 1999-2001 */

	/** 
	    The <code>Complex</code> class generates objects that represent
	    complex numbers in terms of real and imaginary components and
	    supports addition, subtraction, multiplication, scalar
	    multiplication and division or these numbers. The calculation of
	    complex conjugates, magnitude, phase and power (in decibels) of
	    the <code>Complex</code> numbers are also supported. 
	*/

	public static class Complex implements Cloneable {
	    
	    /** Constant required to calculate power values in dBs: log 10 */ 
	    
	    protected static final float LOG10 = (float)Math.log(10);
	    
	    /** Constant required to calculate power values in dBs: 20 / log
		10 */ 
	    
	    protected static final float DBLOG = 20 / LOG10;
	    
	    /** Real component */

	    protected float              real;
	    
	    /** Imaginary component */
	    
	    protected float              imag;
	    
	    /** Create a new <code>Complex</code> number 0 + j0 */
	    
	    public Complex() { 
		real = imag = 0f; 
	    }

	    /** 
		Create a new <code>Complex</code> number <code>real</code> +
		j(<code>imag</code>)
	    */  

	    public Complex(float real, float imag) {
		this.real = real; 
		this.imag = imag;
	    }
	    public Complex(double real, double imag) {
			this.real = (float)real; 
			this.imag = (float)imag;
		    }
	    
	    /** 
		Set the <code>Complex</code> number to be <code>real</code> +
		j(<code>imag</code>) 
	    */
	    
	    public void setRealImag(float real, float imag) {
		this.real = real; 
		this.imag = imag;
	    }
	    
	    /** Get real component */

	    public float getReal() { 
		return real; 
	    }

	    /** Set real component */

	    public void setReal(float real) { 
		this.real = real; 
	    }

	    /** Get imaginary component */

	    public float getImag() { 
		return imag; 
	    }

	    /** Set imaginary component */

	    public void setImag(float imag) { 
		this.imag = imag; 
	    }

	    /** 
		Add the given <code>Complex</code> number to this
		<code>Complex</code> number 
	    */ 

	    public void add(Complex complex) { 
		real += complex.real; 
		imag += complex.imag; 
	    }

	    /** 
		Subtract the given <code>Complex</code> number from this
		<code>Complex</code> number 
	    */  
	    
	    public void subtract(Complex complex) { 
		real -= complex.real; 
		imag -= complex.imag;
	    }

	    /** 
		Multiply this <code>Complex</code> number by the given factor
	    */

	    public void multiply(float factor) {
		real *= factor; 
		imag *= factor;
	    }

	    /** Divide this <code>Complex</code> number by the given factor */ 

	    public void divide(float factor) {
		real /= factor; 
		imag /= factor;
	    }

	    /** 
		Multiply this <code>Complex</code> number by the given
		<code>Complex</code> number 
	    */ 

	    public void multiply(Complex complex) {
		float nuReal = real * complex.real - imag * complex.imag;
		float nuImag = real * complex.imag + imag * complex.real;
		real = nuReal; 
		imag = nuImag;
	    }
	    
	    /** 
		Set this <code>Complex</code> number to be its complex
		conjugate 
	    */ 
	    
	    public void conjugate() { 
		imag = (-imag); 
	    }
	    
	    /** 
		Return result of adding the complex conjugate of this
		<code>Complex</code> number to this <code>Complex</code>
		number
	     */ 

	    public float addConjugate() { 
		return real + real; 
	    }

	    /** 
		Return result of subtracting the complex conjugate of this
		<code>Complex</code> number from this <code>Complex</code>
		number
	    */ 
	    
	    public float subtractConjugate() { 
		return imag + imag; 
	    }
	    
	    /** Return the magnitude of the <code>Complex</code> number */
	    
	    public float getMagnitude() { 
		return magnitude(real, imag); 
	    }
	    
	    /** Return the phase of the <code>Complex</code> number */
	    
	    public float getPhase() { 
		return phase(real, imag); 
	    }
	    
	    /** Return the power of this <code>Complex</code> number in dBs */
	    
	    public float getPower() { 
		return power(real, imag); 
	    }
	    
	    /** Add two <code>Complex</code> numbers: c = a + b */
	    
	    public static void add(Complex a, Complex b, Complex c) {
		c.real = a.real + b.real;
		c.imag = a.imag + b.imag;
	    }
	    
	    /** Subtract two <code>Complex</code> numbers: c = a - b*/
	    
	    public static void subtract(Complex a, Complex b, Complex c) {
		c.real = a.real - b.real;
		c.imag = a.imag - b.imag;
	    }
	    
	    /** 
		Multiply a <code>Complex</code> number by a factor: b = a *
		factor 
	    */
	    
	    public static void multiply(Complex a, float factor, Complex b) {
		b.real = a.real * factor; 
		b.imag = a.imag * factor;
	    }
	    
	    /** 
		Divide a <code>Complex</code> number by a factor: b = a /
		factor 
	    */
	    
	    public static void divide(Complex a, float factor, Complex b) { 
		b.real = a.real / factor;
		b.imag = a.imag / factor;
	    }
	    
	    /** Multiply two <code>Complex</code> numbers: c = a * b */
	    
	    public static void multiply(Complex a, Complex b, Complex c) {
		c.real = a.real * b.real - a.imag * b.imag;
		c.imag = a.real * b.imag + a.imag * b.real;
	    }
	    
	    /** Place the <code>Complex</code> conjugate of a into b */
	    
	    public static void conjugate(Complex a, Complex b) {
		b.real = a.real;  
		b.imag = -a.imag;
	    }
	    
	    /** 
		Return the magnitude of a <code>Complex</code> number
		<code>real</code> + (<code>imag</code>)j 
	    */ 
	    
	    public static float magnitude(float real, float imag) { 
		return (float)Math.sqrt(real * real + imag * imag);
	    }
	    
	    /** 
		Return the phase of a <code>Complex</code> number
		<code>real</code> + (<code>imag</code>)j 
	    */ 
	    
	    public static float phase(float real, float imag) {
		return (float)Math.atan2(imag, real);
	    }
	    
	    /** 
		Return the power of a <code>Complex</code> number
		<code>real</code> + (<code>imag</code>)j 
	    */ 
	    
	    public static float power(float real, float imag) { 
		return DBLOG * (float)Math.log(magnitude(real, imag));
	    }
	    
	    /** 
		Place the real components of the first <code>n</code> elements 
		of the array <code>complex</code> of <code>Complex</code>
		numbers into the given <code>reals</code> array  
	    */
	    
	    public static void reals(int n, Complex[] complex, float[] reals) { 
		for (int i = 0; i < n; ++i) {
		    reals[i] = complex[i].real;
		}
	    }

	    /** 
		Place the imaginary components of the first <code>n</code>
		elements of the array <code>complex</code> of
		<code>Complex</code> numbers into the given <code>imags</code>
		array   
	    */ 
	    
	    public static void imaginaries(int n, Complex[] complex, float[]
					   imags) { 
		for (int i = 0; i < n; ++i) {
		    imags[i] = complex[i].imag;
		}
	    }
	    
	    /** 
		Place the magnitudes of the first <code>n</code> elements of
		the array <code>complex</code> of <code>Complex</code> numbers
		into the given <code>mags</code> array   
	    */ 

	    public static void magnitudes(int n, Complex[] complex, float[]
					 mags) {
		for (int i = 0; i < n; ++i) {
		    mags[i] = complex[i].getMagnitude();
		}
	    }

	    /** 
		Place the powers (in dBs) of the first <code>n</code> elements
		of the array <code>complex</code> of <code>Complex</code>
		numbers into the given <code>powers</code> array   
	    */ 

	    public static void powers(int n, Complex[] complex, float[] powers) {
		for (int i = 0; i < n; ++i) {
		    powers[i] = complex[i].getPower();
		}
	    }
	    
	    /** 
		Place the phases (in radians) of the first <code>n</code>
		elements of the array <code>complex</code> of
		<code>Complex</code> numbers into the given
		<code>phases</code> array
	    */  
	    
	    public static void phase(int n, Complex[] complex, float[] phases)
	    {
		for (int i = 0; i < n; ++i) {
		    phases[i] = complex[i].getPhase();
		}
	    }
	    
	    /** Return a clone of the <code>Complex</code> object */
	    
	    public Object clone() { 
		return new Complex(real, imag); 
	    }
	}
	//Primitives version, assumes zero mean global, passes max run length
	public int[] processSingleSeries(double[] d, int mrl){
		
		return null;
	}
        
        public static void basicTest(){
                //Test FFT	
        //Series 30,-1,2,3,3,2,-1,-4
        /*FFT Desired
        *  	34
                        19.9289321881345-5.82842712474618i
                        32-2i
                        34.0710678118655+0.171572875253798i
                        34
                        34.0710678118655-0.171572875253815i
                        32+2i
                        19.9289321881345+5.8284271247462i
        FFT Achieved
        34	0
        19.928932	-5.8284273
        32	-2
        34.071068	0.17157269
        34	0
        34.071068	-0.17157269
        32	2
        19.928932	5.8284273

        *
        */	
        //Test FFT with truncation
                System.out.println("Basic test of FFT");

                System.out.println("Series: 30,-1,2,3,3,2,-1,-4");
                System.out.println("	/*FFT Desired"+
        "  	34\n"+
                        "19.9289321881345-5.82842712474618i\n"+
                        "32-2i\n"+
                        "34.0710678118655+0.171572875253798i\n"+
                        "34\n"+
                        "34.0710678118655-0.171572875253815i\n"+
                        "32+2i\n"+
                        "19.9289321881345+5.8284271247462i\n"+
        "FFT Achieved\n"+
        "34	0\n"+
        "19.928932	-5.8284273\n"+
        "32	-2\n"+
        "34.071068	0.17157269\n"+
        "34	0\n"+
        "34.071068	-0.17157269\n"+
        "32	2\n"+
        "19.928932	5.8284273");	
                double[] d={30,-1,2,3,3,2,-1,-4};
                int n=8;
                Complex[] x= new Complex[n];
                for(int i=0;i<n;i++)
                    x[i]=new Complex(d[i], 0.0);
                for(int i=0;i<n;i++)
                    System.out.println(x[i].getReal()+","+x[i].getImag());
                System.out.println("Transformed");

                FFT fft =new FFT();
                fft.fft(x,x.length);
                for(int i=0;i<n;i++)
                        System.out.println(x[i].getReal()+","+x[i].getImag());
                fft.fft(x,x.length);   
        }

        public static void paddingTest(){
/* Test to check it works correctly with padded series
 *                    //Series 30,-1,2,3,3,2,-1,-4,3
 *                    //Padded series  30,-1,2,3,3,2,-1,-4,3,0,0,0,0,
 */
        }

        public static void main(String[] args){
            basicTest();
            paddingTest();
//         System.out.println("Reversed");
 //        for(int i=0;i<n;i++)
 //            System.out.println(x[i].getReal()+","+x[i].getImag());
         
/*       float y=50;
       int[] pos = {3,7,10};
       for(int i=0;i<pos.length;i++)
       {
                x[pos[i]].setReal(y);
//                x[pos[i]].setImag(y);
                x[n-pos[i]].setReal(y);
 //               x[n-pos[i]].setImag(-y);
       }
*/
		
	}


}
