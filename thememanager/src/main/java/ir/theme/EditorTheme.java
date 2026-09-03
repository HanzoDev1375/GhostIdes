package ir.theme;

import com.google.gson.annotations.SerializedName;

public class EditorTheme {

  @SerializedName("lineDivider")
  private String lineDivider;

  @SerializedName("lineNumber")
  private String lineNumber;

  @SerializedName("lineNumberBackground")
  private String lineNumberBackground;

  @SerializedName("wholeBackground")
  private String wholeBackground;

  @SerializedName("textNormal")
  private String textNormal;

  @SerializedName("selectedTextBackground")
  private String selectedTextBackground;

  @SerializedName("selectionInsert")
  private String selectionInsert;

  @SerializedName("selectionHandle")
  private String selectionHandle;

  @SerializedName("currentLine")
  private String currentLine;

  @SerializedName("underline")
  private String underline;

  @SerializedName("scrollBarThumb")
  private String scrollBarThumb;

  @SerializedName("scrollBarThumbPressed")
  private String scrollBarThumbPressed;

  @SerializedName("scrollBarTrack")
  private String scrollBarTrack;

  @SerializedName("blockLine")
  private String blockLine;

  @SerializedName("blockLineCurrent")
  private String blockLineCurrent;

  @SerializedName("lineNumberPanel")
  private String lineNumberPanel;

  @SerializedName("lineNumberPanelText")
  private String lineNumberPanelText;

  @SerializedName("completionWndBackground")
  private String completionWndBackground;

  @SerializedName("completionWndCorner")
  private String completionWndCorner;

  @SerializedName("keyword")
  private String keyword;

  @SerializedName("comment")
  private String comment;

  @SerializedName("operator")
  private String operator;

  @SerializedName("literal")
  private String literal;

  @SerializedName("identifierVar")
  private String identifierVar;

  @SerializedName("identifierName")
  private String identifierName;

  @SerializedName("functionName")
  private String functionName;

  @SerializedName("annotation")
  private String annotation;

  @SerializedName("matchedTextBackground")
  private String matchedTextBackground;

  @SerializedName("matchedTextBorder")
  private String matchedTextBorder;

  @SerializedName("textSelected")
  private String textSelected;

  @SerializedName("nonPrintableChar")
  private String nonPrintableChar;

  @SerializedName("htmlTag")
  private String htmlTag;

  @SerializedName("attributeName")
  private String attributeName;

  @SerializedName("attributeValue")
  private String attributeValue;

  @SerializedName("problemError")
  private String problemError;

  @SerializedName("problemWarning")
  private String problemWarning;

  @SerializedName("problemTypo")
  private String problemTypo;

  @SerializedName("colornextdot")
  private String colornextdot;

  @SerializedName("colornextbrak")
  private String colornextbrak;

  @SerializedName("colornextchar")
  private String colornextchar;

  @SerializedName("coloruppercase")
  private String coloruppercase;

  @SerializedName("colornextless")
  private String colornextless;

  // ========== فیلدهای جدید (تکمیل کننده EditorColorScheme) ==========
  @SerializedName("lineNumberCurrent")
  private String lineNumberCurrent;

  @SerializedName("selectedTextBorder")
  private String selectedTextBorder;

  @SerializedName("currentRowBorder")
  private String currentRowBorder;

  @SerializedName("highlightedDelimitersBackground")
  private String highlightedDelimitersBackground;

  @SerializedName("highlightedDelimitersUnderline")
  private String highlightedDelimitersUnderline;

  @SerializedName("highlightedDelimitersForeground")
  private String highlightedDelimitersForeground;

  @SerializedName("highlightedDelimitersBorder")
  private String highlightedDelimitersBorder;

  @SerializedName("textHighlightBackground")
  private String textHighlightBackground;

  @SerializedName("textHighlightBorder")
  private String textHighlightBorder;

  @SerializedName("textHighlightStrongBackground")
  private String textHighlightStrongBackground;

  @SerializedName("textHighlightStrongBorder")
  private String textHighlightStrongBorder;

  @SerializedName("staticSpanBackground")
  private String staticSpanBackground;

  @SerializedName("staticSpanForeground")
  private String staticSpanForeground;

  @SerializedName("textInlayHintBackground")
  private String textInlayHintBackground;

