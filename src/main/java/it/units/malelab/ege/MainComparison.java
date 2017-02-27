/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege;

import it.units.malelab.ege.problems.BenchmarkProblems;
import it.units.malelab.ege.evolver.genotype.BitsGenotype;
import it.units.malelab.ege.evolver.Evolver;
import it.units.malelab.ege.evolver.Individual;
import it.units.malelab.ege.evolver.PartitionConfiguration;
import it.units.malelab.ege.evolver.PartitionEvolver;
import it.units.malelab.ege.evolver.StandardConfiguration;
import it.units.malelab.ege.evolver.StandardEvolver;
import it.units.malelab.ege.evolver.genotype.BitsGenotypeFactory;
import it.units.malelab.ege.evolver.initializer.RandomInitializer;
import it.units.malelab.ege.evolver.listener.CollectorGenerationLogger;
import it.units.malelab.ege.evolver.listener.ConfigurationSaverListener;
import it.units.malelab.ege.evolver.listener.EvolutionImageSaverListener;
import it.units.malelab.ege.evolver.listener.EvolutionListener;
import it.units.malelab.ege.evolver.listener.WithConstants;
import it.units.malelab.ege.evolver.listener.collector.Best;
import it.units.malelab.ege.evolver.listener.collector.Diversity;
import it.units.malelab.ege.evolver.listener.collector.MultiMapperInfo;
import it.units.malelab.ege.evolver.listener.collector.Population;
import it.units.malelab.ege.evolver.operator.BitsSGECrossover;
import it.units.malelab.ege.evolver.operator.LocalizedTwoPointsCrossover;
import it.units.malelab.ege.evolver.operator.ProbabilisticMutation;
import it.units.malelab.ege.evolver.selector.First;
import it.units.malelab.ege.evolver.selector.IndividualComparator;
import it.units.malelab.ege.evolver.selector.RepresenterBasedListSelector;
import it.units.malelab.ege.evolver.selector.Selector;
import it.units.malelab.ege.evolver.selector.Tournament;
import it.units.malelab.ege.grammar.Grammar;
import it.units.malelab.ege.mapper.BitsSGEMapper;
import it.units.malelab.ege.mapper.MappingException;
import it.units.malelab.ege.mapper.MultiMapper;
import it.units.malelab.ege.mapper.PiGEMapper;
import it.units.malelab.ege.mapper.StandardGEMapper;
import it.units.malelab.ege.mapper.WeightedHierarchicalMapper;
import it.units.malelab.ege.util.Utils;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eric
 */
public class MainComparison {

