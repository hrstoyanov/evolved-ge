/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.core.evolver.sepandconq;

import it.units.malelab.ege.core.Node;
import it.units.malelab.ege.core.Problem;
import it.units.malelab.ege.core.evolver.PartitionConfiguration;
import it.units.malelab.ege.core.evolver.PartitionEvolver;
import it.units.malelab.ege.core.fitness.BinaryClassification;
import it.units.malelab.ege.core.fitness.FitnessComputer;
import it.units.malelab.ege.core.fitness.MultiObjectiveFitness;
import it.units.malelab.ege.core.listener.EvolverListener;
import it.units.malelab.ege.core.listener.event.GenerationEvent;
import it.units.malelab.ege.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eric
 */
public class SACEvolver<I, G, T, F extends MultiObjectiveFitness> extends PartitionEvolver<G, T, F> {

  private final SACConfiguration<I, G, T, F> configuration;
  private final int numberOfThreads;

  public SACEvolver(int numberOfThreads, SACConfiguration<I, G, T, F> configuration, Random random, boolean saveAncestry) {
    super(numberOfThreads, configuration, random, saveAncestry);
    this.configuration = configuration;
    this.numberOfThreads = numberOfThreads;
  }

  @Override
  public List<Node<T>> solve(List<EvolverListener<G, T, F>> listeners) throws InterruptedException, ExecutionException {
    Node<T> joined = null;
    BinaryClassification<I, T> allLearningFitness = (BinaryClassification<I, T>) configuration.getProblem().getLearningFitnessComputer();
    BinaryClassification<I, T> allTestingFitness = (BinaryClassification<I, T>) configuration.getProblem().getTestingFitnessComputer();
    List<I> removedPositives = new ArrayList<>();
    int rounds = 0;
    while (true) {
      List<I> localPositives = new ArrayList<>(allLearningFitness.getPositives());
      localPositives.removeAll(removedPositives);
      BinaryClassification<I, T> localFitnessComputer = new BinaryClassification<>(
              localPositives,
              allLearningFitness.getNegatives(),
              allLearningFitness.getClassifier()
      );
      PartitionConfiguration<G, T, F> innerConfiguration = new PartitionConfiguration<>(
              configuration.getPartitionerComparator(),
              configuration.getPartitionSize(),
              configuration.getParentInPartitionRanker(),
              configuration.getParentInPartitionSelector(),
              configuration.getUnsurvivalInPartitionRanker(),
              configuration.getUnsurvivalInPartitionSelector(),
              configuration.getPopulationSize(),
              configuration.getNumberOfGenerations(),
              configuration.getPopulationInitializer(),
              configuration.getInitGenotypeValidator(),
              configuration.getMapper(),
              configuration.getOperators(),
              configuration.getRanker(),
              configuration.getParentSelector(),
              configuration.getUnsurvivalSelector(),
              configuration.getOffspringSize(),
              configuration.isOverlapping(),
              new Problem<>(
                      configuration.getProblem().getGrammar(),
                      (FitnessComputer<T, F>) localFitnessComputer,
                      configuration.getProblem().getTestingFitnessComputer(),
                      configuration.getProblem().getPhenotypePrinter()
              )
      );
      //obtain bests
      PartitionEvolver<G, T, F> partitionEvolver = new PartitionEvolver<>(numberOfThreads, innerConfiguration, random, saveAncestry);
      List<Node<T>> bests = partitionEvolver.solve(listeners);
      Node<T> best = bests.get(0);      
      //remove positives
      int truePositives = 0;
      for (I positive : localPositives) {
        if (allLearningFitness.getClassifier().classify(positive, best)) {
          removedPositives.add(positive);
          truePositives = truePositives+1;
        }
      }
      int falsePositives = 0;
      for (I negative : allLearningFitness.getNegatives()) {
        if (allLearningFitness.getClassifier().classify(negative, best)) {
          falsePositives = falsePositives+1;
        }
      }
      if ((truePositives == 0)||(falsePositives>0)) {
        break;
      }
      //join solutions
      if (joined == null) {
        joined = best;
      } else {
        joined = configuration.getJoiner().join(joined, best);
      }
      rounds = rounds+1;
      MultiObjectiveFitness joinedLearningFitness = allLearningFitness.compute(joined);
      MultiObjectiveFitness joinedTestingFitness = allTestingFitness.compute(joined);
      System.out.printf("Round: %2d, (P=%3d N=%3d) -> LEARN: fpr=%5.3f fnr=%5.3f, TEST: fpr=%5.3f fnr=%5.3f\t%s%n",
              rounds,
              localPositives.size(),
              allLearningFitness.getNegatives().size(),
              joinedLearningFitness.getValue()[0],
              joinedLearningFitness.getValue()[1],
              joinedTestingFitness.getValue()[0],
              joinedTestingFitness.getValue()[1],
              configuration.getProblem().getPhenotypePrinter().toString(joined)
      );
      if (removedPositives.size()==allLearningFitness.getPositives().size()) {
        break;
      }      
    }
    return Collections.singletonList(joined);
  }

}