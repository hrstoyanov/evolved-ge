/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.operator;

import it.units.malelab.ege.BitsGenotype;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author eric
 */
public class CompactFlipMutation extends AbstractMutation<BitsGenotype> {
  
  private final Random random;

  public CompactFlipMutation(Random random) {
    this.random = random;
  }

  @Override
  public List<BitsGenotype> apply(List<BitsGenotype> parents) {
    BitsGenotype parent = parents.get(0);
    BitsGenotype child = new BitsGenotype(parent.size());
    child.set(0, parent);
    int fromIndex = random.nextInt(child.size()-1);
    int size = Math.max(1, random.nextInt(child.size()-fromIndex));
    child.flip(fromIndex, fromIndex+size);
    return Collections.singletonList(child);
  }  
  
}