  public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, MappingException {
    //prepare files
    PrintStream generationFilePS = new PrintStream(args[0] + File.separator + dateForFile() + "-generation.csv");
    PrintStream configurationFilePS = new PrintStream(args[0] + File.separator + dateForFile() + "-config.csv");
    File imagePath = new File(args[0] + File.separator + dateForFile() + "-images");
    if (!imagePath.exists()) {
      imagePath.mkdir();
    }
    //prepare problems
    Map<String, BenchmarkProblems.Problem> problems = new LinkedHashMap<>();
    problems.put("harmonic", BenchmarkProblems.harmonicCurveProblem());
    problems.put("poly4", BenchmarkProblems.classic4PolynomialProblem());
    //problems.put("max", BenchmarkProblems.max());
    problems.put("santaFe", BenchmarkProblems.santaFe());
    problems.put("text", BenchmarkProblems.text("Hello world!"));
    int counter = 0;
    List<EvolutionListener<BitsGenotype, String>> listeners = new ArrayList<>();
    listeners.add(new CollectorGenerationLogger<>(
            Collections.EMPTY_MAP,
            System.out, true, 10, " ", " | ",
            new Population<>(),
            new Best<>("%5.2f"),
            new Diversity<>(),
            new MultiMapperInfo<>(4)
    ));
    listeners.add(new CollectorGenerationLogger<>(
            (Map) Utils.sameValueMap("", "key", "problem", "run", "initGenotypeSize", "variant", "pop"),
            generationFilePS, false, 0, ";", ";",
            new Population<>(),
            new Best<>("%5.2f"),
            new Diversity<>(),
            new MultiMapperInfo<>(4)
    ));
    listeners.add(new ConfigurationSaverListener<>(
            (Map) Utils.sameValueMap("", "key", "problem", "run", "initGenotypeSize", "variant", "pop"),
            configurationFilePS
    ));
    listeners.add(new EvolutionImageSaverListener<>(
            (Map)Utils.sameValueMap("", "key", "problem", "run", "initGenotypeSize", "variant"),
            imagePath.getPath()
    ));
    for (int initGenoSize : new int[]{1024}) {
      for (String problemName : problems.keySet()) {
        BenchmarkProblems.Problem problem = problems.get(problemName);
        for (int r = 0; r < 1; r++) {
          Random random = new Random(r);
          Map<String, Object> constants = new LinkedHashMap<>();
          constants.put("key", counter);
          constants.put("problem", problemName);
          constants.put("run", r);
          constants.put("initGenotypeSize", initGenoSize);
          for (int m : new int[]{0, 1, 2, 3}) {
            StandardConfiguration<BitsGenotype, String> configuration = StandardConfiguration.createDefault(problem, random);
            //PartitionConfiguration<BitsGenotype, String> configuration = PartitionConfiguration.createDefault(problem, random);
            configuration.getOperators().clear();
            configuration
                    .populationSize(500)
                    .offspringSize(500)
                    .overlapping(true)
                    .numberOfGenerations(100)
                    .parentSelector(new Tournament(3, random, new IndividualComparator(IndividualComparator.Attribute.FITNESS)))
                    .populationInitializer(new RandomInitializer<>(random, new BitsGenotypeFactory(initGenoSize)))
                    .operator(new LocalizedTwoPointsCrossover(random), 0.8d)
                    .operator(new ProbabilisticMutation(random, 0.01), 0.2d);
            /*
            configuration
                    .partitionSize(1)
                    .partitionerComparator((Comparator) (new IndividualComparator(IndividualComparator.Attribute.PHENO)))
                    .parentPartitionSelector(new RepresenterBasedListSelector<>(
                            new First<>(),
                            new Tournament(3, random, new IndividualComparator(IndividualComparator.Attribute.FITNESS))
                    ))
                    .parentSelector((Selector) new it.units.malelab.ege.evolver.selector.Best<>(new IndividualComparator(IndividualComparator.Attribute.AGE)));
                    */
            Grammar<String> grammar = problems.get(problemName).getGrammar();
            switch (m) {
              case 0:
                configuration.mapper(new StandardGEMapper<>(8, 5, grammar));
                constants.put("variant", "GE-8-5");
                break;
              case 1:
                configuration.mapper(new PiGEMapper<>(16, 5, grammar));
                constants.put("variant", "piGE-16-5");
                break;
              case 2:
                BitsSGEMapper<String> bitsSGEMapper = new BitsSGEMapper<>(6, grammar);
                configuration.mapper(bitsSGEMapper);
                constants.put("variant", "BitSGE");
                configuration.getOperators().clear();
                configuration
                        .operator(new BitsSGECrossover(bitsSGEMapper, random), 0.8d)
                        .operator(new ProbabilisticMutation(random, 0.01), 0.2d);
                break;
              case 3:
                configuration.mapper(new WeightedHierarchicalMapper<>(3, grammar));
                constants.put("variant", "WHiGE-3");
                break;
              case 4:
                configuration.mapper(new MultiMapper<>(MultiMapper.SelectionCriterion.RESERVED_BITS,
                        new StandardGEMapper<>(8, 5, grammar),
                        new PiGEMapper<>(16, 5, grammar),
                        new BitsSGEMapper<>(6, grammar),
                        new WeightedHierarchicalMapper<>(3, grammar)
                ));
                constants.put("variant", "MuMapper-ReBi");
                break;
              case 5:
                configuration.mapper(new MultiMapper<>(MultiMapper.SelectionCriterion.ALL_MODULE,
                        new StandardGEMapper<>(8, 5, grammar),
                        new PiGEMapper<>(16, 5, grammar),
                        new BitsSGEMapper<>(10, grammar),
                        new WeightedHierarchicalMapper<>(3, grammar)
                ));
                constants.put("variant", "MuMapper-AlMo");
                break;
              case 6:
                configuration.mapper(new MultiMapper<>(MultiMapper.SelectionCriterion.ALL_MAX,
                        new StandardGEMapper<>(8, 5, grammar),
                        new PiGEMapper<>(16, 5, grammar),
                        new BitsSGEMapper<>(6, grammar),
                        new WeightedHierarchicalMapper<>(3, grammar)
                ));
                constants.put("variant", "MuMapper-AlMa");
                break;
              case 7:
                configuration.mapper(new MultiMapper<>(MultiMapper.SelectionCriterion.ALL_BINARY,
                        new StandardGEMapper<>(8, 5, grammar),
                        new PiGEMapper<>(16, 5, grammar),
                        new BitsSGEMapper<>(6, grammar),
                        new WeightedHierarchicalMapper<>(3, grammar)
                ));
                constants.put("variant", "MuMapper-AlBi");
                break;
            }
            Evolver<BitsGenotype, String> evolver = new StandardEvolver<>(Runtime.getRuntime().availableProcessors() - 1, configuration, random, false);
            //Evolver<BitsGenotype, String> evolver = new StandardEvolver<>(1, configuration, random, false);
            //Evolver<BitsGenotype, String> evolver = new PartitionEvolver<>(Runtime.getRuntime().availableProcessors() - 1, configuration, random, false);
            //Evolver<BitsGenotype, String> evolver = new PartitionEvolver<>(1, configuration, random, false);
            System.out.println(constants);
            for (EvolutionListener listener : listeners) {
              if (listener instanceof WithConstants) {
                ((WithConstants) listener).updateConstants(constants);
              }
            }
            evolver.go(listeners);
            System.out.println();
            counter = counter + 1;
          }
        }
      }
    }
    generationFilePS.close();
    configurationFilePS.close();
  }

  private static String dateForFile() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmm");
    return simpleDateFormat.format(new Date());
  }

}
