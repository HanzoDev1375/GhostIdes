package ir.hanzodev1375.ghostide.codeeditors.langs.lsp

import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.utils.toFileUri
import java.net.URI
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Range

object LspUriBridge {
  @JvmStatic fun uri(project: LspProject): URI = project.projectUri.toUri()

  @JvmStatic fun uri(editor: LspEditor): URI = editor.uri.toUri()

  @JvmStatic
  fun path(uriString: String): String? =
          try {
            URI(uriString).toFileUri().path
          } catch (e: Exception) {
            null
          }

  @JvmStatic
  fun findDiagnostics(editor: LspEditor, range: Range): List<Diagnostic> =
          editor.diagnosticsContainer.findDiagnostics(editor.uri, range) ?: emptyList()
}