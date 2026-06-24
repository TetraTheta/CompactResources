package io.github.tetratheta.compactresources.config;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/// Stores normalized stack-size rules loaded from plugin configuration.
///
/// @param defaultRule fallback rule applied when no specific item rule matches
/// @param ids         exact item ID rules keyed by namespaced item ID
/// @param tags        tag rules keyed by namespaced item tag ID
/// @param regexRules  regular expression rules matched against namespaced item IDs
public record StackSizeRules(DefaultRule defaultRule, Map<String, Integer> ids, Map<String, Integer> tags, List<RegexRule> regexRules) {
  /// Stores the fallback stack-size rule for every item material.
  ///
  /// @param enabled      whether the fallback rule should be applied
  /// @param maxStackSize maximum stack size to apply
  public record DefaultRule(boolean enabled, Integer maxStackSize) {}

  /// Stores one regular expression rule and the stack size to apply when it matches.
  ///
  /// @param pattern      compiled item ID pattern
  /// @param maxStackSize maximum stack size to apply
  public record RegexRule(Pattern pattern, Integer maxStackSize) {}
}
