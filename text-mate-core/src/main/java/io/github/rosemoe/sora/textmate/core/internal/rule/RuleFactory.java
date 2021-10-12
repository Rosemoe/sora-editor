/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/Microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package io.github.rosemoe.sora.textmate.core.internal.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.rosemoe.sora.textmate.core.internal.types.IRawCaptures;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawGrammar;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawRepository;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawRule;
import io.github.rosemoe.sora.textmate.core.internal.utils.CloneUtils;

/**
 *
 * @see https://github.com/Microsoft/vscode-textmate/blob/master/src/rule.ts
 *
 */
public class RuleFactory {

    public static CaptureRule createCaptureRule(IRuleFactoryHelper helper, final String name, final String contentName,
                                                final Integer retokenizeCapturedWithRuleId) {
        return (CaptureRule) helper.registerRule(id -> new CaptureRule(id, name, contentName, retokenizeCapturedWithRuleId));
    }

    public static int getCompiledRuleId(final IRawRule desc, final IRuleFactoryHelper helper,
                                        final IRawRepository repository) {
        if (desc.getId() == null) {

            helper.registerRule(id -> {
                desc.setId(id);

                if (desc.getMatch() != null) {
                    return new MatchRule(desc.getId(), desc.getName(), desc.getMatch(),
                            RuleFactory.compileCaptures(desc.getCaptures(), helper, repository));
                }

                if (desc.getBegin() == null) {
                    IRawRepository r = repository;
                    if (desc.getRepository() != null) {
                        r = CloneUtils.mergeObjects(repository, desc.getRepository());
                    }
                    return new IncludeOnlyRule(desc.getId(), desc.getName(), desc.getContentName(),
                            RuleFactory._compilePatterns(desc.getPatterns(), helper, r));
                }

                String ruleWhile = desc.getWhile();
                if (ruleWhile != null) {
                    return new BeginWhileRule(
                            /* desc.$vscodeTextmateLocation, */
                            desc.getId(), desc.getName(), desc.getContentName(), desc.getBegin(),
                            RuleFactory.compileCaptures(
                                    desc.getBeginCaptures() != null ? desc.getBeginCaptures() : desc.getCaptures(),
                                    helper, repository),
                            ruleWhile,
                            RuleFactory.compileCaptures(
                                    desc.getWhileCaptures() != null ? desc.getWhileCaptures() : desc.getCaptures(),
                                    helper, repository),
                            RuleFactory._compilePatterns(desc.getPatterns(), helper, repository));
                }

                return new BeginEndRule(desc.getId(), desc.getName(), desc.getContentName(), desc.getBegin(),
                        RuleFactory.compileCaptures(
                                desc.getBeginCaptures() != null ? desc.getBeginCaptures() : desc.getCaptures(),
                                helper, repository),
                        desc.getEnd(),
                        RuleFactory.compileCaptures(
                                desc.getEndCaptures() != null ? desc.getEndCaptures() : desc.getCaptures(), helper,
                                repository),
                        desc.isApplyEndPatternLast(),
                        RuleFactory._compilePatterns(desc.getPatterns(), helper, repository));
            });
        }

        return desc.getId();
    }

    private static List<CaptureRule> compileCaptures(IRawCaptures captures, IRuleFactoryHelper helper,
                                                     IRawRepository repository) {
        List<CaptureRule> r = new ArrayList<>();
        int numericCaptureId;
        int maximumCaptureId;
        int i;

        if (captures != null) {
            // Find the maximum capture id
            maximumCaptureId = 0;
            for (String captureId : captures) {
                numericCaptureId = parseInt(captureId, 10);
                if (numericCaptureId > maximumCaptureId) {
                    maximumCaptureId = numericCaptureId;
                }
            }

            // Initialize result
            for (i = 0; i <= maximumCaptureId; i++) {
                r.add(null);
            }

            // Fill out result
            for (String captureId : captures) {
                numericCaptureId = parseInt(captureId, 10);
                Integer retokenizeCapturedWithRuleId = null;
                IRawRule rule = captures.getCapture(captureId);
                if (rule.getPatterns() != null) {
                    retokenizeCapturedWithRuleId = RuleFactory.getCompiledRuleId(captures.getCapture(captureId), helper,
                            repository);
                }
                r.set(numericCaptureId, RuleFactory.createCaptureRule(helper, rule.getName(), rule.getContentName(),
                        retokenizeCapturedWithRuleId));
            }
        }

        return r;
    }