  @SerializedName("textInlayHintForeground")
  private String textInlayHintForeground;

  @SerializedName("snippetBackgroundEditing")
  private String snippetBackgroundEditing;

  @SerializedName("snippetBackgroundRelated")
  private String snippetBackgroundRelated;

  @SerializedName("snippetBackgroundInactive")
  private String snippetBackgroundInactive;

  @SerializedName("hardWrapMarker")
  private String hardWrapMarker;

  @SerializedName("functionCharBackgroundStroke")
  private String functionCharBackgroundStroke;

  @SerializedName("diagnosticTooltipBackground")
  private String diagnosticTooltipBackground;

  @SerializedName("diagnosticTooltipBriefMsg")
  private String diagnosticTooltipBriefMsg;

  @SerializedName("diagnosticTooltipDetailedMsg")
  private String diagnosticTooltipDetailedMsg;

  @SerializedName("diagnosticTooltipAction")
  private String diagnosticTooltipAction;

  @SerializedName("stickyScrollDivider")
  private String stickyScrollDivider;

  @SerializedName("strikeThrough")
  private String strikeThrough;

  @SerializedName("sideBlockLine")
  private String sideBlockLine;

  @SerializedName("completionWndTextPrimary")
  private String completionWndTextPrimary;

  @SerializedName("completionWndTextSecondary")
  private String completionWndTextSecondary;

  @SerializedName("completionWndItemCurrent")
  private String completionWndItemCurrent;

  @SerializedName("completionWndTextMatched")
  private String completionWndTextMatched;

  @SerializedName("signatureBackground")
  private String signatureBackground;

  @SerializedName("signatureBorder")
  private String signatureBorder;

  @SerializedName("signatureTextNormal")
  private String signatureTextNormal;

  @SerializedName("signatureTextHighlightedParameter")
  private String signatureTextHighlightedParameter;

  @SerializedName("hoverBackground")
  private String hoverBackground;

  @SerializedName("hoverBorder")
  private String hoverBorder;

  @SerializedName("hoverTextNormal")
  private String hoverTextNormal;

  @SerializedName("hoverTextHighlighted")
  private String hoverTextHighlighted;

  @SerializedName("textActionWindowBackground")
  private String textActionWindowBackground;

  @SerializedName("textActionWindowIconColor")
  private String textActionWindowIconColor;

  @SerializedName("minimapBackground")
  private String minimapBackground;

  @SerializedName("minimapViewport")
  private String minimapViewport;

  @SerializedName("minimapViewportBorder")
  private String minimapViewportBorder;

  /** add new item for theme in 1.1.4 */
  @SerializedName("bracketlevelmatch1")
  private String bracketlevelmatch1;

  @SerializedName("bracketlevelmatch2")
  private String bracketlevelmatch2;

  @SerializedName("bracketlevelmatch3")
  private String bracketlevelmatch3;

  @SerializedName("bracketlevelmatch4")
  private String bracketlevelmatch4;

  @SerializedName("bracketlevelmatch5")
  private String bracketlevelmatch5;

  @SerializedName("bracketlevelmatch6")
  private String bracketlevelmatch6;

  // Getters and Setters for all fields (only showing new ones for brevity, include all existing)
  public String getLineDivider() {
    return lineDivider;
  }

  public void setLineDivider(String lineDivider) {
    this.lineDivider = lineDivider;
  }

  public String getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(String lineNumber) {
    this.lineNumber = lineNumber;
  }

  public String getLineNumberBackground() {
    return lineNumberBackground;
  }

  public void setLineNumberBackground(String lineNumberBackground) {
    this.lineNumberBackground = lineNumberBackground;
  }

  public String getWholeBackground() {
    return wholeBackground;
  }

  public void setWholeBackground(String wholeBackground) {
    this.wholeBackground = wholeBackground;
  }

  public String getTextNormal() {
    return textNormal;
  }

  public void setTextNormal(String textNormal) {
    this.textNormal = textNormal;
  }

  public String getSelectedTextBackground() {
    return selectedTextBackground;
  }

  public void setSelectedTextBackground(String selectedTextBackground) {
    this.selectedTextBackground = selectedTextBackground;
  }

  public String getSelectionInsert() {
    return selectionInsert;
  }

