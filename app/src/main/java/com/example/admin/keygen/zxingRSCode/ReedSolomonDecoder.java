package com.example.admin.keygen.zxingRSCode;

public final class ReedSolomonDecoder {

    private final GenericGF field;

    public ReedSolomonDecoder(GenericGF field) {
        this.field = field;
    }

    /**
     * <p>Decodes given set of received codewords, which include both data and error-correction
     * codewords. Really, this means it uses Reed-Solomon to detect and correct errors, in-place,
     * in the input.</p>
     *
     * @param received data and error-correction codewords
     * @param twoS number of error-correction codewords available
     * @throws ReedSolomonException if decoding fails for any reason
     */
    public void decode(int[] received, int twoS) throws ReedSolomonException {
        GenericGFPoly poly = new GenericGFPoly(field, received);
        int[] syndromeCoefficients = new int[twoS];
        boolean noError = true;
        for (int i = 0; i < twoS; i++) {
            int eval = poly.evaluateAt(field.exp(i + field.getGeneratorBase()));
            syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
            if (eval != 0) {
                noError = false;
            }
        }
        if (noError) {
            return;
        }
        GenericGFPoly syndrome = new GenericGFPoly(field, syndromeCoefficients);
        GenericGFPoly[] sigmaOmega =
                runEuclideanAlgorithm(field.buildMonomial(twoS, 1), syndrome, twoS);
        GenericGFPoly sigma = sigmaOmega[0];
        GenericGFPoly omega = sigmaOmega[1];
        int[] errorLocations = findErrorLocations(sigma);
        int[] errorMagnitudes = findErrorMagnitudes(omega, errorLocations);
        for (int i = 0; i < errorLocations.length; i++) {
            int position = received.length - 1 - field.log(errorLocations[i]);
            if (position < 0) {
                throw new ReedSolomonException("Bad error location");
            }
            received[position] = GenericGF.addOrSubtract(received[position], errorMagnitudes[i]);
        }
    }

    public int[] myDecode(GenericGFPoly aliceSyndrom,int[] bob,int twoS) throws ReedSolomonException
    {
        //GenericGFPoly aliceSyndrom = getSyndromes(alice, twoS);
        System.out.println("alice syndrom: " + aliceSyndrom);
        GenericGFPoly bobSyndrom   = getSyndromes(bob, twoS);
        System.out.println("bob syndrom:   " + bobSyndrom);
        GenericGFPoly diffSyndrom  = aliceSyndrom.addOrSubtract(bobSyndrom);
        System.out.println("diff syndrom:  " + diffSyndrom);
        if(diffSyndrom.isZero()) {
            return bob;
        }
        GenericGFPoly[] sigmaOmega =
                runEuclideanAlgorithm(field.buildMonomial(twoS, 1), diffSyndrom, twoS);
        GenericGFPoly sigma = sigmaOmega[0];
        System.out.println("sigma: " + sigma);
        GenericGFPoly omega = sigmaOmega[1];
        System.out.println("omega: " + omega);
        int[] errorLocations = findErrorLocations(sigma);
        int[] errorMagnitudes = findErrorMagnitudes(omega, errorLocations);
        System.out.println("----------errorLocations--------");
        for(int i:errorLocations)
        {
            System.out.print(i+" ");
        }
        System.out.println();
        System.out.println("----------errorMagnitudes--------");
        for(int i:errorMagnitudes)
        {
            System.out.print(i+" ");
        }
        System.out.println();

        int[] result = bob;
        for (int i = 0; i < errorLocations.length; i++) {
            int position = bob.length - 1 - field.log(errorLocations[i]);
            if (position < 0) {
                throw new ReedSolomonException("Bad error location");
            }
            result[position] = GenericGF.addOrSubtract(bob[position], errorMagnitudes[i]);
            System.out.println("bob["+position+"] - "+"errorMagnitudes["+i+"]=" + bob[position]+" - "+errorMagnitudes[i]+"= "+result[position]);
        }
        System.out.println("-------------- item in result--------");
        for(int i:result) {
            System.err.print(i+" ");
        }
        System.out.println("\n");
        return result;
    }

    public GenericGFPoly getSyndromes(int[] received,int twoS) {
        GenericGFPoly poly = new GenericGFPoly(field, received);
        int[] syndromeCoefficients = new int[twoS];
        for (int i = 0; i < twoS; i++) {
            int eval = poly.evaluateAt(field.exp(i + field.getGeneratorBase()));
            syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
        }
        GenericGFPoly syndrome = new GenericGFPoly(field, syndromeCoefficients);
        return syndrome;
    }