    private static int parseInt(String string, int base) {
        try {
            return Integer.parseInt(string, base);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static ICompilePatternsResult _compilePatterns(Collection<IRawRule> patterns, IRuleFactoryHelper helper,
                                                           IRawRepository repository) {
        Collection<Integer> r = new ArrayList<Integer>();
        int i;
        int len;
        int patternId;
        IRawGrammar externalGrammar;
        Rule rule;
        boolean skipRule;

        if (patterns != null) {
            for (IRawRule pattern : patterns) {
                patternId = -1;

                if (pattern.getInclude() != null) {
                    if (pattern.getInclude().charAt(0) == '#') {
                        // Local include found in `repository`
                        IRawRule localIncludedRule = repository.getProp(pattern.getInclude().substring(1));
                        if (localIncludedRule != null) {
                            patternId = RuleFactory.getCompiledRuleId(localIncludedRule, helper, repository);
                        } else {
                            // console.warn('CANNOT find rule for scopeName: ' +
                            // pattern.include + ', I am: ',
                            // repository['$base'].name);
                        }
                    } else if (pattern.getInclude().equals("$base") || pattern.getInclude().equals("$self")) {
                        // Special include also found in `repository`
                        patternId = RuleFactory.getCompiledRuleId(repository.getProp(pattern.getInclude()), helper,
                                repository);
                    } else {
                        String externalGrammarName = null, externalGrammarInclude = null;
                        int sharpIndex = pattern.getInclude().indexOf('#');
                        if (sharpIndex >= 0) {
                            externalGrammarName = pattern.getInclude().substring(0, sharpIndex);
                            externalGrammarInclude = pattern.getInclude().substring(sharpIndex + 1);
                        } else {
                            externalGrammarName = pattern.getInclude();
                        }
                        // External include
                        externalGrammar = helper.getExternalGrammar(externalGrammarName, repository);

                        if (externalGrammar != null) {
                            if (externalGrammarInclude != null) {
                                IRawRule externalIncludedRule = externalGrammar.getRepository()
                                        .getProp(externalGrammarInclude);
                                if (externalIncludedRule != null) {
                                    patternId = RuleFactory.getCompiledRuleId(externalIncludedRule, helper,
                                            externalGrammar.getRepository());
                                } else {
                                    // console.warn('CANNOT find rule for
                                    // scopeName: ' + pattern.include + ', I am:
                                    // ', repository['$base'].name);
                                }
                            } else {
                                patternId = RuleFactory.getCompiledRuleId(externalGrammar.getRepository().getSelf(),
                                        helper, externalGrammar.getRepository());
                            }
                        } else {
                            // console.warn('CANNOT find grammar for scopeName:
                            // ' + pattern.include + ', I am: ',
                            // repository['$base'].name);
                        }

                    }
                } else {
                    patternId = RuleFactory.getCompiledRuleId(pattern, helper, repository);
                }

                if (patternId != -1) {
                    rule = helper.getRule(patternId);

                    skipRule = false;

                    if (rule instanceof IncludeOnlyRule) {
                        IncludeOnlyRule ior = (IncludeOnlyRule) rule;
                        if (ior.hasMissingPatterns && ior.patterns.length == 0) {
                            skipRule = true;
                        }
                    } else if (rule instanceof BeginEndRule) {
                        BeginEndRule br = (BeginEndRule) rule;
                        if (br.hasMissingPatterns && br.patterns.length == 0) {
                            skipRule = true;
                        }
                    } else if (rule instanceof BeginWhileRule) {
                        BeginWhileRule br = (BeginWhileRule) rule;
                        if (br.hasMissingPatterns && br.patterns.length == 0) {
                            skipRule = true;
                        }
                    }

                    if (skipRule) {
                        // console.log('REMOVING RULE ENTIRELY DUE TO EMPTY
                        // PATTERNS THAT ARE MISSING');
                        continue;
                    }

                    r.add(patternId);
                }
            }
        }

        return new ICompilePatternsResult(r, ((patterns != null ? patterns.size() : 0) != r.size()));
    }

}
