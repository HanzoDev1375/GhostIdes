package ir.hanzodev1375.ghostide.javacore;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.lsp4j.CompletionItem;

/**
 * Requestor for {@code ICompilationUnit.codeComplete(...)} that converts JDT completion proposals
 * into LSP4j {@link CompletionItem}s. Delegates the heavy lifting to {@link JdtProposals} which
 * maps each {@link org.eclipse.jdt.core.CompletionProposal} into an item with kind, label,
 * documentation and a text edit replacing the prefix.
 */
public final class InMemoryCompletionRequestor extends CompletionRequestor {

  public final List<CompletionItem> items = new ArrayList<>();
  private final String text;
  private final int requestOffset;

  public InMemoryCompletionRequestor(String text) {
    this.text = text;
    this.requestOffset = -1;
    setRequireExtendedContext(false);
    // setAllowsRequiredProposals();
  }

  @Override
  public void accept(org.eclipse.jdt.core.CompletionProposal proposal) {
    var item = JdtProposals.toItem(proposal, text, requestOffset);
    if (item != null) items.add(item);
  }
}
