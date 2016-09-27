/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.evolver.fitness;

import it.units.malelab.ege.Node;
import it.units.malelab.ege.symbolicregression.Element;
import it.units.malelab.ege.symbolicregression.MathUtils;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author eric
 */
public class SymbolicRegressionFitness implements FitnessComputer<String> {
  
  public static interface TargetFunction {
    public double compute(double... arguments);
  }
  
  private final double[] targetValues;
  private final LinkedHashMap<String, double[]> varValues;

  public SymbolicRegressionFitness(TargetFunction targetFunction, LinkedHashMap<String, double[]> varValues) {
    this.varValues = varValues;
    targetValues = new double[varValues.get((String)varValues.keySet().toArray()[0]).length];
    for (int i = 0; i<targetValues.length; i++) {
      double[] arguments = new double[varValues.keySet().size()];
      int j = 0;
      for (Map.Entry<String, double[]> entry : varValues.entrySet()) {
        arguments[j] = entry.getValue()[i];
        j++;
      }
      targetValues[i] = targetFunction.compute(arguments);
    }
  }

  @Override
  public Fitness compute(Node<String> phenotype) {
    double[] computed = MathUtils.compute(MathUtils.transform(phenotype), varValues, targetValues.length);
    double mae = 0;
    for (int i = 0; i<targetValues.length; i++) {
      mae = mae+Math.abs(computed[i]-targetValues[i]);
    }
    return new NumericFitness(mae);
  }

  @Override
  public Fitness worstValue() {
    return new NumericFitness(Double.POSITIVE_INFINITY);
  }
  
}
