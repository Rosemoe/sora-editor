/**
 *  Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.languageconfiguration.internal;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.textmate.languageconfiguration.ILanguageConfiguration;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.AutoClosingPairConditional;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.CharacterPair;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.Comments;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.EnterAction;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.EnterAction.IndentAction;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.Folding;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.IndentationRule;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.OnEnterRule;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * VSCode language-configuration.json
 * @see io.github.rosemoe.sora.textmate.languageconfiguration.ILanguageConfiguration
 */
public class LanguageConfiguration implements ILanguageConfiguration {

	/**
	 * Returns an instance of {@link LanguageConfiguration} loaded from the VSCode
	 * language-configuration.json file reader.
	 *
	 * @param reader
	 * @return an instance of {@link LanguageConfiguration} loaded from the VSCode
	 *         language-configuration.json file reader.
	 */
	public static LanguageConfiguration load(Reader reader) {
		return new GsonBuilder()
				.registerTypeAdapter(OnEnterRule.class, (JsonDeserializer<OnEnterRule>) (json, typeOfT, context) -> {
					String beforeText = null;
					String afterText = null;
					EnterAction action = null;
					if (json.isJsonObject()) {
						JsonObject object = json.getAsJsonObject();
						beforeText = getAsString(object.get("beforeText")); //$NON-NLS-1$
						afterText = getAsString(object.get("afterText")); //$NON-NLS-1$
						JsonElement actionElement = object.get("action"); //$NON-NLS-1$
						if (actionElement != null && actionElement.isJsonObject()) {
							JsonObject actionObject = actionElement.getAsJsonObject();
							IndentAction indentAction = IndentAction
									.valueOf(getAsString(actionObject.get("indentAction"))); //$NON-NLS-1$
							Integer removeText = getAsInt(actionObject.get("removeText")); //$NON-NLS-1$
							String appendText = getAsString(actionObject.get("appendText")); //$NON-NLS-1$
							if (indentAction != null) {
								action = new EnterAction(indentAction);
								action.setAppendText(appendText);
								action.setRemoveText(removeText);
							}
						}
					}
					if (beforeText == null || action == null) {
						return null;
					}
					return new OnEnterRule(beforeText, afterText, action);
				}).registerTypeAdapter(Comments.class, (JsonDeserializer<Comments>) (json, typeOfT, context) -> {
					// ex: {"lineComment": "//","blockComment": [ "/*", "*/" ]}
					String lineComment = null;
					CharacterPair blockComment = null;
					if (json.isJsonObject()) {
						JsonObject object = json.getAsJsonObject();
						lineComment = getAsString(object.get("lineComment")); //$NON-NLS-1$
						JsonElement blockCommentElement = object.get("blockComment"); //$NON-NLS-1$
						if (blockCommentElement != null && blockCommentElement.isJsonArray()) {
							JsonArray blockCommentArray = blockCommentElement.getAsJsonArray();
							if (blockCommentArray.size() == 2) {
								String blockCommentStart = getAsString(blockCommentArray.get(0));
								String blockCommentEnd = getAsString(blockCommentArray.get(1));
								if (blockCommentStart != null && blockCommentEnd != null) {
									blockComment = new CharacterPair(blockCommentStart, blockCommentEnd);
								}
							}
						}
					}
					if (lineComment == null && blockComment == null) {
						return null;
					}
					return new Comments(lineComment, blockComment);
				}).registerTypeAdapter(CharacterPair.class,
						(JsonDeserializer<CharacterPair>) (json, typeOfT, context) -> {
							String open = null;
							String close = null;
							if (json.isJsonArray()) {
								// ex: ["{","}"]
								JsonArray characterPairs = json.getAsJsonArray();
								if (characterPairs.size() == 2) {
									open = getAsString(characterPairs.get(0));
									close = getAsString(characterPairs.get(1));
								}
							}
							if (open == null || close == null) {
								return null;
							}
							return new CharacterPair(open, close);
						})
				.registerTypeAdapter(AutoClosingPairConditional.class,
						(JsonDeserializer<AutoClosingPairConditional>) (json, typeOfT, context) -> {
							List<String> notInList = new ArrayList<>();
							String open = null;
							String close = null;
							if (json.isJsonArray()) {
								// ex: ["{","}"]
								JsonArray characterPairs = json.getAsJsonArray();
								if (characterPairs.size() == 2) {
									open = getAsString(characterPairs.get(0));
									close = getAsString(characterPairs.get(1));
								}
							} else if (json.isJsonObject()) {
								// ex: {"open":"'","close":"'", "notIn": ["string", "comment"]}
								JsonObject object = json.getAsJsonObject();
								open = getAsString(object.get("open")); //$NON-NLS-1$
								close = getAsString(object.get("close")); //$NON-NLS-1$
								JsonElement notInElement = object.get("notIn"); //$NON-NLS-1$
								if (notInElement != null && notInElement.isJsonArray()) {
									JsonArray notInArray = notInElement.getAsJsonArray();
									notInArray.forEach(element -> {
										String string = getAsString(element);
										if (string != null) {
											notInList.add(string);
										}
									});
								}
							}
							if (open == null || close == null) {
								return null;
							}
							return new AutoClosingPairConditional(open, close, notInList);
						})
				.registerTypeAdapter(Folding.class, (JsonDeserializer<Folding>) (json, typeOfT, context) -> {
					// ex: {"offSide": true, "markers": {"start": "^\\s*/", "end": "^\\s*"}}
					boolean offSide = false;
					String startMarker = null;
					String endMarker = null;
					if (json.isJsonObject()) {
						JsonObject object = json.getAsJsonObject();
						offSide = getAsBoolean(object.get("offSide"), offSide); //$NON-NLS-1$
						JsonElement markersElement = object.get("markers"); //$NON-NLS-1$
						if (markersElement != null && markersElement.isJsonObject()) {
							JsonObject markersObject = markersElement.getAsJsonObject();
							startMarker = getAsString(markersObject.get("start")); //$NON-NLS-1$
							endMarker = getAsString(markersObject.get("end")); //$NON-NLS-1$
						}
					}
					if (startMarker == null || endMarker == null) {
						return null;
					}
					return new Folding(offSide, startMarker, endMarker);
				})
				.registerTypeAdapter(IndentationRule.class, (JsonDeserializer<IndentationRule>) (json, typeOfT, context) -> {
					// ex: { "increaseIndentPattern": "...", "decreaseIndentPattern": "..." }
					String increaseIndentPattern = null;
					String decreaseIndentPattern = null;
					if (json.isJsonObject()) {
						JsonObject object = json.getAsJsonObject();
						increaseIndentPattern = getAsString(object.get("increaseIndentPattern")); //$NON-NLS-1$
						decreaseIndentPattern = getAsString(object.get("decreaseIndentPattern")); //$NON-NLS-1$
					}
					if (increaseIndentPattern == null || decreaseIndentPattern == null) {
						return null;
					}
					return new IndentationRule(increaseIndentPattern, decreaseIndentPattern);
				}).create().fromJson(reader, LanguageConfiguration.class);
	}

