/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.evolver.selector;

import it.units.malelab.ege.Genotype;
import it.units.malelab.ege.evolver.Individual;
import java.util.List;

/**
 *
 * @author eric
 */
public interface Selector<G extends Genotype, T> {
  
  public Individual<G, T> select(List<Individual<G, T>> population);
  
}
