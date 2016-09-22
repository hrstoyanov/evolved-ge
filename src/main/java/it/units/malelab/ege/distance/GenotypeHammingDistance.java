/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.distance;

import it.units.malelab.ege.Genotype;

/**
 *
 * @author eric
 */
public class GenotypeHammingDistance implements Distance<Genotype> {

  @Override
  public double d(Genotype g1, Genotype g2) {
    if (g2.size()!=g2.size()) {
      throw new IllegalArgumentException(String.format("Genotypes of different size: %d vs. %d%n", g1.size(), g2.size()));
    }
    int count = 0;
    for (int i = 0; i<g1.size(); i++) {
      if (g1.get(i)!=g2.get(i)) {
        count = count+1;
      }
    }
    return count;
  }
  
}
