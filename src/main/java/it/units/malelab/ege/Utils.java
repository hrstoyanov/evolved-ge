/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege;

import it.units.malelab.ege.grammar.Grammar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 *
 * @author eric
 */
public class Utils {
  
  public static Grammar<String> parseFromFile(File file) throws FileNotFoundException, IOException {
    Grammar<String> grammar = new Grammar<>();
    BufferedReader br = new BufferedReader(new FileReader(file));
    String line;
    while ((line = br.readLine())!=null) {
      String[] components = line.split(Pattern.quote(Grammar.RULE_ASSIGNMENT_STRING));
      String toReplaceSymbol = components[0].trim();
      String[] optionStrings = components[1].split(Pattern.quote(Grammar.RULE_OPTION_SEPARATOR_STRING));
      if (grammar.getStartingSymbol()==null) {
        grammar.setStartingSymbol(toReplaceSymbol);
      }
      List<List<String>> options = new ArrayList<>();
      for (String optionString : optionStrings) {
        List<String> symbols = new ArrayList<>();
        for (String symbol : optionString.split("\\s+")) {
          if (!symbol.trim().isEmpty()) {
            symbols.add(symbol.trim());
          }
        }
        if (!symbols.isEmpty()) {
          options.add(symbols);
        }
      }
      grammar.getRules().put(toReplaceSymbol, options);
    }
    br.close();
    return grammar;
  }
  
  public static Genotype randomGenotype(int size, Random random) {
    BitSet bitSet = new BitSet(size);
    for (int i = 0; i<size; i++) {
      bitSet.set(i, random.nextBoolean());
    }
    return new Genotype(size, bitSet);
  }
  
  public static double mean(double[] values) {
    if (values.length==0) {
      return Double.NaN;
    }
    double mean = 0;
    for (double value : values) {
      mean = mean+value;
    }
    return mean/(double)values.length;
  }
  
  public static <T> Grammar<Pair<T, Integer>> resolveRecursiveGrammar(Grammar<T> grammar, int maxDepth) {
    Grammar<Pair<T, Integer>> resolvedGrammar = new Grammar<>();
    //build tree from recursive
    Node<T> tree = new Node<>(grammar.getStartingSymbol());
    while (true) {
      tree.propagateParentship();
      Node<T> toExpandNode = null;
      for (Node<T> node : tree.leaves()) {
        if (grammar.getRules().keySet().contains(node.getContent())) {
          toExpandNode = node;
          break;
        }
      }
      if (toExpandNode==null) {
        break;
      }
      List<T> symbols = new ArrayList<>();
      for (List<T> optionSymbols : grammar.getRules().get(toExpandNode.getContent())) {
        for (T optionSymbol : optionSymbols) {
          if (!symbols.contains(optionSymbol)) {
            symbols.add(optionSymbol);
          }
        }
      }
      for (T symbol : symbols) {
        int countOfEqualAncestors = count(contents(toExpandNode.getAncestors()), symbol);
        if (countOfEqualAncestors<maxDepth) {
          toExpandNode.getChildren().add(new Node<>(symbol));
        }
      }
    }
    //decorate tree with depth
    Node<Pair<T, Integer>> decoratedTree = decorateTreeWithDepth(tree);
    decoratedTree.propagateParentship();
    //rewrite grammar
    resolvedGrammar.getRules().put(new Pair<>(grammar.getStartingSymbol(), 0), new ArrayList<List<Pair<T, Integer>>>());
    while (true) {
      Pair<T, Integer> toFillDecoratedNonTerminal = null;
      for (Pair<T, Integer> decoratedNonTerminal : resolvedGrammar.getRules().keySet()) {
        if (resolvedGrammar.getRules().get(decoratedNonTerminal).isEmpty()) {
          toFillDecoratedNonTerminal = decoratedNonTerminal;
          break;
        }
      }
      if (toFillDecoratedNonTerminal==null) {
        break;
      }
      //look for this non-terminal in the tree
      List<Pair<T, Integer>> decoratedSymbols = contents(findNodeWithContent(decoratedTree, toFillDecoratedNonTerminal).getChildren());
      Map<T, Pair<T, Integer>> map = new LinkedHashMap<>();
      for (Pair<T, Integer> pair : decoratedSymbols) {
        map.put(pair.getFirst(), pair);
      }
      //process original rule
      List<List<T>> options = grammar.getRules().get(toFillDecoratedNonTerminal.getFirst());
      for (List<T> option : options) {
        if (map.keySet().containsAll(option)) {
          List<Pair<T, Integer>> decoratedOption = new ArrayList<>(option.size());
          for (T symbol : option) {
            Pair<T, Integer> decoratedSymbol = map.get(symbol);
            decoratedOption.add(decoratedSymbol);
            if (!resolvedGrammar.getRules().keySet().contains(decoratedSymbol)&&grammar.getRules().keySet().contains(symbol)) {
              resolvedGrammar.getRules().put(decoratedSymbol, new ArrayList<List<Pair<T, Integer>>>());
            }
          }
          resolvedGrammar.getRules().get(toFillDecoratedNonTerminal).add(decoratedOption);
        }
      }
    }
    return resolvedGrammar;
  }
  
  private static <T> Node<T> findNodeWithContent(Node<T> tree, T content) {
    if (tree.getContent().equals(content)) {
      return tree;
    }
      Node<T> foundNode = null;
    for (Node<T> child : tree.getChildren()) {
      foundNode = findNodeWithContent(child, content);
      if (foundNode!=null) {
        break;
      }
    }
    return foundNode;
  }
  
  private static <T> Node<Pair<T, Integer>> decorateTreeWithDepth(Node<T> tree) {
    Node<Pair<T, Integer>> decoratedTree = new Node<>(new Pair<>(tree.getContent(), count(contents(tree.getAncestors()), tree.getContent())));
    for (Node<T> child : tree.getChildren()) {
      decoratedTree.getChildren().add(decorateTreeWithDepth(child));
    }
    return decoratedTree;
  }
  
  public static <T> int count(Collection<T> ts, T matchT) {
    int count = 0;
    for (T t : ts) {
      if (t.equals(matchT)) {
        count = count+1;
      }
    }
    return count;
  }
  
  public static <T> List<T> contents(List<Node<T>> nodes) {
    List<T> contents = new ArrayList<>(nodes.size());
    for (Node<T> node : nodes) {
      contents.add(node.getContent());
    }
    return contents;
  }
  
  public static <T> void prettyPrintTree(Node<T> node, PrintStream ps) {
    ps.printf("%"+(1+node.getAncestors().size()*2)+"s-%s%n", "", node.getContent());
    for (Node<T> child : node.getChildren()) {
      prettyPrintTree(child, ps);
    }
  }
  
}