    public int[] infoReconciliation(GenericGFPoly syndrome,int[] bob) throws ReedSolomonException {
        //12 is the length of correction codeword : 15-3
        GenericGFPoly[] sigmaOmega =
                runEuclideanAlgorithm(field.buildMonomial(12, 1), syndrome, 12);
        GenericGFPoly sigma = sigmaOmega[0];
        GenericGFPoly omega = sigmaOmega[1];
        int[] errorLocations = findErrorLocations(sigma);
        int[] errorMagnitudes = findErrorMagnitudes(omega, errorLocations);
        int[] result = new int[bob.length];
        for (int i = 0; i < errorLocations.length; i++) {
            int position = 15 - 1 - field.log(errorLocations[i]);
            if (position < 0) {
                throw new ReedSolomonException("Bad error location");
            }
            result[position] = GenericGF.addOrSubtract(bob[position], errorMagnitudes[i]);
        }
        return result;
    }




    private GenericGFPoly[] runEuclideanAlgorithm(GenericGFPoly a, GenericGFPoly b, int R)
            throws ReedSolomonException {
        // Assume a's degree is >= b's
        if (a.getDegree() < b.getDegree()) {
            GenericGFPoly temp = a;
            a = b;
            b = temp;
        }

        GenericGFPoly rLast = a;
        GenericGFPoly r = b;
        GenericGFPoly tLast = field.getZero();
        GenericGFPoly t = field.getOne();

        // Run Euclidean algorithm until r's degree is less than R/2
        while (r.getDegree() >= R / 2) {
            GenericGFPoly rLastLast = rLast;
            GenericGFPoly tLastLast = tLast;
            rLast = r;
            tLast = t;

            // Divide rLastLast by rLast, with quotient in q and remainder in r
            if (rLast.isZero()) {
                // Oops, Euclidean algorithm already terminated?
                throw new ReedSolomonException("r_{i-1} was zero");
            }
            r = rLastLast;
            GenericGFPoly q = field.getZero();
            int denominatorLeadingTerm = rLast.getCoefficient(rLast.getDegree());
            int dltInverse = field.inverse(denominatorLeadingTerm);
            while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
                int degreeDiff = r.getDegree() - rLast.getDegree();
                int scale = field.multiply(r.getCoefficient(r.getDegree()), dltInverse);
                q = q.addOrSubtract(field.buildMonomial(degreeDiff, scale));
                r = r.addOrSubtract(rLast.multiplyByMonomial(degreeDiff, scale));
            }

            t = q.multiply(tLast).addOrSubtract(tLastLast);

            if (r.getDegree() >= rLast.getDegree()) {
                throw new IllegalStateException("Division algorithm failed to reduce polynomial?");
            }
        }

        int sigmaTildeAtZero = t.getCoefficient(0);
        if (sigmaTildeAtZero == 0) {
            throw new ReedSolomonException("sigmaTilde(0) was zero");
        }

        int inverse = field.inverse(sigmaTildeAtZero);
        GenericGFPoly sigma = t.multiply(inverse);
        GenericGFPoly omega = r.multiply(inverse);
        return new GenericGFPoly[]{sigma, omega};
    }

    private int[] findErrorLocations(GenericGFPoly errorLocator) throws ReedSolomonException {
        //System.out.println("errorLocator: " + errorLocator);
        // This is a direct application of Chien's search
        int numErrors = errorLocator.getDegree();

        if (numErrors == 1) { // shortcut
            return new int[] { errorLocator.getCoefficient(1) };
        }
        int[] result = new int[numErrors];
        int e = 0;
        for (int i = 1; i < field.getSize() && e < numErrors; i++) {
            if (errorLocator.evaluateAt(i) == 0) {
                result[e] = field.inverse(i);
                e++;
            }
        }
        if (e != numErrors) {
            throw new ReedSolomonException("Error locator degree does not match number of roots");
        }
        return result;
    }

    private int[] findErrorMagnitudes(GenericGFPoly errorEvaluator, int[] errorLocations) {
        // This is directly applying Forney's Formula
        int s = errorLocations.length;
        int[] result = new int[s];
        for (int i = 0; i < s; i++) {
            int xiInverse = field.inverse(errorLocations[i]);
            int denominator = 1;
            for (int j = 0; j < s; j++) {
                if (i != j) {
                    //denominator = field.multiply(denominator,
                    //    GenericGF.addOrSubtract(1, field.multiply(errorLocations[j], xiInverse)));
                    // Above should work but fails on some Apple and Linux JDKs due to a Hotspot bug.
                    // Below is a funny-looking workaround from Steven Parkes
                    int term = field.multiply(errorLocations[j], xiInverse);
                    int termPlus1 = (term & 0x1) == 0 ? term | 1 : term & ~1;
                    denominator = field.multiply(denominator, termPlus1);
                }
            }
            result[i] = field.multiply(errorEvaluator.evaluateAt(xiInverse),
                    field.inverse(denominator));
            if (field.getGeneratorBase() != 0) {
                result[i] = field.multiply(result[i], xiInverse);
            }
        }
        return result;
    }

}
