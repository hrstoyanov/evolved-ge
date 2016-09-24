/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.mapper;

import it.units.malelab.ege.Genotype;
import it.units.malelab.ege.Node;
import it.units.malelab.ege.grammar.Grammar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author eric
 */
public class HierarchicalMapper<T> extends AbstractMapper<T> {

  private final Map<T, Integer> shortestOptionIndexMap;

  public HierarchicalMapper(Grammar<T> grammar) {
    super(grammar);
    Map<T, List<Integer>> optionJumpsToTerminalMap = new LinkedHashMap<>();
    for (Map.Entry<T, List<List<T>>> rule : grammar.getRules().entrySet()) {
      List<Integer> optionsJumps = new ArrayList<>();
      for (List<T> option : rule.getValue()) {
        optionsJumps.add(Integer.MAX_VALUE);
      }
      optionJumpsToTerminalMap.put(rule.getKey(), optionsJumps);
    }
    while (true) {
      boolean completed = true;
      for (Map.Entry<T, List<Integer>> entry : optionJumpsToTerminalMap.entrySet()) {
        for (int i = 0; i < entry.getValue().size(); i++) {
          List<T> option = grammar.getRules().get(entry.getKey()).get(i);
          if (Collections.disjoint(option, grammar.getRules().keySet())) {
            entry.getValue().set(i, 1);
          } else {
            int maxJumps = Integer.MIN_VALUE;
            for (T optionSymbol : option) {
              List<Integer> optionSymbolJumps = optionJumpsToTerminalMap.get(optionSymbol);
              if (optionSymbolJumps == null) {
                maxJumps = Math.max(0, maxJumps);
              } else {
                int minJumps = Integer.MAX_VALUE;
                for (int jumps : optionSymbolJumps) {
                  minJumps = Math.min(minJumps, jumps);
                }
                minJumps = (minJumps==Integer.MAX_VALUE)?minJumps:(minJumps+1);
                maxJumps = Math.max(minJumps, maxJumps);
              }
            }
            entry.getValue().set(i, maxJumps);
            if (maxJumps == Integer.MAX_VALUE) {
              completed = false;
            }
          }
        }
      }
      if (completed) {
        break;
      }
    }
    //build shortestOptionIndexMap
    shortestOptionIndexMap = new LinkedHashMap<>();
    for (Map.Entry<T, List<List<T>>> rule : grammar.getRules().entrySet()) {
      int minJumpsOptionIndex = 0;
      int minJumps = Integer.MAX_VALUE;
      for (int i = 0; i < optionJumpsToTerminalMap.get(rule.getKey()).size(); i++) {
        int jumps = optionJumpsToTerminalMap.get(rule.getKey()).get(i);
        if (jumps<minJumps) {
          minJumps = jumps;
          minJumpsOptionIndex = i;
        }
      }
      shortestOptionIndexMap.put(rule.getKey(), minJumpsOptionIndex);
    }
  }

  private class EnhancedSymbol<T> {

    private final T symbol;
    private final Genotype genotype;

    public EnhancedSymbol(T symbol, Genotype genotype) {
      this.symbol = symbol;
      this.genotype = genotype;
    }

    public T getSymbol() {
      return symbol;
    }

    public Genotype getGenotype() {
      return genotype;
    }

  }

  @Override
  public Node<T> map(Genotype genotype) throws MappingException {
    Node<EnhancedSymbol<T>> enhancedTree = new Node<>(new EnhancedSymbol<>(grammar.getStartingSymbol(), genotype));
    while (true) {
      Node<EnhancedSymbol<T>> nodeToBeReplaced = null;
      for (Node<EnhancedSymbol<T>> node : enhancedTree.leaves()) {
        if (grammar.getRules().keySet().contains(node.getContent().getSymbol())) {
          nodeToBeReplaced = node;
          break;
        }
      }
      if (nodeToBeReplaced == null) {
        break;
      }
      //get genotype
      T symbol = nodeToBeReplaced.getContent().getSymbol();
      Genotype symbolGenotype = nodeToBeReplaced.getContent().getGenotype();
      List<List<T>> options = grammar.getRules().get(symbol);
      //get option
      List<T> symbols;
      if (symbolGenotype.size() < options.size()) {
        symbols = options.get(shortestOptionIndexMap.get(symbol));
      } else {
        symbols = chooseOption(symbolGenotype, options);
      }
      //add children
      List<Genotype> childGenotypes = getSlices(symbolGenotype, symbols);
      for (int i = 0; i < symbols.size(); i++) {
        Node<EnhancedSymbol<T>> newChild = new Node<>(new EnhancedSymbol<>(
                symbols.get(i),
                childGenotypes.get(i)
        ));
        nodeToBeReplaced.getChildren().add(newChild);
      }
    }
    //convert
    return extractFromEnhanced(enhancedTree);
  }

  protected List<Genotype> getSlices(Genotype genotype, List<T> symbols) {
    if (symbols.size()>genotype.size()) {
      List<Genotype> genotypes = new ArrayList<>(symbols.size());
      for (T symbol : symbols) {
        genotypes.add(new Genotype(0));
      }
      return genotypes;
    }
    return genotype.slices(symbols.size());
  }

  private Node<T> extractFromEnhanced(Node<EnhancedSymbol<T>> enhancedNode) {
    Node<T> node = new Node<>(enhancedNode.getContent().getSymbol());
    for (Node<EnhancedSymbol<T>> enhancedChild : enhancedNode.getChildren()) {
      node.getChildren().add(extractFromEnhanced(enhancedChild));
    }
    return node;
  }

  private <K> K chooseOption(Genotype genotype, List<K> options) {
    if (options.size() == 1) {
      return options.get(0);
    }
    int index = 0;
    double max = Double.MIN_VALUE;
    List<Genotype> slices = genotype.slices(options.size());
    for (int i = 0; i < options.size(); i++) {
      double value = (double) slices.get(i).count() / (double) slices.get(i).size();
      if (value > max) {
        max = value;
        index = i;
      }
    }
    return options.get(index);
  }

}