  public void setSelectionInsert(String selectionInsert) {
    this.selectionInsert = selectionInsert;
  }

  public String getSelectionHandle() {
    return selectionHandle;
  }

  public void setSelectionHandle(String selectionHandle) {
    this.selectionHandle = selectionHandle;
  }

  public String getCurrentLine() {
    return currentLine;
  }

  public void setCurrentLine(String currentLine) {
    this.currentLine = currentLine;
  }

  public String getUnderline() {
    return underline;
  }

  public void setUnderline(String underline) {
    this.underline = underline;
  }

  public String getScrollBarThumb() {
    return scrollBarThumb;
  }

  public void setScrollBarThumb(String scrollBarThumb) {
    this.scrollBarThumb = scrollBarThumb;
  }

  public String getScrollBarThumbPressed() {
    return scrollBarThumbPressed;
  }

  public void setScrollBarThumbPressed(String scrollBarThumbPressed) {
    this.scrollBarThumbPressed = scrollBarThumbPressed;
  }

  public String getScrollBarTrack() {
    return scrollBarTrack;
  }

  public void setScrollBarTrack(String scrollBarTrack) {
    this.scrollBarTrack = scrollBarTrack;
  }

  public String getBlockLine() {
    return blockLine;
  }

  public void setBlockLine(String blockLine) {
    this.blockLine = blockLine;
  }

  public String getBlockLineCurrent() {
    return blockLineCurrent;
  }

  public void setBlockLineCurrent(String blockLineCurrent) {
    this.blockLineCurrent = blockLineCurrent;
  }

  public String getLineNumberPanel() {
    return lineNumberPanel;
  }

  public void setLineNumberPanel(String lineNumberPanel) {
    this.lineNumberPanel = lineNumberPanel;
  }

  public String getLineNumberPanelText() {
    return lineNumberPanelText;
  }

  public void setLineNumberPanelText(String lineNumberPanelText) {
    this.lineNumberPanelText = lineNumberPanelText;
  }

  public String getCompletionWndBackground() {
    return completionWndBackground;
  }

  public void setCompletionWndBackground(String completionWndBackground) {
    this.completionWndBackground = completionWndBackground;
  }

  public String getCompletionWndCorner() {
    return completionWndCorner;
  }

