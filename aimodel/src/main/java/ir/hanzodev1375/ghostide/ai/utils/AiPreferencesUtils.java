package ir.hanzodev1375.ghostide.ai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AiPreferencesUtils {

  private final Context context;

  public AiPreferencesUtils(Context context) {
    this.context = context;
  }

  private SharedPreferences getPrefs() {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  // ========== Claude ==========
  public String getClaudeApiKey() {
    return getPrefs().getString(AiConstants.SharedPreferenceKeys.KEY_AI_CLAUDE_API_KEY, "");
  }

  public void setClaudeApiKey(String apiKey) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_CLAUDE_API_KEY, apiKey)
        .apply();
  }

  public boolean hasClaudeApiKey() {
    return !getClaudeApiKey().isEmpty();
  }

  public String getClaudeModel() {
    return getPrefs()
        .getString(
            AiConstants.SharedPreferenceKeys.KEY_AI_CLAUDE_MODEL,
            AiConstants.DefaultModels.CLAUDE_DEFAULT);
  }

  public void setClaudeModel(String model) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_CLAUDE_MODEL, model)
        .apply();
  }

  // ========== ChatGPT ==========
  public String getChatGptApiKey() {
    return getPrefs().getString(AiConstants.SharedPreferenceKeys.KEY_AI_CHATGPT_API_KEY, "");
  }

  public void setChatGptApiKey(String apiKey) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_CHATGPT_API_KEY, apiKey)
        .apply();
  }

  public boolean hasChatGptApiKey() {
    return !getChatGptApiKey().isEmpty();
  }

  public String getChatGptModel() {
    return getPrefs()
        .getString(
            AiConstants.SharedPreferenceKeys.KEY_AI_CHATGPT_MODEL,
            AiConstants.DefaultModels.CHATGPT_DEFAULT);
  }

  public void setChatGptModel(String model) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_CHATGPT_MODEL, model)
        .apply();
  }

  // ========== DeepSeek ==========
  public String getDeepSeekApiKey() {
    return getPrefs().getString(AiConstants.SharedPreferenceKeys.KEY_AI_DEEPSEEK_API_KEY, "");
  }

  public void setDeepSeekApiKey(String apiKey) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_DEEPSEEK_API_KEY, apiKey)
        .apply();
  }

  public boolean hasDeepSeekApiKey() {
    return !getDeepSeekApiKey().isEmpty();
  }

  public String getDeepSeekModel() {
    return getPrefs()
        .getString(
            AiConstants.SharedPreferenceKeys.KEY_AI_DEEPSEEK_MODEL,
            AiConstants.DefaultModels.DEEPSEEK_DEFAULT);
  }

  public void setDeepSeekModel(String model) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_DEEPSEEK_MODEL, model)
        .apply();
  }

  // ========== Gemini ==========
  public String getGeminiApiKey() {
    return getPrefs().getString(AiConstants.SharedPreferenceKeys.KEY_AI_GEMINI_API_KEY, "");
  }

  public void setGeminiApiKey(String apiKey) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_GEMINI_API_KEY, apiKey)
        .apply();
  }

  public boolean hasGeminiApiKey() {
    return !getGeminiApiKey().isEmpty();
  }

  public String getGeminiModel() {
    return getPrefs()
        .getString(
            AiConstants.SharedPreferenceKeys.KEY_AI_GEMINI_MODEL,
            AiConstants.DefaultModels.GEMINI_DEFAULT);
  }

  public void setGeminiModel(String model) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_GEMINI_MODEL, model)
        .apply();
  }

  // ========== OpenRouter ==========
  public String getOpenRouterApiKey() {
    return getPrefs().getString(AiConstants.SharedPreferenceKeys.KEY_AI_OPENROUTER_API_KEY, "");
  }

  public void setOpenRouterApiKey(String apiKey) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_OPENROUTER_API_KEY, apiKey)
        .apply();
  }

  public boolean hasOpenRouterApiKey() {
    return !getOpenRouterApiKey().isEmpty();
  }

  public String getOpenRouterModel() {
    return getPrefs()
        .getString(
            AiConstants.SharedPreferenceKeys.KEY_AI_OPENROUTER_MODEL,
            AiConstants.DefaultModels.OPENROUTER_DEFAULT);
  }

  public void setOpenRouterModel(String model) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_OPENROUTER_MODEL, model)
        .apply();
  }

  // ========== Selected Provider ==========
  public String getSelectedProvider() {
    return getPrefs()
        .getString(
            AiConstants.SharedPreferenceKeys.KEY_AI_SELECTED_PROVIDER,
            AiConstants.AiProvider.CLAUDE);
  }

  public void setSelectedProvider(String provider) {
    getPrefs()
        .edit()
        .putString(AiConstants.SharedPreferenceKeys.KEY_AI_SELECTED_PROVIDER, provider)
        .apply();
  }

  public boolean hasApiKeyForProvider(String provider) {
    switch (provider) {
      case AiConstants.AiProvider.CLAUDE:
        return hasClaudeApiKey();
      case AiConstants.AiProvider.CHATGPT:
        return hasChatGptApiKey();
      case AiConstants.AiProvider.DEEPSEEK:
        return hasDeepSeekApiKey();
      case AiConstants.AiProvider.GEMINI:
        return hasGeminiApiKey();
      case AiConstants.AiProvider.OPENROUTER:
        return hasOpenRouterApiKey();
      default:
        return false;
    }
  }

  public void clearAllApiKeys() {
    getPrefs()
        .edit()
        .remove(AiConstants.SharedPreferenceKeys.KEY_AI_CLAUDE_API_KEY)
        .remove(AiConstants.SharedPreferenceKeys.KEY_AI_CHATGPT_API_KEY)
        .remove(AiConstants.SharedPreferenceKeys.KEY_AI_DEEPSEEK_API_KEY)
        .remove(AiConstants.SharedPreferenceKeys.KEY_AI_GEMINI_API_KEY)
        .remove(AiConstants.SharedPreferenceKeys.KEY_AI_OPENROUTER_API_KEY)
        .apply();
  }
}
