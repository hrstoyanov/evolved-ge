/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.benchmark;

import it.units.malelab.ege.core.Problem;
import it.units.malelab.ege.core.fitness.FitnessComputer;
import it.units.malelab.ege.core.fitness.NumericFitness;
import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.util.Utils;
import it.units.malelab.ege.benchmark.symbolicregression.MathUtils;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 *
 * @author eric
 */
public class Max extends Problem<String, NumericFitness> {

  public Max() throws IOException {
    super(Utils.parseFromFile(new File("grammars/max-grammar.bnf")),
            new FitnessComputer<String, NumericFitness>() {
              @Override
              public NumericFitness compute(Node<String> phenotype) {
                return new NumericFitness(-MathUtils.compute(MathUtils.transform(phenotype), Collections.EMPTY_MAP, 1)[0]);
              }

              @Override
              public NumericFitness worstValue() {
                return new NumericFitness(Double.POSITIVE_INFINITY);
              }

              @Override
              public NumericFitness bestValue() {
                return null;
              }
            },
            null,
            MathUtils.phenotypePrinter());
  }

}