  public void setCompletionWndCorner(String completionWndCorner) {
    this.completionWndCorner = completionWndCorner;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getLiteral() {
    return literal;
  }

  public void setLiteral(String literal) {
    this.literal = literal;
  }

  public String getIdentifierVar() {
    return identifierVar;
  }

  public void setIdentifierVar(String identifierVar) {
    this.identifierVar = identifierVar;
  }

  public String getIdentifierName() {
    return identifierName;
  }

  public void setIdentifierName(String identifierName) {
    this.identifierName = identifierName;
  }

  public String getFunctionName() {
    return functionName;
  }

  public void setFunctionName(String functionName) {
    this.functionName = functionName;
  }

  public String getAnnotation() {
    return annotation;
  }

  public void setAnnotation(String annotation) {
    this.annotation = annotation;
  }

  public String getMatchedTextBackground() {
    return matchedTextBackground;
  }

  public void setMatchedTextBackground(String matchedTextBackground) {
    this.matchedTextBackground = matchedTextBackground;
  }

  public String getMatchedTextBorder() {
    return matchedTextBorder;
  }

  public void setMatchedTextBorder(String matchedTextBorder) {
    this.matchedTextBorder = matchedTextBorder;
  }

  public String getTextSelected() {
    return textSelected;
  }

  public void setTextSelected(String textSelected) {
    this.textSelected = textSelected;
  }

  public String getNonPrintableChar() {
    return nonPrintableChar;
  }

  public void setNonPrintableChar(String nonPrintableChar) {
    this.nonPrintableChar = nonPrintableChar;
  }

  public String getHtmlTag() {
    return htmlTag;
  }

  public void setHtmlTag(String htmlTag) {
    this.htmlTag = htmlTag;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public void setAttributeName(String attributeName) {
    this.attributeName = attributeName;
  }

  public String getAttributeValue() {
    return attributeValue;
  }

  public void setAttributeValue(String attributeValue) {
    this.attributeValue = attributeValue;
  }

  public String getProblemError() {
    return problemError;
  }

  public void setProblemError(String problemError) {
    this.problemError = problemError;
  }

  public String getProblemWarning() {
    return problemWarning;
  }

  public void setProblemWarning(String problemWarning) {
    this.problemWarning = problemWarning;
  }

  public String getProblemTypo() {
    return problemTypo;
  }

  public void setProblemTypo(String problemTypo) {
    this.problemTypo = problemTypo;
  }

  public String getColornextdot() {
    return colornextdot;
  }

  public void setColornextdot(String colornextdot) {
    this.colornextdot = colornextdot;
  }

  public String getColornextbrak() {
    return colornextbrak;
  }

  public void setColornextbrak(String colornextbrak) {
    this.colornextbrak = colornextbrak;
  }

  public String getColornextchar() {
    return colornextchar;
  }

  public void setColornextchar(String colornextchar) {
    this.colornextchar = colornextchar;
  }

  public String getColoruppercase() {
    return coloruppercase;
  }

  public void setColoruppercase(String coloruppercase) {
    this.coloruppercase = coloruppercase;
  }

  public String getColornextless() {
    return colornextless;
  }

  public void setColornextless(String colornextless) {
    this.colornextless = colornextless;
  }

  // Getters/Setters for new fields
  public String getLineNumberCurrent() {
    return lineNumberCurrent;
  }

  public void setLineNumberCurrent(String lineNumberCurrent) {
    this.lineNumberCurrent = lineNumberCurrent;
  }

  public String getSelectedTextBorder() {
    return selectedTextBorder;
  }

  public void setSelectedTextBorder(String selectedTextBorder) {
    this.selectedTextBorder = selectedTextBorder;
  }

  public String getCurrentRowBorder() {
    return currentRowBorder;
  }

  public void setCurrentRowBorder(String currentRowBorder) {
    this.currentRowBorder = currentRowBorder;
  }

  public String getHighlightedDelimitersBackground() {
    return highlightedDelimitersBackground;
  }

  public void setHighlightedDelimitersBackground(String highlightedDelimitersBackground) {
    this.highlightedDelimitersBackground = highlightedDelimitersBackground;
  }

  public String getHighlightedDelimitersUnderline() {
    return highlightedDelimitersUnderline;
  }

  public void setHighlightedDelimitersUnderline(String highlightedDelimitersUnderline) {
    this.highlightedDelimitersUnderline = highlightedDelimitersUnderline;
  }

  public String getHighlightedDelimitersForeground() {
    return highlightedDelimitersForeground;
  }

  public void setHighlightedDelimitersForeground(String highlightedDelimitersForeground) {
    this.highlightedDelimitersForeground = highlightedDelimitersForeground;
  }

  public String getHighlightedDelimitersBorder() {
    return highlightedDelimitersBorder;
  }

  public void setHighlightedDelimitersBorder(String highlightedDelimitersBorder) {
    this.highlightedDelimitersBorder = highlightedDelimitersBorder;
  }

  public String getTextHighlightBackground() {
    return textHighlightBackground;
  }

  public void setTextHighlightBackground(String textHighlightBackground) {
    this.textHighlightBackground = textHighlightBackground;
  }

  public String getTextHighlightBorder() {
    return textHighlightBorder;
  }

  public void setTextHighlightBorder(String textHighlightBorder) {
    this.textHighlightBorder = textHighlightBorder;
  }

  public String getTextHighlightStrongBackground() {
    return textHighlightStrongBackground;
  }

  public void setTextHighlightStrongBackground(String textHighlightStrongBackground) {
    this.textHighlightStrongBackground = textHighlightStrongBackground;
  }

  public String getTextHighlightStrongBorder() {
    return textHighlightStrongBorder;
  }

  public void setTextHighlightStrongBorder(String textHighlightStrongBorder) {
    this.textHighlightStrongBorder = textHighlightStrongBorder;
  }

  public String getStaticSpanBackground() {
    return staticSpanBackground;
  }

  public void setStaticSpanBackground(String staticSpanBackground) {
    this.staticSpanBackground = staticSpanBackground;
  }

  public String getStaticSpanForeground() {
    return staticSpanForeground;
  }

  public void setStaticSpanForeground(String staticSpanForeground) {
    this.staticSpanForeground = staticSpanForeground;
  }

  public String getTextInlayHintBackground() {
    return textInlayHintBackground;
  }

  public void setTextInlayHintBackground(String textInlayHintBackground) {
    this.textInlayHintBackground = textInlayHintBackground;
  }

  public String getTextInlayHintForeground() {
    return textInlayHintForeground;
  }

  public void setTextInlayHintForeground(String textInlayHintForeground) {
    this.textInlayHintForeground = textInlayHintForeground;
  }

  public String getSnippetBackgroundEditing() {
    return snippetBackgroundEditing;
  }

  public void setSnippetBackgroundEditing(String snippetBackgroundEditing) {
    this.snippetBackgroundEditing = snippetBackgroundEditing;
  }

  public String getSnippetBackgroundRelated() {
    return snippetBackgroundRelated;
  }

  public void setSnippetBackgroundRelated(String snippetBackgroundRelated) {
    this.snippetBackgroundRelated = snippetBackgroundRelated;
  }

  public String getSnippetBackgroundInactive() {
    return snippetBackgroundInactive;
  }

  public void setSnippetBackgroundInactive(String snippetBackgroundInactive) {
    this.snippetBackgroundInactive = snippetBackgroundInactive;
  }

  public String getHardWrapMarker() {
    return hardWrapMarker;
  }

  public void setHardWrapMarker(String hardWrapMarker) {
    this.hardWrapMarker = hardWrapMarker;
  }

  public String getFunctionCharBackgroundStroke() {
    return functionCharBackgroundStroke;
  }

  public void setFunctionCharBackgroundStroke(String functionCharBackgroundStroke) {
    this.functionCharBackgroundStroke = functionCharBackgroundStroke;
  }

  public String getDiagnosticTooltipBackground() {
    return diagnosticTooltipBackground;
  }

  public void setDiagnosticTooltipBackground(String diagnosticTooltipBackground) {
    this.diagnosticTooltipBackground = diagnosticTooltipBackground;
  }

  public String getDiagnosticTooltipBriefMsg() {
    return diagnosticTooltipBriefMsg;
  }

  public void setDiagnosticTooltipBriefMsg(String diagnosticTooltipBriefMsg) {
    this.diagnosticTooltipBriefMsg = diagnosticTooltipBriefMsg;
  }

  public String getDiagnosticTooltipDetailedMsg() {
    return diagnosticTooltipDetailedMsg;
  }

  public void setDiagnosticTooltipDetailedMsg(String diagnosticTooltipDetailedMsg) {
    this.diagnosticTooltipDetailedMsg = diagnosticTooltipDetailedMsg;
  }

  public String getDiagnosticTooltipAction() {
    return diagnosticTooltipAction;
  }

  public void setDiagnosticTooltipAction(String diagnosticTooltipAction) {
    this.diagnosticTooltipAction = diagnosticTooltipAction;
  }

  public String getStickyScrollDivider() {
    return stickyScrollDivider;
  }

  public void setStickyScrollDivider(String stickyScrollDivider) {
    this.stickyScrollDivider = stickyScrollDivider;
  }

  public String getStrikeThrough() {
    return strikeThrough;
  }

  public void setStrikeThrough(String strikeThrough) {
    this.strikeThrough = strikeThrough;
  }

  public String getSideBlockLine() {
    return sideBlockLine;
  }

  public void setSideBlockLine(String sideBlockLine) {
    this.sideBlockLine = sideBlockLine;
  }

  public String getCompletionWndTextPrimary() {
    return completionWndTextPrimary;
  }

  public void setCompletionWndTextPrimary(String completionWndTextPrimary) {
    this.completionWndTextPrimary = completionWndTextPrimary;
  }

  public String getCompletionWndTextSecondary() {
    return completionWndTextSecondary;
  }

  public void setCompletionWndTextSecondary(String completionWndTextSecondary) {
    this.completionWndTextSecondary = completionWndTextSecondary;
  }

  public String getCompletionWndItemCurrent() {
    return completionWndItemCurrent;
  }

  public void setCompletionWndItemCurrent(String completionWndItemCurrent) {
    this.completionWndItemCurrent = completionWndItemCurrent;
  }

  public String getCompletionWndTextMatched() {
    return completionWndTextMatched;
  }

  public void setCompletionWndTextMatched(String completionWndTextMatched) {
    this.completionWndTextMatched = completionWndTextMatched;
  }

  public String getSignatureBackground() {
    return signatureBackground;
  }

  public void setSignatureBackground(String signatureBackground) {
    this.signatureBackground = signatureBackground;
  }

  public String getSignatureBorder() {
    return signatureBorder;
  }

  public void setSignatureBorder(String signatureBorder) {
    this.signatureBorder = signatureBorder;
  }

  public String getSignatureTextNormal() {
    return signatureTextNormal;
  }

  public void setSignatureTextNormal(String signatureTextNormal) {
    this.signatureTextNormal = signatureTextNormal;
  }

  public String getSignatureTextHighlightedParameter() {
    return signatureTextHighlightedParameter;
  }

  public void setSignatureTextHighlightedParameter(String signatureTextHighlightedParameter) {
    this.signatureTextHighlightedParameter = signatureTextHighlightedParameter;
  }

  public String getHoverBackground() {
    return hoverBackground;
  }

  public void setHoverBackground(String hoverBackground) {
    this.hoverBackground = hoverBackground;
  }

  public String getHoverBorder() {
    return hoverBorder;
  }

  public void setHoverBorder(String hoverBorder) {
    this.hoverBorder = hoverBorder;
  }

  public String getHoverTextNormal() {
    return hoverTextNormal;
  }

  public void setHoverTextNormal(String hoverTextNormal) {
    this.hoverTextNormal = hoverTextNormal;
  }

  public String getHoverTextHighlighted() {
    return hoverTextHighlighted;
  }

  public void setHoverTextHighlighted(String hoverTextHighlighted) {
    this.hoverTextHighlighted = hoverTextHighlighted;
  }

  public String getTextActionWindowBackground() {
    return textActionWindowBackground;
  }

  public void setTextActionWindowBackground(String textActionWindowBackground) {
    this.textActionWindowBackground = textActionWindowBackground;
  }

  public String getTextActionWindowIconColor() {
    return textActionWindowIconColor;
  }

  public void setTextActionWindowIconColor(String textActionWindowIconColor) {
    this.textActionWindowIconColor = textActionWindowIconColor;
  }

  public String getMinimapBackground() {
    return minimapBackground;
  }

  public void setMinimapBackground(String minimapBackground) {
    this.minimapBackground = minimapBackground;
  }

  public String getMinimapViewport() {
    return minimapViewport;
  }

  public void setMinimapViewport(String minimapViewport) {
    this.minimapViewport = minimapViewport;
  }

  public String getMinimapViewportBorder() {
    return minimapViewportBorder;
  }

  public void setMinimapViewportBorder(String minimapViewportBorder) {
    this.minimapViewportBorder = minimapViewportBorder;
  }

  public String getBracketlevelmatch1() {
    return this.bracketlevelmatch1;
  }

  public void setBracketlevelmatch1(String bracketlevelmatch1) {
    this.bracketlevelmatch1 = bracketlevelmatch1;
  }

  public String getBracketlevelmatch2() {
    return this.bracketlevelmatch2;
  }

  public void setBracketlevelmatch2(String bracketlevelmatch2) {
    this.bracketlevelmatch2 = bracketlevelmatch2;
  }

  public String getBracketlevelmatch3() {
    return this.bracketlevelmatch3;
  }

  public void setBracketlevelmatch3(String bracketlevelmatch3) {
    this.bracketlevelmatch3 = bracketlevelmatch3;
  }

  public String getBracketlevelmatch4() {
    return this.bracketlevelmatch4;
  }

  public void setBracketlevelmatch4(String bracketlevelmatch4) {
    this.bracketlevelmatch4 = bracketlevelmatch4;
  }

  public String getBracketlevelmatch5() {
    return this.bracketlevelmatch5;
  }

  public void setBracketlevelmatch5(String bracketlevelmatch5) {
    this.bracketlevelmatch5 = bracketlevelmatch5;
  }

  public String getBracketlevelmatch6() {
    return this.bracketlevelmatch6;
  }

  public void setBracketlevelmatch6(String bracketlevelmatch6) {
    this.bracketlevelmatch6 = bracketlevelmatch6;
  }
}
