package io.llmttestrunner.artifact

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Captures screen state including view hierarchy and screenshots.
 * This information is used to provide context to the LLM when generating artifacts.
 */
class ScreenStateCapture(
    private val composeTestRule: ComposeTestRule? = null
) {
    
    /**
     * Capture current screen state.
     */
    fun captureScreenState(screenshotDir: File? = null): ScreenContext {
        val hierarchy = captureViewHierarchy()
        val visibleElements = extractVisibleElements()
        val screenshotPath = screenshotDir?.let { dir ->
            captureScreenshot(dir)
        }
        
        return ScreenContext(
            timestamp = System.currentTimeMillis(),
            viewHierarchy = hierarchy,
            screenshotPath = screenshotPath,
            visibleElements = visibleElements
        )
    }

    /**
     * Capture view hierarchy as text representation.
     */
    private fun captureViewHierarchy(): String {
        val rootView = getRootView() ?: return "No view hierarchy available"
        val sb = StringBuilder()
        dumpViewHierarchy(rootView, sb, 0)
        return sb.toString()
    }

    /**
     * Recursively dump view hierarchy.
     */
    private fun dumpViewHierarchy(view: View, sb: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val className = view::class.java.simpleName
        val id = try {
            view.context.resources.getResourceEntryName(view.id)
        } catch (e: Exception) {
            "no-id"
        }
        
        val text = when {
            view is android.widget.TextView -> view.text.toString()
            else -> ""
        }
        
        val visibility = when (view.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }
        
        sb.append("$indent$className (id=$id, visibility=$visibility")
        if (text.isNotEmpty()) {
            sb.append(", text='$text'")
        }
        sb.append(")\n")
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                dumpViewHierarchy(view.getChildAt(i), sb, depth + 1)
            }
        }
    }

    /**
     * Extract information about visible elements.
     */
    private fun extractVisibleElements(): List<ElementInfo> {
        val elements = mutableListOf<ElementInfo>()
        val rootView = getRootView() ?: return elements
        
        extractElementsRecursive(rootView, elements)
        return elements
    }

    /**
     * Recursively extract element information.
     */
    private fun extractElementsRecursive(view: View, elements: MutableList<ElementInfo>) {
        if (view.visibility != View.VISIBLE) return
        
        val id = try {
            view.context.resources.getResourceEntryName(view.id)
        } catch (e: Exception) {
            ""
        }
        
        val text = when (view) {
            is android.widget.TextView -> view.text.toString()
            is android.widget.Button -> view.text.toString()
            else -> ""
        }
        
        val contentDescription = view.contentDescription?.toString() ?: ""
        
        val bounds = "${view.left},${view.top},${view.right},${view.bottom}"
        
        if (id.isNotEmpty() || text.isNotEmpty() || contentDescription.isNotEmpty()) {
            elements.add(
                ElementInfo(
                    id = id,
                    tag = view.transitionName ?: "",
                    text = text,
                    contentDescription = contentDescription,
                    className = view::class.java.simpleName,
                    bounds = bounds,
                    isClickable = view.isClickable,
                    isEnabled = view.isEnabled,
                    isVisible = view.visibility == View.VISIBLE
                )
            )
        }
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                extractElementsRecursive(view.getChildAt(i), elements)
            }
        }
    }

    /**
     * Capture screenshot and save to file.
     */
    private fun captureScreenshot(dir: File): String? {
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val filename = "screenshot_$timestamp.png"
            val file = File(dir, filename)
            
            val rootView = getRootView()
            if (rootView != null) {
                rootView.isDrawingCacheEnabled = true
                val bitmap = Bitmap.createBitmap(rootView.drawingCache)
                rootView.isDrawingCacheEnabled = false
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                return file.absolutePath
            }
        } catch (e: Exception) {
            println("⚠ Failed to capture screenshot: ${e.message}")
        }
        return null
    }

    /**
     * Get the root view of the current activity.
     */
    private fun getRootView(): View? {
        return try {
            val activity = InstrumentationRegistry.getInstrumentation()
                .targetContext
            val windowManager = activity.getSystemService(android.content.Context.WINDOW_SERVICE) 
                as? android.view.WindowManager
            windowManager?.defaultDisplay?.let {
                // This is a simplified approach
                // In a real implementation, you'd need to get the actual activity's root view
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate a textual summary of the screen for LLM context.
     */
    fun generateScreenSummary(context: ScreenContext): String {
        val summary = StringBuilder()
        summary.append("=== Screen State ===\n\n")
        
        summary.append("Visible Elements:\n")
        context.visibleElements.forEach { element ->
            if (element.text.isNotEmpty()) {
                summary.append("- ${element.className}: \"${element.text}\"")
            } else {
                summary.append("- ${element.className}")
            }
            
            if (element.id.isNotEmpty()) {
                summary.append(" (id: ${element.id})")
            }
            if (element.tag.isNotEmpty()) {
                summary.append(" (tag: ${element.tag})")
            }
            if (element.isClickable) {
                summary.append(" [clickable]")
            }
            summary.append("\n")
        }
        
        if (context.visibleElements.isEmpty()) {
            summary.append("  No elements detected\n")
        }
        
        summary.append("\nView Hierarchy:\n")
        summary.append(context.viewHierarchy)
        
        return summary.toString()
    }
}