	private static String getAsString(JsonElement element) {
		if (element == null) {
			return null;
		}
		try {
			return element.getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	private static Boolean getAsBoolean(JsonElement element, Boolean defaultValue) {
		if (element == null) {
			return defaultValue;
		}
		try {
			return element.getAsBoolean();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private static Integer getAsInt(JsonElement element) {
		if (element == null) {
			return null;
		}
		try {
			return element.getAsInt();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Defines the comment symbols
	 */
	private Comments comments;

	/**
	 * The language's brackets. This configuration implicitly affects pressing Enter
	 * around these brackets.
	 */
	private List<CharacterPair> brackets;

	/**
	 * The language's rules to be evaluated when pressing Enter.
	 */
	private List<OnEnterRule> onEnterRules;

	/**
	 * The language's auto closing pairs. The 'close' character is automatically
	 * inserted with the 'open' character is typed. If not set, the configured
	 * brackets will be used.
	 */
	private List<AutoClosingPairConditional> autoClosingPairs;

	/**
	 * The language's surrounding pairs. When the 'open' character is typed on a
	 * selection, the selected string is surrounded by the open and close
	 * characters. If not set, the autoclosing pairs settings will be used.
	 */
	private List<CharacterPair> surroundingPairs;

	/**
	 * Defines when and how code should be folded in the editor
	 */
	private Folding folding;

	/**
	 * Regex which defines what is considered to be a word in the programming
	 * language.
	 */
	private String wordPattern;

	private String autoCloseBefore;

	private IndentationRule indentationRules;

	@Override
	public Comments getComments() {
		return comments;
	}

	@Override
	public List<CharacterPair> getBrackets() {
		return brackets;
	}

	@Override
	public List<AutoClosingPairConditional> getAutoClosingPairs() {
		return autoClosingPairs;
	}

	@Override
	public String getAutoCloseBefore() {
		return autoCloseBefore;
	}

	@Override
	public List<OnEnterRule> getOnEnterRules() {
		return onEnterRules;
	}

	@Override
	public List<CharacterPair> getSurroundingPairs() {
		return surroundingPairs;
	}

	@Override
	public Folding getFolding() {
		return folding;
	}

	@Override
	public String getWordPattern() {
		return wordPattern;
	}

	@Override
	public IndentationRule getIndentationRule() {
		return indentationRules;
	}
}
